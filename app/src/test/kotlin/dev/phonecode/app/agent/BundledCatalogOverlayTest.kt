package dev.phonecode.app.agent

import dev.phonecode.provider.catalog.catalogJson
import dev.phonecode.provider.domain.ReasoningEffort
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BundledCatalogOverlayTest {

    private fun catalog(providerId: String, body: String) =
        catalogJson.decodeFromString<dev.phonecode.provider.catalog.Catalog>("""{"$providerId":$body}""")

    @Test
    fun overlayAddsFirstPartyProvidersMissingFromLiveCatalog() {
        val merged = withBundledOverlay(catalog("openai", """{"id":"openai","models":{}}"""))

        val sensenova = merged["sensenova"] ?: error("sensenova missing after overlay")
        assertEquals(
            listOf(
                ReasoningEffort.DEFAULT,
                ReasoningEffort.NONE,
                ReasoningEffort.LOW,
                ReasoningEffort.MEDIUM,
                ReasoningEffort.HIGH,
            ),
            reasoningOptionsOf(sensenova, "glm-5.2"),
        )
        assertEquals(
            listOf(
                ReasoningEffort.DEFAULT,
                ReasoningEffort.NONE,
                ReasoningEffort.LOW,
                ReasoningEffort.MEDIUM,
                ReasoningEffort.HIGH,
            ),
            reasoningOptionsOf(sensenova, "deepseek-v4-flash"),
        )
        val go = merged["opencode-go"] ?: error("opencode-go missing after overlay")
        assertEquals(
            listOf(ReasoningEffort.DEFAULT, ReasoningEffort.HIGH, ReasoningEffort.MAX),
            reasoningOptionsOf(go, "deepseek-v4-flash"),
        )
        // Non-first-party providers are left untouched.
        assertEquals(emptyMap<String, Any>(), merged["openai"]?.models)
        assertNull(merged["openai"]?.models?.get("gpt-5.6"))
    }

    @Test
    fun overlayBackfillsReasoningOptionsWhenLiveEntryLacksThem() {
        val live = catalog(
            "sensenova",
            """{"id":"sensenova","models":{"glm-5.2":{"id":"glm-5.2","name":"GLM 5.2","reasoning":true}}}""",
        )
        val merged = withBundledOverlay(live)

        val efforts = reasoningOptionsOf(merged["sensenova"]!!, "glm-5.2")
        assertEquals(ReasoningEffort.HIGH, efforts.last())
        assertEquals(5, efforts.size)
    }

    @Test
    fun overlayKeepsLiveModelsAlongsideBundledOnes() {
        val live = catalog(
            "sensenova",
            """{"id":"sensenova","models":{"future-model":{"id":"future-model","name":"Future","reasoning":true}}}""",
        )
        val merged = withBundledOverlay(live)

        assertEquals(setOf("future-model", "glm-5.2", "deepseek-v4-flash", "sensenova-6.7-flash-lite"), merged["sensenova"]!!.models.keys)
    }

    private fun reasoningOptionsOf(
        provider: dev.phonecode.provider.catalog.ProviderInfo,
        modelId: String,
    ): List<ReasoningEffort> {
        val model = provider.models[modelId] ?: error("model $modelId missing")
        val efforts = model.reasoningOptions
            .firstOrNull { it.type == "effort" }
            ?.values
            .orEmpty()
            .mapNotNull(ReasoningEffort::fromWire)
        return (listOf(ReasoningEffort.DEFAULT) + efforts).distinct()
    }
}
