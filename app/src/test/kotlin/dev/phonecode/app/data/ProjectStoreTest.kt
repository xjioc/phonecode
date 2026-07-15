package dev.phonecode.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class ProjectStoreTest {
    @Test fun replaceRestoresTheExactProjectSnapshot() {
        val dir = Files.createTempDirectory("project-store-test").toFile()
        try {
            val store = ProjectStore(dir.resolve("projects.json"))
            val original = listOf(
                Project("project-one", "One", "folder-one"),
                Project("project-two", "Two", "folder-two"),
            )
            store.replace(original)
            store.delete("project-one")
            store.replace(original)
            assertEquals(original, store.list())
        } finally {
            dir.deleteRecursively()
        }
    }
}
