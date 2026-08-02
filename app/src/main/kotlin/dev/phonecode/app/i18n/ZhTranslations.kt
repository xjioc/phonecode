package dev.phonecode.app.i18n

/**
 * Chinese (Simplified) translations for PhoneCode UI strings.
 * Keys are the original English strings; values are their Chinese equivalents.
 * Untranslated strings fall back to English automatically.
 */
val zhTranslations: Map<String, String> = mapOf(
    // ─── Onboarding ───────────────────────────────────────────────────────────
    "Build real projects from your phone" to "在手机上构建真实项目",
    "Run an AI coding agent in a private local workspace, with the models and tools you trust and access to phone folders you choose." to
        "在私有的本地工作区中运行 AI 编程代理，使用你信任的模型和工具，并访问你选择的手机文件夹。",
    "Private project workspaces" to "私有项目工作区",
    "Keep each project and its chats together" to "将每个项目及其对话放在一起",
    "Local tools and Git" to "本地工具和 Git",
    "Build, test, and manage source control on device" to "在设备上构建、测试和管理源代码",
    "Your choice of model" to "自选模型",
    "Sign in or add provider access" to "登录或添加服务商访问",
    "Get started" to "开始使用",
    "Setup" to "设置",
    "Step 2 of 2" to "第 2 步（共 2 步）",
    "Get ready to build" to "准备开始构建",
    "Connect a model to start. Link a phone folder and GitHub when you need them." to
        "连接一个模型即可开始。需要时再关联手机文件夹和 GitHub。",
    "Connect a model" to "连接模型",
    "Model configured on this device" to "此设备已配置模型",
    "Required for agent work" to "代理工作所必需",
    "Link a phone folder" to "关联手机文件夹",
    "Folder linked for shared file access" to "已关联文件夹以共享文件访问",
    "Optional access to files already on your phone" to "可选：访问手机上已有的文件",
    "Connect GitHub" to "连接 GitHub",
    "GitHub account connected" to "GitHub 账户已连接",
    "Optional for repository sync" to "可选：用于仓库同步",
    "Start building" to "开始构建",
    "A model is required to run an agent. Optional setup can wait." to "运行代理需要模型。可选设置可以稍后再做。",
    "Explore without a model" to "不连接模型先看看",

    // ─── Model Setup ──────────────────────────────────────────────────────────
    "Set up a model" to "设置模型",
    "Continue with ChatGPT" to "继续使用 ChatGPT",
    "Choose how to connect" to "选择连接方式",
    "Keys stay encrypted on this device. Prompts, attachments, and tool results go directly to the provider you choose." to
        "密钥在此设备上保持加密。提示词、附件和工具结果直接发送到你选择的服务商。",
    "Configured" to "已配置",
    "Sign in with ChatGPT" to "使用 ChatGPT 登录",
    "Could not open the sign-in page." to "无法打开登录页面。",
    "Recommended providers" to "推荐的服务商",
    "Fewer providers" to "收起更多服务商",
    "More providers" to "更多服务商",
    "Add an API key" to "添加 API 密钥",
    "Use configured provider" to "使用已配置的服务商",
    "Save and continue" to "保存并继续",
    "A key is already configured. Continue with it, or enter a replacement." to
        "已配置密钥。可以直接继续，或输入新密钥替换。",
    "Enter your API key. It is stored in Android secure storage and is never included in exports." to
        "输入你的 API 密钥。它存储在 Android 安全存储中，永远不会包含在导出中。",
    "New API key (optional)" to "新 API 密钥（可选）",
    "API key" to "API 密钥",
    "Secure storage is unavailable on this device, so PhoneCode cannot save this key." to
        "此设备无法使用安全存储，PhoneCode 无法保存此密钥。",
    "Dismiss" to "关闭",
    "API key saved, but PhoneCode could not activate an available model for this provider." to
        "API 密钥已保存，但 PhoneCode 无法为此服务商激活可用模型。",
    "PhoneCode could not save this API key in secure storage." to "PhoneCode 无法在安全存储中保存此 API 密钥。",

    // ─── Chat Screen ──────────────────────────────────────────────────────────
    "Couldn't read that photo." to "无法读取该照片。",
    "Couldn't read that file." to "无法读取该文件。",
    "Choose a photo or text file." to "请选择照片或文本文件。",
    "Menu" to "菜单",
    "Set up model" to "设置模型",
    "Switch model" to "切换模型",
    "Retry" to "重试",
    "Opening chat…" to "正在打开对话…",
    "New chat" to "新对话",
    "Model" to "模型",
    "Connect a model to start" to "连接模型以开始",
    "Choose ChatGPT or add an API key. You can change providers at any time." to
        "选择 ChatGPT 或添加 API 密钥。你可以随时更换服务商。",
    "What should we build?" to "我们来构建什么？",
    "Build a small web app" to "构建一个小型 Web 应用",
    "Explain an error message" to "解释一条错误信息",
    "Refactor a function" to "重构一个函数",
    "Set up a git project" to "设置一个 Git 项目",
    "Restore" to "恢复",
    "Clear" to "清除",
    "Thinking" to "思考中",
    "Done" to "完成",
    "Copy" to "复制",
    "Redo" to "重新生成",
    "Send safety feedback" to "发送安全反馈",
    "Feedback sent" to "反馈已发送",
    "Thank you. Your feedback will be used to improve PhoneCode's safeguards." to
        "感谢你的反馈，它将用于改进 PhoneCode 的安全机制。",
    "Send" to "发送",
    "Sending…" to "发送中…",
    "Choose what went wrong. PhoneCode sends only this category, your optional note, and basic app information." to
        "选择出了什么问题。PhoneCode 仅发送此分类、你的可选备注和基本应用信息。",
    "Reason" to "原因",
    "What happened? (optional)" to "发生了什么？（可选）",
    "Describe the problem without pasting private information." to "描述问题，请勿粘贴私人信息。",
    "The response, prompt, files, credentials, tool activity, chat history, and device identifiers are never attached." to
        "回复、提示词、文件、凭据、工具活动、聊天记录和设备标识符永远不会被附带。",
    "Hate" to "仇恨",
    "Hateful or dehumanizing content" to "仇恨或非人化内容",
    "Harassment" to "骚扰",
    "Bullying, threats, or targeted abuse" to "欺凌、威胁或定向辱骂",
    "Sexual content" to "色情内容",
    "Sexual or exploitative material" to "性相关或剥削性材料",
    "Violence" to "暴力",
    "Violent threats or harmful instructions" to "暴力威胁或有害指令",
    "Self-harm" to "自残",
    "Encouragement of self-harm" to "鼓励自残",
    "Illegal or malicious" to "违法或恶意",
    "Scams, malware, or unauthorized access" to "诈骗、恶意软件或未授权访问",
    "Privacy" to "隐私",
    "Exposure of private or sensitive information" to "暴露私人或敏感信息",
    "Other" to "其他",
    "Another harmful or inappropriate response" to "其他有害或不当回复",
    "Auto" to "自动",
    "Extra high" to "极高",

    // ─── Settings Home ────────────────────────────────────────────────────────
    "Settings" to "设置",
    "General" to "通用",
    "Appearance" to "外观",
    "Personalization" to "个性化",
    "Models" to "模型",
    "Providers" to "服务商",
    "Tools" to "工具",
    "Agent tools" to "代理工具",
    "MCP servers" to "MCP 服务器",
    "Skills" to "技能",
    "Workspace" to "工作区",
    "Files & permissions" to "文件和权限",
    "Git" to "Git",
    "Data" to "数据",
    "Export & import" to "导出和导入",
    "About" to "关于",
    "Could not open your browser. Check that a browser is installed, then try again." to
        "无法打开浏览器。请确认已安装浏览器，然后重试。",
    "Dismiss error" to "关闭错误",
    "Terms of Service" to "服务条款",
    "Privacy Policy" to "隐私政策",
    "Open-source notices" to "开源声明",

    // ─── Settings: Agent Tools ────────────────────────────────────────────────
    "Available capabilities" to "可用功能",
    " tools" to " 个工具",
    "All" to "全部",
    "Read only" to "只读",
    "Approval" to "需审批",
    "Conditional" to "有条件",
    "Search tools" to "搜索工具",
    "No tools are available yet. Connect an MCP server or add a skill to extend PhoneCode." to
        "暂无可用工具。连接 MCP 服务器或添加技能来扩展 PhoneCode。",

    // ─── Settings: General ────────────────────────────────────────────────────
    "Language" to "语言",
    "System default" to "跟随系统",
    "Follow your phone's language" to "跟随手机的语言设置",
    "Default agent mode" to "默认代理模式",
    "Can use tools and make approved changes" to "可使用工具并进行已批准的更改",
    "Explores and proposes a plan without changing files" to "探索并提出计划，不修改文件",
    "This applies to new chats. You can switch the active chat from the model menu." to
        "此设置适用于新对话。你可以从模型菜单切换当前对话的模式。",
    "Message input" to "消息输入",
    "Send on Enter" to "按 Enter 发送",
    "When off, Enter adds a new line" to "关闭时，Enter 将换行",

    // ─── Settings: Files & Permissions ────────────────────────────────────────
    "Private project workspace" to "私有项目工作区",
    "Permanent and fully available to the agent" to "永久存在且完全可供代理使用",
    "Phone folders" to "手机文件夹",
    "Read & write" to "读写",
    "Link a folder" to "关联文件夹",
    "Approval policy" to "审批策略",
    "Ask before each change" to "每次更改前询问",
    "Review every action before it runs" to "在每个操作运行前审查",
    "Allow changes automatically" to "自动允许更改",
    "Run workspace changes without approval prompts" to "无需审批提示即可运行工作区更改",
    "Remove folder access?" to "移除文件夹访问？",
    "Remove access" to "移除访问",
    "Enable automatic approval?" to "启用自动审批？",
    "Enable automatic approval" to "启用自动审批",
    "Enabling…" to "启用中…",

    // ─── Settings: Appearance ─────────────────────────────────────────────────
    "Color theme" to "颜色主题",
    "Match your phone's appearance" to "跟随手机外观设置",
    "Always use the light theme" to "始终使用浅色主题",
    "Always use the dark theme" to "始终使用深色主题",

    // ─── Settings: Personalization ────────────────────────────────────────────
    "Custom instructions" to "自定义指令",
    "Saving…" to "保存中…",
    "Saved" to "已保存",
    "These instructions are included in new agent turns. Do not add passwords, tokens, or other secrets." to
        "这些指令将包含在新的代理回合中。请勿添加密码、令牌或其他机密信息。",

    // ─── Settings: Providers ──────────────────────────────────────────────────
    "Sign in with ChatGPT (Codex)" to "使用 ChatGPT (Codex) 登录",
    "Add custom provider" to "添加自定义服务商",
    "Signed in with ChatGPT" to "已使用 ChatGPT 登录",
    "API key saved" to "API 密钥已保存",
    "Setup required" to "需要设置",
    "Shown in model picker" to "在模型选择器中显示",
    "Hidden from model picker" to "在模型选择器中隐藏",
    "Custom provider" to "自定义服务商",
    "Remove this provider" to "移除此服务商",
    "Account" to "账户",
    "Signed in" to "已登录",
    "Disconnect" to "断开连接",
    "New API key" to "新 API 密钥",
    "Save key" to "保存密钥",
    "Remove saved key" to "移除已保存的密钥",
    "The API key could not be saved securely." to "API 密钥无法安全保存。",
    "Secure storage is unavailable on this device, so PhoneCode cannot change this key." to
        "此设备无法使用安全存储，PhoneCode 无法更改此密钥。",
    "No models loaded for this provider yet. Models refresh automatically when PhoneCode opens." to
        "此服务商尚未加载模型。模型会在 PhoneCode 打开时自动刷新。",
    "Search models" to "搜索模型",
    "All on" to "全部开启",
    "All off" to "全部关闭",
    "Disconnect ChatGPT?" to "断开 ChatGPT？",
    "This signs out of ChatGPT and removes the saved sign-in credentials. Existing chats stay on this device." to
        "这将登出 ChatGPT 并移除已保存的登录凭据。现有对话仍保留在此设备上。",

    // ─── Settings: MCP ────────────────────────────────────────────────────────
    "Delete MCP server?" to "删除 MCP 服务器？",
    "Delete server" to "删除服务器",
    "Deleting…" to "删除中…",
    "Discard changes?" to "放弃更改？",
    "Keep editing" to "继续编辑",
    "Discard" to "放弃",
    "This server has unsaved changes." to "此服务器有未保存的更改。",
    "This server changed elsewhere. Reload before saving." to "此服务器在其他地方被修改。保存前请重新加载。",

    // ─── Settings: Skills ─────────────────────────────────────────────────────
    "Compatibility" to "兼容性",
    "License" to "许可证",
    "Location" to "位置",
    "Delete skill" to "删除技能",
    "Permanently remove this skill" to "永久移除此技能",
    "Delete skill?" to "删除技能？",
    "This skill was removed or renamed. Your draft is preserved here." to
        "此技能已被移除或重命名。你的草稿保留在此处。",
    "This skill changed elsewhere. Your draft is preserved; reopen the editor to load the latest file." to
        "此技能在其他地方被修改。你的草稿已保留；重新打开编辑器以加载最新文件。",
    "New skill" to "新建技能",
    "Identity" to "标识",
    "Skill name" to "技能名称",
    "Global" to "全局",
    "Current project" to "当前项目",
    "When to use" to "何时使用",
    "When should the agent use this skill?" to "代理应在何时使用此技能？",
    "Instructions" to "指令",
    "Advanced" to "高级",
    "Advanced source" to "高级源码",
    "Edit the complete SKILL.md file" to "编辑完整的 SKILL.md 文件",
    "Save" to "保存",
    "Copy draft" to "复制草稿",
    "Active" to "已启用",
    "Off" to "已关闭",
    "Overridden" to "被覆盖",
    "Needs attention" to "需要关注",
    "All skills" to "全部技能",
    "Inactive skills" to "未启用的技能",
    "Inactive" to "未启用",
    "Issues" to "有问题",
    "Project" to "项目",
    "Danger zone" to "危险区域",
    "Updating…" to "更新中…",
    "Another skill with this name takes precedence. Disable or edit the active copy to use this one." to
        "另一个同名技能优先级更高。禁用或编辑活动副本以使用此技能。",
    "The agent can edit this skill with permission. Changes reload into this session automatically." to
        "代理可以在获得许可后编辑此技能。更改会自动重新加载到当前会话。",

    // ─── Settings: Git ────────────────────────────────────────────────────────
    "Enter this code on GitHub" to "在 GitHub 上输入此代码",
    "Open github.com/login/device" to "打开 github.com/login/device",
    "Cancel" to "取消",
    "Sign out" to "登出",
    "Sign in with GitHub" to "使用 GitHub 登录",
    "Advanced Git settings" to "高级 Git 设置",
    "Task branches and manual credentials" to "任务分支和手动凭据",
    "Auto-branch each task" to "为每个任务自动创建分支",
    "Each new chat works on its own branch of the project" to "每个新对话在项目的独立分支上工作",
    "Push and pull also require a local Git repository with a valid HTTPS origin." to
        "推送和拉取还需要一个具有有效 HTTPS 源的本地 Git 仓库。",

    // ─── Sidebar / PhoneCodeApp ───────────────────────────────────────────────
    "Projects" to "项目",
    "Clear search" to "清除搜索",
    "No chats" to "暂无对话",
    "Pinned" to "已置顶",
    "Archived" to "已归档",
    "New project" to "新建项目",
    "Project options" to "项目选项",

    // ─── Notification (TurnService) ──────────────────────────────────────────
    "Agent activity" to "代理活动",
    "Shown while PhoneCode is working in the background." to "PhoneCode 在后台工作时显示。",
    "PhoneCode is working" to "PhoneCode 正在工作",
    "Agent work and local processes remain active." to "代理工作和本地进程保持活动状态。",
    "Stop" to "停止",

    // ─── Common / Shared ──────────────────────────────────────────────────────
    "Back" to "返回",
    "Loading…" to "加载中…",
    "Search" to "搜索",
    "Next" to "下一步",
    "Submit" to "提交",
    "Skip all" to "全部跳过",
    "Question" to "问题",
    "Choose one, or write your own." to "选择一个，或输入你自己的答案。",
    "Choose any that apply, or write your own." to "选择所有适用的，或输入你自己的答案。",
    "Something else" to "其他",
    "Unknown" to "未知",
    "Version" to "版本",
    "Document unavailable." to "文档不可用。",
    "Copied config path" to "已复制配置路径",

    // ─── Chat: Tool Actions ───────────────────────────────────────────────────
    "Reading file" to "正在读取文件",
    "Read file" to "已读取文件",
    "Writing file" to "正在写入文件",
    "Wrote file" to "已写入文件",
    "Editing code" to "正在编辑代码",
    "Edited code" to "已编辑代码",
    "Browsing files" to "正在浏览文件",
    "Browsed files" to "已浏览文件",
    "Searching code" to "正在搜索代码",
    "Searched code" to "已搜索代码",
    "Running command" to "正在运行命令",
    "Ran command" to "已运行命令",
    "Searching the web" to "正在搜索网络",
    "Searched the web" to "已搜索网络",
    "Opening webpage" to "正在打开网页",
    "Opened webpage" to "已打开网页",
    "Running Git" to "正在运行 Git",
    "Asked a question" to "已提问",
    "Delegating task" to "正在委派任务",
    "Completed delegated task" to "已完成委派任务",
    "Loading skill" to "正在加载技能",
    "Loaded skill" to "已加载技能",
    "Updating tasks" to "正在更新任务",
    "Updated tasks" to "已更新任务",
    "Read failed" to "读取失败",
    "Write failed" to "写入失败",
    "Edit failed" to "编辑失败",
    "File browsing failed" to "文件浏览失败",
    "Code search failed" to "代码搜索失败",
    "Command failed" to "命令失败",
    "Web search failed" to "网络搜索失败",
    "Webpage failed to open" to "网页打开失败",
    "Git operation failed" to "Git 操作失败",
    "Question failed" to "提问失败",
    "Delegated task failed" to "委派任务失败",
    "Skill failed to load" to "技能加载失败",
    "Task update failed" to "任务更新失败",
    "Read stopped" to "读取已停止",
    "Write stopped" to "写入已停止",
    "Edit stopped" to "编辑已停止",
    "File browsing stopped" to "文件浏览已停止",
    "Code search stopped" to "代码搜索已停止",
    "Command stopped" to "命令已停止",
    "Web search stopped" to "网络搜索已停止",
    "Webpage opening stopped" to "网页打开已停止",
    "Git operation stopped" to "Git 操作已停止",
    "Question stopped" to "提问已停止",
    "Delegated task stopped" to "委派任务已停止",
    "Skill loading stopped" to "技能加载已停止",
    "Task update stopped" to "任务更新已停止",
    "Waiting to write file" to "等待写入文件",
    "Waiting to edit code" to "等待编辑代码",
    "Waiting to run command" to "等待运行命令",
    "Waiting to run Git" to "等待运行 Git",

    // ─── Chat: Tool Details / Approval ────────────────────────────────────────
    "Input" to "输入",
    "Output" to "输出",
    "Copy details" to "复制详情",
    "(none)" to "（无）",
    "(no output)" to "（无输出）",
    "Waiting for output…" to "等待输出…",
    "Next section" to "下一部分",
    "Deny" to "拒绝",
    "Approve once" to "批准一次",
    "Turn stopped · Partial output may be incomplete." to "回合已停止·部分输出可能不完整。",
    "Turn failed · Partial output may be incomplete." to "回合失败·部分输出可能不完整。",
    "Read outside linked folders" to "读取关联文件夹之外的内容",
    "External file access" to "外部文件访问",
    "This reads the exact file or folder path shown above. PhoneCode always asks for this access." to
        "这将读取上方显示的确切文件或文件夹路径。PhoneCode 始终会请求此访问权限。",
    "Run an MCP server action" to "运行 MCP 服务器操作",
    "Connected service change" to "连接的服务更改",
    "This enabled MCP server may send data to or change an external service." to
        "此已启用的 MCP 服务器可能会向外部服务发送数据或更改外部服务。",
    "Run a command" to "运行命令",
    "Command execution" to "命令执行",
    "Commands can change files, install software, or contact external services." to
        "命令可以更改文件、安装软件或联系外部服务。",
    "Change files" to "更改文件",
    "Workspace change" to "工作区更改",
    "The agent may create, edit, move, or delete project files." to
        "代理可能会创建、编辑、移动或删除项目文件。",
    "Run a Git operation" to "运行 Git 操作",
    "Repository change" to "仓库更改",
    "This may change branches, commits, or a connected remote repository." to
        "这可能会更改分支、提交或连接的远程仓库。",
    "Contact an external service" to "联系外部服务",
    "External request" to "外部请求",
    "Data in the request may be sent outside this device." to
        "请求中的数据可能会发送到设备外部。",
    "Approval required" to "需要审批",
    "Only approve actions that match what you asked PhoneCode to do." to
        "仅批准与你要求 PhoneCode 执行的操作匹配的请求。",

    // ─── Sidebar: Dialogs ─────────────────────────────────────────────────────
    "Rename chat" to "重命名对话",
    "Chat title" to "对话标题",
    "Rename project" to "重命名项目",
    "Project name" to "项目名称",
    "Delete chat?" to "删除对话？",
    "Delete project?" to "删除项目？",

    // ─── Settings: Skills (additional) ────────────────────────────────────────
    "Your skills" to "你的技能",
    "No skills yet" to "还没有技能",
    "Create one to give the agent a reusable workflow or set of instructions." to
        "创建一个技能，为代理提供可复用的工作流或指令集。",
    "Create skill" to "创建技能",
    "Search skills" to "搜索技能",
    "Describe the tasks or situations that should activate this skill" to
        "描述应激活此技能的任务或场景",
    "Give the agent clear, actionable steps" to "为代理提供清晰、可执行的步骤",
    "Complete SKILL.md source" to "完整的 SKILL.md 源码",
    "Advanced source includes frontmatter and instructions. Keep the name aligned with the skill folder." to
        "高级源码包含 frontmatter 和指令。保持名称与技能文件夹一致。",
    "Fix the SKILL.md source before returning to the guided editor." to
        "请先修复 SKILL.md 源码，再返回引导式编辑器。",
    "Add a valid name, when-to-use description, and instructions before saving." to
        "保存前请添加有效的名称、使用场景描述和指令。",

    // ─── Settings: Providers (additional) ─────────────────────────────────────
    "Provider name" to "服务商名称",
    "Base URL" to "基础 URL",
    "Model IDs" to "模型 ID",
    "Anthropic format" to "Anthropic 格式",
    "Off = OpenAI-compatible (most servers)" to "关闭 = OpenAI 兼容（大多数服务器）",
    "Enter a provider name" to "请输入服务商名称",
    "Enter the provider base URL" to "请输入服务商基础 URL",
    "Add at least one model id" to "至少添加一个模型 ID",
    "This custom provider has unsaved changes." to "此自定义服务商有未保存的更改。",
    "A key is saved securely. Enter a replacement and save it explicitly." to
        "密钥已安全保存。输入新密钥并显式保存。",
    "Keys are saved in Android secure storage and excluded from exports." to
        "密钥保存在 Android 安全存储中，不包含在导出中。",
    "Open-source licenses" to "开源许可证",
    "Waiting for you to authorize on GitHub - this completes automatically." to
        "等待你在 GitHub 上授权——这将自动完成。",
    "Read-only tools work in Plan mode. Changes follow your approval setting in Files & permissions." to
        "只读工具在计划模式下工作。更改遵循“文件和权限”中的审批设置。",

    // ─── Settings: Export & Import ────────────────────────────────────────────
    "Your data" to "你的数据",
    "Importing backup…" to "正在导入备份…",
    "Exports are not encrypted. Saved provider and sign-in credentials are excluded, but chats and tool activity may contain sensitive content." to
        "导出未加密。已保存的服务商和登录凭据不包含在内，但对话和工具活动可能包含敏感内容。",
    "Import replaces chats and settings with the backup. Linked phone folders, provider keys, MCP servers, and skills are not included. Approval always returns to Ask before each change." to
        "导入会用备份替换对话和设置。关联的手机文件夹、服务商密钥、MCP 服务器和技能不包含在内。审批始终恢复为“每次更改前询问”。",
    "Export chats & settings" to "导出对话和设置",
    "Import from a file" to "从文件导入",
    "Replace chats and settings?" to "替换对话和设置？",
    "Choose backup file" to "选择备份文件",
    "Export first" to "先导出",

    // ─── Settings: About ──────────────────────────────────────────────────────
    "Website" to "网站",
    "Config directory" to "配置目录",

    // ─── Settings: MCP (additional) ───────────────────────────────────────────
    "Servers" to "服务器",
    "Search servers" to "搜索服务器",
    "Add server" to "添加服务器",
    "Reconnecting…" to "重新连接中…",
    "Reconnect enabled servers" to "重新连接已启用的服务器",
    "Connecting" to "连接中",
    "Not tested" to "未测试",
    "Off · Test to enable" to "已关闭·测试后启用",
    "Testing…" to "测试中…",
    "Test" to "测试",
    "HTTP headers" to "HTTP 头",
    "Add header" to "添加头",
    "Connection timeout" to "连接超时",
    "Enabled" to "已启用",
    "Test successfully before enabling" to "启用前请先成功测试",
    "Server" to "服务器",
    "Name" to "名称",
    "Protocol" to "协议",
    "Advertised capabilities" to "声明的能力",
    "None" to "无",
    "Available in PhoneCode" to "在 PhoneCode 中可用",
    "No tool calls" to "无工具调用",
    "Tool calls" to "工具调用",
    "This server exposes no tools." to "此服务器未提供工具。",
    "Header values are concealed after saving and encrypted with Android Keystore." to
        "头值在保存后会被隐藏，并使用 Android Keystore 加密。",
    "No MCP servers configured. Add one over HTTPS, or use local HTTP for a server on this device." to
        "未配置 MCP 服务器。通过 HTTPS 添加，或对本地服务器使用 HTTP。",
    "The existing opencode.json has been preserved. Fix it before changing MCP servers here." to
        "已保留现有的 opencode.json。请先修复它，再在此处更改 MCP 服务器。",
    "Your saved server list is still available. Review the affected server and try again." to
        "你保存的服务器列表仍然可用。请检查受影响的服务器并重试。",
    "MCP servers receive tool inputs from the agent. Review the reported tools before enabling; mutating actions follow your approval setting." to
        "MCP 服务器会接收代理的工具输入。启用前请审查报告的工具；变更操作遵循你的审批设置。",
    "Test this changed configuration and review its reported tools before saving it enabled." to
        "请先测试此更改的配置并审查其报告的工具，然后再保存为启用状态。",

    // ─── Settings: Git (additional) ───────────────────────────────────────────
    "Save manual credentials" to "保存手动凭据",
    "Manual Git credentials could not be saved securely." to "手动 Git 凭据无法安全保存。",
    "Sign out of GitHub?" to "登出 GitHub？",
    "This disconnects the GitHub account and clears manual Git credentials. Local repositories and commits stay on this device." to
        "这将断开 GitHub 账户并清除手动 Git 凭据。本地仓库和提交仍保留在此设备上。",
)
