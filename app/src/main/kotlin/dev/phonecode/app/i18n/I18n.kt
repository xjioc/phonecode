package dev.phonecode.app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import java.util.Locale

object I18n {
    private val translations: Map<String, Map<String, String>> = mapOf(
        "zh" to zhTranslations,
    )

    /** Language override from user settings. "SYSTEM" means follow device locale. */
    @Volatile
    var overrideLanguage: String = "SYSTEM"

    fun currentLanguage(): String =
        if (overrideLanguage != "SYSTEM") overrideLanguage
        else Locale.getDefault().language

    fun tr(key: String): String {
        val lang = currentLanguage()
        return translations[lang]?.get(key) ?: key
    }
}

@Composable
@ReadOnlyComposable
fun tr(text: String): String = I18n.tr(text)
