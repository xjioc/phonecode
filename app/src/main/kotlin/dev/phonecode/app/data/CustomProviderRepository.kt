package dev.phonecode.app.data

import dev.phonecode.provider.preset.AuthScheme
import dev.phonecode.provider.preset.BuiltInPresets
import dev.phonecode.provider.preset.ProviderPreset
import dev.phonecode.provider.preset.WireFormat
import kotlinx.serialization.Serializable
import java.io.File
import java.net.URI
import java.security.MessageDigest

@Serializable
data class CustomModel(val name: String = "", val context: Long? = null)

@Serializable
data class CustomProvider(
    val name: String = "",
    val baseUrl: String = "",
    /** "openai" (default, OpenAI-compatible) or "anthropic". */
    val format: String = "openai",
    val models: Map<String, CustomModel> = emptyMap(),
)

@Serializable
data class ProvidersConfig(val provider: Map<String, CustomProvider> = emptyMap())

sealed interface ProvidersConfigLoad {
    data class Ready(val config: ProvidersConfig, val warning: String? = null) : ProvidersConfigLoad
    data class Invalid(val message: String) : ProvidersConfigLoad
}

class InvalidProvidersConfigException(message: String) : IllegalStateException(message)

/**
 * Agent- and user-editable custom providers/models, stored as `providers.json` under the config dir
 * (same dir the agent already knows from the system prompt). The agent can add a provider/model by
 * editing this file with its file tools; the app reloads it into the catalog + model picker.
 */
class CustomProviderRepository(private val configDir: File) {
    private val file = File(configDir, "providers.json")
    private val json = storeJson

    fun loadState(): ProvidersConfigLoad {
        if (!file.exists()) return ProvidersConfigLoad.Ready(ProvidersConfig())
        if (!file.isFile || file.length() > MAX_CONFIG_BYTES) return ProvidersConfigLoad.Invalid("Provider configuration is too large")
        val raw = runCatching { file.readText() }.getOrElse {
            return ProvidersConfigLoad.Invalid("Provider configuration could not be read")
        }
        return runCatching {
            val decoded = json.decodeFromString(ProvidersConfig.serializer(), raw)
            val valid = decoded.provider.filter { (id, provider) ->
                isSafeCustomProviderId(id) && isSafeProviderEndpoint(provider.baseUrl)
            }
            ProvidersConfigLoad.Ready(
                decoded.copy(provider = valid),
                if (valid.size == decoded.provider.size) null else "Provider configuration contains invalid entries",
            )
        }.getOrElse { ProvidersConfigLoad.Invalid("Provider configuration is invalid") }
    }

    fun load(): ProvidersConfig = when (val loaded = loadState()) {
        is ProvidersConfigLoad.Ready -> loaded.config.takeIf { loaded.warning == null }
            ?: throw InvalidProvidersConfigException(requireNotNull(loaded.warning))
        is ProvidersConfigLoad.Invalid -> throw InvalidProvidersConfigException(loaded.message)
    }

    fun save(config: ProvidersConfig) {
        require(config.provider.keys.all(::isSafeCustomProviderId)) { "Provider ids must be unique, lowercase, and non-reserved" }
        require(config.provider.values.all { isSafeProviderEndpoint(it.baseUrl) }) { "Provider URLs must use HTTPS or local HTTP" }
        val encoded = json.encodeToString(ProvidersConfig.serializer(), config)
        require(encoded.toByteArray().size <= MAX_CONFIG_BYTES) { "Provider configuration is too large" }
        configDir.mkdirs()
        file.writeTextAtomically(encoded)
    }

    fun fingerprint(): String {
        if (!file.isFile) return "missing"
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8_192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    val path: String get() = file.absolutePath

    private companion object {
        const val MAX_CONFIG_BYTES = 512L * 1024L
    }
}

/** Map a custom provider entry to a [ProviderPreset] the ProviderFactory can construct. */
fun CustomProvider.toPreset(id: String): ProviderPreset {
    require(isSafeCustomProviderId(id)) { "Invalid custom provider id" }
    require(isSafeProviderEndpoint(baseUrl)) { "Provider URLs must use HTTPS or local HTTP" }
    val anthropic = format.equals("anthropic", ignoreCase = true)
    return ProviderPreset(
        id = id,
        displayName = name.ifBlank { id },
        baseUrl = baseUrl,
        wireFormat = if (anthropic) WireFormat.ANTHROPIC else WireFormat.OPENAI_COMPAT,
        authScheme = if (anthropic) AuthScheme.X_API_KEY else AuthScheme.BEARER,
        extraHeaders = if (anthropic) mapOf("anthropic-version" to "2023-06-01") else emptyMap(),
    )
}

fun isSafeCustomProviderId(value: String): Boolean =
    value.matches(Regex("[a-z0-9][a-z0-9-]{0,63}")) &&
        BuiltInPresets.byId(value) == null &&
        listOf("git.", "codex.", "github.", "mcp.", "provider.").none(value::startsWith)

fun customProviderSecretName(providerId: String): String = "provider.custom.$providerId"

fun isSafeProviderEndpoint(value: String): Boolean {
    val uri = runCatching { URI(value) }.getOrNull() ?: return false
    if (uri.host.isNullOrBlank() || uri.userInfo != null) return false
    val host = uri.host.lowercase().removePrefix("[").removeSuffix("]")
    return uri.scheme.equals("https", true) || uri.scheme.equals("http", true) &&
        host in setOf("localhost", "127.0.0.1", "::1")
}
