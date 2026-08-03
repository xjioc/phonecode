package dev.phonecode.tools.git

import dev.phonecode.tools.Tool
import dev.phonecode.tools.ToolContext
import dev.phonecode.tools.ToolResult
import dev.phonecode.tools.files.bool
import dev.phonecode.tools.files.boolSchema
import dev.phonecode.tools.files.int
import dev.phonecode.tools.files.intSchema
import dev.phonecode.tools.files.objectSchema
import dev.phonecode.tools.files.str
import dev.phonecode.tools.files.strSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

/** Native git via JGit, confined to the workspace. push/pull use [credentials] = (username, token) over HTTPS. */
fun gitTools(credentials: suspend () -> Pair<String, String>?): List<Tool> = listOf(
    GitInitTool(), GitStatusTool(), GitDiffTool(), GitAddTool(), GitCommitTool(),
    GitLogTool(), GitBranchTool(), GitCheckoutTool(), GitPushTool(credentials), GitPullTool(credentials),
)

private val NO_PARAMS = objectSchema(emptyMap(), emptyList())

fun openGit(workspace: File): Git {
    val root = workspace.canonicalFile
    return Git(
        FileRepositoryBuilder()
            .setWorkTree(root)
            .setGitDir(gitDirectory(root))
            .setFS(NoExecFs())
            .setMustExist(true)
            .build(),
    )
}

private fun gitDirectory(workspace: File): File {
    require(workspace.isDirectory) { "workspace is not a directory" }
    val git = File(workspace, ".git")
    require(!Files.isSymbolicLink(git.toPath())) { ".git cannot be a symbolic link" }
    require(git.canonicalPath.startsWith(workspace.canonicalPath + File.separator)) { ".git is outside the workspace" }
    return git
}

/** Open the workspace repo and run [block]; a friendly error if it isn't a repo. */
private suspend fun withRepo(context: ToolContext, name: String, block: (Git) -> String): ToolResult =
    withContext(Dispatchers.IO) {
        runCatching { openGit(File(context.workspacePath)).use { ToolResult(block(it)) } }
            .getOrElse { ToolResult("$name: ${it.message ?: "not a git repository (try git_init)"}", isError = true) }
    }

private fun requireHttpsOrigin(git: Git, push: Boolean = false): List<URIish> {
    val config = git.repository.config
    val fetchUrls = config.getStringList("remote", "origin", "url")
    val pushUrls = config.getStringList("remote", "origin", "pushurl")
    val urls = if (push && pushUrls.isNotEmpty()) pushUrls else fetchUrls
    require(urls.isNotEmpty()) { "origin has no URL" }
    val origins = urls.map(::URIish)
    require(origins.all { it.scheme.equals("https", true) && !it.host.isNullOrBlank() }) {
        "origin must use HTTPS"
    }
    return origins
}

private fun URIish.isGitHub(): Boolean = host.equals("github.com", true)

private fun requireSslVerification(git: Git) {
    val config = git.repository.config
    val scopes = listOf<String?>(null) + config.getSubsections("http")
    require(scopes.all { config.getBoolean("http", it, "sslVerify", true) }) {
        "http.sslVerify must remain enabled for remote operations"
    }
}

internal fun successfulPushStatus(status: RemoteRefUpdate.Status): Boolean =
    status == RemoteRefUpdate.Status.OK || status == RemoteRefUpdate.Status.UP_TO_DATE

private fun Status.render(): String {
    val sb = StringBuilder()
    val staged = added.map { "A  $it" } + changed.map { "M  $it" } + removed.map { "D  $it" }
    val unstaged = modified.map { " M $it" } + missing.map { " D $it" }
    if (staged.isNotEmpty()) sb.append("Staged:\n").append(staged.joinToString("\n") { "  $it" }).append("\n")
    if (unstaged.isNotEmpty()) sb.append("Unstaged:\n").append(unstaged.joinToString("\n") { "  $it" }).append("\n")
    if (untracked.isNotEmpty()) sb.append("Untracked:\n").append(untracked.joinToString("\n") { "  ?? $it" }).append("\n")
    return sb.toString().trimEnd().ifEmpty { "working tree clean" }
}

