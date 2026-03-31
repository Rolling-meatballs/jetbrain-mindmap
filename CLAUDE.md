# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Dual-platform mind map editor supporting `.km` (KityMinder native) and `.xmind` files. Ships as both a **VSCode extension** and a **JetBrains plugin**, sharing a common KityMinder-based web UI.

## Build & Development Commands

### VSCode Extension (root directory)

```bash
npm install          # Install dependencies
npm run build        # Production build → out/extension.js
npm run dev          # Watch mode (alias: npm run watch)
npm run lint         # ESLint on src/
npm test             # Run tests via out/test/runTest.js
```

### JetBrains Plugin (jetbrains-plugin/)

```bash
cd jetbrains-plugin
./gradlew build              # Full build (compiles + tests + bundles webui)
./gradlew test               # Run unit tests only
./gradlew runIde             # Launch sandbox IDE for manual testing
./gradlew buildPlugin        # Package .zip for distribution
./gradlew publishPlugin      # Publish to JetBrains Marketplace (requires JETBRAINS_MARKETPLACE_TOKEN env var)
```

Run a single test class:
```bash
./gradlew test --tests "com.souche.mindmap.idea.XmindConverterTest"
```

## Architecture

### Shared Layer: `webui/`
KityMinder-based HTML/JS/CSS rendered in both platforms. Entry point is `webui/mindmap.html`. Assets are served via VSCode's `vscode-resource:` scheme or bundled into the JetBrains JAR at `bundled-webui/` during Gradle build (`syncBundledWebUi` task).

### VSCode Extension: `src/`
- **`extension.ts`** — registers command `extension.mindmap` (Cmd/Ctrl+M), creates a `WebviewPanel`, manages opened panel deduplication via `openedPanelMap`
- **Message protocol** (host ↔ webview):
  - `loaded` (webview → host): triggers `import` message back with file data
  - `import` (host → webview): sends `importData` JSON + `extName`
  - `save` (webview → host): writes JSON back to `.km` file (`.xmind` files saved as new `.km`)
  - `exportToImage` (webview → host): decodes base64 PNG and writes to disk
- **`services/xmind.ts`** — wraps NPM `xmind` package, converts XMind workbook to `KMRootNode`
- **`types/index.ts`** — `KMRootNode` / `KMSubNode` interfaces for KityMinder JSON format

### JetBrains Plugin: `jetbrains-plugin/src/main/kotlin/com/souche/mindmap/idea/`
Mirrors the VSCode extension architecture using JCEF instead of a webview API:

| Class | Role |
|---|---|
| `MindmapBridge` | Core orchestrator — wires together JCEF browser, file I/O, and message routing |
| `MindmapBridgeCommandDispatcher` | Routes inbound JS messages by `command` field |
| `MindmapBridgeMessageCodec` | Encodes/decodes messages injected via `executeJavaScript` |
| `MindmapBridgeImportMessageBuilder` | Builds the `import` payload for the webview |
| `XmindConverter` | Full XMind→KM conversion in Kotlin (handles styles, images, notes, relations, hyperlinks) |
| `MindmapFileEditor` | JCEF-backed `FileEditor` implementation |
| `MindmapWebUiLoader` | Loads bundled webui assets from classpath |
| `MindmapProjectState` | IntelliJ persisted service — tracks `currentFile` across sessions |

### Key Constraint
The Gradle `syncBundledWebUi` task copies `webui/` into the build directory before packaging. Any change to `webui/` requires a Gradle rebuild to take effect in the JetBrains plugin.

## File Format
`.km` files are plain JSON conforming to the `KMRootNode` schema: `{ root, template, theme, version }` where `root` is a recursive `KMSubNode` tree (`{ data: { id, text, created }, children[] }`).

`.xmind` files are ZIP archives — both platforms unzip and convert them to KM JSON on open; saving always produces a `.km` file alongside the original.
