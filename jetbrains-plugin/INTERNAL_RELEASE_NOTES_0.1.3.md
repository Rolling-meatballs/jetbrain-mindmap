# Mindmap JetBrains Internal Release Notes 0.1.3

Date: 2026-03-12
Plugin: `mindmap-jetbrains-0.1.3.zip`

## Highlights

- MMP-006: `.xmind` embedded `resources/*` images are converted to browser-safe data URLs during import, and the fix is now validated with a real XMind sample in the sandbox IDE.
- MMP-006: `.xmind` relation data is preserved as `root.data.xmindRelations` metadata for lossless import.
- MMP-006: Current web UI does not render relation lines yet; relation metadata remains available for follow-up rendering work.
- MMP-007: PNG export validated end-to-end in sandbox (`exportToImage` -> sibling `.png`).
- MMP-009: Bridge smoke coverage completed for loaded/import/save/export flow (codec, builder, dispatcher, roundtrip).
- MMP-003: Behavior updated and validated: double-clicking `.km/.xmind` opens editor tab only; ToolWindow is manual via `Open Mindmap`.
- MMP-010: Replaced deprecated Kotlin `runReadAction` bridge usage, and followed up after verifier feedback by switching bridge file reads to `Application.runReadAction(Computable)`.

## Validation Snapshot

- Build/Test/Runtime matrix baseline was previously executed with `buildPlugin`, `test`, and `runIde`; startup warnings remain classified as non-blocking platform/sandbox noise.
- 2026-03-12 follow-up: user validated the real sample `.xmind` file and confirmed embedded images display correctly in the JetBrains sandbox editor.
- 2026-03-12 follow-up: plugin `0.1.3` was uploaded successfully to JetBrains Marketplace default channel.
- 2026-03-12 follow-up: packaged artifact checksum was recorded (`c39909fcb1eda814d7fd130be1e7aec170b5fb18fd0268b4fc50d9a63a8961d3`), and `bundled-webui/**` resources were verified inside `build/libs/mindmap-jetbrains-0.1.3.jar`.
- 2026-03-12 follow-up: verifier still reported `ReadAction.compute(ThrowableComputable)` as deprecated, so bridge file loading was updated again to use `Application.runReadAction(Computable)` and now requires one more verifier pass.
- 2026-03-12 follow-up: local Gradle `test` and targeted smoke test invocations still hang in the current environment and did not produce stable test result files; this needs separate follow-up after release.

## Known Limits

- MMP-006 follow-up: relation line rendering in web UI is pending (metadata already preserved).
- MMP-010 follow-up: JetBrains Marketplace now has a published `0.1.3`, but a fresh build plus verifier rerun is still required to confirm the final deprecated API warning is gone.
- MMP-010 follow-up: local IntelliJ Platform Gradle test execution still hangs in the current environment and needs a separate fix/diagnosis.