class GitInitTool : Tool {
    override val name = "git_init"
    override val description = "Initialize a new git repository in the workspace."
    override val mutating = true
    override val promptSnippet = "initialize a git repository in the workspace"
    override val parameters = objectSchema(mapOf("branch" to strSchema("Initial branch name (default: repository default)")), emptyList())
    override suspend fun execute(args: JsonObject, context: ToolContext): ToolResult = withContext(Dispatchers.IO) {
        runCatching {
            val workspace = File(context.workspacePath).canonicalFile
            gitDirectory(workspace)
            val initial = args.str("branch")?.takeIf { it.isNotBlank() }
            val init = Git.init().setDirectory(workspace).setFs(NoExecFs())
            if (initial != null) init.setInitialBranch(initial)
            init.call().use {}
            ToolResult("git: initialized repository" + if (initial != null) " (branch: $initial)" else "")
        }
            .getOrElse { ToolResult("git_init: ${it.message}", isError = true) }
    }
}

class GitStatusTool : Tool {
    override val name = "git_status"
    override val description = "Show the working-tree status (staged, unstaged, and untracked files)."
    override val promptSnippet = "show git status"
    override val parameters = NO_PARAMS
    override suspend fun execute(args: JsonObject, context: ToolContext) = withRepo(context, name) { it.status().call().render() }
}

class GitDiffTool : Tool {
    override val name = "git_diff"
    override val description = "Show the diff. Defaults to unstaged (working tree vs index); pass staged=true for staged changes. " +
        "Optionally filter by path, or list changed files instead of full diffs with nameOnly/nameStatus (nameStatus wins if both are set)."
    override val promptSnippet = "show the git diff (unstaged, or staged)"
    override val parameters = objectSchema(
        mapOf(
            "staged" to boolSchema("true to show staged changes (default false)"),
            "path" to strSchema("Only show changes under this file or directory path (optional)"),
            "nameOnly" to boolSchema("true to list only changed file names (default false)"),
            "nameStatus" to boolSchema("true to list changed files with status letters A/M/D (default false)"),
        ),
        emptyList(),
    )
    override suspend fun execute(args: JsonObject, context: ToolContext): ToolResult {
        val staged = args.bool("staged") == true
        val path = args.str("path")?.takeIf { it.isNotBlank() }
        val nameOnly = args.bool("nameOnly") == true
        val nameStatus = args.bool("nameStatus") == true
        return withRepo(context, name) { git ->
            val out = ByteArrayOutputStream()
            git.diff()
                .setCached(staged)
                .apply {
                    if (path != null) setPathFilter(org.eclipse.jgit.treewalk.filter.PathFilter.create(path))
                    if (nameOnly) setShowNameOnly(true)
                    if (nameStatus) setShowNameAndStatusOnly(true)
                }
                .setOutputStream(out)
                .call()
            out.toString(Charsets.UTF_8).take(20_000).ifEmpty { "(no changes)" }
        }
    }
}

class GitAddTool : Tool {
    override val name = "git_add"
    override val description = "Stage files for commit. Pass a file or directory path, or omit to stage everything ('.')."
    override val mutating = true
    override val promptSnippet = "stage files for commit (git add)"
    override val parameters = objectSchema(mapOf("path" to strSchema("File or directory path to stage (default: all). A path prefix, not a glob.")), emptyList())
    override suspend fun execute(args: JsonObject, context: ToolContext): ToolResult {
        val pattern = args.str("path")?.takeIf { it.isNotBlank() } ?: "."
        return withRepo(context, name) { git ->
            git.add().addFilepattern(pattern).call()
            git.add().setUpdate(true).addFilepattern(pattern).call() // also stage deletions
            "staged: $pattern"
        }
    }
}

