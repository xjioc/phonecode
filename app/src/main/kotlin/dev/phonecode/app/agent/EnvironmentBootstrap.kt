package dev.phonecode.app.agent

import android.content.Context
import android.content.res.AssetManager
import android.net.ConnectivityManager
import android.os.Build
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPInputStream

object EnvironmentBootstrap {

    private const val ALPINE_VERSION = "3.21.7"
    private const val ALPINE_ASSET = "alpine-aarch64.rootfs"
    private const val ALPINE_SHA256 = "d1d1a3fae5f4d6146e9742790a47fcb116199622cfb8439f218a4d5fbe5000da"
    private const val MAX_ARCHIVE_ENTRY_BYTES = 512L * 1024L * 1024L
    private const val MAX_ARCHIVE_BYTES = 2L * 1024L * 1024L * 1024L

    class Userland internal constructor(
        private val linux: Linux?,
    ) {
        val linuxAvailable: Boolean get() = linux != null

        fun linuxReady(): Boolean = linux?.ready() == true

        fun ensureLinux(): Boolean = linux?.ensure() ?: false

        fun shell(workspacePath: String): List<String> {
            linux?.kickoffSetup()
            check(linux?.ready() == true) { "bundled Alpine environment is not ready" }
            return linux.shellArgv(workspacePath)
        }

        fun shellEnv(): Map<String, String> {
            check(linux?.ready() == true) { "bundled Alpine environment is not ready" }
            return linux.env()
        }
    }

    fun ensure(context: Context): Userland = Userland(buildLinux(context))

    private fun buildLinux(context: Context): Linux? {
        val nativeDir = context.applicationInfo.nativeLibraryDir
        val proot = File(nativeDir, "libproot.so")
        val loader = File(nativeDir, "libproot-loader.so")
        if (!proot.canExecute() || !loader.exists()) return null
        if (Build.SUPPORTED_ABIS.firstOrNull() != "arm64-v8a") return null
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return Linux(
            proot = proot,
            loader = loader,
            rootfs = File(context.filesDir, "linux/aarch64"),
            tmpDir = File(context.cacheDir, "proot-tmp"),
            workspacesRoot = File(context.filesDir, "workspaces"),
            assets = context.assets,
            dnsServers = {
                connectivity.getLinkProperties(connectivity.activeNetwork)
                    ?.dnsServers
                    ?.mapNotNull { it.hostAddress }
                    .orEmpty()
            },
        )
    }

