package dev.phonecode.app.data

import dev.phonecode.tools.mcp.McpConfig
import dev.phonecode.tools.mcp.McpServerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class McpSkillRepositoryTest {
    @Test fun discoversPortableRootsWithProjectPrecedence() = withRepository { config, project, repository ->
        writeSkill(config.resolve("skills/shared"), "shared", "Global skill")
        writeSkill(config.resolve(".opencode/skills/global-opencode"), "global-opencode", "Global OpenCode skill")
        writeSkill(config.resolve(".claude/skills/claude"), "claude", "Claude skill")
        writeSkill(config.resolve(".codex/skills/codex"), "codex", "Codex skill")
        writeSkill(project.resolve(".agents/skills/shared"), "shared", "Project skill")
        writeSkill(project.resolve(".opencode/skills/opencode"), "opencode", "OpenCode skill")
        writeSkill(project.resolve(".agents/skills/wrong-folder"), "wrong-name", "Invalid location")

        val skills = repository.discoverSkills(project)

        assertEquals(listOf("shared", "opencode", "global-opencode", "claude", "codex"), skills.map { it.name })
        assertEquals("Project skill", skills.first().description)
        assertTrue(skills.first().location.endsWith(".agents/skills/shared/SKILL.md"))
    }

    @Test fun inventoryShowsInvalidAndShadowedSkills() = withRepository { config, project, repository ->
        writeSkill(project.resolve(".agents/skills/shared"), "shared", "Project skill")
        writeSkill(config.resolve("skills/shared"), "shared", "Global skill")
        writeSkill(config.resolve("skills/wrong-folder"), "wrong-name", "Wrong folder")
        config.resolve("skills/missing").mkdirs()
        config.resolve("skills/broken").apply {
            mkdirs()
            resolve("SKILL.md").writeText("not frontmatter")
        }

        val inventory = repository.scanSkills(project)

        assertEquals(SkillStatus.ACTIVE, inventory.items.first { it.location.contains("project") }.status)
        assertEquals(SkillStatus.SHADOWED, inventory.items.first { it.name == "shared" && it.scope == SkillScope.GLOBAL }.status)
        assertEquals("Skill name must match its folder", inventory.items.first { it.name == "wrong-name" }.issue)
        assertEquals("Missing SKILL.md", inventory.items.first { it.name == "missing" }.issue)
        assertEquals("Invalid SKILL.md frontmatter", inventory.items.first { it.name == "broken" }.issue)
    }

    @Test fun disabledProjectOverridePersistsAndRevealsGlobalSkill() = withRepository { config, project, repository ->
        writeSkill(project.resolve(".agents/skills/shared"), "shared", "Project skill")
        writeSkill(config.resolve("skills/shared"), "shared", "Global skill")
        val projectSkill = repository.scanSkills(project).items.first { it.scope == SkillScope.PROJECT }

        assertTrue(repository.setSkillEnabled(projectSkill.id, false, project).isSuccess)

        val reloaded = McpSkillRepository(config).scanSkills(project)
        assertEquals(SkillStatus.DISABLED, reloaded.items.first { it.id == projectSkill.id }.status)
        assertEquals(SkillStatus.ACTIVE, reloaded.items.first { it.scope == SkillScope.GLOBAL }.status)
        assertEquals("Global skill", reloaded.active.single().description)
    }

    @Test fun deleteSkillOnlyAcceptsDiscoveredManagedEntries() = withRepository { config, project, repository ->
        writeSkill(config.resolve("skills/remove-me"), "remove-me", "Remove me")
        val skill = repository.scanSkills(project).items.single()
        val outside = requireNotNull(config.parentFile).resolve("outside/SKILL.md").apply {
            requireNotNull(parentFile).mkdirs()
            writeText("outside")
        }

        assertTrue(repository.deleteSkill(outside.canonicalPath, project).isFailure)
        assertTrue(outside.exists())
        assertTrue(repository.deleteSkill(skill.id, project).isSuccess)
        assertFalse(requireNotNull(File(skill.location).parentFile).exists())
    }

    @Test fun malformedMcpConfigIsReportedAndPreservedByEveryMutation() = withRepository { config, _, repository ->
        val file = config.resolve("opencode.json").apply {
            requireNotNull(parentFile).mkdirs()
            writeText("{ definitely not valid")
        }
        val original = file.readBytes()

        assertTrue(repository.loadMcpConfigState() is McpConfigLoad.Invalid)
        assertTrue(repository.saveMcpConfig(McpConfig()).isFailure)
        assertTrue(repository.upsertMcpServer(name = "new", server = McpServerConfig(url = "https://example.com/mcp")).isFailure)
        assertTrue(repository.removeMcpServer("new").isFailure)
        assertTrue(repository.setMcpEnabled("new", false).isFailure)
        assertTrue(original.contentEquals(file.readBytes()))
    }

    @Test fun safeMcpMutatorsAddRenameToggleAndDelete() = withRepository { _, _, repository ->
        val server = McpServerConfig(url = "https://example.com/mcp")

        assertTrue(repository.upsertMcpServer(name = "one", server = server).isSuccess)
        assertTrue(repository.upsertMcpServer(originalName = "one", name = "two", server = server).isSuccess)
        assertTrue(repository.setMcpEnabled("two", false).isSuccess)
        assertFalse(repository.loadMcpConfig().mcp.getValue("two").enabled)
        assertFalse("one" in repository.loadMcpConfig().mcp)
        assertTrue(repository.removeMcpServer("two").isSuccess)
        assertTrue(repository.loadMcpConfig().mcp.isEmpty())
    }

    @Test fun editorMcpWriteRejectsAnExternalChange() = withRepository { _, _, repository ->
        val original = McpServerConfig(url = "https://example.com/mcp")
        val external = original.copy(timeout = 8_000)
        assertTrue(repository.upsertMcpServer(name = "docs", server = original).isSuccess)
        assertTrue(repository.upsertMcpServer(originalName = "docs", name = "docs", server = external).isSuccess)

        val staleWrite = repository.upsertMcpServer(
            originalName = "docs",
            name = "docs",
            server = original.copy(enabled = false),
            expectedServer = original,
        )

        assertTrue(staleWrite.isFailure)
        assertEquals(external, repository.loadMcpConfig().mcp.getValue("docs"))
    }

    @Test fun runtimeFingerprintTracksMcpSkillAndProjectChanges() = withRepository { config, project, repository ->
        val initial = repository.runtimeFingerprint(project)
        repository.replaceMcpConfig("""{"mcp":{}}""").getOrThrow()
        val afterMcp = repository.runtimeFingerprint(project)
        repository.writeSkillFile(
            SkillScope.PROJECT,
            "live-skill",
            content = "---\nname: live-skill\ndescription: Live\n---\nFirst",
            projectDir = project,
        ).getOrThrow()
        val afterProjectSkill = repository.runtimeFingerprint(project)
        repository.writeSkillFile(
            SkillScope.GLOBAL,
            "global-skill",
            content = "---\nname: global-skill\ndescription: Global\n---\nSecond",
            projectDir = project,
        ).getOrThrow()
        val afterGlobalSkill = repository.runtimeFingerprint(project)

        assertFalse(initial.mcp == afterMcp.mcp)
        assertEquals(initial.skills, afterMcp.skills)
        assertFalse(afterMcp.skills == afterProjectSkill.skills)
        assertFalse(afterProjectSkill.skills == afterGlobalSkill.skills)
        assertTrue(repository.watchedDirectories(project).contains(config.canonicalFile))
        assertTrue(repository.watchedDirectories(project).contains(project.canonicalFile))
    }

    @Test fun boundedSkillBridgeReadsWritesAndRejectsTraversal() = withRepository { _, project, repository ->
        val content = "---\nname: bridge\ndescription: Bridge\n---\nBody"

        assertTrue(repository.writeSkillFile(SkillScope.GLOBAL, "bridge", content = content, projectDir = project).isSuccess)
        assertEquals(content, repository.readSkillFile(SkillScope.GLOBAL, "bridge", projectDir = project).getOrThrow())
        assertTrue(repository.writeSkillFile(SkillScope.PROJECT, "bridge", "references/guide.md", "Guide", project).isSuccess)
        assertEquals("Guide", repository.readSkillFile(SkillScope.PROJECT, "bridge", "references/guide.md", project).getOrThrow())
        assertTrue(repository.writeSkillFile(SkillScope.GLOBAL, "bridge", "../outside", "bad", project).isFailure)
        assertTrue(repository.readSkillFile(SkillScope.GLOBAL, "bridge", "../outside", project).isFailure)
    }

    @Test fun managedSkillEditorValidatesIdentityAndPersistsAtomically() = withRepository { config, project, repository ->
        writeSkill(config.resolve("skills/editable"), "editable", "Before")
        val skill = repository.scanSkills(project).items.single()
        val updated = "---\nname: editable\ndescription: After\n---\nUpdated body"

        assertTrue(repository.writeSkill(skill.id, updated, project).isSuccess)
        assertEquals(updated, repository.readSkill(skill.id, project).getOrThrow())
        assertTrue(repository.writeSkill(skill.id, updated.replace("name: editable", "name: renamed"), project).isFailure)
        assertEquals(updated, repository.readSkill(skill.id, project).getOrThrow())
    }

    @Test fun managedSkillEditorRejectsAnExternalChange() = withRepository { config, project, repository ->
        writeSkill(config.resolve("skills/editable"), "editable", "Before")
        val skill = repository.scanSkills(project).items.single()
        val baseline = repository.readSkill(skill.id, project).getOrThrow()
        val external = baseline.replace("Before", "External")
        File(skill.location).writeText(external)

        val staleWrite = repository.writeSkill(
            skill.id,
            baseline.replace("Before", "Editor"),
            project,
            expectedContent = baseline,
        )

        assertTrue(staleWrite.isFailure)
        assertEquals(external, repository.readSkill(skill.id, project).getOrThrow())
    }

    @Test fun validRawReplacementRepairsMalformedMcpConfig() = withRepository { config, _, repository ->
        config.resolve("opencode.json").apply {
            parentFile?.mkdirs()
            writeText("invalid")
        }

        val repaired = repository.replaceMcpConfig(
            """{"mcp":{"docs":{"type":"remote","url":"https://example.com/mcp","enabled":true,"timeout":5000}}}""",
        )

        assertTrue(repaired.isSuccess)
        assertEquals("https://example.com/mcp", repository.loadMcpConfig().mcp.getValue("docs").url)
    }

    @Test fun everyMcpWritePathEnforcesTransportAndHeaderValidation() = withRepository { config, _, repository ->
        val unsafe = McpServerConfig(url = "http://example.com/mcp", headers = mapOf("X-Test" to "ok\r\nInjected: yes"))

        assertTrue(repository.upsertMcpServer(name = "unsafe", server = unsafe).isFailure)
        assertTrue(repository.replaceMcpConfig("""{"mcp":{"unsafe":{"url":"http://example.com/mcp"}}}""").isFailure)
        assertTrue(repository.upsertMcpServer(name = "userinfo", server = McpServerConfig(url = "https://user:password@example.com/mcp")).isFailure)
        assertFalse(config.resolve("opencode.json").exists())
        assertTrue(repository.upsertMcpServer(name = "local", server = McpServerConfig(url = "http://127.0.0.1:8080/mcp")).isSuccess)
    }

    @Test fun endpointValidationMatchesEverySupportedMcpSurface() {
        assertTrue(isSafeMcpEndpoint("https://example.com/mcp"))
        assertTrue(isSafeMcpEndpoint("http://localhost:8080/mcp"))
        assertTrue(isSafeMcpEndpoint("http://127.0.0.1:8080/mcp"))
        assertTrue(isSafeMcpEndpoint("http://[::1]:8080/mcp"))
        assertFalse(isSafeMcpEndpoint("http://example.com/mcp"))
        assertFalse(isSafeMcpEndpoint("https://user:password@example.com/mcp"))
    }

    @Test fun mcpHeadersAreEncryptedOutsideTheConfigFile() {
        val root = Files.createTempDirectory("phonecode-mcp-secrets").toFile()
        try {
            val config = root.resolve("config")
            val secrets = MemorySecrets()
            val repository = McpSkillRepository(config, secrets)
            val server = McpServerConfig(
                url = "https://example.com/mcp",
                headers = mapOf("Authorization" to "Bearer secret-token"),
            )

            assertTrue(repository.upsertMcpServer(name = "docs", server = server).isSuccess)
            assertFalse(config.resolve("opencode.json").readText().contains("secret-token"))
            assertEquals(server.headers, McpSkillRepository(config, secrets).loadMcpConfig().mcp.getValue("docs").headers)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun plaintextMcpHeadersMigrateWithoutChangingTheirValue() {
        val root = Files.createTempDirectory("phonecode-mcp-migration").toFile()
        try {
            val config = root.resolve("config").apply { mkdirs() }
            config.resolve("opencode.json").writeText(
                """{"mcp":{"docs":{"type":"remote","url":"https://example.com/mcp","headers":{"Authorization":"Bearer old-token"},"enabled":true,"timeout":5000}}}""",
            )
            val secrets = MemorySecrets()
            val repository = McpSkillRepository(config, secrets)

            assertEquals("Bearer old-token", repository.loadMcpConfig().mcp.getValue("docs").headers["Authorization"])
            assertFalse(config.resolve("opencode.json").readText().contains("old-token"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun secretWriteFailureRollsBackEndpointAndCredentials() {
        val root = Files.createTempDirectory("phonecode-mcp-secret-rollback").toFile()
        try {
            val config = root.resolve("config")
            val secrets = MemorySecrets()
            val repository = McpSkillRepository(config, secrets)
            val original = McpServerConfig(
                url = "https://old.example/mcp",
                headers = mapOf("Authorization" to "Bearer old-token"),
            )
            repository.upsertMcpServer(name = "docs", server = original).getOrThrow()
            val configBefore = config.resolve("opencode.json").readBytes()
            val stateBefore = config.resolve(".mcp-secret-state.json").readBytes()
            secrets.failNextPutAfterWrite = true

            val result = repository.upsertMcpServer(
                originalName = "docs",
                name = "docs",
                server = original.copy(
                    url = "https://new.example/mcp",
                    headers = mapOf("Authorization" to "Bearer new-token"),
                ),
            )

            assertTrue(result.isFailure)
            assertTrue(configBefore.contentEquals(config.resolve("opencode.json").readBytes()))
            assertTrue(stateBefore.contentEquals(config.resolve(".mcp-secret-state.json").readBytes()))
            assertEquals(original, McpSkillRepository(config, secrets).loadMcpConfig().mcp.getValue("docs"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun configFailureBeforeCommitKeepsTheOldEndpointCredentialPair() {
        val root = Files.createTempDirectory("phonecode-mcp-config-rollback").toFile()
        try {
            val config = root.resolve("config")
            val secrets = MemorySecrets()
            val original = McpServerConfig(
                url = "https://old.example/mcp",
                headers = mapOf("Authorization" to "Bearer old-token"),
            )
            McpSkillRepository(config, secrets).upsertMcpServer(name = "docs", server = original).getOrThrow()
            val configBefore = config.resolve("opencode.json").readBytes()
            val stateBefore = config.resolve(".mcp-secret-state.json").readBytes()
            var failConfigWrite = true
            val repository = McpSkillRepository(config, secrets) { file, text ->
                if (failConfigWrite && file.name == "opencode.json") {
                    failConfigWrite = false
                    error("Injected config write failure")
                }
                file.writeTextAtomically(text)
            }

            val result = repository.upsertMcpServer(
                originalName = "docs",
                name = "docs",
                server = original.copy(
                    url = "https://new.example/mcp",
                    headers = mapOf("Authorization" to "Bearer new-token"),
                ),
            )

            assertTrue(result.isFailure)
            assertTrue(configBefore.contentEquals(config.resolve("opencode.json").readBytes()))
            assertTrue(stateBefore.contentEquals(config.resolve(".mcp-secret-state.json").readBytes()))
            assertEquals(original, McpSkillRepository(config, secrets).loadMcpConfig().mcp.getValue("docs"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun failureReportedAfterConfigCommitKeepsTheNewEndpointCredentialPair() {
        val root = Files.createTempDirectory("phonecode-mcp-config-observed").toFile()
        try {
            val config = root.resolve("config")
            val secrets = MemorySecrets()
            val original = McpServerConfig(
                url = "https://old.example/mcp",
                headers = mapOf("Authorization" to "Bearer old-token"),
            )
            McpSkillRepository(config, secrets).upsertMcpServer(name = "docs", server = original).getOrThrow()
            var failAfterConfigWrite = true
            val repository = McpSkillRepository(config, secrets) { file, text ->
                file.writeTextAtomically(text)
                if (failAfterConfigWrite && file.name == "opencode.json") {
                    failAfterConfigWrite = false
                    error("Injected failure after config commit")
                }
            }
            val updated = original.copy(
                url = "https://new.example/mcp",
                headers = mapOf("Authorization" to "Bearer new-token"),
            )

            val result = repository.upsertMcpServer(originalName = "docs", name = "docs", server = updated)

            assertTrue(result.isSuccess)
            assertEquals(updated, McpSkillRepository(config, secrets).loadMcpConfig().mcp.getValue("docs"))
            assertEquals(1, secrets.keys().size)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun committedConfigDoesNotDependOnTheSecretStateMirror() {
        val root = Files.createTempDirectory("phonecode-mcp-state-mirror").toFile()
        try {
            val config = root.resolve("config")
            val secrets = MemorySecrets()
            val original = McpServerConfig(
                url = "https://old.example/mcp",
                headers = mapOf("Authorization" to "Bearer old-token"),
            )
            McpSkillRepository(config, secrets).upsertMcpServer(name = "docs", server = original).getOrThrow()
            val repository = McpSkillRepository(config, secrets) { file, text ->
                if (file.name == ".mcp-secret-state.json") error("Injected mirror failure")
                file.writeTextAtomically(text)
            }
            val updated = original.copy(
                url = "https://new.example/mcp",
                headers = mapOf("Authorization" to "Bearer new-token"),
            )

            val result = repository.upsertMcpServer(originalName = "docs", name = "docs", server = updated)

            assertTrue(result.isSuccess)
            assertEquals(updated, McpSkillRepository(config, secrets).loadMcpConfig().mcp.getValue("docs"))
            assertEquals(1, secrets.keys().size)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun legacyEncryptedMcpHeadersMigrateToAReferencedRevision() {
        val root = Files.createTempDirectory("phonecode-mcp-legacy-secret").toFile()
        try {
            val config = root.resolve("config").apply { mkdirs() }
            config.resolve("opencode.json").writeText(
                """{"mcp":{"docs":{"type":"remote","url":"https://example.com/mcp","headers":{},"enabled":true,"timeout":5000}}}""",
            )
            config.resolve(".mcp-secret-state.json").writeText(
                """{"names":["docs"],"revision":"legacy"}""",
            )
            val secrets = MemorySecrets().apply {
                put("mcp.headers.docs", """{"values":{"Authorization":"Bearer old-token"}}""")
            }

            val loaded = McpSkillRepository(config, secrets).loadMcpConfig().mcp.getValue("docs")

            assertEquals("Bearer old-token", loaded.headers["Authorization"])
            assertEquals(1, secrets.keys().size)
            assertTrue(secrets.keys().single().startsWith("mcp.headers."))
            assertFalse("mcp.headers.docs" in secrets.keys())
            assertTrue(config.resolve("opencode.json").readText().contains("_phonecodeMcpSecrets"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun encryptedMcpHeadersFailClosedWhenSecureStorageIsUnavailable() {
        val root = Files.createTempDirectory("phonecode-mcp-locked").toFile()
        try {
            val config = root.resolve("config")
            val secrets = MemorySecrets()
            val repository = McpSkillRepository(config, secrets)
            repository.upsertMcpServer(
                name = "docs",
                server = McpServerConfig(
                    url = "https://example.com/mcp",
                    headers = mapOf("Authorization" to "Bearer secret-token"),
                ),
            ).getOrThrow()

            val locked = McpSkillRepository(config, MemorySecrets(available = false)).loadMcpConfigState()

            assertTrue(locked is McpConfigLoad.Invalid)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun encryptedMcpHeadersFailClosedWhenCredentialMetadataIsCorrupt() {
        val root = Files.createTempDirectory("phonecode-mcp-corrupt-secrets").toFile()
        try {
            val config = root.resolve("config")
            val secrets = MemorySecrets()
            val repository = McpSkillRepository(config, secrets)
            repository.upsertMcpServer(
                name = "docs",
                server = McpServerConfig(
                    url = "https://example.com/mcp",
                    headers = mapOf("Authorization" to "Bearer secret-token"),
                ),
            ).getOrThrow()
            val file = config.resolve("opencode.json")
            val raw = file.readText()
            file.writeText(raw.substringBefore("\"_phonecodeMcpSecrets\":") + "\"_phonecodeMcpSecrets\":\"invalid\"}")

            assertTrue(repository.loadMcpConfigState() is McpConfigLoad.Invalid)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun withRepository(block: (File, File, McpSkillRepository) -> Unit) {
        val root = Files.createTempDirectory("phonecode-skill-repository").toFile()
        try {
            val config = root.resolve("config")
            val project = root.resolve("project")
            block(config, project, McpSkillRepository(config))
        } finally {
            root.deleteRecursively()
        }
    }

    private fun writeSkill(directory: File, name: String, description: String) {
        directory.mkdirs()
        directory.resolve("SKILL.md").writeText("---\nname: $name\ndescription: $description\n---\nBody")
    }

    private class MemorySecrets(override val available: Boolean = true) : SecretValueStore {
        private val values = mutableMapOf<String, String>()
        var failNextPutAfterWrite = false
        override fun get(name: String): String? = values[name]
        override fun put(name: String, value: String) {
            if (value.isBlank()) values.remove(name) else values[name] = value
            if (failNextPutAfterWrite) {
                failNextPutAfterWrite = false
                error("Injected secret write failure")
            }
        }

        fun keys(): Set<String> = values.keys
    }
}