class GitCommitTool : Tool {
    override val name = "git_commit"
    override val description = "Commit the staged changes with a message."
    override val mutating = true
    override val promptSnippet = "commit staged changes with a message"
    override val parameters = objectSchema(
        mapOf(
            "message" to strSchema("The commit message"),
            "author" to strSchema("Author name (optional)"),
            "email" to strSchema("Author email (optional)"),
            "amend" to boolSchema("true to amend the previous commit instead of creating a new one (default false)"),
            "allowEmpty" to boolSchema("true to allow a commit with no changes (default false)"),
        ),
        required = listOf("message"),
    )
    override suspend fun execute(args: JsonObject, context: ToolContext): ToolResult {
        val message = args.str("message") ?: return ToolResult("git_commit: missing 'message'", isError = true)
        val author = args.str("author")?.takeIf { it.isNotBlank() } ?: "PhoneCode"
        val email = args.str("email")?.takeIf { it.isNotBlank() } ?: "agent@phonecode.dev"
        val amend = args.bool("amend") == true
        val allowEmpty = args.bool("allowEmpty") == true
        return withRepo(context, name) { git ->
            val commit = git.commit()
                .setMessage(message)
                .setAuthor(author, email)
                .setNoVerify(true)
                .setAmend(amend)
                .setAllowEmpty(allowEmpty)
                .call()
            "committed ${commit.name.take(8)}: ${commit.shortMessage}"
        }
    }
}

class GitLogTool : Tool {
    override val name = "git_log"
    override val description = "Show recent commits (hash, author, message). Can filter by path, include all refs, and skip commits for paging."
    override val promptSnippet = "show recent git commits"
    override val parameters = objectSchema(
        mapOf(
            "count" to intSchema("How many commits (default 15)"),
            "path" to strSchema("Only show commits touching this file or directory path (optional)"),
            "all" to boolSchema("true to include commits from all refs, not just HEAD (default false)"),
            "skip" to intSchema("How many commits to skip, for paging (default 0)"),
        ),
        emptyList(),
    )
    override suspend fun execute(args: JsonObject, context: ToolContext): ToolResult {
        val count = args.int("count")?.coerceIn(1, 100) ?: 15
        val path = args.str("path")?.takeIf { it.isNotBlank() }
        val all = args.bool("all") == true
        val skip = (args.int("skip") ?: 0).coerceIn(0, 1000)
        return withRepo(context, name) { git ->
            git.log().apply {
                setMaxCount(count)
                if (path != null) addPath(path)
                if (all) all()
                if (skip > 0) setSkip(skip)
            }.call().joinToString("\n") { c ->
                "${c.name.take(8)}  ${c.authorIdent.name}  ${c.shortMessage}"
            }.ifEmpty { "(no commits yet)" }
        }
    }
}

class GitBranchTool : Tool {
    override val name = "git_branch"
    override val description = "List branches (remote=true includes remote-tracking branches), create with name=<branch>, or delete with delete=<branch>."
    override fun mutates(args: JsonObject): Boolean =
        !args.str("name").isNullOrBlank() || !args.str("delete").isNullOrBlank()
    override val promptSnippet = "list, create, or delete git branches"
    override val parameters = objectSchema(
        mapOf(
            "name" to strSchema("New branch name to create (omit to just list)"),
            "delete" to strSchema("Branch name to delete (omit unless deleting)"),
            "force" to boolSchema("true to force-delete a branch that is not fully merged (default false)"),
            "remote" to boolSchema("true to include remote-tracking branches in the list (default false)"),
        ),
        emptyList(),
    )
    override suspend fun execute(args: JsonObject, context: ToolContext): ToolResult {
        val create = args.str("name")?.takeIf { it.isNotBlank() }
        val delete = args.str("delete")?.takeIf { it.isNotBlank() }
        val force = args.bool("force") == true
        val remote = args.bool("remote") == true
        return withRepo(context, name) { git ->
            when {
                create != null -> {
                    git.branchCreate().setName(create).call()
                    "created branch $create"
                }
                delete != null -> {
                    git.branchDelete().setBranchNames(delete).setForce(force).call()
                    "deleted branch $delete"
                }
                else -> {
                    val current = git.repository.branch
                    git.branchList().apply {
                        if (remote) setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.ALL)
                    }.call().joinToString("\n") { ref ->
                        val short = ref.name.removePrefix("refs/heads/").removePrefix("refs/remotes/")
                        if (short == current) "* $short" else "  $short"
                    }
                }
            }
        }
    }
}

