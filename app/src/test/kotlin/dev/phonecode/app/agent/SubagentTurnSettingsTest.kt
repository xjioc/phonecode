package dev.phonecode.app.agent

import dev.phonecode.provider.catalog.Limit
import dev.phonecode.provider.domain.ReasoningEffort
import org.junit.Assert.assertEquals
import org.junit.Test

class SubagentTurnSettingsTest {
    @Test fun childTurnKeepsTheSelectedModelsContextAndOutputBounds() {
        val settings = boundedTurnSettings(
            model = "bounded-model",
            effort = ReasoningEffort.HIGH,
            limit = Limit(context = 32_000, output = 4_000),
        )

        assertEquals("bounded-model", settings.model)
        assertEquals(ReasoningEffort.HIGH, settings.reasoningEffort)
        assertEquals(32_000L, settings.contextLimit)
        assertEquals(4_000L, settings.maxOutput)
    }
}
