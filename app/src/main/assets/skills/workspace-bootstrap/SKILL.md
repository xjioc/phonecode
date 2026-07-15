---
name: workspace-bootstrap
description: Prepare or diagnose a local development workspace before implementation. Use when a project is newly opened, its toolchain is unclear, commands fail because the environment is incomplete, or a reproducible setup plan is needed.
license: Apache-2.0
compatibility: Uses only the workspace files and locally available tools.
---

Treat repository files as the source of truth. Read the project instructions, top-level documentation, manifests, lockfiles, checked-in wrappers, version files, and automation before installing or changing anything.

Bootstrap in this order:

1. Identify the project roots, declared toolchains, package managers, required versions, and normal build or start commands.
2. Inspect what is already available in the environment. Record executable paths and versions for only the tools the project declares.
3. Reuse checked-in wrappers, lockfiles, caches, and scripts. Do not introduce a second package manager or replace a working project command.
4. If something is missing, explain the smallest installation or configuration needed and its impact before performing a privileged, global, or destructive action.
5. Run the cheapest command that proves the environment works, then the project's own focused check.
6. Start servers, watchers, and other long-running commands through the process manager when available. Report how to inspect and stop them.

When diagnosis fails, separate project defects from unavailable tools, permissions, network access, storage, architecture, and operating-system constraints. Preserve the exact command, exit code, and first useful error. Never claim a runtime or service is ready until a local probe succeeds.

Finish with a compact readiness report: detected stack, verified commands, installed or missing requirements, active processes, and the next actionable blocker.
