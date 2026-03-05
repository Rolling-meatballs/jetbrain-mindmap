# Mindmap JetBrains Porting Tasks

Last updated: 2026-03-05

## Tracking Rules

- Update this file before and after each migration change.
- Keep task states synchronized with code reality: `todo`, `in_progress`, `done`, `blocked`.
- Add one entry to `## Progress Log` for every completed task or blocked issue.

## Goal

Port `vscode-mindmap` to JetBrains (PyCharm Professional 2025.3.3) with feature parity for:

- `.km` edit/save
- `.xmind` import + save as `.km`
- PNG export
- embedded web UI editor experience

## Current Baseline

- Host platform: PyCharm Professional `2025.3.3` (build `253`)
- Build plugin: `org.jetbrains.intellij.platform` `2.10.5`
- Runtime bridge: JS <-> Kotlin message protocol available (`loaded/import/save/exportToImage`)
- Web UI strategy: bundled webui -> project webui -> fallback page

## Task Board

| ID | Task | Priority | Owner | Status | Notes |
|---|---|---|---|---|---|
| MMP-001 | Migrate Gradle build to IntelliJ Platform Plugin 2.x | P0 | dev | done | Switched to 2.x and PyCharm Pro target |
| MMP-002 | Ensure Gradle wrapper bootstrap for local setup | P0 | dev | done | `scripts/bootstrap-wrapper.sh` available |
| MMP-003 | Add auto-open behavior for `.km/.xmind` files | P1 | dev | blocked | Behavior not stable on PyCharm Pro 2025.3 sandbox |
| MMP-004 | Stabilize warnings/noise under 2025.3.3 sandbox | P1 | dev | in_progress | Non-blocking startup warnings still present |
| MMP-005 | Implement file editor provider (open as editor tab, not only ToolWindow) | P1 | dev | done | Added FileEditorProvider for `.km/.xmind`; fixed 2025.3 provider/fileType compatibility |
| MMP-006 | Improve `.xmind` parser coverage (markers/notes/relations) | P1 | dev | in_progress | Added note/hyperlink/labels/marker mapping; relations/style pending |
| MMP-007 | Verify PNG export with real KityMinder page in sandbox | P1 | dev | in_progress | Host save path ready, full UI validation pending |
| MMP-008 | Package webui assets reliably for build/release | P0 | dev | in_progress | Bundling exists, need release validation matrix |
| MMP-009 | Add automated smoke tests for bridge commands | P2 | dev | todo | loaded/import/save/exportToImage roundtrip |
| MMP-010 | Prepare release checklist (build, sign, compatibility) | P2 | dev | todo | For first internal release |

## Immediate Next Steps

1. Complete MMP-004: collect and classify sandbox startup warnings (actionable vs ignorable).
2. Complete MMP-007: run export-to-image end-to-end validation on real webui.
3. Complete MMP-006: cover relations/style/image and add sample regression files.

## Progress Log

- 2026-03-05 | MMP-001 | done | Migrated build to IntelliJ Platform Gradle Plugin 2.x and PyCharm Pro 2025.3.3.
- 2026-03-05 | MMP-002 | done | Added bootstrap wrapper script and documented wrapper-first workflow.
- 2026-03-05 | MMP-003 | done | Added auto-open ToolWindow logic when selecting `.km/.xmind` files.
- 2026-03-05 | MMP-008 | in_progress | Added bundled webui extraction/loading fallback chain; release validation pending.
- 2026-03-05 | MMP-003 | blocked | User reported auto-open ToolWindow does not reliably trigger in PyCharm Pro 2025.3 sandbox.
- 2026-03-05 | MMP-005 | done | Implemented `FileEditorProvider` and `MindmapFileEditor` for `.km/.xmind` direct opening.
- 2026-03-05 | MMP-005 | done | Fixed runtime compat: registered `MindmapFileType` class in plugin.xml, made editor provider `DumbAware`, and implemented `MindmapFileEditor.getFile()`.
- 2026-03-05 | MMP-004 | in_progress | Adjusted file type descriptor to include `language="TEXT"` with implementationClass registration for PyCharm 2025.3 compatibility.
- 2026-03-05 | MMP-006 | in_progress | Fixed JS bridge UTF-8 decode path (`atob` -> `TextDecoder`) to avoid Chinese text mojibake when importing `.xmind`.
- 2026-03-05 | MMP-006 | in_progress | Extended xmind conversion to include note/hyperlink/resource/priority/progress fields from JSON/XML.
