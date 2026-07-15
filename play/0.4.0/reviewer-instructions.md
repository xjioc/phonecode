# Reviewer access and test instructions

Status: **BLOCKED — dedicated review credentials and final-build steps are not yet verified.**

## App access declaration

PhoneCode has no first-party account and can be opened without signing in. Core AI-agent output,
however, requires a third-party model credential or ChatGPT sign-in. Conservatively select **All or
some functionality is restricted** unless the final review build provides a complete credential-free
core path. Google requires valid, reusable review access for restricted functionality; see [Prepare your app for review](https://support.google.com/googleplay/android-developer/answer/9859455?hl=en-EN).

Before submission, create a dedicated reviewer credential for a supported provider that explicitly
permits this use. It must be non-personal, minimally scoped, capped, monitored, valid for the full
review period, and free of MFA or region restrictions. Put the secret only in Play Console's app
access fields, never in this repository, screenshots, listing text, or the evidence video.

| Play Console field | Value |
| --- | --- |
| Instruction name | `PhoneCode model review access` |
| Username | UNRESOLVED or not applicable for the chosen provider |
| Password/API key | BLOCKED — enter the dedicated secret in Play Console only |
| Other instructions | Name the exact provider row, credential field, model to select, expiry policy, and a support contact that can restore access during review |

GitHub, MCP, custom endpoints, package repositories, and paid ChatGPT access must not be required to
complete the primary review path. Do not give reviewers a personal GitHub or ChatGPT account.

## Primary review path

Validate these labels and steps against the exact signed AAB before pasting them into Play Console.

1. Launch PhoneCode, tap **Get started**, then **Skip setup for now**.
2. Open Settings, then the model-provider page named in the private Play Console instructions.
3. Enter the dedicated review credential and select the specified model.
4. Create a disposable project folder using Android's system folder picker.
5. Start a new chat and ask: `Create review.txt containing PhoneCode review test, then read it back.`
6. Approve the file action if the chosen permission mode asks. Verify the response and file content.
7. Open the project drawer to show the project-scoped chat, then open Settings to inspect Skills and
   MCP pages. MCP setup is optional and not required for the test.
8. Open the action menu on an AI response and confirm **Report AI response** is available in-app.
   Do not submit a test report unless the review credential owner has approved that test.
9. Use the separate foreground-service script in [`foreground-service.md`](foreground-service.md)
   to test background execution and the Stop action.
10. Delete the disposable chat/project data and remove the dedicated credential from PhoneCode.

## Reviewer context

- PhoneCode is a coding tool whose agent can modify selected files and run development actions.
- File and photo access uses Android's system picker. A linked folder remains accessible until the
  user unlinks it, revokes the system grant, clears app data, or uninstalls the app.
- The app has no ads, analytics, telemetry, PhoneCode account, subscription, or in-app purchase based
  on the candidate source. Verify those statements against the final AAB.
- Ko-fi and Stripe are intentionally absent from the Play app and review flow. External support does
  not unlock app features or services.
- AI output may vary. The dedicated review model and prompt must be tested repeatedly before
  submission; provide a fallback credential in Play Console if permitted and necessary.
- PhoneCode's in-app report action must remain reachable without leaving the app, and its handling
  must satisfy the current [AI-Generated Content policy](https://support.google.com/googleplay/android-developer/answer/13985936?hl=en).
- The current PRoot/Alpine prototype is not acceptable as the Play release runtime. Do not submit a
  build exposing it as the release execution boundary. Complete and verify the isolated VM gate first.

## Pre-submission access check

- Install the signed AAB through a Play internal-testing track on a clean, non-developer device.
- Follow these instructions with no repository knowledge and no developer shell access.
- Confirm the credential works after reinstall and from a second network.
- Confirm every screen is reachable without hidden gestures or an unavailable personal account.
- Confirm the reviewer can stop active work and delete local test data.
- Keep the credential active until review and any appeal are complete, then revoke it.
- Record the test date, device/API, Play-delivered version, provider/model, and redacted success proof
  in the release evidence index.
