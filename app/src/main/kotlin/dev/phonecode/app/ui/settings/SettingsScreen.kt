@file:SuppressLint("LocalContextGetResourceValueCall")

package dev.phonecode.app.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.ripple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import dev.phonecode.app.agent.ChatViewModel
import dev.phonecode.app.R
import dev.phonecode.app.data.CustomModel
import dev.phonecode.app.data.CustomProvider
import dev.phonecode.app.data.ManagedSkill
import dev.phonecode.app.data.isSafeMcpEndpoint
import dev.phonecode.app.data.isSafeCustomProviderId
import dev.phonecode.app.data.isSafeProviderEndpoint
import dev.phonecode.app.data.SkillScope
import dev.phonecode.app.data.SkillStatus
import dev.phonecode.app.data.ThemeMode
import dev.phonecode.app.ui.SettingsViewModel
import dev.phonecode.app.ui.chat.MarkdownBlocks
import dev.phonecode.app.ui.components.PcButton
import dev.phonecode.app.ui.components.PcField
import dev.phonecode.app.ui.components.PcGroup
import dev.phonecode.app.ui.components.PcIconButton
import dev.phonecode.app.ui.components.PcRow
import dev.phonecode.app.ui.components.PcSectionLabel
import dev.phonecode.app.ui.components.PcToggle
import dev.phonecode.app.ui.components.contentVerticalScroll
import dev.phonecode.app.ui.components.predictiveBackTransform
import dev.phonecode.app.ui.components.pressFeedback
import dev.phonecode.app.ui.components.rememberPredictiveBackMotion
import dev.phonecode.app.ui.theme.PcMono
import dev.phonecode.app.ui.theme.PhoneEasings
import dev.phonecode.app.ui.theme.PhoneSprings
import dev.phonecode.app.ui.theme.ShapePill
import dev.phonecode.app.ui.theme.Spacing
import dev.phonecode.agent.AgentMode
import dev.phonecode.tools.mcp.McpServerConfig
import dev.phonecode.tools.mcp.McpServerSnapshot
import java.text.SimpleDateFormat
import java.security.MessageDigest
import java.util.Date
import java.util.Locale

/** The page each settings route pops back to - doc pages return to About, provider details to
 *  Providers. The old "always home" pop sent the back GESTURE somewhere different from the back
 *  BUTTON on nested pages (round-4: "sometimes it brings back to settings... sometimes the About"). */
private fun parentOf(page: String): String = when {
    page.startsWith("doc:") -> "about"
    page.startsWith("provider:") -> "providers"
    else -> "home"
}

/** Nav depth per route - the slide direction keys off depth, so popping doc → about animates as a
 *  POP even though neither side is "home" (the old home-test played nested pops backwards). */
private fun depthOf(page: String): Int = when {
    page == "home" -> 0
    page.startsWith("doc:") || page.startsWith("provider:") -> 2
    else -> 1
}

private fun revisionOf(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }

private fun revisionOf(config: McpServerConfig): String = revisionOf(buildString {
    append(config.type).append('\u0000').append(config.url).append('\u0000')
    config.headers.toSortedMap().forEach { (name, value) ->
        append(name).append('\u0000').append(value).append('\u0000')
    }
    append(config.enabled).append('\u0000').append(config.timeout)
})

/** Settings: a home list + every sub-page, navigated with an iOS-style slide.
 *  [initialPage] lets callers (onboarding) deep-link straight to a sub-page. */
@Composable
fun SettingsScreen(vm: ChatViewModel, settingsVm: SettingsViewModel, onBack: () -> Unit, initialPage: String = "home") {
    var page by rememberSaveable(initialPage) { mutableStateOf(initialPage) }
    var predictiveCommit by remember { mutableStateOf(false) }
    var nestedBackActive by remember { mutableStateOf(false) }
    val navigateBack = {
        if (page == initialPage) onBack() else page = parentOf(page)
    }

    val backMotion = rememberPredictiveBackMotion(enabled = page != initialPage && !nestedBackActive) {
        predictiveCommit = true
        navigateBack()
    }
    LaunchedEffect(page) {
        predictiveCommit = false
        nestedBackActive = false
    }

    Box(Modifier.fillMaxSize()) {
        if (backMotion.progress > 0f) {
            Box(Modifier.fillMaxSize()) {
                SettingsPageContent(parentOf(page), vm, settingsVm, onBack = {}, navigate = {}, onNestedBackActive = {})
            }
        }
        Box(Modifier.fillMaxSize().predictiveBackTransform(backMotion)) {
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (predictiveCommit) {
                        androidx.compose.animation.EnterTransition.None togetherWith
                            androidx.compose.animation.ExitTransition.None
                    } else {
                        val pop = depthOf(targetState) < depthOf(initialState)
                        (if (pop) {
                            (slideInHorizontally(tween(260, easing = PhoneEasings.iOSStandard)) { -it / 4 }) togetherWith
                                slideOutHorizontally(tween(220, easing = PhoneEasings.iOSStandard)) { it }
                        } else {
                            (slideInHorizontally(tween(260, easing = PhoneEasings.iOSStandard)) { it }) togetherWith
                                slideOutHorizontally(tween(220, easing = PhoneEasings.iOSStandard)) { -it / 4 }
                        }).apply { targetContentZIndex = if (pop) -1f else 1f }
                    }
                },
                label = "settings",
            ) { p ->
                Box(Modifier.fillMaxSize()) {
                    SettingsPageContent(p, vm, settingsVm, navigateBack, { page = it }) { nestedBackActive = it }
                }
            }
        }
    }
}

@Composable
private fun SettingsPageContent(
    page: String,
    vm: ChatViewModel,
    settingsVm: SettingsViewModel,
    onBack: () -> Unit,
    navigate: (String) -> Unit,
    onNestedBackActive: (Boolean) -> Unit,
) {
    when (page) {
        "general" -> GeneralPage(settingsVm, onBack)
        "appearance" -> AppearancePage(settingsVm, onBack)
        "personal" -> PersonalPage(settingsVm, onBack)
        "providers" -> ProvidersPage(vm, onOpenProvider = { navigate("provider:$it") }, onBack = onBack)
        "mcp" -> McpPage(vm, onBack, onNestedBackActive)
        "skills" -> SkillsPage(vm, onBack, onNestedBackActive)
        "tools" -> AgentToolsPage(vm, onBack)
        "files" -> FilesPage(vm, settingsVm, onBack)
        "git" -> GitPage(vm, settingsVm, onBack)
        "export" -> ExportPage(vm, settingsVm, onBack)
        "about" -> AboutPage(vm, onOpenDoc = navigate, onBack = onBack)
        "doc:terms" -> DocPage(stringResource(R.string.settings_terms_of_service), "terms.md", onBack)
        "doc:privacy" -> DocPage(stringResource(R.string.settings_privacy_policy), "privacy.md", onBack)
        "doc:licenses" -> DocPage(stringResource(R.string.settings_open_source_notices), "licenses.md", onBack)
        else -> {
            if (page.startsWith("provider:")) {
                ProviderDetailPage(vm, page.removePrefix("provider:"), onBack)
            } else {
                HomePage(vm, settingsVm, onBack = onBack, onOpen = navigate)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Scaffolding
// ---------------------------------------------------------------------------------------------

@Composable
private fun Page(
    title: String,
    onBack: () -> Unit,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scrollState.value > 0 } }
    Box(Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Column(
            Modifier.fillMaxSize()
                .contentVerticalScroll(scrollState)
                .background(colors.background)
                .padding(start = Spacing.m, end = Spacing.m, top = Spacing.navBarHeight + 4.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
        ) {
            content()
            Spacer(Modifier.height(Spacing.xxl))
        }
        Row(
            Modifier.fillMaxWidth().height(Spacing.navBarHeight)
                .shadow(if (scrolled) 2.dp else 0.dp, RectangleShape, clip = false)
                .background(colors.background)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PcIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back), onClick = onBack)
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            if (action == null) Spacer(Modifier.width(Spacing.touchTarget)) else Box(Modifier.width(Spacing.touchTarget)) { action() }
        }
    }
}

// M3 Expressive list metrics (round-3 settings redesign): bodyLarge headline + bodyMedium
// supporting text, 24dp leading icon in onSurfaceVariant, trailing chevron - no hairlines,
// the connected-card gaps do the separating.

@Composable
private fun NavRow(label: String, value: String? = null, icon: ImageVector? = null, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    PcRow(onClick = onClick) {
        if (icon != null) Icon(icon, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onBackground, modifier = Modifier.weight(1f))
        if (value != null) Text(value, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.tertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ToggleRow(label: String, sub: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    PcRow(onClick = { onChange(!checked) }) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onBackground)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 1.dp))
        }
        PcToggle(checked, onChange, "$label ${if (checked) stringResource(R.string.settings_on) else stringResource(R.string.settings_off_label)}")
    }
}

