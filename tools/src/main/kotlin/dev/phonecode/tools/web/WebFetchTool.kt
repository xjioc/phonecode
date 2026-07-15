package dev.phonecode.tools.web

import dev.phonecode.tools.Tool
import dev.phonecode.tools.ToolContext
import dev.phonecode.tools.ToolResult
import dev.phonecode.tools.http.awaitResponse
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.TimeUnit

internal fun safeWebUrl(url: HttpUrl): Boolean = url.isHttps || url.scheme == "http" &&
    url.host.lowercase() in setOf("localhost", "127.0.0.1", "::1")

internal fun OkHttpClient.webToolClient(): OkHttpClient = newBuilder()
    .callTimeout(90, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .followSslRedirects(false)
    .addNetworkInterceptor { chain ->
        require(safeWebUrl(chain.request().url)) { "remote web requests must use HTTPS" }
        chain.proceed(chain.request())
    }
    .build()

/**
 * Fetches a URL over HTTP(S) and returns its content as text (mirrors OpenCode `webfetch`). HTML is reduced
 * to readable text unless `format=html`. Read-only research: visible in PLAN, no permission prompt - but
 * every call shows up as a tool-activity line with its URL, so the user always sees what was fetched.
 */
class WebFetchTool(http: OkHttpClient) : Tool {
    private val webHttp = http.webToolClient()
    override val name = "webfetch"
    override val description =
        "Fetch an HTTPS URL or local HTTP URL and return its content. HTML is converted to readable text by default; " +
            "pass format=html for raw HTML or format=text/markdown for text. Use to read docs or web pages."
    override val promptSnippet = "fetch a URL and read its content as text"
    override val parameters: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("url") { put("type", "string"); put("description", "The HTTPS URL, or localhost HTTP URL, to fetch.") }
            putJsonObject("format") {
                put("type", "string")
                put("description", "text (default) | markdown | html")
            }
        }
        put("required", buildJsonArray { add("url") })
        put("additionalProperties", false)
    }

    override suspend fun execute(args: JsonObject, context: ToolContext): ToolResult {
        val url = (args["url"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: return ToolResult("webfetch: missing 'url'", isError = true)
        val target = url.toHttpUrlOrNull()
        if (target == null || !safeWebUrl(target)) return ToolResult(
            "webfetch: use HTTPS, or HTTP only for localhost",
            isError = true,
        )
        val format = (args["format"] as? JsonPrimitive)?.content ?: "text"
        return try {
            val request = Request.Builder().url(target).header("User-Agent", USER_AGENT).get().build()
            webHttp.newCall(request).awaitResponse { response ->
                if (!response.isSuccessful) {
                    return@awaitResponse ToolResult("webfetch: HTTP ${response.code} for $url", isError = true)
                }
                val body = response.peekBody(MAX_BYTES).string()
                val contentType = response.header("Content-Type").orEmpty()
                val rendered = if (format == "html" || !(contentType.contains("html", true) || looksLikeHtml(body))) {
                    body
                } else {
                    htmlToText(body)
                }
                val output = if (rendered.length > MAX_CHARS) {
                    rendered.take(MAX_CHARS) + "\n\n[truncated at $MAX_CHARS characters]"
                } else {
                    rendered
                }
                ToolResult(output.ifBlank { "(empty response)" })
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            ToolResult("webfetch: ${error.message}", isError = true)
        }
    }

    private fun looksLikeHtml(body: String): Boolean {
        val head = body.trimStart().take(200).lowercase()
        return head.startsWith("<!doctype html") || head.startsWith("<html") || head.contains("<body")
    }

    private fun htmlToText(html: String): String {
        var s = html
        s = s.replace(SCRIPT, " ").replace(STYLE, " ").replace(HEAD, " ")
        s = s.replace(TAG, " ")
        ENTITIES.forEach { (from, to) -> s = s.replace(from, to) }
        s = s.replace(HORIZONTAL_WS, " ").replace(EXTRA_BLANK_LINES, "\n\n")
        return s.trim()
    }

    private companion object {
        const val MAX_CHARS = 100_000
        const val MAX_BYTES = 5_000_000L // raw-body read cap (HTML only shrinks once stripped to text)
        const val USER_AGENT = "PhoneCode/0.4 (+https://dttdrv.xyz/phonecode)"
        val SCRIPT = Regex("(?is)<script.*?</script>")
        val STYLE = Regex("(?is)<style.*?</style>")
        val HEAD = Regex("(?is)<head.*?</head>")
        val TAG = Regex("(?s)<[^>]+>")
        val HORIZONTAL_WS = Regex("[ \\t]+")
        val EXTRA_BLANK_LINES = Regex("\\n\\s*\\n\\s*\\n+")
        val ENTITIES = listOf(
            "&nbsp;" to " ", "&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"", "&#39;" to "'", "&apos;" to "'",
        )
    }
}
