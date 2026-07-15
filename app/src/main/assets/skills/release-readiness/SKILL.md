---
name: release-readiness
description: Decide whether a software release is ready to publish by auditing the shipped artifact, versioning, legal inventory, privacy disclosures, security posture, rollback path, and distribution metadata. Use when preparing a production, store, package, or hosted release.
license: Apache-2.0
compatibility: Applies across release channels and uses the project's existing build and distribution tooling.
---

Define the exact release target, audience, channel, version, and artifact before evaluating readiness. Read the repository's release, security, privacy, licensing, and platform guidance. Treat explicit release gates as blockers, not suggestions.

Audit the release boundary:

1. Confirm version identifiers are monotonic, consistent, and tied to the intended source revision.
2. Build the production configuration and inspect the packaged artifact for debug features, test keys, secrets, development payloads, unexpected permissions, and missing resources.
3. Reconcile data collection, network destinations, account behavior, permissions, background work, and user controls with privacy and distribution declarations.
4. Inventory bundled code, assets, models, runtimes, fonts, and generated content. Verify licenses, notices, source obligations, provenance, and export or platform requirements.
5. Check signing, update compatibility, migration and backup behavior, failure recovery, support links, and rollback ownership.
6. Run the project's validation workflow and smoke-test first launch, onboarding, the primary task, interruption, resume, offline failure, and destructive actions on the release artifact.

Never mark a release ready because source checks passed while the final artifact was not inspected. Do not weaken or bypass a gate to obtain a green result.

Return one decision: ready, ready with non-blocking follow-ups, or blocked. List blockers with owners and evidence, then the shortest remaining release sequence.
