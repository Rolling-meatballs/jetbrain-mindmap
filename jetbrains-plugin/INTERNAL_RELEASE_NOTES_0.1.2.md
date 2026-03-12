# Mindmap JetBrains Internal Release Notes 0.1.2

Date: 2026-03-11
Plugin: `mindmap-jetbrains-0.1.2.zip`

## Highlights

- MMP-006: `.xmind` embedded `resources/*` images are now converted to browser-safe data URLs during import, so packaged image attachments can render inside the JCEF editor.
- MMP-006: `.xmind` relation data is preserved as `root.data.xmindRelations` metadata for lossless import.
- MMP-006: Current web UI does not render relation lines yet; relation metadata remains available for follow-up rendering work.
- MMP-007: PNG export validated end-to-end in sandbox (`exportToImage` -> sibling `.png`).
- MMP-009: Bridge smoke coverage completed for loaded/import/save/export flow (codec, builder, dispatcher, roundtrip).
- MMP-003: Behavior updated and validated: double-clicking `.km/.xmind` opens editor tab only; ToolWindow is manual via `Open Mindmap`.
- MMP-010: Replaced deprecated `runReadAction` bridge usage with `ReadAction.compute(...)` to keep 2026.1+ plugin verification clean.

## Validation Snapshot

- Build/Test/Runtime matrix executed on 2026-03-06: `buildPlugin`, `test`, `runIde` passed.
- Startup warnings were reviewed against MMP-004 baseline and classified as non-blocking platform/sandbox noise.
- 2026-03-11 follow-up: deprecated API warning from Marketplace verifier was addressed in bridge file loading; local Gradle revalidation is still required outside the current CLI sandbox.

## Known Limits

- MMP-006 follow-up: embedded image fix still needs local validation with the real sample `.xmind` file.
- MMP-006 follow-up: relation line rendering in web UI is pending (metadata already preserved).
