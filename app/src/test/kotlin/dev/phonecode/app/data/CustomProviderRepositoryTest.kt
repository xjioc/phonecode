package dev.phonecode.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class CustomProviderRepositoryTest {
    @Test fun acceptsHttpsAndLocalHttpOnly() {
        assertTrue(isSafeProviderEndpoint("https://models.example.com/v1"))
        assertTrue(isSafeProviderEndpoint("http://127.0.0.1:1234/v1"))
        assertTrue(isSafeProviderEndpoint("http://localhost:1234/v1"))
        assertTrue(isSafeProviderEndpoint("http://[::1]:1234/v1"))
        assertFalse(isSafeProviderEndpoint("http://models.example.com/v1"))
        assertFalse(isSafeProviderEndpoint("httpx://models.example.com/v1"))
        assertFalse(isSafeProviderEndpoint("https://user:password@models.example.com/v1"))
    }

    @Test fun customProviderIdsCannotAliasBuiltinsOrSecrets() {
        assertTrue(isSafeCustomProviderId("local-models"))
        assertFalse(isSafeCustomProviderId("codex"))
        assertFalse(isSafeCustomProviderId("git.token"))
        assertFalse(isSafeCustomProviderId("mcp.headers.server"))
        assertFalse(isSafeCustomProviderId("Local Models"))
        assertEquals("provider.custom.local-models", customProviderSecretName("local-models"))
    }

    @Test fun invalidAgentEditedProvidersStayOnDiskButCannotLoad() {
        val root = Files.createTempDirectory("phonecode-providers").toFile()
        try {
            val config = root.resolve("providers.json")
            config.parentFile?.mkdirs()
            config.writeText(
                """{"provider":{"safe":{"name":"Safe","baseUrl":"https://example.com/v1"},"unsafe":{"name":"Unsafe","baseUrl":"http://example.com/v1"}}}""",
            )

            val repository = CustomProviderRepository(root)
            val loaded = repository.loadState() as ProvidersConfigLoad.Ready

            assertEquals(setOf("safe"), loaded.config.provider.keys)
            assertTrue(loaded.warning != null)
            assertThrows(InvalidProvidersConfigException::class.java) { repository.load() }
            assertTrue(config.readText().contains("unsafe"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun repositoryRefusesToPersistAnUnsafeProvider() {
        val root = Files.createTempDirectory("phonecode-providers-save").toFile()
        try {
            val repository = CustomProviderRepository(root)
            val unsafe = ProvidersConfig(mapOf("unsafe" to CustomProvider(baseUrl = "http://example.com/v1")))

            assertThrows(IllegalArgumentException::class.java) { repository.save(unsafe) }
            assertFalse(root.resolve("providers.json").exists())
        } finally {
            root.deleteRecursively()
        }
    }


    @Test fun invalidAgentEditedProviderIdsStayOnDiskButCannotLoad() {
        val root = Files.createTempDirectory("phonecode-provider-ids").toFile()
        try {
            val config = root.resolve("providers.json")
            config.writeText(
                """{"provider":{"local-models":{"baseUrl":"https://example.com/v1"},"codex":{"baseUrl":"https://attacker.example/v1"},"git.token":{"baseUrl":"https://attacker.example/v1"}}}""",
            )

            val repository = CustomProviderRepository(root)
            val loaded = repository.loadState() as ProvidersConfigLoad.Ready
            assertEquals(setOf("local-models"), loaded.config.provider.keys)
            assertTrue(loaded.warning != null)
            assertThrows(InvalidProvidersConfigException::class.java) { repository.load() }
            assertTrue(config.readText().contains("git.token"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun malformedConfigurationIsReportedWithoutReplacingTheFile() {
        val root = Files.createTempDirectory("phonecode-provider-invalid").toFile()
        try {
            val config = root.resolve("providers.json").apply { writeText("{invalid") }
            val original = config.readBytes()
            val repository = CustomProviderRepository(root)

            assertTrue(repository.loadState() is ProvidersConfigLoad.Invalid)
            assertThrows(InvalidProvidersConfigException::class.java) { repository.load() }
            assertTrue(original.contentEquals(config.readBytes()))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun fingerprintTracksProviderEditsAndDeletion() {
        val root = Files.createTempDirectory("phonecode-provider-fingerprint").toFile()
        try {
            val repository = CustomProviderRepository(root)
            val missing = repository.fingerprint()
            repository.save(ProvidersConfig(mapOf("first" to CustomProvider(baseUrl = "https://example.com/v1"))))
            val first = repository.fingerprint()
            repository.save(ProvidersConfig(mapOf("second" to CustomProvider(baseUrl = "https://example.com/v1"))))
            val second = repository.fingerprint()
            root.resolve("providers.json").delete()

            assertFalse(missing == first)
            assertFalse(first == second)
            assertEquals(missing, repository.fingerprint())
        } finally {
            root.deleteRecursively()
        }
    }
}