class GitCheckoutTool : Tool {
    override val name = "git_checkout"
    override val description = "Switch to an existing branch (or create+switch with create=true), restore working-tree files from the index with path=<file>, or force-switch discarding local changes with force=true."
    override val mutating = true
    override val promptSnippet = "switch git branches (checkout)"
    override val parameters = objectSchema(
        mapOf(
            "name" to strSchema("Branch to switch to (omit to restore files instead)"),
            "create" to boolSchema("true to create the branch first"),
            "path" to strSchema("Restore this file or directory from the index (git checkout -- <path>); takes precedence over branch switching"),
            "force" to boolSchema("true to discard local changes when switching (default false)"),
        ),
        emptyList(),
    )
    override suspend fun execute(args: JsonObject, context: ToolContext): ToolResult {
        val branch = args.str("name")?.takeIf { it.isNotBlank() }
        val create = args.bool("create") == true
        val path = args.str("path")?.takeIf { it.isNotBlank() }
        val force = args.bool("force") == true
        return withRepo(context, name) { git ->
            if (path != null) {
                git.checkout().addPath(path).call()
                "restored: $path"
            } else {
                val target = branch ?: return@withRepo "git_checkout: missing 'name' or 'path'"
                git.checkout().setName(target).setCreateBranch(create).setForced(force).call()
                "switched to $target"
            }
        }
    }
}

class GitPushTool(private val credentials: suspend () -> Pair<String, String>?) : Tool {
    override val name = "git_push"
    override val description = "Push the current branch to the remote (origin) over HTTPS, using the configured git credentials. " +
        "Can force-push, push tags, or dry-run."
    override val mutating = true
    override val promptSnippet = "push commits to the remote"
    override val parameters = objectSchema(
        mapOf(
            "force" to boolSchema("true to force-push, overwriting remote history (default false)"),
            "tags" to boolSchema("true to also push tags (default false)"),
            "dryRun" to boolSchema("true to simulate the push without sending anything (default false)"),
        ),
        emptyList(),
    )
    override suspend fun execute(args: JsonObject, context: ToolContext): ToolResult {
        val creds = credentials() ?: return ToolResult("git_push: no git credentials set (add a username + token in Settings)", isError = true)
        val force = args.bool("force") == true
        val tags = args.bool("tags") == true
        val dryRun = args.bool("dryRun") == true
        return withRepo(context, name) { git ->
            require(requireHttpsOrigin(git, push = true).all(URIish::isGitHub)) { "origin must use GitHub HTTPS" }
            requireSslVerification(git)
            val failures = git.push().setRemote("origin")
                .setCredentialsProvider(UsernamePasswordCredentialsProvider(creds.first, creds.second))
                .setForce(force)
                .setDryRun(dryRun)
                .apply { if (tags) setPushTags() }
                .call()
                .flatMap { it.remoteUpdates }
                .filterNot { successfulPushStatus(it.status) }
            require(failures.isEmpty()) {
                "push rejected: ${failures.joinToString { "${it.remoteName} ${it.status}" }}"
            }
            if (dryRun) "dry-run: push would succeed" else "pushed to origin"
        }
    }
}

class GitPullTool(private val credentials: suspend () -> Pair<String, String>?) : Tool {
    override val name = "git_pull"
    override val description = "Pull from the remote (origin) over HTTPS, using the configured git credentials. Pass rebase=true for a rebase pull."
    override val mutating = true
    override val promptSnippet = "pull from the remote"
    override val parameters = objectSchema(
        mapOf("rebase" to boolSchema("true to rebase local commits onto the remote instead of merging (default false)")),
        emptyList(),
    )
    override suspend fun execute(args: JsonObject, context: ToolContext): ToolResult {
        val creds = credentials()
        val rebase = args.bool("rebase") == true
        return withRepo(context, name) { git ->
            val origins = requireHttpsOrigin(git)
            requireSslVerification(git)
            val pull = git.pull().setRemote("origin").setRebase(rebase)
            if (creds != null && origins.all(URIish::isGitHub)) {
                pull.setCredentialsProvider(UsernamePasswordCredentialsProvider(creds.first, creds.second))
            }
            val result = pull.call()
            if (result.isSuccessful) "pulled from origin" else "pull completed with conflicts"
        }
    }
}
