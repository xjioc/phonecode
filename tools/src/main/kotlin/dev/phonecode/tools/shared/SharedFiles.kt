package dev.phonecode.tools.shared

import dev.phonecode.tools.Tool
import dev.phonecode.tools.ToolContext
import dev.phonecode.tools.ToolResult
import dev.phonecode.tools.files.int
import dev.phonecode.tools.files.intSchema
import dev.phonecode.tools.files.objectSchema
import dev.phonecode.tools.files.str
import dev.phonecode.tools.files.strSchema
import kotlinx.serialization.json.JsonObject

data class SharedRoot(val id: String, val name: String, val writable: Boolean)

data class SharedEntry(val name: String, val directory: Boolean, val size: Long?)

interface SharedFileAccess {
    fun roots(): List<SharedRoot>
    suspend fun list(rootId: String, path: String): List<SharedEntry>
    suspend fun read(rootId: String, path: String, maxBytes: Int): String
    suspend fun write(rootId: String, path: String, content: String)
    suspend fun mkdir(rootId: String, path: String)
    suspend fun delete(rootId: String, path: String)
    suspend fun rename(rootId: String, path: String, newName: String)
}

fun sharedPathSegments(path: String): List<String> {
    require(!path.startsWith('/') && !path.startsWith('\\')) { "path must be relative" }
    return path.replace('\\', '/').split('/').filter { it.isNotEmpty() && it != "." }.also { segments ->
        require(segments.none { it == ".." || '\u0000' in it }) { "path escapes the linked folder" }
    }
}

class SharedReadTool(private val access: SharedFileAccess) : Tool {
    override val name = "shared_files"
    override val description =
        "List linked phone folders, list a directory, or read a UTF-8 file. Only folders linked in Settings are reachable."
    override val promptSnippet = "browse and read user-linked phone folders"
    override val promptGuidelines = listOf(
        "Call shared_files with action=roots before using a linked folder.",
        "Linked folders are provider-backed and are not shell paths.",
    )
    override val parameters: JsonObject = objectSchema(
        mapOf(
            "action" to strSchema("roots, list, or read"),
            "root" to strSchema("Linked folder id returned by action=roots"),
            "path" to strSchema("Path relative to the linked folder; empty means its root"),
            "maxBytes" to intSchema("Maximum bytes returned by read; default 1000000, max 5000000"),
        ),
        required = listOf("action"),
    )

    override suspend fun execute(args: JsonObject, context: ToolContext): ToolResult = runCatching {
        when (val action = args.str("action")) {
            "roots" -> {
                val roots = access.roots()
                ToolResult(
                    roots.joinToString("\n") { "${it.id}\t${it.name}\t${if (it.writable) "read-write" else "read-only"}" }
                        .ifEmpty { "(no linked folders; link one in Settings > Files & permissions)" },
                )
            }
            "list" -> {
                val root = args.str("root") ?: return ToolResult("shared_files: missing 'root'", true)
                val path = args.str("path").orEmpty()
                val entries = access.list(root, path)
                ToolResult(
                    entries.joinToString("\n") {
                        if (it.directory) "${it.name}/" else if (it.size != null) "${it.name}\t${it.size} bytes" else it.name
                    }.ifEmpty { "(empty directory)" },
                )
            }
            "read" -> {
                val root = args.str("root") ?: return ToolResult("shared_files: missing 'root'", true)
                val path = args.str("path")?.takeIf { it.isNotBlank() }
                    ?: return ToolResult("shared_files: missing 'path'", true)
                val cap = (args.int("maxBytes") ?: 1_000_000).coerceIn(1, 5_000_000)
                ToolResult(access.read(root, path, cap))
            }
            else -> ToolResult("shared_files: unknown action '$action'", true)
        }
    }.getOrElse { ToolResult("shared_files: ${it.message}", true) }
}

class SharedWriteTool(private val access: SharedFileAccess) : Tool {
    override val name = "shared_files_write"
    override val description =
        "Write, create, delete, or rename items inside a user-linked phone folder. Changes follow the configured permission policy."
    override val mutating = true
    override val promptSnippet = "change files inside user-linked phone folders"
    override val promptGuidelines = listOf(
        "Use shared_files to discover linked folder ids and inspect content first.",
        "Paths are relative to the selected folder and cannot escape it.",
    )
    override val parameters: JsonObject = objectSchema(
        mapOf(
            "action" to strSchema("write, mkdir, delete, or rename"),
            "root" to strSchema("Linked folder id returned by shared_files action=roots"),
            "path" to strSchema("Path relative to the linked folder"),
            "content" to strSchema("Full UTF-8 content for write"),
            "newName" to strSchema("New item name for rename"),
        ),
        required = listOf("action", "root", "path"),
    )

    override suspend fun execute(args: JsonObject, context: ToolContext): ToolResult {
        val action = args.str("action") ?: return ToolResult("shared_files_write: missing 'action'", true)
        val root = args.str("root") ?: return ToolResult("shared_files_write: missing 'root'", true)
        val path = args.str("path")?.takeIf { it.isNotBlank() }
            ?: return ToolResult("shared_files_write: missing 'path'", true)
        return runCatching {
            when (action) {
                "write" -> {
                    val content = args.str("content") ?: return ToolResult("shared_files_write: missing 'content'", true)
                    access.write(root, path, content)
                    ToolResult("wrote ${content.toByteArray().size} bytes to $path")
                }
                "mkdir" -> {
                    access.mkdir(root, path)
                    ToolResult("created $path/")
                }
                "delete" -> {
                    access.delete(root, path)
                    ToolResult("deleted $path")
                }
                "rename" -> {
                    val newName = args.str("newName")?.takeIf { it.isNotBlank() }
                        ?: return ToolResult("shared_files_write: missing 'newName'", true)
                    access.rename(root, path, newName)
                    ToolResult("renamed $path to $newName")
                }
                else -> ToolResult("shared_files_write: unknown action '$action'", true)
            }
        }.getOrElse { ToolResult("shared_files_write: ${it.message}", true) }
    }
}
