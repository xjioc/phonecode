---
name: code-review
description: Review software changes for correctness, security, data loss, concurrency, performance, and maintainability. Use when the user asks for a code review, reliability audit, security review, or pre-merge risk assessment.
license: Apache-2.0
compatibility: Requires repository read access and benefits from the project's existing checks.
---

Review the requested diff or scope without editing it unless the user also asks for fixes. Read repository guidance first, then trace changed behavior through callers, persistence, network, process, and user-interface boundaries.

Prioritize defects that can change behavior or harm users:

1. Incorrect results, crashes, hangs, data loss, broken recovery, and compatibility regressions.
2. Trust-boundary failures involving secrets, authentication, authorization, untrusted input, file paths, network responses, dependencies, or sensitive logs.
3. Races, cancellation leaks, lifecycle mistakes, non-atomic writes, and stale state.
4. Unbounded memory, work, output, retries, or background activity on realistic devices.
5. Missing tests only when they leave important behavior unprotected.

Prove each finding from the code. Follow the value or state to the point where the failure occurs, check whether an existing guard already covers it, and use the project's focused checks when they can confirm the risk. Do not report style preferences, speculative future problems, or a finding whose behavior you have not verified.

Report findings first, ordered by severity. Each finding must include a precise file and line, the failing scenario, user impact, and the smallest credible fix. Keep distinct causes separate. If no actionable defects remain, say so and name the areas or checks that were not verifiable.
