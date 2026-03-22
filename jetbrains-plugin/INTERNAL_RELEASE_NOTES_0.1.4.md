# Mindmap JetBrains Internal Release Notes 0.1.4

Date: 2026-03-12
Plugin: `mindmap-jetbrains-0.1.4.zip`

## Highlights

- MMP-006: `.xmind` embedded `resources/*` images are converted to browser-safe data URLs during import, and the fix is validated with a real XMind sample in the sandbox IDE.
- MMP-006: `.xmind` relation data is preserved as `root.data.xmindRelations` metadata for lossless import.
- MMP-006: Current web UI does not render relation lines yet; relation metadata remains available for follow-up rendering work.
- MMP-007: PNG export validated end-to-end in sandbox (`exportToImage` -> sibling `.png`).
- MMP-009: Bridge smoke coverage exists for loaded/import/save/export flow (codec, builder, dispatcher, roundtrip), though local Gradle test execution still hangs in the current environment.
- MMP-003: Behavior updated and validated: double-clicking `.km/.xmind` opens editor tab only; ToolWindow is manual via `Open Mindmap`.
- MMP-010: Bridge file loading now uses `Application.runReadAction(Computable)` as a follow-up compatibility fix for the 2026.1 verifier deprecated API warning.

## Validation Snapshot

- `0.1.3` was uploaded successfully to JetBrains Marketplace default channel on 2026-03-12.
- Real-sample `.xmind` import validation confirmed embedded images display correctly in the JetBrains sandbox editor.
- `0.1.3` packaging validation recorded checksum `c39909fcb1eda814d7fd130be1e7aec170b5fb18fd0268b4fc50d9a63a8961d3`, and `bundled-webui/**` resources were verified inside the plugin jar.
- `0.1.4` is the follow-up release target created specifically to rebuild and rerun verifier checks after the final read-action API change.

## Known Limits

- MMP-006 follow-up: relation line rendering in web UI is pending (metadata already preserved).
- MMP-010 follow-up: a fresh `0.1.4` build plus verifier rerun is still required to confirm the deprecated API warning is gone.
- MMP-010 follow-up: local IntelliJ Platform Gradle `test` execution still hangs in the current environment and needs separate diagnosis.
