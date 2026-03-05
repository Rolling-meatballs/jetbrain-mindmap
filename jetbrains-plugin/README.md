# mindmap-jetbrains (skeleton)

This folder contains a JetBrains plugin skeleton for porting `vscode-mindmap`.

## Current scope

- Action: `Open Mindmap` (Editor menu + Tools menu)
- Shortcut: `Ctrl+M` / `Cmd+M`
- Supports target files: `.km` and `.xmind`
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

## What is intentionally incomplete

- Real KityMinder web UI integration (currently a JSON editor page)
- Full fidelity xmind edge-case coverage (markers, notes, style, relation, etc.)
- Front-end side PNG payload generation is still placeholder in current demo page

## Run

From this folder:

```bash
gradle runIde
```

If you do not have local Gradle installed yet, import this folder as a Gradle project in IntelliJ IDEA and run the `runIde` task.

## Suggested next steps

1. Move `webui/` assets into plugin resources and replace VS Code APIs with the current shim (`acquireVsCodeApi` + `window.vscode`).
2. Expand xmind conversion coverage to include metadata/attachments and more package variants.
3. Replace the placeholder editor page with actual KityMinder view and wire true image export payload.
4. Add a file editor provider to open mindmap directly instead of action-triggered Tool Window.
