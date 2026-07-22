# Mindmap JetBrains Internal Release Notes 0.1.8

Date: 2026-07-22
Plugin: `mindmap-jetbrains-0.1.8.zip`

## Highlights

- Uplifts JetBrains Marketplace compatibility for 2026.2 IDEs.
- Keeps the build target on IntelliJ Platform `2026.1` / build `261` so the plugin remains compatible with the previous 2026.1 line.
- Removes the upper `untilBuild` cap that previously limited installs to `261.*` and blocked 2026.2 IDE updates.
- Adds explicit Plugin Verifier targets for IDEA 2026.2 and PyCharm 2026.2.
- Declares `<depends>com.intellij.modules.jcef</depends>` in plugin.xml. 2026.2 (build 262) ships JCEF as a separate module; without the dependency the editor fails at runtime with `NoClassDefFoundError: com/intellij/ui/jcef/JBCefApp` (reproduced on installed IDEA 2026.2, idea.log 16:01; same failure class as cline/cline#11830). Plugin Verifier reported Compatible and did NOT catch this. The module alias is also declared by the 2026.1 platform (checked in the cached `idea-2026.1-aarch64` distribution), so `sinceBuild=261` is preserved.

- Restores the classic `marked` 0.3.6 build (`bower_components/marked/lib/marked.js`). `webui/bower.json` pinned marked to `git://github.com/chjj/marked.git#master`, an unpinned moving target: a fresh `bower install` today fetches modern marked (no `lib/marked.js`), so `mindmap.html`'s required-asset check failed and the editor fell back ("Bundled webui dependencies are incomplete"). The morning artifact had this defect too. bower.json is now pinned to `https://github.com/markedjs/marked.git#v0.3.6`, and a minimal classic-layout marked (lib/marked.js + LICENSE + bower.json) is vendored under `webui/bower_components/`. All 27 assets referenced by mindmap.html verified present and listed in manifest.txt.
- `syncBundledWebUi` now skips hidden files. Bower git checkouts include `.gitattributes`/`.gitignore`/`.gitmodules`; the jar's default excludes drop them while manifest.txt listed them, producing false "Bundled webui missing resource" warnings at runtime.

## Validation Snapshot

- Artifact path: `build/distributions/mindmap-jetbrains-0.1.8.zip` (copy at `jetbrains-plugin/mindmap-jetbrains-0.1.8-rc.zip`)
- Artifact size: `5509393` bytes
- SHA256: `e04fae02562f4e65105fd5e5504ae314f240cff17ba2fef2fbe7128342c99ad3`
- Superseded artifacts: morning build `5806552` / `37b5fd…` (no jcef depends, modern marked); 16:28 rebuild `5807347` / `4adf63…` (jcef depends but modern marked, editor falls back to fallback UI).
- Packaged jar includes `bundled-webui/mindmap.html`, `bundled-webui/dist/kityminder.editor.min.js`, `bundled-webui/dist/main.min.js`, `node_modules/kity/dist/kity.js`, and `node_modules/kityminder-core/dist/kityminder.core.js`.
- Legacy `webui` assets were prepared before packaging with `npm run init`, temporary `npm install --no-save --package-lock=false source-map`, and `npm run build`; lockfile side effects were removed from the release diff.
- `jetbrains-plugin`: `./gradlew verifyPluginProjectConfiguration` passed.
- `jetbrains-plugin`: `./gradlew test` passed.
- `jetbrains-plugin`: `./gradlew buildPlugin` passed.
- `jetbrains-plugin`: `./gradlew verifyPlugin` passed with:
  - `com.souche.mindmap.idea:0.1.8` against `PY-262.8665.309`: Compatible
  - `com.souche.mindmap.idea:0.1.8` against `IU-262.8665.258`: Compatible

## Release Gate

- Not published yet.
- No commit or push has been performed for this release candidate.
- Release candidate was built from a clean release worktree at commit `b2abc7f`, excluding the uncommitted `webui-react` ImageEditor learning work in the main worktree.
- 2026-07-22 16:28 rebuild (with jcef depends) was done from the main worktree; legacy `webui/dist`, `bower_components`, and engine `node_modules` were copied in from the release worktree, and jar contents were re-verified.
- Manual validation on an installed 2026.2 IDE is still required before GO: install the rc zip, open a `.km`, confirm the legacy editor renders and saves.
- 261-line load check: `runIde` sandbox (IDEA 2026.1) with `-PreactEditor=false` should show the legacy editor with no missing-dependency plugin error.
