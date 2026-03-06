# Mindmap JetBrains Porting Tasks

Last updated: 2026-03-06

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
| MMP-004 | Stabilize warnings/noise under 2025.3.3 sandbox | P1 | dev | done | Warnings classified: platform noise vs plugin-actionable; no feature blocker remains |
| MMP-005 | Implement file editor provider (open as editor tab, not only ToolWindow) | P1 | dev | done | Added FileEditorProvider for `.km/.xmind`; fixed 2025.3 provider/fileType compatibility |
| MMP-006 | Improve `.xmind` parser coverage (markers/notes/relations) | P1 | dev | done | Added note/hyperlink/labels/marker + style/image mapping; relations imported as `xmindRelations` metadata with explicit runtime notice |
| MMP-007 | Verify PNG export with real KityMinder page in sandbox | P1 | dev | done | End-to-end verified: export button -> bridge -> `.png` file in sandbox project |
| MMP-008 | Package webui assets reliably for build/release | P0 | dev | done | Validation matrix executed: buildPlugin/test/runIde passed; startup warnings classified as non-blocking |
| MMP-009 | Add automated smoke tests for bridge commands | P2 | dev | done | Loaded/import/save/export helper-level smoke coverage completed and validated via Gradle tests |
| MMP-010 | Prepare release checklist (build, sign, compatibility) | P2 | dev | in_progress | Checklist drafted in `RELEASE_CHECKLIST.md`; execution/sign-off pending |

## Immediate Next Steps