@Composable
private fun CheckRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    PcRow(onClick = onClick) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onBackground, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, null, tint = colors.onBackground, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Note(text: String) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth().padding(top = Spacing.xs).clip(MaterialTheme.shapes.medium)
            .background(colors.surface).padding(14.dp),
    ) { Text(text, style = MaterialTheme.typography.labelMedium, color = colors.secondary) }
}

// ---------------------------------------------------------------------------------------------
// Pages
// ---------------------------------------------------------------------------------------------

@Composable
private fun HomePage(vm: ChatViewModel, settingsVm: SettingsViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    Page(stringResource(R.string.common_settings), onBack) {
        // The first group carries no label - an unlabeled lead group is the platform convention
        // (Grok's settings open straight into the Customize cards).
        PcGroup {
            NavRow(stringResource(R.string.common_general), icon = Icons.Outlined.Tune) { onOpen("general") }
            NavRow(stringResource(R.string.common_appearance), value = settings.mode.name.lowercase().replaceFirstChar { it.uppercase() }, icon = Icons.Outlined.Palette) { onOpen("appearance") }
            NavRow(stringResource(R.string.common_personalization), icon = Icons.Outlined.Person) { onOpen("personal") }
        }
        PcSectionLabel(stringResource(R.string.common_models))
        PcGroup {
            NavRow(stringResource(R.string.common_providers), icon = Icons.Outlined.Cloud) { onOpen("providers") }
        }
        PcSectionLabel(stringResource(R.string.common_tools))
        PcGroup {
            NavRow(stringResource(R.string.settings_agent_tools), value = vm.availableTools().size.toString(), icon = Icons.Outlined.Build) { onOpen("tools") }
            NavRow(stringResource(R.string.settings_mcp_servers), value = "${state.mcpServers.size}", icon = Icons.Outlined.Extension) { onOpen("mcp") }
            NavRow(
                stringResource(R.string.common_skills),
                value = state.skills.count { it.status == SkillStatus.ACTIVE }.toString(),
                icon = Icons.Outlined.AutoAwesome,
            ) { onOpen("skills") }
        }
        // "GIT > Git" was the same duplication as the old GENERAL group - the workspace label
        // says what the section governs (per-project repos), the row keeps the familiar name.
        PcSectionLabel(stringResource(R.string.common_workspace))
        PcGroup {
            NavRow(
                stringResource(R.string.settings_files_permissions),
                value = if (state.sharedFolders.isEmpty()) stringResource(R.string.common_private) else stringResource(R.string.settings_folders_linked, state.sharedFolders.size),
                icon = Icons.Outlined.Folder,
            ) { onOpen("files") }
            NavRow(stringResource(R.string.common_git), icon = Icons.Outlined.AccountTree) { onOpen("git") }
        }
        PcSectionLabel(stringResource(R.string.common_data))
        PcGroup {
            NavRow(stringResource(R.string.settings_export_import), icon = Icons.Outlined.SwapVert) { onOpen("export") }
        }
        Spacer(Modifier.height(8.dp))
        PcGroup {
            NavRow(stringResource(R.string.common_about), icon = Icons.Outlined.Info) { onOpen("about") }
        }
    }
}

