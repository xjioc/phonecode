package dev.phonecode.tools.shell

import dev.phonecode.tools.ToolContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class ShellToolTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val context get() = object : ToolContext {
        override val workspacePath: String get() = tmp.root.absolutePath
        override suspend fun requestPermission(tool: String, summary: String) = true
    }

    /** Host shell: the tool's default targets Android's /system/bin/sh; tests use the JVM host's. */
    private fun hostShell(): List<String> =
        if (System.getProperty("os.name").lowercase().contains("win")) listOf("cmd.exe", "/c")
        else listOf("/bin/sh", "-c")

    private fun args(command: String, timeoutS: Int? = null) = buildJsonObject {
        put("command", command)
        timeoutS?.let { put("timeout_s", it) }
    }

    private fun backgroundArgs(command: String) = buildJsonObject {
        put("command", command)
        put("background", true)
    }

    private fun isAlive(pid: Long) = ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)

    private suspend fun awaitStopped(vararg pids: Long) {
        withTimeout(5_000) {
            while (pids.any(::isAlive)) delay(20)
        }
    }

    private suspend fun assertConcurrentStopCatchesStart(stop: suspend (ProcessManager) -> Unit) = coroutineScope {
        val shellEntered = CountDownLatch(1)
        val allowLaunch = CountDownLatch(1)
        val starts = AtomicInteger()
        val stops = AtomicInteger()
        val stoppingThread = AtomicReference<Thread?>()
        val manager = ProcessManager(
            shellProvider = {
                shellEntered.countDown()
                assertTrue(allowLaunch.await(5, TimeUnit.SECONDS))
                hostShell()
            },
            onStarted = { starts.incrementAndGet() },
            onStopped = { stops.incrementAndGet() },
        )
        val started = async(Dispatchers.IO) {
            manager.start("exec sleep 30", context.workspacePath)
        }
        assertTrue(shellEntered.await(5, TimeUnit.SECONDS))
        val stopping = async(Dispatchers.IO) {
            stoppingThread.set(Thread.currentThread())
            stop(manager)
        }
        try {
            withTimeout(5_000) {
                while (stoppingThread.get()?.state != Thread.State.BLOCKED) delay(5)
            }
        } finally {
            allowLaunch.countDown()
        }

        val result = started.await()
        stopping.await()
        val id = Regex("proc-\\d+").find(result.output)?.value ?: error(result.output)

        assertTrue(manager.output(id).output.contains("stopped"))
        assertEquals(1, starts.get())
        assertEquals(1, stops.get())
    }

    @Test fun runsACommandInTheWorkspace() = runBlocking {
        tmp.newFile("hello.txt")
        val result = ShellTool({ hostShell() }).execute(args("dir") , context).let {
            // "dir" works on cmd; on sh use ls
            if (it.isError) ShellTool({ hostShell() }).execute(args("ls"), context) else it
        }
        assertFalse(result.output, result.isError)
        assertTrue(result.output, result.output.contains("hello.txt"))
    }

    @Test fun nonZeroExitIsAnError() = runBlocking {
        val result = ShellTool({ hostShell() }).execute(args("exit 3"), context)
        assertTrue(result.isError)
        assertTrue(result.output, result.output.contains("exit code 3"))
    }

    @Test fun missingCommandIsAnError() = runBlocking {
        val result = ShellTool({ hostShell() }).execute(buildJsonObject { }, context)
        assertTrue(result.isError)
    }

    @Test fun refusesToUseAHostShellWhenAlpineIsUnavailable() = runBlocking {
        val result = ShellTool().execute(args("echo unsafe"), context)

        assertTrue(result.isError)
        assertEquals("bash: bundled Alpine environment is not ready", result.output)
    }

    @Test fun cancellationStopsTheForegroundProcess() = runBlocking {
        assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
        val pidFile = File(tmp.root, "foreground.pid")
        val childFile = File(tmp.root, "foreground-child.pid")
        val running = async {
            ShellTool({ hostShell() }).execute(
                args("echo \$\$ > foreground.pid; sleep 30 & child=\$!; echo \$child > foreground-child.pid; wait \$child"),
                context,
            )
        }
        withTimeout(5_000) {
            while (!pidFile.isFile || !childFile.isFile) delay(20)
        }
        val pids = longArrayOf(pidFile.readText().trim().toLong(), childFile.readText().trim().toLong())

        running.cancelAndJoin()

        awaitStopped(*pids)
        assertTrue(pids.none(::isAlive))
    }

    @Test fun timeoutStopsForegroundDescendants() = runBlocking {
        assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
        val childFile = File(tmp.root, "timeout-child.pid")

        val result = ShellTool({ hostShell() }).execute(
            args("sleep 30 & child=\$!; echo \$child > timeout-child.pid; wait \$child", 1),
            context,
        )
        val pid = childFile.readText().trim().toLong()

        assertTrue(result.output, result.isError)
        assertTrue(result.output, result.output.contains("killed after 1s timeout"))
        awaitStopped(pid)
        assertFalse(isAlive(pid))
    }

    @Test fun injectedEnvironmentReachesTheProcess() = runBlocking {
        val isWin = System.getProperty("os.name").lowercase().contains("win")
        val echo = if (isWin) "echo %PC_TEST%" else "echo \$PC_TEST"
        val result = ShellTool({ hostShell() }, { mapOf("PC_TEST" to "phonecode-env") }).execute(args(echo), context)
        assertFalse(result.output, result.isError)
        assertTrue(result.output, result.output.contains("phonecode-env"))
    }

    @Test fun schemaRequiresCommand() {
        val schema = ShellTool().parameters.toString()
        assertTrue(schema, schema.contains("\"required\":[\"command\"]"))
        assertTrue(schema, schema.contains("\"background\""))
        assertEquals("bash", ShellTool().name)
        assertTrue(ShellTool().mutating)
    }

    @Test fun managesBackgroundCommands() = runBlocking {
        assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
        val manager = ProcessManager({ hostShell() })
        val tool = ShellTool({ hostShell() }, processManager = manager)
        val started = tool.execute(backgroundArgs("printf ready; exec sleep 30"), context)

        assertFalse(started.output, started.isError)
        val id = Regex("proc-\\d+").find(started.output)!!.value
        assertTrue(manager.output(id).output.contains("ready"))
        assertTrue(manager.list().output.contains(id))
        assertFalse(manager.stop(id).isError)
        assertTrue(manager.output(id).output.contains("exited"))
    }

    @Test fun reportsBackgroundStartupFailure() = runBlocking {
        assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
        val manager = ProcessManager({ hostShell() })
        val result = ShellTool({ hostShell() }, processManager = manager)
            .execute(backgroundArgs("exit 7"), context)

        assertTrue(result.output, result.isError)
        assertTrue(result.output, result.output.contains("exited (7)"))
    }

    @Test fun fastSuccessfulBackgroundCommandIsNotAnError() = runBlocking {
        assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
        val manager = ProcessManager({ hostShell() })

        val result = manager.start("printf done", context.workspacePath)

        assertFalse(result.output, result.isError)
        assertTrue(result.output.contains("exited (0)"))
    }

    @Test fun processInspectionIsReadOnlyAndScopedToTheWorkspace() = runBlocking {
        assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
        val manager = ProcessManager({ hostShell() })
        val tool = ProcessTool(manager)
        val started = manager.start("exec sleep 30", context.workspacePath)
        val id = Regex("proc-\\d+").find(started.output)!!.value
        val other = tmp.newFolder("other").absolutePath

        assertFalse(tool.mutates(buildJsonObject { put("action", "list") }))
        assertFalse(tool.mutates(buildJsonObject { put("action", "output"); put("session_id", id) }))
        assertTrue(tool.mutates(buildJsonObject { put("action", "stop"); put("session_id", id) }))
        assertEquals("No managed background processes.", manager.list(other).output)
        assertTrue(manager.output(id, other).isError)
        assertFalse(manager.stop(id).isError)
    }

    @Test fun sendsInputToManagedCommands() = runBlocking {
        assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
        val manager = ProcessManager({ hostShell() })
        val started = manager.start("read value; printf 'received:%s\\n' \"\$value\"; exec sleep 30", context.workspacePath)
        val id = Regex("proc-\\d+").find(started.output)!!.value

        assertFalse(manager.input(id, "hello").isError)
        repeat(20) {
            if (manager.output(id).output.contains("received:hello")) return@repeat
            kotlinx.coroutines.delay(25)
        }
        assertTrue(manager.output(id).output, manager.output(id).output.contains("received:hello"))
        assertFalse(manager.stop(id).isError)
    }

    @Test fun reportsProcessesInterruptedByAppDeath() = runBlocking {
        assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
        val storage = tmp.newFolder("process-records")
        val manager = ProcessManager({ hostShell() }, storageDirectory = storage)
        val started = manager.start("printf ready; exec sleep 30", context.workspacePath)
        val id = Regex("proc-\\d+").find(started.output)!!.value

        val restored = ProcessManager({ hostShell() }, storageDirectory = storage)

        assertTrue(restored.output(id).output, restored.output(id).output.contains("interrupted"))
        assertTrue(restored.output(id).output, restored.output(id).output.contains("ready"))
        assertFalse(manager.stop(id).isError)
    }

    @Test fun stopAllStopsEveryRunningProcess() = runBlocking {
        assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
        val stopped = mutableListOf<String>()
        val manager = ProcessManager({ hostShell() }, onStopped = stopped::add)
        val first = manager.start("exec sleep 30", context.workspacePath)
        val second = manager.start("exec sleep 30", context.workspacePath)
        val ids = listOf(first, second).map { Regex("proc-\\d+").find(it.output)!!.value }

        manager.stopAll()

        assertTrue(ids.all { manager.output(it).output.contains("stopped") })
        assertEquals(ids.toSet(), stopped.toSet())
    }

    @Test fun concurrentStopAllCannotMissAStartingProcess() = runBlocking {
        assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
        assertConcurrentStopCatchesStart { it.stopAll() }
    }

    @Test fun concurrentWorkspaceStopCannotMissAStartingProcess() = runBlocking {
        assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
        assertConcurrentStopCatchesStart { it.stopWorkspace(context.workspacePath) }
    }

    @Test fun limitsLiveBackgroundProcessesBeforeLaunchingAnother() = runBlocking {
        assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
        val launches = AtomicInteger()
        val manager = ProcessManager({ launches.incrementAndGet(); hostShell() })

        try {
            val results = List(8) { async { manager.start("exec sleep 30", context.workspacePath) } }.awaitAll()

            assertEquals(4, results.count { !it.isError })
            assertEquals(4, results.count { it.isError })
            assertTrue(results.filter { it.isError }.all { it.output.contains("background process limit reached (4)") })
            assertEquals(4, launches.get())

            val firstId = Regex("proc-\\d+").find(results.first { !it.isError }.output)!!.value
            assertFalse(manager.stop(firstId).isError)
            assertFalse(manager.start("exec sleep 30", context.workspacePath).isError)
            assertEquals(5, launches.get())
        } finally {
            manager.stopAll()
        }
    }

    @Test fun stopsOnlyProcessesInTheRequestedWorkspace() = runBlocking {
        assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
        val manager = ProcessManager({ hostShell() })
        val firstWorkspace = tmp.newFolder("first-workspace")
        val secondWorkspace = tmp.newFolder("second-workspace")
        val first = manager.start("exec sleep 30", firstWorkspace.absolutePath)
        val second = manager.start("exec sleep 30", firstWorkspace.absolutePath)
        val other = manager.start("exec sleep 30", secondWorkspace.absolutePath)
        val firstIds = listOf(first, second).map { Regex("proc-\\d+").find(it.output)!!.value }
        val otherId = Regex("proc-\\d+").find(other.output)!!.value

        manager.stopWorkspace(File(firstWorkspace, ".").path)

        assertTrue(firstIds.all { manager.output(it).output.contains("stopped") })
        assertTrue(manager.output(otherId).output.contains("running"))
        assertFalse(manager.stop(otherId).isError)
    }

    @Test fun stoppingManagedProcessStopsItsDescendants() = runBlocking {
        assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
        val childFile = File(tmp.root, "managed-child.pid")
        val manager = ProcessManager({ hostShell() })
        val started = manager.start(
            "sleep 30 & child=\$!; echo \$child > managed-child.pid; wait \$child",
            context.workspacePath,
        )
        val id = Regex("proc-\\d+").find(started.output)!!.value
        withTimeout(5_000) {
            while (!childFile.isFile) delay(20)
        }
        val pid = childFile.readText().trim().toLong()

        assertFalse(manager.stop(id).isError)

        awaitStopped(pid)
        assertFalse(isAlive(pid))
    }

    @Test fun persistenceFailureDoesNotStartAnUnmanagedProcess() = runBlocking {
        assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
        val starts = AtomicInteger()
        val stops = AtomicInteger()
        val manager = ProcessManager(
            shellProvider = { hostShell() },
            onStarted = { starts.incrementAndGet() },
            onStopped = { stops.incrementAndGet() },
            storageDirectory = tmp.newFile("not-a-directory"),
        )

        val started = manager.start("exec sleep 30", context.workspacePath)

        assertTrue(started.isError)
        assertTrue(started.output.contains("could not be saved"))
        assertEquals(0, starts.get())
        assertEquals(0, stops.get())
        assertEquals("No managed background processes.", manager.list().output)
    }

    @Test fun serviceStartFailureDoesNotOrphanTheProcess() = runBlocking {
        assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
        val pidFile = File(tmp.root, "failed-start.pid")
        val manager = ProcessManager(
            shellProvider = { hostShell() },
            onStarted = {
                repeat(250) {
                    if (pidFile.isFile) return@repeat
                    Thread.sleep(20)
                }
                error("service unavailable")
            },
        )

        val result = manager.start("echo \$\$ > failed-start.pid; exec sleep 30", context.workspacePath)
        val pid = pidFile.readText().trim().toLong()

        assertTrue(result.isError)
        assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false))
        assertEquals("No managed background processes.", manager.list().output)
    }
}