    /**
     * The proot + Alpine tier. [ready] is a cheap marker check; [ensure]/[kickoffSetup] extract the BUNDLED
     * rootfs once (local, no network), so the base Linux is reliable - the old runtime download could die
     * mid-flight (detached thread, flaky network) and leave the agent with no package manager.
     */
    class Linux internal constructor(
        private val proot: File,
        private val loader: File,
        private val rootfs: File,
        private val tmpDir: File,
        private val workspacesRoot: File,
        private val assets: AssetManager,
        private val dnsServers: () -> List<String>,
    ) {
        // Marker is version-keyed so a newer bundled rootfs (after an app update) re-extracts instead of
        // running against the stale tree.
        private val marker = File(rootfs.parentFile, "${rootfs.name}-$ALPINE_VERSION-${ALPINE_SHA256.take(12)}.ready")
        private val legacyMarker = File(rootfs.parentFile, "${rootfs.name}-$ALPINE_VERSION.ready")
        private val started = AtomicBoolean(false)

        fun ready(): Boolean = migrateLinuxMarker(marker, legacyMarker, rootfs)

        /** Extract the bundled rootfs once, on a background thread (no-op if ready or already running). */
        fun kickoffSetup() {
            if (ready() || !started.compareAndSet(false, true)) return
            Thread({ runCatching { ensure() } }, "alpine-setup").apply { isDaemon = true }.start()
        }

        /** Idempotent, blocking: extract the bundled Alpine rootfs if not already done. Returns ready state. */
        @Synchronized
        fun ensure(): Boolean {
            if (ready()) return true
            return runCatching { extract() }.getOrDefault(false)
        }

        /**
         * Extract the bundled gzipped tar in PURE KOTLIN. We do NOT shell out to busybox/toybox tar: that
         * process runs in the app's untrusted_app domain whose seccomp filter SIGSYS-kills the metadata
         * syscalls tar uses (timestamps/ownership), so it died with exit 159. This extractor uses only the
         * calls the app already makes (write, mkdir, Os.symlink, chmod), all seccomp-allowed.
         */
        private fun extract(): Boolean {
            require(assetSha256() == ALPINE_SHA256) { "bundled Alpine image failed verification" }
            val parent = requireNotNull(rootfs.parentFile).apply { mkdirs() }
            val staging = File(parent, ".${rootfs.name}-${UUID.randomUUID()}.staging")
            val backup = File(parent, ".${rootfs.name}-${UUID.randomUUID()}.backup")
            staging.mkdirs()
            tmpDir.mkdirs()
            marker.delete()
            try {
                GZIPInputStream(assets.open(ALPINE_ASSET).buffered()).use { untar(it, staging) }
                require(File(staging, "bin/busybox").isFile) { "bundled Alpine image is incomplete" }
                if (rootfs.exists()) moveDirectory(rootfs, backup)
                try {
                    moveDirectory(staging, rootfs)
                    File(rootfs, "etc").mkdirs()
                    updateDns()
                    require(smokeTest()) { "bundled Alpine shell failed its readiness check" }
                    marker.writeText("ok")
                    marker.parentFile?.listFiles { file -> file.name.startsWith("${rootfs.name}-") && file.name.endsWith(".ready") && file != marker }
                        .orEmpty().forEach(File::delete)
                    backup.deleteRecursively()
                    return true
                } catch (error: Throwable) {
                    marker.delete()
                    rootfs.deleteRecursively()
                    if (backup.exists()) moveDirectory(backup, rootfs)
                    throw error
                }
            } finally {
                staging.deleteRecursively()
                if (ready()) backup.deleteRecursively()
            }
        }

        private fun smokeTest(): Boolean {
            val workspace = File(workspacesRoot, ".runtime-check-${UUID.randomUUID()}")
            if (!workspace.mkdirs()) return false
            return try {
                val process = ProcessBuilder(linuxShellArgv(proot, rootfs, tmpDir, workspacesRoot, workspace.absolutePath) +
                    "printf phonecode-ready")
                    .redirectErrorStream(true)
                    .apply { environment().putAll(env()) }
                    .start()
                if (!process.waitFor(15, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    false
                } else {
                    process.exitValue() == 0 && process.inputStream.bufferedReader().use { it.readText() } == "phonecode-ready"
                }
            } finally {
                workspace.deleteRecursively()
            }
        }

        private fun assetSha256(): String {
            val digest = MessageDigest.getInstance("SHA-256")
            assets.open(ALPINE_ASSET).use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        private fun moveDirectory(from: File, to: File) {
            runCatching {
                Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE)
            }.getOrElse {
                Files.move(from.toPath(), to.toPath())
            }
        }

        private fun updateDns() {
            val content = resolvConf(dnsServers())
            if (content.isNotEmpty()) File(rootfs, "etc/resolv.conf").writeText(content)
        }

        /**
         * Minimal ustar extractor: directories, regular files, symlinks. Skips hardlinks and device/fifo
         * nodes (the Alpine minirootfs has none, and proot binds the host /dev anyway). Only regular files
         * carry data blocks; every entry is padded to a 512-byte boundary.
         */
        private fun untar(input: InputStream, dest: File) {
            val header = ByteArray(512)
            val root = dest.canonicalFile
            var extractedBytes = 0L
            while (readFully(input, header)) {
                if (header.all { it.toInt() == 0 }) break // end-of-archive marker
                val name = cString(header, 0, 100)
                if (name.isEmpty()) continue
                val mode = octal(header, 100, 8)
                val size = octal(header, 124, 12)
                require(size in 0..MAX_ARCHIVE_ENTRY_BYTES) { "invalid Alpine archive entry size" }
                extractedBytes += size
                require(extractedBytes <= MAX_ARCHIVE_BYTES) { "bundled Alpine image is too large" }
                val type = header[156].toInt().toChar()
                val target = File(root, name).canonicalFile
                require(target.toPath().startsWith(root.toPath()) && (target != root || type == '5')) {
                    "Alpine archive entry escapes its root"
                }
                when (type) {
                    '5' -> target.mkdirs()
                    '2' -> { // symlink: target path is in the linkname field, not data
                        target.parentFile?.mkdirs()
                        if (target.exists() || Files.isSymbolicLink(target.toPath())) require(target.delete())
                        android.system.Os.symlink(cString(header, 157, 100), target.absolutePath)
                    }
                    '0', '\u0000' -> {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { copyN(input, it, size) }
                        skipN(input, padding(size))
                        if (mode and 0b001_000_000 != 0L) target.setExecutable(true, false)
                    }
                    else -> {
                        skipN(input, size)
                        skipN(input, padding(size))
                    }
                }
            }
        }

        private fun padding(size: Long): Long = (512 - (size % 512)) % 512

        private fun readFully(input: InputStream, buf: ByteArray): Boolean {
            var off = 0
            while (off < buf.size) {
                val n = input.read(buf, off, buf.size - off)
                if (n < 0) {
                    if (off == 0) return false
                    error("truncated Alpine archive header")
                }
                off += n
            }
            return true
        }

        private fun copyN(input: InputStream, out: java.io.OutputStream, n: Long) {
            val buf = ByteArray(64 * 1024)
            var left = n
            while (left > 0) {
                val r = input.read(buf, 0, minOf(left, buf.size.toLong()).toInt())
                if (r < 0) error("truncated Alpine archive entry")
                out.write(buf, 0, r)
                left -= r
            }
        }

        private fun skipN(input: InputStream, n: Long) {
            var left = n
            val buf = ByteArray(8 * 1024)
            while (left > 0) {
                val r = input.read(buf, 0, minOf(left, buf.size.toLong()).toInt())
                if (r < 0) error("truncated Alpine archive padding")
                left -= r
            }
        }

        private fun cString(b: ByteArray, off: Int, len: Int): String {
            var end = off
            while (end < off + len && b[end].toInt() != 0) end++
            return String(b, off, end - off, Charsets.UTF_8)
        }

        private fun octal(b: ByteArray, off: Int, len: Int): Long =
            cString(b, off, len).trim().takeIf { it.isNotEmpty() }?.toLongOrNull(8) ?: 0L

        fun shellArgv(workspacePath: String): List<String> {
            updateDns()
            return linuxShellArgv(proot, rootfs, tmpDir, workspacesRoot, workspacePath)
        }

        fun env(): Map<String, String> = mapOf(
            "PROOT_LOADER" to loader.absolutePath,
            "PROOT_TMP_DIR" to tmpDir.absolutePath,
            "HOME" to "/root",
            "PATH" to "/usr/bin:/bin:/usr/sbin:/sbin",
            "TERM" to "dumb",
            "LANG" to "C.UTF-8",
        )
    }
}

internal fun linuxTreeReady(marker: File, rootfs: File): Boolean {
    val shell = File(rootfs, "bin/sh")
    return marker.isFile &&
        File(rootfs, "bin/busybox").isFile &&
        (shell.isFile || Files.isSymbolicLink(shell.toPath()))
}

internal fun migrateLinuxMarker(marker: File, legacyMarker: File, rootfs: File): Boolean {
    if (linuxTreeReady(marker, rootfs)) return true
    if (!linuxTreeReady(legacyMarker, rootfs)) return false
    return runCatching {
        marker.writeText("ok")
        check(marker.isFile)
        legacyMarker.delete()
        true
    }.getOrDefault(false)
}

internal fun resolvConf(servers: List<String>): String =
    servers.distinct().joinToString("") { "nameserver $it\n" }

internal fun linuxShellArgv(
    proot: File,
    rootfs: File,
    tmpDir: File,
    workspacesRoot: File,
    workspacePath: String,
): List<String> {
    val requested = File(workspacePath)
    require(requested.isAbsolute) { "workspace path must be absolute" }
    val root = workspacesRoot.canonicalFile
    val workspace = requested.canonicalFile
    require(workspace.isDirectory) { "workspace is not a directory" }
    require(workspace.path.startsWith(root.path + File.separator)) { "workspace is outside the workspace root" }
    require(':' !in workspace.path) { "workspace path cannot contain ':'" }
    val guestWorkspace = File(rootfs, "workspace")
    check(guestWorkspace.isDirectory || guestWorkspace.mkdirs()) { "guest workspace is unavailable" }
    check(tmpDir.isDirectory || tmpDir.mkdirs()) { "guest temporary directory is unavailable" }
    return listOf(
        proot.absolutePath,
        "-r", rootfs.canonicalPath,
        "-0",
        "-b", "/dev",
        "-b", "/proc",
        "-b", "${tmpDir.canonicalPath}:/tmp",
        "-b", "${workspace.path}:/workspace",
        "-w", "/workspace",
        "/bin/sh", "-c",
    )
}
