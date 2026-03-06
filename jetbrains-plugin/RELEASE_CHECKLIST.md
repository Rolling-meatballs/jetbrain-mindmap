# Mindmap JetBrains Release Checklist

Last updated: 2026-03-06

Use this checklist for MMP-010 before cutting an internal release.

## 1) Version & Metadata

- [ ] Bump plugin version in `build.gradle.kts`
- [x] Verify plugin id/name/vendor in `src/main/resources/META-INF/plugin.xml`
- [x] Verify `sinceBuild` / `untilBuild` compatibility window in `build.gradle.kts`

## 2) Build & Test Gate

- [x] `./gradlew clean buildPlugin`
- [x] `./gradlew test`
- [ ] Targeted smoke tests:
  - [x] `./gradlew test --tests com.souche.mindmap.idea.XmindConverterTest`
  - [x] `./gradlew test --tests com.souche.mindmap.idea.MindmapBridgeRoundtripTest`

## 3) Runtime Validation

- [x] `./gradlew runIde` starts sandbox successfully
- [x] Open `.km` file, edit, and save succeeds
- [x] Open `.xmind` file, import displays correctly, save creates sibling `.km`
- [x] `exportToImage` creates sibling `.png`
- [x] `.xmind` relation metadata notice appears when relation data exists

## 4) Packaging Integrity

- [x] `build/generated-resources/bundled-webui/manifest.txt` exists
- [ ] Manifest includes:
  - [x] `mindmap.html`
  - [x] `dist/kityminder.editor.min.js`
  - [x] `dist/main.min.js`
  - [x] `bower_components/marked/lib/marked.js`
- [x] Distribution zip exists under `build/distributions/`
- [x] Plugin jar contains `bundled-webui/**` (checked via `jar tf build/libs/mindmap-jetbrains-0.1.0.jar`)

## 5) Known Non-Blocking Warnings (Documented)

- [x] CDS warning with `PathClassLoader`
- [x] Missing `CFBundleURLTypes`
- [x] Ultimate plugin dependency noise in sandbox
- [x] `LoadingState` noise in sandbox startup
- [x] `nativecerts` / Grazie cloud login warnings
- [x] JCEF launcher/sandbox warnings in non-launcher runs

## 6) Release Notes (Internal)

- [ ] Mention MMP-006 relation strategy: metadata preserved as `xmindRelations` (no line rendering yet)
- [ ] Mention MMP-007 PNG export validated in sandbox
- [ ] Mention MMP-009 bridge smoke tests completed
- [ ] Mention MMP-003 remains blocked and tracked separately

## 7) Artifacts & Handoff

- [x] Archive plugin zip checksum (SHA256) for traceability
- [ ] Record command outputs or screenshots for key validations
- [ ] Update `PORTING_TASKS.md` with final MMP-010 status

## 8) Sign-off Record (Fill Before Internal Release)

### Release Identity

- Release version: `0.1.0`
- Build date (YYYY-MM-DD): `2026-03-06`
- Plugin zip path: `build/distributions/mindmap-jetbrains-0.1.0.zip`
- Plugin zip size: `5638813 bytes`
- SHA256 (`shasum -a 256 <zip>`): `427e80b006af4e20b2ae61d45c699a2fd848fb36d8ceb405baefffb1e8fdbcba`

### Compatibility Window

- Target IDE: `PyCharm Professional 2025.3.3 (build 253)`
- `sinceBuild`: `253`
- `untilBuild`: `253.*`

### Validation Evidence

- Matrix run log path: `Terminal session outputs on 2026-03-06 (buildPlugin/test/runIde)`
- Runtime validation evidence path (screenshots/logs): `PORTING_TASKS.md progress entries for MMP-006/MMP-007/MMP-008`
- Known warnings reviewed against baseline (`MMP-004`): `[x] yes  [ ] no`
- MMP-003 blocked status acknowledged in release note: `[ ] yes  [x] no`

### Approval

- QA/Validator: `________________`
- Tech owner: `________________`
- Approval date (YYYY-MM-DD): `________________`
- Release decision: `[ ] GO  [ ] NO-GO`
- Decision notes: `________________`
