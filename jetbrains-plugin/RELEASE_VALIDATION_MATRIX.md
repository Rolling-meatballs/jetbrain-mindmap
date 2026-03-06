# Mindmap JetBrains Release Validation Matrix

Last updated: 2026-03-06

This matrix is for MMP-008 (webui packaging reliability) and pre-release confidence checks.

## A. Environment Baseline

| Item | Required |
|---|---|
| Host IDE | PyCharm Professional 2025.3.3 (build 253) |
| JDK | 21 (JetBrains Runtime/OpenJDK 21.x) |
| Gradle | Wrapper (`./gradlew`) only |
| Plugin build | `org.jetbrains.intellij.platform` 2.10.5 |

## B. Packaging Integrity (Must Pass)

Run from `jetbrains-plugin`:

```bash
./gradlew clean buildPlugin
```

| Check | Command / Method | Pass Criteria |
|---|---|---|
| Bundled webui manifest generated | inspect `build/generated-resources/bundled-webui/manifest.txt` | file exists and contains key entries |
| Critical webui files included | search manifest for `mindmap.html`, `dist/kityminder.editor.min.js`, `dist/main.min.js`, `bower_components/marked/lib/marked.js` | all present |
| Packaged plugin contains bundled webui | inspect `build/distributions/*.zip` or sandbox plugin jar | `bundled-webui/**` present |
| Plugin loads bundled ui in sandbox | run `./gradlew runIde`, open `.km/.xmind` | no fallback skeleton; graphical mindmap UI appears |

## C. Runtime Feature Validation (Must Pass)

Run sandbox:

```bash
./gradlew runIde
```

| Scenario | Steps | Expected Result |
|---|---|---|
| Open `.km` | open a `.km` file | mindmap editor opens and renders correctly |
| Open `.xmind` | open a `.xmind` file | mindmap renders; chinese text not garbled |
| Save from `.km` | click `save` | `.km` updated; bridge log shows `saved path=` |
| Save from `.xmind` | click `save` | sibling `.km` generated/updated |
| Export PNG | click `exportToImage` | sibling `.png` generated |
| Relation metadata import | open `.xmind` containing relations | `xmindRelations` preserved in generated `.km`; info notice indicates metadata-only relation handling |

## D. Automated Test Gate (Must Pass)

Run from `jetbrains-plugin`:

```bash
./gradlew test \
  --tests com.souche.mindmap.idea.XmindConverterTest \
  --tests com.souche.mindmap.idea.MindmapBridgeMessageCodecTest \
  --tests com.souche.mindmap.idea.MindmapBridgeImportMessageBuilderTest \
  --tests com.souche.mindmap.idea.MindmapBridgeCommandDispatcherTest \
  --tests com.souche.mindmap.idea.MindmapBridgeRoundtripTest
```

Pass criteria: all tests green, no compilation errors.

## E. Known Non-Blocking Warnings

These are currently treated as non-blocking in sandbox unless behavior regresses:

- CDS warning (`java.system.class.loader` with `PathClassLoader`)
- `CFBundleURLTypes` missing
- `com.intellij.modules.ultimate` dependency warnings from unrelated bundled plugins
- `nativecerts` empty custom roots warning
- `JCEF-sandbox was disabled` in non-launcher run mode

## F. Release Decision

Ready for internal release only if:

1. Section B/C/D all pass.
2. No new blocking regressions in save/export/import flows.
3. `PORTING_TASKS.md` reflects final state for MMP-008 and MMP-010.
