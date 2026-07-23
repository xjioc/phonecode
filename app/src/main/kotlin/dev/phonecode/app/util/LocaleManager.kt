package dev.phonecode.app.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Applies an in-app language override on top of the system locale.
 *
 * Usage: call [applyLanguage] inside [android.app.Activity.attachBaseContext] so every
 * resource lookup (strings, layouts, plurals) uses the chosen locale.
 *
 * Supported values for [language]:
 *  - `"SYSTEM"` – follow the device locale (no-op, returns [context] unchanged).
 *  - `"en"`     – force English.
 *  - `"zh"`     – force Simplified Chinese.
 */
object LocaleManager {

    /**
     * Returns a [Context] whose [android.content.res.Resources] are configured for [language].
     * When [language] is `"SYSTEM"` (or unrecognised) the original [context] is returned as-is.
     */
    fun applyLanguage(context: Context, language: String): Context {
        val locale = when (language) {
            "en" -> Locale.ENGLISH
            "zh" -> Locale.SIMPLIFIED_CHINESE
            else -> return context // SYSTEM – keep the device default
        }

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        // Also set the layout direction so RTL/LTR picks up correctly for the chosen locale.
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
