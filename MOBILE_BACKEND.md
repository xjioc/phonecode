# Mobile backend

PhoneCode should use OpenCode as an optional remote execution host. The phone remains the owner of user consent, linked-folder access, notifications, and presentation. OpenCode owns long-running sessions, shells, package installation, model discovery, and provider credentials.

## First release

The first remote release should use the current OpenCode v2 API directly:

- sessions, prompts, history, and resumable session events
- model and provider catalogs
- permission and question requests
- filesystem browsing and reads
- PTY creation and streaming
- project and workspace locations

PhoneCode should keep its local agent available for offline work. The Google Play build must not download executable toolchains. Python, Node.js, npm packages, and other native tools run on the configured OpenCode host.

## Required OpenCode changes

Add a compatibility response that reports the OpenCode version, protocol version, minimum supported PhoneCode version, and supported features. PhoneCode must refuse incompatible servers with a clear upgrade message and may reconnect automatically after a compatible server update.

Add revocable per-device credentials after the manual connection flow is stable. Pairing should use a short-lived code or QR exchange and return a device token that can be named and revoked from the host. Remote connections must use HTTPS. The existing server password remains acceptable for local development.

Do not add a separate mobile agent loop, model catalog, tool registry, or question protocol. PhoneCode should consume the backend catalog and stage multi-question requests as one question per screen before submitting the existing ordered reply.

## Workspace modes

Server workspace is the default. The agent works in a directory already available to the OpenCode host and PhoneCode displays its files and changes.

Phone mirror copies an approved Android folder into a server workspace using relative paths, content hashes, and changed-file transfers. A sync records its base revision. Server changes return as a patch against that base and require the configured Ask or Allow automatically rule before PhoneCode writes them through Android's document provider. Conflicts never overwrite either side silently.

Local workspace keeps the current on-device tools and never sends files to an OpenCode host.

## Ownership

PhoneCode stores the server URL, device credential, workspace mapping, and permission preferences in Android secure storage. OpenCode stores sessions, provider credentials, model configuration, processes, and the mirrored workspace. Provider credentials do not move between the phone and server automatically.

Disconnecting a host stops new remote work but keeps local chat metadata. Removing a mirrored workspace deletes its server copy only after explicit confirmation. The privacy policy and Play Data safety declaration must distinguish local work, direct provider use, and optional remote execution.

## Delivery order

1. Compatibility handshake and Android connection test
2. Dynamic model and provider catalog
3. Remote session, history, and event client
4. Permission and one-question-at-a-time UI bridge
5. Server workspace file browser and PTY
6. Phone mirror with conflict-safe patch return
7. Device pairing and credential revocation
8. Local-to-remote session migration
