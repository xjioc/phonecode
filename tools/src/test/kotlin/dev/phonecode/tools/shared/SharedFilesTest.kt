package dev.phonecode.tools.shared

import dev.phonecode.tools.ToolContext
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class SharedFilesTest {
    private val context = object : ToolContext {
        override val workspacePath = "/tmp"
        override suspend fun requestPermission(tool: String, summary: String) = true
    }

    private class Access : SharedFileAccess {
        var written: Triple<String, String, String>? = null
        override fun roots() = listOf(SharedRoot("root-1", "Documents", true))
        override suspend fun list(rootId: String, path: String) =
            listOf(SharedEntry("src", true, null), SharedEntry("README.md", false, 12))
        override suspend fun read(rootId: String, path: String, maxBytes: Int) = "hello"
        override suspend fun write(rootId: String, path: String, content: String) {
            written = Triple(rootId, path, content)
        }
        override suspend fun mkdir(rootId: String, path: String) = Unit
        override suspend fun delete(rootId: String, path: String) = Unit
        override suspend fun rename(rootId: String, path: String, newName: String) = Unit
    }

    @Test fun listsRootsAndFolderContents() = runTest {
        val access = Access()
        val tool = SharedReadTool(access)
        val roots = tool.execute(buildJsonObject { put("action", "roots") }, context)
        assertFalse(roots.isError)
        assertTrue(roots.output.contains("root-1\tDocuments\tread-write"))

        val list = tool.execute(buildJsonObject {
            put("action", "list")
            put("root", "root-1")
            put("path", "")
        }, context)
        assertTrue(list.output.contains("src/"))
        assertTrue(list.output.contains("README.md\t12 bytes"))
    }

    @Test fun writesThroughTheSelectedRoot() = runTest {
        val access = Access()
        val tool = SharedWriteTool(access)
        val result = tool.execute(buildJsonObject {
            put("action", "write")
            put("root", "root-1")
            put("path", "notes.txt")
            put("content", "saved")
        }, context)
        assertFalse(result.isError)
        assertTrue(tool.mutating)
        assertEquals(Triple("root-1", "notes.txt", "saved"), access.written)
    }

    @Test fun pathsCannotEscapeLinkedFolders() {
        assertThrows(IllegalArgumentException::class.java) { sharedPathSegments("../secret.txt") }
        assertThrows(IllegalArgumentException::class.java) { sharedPathSegments("/absolute.txt") }
        assertEquals(listOf("src", "main.kt"), sharedPathSegments("./src/main.kt"))
    }
}
