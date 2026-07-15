package dev.phonecode.app.data

import android.content.res.AssetManager
import dev.phonecode.tools.mcp.McpConfig
import dev.phonecode.tools.mcp.McpServerConfig
import dev.phonecode.tools.mcp.decodeMcpConfig
import dev.phonecode.tools.mcp.serialize
import dev.phonecode.tools.skills.SkillManifest
import dev.phonecode.tools.skills.parseSkillMarkdown
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.security.MessageDigest
import java.util.UUID

sealed interface McpConfigLoad {
    data class Ready(val config: McpConfig) : McpConfigLoad
    data class Invalid(val raw: String, val message: String) : McpConfigLoad
}

enum class SkillScope { PROJECT, GLOBAL }

enum class SkillStatus { ACTIVE, DISABLED, SHADOWED, INVALID }

data class ManagedSkill(
    val id: String,
    val name: String,
    val manifest: SkillManifest?,
    val location: String,
    val scope: SkillScope,
    val status: SkillStatus,
    val issue: String? = null,
)

data class SkillInventory(val items: List<ManagedSkill>) {
    val active: List<SkillManifest> = items.mapNotNull { item ->
        item.manifest?.takeIf { item.status == SkillStatus.ACTIVE }
    }
}

data class RuntimeConfigFingerprint(val mcp: String, val skills: String)

fun isSafeMcpEndpoint(value: String): Boolean {
    val uri = runCatching { java.net.URI(value) }.getOrNull() ?: return false
    val host = uri.host?.lowercase()?.removePrefix("[")?.removeSuffix("]") ?: return false
    if (uri.userInfo != null || host.isBlank()) return false
    return uri.scheme.equals("https", true) || uri.scheme.equals("http", true) &&
        host in setOf("localhost", "127.0.0.1", "::1")
}

class InvalidMcpConfigException(message: String) : IllegalStateException(message)

