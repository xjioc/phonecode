package dev.phonecode.tools

import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertSame
import org.junit.Test

class ToolRegistryTest {
    @Test fun keepsTheFirstToolWhenNamesCollide() {
        val first = StubTool("same")
        val second = StubTool("same")
        assertSame(first, ToolRegistry(listOf(first, second)).get("same"))
    }

    private class StubTool(override val name: String) : Tool {
        override val description = name
        override val parameters = JsonObject(emptyMap())
        override suspend fun execute(args: JsonObject, context: ToolContext) = ToolResult(name)
    }
}
