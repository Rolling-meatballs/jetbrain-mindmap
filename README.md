<p>
  <h1 align="center">vscode-mindmap</h1>
</p>

![mindmap](https://img.souche.com/f2e/f12837b4057a8f1c5dd5033560a48f20.gif)

## Features

-  File Edit(eg: .km), Save, Export
-  Transpile .xmind to .km
-  Support export to image(.png)

## Installation

  Install through VS Code extensions. Search for "vscode-mindmap"

  Visual Studio Code Market Place: vscode-mindmap

## Usage

  open any file with extension of km or xmind after install the plugin

## Keyboard Shortcuts

| Key                              | Command                        |
| -------------------------------- | ------------------------------ |
| cmd + m(mac) / ctrl + m(windows) | open a webview of textDocument |
| cmd + s(mac) / ctrl + s(windows) | save mindmap file              |

## FAQ
  - **File parsed incorrectly, the current webview only show initial mindmap**
  
    close the file and webview, try to reopen this file

  - **Extension invalid when the plugin installed**

    check your vscode version, please ensure the version is 1.29.0 or above

## Feedback

![Feedback](https://img.souche.com/f2e/5e127e01cf164f9f9cff4892653d7d02.jpeg)

## Development

### Requirements

- Node.js 16+
- TypeScript 5.3+
- VSCode 1.75.0+

### Build

```bash
npm install
npm run build
```

### Development Mode

```bash
npm run dev
```

### Lint

```bash
npm run lint
```

## JetBrains Port

JetBrains plugin skeleton is available in `jetbrains-plugin/`.

- current scope: open `.km` / `.xmind`, JCEF bridge, save protocol
- pending: full KityMinder UI migration, `.xmind -> .km`, PNG export
- details: see `jetbrains-plugin/README.md`

## 

[大搜车无线开发中心](https://blog.souche.com/tag/frontend/) Present