1. Complete MMP-010: execute `RELEASE_CHECKLIST.md` items and record sign-off artifacts.
2. Revisit relation line rendering in webui as a post-parity enhancement (metadata already preserved).
3. Triage MMP-003 auto-open instability separately from release path.

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
- 2026-03-05 | MMP-004 | in_progress | Switched JCEF query creation to `JBCefBrowserBase` overload to remove 2025.3 deprecation warnings.
- 2026-03-05 | MMP-006 | in_progress | Added basic style/image extraction from `.xmind` JSON/XML (font/color/background/image url+size) for richer import fidelity.
- 2026-03-05 | MMP-006 | in_progress | Fixed nullable-safe call regression in XML hyperlink extraction after style/image mapping changes.
- 2026-03-05 | MMP-008 | in_progress | Added `webui/bower_components/marked/lib/marked.js` compatibility file to satisfy legacy asset path and avoid fallback html mode.
- 2026-03-05 | MMP-008 | in_progress | User verified sandbox now opens graphical mindmap UI (no fallback JSON-text page).
- 2026-03-05 | MMP-007 | in_progress | Hardened save/export target resolution and show full saved/exported paths in notifications for end-to-end validation.
- 2026-03-05 | MMP-007 | in_progress | Added PNG export promise error reporting from webui and bridge-side `clientError` handling; missing current file now reports explicit notification.
- 2026-03-05 | MMP-007 | in_progress | Added runtime export-button compatibility hook in webui shim to force `exportToImage` postMessage with explicit error feedback when legacy bundled JS swallows export errors.
- 2026-03-05 | MMP-007 | in_progress | Fixed shim early-return logic so export compatibility hook is always installed even when host already provides `acquireVsCodeApi`.
- 2026-03-05 | MMP-007 | in_progress | Moved save/export actions to shim-level button hooks with direct host postMessage fallback and forced initial `loaded` event to bypass unreliable legacy window messaging.
- 2026-03-05 | MMP-007 | in_progress | Added `.mindmap-bridge.log` command tracing + shim `debugProbe` messages to diagnose silent save/export clicks in sandbox.
- 2026-03-05 | MMP-007 | in_progress | Extended bridge tracing to always write under IDE log path (`idea.log.path/mindmap-bridge.log`) because sandbox project base may differ from repo path.
- 2026-03-05 | MMP-007 | in_progress | Added detailed bridge trace fields (`message`, saved/exported bytes/path) and PNG export timeout guard to surface hangs as explicit `clientError`.
- 2026-03-05 | MMP-007 | in_progress | Moved save/export file writes to `invokeLater` (EDT) with queued/success/failure trace logs to avoid background write-action stalls in 2025.3 sandbox.
- 2026-03-05 | MMP-007 | in_progress | Added bridge handler exception guard + payload-size tracing (`save_payload_chars`/`export_payload_chars`) to locate silent early returns in save/export path.
- 2026-03-05 | MMP-007 | in_progress | Normalized incoming command (`trim`) and logged raw command text to diagnose hidden whitespace causing command branch miss.
- 2026-03-05 | MMP-007 | in_progress | Fixed write-access violation by moving `.xmind -> .km` and `.png` target creation into write-action blocks (no VFS writes on JCEF handler thread).
- 2026-03-05 | MMP-007 | in_progress | Hardened shim export flow to catch synchronous `minder.exportData('png')` errors, add export payload-size probe, and fail explicitly when host postMessage transport fails.
- 2026-03-05 | MMP-007 | in_progress | Added PNG export recovery for malformed image nodes (`image` without `imageSize`) and made xmind image import require valid width/height to prevent `reading 'width'` export crash.
- 2026-03-05 | MMP-007 | done | User validated PNG export end-to-end in sandbox: `exportToImage` -> `command=exportToImage` -> `exported path=.../E项目V2.0需求.png`.
- 2026-03-06 | MMP-004 | done | Classified sandbox startup warnings: ignorable platform noise (`cds`, `CFBundleURLTypes`, `com.intellij.modules.ultimate` dependency set, `nativecerts`, `JCEF-sandbox`, Grazie/cloud login) vs actionable plugin defects (save write-action/threading, export image payload), and verified no remaining feature blocker.
- 2026-03-05 | MMP-006 | in_progress | Extended xmind conversion to include note/hyperlink/resource/priority/progress fields from JSON/XML.
- 2026-03-06 | MMP-006 | in_progress | Added `.xmind` relationship parsing from JSON/XML and persisted as `root.data.xmindRelations` in `.km` for lossless import (visual relation rendering in webui still pending).
- 2026-03-06 | MMP-009 | in_progress | Added bridge outbound message codec/import-message builder tests; user validated `MindmapBridgeMessageCodecTest` and `MindmapBridgeImportMessageBuilderTest` via Gradle.
- 2026-03-06 | MMP-009 | in_progress | Added `MindmapBridgeCommandDispatcher` and smoke tests for loaded/save/exportToImage dispatch, payload forwarding, and error wrapping.
- 2026-03-06 | MMP-009 | in_progress | User validated `./gradlew test --tests com.souche.mindmap.idea.MindmapBridgeCommandDispatcherTest` passed successfully.
- 2026-03-06 | MMP-009 | in_progress | Added `MindmapBridgeRoundtripTest` to verify loaded/import message build + save/export command dispatch + outbound codec in one helper-level roundtrip scenario.
- 2026-03-06 | MMP-009 | done | User validated `./gradlew test --tests com.souche.mindmap.idea.MindmapBridgeRoundtripTest` passed successfully; bridge smoke test scope closed.
- 2026-03-06 | MMP-006 | done | Finalized relation strategy: converter now preserves JSON/XML relationships as `root.data.xmindRelations`, added converter regression tests, and bridge shows explicit info notification that relation lines are metadata-only in current web UI.
- 2026-03-06 | MMP-008 | in_progress | Added `RELEASE_VALIDATION_MATRIX.md` (packaging integrity, runtime feature checks, automated test gate, release decision criteria) and linked it from README.
- 2026-03-06 | MMP-008 | done | User executed matrix core commands (`buildPlugin`, `test`, `runIde`) successfully; observed sandbox startup warnings remain non-blocking (Ultimate deps, LoadingState noise, nativecerts/Grazie/JCEF launcher warnings).
- 2026-03-06 | MMP-010 | in_progress | Added `RELEASE_CHECKLIST.md` (version/metadata, build-test gate, runtime validation, packaging integrity, warning baseline, release notes, handoff artifacts) and linked it from README.
- 2026-03-06 | MMP-010 | in_progress | Extended `RELEASE_CHECKLIST.md` with sign-off template (release identity, compatibility window, evidence paths, validator/owner approval, GO/NO-GO decision fields).
- 2026-03-06 | MMP-010 | in_progress | Pre-filled checklist evidence from current build artifacts and validations (zip path/size/SHA256, since/until build, manifest+bundled-webui checks, warning baseline), leaving approval/release-note sign-off items pending.
- 2026-03-06 | MMP-003 | in_progress | 临时变更申请已确认：双击 `.km/.xmind` 仅打开文件编辑器，不再自动弹出 Mindmap 侧边 ToolWindow（保留手动 `Open Mindmap` 入口）。
- 2026-03-06 | MMP-003 | in_progress | 已移除 `MindmapStartupActivity` 中 selection-change 自动 `ToolWindow.show()` 逻辑；双击文件现在仅打开编辑器，ToolWindow 需手动触发（待你本地 runIde 复核）。
