package dev.phonecode.app.agent

import dev.phonecode.provider.http.CodexModelInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class CodexModelVisibilityTest {
    @Test fun chatGptLoginKeepsListedSubscriptionModels() {
        val visible = CodexModelInfo(
            slug = "subscription-model",
            displayName = "Subscription model",
            visibility = "list",
            supportedInApi = false,
        )
        val hidden = CodexModelInfo(
            slug = "hidden-model",
            displayName = "Hidden model",
            visibility = "hide",
            supportedInApi = true,
        )

        assertEquals(listOf(visible), visibleCodexModels(listOf(visible, hidden)))
    }
}
