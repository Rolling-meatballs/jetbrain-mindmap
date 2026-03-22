# mindmap-jetbrains (skeleton)

This folder contains a JetBrains plugin skeleton for porting `vscode-mindmap`.

Migration tracking file: `jetbrains-plugin/PORTING_TASKS.md`
Release validation matrix: `jetbrains-plugin/RELEASE_VALIDATION_MATRIX.md`
Release checklist: `jetbrains-plugin/RELEASE_CHECKLIST.md`
Internal release notes draft: `jetbrains-plugin/INTERNAL_RELEASE_NOTES_0.1.4.md`

## Current scope

- Action: `Open Mindmap` (Editor menu + Tools menu)
- Shortcut: `Ctrl+M` / `Cmd+M`
- Supports target files: `.km` and `.xmind`
- Double-click `.km` / `.xmind` opens Mindmap custom editor tab (FileEditorProvider)
- Tool Window (`Mindmap`) rendered with JCEF
- Auto-follow selected editor file (`.km` / `.xmind`)
- JS <-> Kotlin bridge protocol compatible with VS Code messages:
  - `loaded`
  - `import`
  - `save`
  - `exportToImage`
- Save behavior:
  - `.km` saves to itself
  - `.xmind` saves to sibling `.km` file (same as current VS Code plugin behavior)
- Basic `.xmind -> .km` conversion:
  - supports `content.json` (newer xmind packages)
  - supports `content.xml` (classic xmind packages)
- PNG export:
  - accepts base64 image payload and writes sibling `.png`
- UI loading strategy:
  - prefers bundled webui assets packaged inside plugin
  - falls back to project `webui/mindmap.html` when bundled assets are unavailable/incomplete
  - falls back to built-in skeleton UI when assets are missing

## What is intentionally incomplete

- Packaged plugin does not bundle full KityMinder assets yet (uses project-local `webui/` in dev)
- Full fidelity xmind edge-case coverage (markers, notes, style, relation, etc.)
- Front-end side PNG payload generation still depends on real KityMinder page availability

## Run

From this folder:

```bash
./gradlew runIde
```

If you do not have local Gradle installed yet, import this folder as a Gradle project in IntelliJ IDEA and run the `runIde` task.

> Required Gradle version: **8.13+** (recommended: 8.13).  
> Build system now uses **IntelliJ Platform Gradle Plugin 2.x** and targets **IntelliJ Platform 253-based IDEs**.

## Build packaged webui

The plugin build snapshots runtime webui files from repo `../webui` into plugin resources (`bundled-webui/**`).

For complete packaged UI, prepare webui dependencies before building plugin:

```bash
cd webui
npm run init
npm run build
```

Then build plugin from `jetbrains-plugin`:

```bash
./gradlew buildPlugin
```

At runtime, plugin loads bundled webui first.

## Release validation

Before internal release, run the validation matrix:

```bash
cd jetbrains-plugin
./gradlew clean buildPlugin
./gradlew test
./gradlew runIde
```

Then verify checklist items in `RELEASE_VALIDATION_MATRIX.md`.

For release readiness sign-off, complete `RELEASE_CHECKLIST.md`.

## Publish to Marketplace

Use an environment variable for the Marketplace token; do not hardcode it in the repo.

```bash
cd jetbrains-plugin
export JETBRAINS_MARKETPLACE_TOKEN='your-token-here'
./gradlew clean buildPlugin publishPlugin
```

If you only want to upload an already-built artifact:

```bash
cd jetbrains-plugin
export JETBRAINS_MARKETPLACE_TOKEN='your-token-here'
./gradlew publishPlugin
```

## Enable full webui in development

The plugin will use real KityMinder page only when `webui/mindmap.html` and all referenced assets exist.

From repo root:

```bash
cd webui
npm run init
npm run build
```

If both bundled and project webui are incomplete, plugin shows a warning and automatically uses fallback UI.

## Troubleshooting

- If you do not have Gradle wrapper yet (or wrapper version is too old), generate it with:

```bash
cd jetbrains-plugin
./scripts/bootstrap-wrapper.sh
```

- Then always run with wrapper (`Use Gradle from: gradle-wrapper.properties` in IDE):

```bash
./gradlew runIde
```

## Suggested next steps

1. Move `webui/` assets into plugin resources and replace VS Code APIs with the current shim (`acquireVsCodeApi` + `window.vscode`).
2. Expand xmind conversion coverage to include metadata/attachments and more package variants.
3. Replace the placeholder editor page with actual KityMinder view and wire true image export payload.
4. Add a file editor provider to open mindmap directly instead of action-triggered Tool Window.