class McpSkillRepository(
    private val configDir: File,
    private val secrets: SecretValueStore? = null,
    private val mcpFileWriter: (File, String) -> Unit = { file, text -> file.writeTextAtomically(text) },
) {
    private val mcpFile = File(configDir, "opencode.json")
    private val mcpSecretStateFile = File(configDir, ".mcp-secret-state.json")
    private val skillStateFile = File(configDir, "skills-state.json")

    @Synchronized
    fun loadMcpConfigState(): McpConfigLoad {
        if (!mcpFile.exists()) return McpConfigLoad.Ready(McpConfig())
        val raw = runCatching { mcpFile.readText() }.getOrElse {
            return McpConfigLoad.Invalid("", "MCP configuration could not be read")
        }
        return runCatching {
            val stored = storeJson.decodeFromString(StoredMcpConfig.serializer(), raw)
            val config = validateMcpConfig(McpConfig(stored.mcp))
            McpConfigLoad.Ready(hydrateMcpHeaders(config, stored.secretState ?: loadMcpSecretState()))
        }
            .getOrElse { McpConfigLoad.Invalid(raw, it.message ?: "MCP configuration is invalid") }
    }

    fun loadMcpConfig(): McpConfig = when (val loaded = loadMcpConfigState()) {
        is McpConfigLoad.Ready -> loaded.config
        is McpConfigLoad.Invalid -> throw InvalidMcpConfigException(loaded.message)
    }

    @Synchronized
    fun saveMcpConfig(config: McpConfig): Result<Unit> = runCatching {
        requireValidMcpConfig()
        val validated = validateMcpConfig(config)
        configDir.mkdirs()
        persistMcpConfig(validated)
    }

    @Synchronized
    fun replaceMcpConfig(raw: String): Result<McpConfig> = runCatching {
        require(raw.toByteArray().size <= MAX_MCP_CONFIG_BYTES) { "MCP configuration is too large" }
        val config = validateMcpConfig(decodeMcpConfig(raw))
        configDir.mkdirs()
        persistMcpConfig(config)
        config
    }

    fun upsertMcpServer(
        originalName: String? = null,
        name: String,
        server: McpServerConfig,
        expectedServer: McpServerConfig? = null,
    ): Result<McpConfig> = mutateMcpConfig { current ->
        val finalName = name.trim()
        require(finalName.isNotEmpty()) { "Server name is required" }
        if (originalName != null) require(originalName in current.mcp) { "Server does not exist" }
        if (expectedServer != null) {
            require(current.mcp[originalName ?: finalName] == expectedServer) { "Server changed outside this editor; reload it first" }
        }
        require(finalName == originalName || finalName !in current.mcp) { "Server name already exists" }
        val servers = current.mcp.toMutableMap()
        if (originalName != null && originalName != finalName) servers.remove(originalName)
        servers[finalName] = server
        McpConfig(servers)
    }

    fun removeMcpServer(name: String): Result<McpConfig> = mutateMcpConfig { current ->
        require(name in current.mcp) { "Server does not exist" }
        McpConfig(current.mcp - name)
    }

    fun setMcpEnabled(name: String, enabled: Boolean): Result<McpConfig> = mutateMcpConfig { current ->
        val server = current.mcp[name] ?: error("Server does not exist")
        McpConfig(current.mcp + (name to server.copy(enabled = enabled)))
    }

    fun runtimeFingerprint(projectDir: File? = null): RuntimeConfigFingerprint = RuntimeConfigFingerprint(
        mcp = fingerprint(listOf(mcpFile, mcpSecretStateFile)),
        skills = fingerprint(buildList {
            add(skillStateFile)
            skillRootCandidates(projectDir).forEach { root ->
                add(root.file)
                root.file.listFiles().orEmpty().filter { it.isDirectory }.forEach { directory ->
                    add(directory)
                    add(File(directory, "SKILL.md"))
                }
            }
        }),
    )

    fun watchedDirectories(projectDir: File? = null): List<File> = buildSet {
        listOfNotNull(configDir, projectDir).mapNotNull(::canonicalDirectory).forEach(::add)
        skillRootCandidates(projectDir).forEach { root ->
            generateSequence(root.file.parentFile) { parent ->
                parent.parentFile?.takeUnless { parent == configDir || parent == projectDir }
            }.take(2).mapNotNull(::canonicalDirectory).forEach(::add)
            canonicalDirectory(root.file)?.let { directory ->
                add(directory)
                directory.listFiles().orEmpty().filter { it.isDirectory }.mapNotNull(::canonicalDirectory).forEach(::add)
            }
        }
    }.toList()

    fun readSkillFile(
        scope: SkillScope,
        name: String,
        path: String = "SKILL.md",
        projectDir: File? = null,
    ): Result<String> = runCatching {
        val file = resolveEditableSkillFile(scope, name, path, projectDir, createRoot = false)
        require(file.isFile && file.length() <= MAX_SKILL_RESOURCE_BYTES) { "Skill file is unavailable" }
        val bytes = file.readBytes()
        require(0.toByte() !in bytes) { "Skill file is not readable text" }
        bytes.toString(Charsets.UTF_8)
    }

    fun readSkill(id: String, projectDir: File? = null): Result<String> = runCatching {
        val item = scanSkills(projectDir).items.firstOrNull { it.id == id } ?: error("Skill does not exist")
        val file = File(item.location).canonicalFile
        require(file.isFile && file.length() <= MAX_SKILL_RESOURCE_BYTES) { "Skill file is unavailable" }
        file.readText()
    }

    @Synchronized
    fun writeSkillFile(
        scope: SkillScope,
        name: String,
        path: String = "SKILL.md",
        content: String,
        projectDir: File? = null,
    ): Result<Unit> = runCatching {
        require(content.toByteArray().size <= MAX_SKILL_RESOURCE_BYTES) { "Skill file is too large" }
        require('\u0000' !in content) { "Skill file must be text" }
        if (path == "SKILL.md") {
            val manifest = parseSkillMarkdown(content) ?: error("Invalid SKILL.md frontmatter")
            require(manifest.name == name) { "Skill name must match its folder" }
        }
        val file = resolveEditableSkillFile(scope, name, path, projectDir, createRoot = true)
        file.writeTextAtomically(content)
    }

    @Synchronized
    fun writeSkill(
        id: String,
        content: String,
        projectDir: File? = null,
        expectedContent: String? = null,
    ): Result<Unit> = runCatching {
        require(content.toByteArray().size <= MAX_SKILL_RESOURCE_BYTES) { "Skill file is too large" }
        require('\u0000' !in content) { "Skill file must be text" }
        val item = scanSkills(projectDir).items.firstOrNull { it.id == id } ?: error("Skill does not exist")
        val manifest = parseSkillMarkdown(content) ?: error("Invalid SKILL.md frontmatter")
        require(manifest.name == item.name) { "Rename a skill by creating a new one" }
        val file = File(item.location).canonicalFile
        require(file.absolutePath == id && file.isFile) { "Skill file is unavailable" }
        if (expectedContent != null) require(file.readText() == expectedContent) { "Skill changed outside this editor; reload it first" }
        file.writeTextAtomically(content)
    }

    @Synchronized
    fun deleteEditableSkill(scope: SkillScope, name: String, projectDir: File? = null): Result<Unit> = runCatching {
        val directory = editableSkillRoot(scope, projectDir, create = false).resolve(name).canonicalFile
        require(directory.parentFile == editableSkillRoot(scope, projectDir, create = false)) { "Skill is outside its managed root" }
        require(directory.isDirectory) { "Skill does not exist" }
        check(directory.deleteRecursively()) { "Skill could not be deleted" }
        val id = File(directory, "SKILL.md").canonicalPath
        saveSkillState(SkillState(loadSkillState().disabled - id))
    }

    fun seedBundledSkills(assets: AssetManager) {
        val marker = File(configDir, ".bundled-skills")
        val seeded = runCatching { marker.readLines().toSet() }.getOrDefault(emptySet()).toMutableSet()
        val bundled = runCatching { assets.list("skills").orEmpty().sorted() }.getOrDefault(emptyList())
        bundled.filterNot(seeded::contains).forEach { name ->
            runCatching {
                val target = File(configDir, "skills/$name/SKILL.md")
                if (!target.exists()) {
                    target.parentFile?.mkdirs()
                    assets.open("skills/$name/SKILL.md").use { input -> target.writeBytesAtomically(input.readBytes()) }
                }
                seeded += name
            }
        }
        configDir.mkdirs()
        runCatching { marker.writeTextAtomically(seeded.sorted().joinToString("\n", postfix = "\n")) }
    }

    fun scanSkills(projectDir: File? = null): SkillInventory {
        val disabled = loadSkillState().disabled
        val candidates = skillRoots(projectDir).flatMap { root -> scanRoot(root, disabled) }
        val activeNames = mutableSetOf<String>()
        val items = candidates.map { candidate ->
            when {
                candidate.status == SkillStatus.INVALID -> candidate
                candidate.status == SkillStatus.DISABLED -> candidate
                activeNames.add(candidate.name) -> candidate.copy(status = SkillStatus.ACTIVE)
                else -> candidate.copy(status = SkillStatus.SHADOWED)
            }
        }
        return SkillInventory(items)
    }

    fun discoverSkills(projectDir: File? = null): List<SkillManifest> = scanSkills(projectDir).active

    @Synchronized
    fun setSkillEnabled(id: String, enabled: Boolean, projectDir: File? = null): Result<Unit> = runCatching {
        val item = scanSkills(projectDir).items.firstOrNull { it.id == id } ?: error("Skill does not exist")
        require(item.manifest != null) { "Invalid skills cannot be enabled" }
        val disabled = loadSkillState().disabled.toMutableSet()
        if (enabled) disabled.remove(id) else disabled.add(id)
        saveSkillState(SkillState(disabled))
    }

    @Synchronized
    fun deleteSkill(id: String, projectDir: File? = null): Result<Unit> = runCatching {
        val roots = skillRoots(projectDir)
        val item = scanSkills(projectDir).items.firstOrNull { it.id == id } ?: error("Skill does not exist")
        val directory = File(item.location).parentFile?.canonicalFile ?: error("Skill location is invalid")
        val root = roots.firstOrNull { directory.toPath().startsWith(it.file.toPath()) }
            ?: error("Skill is outside managed roots")
        require(directory.parentFile == root.file) { "Skill is outside managed roots" }
        check(directory.deleteRecursively()) { "Skill could not be deleted" }
        val disabled = loadSkillState().disabled - id
        saveSkillState(SkillState(disabled))
    }

    @Synchronized
    private fun mutateMcpConfig(transform: (McpConfig) -> McpConfig): Result<McpConfig> = runCatching {
        val updated = validateMcpConfig(transform(requireValidMcpConfig()))
        configDir.mkdirs()
        persistMcpConfig(updated)
        updated
    }

    private fun hydrateMcpHeaders(config: McpConfig, state: McpSecretState): McpConfig {
        val store = secrets ?: return config
        val plaintext = config.mcp.filterValues { it.headers.isNotEmpty() }
        if ((state.names.isNotEmpty() || plaintext.isNotEmpty()) && !store.available) {
            error("Secure storage is unavailable; MCP credentials are locked")
        }
        val hydrated = McpConfig(config.mcp.mapValues { (name, server) ->
            when {
                server.headers.isNotEmpty() -> server
                name in state.names -> {
                    val raw = store.get(mcpSecretKey(name, state)) ?: error("Encrypted MCP credentials are unavailable")
                    val headers = storeJson.decodeFromString(StoredMcpHeaders.serializer(), raw).values
                    server.copy(headers = headers)
                }
                else -> server
            }
        })
        if (plaintext.isNotEmpty() || state.names.isNotEmpty() && state.version < MCP_SECRET_STATE_VERSION) {
            persistMcpConfig(hydrated)
        }
        return hydrated
    }

    private fun persistMcpConfig(config: McpConfig) {
        val store = secrets
        if (store == null) {
            mcpFileWriter(mcpFile, config.serialize())
            return
        }
        val secured = config.mcp.filterValues { it.headers.isNotEmpty() }
        val previous = currentMcpSecretState()
        if ((secured.isNotEmpty() || previous.names.isNotEmpty()) && !store.available) {
            error("Secure storage is unavailable; MCP credentials cannot be changed")
        }
        val next = McpSecretState(
            names = secured.keys,
            revision = if (secured.isEmpty()) "" else UUID.randomUUID().toString(),
            version = MCP_SECRET_STATE_VERSION,
        )
        val sanitized = McpConfig(config.mcp.mapValues { (_, server) -> server.copy(headers = emptyMap()) })
        val encoded = storeJson.encodeToString(
            StoredMcpConfig.serializer(),
            StoredMcpConfig(sanitized.mcp, next),
        )
        require(encoded.toByteArray().size <= MAX_MCP_CONFIG_BYTES) { "MCP configuration is too large" }
        try {
            secured.forEach { (name, server) ->
                store.put(
                    mcpSecretKey(name, next),
                    storeJson.encodeToString(StoredMcpHeaders.serializer(), StoredMcpHeaders(server.headers)),
                )
            }
        } catch (failure: Throwable) {
            clearMcpSecrets(store, next)
            throw failure
        }
        try {
            mcpFileWriter(mcpFile, encoded)
        } catch (failure: Throwable) {
            if (mcpFile.snapshotText() != encoded) {
                clearMcpSecrets(store, next)
                throw failure
            }
        }
        runCatching { saveMcpSecretState(next) }
        clearMcpSecrets(store, previous)
    }

    private fun File.snapshotText(): String? = if (exists()) readText() else null

    private fun currentMcpSecretState(): McpSecretState {
        val embedded = mcpFile.snapshotText()?.let { raw ->
            runCatching { storeJson.decodeFromString(StoredMcpConfig.serializer(), raw).secretState }.getOrNull()
        }
        return embedded ?: loadMcpSecretState()
    }

    private fun clearMcpSecrets(store: SecretValueStore, state: McpSecretState) {
        state.names.forEach { name -> runCatching { store.put(mcpSecretKey(name, state), "") } }
    }

    private fun mcpSecretKey(name: String, state: McpSecretState): String =
        if (state.version >= MCP_SECRET_STATE_VERSION && state.revision.isNotBlank()) {
            "mcp.headers.${state.revision}.$name"
        } else {
            "mcp.headers.$name"
        }

    private fun loadMcpSecretState(): McpSecretState = if (mcpSecretStateFile.isFile) {
        try {
            storeJson.decodeFromString(McpSecretState.serializer(), mcpSecretStateFile.readText())
        } catch (error: Exception) {
            throw InvalidMcpConfigException("MCP credential metadata is invalid")
        }
    } else {
        McpSecretState()
    }

    private fun saveMcpSecretState(state: McpSecretState) {
        mcpFileWriter(mcpSecretStateFile, storeJson.encodeToString(McpSecretState.serializer(), state))
    }

    private fun requireValidMcpConfig(): McpConfig = when (val loaded = loadMcpConfigState()) {
        is McpConfigLoad.Ready -> loaded.config
        is McpConfigLoad.Invalid -> throw InvalidMcpConfigException(loaded.message)
    }

    private fun validateMcpConfig(config: McpConfig): McpConfig {
        require(config.mcp.size <= MAX_MCP_SERVERS) { "Too many MCP servers" }
        config.mcp.forEach { (name, server) ->
            require(name.length <= 80 && MCP_NAME.matches(name)) { "Invalid MCP server name" }
            require(server.type == "remote") { "Only remote MCP servers are supported" }
            require(isSafeMcpEndpoint(server.url)) { "MCP URL must use HTTPS, or HTTP only for localhost" }
            require(server.timeout in 1_000L..60_000L) { "MCP timeout is out of range" }
            require(server.headers.size <= 32) { "Too many MCP headers" }
            server.headers.forEach { (key, value) ->
                require(key.matches(HEADER_NAME) && value.length <= 8_192 && '\r' !in value && '\n' !in value) { "Invalid MCP header" }
            }
        }
        require(config.serialize().toByteArray().size <= MAX_MCP_CONFIG_BYTES) { "MCP configuration is too large" }
        return config
    }

    private fun skillRoots(projectDir: File?): List<SkillRoot> = skillRootCandidates(projectDir).mapNotNull { root ->
        canonicalDirectory(root.file)?.let { root.copy(file = it) }
    }

    private fun skillRootCandidates(projectDir: File?): List<SkillRoot> = buildList {
        projectDir?.let { project ->
            listOf(".agents/skills", ".opencode/skills", ".claude/skills", ".codex/skills")
                .forEach { add(SkillRoot(File(project, it), SkillScope.PROJECT)) }
        }
        listOf(".agents/skills", "skills", ".opencode/skills", ".claude/skills", ".codex/skills")
            .forEach { add(SkillRoot(File(configDir, it), SkillScope.GLOBAL)) }
    }

    private fun scanRoot(root: SkillRoot, disabled: Set<String>): List<ManagedSkill> =
        root.file.listFiles().orEmpty().filter { it.isDirectory }.sortedBy { it.name }.mapNotNull { directory ->
            val canonicalDirectory = runCatching { directory.canonicalFile }.getOrNull() ?: return@mapNotNull null
            if (canonicalDirectory.parentFile != root.file) return@mapNotNull null
            val skillFile = File(canonicalDirectory, "SKILL.md")
            val id = runCatching { skillFile.canonicalPath }.getOrDefault(skillFile.absolutePath)
            val location = skillFile.absolutePath
            if (!skillFile.isFile) {
                return@mapNotNull ManagedSkill(id, canonicalDirectory.name, null, location, root.scope, SkillStatus.INVALID, "Missing SKILL.md")
            }
            val canonicalSkill = runCatching { skillFile.canonicalFile }.getOrNull()
            if (canonicalSkill == null || canonicalSkill.parentFile != canonicalDirectory) {
                return@mapNotNull ManagedSkill(id, canonicalDirectory.name, null, location, root.scope, SkillStatus.INVALID, "SKILL.md points outside its skill directory")
            }
            if (canonicalSkill.length() > MAX_SKILL_BYTES) {
                return@mapNotNull ManagedSkill(id, canonicalDirectory.name, null, location, root.scope, SkillStatus.INVALID, "SKILL.md is too large")
            }
            val manifest = runCatching { parseSkillMarkdown(canonicalSkill.readText(), canonicalSkill.absolutePath) }.getOrNull()
                ?: return@mapNotNull ManagedSkill(id, canonicalDirectory.name, null, location, root.scope, SkillStatus.INVALID, "Invalid SKILL.md frontmatter")
            if (manifest.name != canonicalDirectory.name) {
                return@mapNotNull ManagedSkill(id, manifest.name, manifest, location, root.scope, SkillStatus.INVALID, "Skill name must match its folder")
            }
            val status = if (id in disabled) SkillStatus.DISABLED else SkillStatus.ACTIVE
            ManagedSkill(id, manifest.name, manifest, location, root.scope, status)
        }

    private fun canonicalDirectory(file: File): File? = runCatching { file.canonicalFile }.getOrNull()?.takeIf { it.isDirectory }

    private fun loadSkillState(): SkillState = if (skillStateFile.isFile) {
        runCatching { storeJson.decodeFromString(SkillState.serializer(), skillStateFile.readText()) }.getOrDefault(SkillState())
    } else {
        SkillState()
    }

    private fun saveSkillState(state: SkillState) {
        skillStateFile.writeTextAtomically(storeJson.encodeToString(SkillState.serializer(), state))
    }

    private fun editableSkillRoot(scope: SkillScope, projectDir: File?, create: Boolean): File {
        val root = when (scope) {
            SkillScope.GLOBAL -> File(configDir, "skills")
            SkillScope.PROJECT -> File(requireNotNull(projectDir) { "A project is required" }, ".agents/skills")
        }
        if (create) root.mkdirs()
        return root.canonicalFile
    }

    private fun resolveEditableSkillFile(
        scope: SkillScope,
        name: String,
        path: String,
        projectDir: File?,
        createRoot: Boolean,
    ): File {
        require(SKILL_NAME.matches(name)) { "Invalid skill name" }
        require(path.isNotBlank() && !path.startsWith('/') && '\\' !in path && ':' !in path) { "Invalid skill path" }
        val root = editableSkillRoot(scope, projectDir, createRoot)
        val directory = File(root, name).canonicalFile
        require(directory.parentFile == root) { "Skill is outside its managed root" }
        if (createRoot) directory.mkdirs()
        val file = File(directory, path).canonicalFile
        require(file.toPath().startsWith(directory.toPath()) && file != directory) { "Skill file is outside its directory" }
        return file
    }

    private fun fingerprint(files: List<File>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        files.distinctBy { it.absolutePath }.sortedBy { it.absolutePath }.forEach { file ->
            digest.update(file.absolutePath.toByteArray())
            when {
                file.isDirectory -> digest.update(1.toByte())
                file.isFile && file.length() <= MAX_FINGERPRINT_BYTES -> file.inputStream().use { input ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                    }
                }
                file.isFile -> digest.update("${file.length()}:${file.lastModified()}".toByteArray())
                else -> digest.update(0.toByte())
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    @Serializable
    private data class SkillState(val disabled: Set<String> = emptySet())

    @Serializable
    private data class StoredMcpHeaders(val values: Map<String, String>)

    @Serializable
    private data class StoredMcpConfig(
        val mcp: Map<String, McpServerConfig> = emptyMap(),
        @SerialName("_phonecodeMcpSecrets") val secretState: McpSecretState? = null,
    )

    @Serializable
    private data class McpSecretState(
        val names: Set<String> = emptySet(),
        val revision: String = "",
        val version: Int = 1,
    )

    private data class SkillRoot(val file: File, val scope: SkillScope)

    private companion object {
        const val MAX_SKILL_BYTES = 512L * 1024L
        const val MAX_SKILL_RESOURCE_BYTES = 512L * 1024L
        const val MAX_FINGERPRINT_BYTES = 1024L * 1024L
        const val MAX_MCP_CONFIG_BYTES = 1024L * 1024L
        const val MAX_MCP_SERVERS = 20
        const val MCP_SECRET_STATE_VERSION = 2
        val SKILL_NAME = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")
        val MCP_NAME = Regex("^[a-zA-Z0-9][a-zA-Z0-9 ._-]*$")
        val HEADER_NAME = Regex("^[!#$%&'*+.^_`|~0-9A-Za-z-]+$")
    }
}
