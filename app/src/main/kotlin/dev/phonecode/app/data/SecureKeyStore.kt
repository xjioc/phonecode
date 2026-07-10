package dev.phonecode.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureKeyStore(context: Context) {
    private val prefs = createPrefs(context)
    val secureStorageUnavailable = prefs == null

    fun get(providerId: String): String? = prefs?.getString(providerId, null)

    fun put(providerId: String, key: String) {
        prefs?.edit()?.apply { if (key.isBlank()) remove(providerId) else putString(providerId, key) }?.apply()
    }

    private companion object {
        private const val FILE = "phonecode_provider_keys"

        fun createPrefs(context: Context): SharedPreferences? = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            try {
                build(context, masterKey)
            } catch (e: Exception) {
                context.deleteSharedPreferences(FILE)
                build(context, masterKey)
            }
        } catch (e: Exception) {
            context.deleteSharedPreferences(FILE + "_plain")
            null
        }

        private fun build(context: Context, masterKey: MasterKey) = EncryptedSharedPreferences.create(
            context,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}
