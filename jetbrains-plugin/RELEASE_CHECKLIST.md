# Mindmap JetBrains Release Checklist

Last updated: 2026-03-12

Use this checklist for MMP-010 before cutting an internal release.

## 1) Version & Metadata

- [x] Bump plugin version in `build.gradle.kts`
- [x] Verify plugin id/name/vendor in `src/main/resources/META-INF/plugin.xml`
- [x] Verify `sinceBuild` / `untilBuild` compatibility window in `build.gradle.kts`

## 2) Build & Test Gate

- [ ] `./gradlew clean buildPlugin`
- [ ] `./gradlew test`
- [ ] Targeted smoke tests:
  - [ ] `./gradlew test --tests com.souche.mindmap.idea.XmindConverterTest`
  - [ ] `./gradlew test --tests com.souche.mindmap.idea.MindmapBridgeRoundtripTest`

## 3) Runtime Validation

- [ ] `./gradlew runIde` starts sandbox successfully
- [x] Open `.km` file, edit, and save succeeds
- [x] Open `.xmind` file, import displays correctly, save creates sibling `.km`
- [x] Real-sample `.xmind` embedded `resources/*` images render correctly in sandbox
- [x] `exportToImage` creates sibling `.png`
- [x] `.xmind` relation metadata notice appears when relation data exists

## 4) Packaging Integrity

- [x] `build/generated-resources/bundled-webui/manifest.txt` exists
- [ ] Manifest includes:
  - [x] `mindmap.html`
  - [x] `dist/kityminder.editor.min.js`
  - [x] `dist/main.min.js`
  - [x] `bower_components/marked/lib/marked.js`
- [ ] Distribution zip exists under `build/distributions/`
- [ ] Plugin jar contains `bundled-webui/**` (verify via `jar tf build/libs/mindmap-jetbrains-0.1.4.jar`)

## 5) Known Non-Blocking Warnings (Documented)

- [x] CDS warning with `PathClassLoader`
- [x] Missing `CFBundleURLTypes`
- [x] Ultimate plugin dependency noise in sandbox
- [x] `LoadingState` noise in sandbox startup
- [x] `nativecerts` / Grazie cloud login warnings
- [x] JCEF launcher/sandbox warnings in non-launcher runs

## 6) Release Notes (Internal)

- [x] Mention MMP-006 relation strategy: metadata preserved as `xmindRelations` (no line rendering yet)
- [x] Mention MMP-006 embedded image import fix and real-sample validation
- [x] Mention MMP-010 deprecated API cleanup follow-up (`Application.runReadAction(Computable)`)
- [x] Mention MMP-007 PNG export validated in sandbox
- [x] Mention MMP-009 bridge smoke tests completed
- [x] Mention MMP-003 behavior update: double-click opens editor only; ToolWindow is manual (`Open Mindmap`)

## 7) Artifacts & Handoff

- [ ] Archive plugin zip checksum (SHA256) for traceability
- [ ] Record command outputs or screenshots for key validations
- [ ] Update `PORTING_TASKS.md` with final MMP-010 status

## 8) Sign-off Record (Fill Before Internal Release)

### Release Identity

- Release version: `0.1.4`
- Build date (YYYY-MM-DD): `2026-03-12`
- Plugin zip path: `build/distributions/mindmap-jetbrains-0.1.4.zip`
- Plugin zip size: `________________`
- SHA256 (`shasum -a 256 <zip>`): `________________`

### Compatibility Window

- Target IDE baseline: `IntelliJ Platform 253-based IDEs` (Marketplace currently resolves to Android Studio, AppCode, Aqua, CLion, DataGrip, DataSpell, GoLand, IntelliJ IDEA/Community, JetBrains Client/Gateway, MPS, PhpStorm, PyCharm/Community, Rider, RubyMine, RustRover, WebStorm, Writerside)
- `sinceBuild`: `253`
- `untilBuild`: `253.*`

### Validation Evidence

- Matrix run log path: `Terminal session outputs for 0.1.4 validation (buildPlugin/test/runIde)`
- Runtime validation evidence path (screenshots/logs): `PORTING_TASKS.md progress entries for MMP-003/MMP-006/MMP-007/MMP-008/MMP-010 and INTERNAL_RELEASE_NOTES_0.1.4.md`
- Known warnings reviewed against baseline (`MMP-004`): `[x] yes  [ ] no`
- MMP-003 behavior update acknowledged in release note: `[x] yes  [ ] no`
- 2026.1 verifier warning fully cleared for deprecated read-action API: `[ ] yes  [x] no`

### Approval

- QA/Validator: `________________`
- Tech owner: `________________`
- Approval date (YYYY-MM-DD): `________________`
- Release decision: `[ ] GO  [ ] NO-GO`
- Decision notes: `________________`
