# Mindmap JetBrains Internal Release Notes 0.1.0

Date: 2026-03-06
Plugin: `mindmap-jetbrains-0.1.0.zip`

## Highlights

- MMP-006: `.xmind` relation data is preserved as `root.data.xmindRelations` metadata for lossless import.
- MMP-006: Current web UI does not render relation lines yet; relation metadata remains available for follow-up rendering work.
- MMP-007: PNG export validated end-to-end in sandbox (`exportToImage` -> sibling `.png`).
- MMP-009: Bridge smoke coverage completed for loaded/import/save/export flow (codec, builder, dispatcher, roundtrip).
- MMP-003: Behavior updated and validated: double-clicking `.km/.xmind` opens editor tab only; ToolWindow is manual via `Open Mindmap`.

## Validation Snapshot

- Build/Test/Runtime matrix executed on 2026-03-06: `buildPlugin`, `test`, `runIde` passed.
- Startup warnings were reviewed against MMP-004 baseline and classified as non-blocking platform/sandbox noise.

## Known Limits

- MMP-006 follow-up: relation line rendering in web UI is pending (metadata already preserved).