@Composable
private fun AgentToolsPage(vm: ChatViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val inventory = remember(state.mcpToolCount, state.skills.size) { vm.availableTools() }
    val tools = remember(inventory, query) {
        inventory.filter {
            query.isBlank() || it.name.contains(query, true) || it.description.contains(query, true) || it.source.contains(query, true)
        }
    }
    val colors = MaterialTheme.colorScheme
    Page(stringResource(R.string.settings_agent_tools), onBack) {
        Note(stringResource(R.string.settings_agent_tools_note, inventory.size))
        PcField(query, { query = it }, stringResource(R.string.common_search_tools))
        tools.groupBy { it.source }.forEach { (source, entries) ->
            PcSectionLabel(source)
            PcGroup {
                entries.forEach { tool ->
                    PcRow {
                        Column(Modifier.weight(1f)) {
                            Text(tool.name, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PcMono), color = colors.onBackground)
                            Text(tool.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Text(tool.access, style = MaterialTheme.typography.labelSmall, color = colors.tertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralPage(settingsVm: SettingsViewModel, onBack: () -> Unit) {
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Page(stringResource(R.string.common_general), onBack) {
        PcSectionLabel(stringResource(R.string.settings_defaults))
        PcGroup {
            AgentMode.entries.forEach { mode ->
                CheckRow(
                    stringResource(R.string.settings_default_mode, mode.name.lowercase().replaceFirstChar { it.uppercase() }),
                    selected = settings.defaultMode == mode.name,
                ) {
                    // Default governs NEW conversations (applied in ChatViewModel.newChat / init); changing
                    // it must not retroactively flip the active chat's mode - that's the per-chat Plan toggle.
                    settingsVm.update { it.copy(defaultMode = mode.name) }
                }
            }
            ToggleRow(stringResource(R.string.settings_send_on_enter), checked = settings.sendOnEnter) { v -> settingsVm.update { it.copy(sendOnEnter = v) } }
        }
        PcSectionLabel(stringResource(R.string.settings_language))
        PcGroup {
            CheckRow(
                stringResource(R.string.settings_language_system),
                selected = settings.language == "SYSTEM",
            ) {
                settingsVm.update { it.copy(language = "SYSTEM") }
                (context as? androidx.activity.ComponentActivity)?.recreate()
            }
            CheckRow(
                stringResource(R.string.settings_language_en),
                selected = settings.language == "en",
            ) {
                settingsVm.update { it.copy(language = "en") }
                (context as? androidx.activity.ComponentActivity)?.recreate()
            }
            CheckRow(
                stringResource(R.string.settings_language_zh),
                selected = settings.language == "zh",
            ) {
                settingsVm.update { it.copy(language = "zh") }
                (context as? androidx.activity.ComponentActivity)?.recreate()
            }
        }
    }
}

@Composable
private fun FilesPage(vm: ChatViewModel, settingsVm: SettingsViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.linkSharedFolder(uri)
    }
    var pendingUnlinkId by rememberSaveable { mutableStateOf<String?>(null) }
    Page(stringResource(R.string.settings_files_permissions), onBack) {
        PcSectionLabel(stringResource(R.string.common_workspace))
        PcGroup {
            PcRow {
                Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_private_workspace), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text(stringResource(R.string.settings_private_workspace_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
            }
        }
        PcSectionLabel(stringResource(R.string.settings_phone_folders))
        if (state.sharedFolders.isNotEmpty()) {
            PcGroup {
                state.sharedFolders.forEach { folder ->
                    PcRow {
                        Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text(folder.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                            Text(if (folder.writable) stringResource(R.string.common_read_write) else stringResource(R.string.common_read_only), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        PcIconButton(Icons.Filled.Delete, stringResource(R.string.settings_cd_remove_folder, folder.name)) { pendingUnlinkId = folder.id }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        PcButton(stringResource(R.string.common_link_folder), filled = false) { picker.launch(null) }
        Note(stringResource(R.string.settings_folder_picker_note))
        PcSectionLabel(stringResource(R.string.settings_agent_changes))
        PcGroup {
            CheckRow(stringResource(R.string.settings_ask_before_changes), selected = !settings.autoAccept) {
                settingsVm.update { it.copy(autoAccept = false) }
                vm.setAutoAccept(false)
            }
            CheckRow(stringResource(R.string.settings_allow_changes_auto), selected = settings.autoAccept) {
                settingsVm.update { it.copy(autoAccept = true) }
                vm.setAutoAccept(true)
            }
        }
        Note(stringResource(R.string.settings_files_note))
        PcSectionLabel(stringResource(R.string.settings_sync))
        PcGroup {
            PcRow {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_sync_parallelism), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text(stringResource(R.string.settings_sync_parallelism_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier.size(32.dp).clip(ShapePill).clickable(
                            indication = ripple(),
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { if (settings.syncParallelism > 1) settingsVm.update { it.copy(syncParallelism = settings.syncParallelism - 1) } },
                        ),
                        contentAlignment = Alignment.Center,
                    ) { Text("-", style = MaterialTheme.typography.titleMedium, color = if (settings.syncParallelism > 1) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)) }
                    Text("${settings.syncParallelism}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.width(24.dp).padding(top = 2.dp).clipToBounds(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Box(
                        Modifier.size(32.dp).clip(ShapePill).clickable(
                            indication = ripple(),
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { if (settings.syncParallelism < 10) settingsVm.update { it.copy(syncParallelism = settings.syncParallelism + 1) } },
                        ),
                        contentAlignment = Alignment.Center,
                    ) { Text("+", style = MaterialTheme.typography.titleMedium, color = if (settings.syncParallelism < 10) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)) }
                }
            }
        }
        state.notice?.let {
            Spacer(Modifier.height(10.dp))
            Note(it)
            LaunchedEffect(it) { delay(3000); vm.clearNotice() }
        }
        state.error?.let {
            Spacer(Modifier.height(10.dp))
            Note(it)
            LaunchedEffect(it) { delay(5000); vm.clearError() }
        }
    }
    pendingUnlinkId?.let { folderId ->
        val folder = state.sharedFolders.firstOrNull { it.id == folderId }
        val projects = state.projects.count { it.folderId == folderId }
        ConfirmActionDialog(
            title = stringResource(R.string.settings_remove_folder_title),
            message = if (projects == 0) {
                stringResource(R.string.settings_remove_folder_msg_no_projects, folder?.name ?: stringResource(R.string.settings_this_folder))
            } else {
                stringResource(R.string.settings_remove_folder_msg_with_projects, folder?.name ?: stringResource(R.string.settings_this_folder), projects)
            },
            action = stringResource(R.string.settings_remove_access),
            onDismiss = { pendingUnlinkId = null },
        ) {
            vm.unlinkSharedFolder(folderId)
            pendingUnlinkId = null
        }
    }
}

@Composable
private fun AppearancePage(settingsVm: SettingsViewModel, onBack: () -> Unit) {
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    Page(stringResource(R.string.common_appearance), onBack) {
        PcSectionLabel(stringResource(R.string.settings_theme))
        PcGroup {
            ThemeMode.entries.forEach { mode ->
                CheckRow(
                    mode.name.lowercase().replaceFirstChar { it.uppercase() },
                    selected = settings.mode == mode,
                ) { settingsVm.update { it.copy(themeMode = mode.name) } }
            }
        }
    }
}

@Composable
private fun PersonalPage(settingsVm: SettingsViewModel, onBack: () -> Unit) {
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    var text by remember(settings.customInstructions) { mutableStateOf(settings.customInstructions) }
    // Persist on a debounce instead of per keystroke (the old onValueChange rewrote the whole settings
    // file on every character). The debounce coalesces typing; the onDispose flush below covers leaving
    // the page within the debounce window so no edit is lost.
    LaunchedEffect(text) {
        if (text != settings.customInstructions) {
            delay(400)
            settingsVm.update { it.copy(customInstructions = text) }
        }
    }
    val latest by rememberUpdatedState(text)
    DisposableEffect(Unit) {
        onDispose { settingsVm.update { if (latest != it.customInstructions) it.copy(customInstructions = latest) else it } }
    }
    Page(stringResource(R.string.common_personalization), onBack) {
        PcSectionLabel(stringResource(R.string.settings_custom_instructions))
        PcField(
            text,
            onValueChange = { text = it },
            placeholder = stringResource(R.string.settings_custom_instructions_placeholder),
            singleLine = false,
            minLines = 5,
        )
    }
}

@Composable
private fun ProvidersPage(vm: ChatViewModel, onOpenProvider: (String) -> Unit, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var addingCustom by remember { mutableStateOf(false) }
    Page(stringResource(R.string.common_providers), onBack) {
        if (!state.codexConnected) {
            PcSectionLabel(stringResource(R.string.settings_chatgpt))
            PcButton(stringResource(R.string.settings_sign_in_chatgpt)) {
                vm.startCodexSignIn()?.let { url ->
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        PcSectionLabel(stringResource(R.string.common_providers))
        if (vm.secureStorageUnavailable()) {
            Text(
                stringResource(R.string.settings_secure_storage_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.error,
                modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs),
            )
        }
        state.providerConfigError?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error)
            Note(stringResource(R.string.settings_providers_json_preserved))
        }
        // Hoist the provider list and the key lookup out of per-recomposition work: keyFor() decrypts from
        // EncryptedSharedPreferences, so doing it per row per frame ran crypto on every toggle/recompose.
        // Keyed on state.models (changes when custom providers reload); a fresh entry after editing a key
        // on the detail page resets this remember because the page leaves the AnimatedContent composition.
        val providers = remember(state.models, state.codexConnected) {
            vm.allProviders().filter { it.id != "codex" || state.codexConnected }
        }
        val keyedIds = remember(state.models) { providers.filter { vm.keyFor(it.id).isNotBlank() }.map { it.id }.toSet() }
        PcGroup {
            providers.forEach { preset ->
                val enabled = preset.id !in state.disabledProviders
                val connected = preset.id == "codex" && state.codexConnected
                val hasKey = connected || preset.id in keyedIds
                PcRow(onClick = { onOpenProvider(preset.id) }) {
                    Column(Modifier.weight(1f)) {
                        Text(preset.displayName, style = MaterialTheme.typography.bodyLarge, color = if (enabled) colors.onBackground else colors.tertiary)
                        Text(
                            if (connected) stringResource(R.string.settings_signed_in_chatgpt) else if (hasKey) stringResource(R.string.settings_key_set) else stringResource(R.string.settings_no_key),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hasKey) colors.onSurfaceVariant else colors.tertiary,
                        )
                    }
                    PcToggle(enabled, { vm.toggleProviderDisabled(preset.id) }, stringResource(R.string.settings_cd_provider_enabled, preset.displayName))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.tertiary, modifier = Modifier.size(20.dp))
                }
            }
        }
        // Custom providers (round-3: "Add the option to add a custom provider") - any
        // OpenAI-compatible or Anthropic-style endpoint, stored in providers.json (the same
        // file the agent can edit), so both paths land in one catalog.
        Spacer(Modifier.height(Spacing.s))
        PcButton(stringResource(R.string.settings_add_custom_provider), filled = false, icon = Icons.Filled.Add, enabled = state.providerConfigError == null) { addingCustom = true }
    }
    if (addingCustom) {
        CustomProviderDialog(
            existingIds = vm.allProviders().map { it.id }.toSet(),
            onSave = vm::saveCustomProvider,
            onDismiss = { addingCustom = false },
        )
    }
}

@Composable
private fun ProviderDetailPage(vm: ChatViewModel, providerId: String, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preset = vm.allProviders().firstOrNull { it.id == providerId }
    var key by remember(providerId) { mutableStateOf(vm.keyFor(providerId)) }
    var confirmRemove by remember(providerId) { mutableStateOf(false) }
    Page(preset?.displayName ?: providerId, onBack) {
        if (providerId == "codex") {
            PcSectionLabel(stringResource(R.string.common_account))
            PcGroup {
                PcRow(onClick = { vm.signOutCodex(); onBack() }) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_chatgpt), style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
                        Text(stringResource(R.string.settings_signed_in), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    Text(stringResource(R.string.settings_disconnect), style = MaterialTheme.typography.labelLarge, color = colors.error)
                }
            }
        } else {
            PcSectionLabel(stringResource(R.string.settings_api_key))
            PcField(key, { key = it; vm.setKey(providerId, it) }, stringResource(R.string.settings_api_key), password = true)
        }
        val models = state.models.filter { it.providerId == providerId }
        PcSectionLabel(stringResource(R.string.settings_models_count, models.size))
        if (models.isEmpty()) {
            Note(stringResource(R.string.settings_no_models_loaded))
        } else {
            // Search + bulk visibility (device feedback): long provider lists need both.
            var modelQuery by remember(providerId) { mutableStateOf("") }
            PcField(modelQuery, { modelQuery = it }, stringResource(R.string.common_search_models))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PcButton(stringResource(R.string.common_all_on), filled = false, modifier = Modifier.weight(1f)) { vm.setAllModelsHidden(models, hidden = false) }
                PcButton(stringResource(R.string.common_all_off), filled = false, modifier = Modifier.weight(1f)) { vm.setAllModelsHidden(models, hidden = true) }
            }
            Spacer(Modifier.height(8.dp))
            val shown = models.filter { modelQuery.isBlank() || it.label.contains(modelQuery, ignoreCase = true) || it.modelId.contains(modelQuery, ignoreCase = true) }
            if (shown.isEmpty()) Note(stringResource(R.string.settings_no_models_match, modelQuery))
            PcGroup {
                shown.forEach { option ->
                    val k = "${option.providerId}/${option.modelId}"
                    val visible = k !in state.hiddenModels
                    PcRow {
                        Text(
                            option.label.substringAfterLast(" · "),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (visible) colors.onBackground else colors.tertiary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        PcToggle(visible, { vm.toggleModelHidden(option) }, stringResource(R.string.settings_cd_model_visible, option.label))
                    }
                }
            }
        }
        // "Discover models" button for built-in providers (non-custom, non-codex)
        if (providerId != "codex" && !vm.isCustomProvider(providerId) && preset != null) {
            Spacer(Modifier.height(6.dp))
            var discoveringBuiltin by remember { mutableStateOf(false) }
            var discoverDialog by remember { mutableStateOf<String?>(null) }
            var discoverError by remember { mutableStateOf<String?>(null) }
            PcButton(
                if (discoveringBuiltin) stringResource(R.string.settings_discovering_models) else stringResource(R.string.settings_discover_models),
                filled = false,
                enabled = !discoveringBuiltin,
            ) {
                val apiKey = vm.keyFor(providerId)
                if (apiKey.isBlank()) {
                    discoverError = context.getString(R.string.settings_discover_no_key)
                    return@PcButton
                }
                discoveringBuiltin = true
                discoverError = null
                scope.launch {
                    try {
                        val (body, code) = withContext(Dispatchers.IO) {
                            val client = OkHttpClient()
                            val reqBuilder = Request.Builder().url("${preset.baseUrl.trimEnd('/')}/models")
                            when (preset.authScheme) {
                                dev.phonecode.provider.preset.AuthScheme.X_API_KEY -> {
                                    reqBuilder.addHeader("x-api-key", apiKey)
                                    preset.extraHeaders.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
                                }
                                else -> reqBuilder.addHeader("Authorization", "Bearer $apiKey")
                            }
                            val response = client.newCall(reqBuilder.build()).execute()
                            Pair(response.body?.string(), response.code)
                        }
                        if (code !in 200..299 || body == null) {
                            discoverError = context.getString(R.string.settings_discover_models_failed, "HTTP $code")
                            discoveringBuiltin = false
                            return@launch
                        }
                        val json = JSONObject(body)
                        val data = json.optJSONArray("data")
                        val discoveredIds = if (data != null) {
                            (0 until data.length()).mapNotNull { i ->
                                data.optJSONObject(i)?.optString("id")?.takeIf { it.isNotBlank() }
                            }
                        } else {
                            emptyList()
                        }
                        if (discoveredIds.isEmpty()) {
                            discoverError = context.getString(R.string.settings_discover_models_none)
                        } else {
                            discoverDialog = discoveredIds.joinToString("\n")
                        }
                    } catch (e: Exception) {
                        discoverError = context.getString(R.string.settings_discover_models_failed, e.message ?: e.toString())
                    } finally {
                        discoveringBuiltin = false
                    }
                }
            }
            discoverError?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.labelSmall, color = colors.error, modifier = Modifier.padding(horizontal = Spacing.m))
            }
            // Dialog showing discovered models
            discoverDialog?.let { idsJoined ->
                Dialog(onDismissRequest = { discoverDialog = null }) {
                    Column(
                        Modifier.fillMaxWidth().shadow(24.dp, MaterialTheme.shapes.extraLarge, clip = false)
                            .clip(MaterialTheme.shapes.extraLarge).background(colors.surfaceContainerHigh).padding(Spacing.m),
                    ) {
                        Text(
                            stringResource(R.string.settings_discover_models_title, preset.displayName),
                            style = MaterialTheme.typography.titleMedium, color = colors.onBackground,
                            modifier = Modifier.padding(bottom = Spacing.s),
                        )
                        val lines = idsJoined.lines()
                        PcField(
                            idsJoined, {}, stringResource(R.string.settings_model_ids_placeholder),
                            singleLine = false, minLines = (lines.size.coerceIn(3, 12)).coerceAtMost(lines.size),
                        )
                        Spacer(Modifier.height(Spacing.s))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            PcButton(stringResource(R.string.common_close)) { discoverDialog = null }
                        }
                    }
                }
            }
        }
        if (vm.isCustomProvider(providerId)) {
            PcSectionLabel(stringResource(R.string.settings_custom_provider))
            PcGroup {
                PcRow(onClick = {
                    if (confirmRemove) {
                        vm.deleteCustomProvider(providerId)
                        onBack()
                    } else {
                        confirmRemove = true
                    }
                }) {
                    Text(
                        if (confirmRemove) stringResource(R.string.settings_tap_again_remove_provider) else stringResource(R.string.settings_remove_provider),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun McpPage(vm: ChatViewModel, onBack: () -> Unit, onNestedBackActive: (Boolean) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var editorDirty by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val closeEditor = {
        if (editorDirty) confirmDiscard = true else editing = null
    }
    val detailBackMotion = rememberPredictiveBackMotion(enabled = editing != null && !confirmDiscard) { closeEditor() }
    DisposableEffect(editing) {
        onNestedBackActive(editing != null)
        onDispose { onNestedBackActive(false) }
    }
    val visible = remember(state.mcpServers, query) {
        state.mcpServers.filter { (name, server) ->
            query.isBlank() || name.contains(query, true) || server.url.contains(query, true)
        }
    }
    Box(Modifier.fillMaxSize()) {
    Box(
        Modifier.fillMaxSize().then(
            if (editing != null) Modifier.clearAndSetSemantics {} else Modifier,
        ),
    ) {
    Page(stringResource(R.string.settings_mcp_servers), onBack) {
        val connected = state.mcpSnapshots.count { it.value.connected }
        Note(stringResource(R.string.settings_mcp_status, connected, state.mcpServers.count { it.value.enabled }, state.mcpToolCount))
        state.mcpConfigError?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error)
            Note(stringResource(R.string.settings_mcp_json_preserved))
        }
        if (state.mcpServers.isNotEmpty()) PcField(query, { query = it }, stringResource(R.string.common_search_servers))
        PcSectionLabel(stringResource(R.string.settings_servers))
        if (state.mcpServers.isEmpty()) {
            Note(stringResource(R.string.settings_no_mcp_servers))
        } else {
            PcGroup {
                visible.entries.forEach { (name, server) ->
                    val snapshot = state.mcpSnapshots[name]
                    val status = when {
                        !server.enabled -> stringResource(R.string.common_off)
                        name in state.mcpConnecting -> stringResource(R.string.common_connecting)
                        snapshot?.connected == true -> stringResource(R.string.settings_mcp_connected_tools, snapshot.tools.size)
                        snapshot?.error?.isNotBlank() == true -> stringResource(R.string.settings_mcp_failed, snapshot.error)
                        else -> stringResource(R.string.common_not_tested)
                    }
                    PcRow(onClick = if (state.mcpConfigError == null) ({ editorDirty = false; editing = name }) else null) {
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
                            Text(
                                status,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (snapshot?.connected == true) colors.primary else colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (state.mcpConfigError == null) PcToggle(server.enabled, { vm.setMcpEnabled(name, it) }, stringResource(R.string.settings_cd_server_enabled, name))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.tertiary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        if (state.mcpConfigError == null) {
            Spacer(Modifier.height(Spacing.s))
            PcButton(stringResource(R.string.settings_add_server), filled = false, icon = Icons.Filled.Add) { editorDirty = false; editing = "" }
            if (state.mcpServers.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.xs))
                PcButton(stringResource(R.string.settings_reconnect_servers), filled = false) { vm.reconnectMcp() }
            }
        }
    }
    }
    editing?.let { initialName ->
        Box(Modifier.fillMaxSize().predictiveBackTransform(detailBackMotion).background(colors.background)) {
            McpServerPage(
                vm = vm,
                initialName = initialName,
                initial = state.mcpServers[initialName] ?: McpServerConfig(),
                existingNames = state.mcpServers.keys,
                snapshot = state.mcpSnapshots[initialName],
                onBack = closeEditor,
                onDirtyChange = { editorDirty = it },
                onSaved = { editorDirty = false; editing = null },
            )
        }
    }
    if (confirmDiscard) {
        ConfirmDiscardDialog(
            message = stringResource(R.string.settings_unsaved_changes_server),
            onKeepEditing = { confirmDiscard = false },
            onDiscard = { confirmDiscard = false; editorDirty = false; editing = null },
        )
    }
    }
}

@Composable
private fun McpServerPage(
    vm: ChatViewModel,
    initialName: String,
    initial: McpServerConfig,
    existingNames: Set<String>,
    snapshot: McpServerSnapshot?,
    onBack: () -> Unit,
    onDirtyChange: (Boolean) -> Unit,
    onSaved: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val isNew = initialName.isEmpty()
    val scope = rememberCoroutineScope()
    var baseline by remember(initialName) { mutableStateOf(initial) }
    val currentRevision = remember(initial) { revisionOf(initial) }
    var acceptedRevision by rememberSaveable(initialName) { mutableStateOf(currentRevision) }
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var url by rememberSaveable(initialName) { mutableStateOf(initial.url) }
    var headers by rememberSaveable(initialName) { mutableStateOf(initial.headers.entries.joinToString("\n") { "${it.key}: ${it.value}" }) }
    var timeout by rememberSaveable(initialName) { mutableStateOf(initial.timeout.toString()) }
    var enabled by rememberSaveable(initialName) { mutableStateOf(initial.enabled) }
    var error by remember(initialName) { mutableStateOf<String?>(null) }
    var testing by remember(initialName) { mutableStateOf(false) }
    var saving by remember(initialName) { mutableStateOf(false) }
    var testResult by remember(initialName) { mutableStateOf<McpServerSnapshot?>(null) }
    var confirmDelete by remember(initialName) { mutableStateOf(false) }
    val externalChange = !isNew && currentRevision != acceptedRevision

    fun validationMessage(): String? {
        val finalName = if (isNew) name.trim() else initialName
        val finalTimeout = timeout.toLongOrNull()
        val invalidHeader = headers.lineSequence().filter { it.isNotBlank() }.firstOrNull { line ->
            val separator = line.indexOf(':')
            separator <= 0 || line.substring(0, separator).isBlank() || line.substring(separator + 1).isBlank()
        }
        return when {
            finalName.isBlank() -> context.getString(R.string.settings_name_required)
            !isSafeMcpEndpoint(url.trim()) -> context.getString(R.string.settings_use_https)
            isNew && finalName in existingNames -> context.getString(R.string.settings_server_exists, finalName)
            invalidHeader != null -> context.getString(R.string.settings_header_format)
            finalTimeout == null || finalTimeout !in 1_000L..60_000L -> context.getString(R.string.settings_timeout_range)
            else -> null
        }
    }

    fun draft(): Pair<String, McpServerConfig>? {
        val finalName = if (isNew) name.trim() else initialName
        val finalTimeout = timeout.toLongOrNull()
        error = validationMessage()
        return if (error == null) {
            finalName to McpServerConfig("remote", url.trim(), parseHeaders(headers), enabled, finalTimeout!!)
        } else null
    }

    val initialHeaders = baseline.headers.entries.joinToString("\n") { "${it.key}: ${it.value}" }
    val changed = if (isNew) {
        name.isNotBlank() || url.isNotBlank() || headers.isNotBlank() || timeout != baseline.timeout.toString() || enabled != baseline.enabled
    } else {
        url != baseline.url || headers != initialHeaders || timeout != baseline.timeout.toString() || enabled != baseline.enabled
    }
    val canSubmit = validationMessage() == null && !testing && !saving && !externalChange
    LaunchedEffect(changed) { onDirtyChange(changed) }
    val shownSnapshot = testResult ?: snapshot.takeUnless { changed }
    Page(if (isNew) stringResource(R.string.settings_add_mcp_server) else initialName, onBack) {
        if (externalChange) {
            Text(
                stringResource(R.string.settings_mcp_changed_elsewhere),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.error,
            )
            Spacer(Modifier.height(Spacing.xs))
            PcButton(stringResource(R.string.common_reload), filled = false) {
                baseline = initial
                url = initial.url
                headers = initial.headers.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                timeout = initial.timeout.toString()
                enabled = initial.enabled
                acceptedRevision = currentRevision
                error = null
                testResult = null
            }
            Spacer(Modifier.height(Spacing.s))
        }
        when {
            testing -> Note(stringResource(R.string.settings_testing_config))
            shownSnapshot?.connected == true -> Note(stringResource(R.string.settings_connected_to, shownSnapshot.serverTitle.ifBlank { shownSnapshot.serverName }.ifBlank { name }))
            shownSnapshot?.error?.isNotBlank() == true -> Text(shownSnapshot.error, style = MaterialTheme.typography.bodyMedium, color = colors.error)
            !isNew && !changed -> Note(if (enabled) stringResource(R.string.common_not_tested) else stringResource(R.string.common_off))
        }
        PcSectionLabel(stringResource(R.string.common_connection))
        if (isNew) {
            McpFieldLabel(stringResource(R.string.settings_server_name))
            PcField(name, { name = it; error = null; testResult = null }, "context7", contentDescription = stringResource(R.string.settings_server_name))
        }
        McpFieldLabel(stringResource(R.string.settings_remote_url))
        PcField(url, { url = it; error = null; testResult = null }, "https://host/mcp", contentDescription = stringResource(R.string.settings_remote_url))
        McpFieldLabel(stringResource(R.string.settings_http_headers))
        PcField(
            headers,
            { headers = it; error = null; testResult = null },
            "Authorization: Bearer …",
            singleLine = false,
            minLines = 2,
            contentDescription = stringResource(R.string.settings_http_headers),
        )
        Note(stringResource(R.string.settings_headers_note))
        McpFieldLabel(stringResource(R.string.settings_connection_timeout))
        PcField(
            timeout,
            { timeout = it.filter(Char::isDigit); error = null; testResult = null },
            "5000 milliseconds",
            contentDescription = stringResource(R.string.settings_cd_connection_timeout),
        )
        Spacer(Modifier.height(Spacing.xs))
        ToggleRow(stringResource(R.string.common_enabled), checked = enabled) { enabled = it; error = null; testResult = null }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error, modifier = Modifier.padding(top = Spacing.xs)) }
        Spacer(Modifier.height(Spacing.s))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Box(Modifier.weight(1f)) {
                PcButton(if (testing) stringResource(R.string.common_testing) else stringResource(R.string.common_test), filled = false, enabled = canSubmit) {
                    if (!testing) draft()?.let { (draftName, server) ->
                        scope.launch {
                            testing = true
                            testResult = vm.testMcpServer(draftName, server)
                            testing = false
                        }
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                PcButton(if (saving) stringResource(R.string.common_saving) else stringResource(R.string.common_save), enabled = canSubmit) {
                    draft()?.let { (draftName, server) ->
                        scope.launch {
                            saving = true
                            vm.saveMcpServerAndWait(draftName, server, baseline.takeUnless { isNew }).fold(
                                onSuccess = { onSaved() },
                                onFailure = { error = it.message ?: context.getString(R.string.settings_mcp_save_failed) },
                            )
                            saving = false
                        }
                    }
                }
            }
        }
        shownSnapshot?.takeIf { it.connected }?.let { connectedSnapshot ->
            PcSectionLabel(stringResource(R.string.settings_server))
            PcGroup {
                McpValueRow(stringResource(R.string.common_name), connectedSnapshot.serverTitle.ifBlank { connectedSnapshot.serverName }.ifBlank { name })
                McpValueRow(stringResource(R.string.common_version), connectedSnapshot.serverVersion.ifBlank { stringResource(R.string.common_unknown) })
                McpValueRow(stringResource(R.string.settings_protocol), connectedSnapshot.protocolVersion)
                McpValueRow(stringResource(R.string.settings_capabilities), connectedSnapshot.capabilities.sorted().joinToString().ifBlank { stringResource(R.string.common_none) })
            }
            if (connectedSnapshot.instructions.isNotBlank()) {
                PcSectionLabel(stringResource(R.string.common_instructions))
                Note(connectedSnapshot.instructions)
            }
            PcSectionLabel(stringResource(R.string.common_tools))
            if (connectedSnapshot.tools.isEmpty()) Note(stringResource(R.string.settings_no_tools)) else {
                PcGroup {
                    connectedSnapshot.tools.take(30).forEach { tool ->
                        PcRow {
                            Column {
                                Text(tool.title.ifBlank { tool.name }, style = MaterialTheme.typography.bodyMedium, color = colors.onBackground)
                                if (tool.description.isNotBlank()) Text(tool.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (connectedSnapshot.tools.size > 30) Note(stringResource(R.string.settings_more_tools, connectedSnapshot.tools.size - 30))
            }
        }
        if (!isNew) {
            Spacer(Modifier.height(Spacing.l))
            PcButton(if (confirmDelete) stringResource(R.string.settings_confirm_delete) else stringResource(R.string.settings_delete_server), filled = false, destructive = true) {
                if (confirmDelete) {
                    vm.deleteMcpServer(initialName)
                    onSaved()
                } else {
                    confirmDelete = true
                }
            }
        }
    }
}

@Composable
private fun ConfirmDiscardDialog(
    message: String,
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
) {
    Dialog(onDismissRequest = onKeepEditing) {
        Column(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(Spacing.m),
        ) {
            Text(stringResource(R.string.settings_discard_changes), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.m),
            )
            PcButton(stringResource(R.string.common_keep_editing), onClick = onKeepEditing)
            Spacer(Modifier.height(Spacing.xs))
            PcButton(stringResource(R.string.common_discard), filled = false, destructive = true, onClick = onDiscard)
        }
    }
}

@Composable
private fun ConfirmActionDialog(
    title: String,
    message: String,
    action: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(Spacing.m),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.m),
            )
            PcButton(stringResource(R.string.common_cancel), onClick = onDismiss)
            Spacer(Modifier.height(Spacing.xs))
            PcButton(action, filled = false, destructive = true, onClick = onConfirm)
        }
    }
}

@Composable
private fun McpFieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 2.dp, top = 10.dp, bottom = 4.dp),
    )
}

@Composable
private fun McpValueRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    PcRow {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

private enum class SkillFilter { ALL, ACTIVE, OFF, ISSUES }

@Composable
private fun SkillsPage(vm: ChatViewModel, onBack: () -> Unit, onNestedBackActive: (Boolean) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(SkillFilter.ALL) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorDirty by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val selectedSkill = state.skills.firstOrNull { it.id == selectedId }
    val editorSkill = state.skills.firstOrNull { it.id == editingId }
    val nested = selectedSkill != null || editingId != null
    val closeNested = {
        if (editingId != null) {
            if (editorDirty) confirmDiscard = true else editingId = null
        } else {
            selectedId = null
        }
    }
    val detailBackMotion = rememberPredictiveBackMotion(enabled = nested && !confirmDiscard) { closeNested() }
    DisposableEffect(nested) {
        onNestedBackActive(nested)
        onDispose { onNestedBackActive(false) }
    }
    val filtered = remember(state.skills, query, filter) {
        state.skills.filter { skill ->
            val matchesQuery = query.isBlank() || skill.name.contains(query, true) ||
                skill.manifest?.description?.contains(query, true) == true || skill.issue?.contains(query, true) == true
            val matchesFilter = when (filter) {
                SkillFilter.ALL -> true
                SkillFilter.ACTIVE -> skill.status == SkillStatus.ACTIVE
                SkillFilter.OFF -> skill.status == SkillStatus.DISABLED || skill.status == SkillStatus.SHADOWED
                SkillFilter.ISSUES -> skill.status == SkillStatus.INVALID
            }
            matchesQuery && matchesFilter
        }
    }
    Box(Modifier.fillMaxSize()) {
    Box(
        Modifier.fillMaxSize().then(
            if (nested) Modifier.clearAndSetSemantics {} else Modifier,
        ),
    ) {
    Page(
        stringResource(R.string.common_skills),
        onBack,
        action = {
            PcIconButton(Icons.Filled.Add, stringResource(R.string.settings_new_skill)) {
                editorDirty = false
                editingId = NEW_SKILL_ID
            }
        },
    ) {
        val active = state.skills.count { it.status == SkillStatus.ACTIVE }
        val issues = state.skills.count { it.status == SkillStatus.INVALID }
        Note(if (issues > 0) stringResource(R.string.settings_skills_status_issues, active, state.skills.size, issues) else stringResource(R.string.settings_skills_status, active, state.skills.size))
        PcField(query, { query = it }, stringResource(R.string.common_search_skills))
        SkillFilters(filter) { filter = it }
        PcSectionLabel(filter.label())
        if (filtered.isEmpty()) {
            Note(stringResource(R.string.settings_no_matching_skills))
        } else {
            PcGroup {
                filtered.forEach { skill ->
                    PcRow(onClick = { selectedId = skill.id }) {
                        Column(Modifier.weight(1f)) {
                            Text(skill.name, style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
                            Text(
                                "${skill.scope.label()} · ${skill.status.label()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (skill.status == SkillStatus.INVALID) colors.error else colors.onSurfaceVariant,
                            )
                            skill.manifest?.description?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (skill.manifest != null && skill.status != SkillStatus.SHADOWED && skill.status != SkillStatus.INVALID) {
                            PcToggle(
                                skill.status == SkillStatus.ACTIVE,
                                { vm.setSkillEnabled(skill.id, it) },
                                stringResource(R.string.settings_cd_skill_enabled, skill.name),
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.tertiary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Note(stringResource(R.string.settings_skills_note))
    }
    }
    selectedSkill?.let { skill ->
        val modifier = if (editingId == null) Modifier.predictiveBackTransform(detailBackMotion) else Modifier
        Box(Modifier.fillMaxSize().then(modifier).background(colors.background)) {
            SkillDetailPage(
                vm,
                skill,
                onEdit = { editorDirty = false; editingId = skill.id },
                onBack = { selectedId = null },
            )
        }
    }
    editingId?.let { id ->
        Box(Modifier.fillMaxSize().predictiveBackTransform(detailBackMotion).background(colors.background)) {
            SkillEditorPage(
                vm = vm,
                skillId = id.takeUnless { it == NEW_SKILL_ID },
                skill = editorSkill,
                isNew = id == NEW_SKILL_ID,
                onDirtyChange = { editorDirty = it },
                onBack = closeNested,
                onSaved = { editorDirty = false; editingId = null },
            )
        }
    }
    if (confirmDiscard) {
        ConfirmDiscardDialog(
            message = stringResource(R.string.settings_unsaved_changes_skill),
            onKeepEditing = { confirmDiscard = false },
            onDiscard = { confirmDiscard = false; editorDirty = false; editingId = null },
        )
    }
    }
}

@Composable
private fun SkillDetailPage(vm: ChatViewModel, skill: ManagedSkill, onEdit: () -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val manifest = skill.manifest
    var confirmDelete by remember(skill.id) { mutableStateOf(false) }
    Page(skill.name, onBack = onBack) {
        Note("${skill.scope.label()} · ${skill.status.label()}")
        skill.issue?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error) }
        manifest?.description?.takeIf { it.isNotBlank() }?.let { Note(it) }
        if (manifest != null && skill.status != SkillStatus.SHADOWED && skill.status != SkillStatus.INVALID) {
            ToggleRow(
                stringResource(R.string.common_enabled),
                sub = stringResource(R.string.settings_skill_enabled_desc),
                checked = skill.status != SkillStatus.DISABLED,
            ) { vm.setSkillEnabled(skill.id, it) }
        } else if (skill.status == SkillStatus.SHADOWED) {
            Note(stringResource(R.string.settings_skill_shadowed_note))
        }
        manifest?.body?.takeIf { it.isNotBlank() }?.let { instructions ->
            PcSectionLabel(stringResource(R.string.common_instructions))
            Box(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                    .background(colors.surface).padding(Spacing.m),
            ) {
                MarkdownBlocks(instructions)
            }
            Note(stringResource(R.string.settings_skill_edit_note))
        }
        PcSectionLabel(stringResource(R.string.common_details))
        PcGroup {
            if (!manifest?.compatibility.isNullOrBlank()) PcRow {
                Column {
                    Text(stringResource(R.string.settings_compatibility), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Text(manifest.compatibility, style = MaterialTheme.typography.bodyMedium, color = colors.onBackground, modifier = Modifier.padding(top = 2.dp))
                }
            }
            if (!manifest?.license.isNullOrBlank()) PcRow {
                Text(stringResource(R.string.settings_license), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Text(manifest.license, style = MaterialTheme.typography.bodyMedium, color = colors.onBackground)
            }
            if (skill.location.isNotBlank()) PcRow {
                Column {
                    Text(stringResource(R.string.settings_location), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Text(skill.location, style = MaterialTheme.typography.bodySmall, color = colors.onBackground, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.height(Spacing.l))
        PcButton(stringResource(R.string.settings_edit_skill), filled = false, onClick = onEdit)
        Spacer(Modifier.height(Spacing.xs))
        PcButton(if (confirmDelete) stringResource(R.string.settings_confirm_delete) else stringResource(R.string.settings_delete_skill), filled = false, destructive = true) {
            if (confirmDelete) {
                vm.deleteSkill(skill.id)
                onBack()
            } else {
                confirmDelete = true
            }
        }
    }
}

@Composable
private fun SkillEditorPage(
    vm: ChatViewModel,
    skillId: String?,
    skill: ManagedSkill?,
    isNew: Boolean,
    onDirtyChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val editorKey = skillId ?: NEW_SKILL_ID
    val initialName = skill?.name ?: skillId?.substringBeforeLast('/')?.substringAfterLast('/') ?: "new-skill"
    val initialContent = remember(editorKey) { if (isNew) newSkillTemplate(initialName) else "" }
    val clipboard = LocalClipboardManager.current
    var name by rememberSaveable(editorKey) { mutableStateOf(initialName) }
    var skillScope by rememberSaveable(editorKey) { mutableStateOf(skill?.scope ?: SkillScope.GLOBAL) }
    var baselineRevision by rememberSaveable(editorKey) { mutableStateOf(if (isNew) revisionOf(initialContent) else "") }
    var content by rememberSaveable(editorKey) { mutableStateOf(initialContent) }
    var loaded by rememberSaveable(editorKey) { mutableStateOf(isNew) }
    var baseline by remember(editorKey) { mutableStateOf(initialContent) }
    var baselineReady by remember(editorKey) { mutableStateOf(isNew) }
    var loading by remember(editorKey) { mutableStateOf(!isNew) }
    var unavailable by remember(editorKey) { mutableStateOf(false) }
    var conflict by remember(editorKey) { mutableStateOf(false) }
    var saving by remember(editorKey) { mutableStateOf(false) }
    var error by remember(editorKey) { mutableStateOf<String?>(null) }
    LaunchedEffect(editorKey, skill) {
        if (isNew) return@LaunchedEffect
        loading = true
        if (skill == null || skillId == null) {
            unavailable = true
            conflict = false
            baselineReady = false
            loading = false
            return@LaunchedEffect
        }
        vm.readSkill(skillId).fold(
            onSuccess = { latest ->
                val latestRevision = revisionOf(latest)
                unavailable = false
                if (!loaded) {
                    baseline = latest
                    baselineRevision = latestRevision
                    content = latest
                    loaded = true
                    baselineReady = true
                    conflict = false
                } else if (latestRevision == baselineRevision) {
                    baseline = latest
                    baselineReady = true
                    conflict = false
                } else {
                    baselineReady = false
                    conflict = true
                }
            },
            onFailure = {
                unavailable = true
                baselineReady = false
                error = it.message ?: context.getString(R.string.settings_skill_read_failed)
            },
        )
        loading = false
    }
    val changed = !loading && (content != baseline || isNew && (name != "new-skill" || skillScope != SkillScope.GLOBAL))
    LaunchedEffect(changed, loading) { if (!loading) onDirtyChange(changed) }
    Page(if (isNew) stringResource(R.string.settings_new_skill) else stringResource(R.string.settings_edit_skill_name, name), onBack) {
        if (isNew) {
            PcSectionLabel(stringResource(R.string.settings_identity))
            McpFieldLabel(stringResource(R.string.settings_skill_name))
            PcField(
                name,
                { value ->
                    val next = value.lowercase().replace(Regex("[^a-z0-9-]"), "")
                    content = content.replaceFirst(Regex("(?m)^name:.*$"), "name: $next")
                    name = next
                    error = null
                },
                "my-skill",
                contentDescription = stringResource(R.string.settings_skill_name),
            )
            PcGroup {
                CheckRow(stringResource(R.string.common_global), skillScope == SkillScope.GLOBAL) { skillScope = SkillScope.GLOBAL }
                CheckRow(stringResource(R.string.settings_current_project), skillScope == SkillScope.PROJECT) { skillScope = SkillScope.PROJECT }
            }
        } else {
            Note(stringResource(R.string.settings_skill_scope_reload, skillScope.label()))
        }
        if (unavailable) {
            Text(
                stringResource(R.string.settings_skill_removed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            if (content.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.xs))
                PcButton(stringResource(R.string.settings_copy_draft), filled = false) { clipboard.setText(AnnotatedString(content)) }
            }
        } else if (conflict) {
            Text(
                stringResource(R.string.settings_skill_changed_elsewhere),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        PcSectionLabel(stringResource(R.string.settings_skill_md))
        PcField(
            content,
            { content = it; error = null },
            if (loading) stringResource(R.string.common_loading) else stringResource(R.string.settings_skill_instructions),
            singleLine = false,
            minLines = 12,
            contentDescription = stringResource(R.string.settings_skill_instructions),
        )
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(Spacing.s))
        PcButton(
            if (saving) stringResource(R.string.common_saving) else stringResource(R.string.common_save),
            enabled = !loading && !saving && !unavailable && !conflict && baselineReady && name.isNotBlank() && content.isNotBlank(),
        ) {
            scope.launch {
                saving = true
                vm.saveSkillAndWait(skillId, skillScope, name, content, baseline.takeUnless { isNew }).fold(
                    onSuccess = { onSaved() },
                    onFailure = { error = it.message ?: context.getString(R.string.settings_skill_save_failed) },
                )
                saving = false
            }
        }
    }
}

private fun newSkillTemplate(name: String): String =
    "---\nname: $name\ndescription: Describe when the agent should use this skill.\nlicense: Apache-2.0\n---\n\nWrite concise, actionable instructions here."

private const val NEW_SKILL_ID = "__phonecode_new_skill__"

@Composable
private fun SkillFilters(selected: SkillFilter, onSelect: (SkillFilter) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(top = Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SkillFilter.entries.forEach { filter ->
            val active = filter == selected
            val interaction = remember(filter) { MutableInteractionSource() }
            Box(
                Modifier.weight(1f).heightIn(min = Spacing.touchTarget)
                    .pressFeedback(interaction, pressedScale = 0.98f)
                    .clip(MaterialTheme.shapes.large)
                    .background(if (active) colors.primary else colors.surfaceContainerHigh)
                    .semantics { this.selected = active; role = Role.Tab }
                    .clickable(interactionSource = interaction, indication = ripple(), role = Role.Tab) { onSelect(filter) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(filter.shortLabel(), style = MaterialTheme.typography.labelMedium, color = if (active) colors.onPrimary else colors.onBackground)
            }
        }
    }
}

@Composable
private fun SkillScope.label() = stringResource(if (this == SkillScope.PROJECT) R.string.common_project else R.string.common_global)

@Composable
private fun SkillStatus.label() = when (this) {
    SkillStatus.ACTIVE -> stringResource(R.string.common_active)
    SkillStatus.DISABLED -> stringResource(R.string.common_off)
    SkillStatus.SHADOWED -> stringResource(R.string.common_overridden)
    SkillStatus.INVALID -> stringResource(R.string.common_needs_attention)
}

@Composable
private fun SkillFilter.label() = when (this) {
    SkillFilter.ALL -> stringResource(R.string.settings_all_skills)
    SkillFilter.ACTIVE -> stringResource(R.string.common_active)
    SkillFilter.OFF -> stringResource(R.string.settings_off_and_overridden)
    SkillFilter.ISSUES -> stringResource(R.string.common_needs_attention)
}

@Composable
private fun SkillFilter.shortLabel() = when (this) {
    SkillFilter.ALL -> stringResource(R.string.common_all)
    SkillFilter.ACTIVE -> stringResource(R.string.common_active)
    SkillFilter.OFF -> stringResource(R.string.common_off)
    SkillFilter.ISSUES -> stringResource(R.string.settings_issues)
}

@Composable
private fun GitPage(vm: ChatViewModel, settingsVm: SettingsViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var advanced by rememberSaveable { mutableStateOf(false) }
    Page(stringResource(R.string.common_git), onBack) {
        PcSectionLabel(stringResource(R.string.settings_github))
        when {
            state.githubAuthCode != null -> {
                // Device flow in progress: show the code big, open the browser, keep polling.
                // Rows now paint their own card surface, so freeform group content does too.
                PcGroup {
                    Column(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(Spacing.m),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.settings_github_enter_code), style = MaterialTheme.typography.labelMedium, color = colors.secondary)
                        Text(
                            state.githubAuthCode.orEmpty(),
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = PcMono, letterSpacing = 2.sp),
                            color = colors.onBackground,
                            modifier = Modifier.padding(vertical = Spacing.s),
                        )
                        PcButton(stringResource(R.string.settings_github_open_device)) {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.githubVerifyUri ?: "https://github.com/login/device")))
                            }
                        }
                        Spacer(Modifier.height(Spacing.xs))
                        PcButton(stringResource(R.string.common_cancel), filled = false) { vm.cancelGitHubSignIn() }
                    }
                }
                Note(stringResource(R.string.settings_github_waiting))
            }
            state.githubLogin != null -> {
                PcGroup {
                    PcRow {
                        Column(Modifier.weight(1f)) {
                            Text("@${state.githubLogin}", style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
                            Text(stringResource(R.string.settings_github_connected), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                        Text(
                            stringResource(R.string.settings_sign_out),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.error,
                            modifier = Modifier.clip(MaterialTheme.shapes.extraSmall).clickable { vm.signOutGitHub() }.padding(8.dp),
                        )
                    }
                }
            }
            else -> {
                PcButton(stringResource(R.string.settings_sign_in_github)) { vm.startGitHubSignIn() }
                Spacer(Modifier.height(6.dp))
            }
        }
        PcSectionLabel(stringResource(R.string.common_advanced))
        PcGroup {
            ToggleRow(stringResource(R.string.settings_show_advanced), stringResource(R.string.settings_show_advanced_desc), checked = advanced) { advanced = it }
            if (advanced) {
                ToggleRow(
                    stringResource(R.string.settings_auto_branch),
                    stringResource(R.string.settings_auto_branch_desc),
                    checked = settings.gitAutoBranch,
                ) { v -> settingsVm.update { it.copy(gitAutoBranch = v) } }
                var token by remember { mutableStateOf(vm.keyFor("git.token")) }
                PcRow {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_manual_token), style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
                        Spacer(Modifier.height(6.dp))
                        PcField(token, { token = it; vm.setKey("git.token", it) }, stringResource(R.string.settings_pat_placeholder), password = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportPage(vm: ChatViewModel, settingsVm: SettingsViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val stamp = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) vm.exportTo(uri)
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // reload AFTER the import lands: it overwrote app_settings.json on disk, and the
        // theme/instructions must go live without a restart (review finding #3).
        if (uri != null) vm.importFrom(uri) { settingsVm.reload() }
    }
    Page(stringResource(R.string.settings_export_import), onBack) {
        PcSectionLabel(stringResource(R.string.settings_your_data))
        Note(stringResource(R.string.settings_export_note))
        Spacer(Modifier.height(10.dp))
        PcButton(stringResource(R.string.settings_export_chats), filled = false) { exporter.launch("phonecode-backup-$stamp.zip") }
        Spacer(Modifier.height(10.dp))
        PcButton(stringResource(R.string.settings_import_file), filled = false) { importer.launch(arrayOf("application/zip", "application/octet-stream")) }
        state.notice?.let {
            Spacer(Modifier.height(10.dp))
            Note(it)
            LaunchedEffect(it) { kotlinx.coroutines.delay(4000); vm.clearNotice() }
        }
    }
}

@Composable
private fun AboutPage(vm: ChatViewModel, onOpenDoc: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "0.1"
    }
    Page(stringResource(R.string.common_about), onBack) {
        Column(Modifier.fillMaxWidth().padding(vertical = Spacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(R.drawable.ic_phonecode_mark), null, tint = colors.onBackground, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.common_phonecode), style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
            Text(stringResource(R.string.settings_version_format, version), style = MaterialTheme.typography.labelMedium, color = colors.tertiary, modifier = Modifier.padding(top = 4.dp))
        }
        PcGroup {
            PcRow(onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://dttdrv.xyz/phonecode"))) }
            }) {
                Text(stringResource(R.string.settings_website), style = MaterialTheme.typography.bodyLarge, color = colors.onBackground, modifier = Modifier.weight(1f))
                Text("dttdrv.xyz/phonecode", style = MaterialTheme.typography.labelMedium, color = colors.tertiary)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.tertiary, modifier = Modifier.size(20.dp))
            }
            PcRow {
                Text(stringResource(R.string.settings_config_directory), style = MaterialTheme.typography.bodyLarge, color = colors.onBackground, modifier = Modifier.weight(1f))
                Text(vm.configDirPath().substringAfterLast("/"), style = MaterialTheme.typography.labelSmall.copy(fontFamily = PcMono), color = colors.tertiary)
            }
            NavRow(stringResource(R.string.settings_terms_of_service)) { onOpenDoc("doc:terms") }
            NavRow(stringResource(R.string.settings_privacy_policy)) { onOpenDoc("doc:privacy") }
            NavRow(stringResource(R.string.settings_open_source_licenses)) { onOpenDoc("doc:licenses") }
        }
    }
}

@Composable
private fun DocPage(title: String, assetName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val text = remember(assetName) {
        runCatching { context.assets.open(assetName).bufferedReader().use { it.readText() } }
            .getOrDefault(context.getString(R.string.settings_document_unavailable))
    }
    Page(title, onBack) {
        // Render the markdown (headings, bold, lists) instead of dumping the raw source: the old single
        // Text showed literal '#', '**' and '-' in a tiny caption font. The doc's own H1 title is dropped
        // since the page header already shows it.
        val body = remember(text) { text.replace(Regex("^#\\s+.*(\\R+)?"), "") }
        Box(Modifier.padding(vertical = Spacing.xs)) {
            MarkdownBlocks(body, color = colors.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Custom provider dialog
// ---------------------------------------------------------------------------------------------

/**
 * Add a user-defined provider (round-3 feedback): name, base URL, wire format, and the model ids
 * to expose. Saved to providers.json - the same file the agent edits - so both arrival paths feed
 * one catalog. The API key is set afterwards on the provider's own detail page, like every preset.
 */
@Composable
private fun CustomProviderDialog(
    existingIds: Set<String>,
    onSave: suspend (String, CustomProvider) -> Result<Unit>,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var baseUrl by rememberSaveable { mutableStateOf("") }
    var anthropicFormat by rememberSaveable { mutableStateOf(false) }
    var modelsText by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().shadow(24.dp, MaterialTheme.shapes.extraLarge, clip = false)
                .clip(MaterialTheme.shapes.extraLarge).background(colors.surfaceContainerHigh).padding(Spacing.m),
        ) {
            Text(stringResource(R.string.settings_add_custom_provider), style = MaterialTheme.typography.titleMedium, color = colors.onBackground, modifier = Modifier.padding(bottom = Spacing.s))
            PcField(name, { name = it }, stringResource(R.string.settings_provider_name_placeholder))
            Spacer(Modifier.height(6.dp))
            PcField(baseUrl, { baseUrl = it }, stringResource(R.string.settings_base_url_placeholder))
            Spacer(Modifier.height(6.dp))
            var discovering by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Box(Modifier.weight(1f)) {
                    PcButton(
                        if (discovering) stringResource(R.string.settings_discovering_models) else stringResource(R.string.settings_discover_models),
                        filled = false,
                        enabled = !discovering,
                    ) {
                        val url = baseUrl.trim().trimEnd('/')
                        if (url.isBlank()) {
                            error = context.getString(R.string.settings_discover_enter_url)
                            return@PcButton
                        }
                        discovering = true
                        error = null
                        scope.launch {
                            try {
                                val (body, code) = withContext(Dispatchers.IO) {
                                    val client = OkHttpClient()
                                    val request = Request.Builder().url("$url/models").build()
                                    val response = client.newCall(request).execute()
                                    Pair(response.body?.string(), response.code)
                                }
                                if (code !in 200..299 || body == null) {
                                    error = context.getString(R.string.settings_discover_models_failed, "HTTP $code")
                                    discovering = false
                                    return@launch
                                }
                                val json = JSONObject(body)
                                val data = json.optJSONArray("data")
                                if (data == null || data.length() == 0) {
                                    error = context.getString(R.string.settings_discover_models_none)
                                    discovering = false
                                    return@launch
                                }
                                val ids = mutableListOf<String>()
                                for (i in 0 until data.length()) {
                                    val obj = data.optJSONObject(i)
                                    val id = obj?.optString("id")?.takeIf { it.isNotBlank() }
                                    if (id != null) ids.add(id)
                                }
                                if (ids.isEmpty()) {
                                    error = context.getString(R.string.settings_discover_models_none)
                                } else {
                                    modelsText = ids.joinToString("\n")
                                    error = context.getString(R.string.settings_discover_models_found, ids.size)
                                }
                            } catch (e: Exception) {
                                error = context.getString(R.string.settings_discover_models_failed, e.message ?: e.toString())
                            } finally {
                                discovering = false
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            PcField(modelsText, { modelsText = it }, stringResource(R.string.settings_model_ids_placeholder), singleLine = false, minLines = 2)
            Spacer(Modifier.height(Spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_anthropic_format), style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
                    Text(stringResource(R.string.settings_anthropic_format_desc), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                PcToggle(anthropicFormat, { anthropicFormat = it }, stringResource(R.string.settings_cd_anthropic_format))
            }
            error?.let { Spacer(Modifier.height(6.dp)); Text(it, style = MaterialTheme.typography.labelSmall, color = colors.error) }
            Spacer(Modifier.height(Spacing.s))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Box(Modifier.weight(1f)) { PcButton(stringResource(R.string.common_cancel), filled = false, onClick = onDismiss) }
                Box(Modifier.weight(1f)) {
                    PcButton(if (saving) stringResource(R.string.common_saving) else stringResource(R.string.common_save), enabled = !saving) {
                        val id = name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
                        val models = modelsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                        when {
                            name.isBlank() -> error = context.getString(R.string.settings_name_required)
                            id.isBlank() -> error = context.getString(R.string.settings_name_needs_letter)
                            !isSafeCustomProviderId(id) -> error = context.getString(R.string.settings_provider_name_short)
                            id in existingIds -> error = context.getString(R.string.settings_provider_exists, id)
                            !isSafeProviderEndpoint(baseUrl.trim()) -> error = context.getString(R.string.settings_use_https)
                            models.isEmpty() -> error = context.getString(R.string.settings_add_model_id)
                            else -> {
                                saving = true
                                scope.launch {
                                    onSave(id, CustomProvider(
                                        name = name.trim(),
                                        baseUrl = baseUrl.trim().trimEnd('/'),
                                        format = if (anthropicFormat) "anthropic" else "openai",
                                        models = models.associateWith { CustomModel(name = it) },
                                    )).onSuccess { onDismiss() }.onFailure { failure ->
                                        error = failure.message ?: context.getString(R.string.settings_custom_provider_save_failed)
                                    }
                                    saving = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseHeaders(text: String): Map<String, String> =
    text.lineSequence().mapNotNull { line ->
        val i = line.indexOf(':')
        if (i <= 0) null else line.substring(0, i).trim() to line.substring(i + 1).trim()
    }.filter { it.first.isNotEmpty() && it.second.isNotEmpty() }.toMap()
