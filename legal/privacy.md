# PhoneCode Privacy Policy

_Last updated: 10 July 2026_

This Privacy Policy explains how PhoneCode handles data. PhoneCode is an on-device AI coding client published by Deyan Todorov. The developer does not operate a PhoneCode backend and does not receive your prompts, files, credentials, or usage data.

## 1. Data PhoneCode keeps on your device

PhoneCode stores the following locally so the app can work:

- API keys, Git credentials, and third-party sign-in tokens, encrypted with the Android Keystore. If secure storage is unavailable, PhoneCode does not save credentials.
- Projects, workspace files, linked-folder references, chat history, provider configuration, and app settings.
- An optional Alpine Linux environment and packages you install.

Android cloud backup is disabled for PhoneCode. You can create a manual export of supported chats and settings. Manual exports are not encrypted, so store and share them carefully. They do not contain API keys or sign-in credentials.

## 2. Files and photos you choose

PhoneCode can access files or photos only after you select them through Android's system picker. If you link a folder, Android grants PhoneCode continuing access to that folder until you unlink it, revoke access in system settings, clear the app's data, or uninstall the app. The agent may read, create, change, rename, or delete content within a linked folder according to your permission setting and instructions.

Files remain on your device unless you ask the agent to send relevant content to an AI provider, Git host, search service, or another destination. Review linked-folder access and agent permission settings before allowing automatic changes.

## 3. Data sent to services you choose

PhoneCode does not contain an AI model. When you use the agent, the app sends your prompt and the context needed for the request directly from your device to the AI provider you selected. This may include text, source code, attached photos or files, tool results, and content the agent reads from a workspace or linked folder.

You may connect services such as OpenAI or ChatGPT, Anthropic, OpenRouter, OpenCode Zen or Go, Google, xAI, DeepSeek, Mistral, a custom provider, or a Git host. PhoneCode sends credentials only to authenticate with the service they belong to. The selected service's privacy policy, retention rules, and terms apply once it receives data. The developer cannot access or delete data held by those services.

These transfers are optional and initiated by features you choose. Do not send confidential, personal, or regulated information unless you are authorized to do so and accept the receiving service's practices.

## 4. Other network requests

Depending on the features you use, PhoneCode may connect directly to:

- Git hosts to clone, fetch, pull, or push repositories.
- DuckDuckGo or a model provider's search service for searches requested by you or the agent.
- models.dev to refresh public provider and model metadata. Prompts and workspace files are not included in this request.
- Alpine Linux repositories and any package source you add to install software inside the optional Linux environment.
- MCP servers or custom endpoints you configure.

The receiving service can see normal network information such as your IP address and request metadata. Its own privacy policy applies.

## 5. Data the developer collects

PhoneCode contains no advertising SDK, analytics, telemetry, or remote crash-reporting service. The developer does not receive or sell personal data from the app. User-directed transfers to third-party services are described above.

## 6. Security

Credentials are stored using Android Keystore-backed encryption and are excluded from manual exports. Network connections use HTTPS where supported by the service. Custom endpoints and software installed by the agent are under your control and may have different security properties. No security measure is perfect; protect your device and revoke provider credentials if you believe they were exposed.

## 7. Retention and deletion

Local data remains until you delete it in PhoneCode, clear the app's storage, or uninstall the app. Unlinking a folder removes PhoneCode's saved access but does not delete that folder. Uninstalling PhoneCode does not delete remote repositories, provider records, or third-party accounts.

To access or delete data held by an AI provider, Git host, search service, MCP server, or package source, use that service's account and privacy controls. PhoneCode has no account of its own, so there is no PhoneCode account to delete.

## 8. Children

PhoneCode is not directed to children under 13. Users must also meet the age requirements of every third-party service they connect.

## 9. Changes

This policy may change as PhoneCode changes. The current policy is included in the app and will show a new date when updated.

## 10. Contact

For privacy questions, use the contact form at [dttdrv.xyz](https://dttdrv.xyz/#contact). Product information is available at [dttdrv.xyz/phonecode](https://dttdrv.xyz/phonecode), and source code is available at [github.com/dttdrv/phonecode](https://github.com/dttdrv/phonecode).
