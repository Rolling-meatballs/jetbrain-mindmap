package com.souche.mindmap.idea

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class MindmapWebUiLoader(
    private val project: Project
) {
    data class Result(
        val html: String,
        val source: Source,
        val issues: List<String>
    )

    enum class Source {
        BUNDLED_WEBUI,
        PROJECT_WEBUI,
        FALLBACK
    }

    fun load(jsQueryCall: String): Result {
        val issues = mutableListOf<String>()

        val bundledExtraction = extractBundledWebUi()
        bundledExtraction.baseDir?.let { bundledDir ->
            val bundledResult = tryLoadFromDirectory(
                baseDir = bundledDir,
                jsQueryCall = jsQueryCall,
                source = Source.BUNDLED_WEBUI,
                label = "Bundled webui"
            )
            if (bundledResult.result != null) {
                return bundledResult.result
            }
            issues.addAll(bundledResult.issues)
        }
        issues.addAll(bundledExtraction.issues)

        val projectResult = tryLoadFromProject(jsQueryCall)
        if (projectResult.result != null) {
            return projectResult.result
        }
        issues.addAll(projectResult.issues)

        if (issues.isEmpty()) {
            issues += "No usable webui source found."
        }

        return Result(MindmapHtml.page(jsQueryCall), Source.FALLBACK, issues)
    }

    private fun tryLoadFromProject(jsQueryCall: String): LoadAttempt {
        val basePath = project.basePath ?: return LoadAttempt(
            issues = listOf("Project webui unavailable: project base path is unavailable.")
        )

        val webuiDir = Paths.get(basePath, "webui")
        return tryLoadFromDirectory(
            baseDir = webuiDir,
            jsQueryCall = jsQueryCall,
            source = Source.PROJECT_WEBUI,
            label = "Project webui"
        )
    }

    private fun tryLoadFromDirectory(
        baseDir: Path,
        jsQueryCall: String,
        source: Source,
        label: String
    ): LoadAttempt {
        val mindmapHtml = baseDir.resolve("mindmap.html")
        if (!Files.exists(mindmapHtml)) {
            return LoadAttempt(
                issues = listOf("$label unavailable: cannot find ${mindmapHtml.fileName}.")
            )
        }

        val rawHtml = runCatching {
            Files.readString(mindmapHtml, StandardCharsets.UTF_8)
        }.getOrElse {
            return LoadAttempt(
                issues = listOf("$label unavailable: failed to read mindmap.html: ${it.message ?: "Unknown error"}.")
            )
        }

        val requiredAssets = extractRequiredAssets(rawHtml)
        val missingAssets = requiredAssets.filterNot { Files.exists(baseDir.resolve(it)) }
        if (missingAssets.isNotEmpty()) {
            return LoadAttempt(
                issues = listOf(
                    "$label dependencies are incomplete.",
                    "Missing assets: ${missingAssets.take(8).joinToString(", ")}${if (missingAssets.size > 8) " ..." else ""}"
                )
            )
        }

        val baseUrl = baseDir.toUri().toString().removeSuffix("/")
        val shimScript = buildVsCodeShim(jsQueryCall)
        val transformed = rawHtml
            .replace("\${vscode}", baseUrl)
            .replace(
                Regex("""window\.vscode\s*=\s*acquireVsCodeApi\(\);?"""),
                "window.vscode = window.acquireVsCodeApi ? window.acquireVsCodeApi() : window.vscode;"
            )

        val htmlWithShim = injectBeforeHeadClose(transformed, shimScript)
        return LoadAttempt(result = Result(htmlWithShim, source, emptyList()))
    }

    private fun extractBundledWebUi(): ExtractionAttempt {
        val classLoader = javaClass.classLoader
        val manifestResource = "bundled-webui/manifest.txt"
        val manifestContent = classLoader.getResourceAsStream(manifestResource)?.use { stream ->
            stream.readBytes().toString(StandardCharsets.UTF_8)
        } ?: return ExtractionAttempt(
            issues = listOf("Bundled webui unavailable: manifest not found in plugin resources.")
        )

        val entries = manifestContent
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        if (entries.isEmpty()) {
            return ExtractionAttempt(
                issues = listOf("Bundled webui unavailable: manifest is empty.")
            )
        }

        val outputDir = Paths.get(PathManager.getTempPath(), "mindmap-jetbrains", "bundled-webui")
        runCatching {
            Files.createDirectories(outputDir)
        }.onFailure {
            return ExtractionAttempt(
                issues = listOf("Bundled webui unavailable: cannot create cache directory: ${it.message ?: "Unknown error"}.")
            )
        }

        val issues = mutableListOf<String>()
        for (relativePath in entries) {
            val normalizedPath = outputDir.resolve(relativePath).normalize()
            if (!normalizedPath.startsWith(outputDir)) {
                issues += "Bundled webui skipped invalid path: $relativePath"
                continue
            }

            val resourcePath = "bundled-webui/$relativePath"
            val stream = classLoader.getResourceAsStream(resourcePath)
            if (stream == null) {
                issues += "Bundled webui missing resource: $relativePath"
                continue
            }

            stream.use { input ->
                Files.createDirectories(normalizedPath.parent)
                Files.copy(input, normalizedPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }

        return ExtractionAttempt(outputDir, issues)
    }

    private fun extractRequiredAssets(html: String): List<String> {
        val regex = Regex("""(?:src|href)=['"]\$\{vscode}/([^'"]+)['"]""")
        return regex.findAll(html)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
    }

    private fun injectBeforeHeadClose(html: String, script: String): String {
        val headClose = "</head>"
        val index = html.indexOf(headClose)
        return if (index >= 0) {
            html.substring(0, index) + script + "\n" + html.substring(index)
        } else {
            script + "\n" + html
        }
    }

    private fun buildVsCodeShim(jsQueryCall: String): String {
        return """
<script>
(function() {
  var bridgeState = {};
  var api = null;

  function sendRawPayload(payload) {
    $jsQueryCall;
  }

  if (window.acquireVsCodeApi) {
    try {
      api = window.acquireVsCodeApi();
    } catch (e) {
      api = null;
    }
  }

  if (!api && window.vscode) {
    api = window.vscode;
  }

  if (!api) {
    api = {
      postMessage: function(message) {
        var payload = typeof message === 'string' ? message : JSON.stringify(message || {});
        sendRawPayload(payload);
      },
      setState: function(partial) {
        Object.assign(bridgeState, partial || {});
        return bridgeState;
      },
      getState: function() {
        return bridgeState;
      }
    };
  }

  if (typeof api.postMessage !== 'function') {
    api.postMessage = function(message) {
      var payload = typeof message === 'string' ? message : JSON.stringify(message || {});
      sendRawPayload(payload);
    };
  }

  if (typeof api.setState !== 'function') {
    api.setState = function(partial) {
      Object.assign(bridgeState, partial || {});
      return bridgeState;
    };
  }

  if (typeof api.getState !== 'function') {
    api.getState = function() {
      return bridgeState;
    };
  }

  window.acquireVsCodeApi = function() {
    return api;
  };
  window.vscode = window.acquireVsCodeApi();

  function postClientError(message) {
    try {
      api.postMessage({
        command: 'clientError',
        message: message
      });
    } catch (e) {
      sendRawPayload(JSON.stringify({
        command: 'clientError',
        message: message
      }));
    }
  }

  function postHostMessage(message) {
    var payload = typeof message === 'string' ? message : JSON.stringify(message || {});
    try {
      if (api && typeof api.postMessage === 'function') {
        api.postMessage(message);
        return;
      }
    } catch (e) {
      // fallback below
    }

    try {
      sendRawPayload(payload);
    } catch (e) {
      throw new Error('host postMessage transport failed');
    }
  }

  // Expose a stable bridge helper so page scripts can bypass fragile window.vscode wiring.
  window.__mindmapHostPostMessage = postHostMessage;

  function wireButton(selector, marker, handler) {
    var button = document.querySelector(selector);
    if (!button || button[marker]) {
      return !!button;
    }

    button[marker] = true;
    button.addEventListener('click', function(event) {
      event.preventDefault();
      event.stopImmediatePropagation();
      handler();
    }, true);

    return true;
  }

  function wireSaveButton() {
    return wireButton('.km-export-save', '__mindmapSaveWired', function() {
      postHostMessage({ command: 'debugProbe', message: 'save_clicked' });
      if (!window.minder || typeof window.minder.exportJson !== 'function') {
        postClientError('Save failed: minder is unavailable.');
        return;
      }

      postHostMessage({
        command: 'save',
        exportData: JSON.stringify(window.minder.exportJson(), null, 4)
      });
    });
  }

  function wireExportButton() {
    return wireButton('.km-export-image', '__mindmapExportWired', function() {
      postHostMessage({ command: 'debugProbe', message: 'export_clicked' });
      if (!window.minder || typeof window.minder.exportData !== 'function') {
        postClientError('PNG export failed: minder is unavailable.');
        return;
      }

      function sanitizeImageNodes() {
        if (!window.minder.getRoot || typeof window.minder.getRoot !== 'function') {
          return;
        }
        var root = window.minder.getRoot();
        if (!root || typeof root.traverse !== 'function') {
          return;
        }

        root.traverse(function(node) {
          var data = node && node.data;
          if (!data || !data.image) {
            return;
          }
          var size = data.imageSize;
          var valid = size && Number(size.width) > 0 && Number(size.height) > 0;
          if (!valid) {
            if (typeof node.setData === 'function') {
              node.setData('image', null);
              node.setData('imageTitle', null);
              node.setData('imageSize', null);
            } else {
              data.image = null;
              data.imageTitle = null;
              data.imageSize = null;
            }
          }
        });
      }

      function exportPng() {
        return Promise.resolve().then(function() {
          return window.minder.exportData('png');
        });
      }

      var timeoutPromise = new Promise(function(_, reject) {
        window.setTimeout(function() {
          reject(new Error('export timeout'));
        }, 8000);
      });

      Promise.race([
        exportPng().catch(function(err) {
          var text = (err && err.message) || String(err);
          if (text.indexOf("reading 'width'") >= 0) {
            sanitizeImageNodes();
            return exportPng();
          }
          throw err;
        }),
        timeoutPromise
      ]).then(function(result) {
        if (!result) {
          throw new Error('empty PNG payload');
        }

        postHostMessage({ command: 'debugProbe', message: 'export_payload_chars=' + String(result).length });
        postHostMessage({
          command: 'exportToImage',
          exportData: result
        });
      }).catch(function(err) {
        postClientError('PNG export failed: ' + ((err && err.message) || String(err)));
      });
    });
  }

  postHostMessage({ command: 'debugProbe', message: 'shim_ready' });
  postHostMessage({ command: 'loaded' });

  var retries = 0;
  var timer = window.setInterval(function() {
    retries += 1;
    var saveReady = wireSaveButton();
    var exportReady = wireExportButton();
    if ((saveReady && exportReady) || retries > 120) {
      window.clearInterval(timer);
    }
  }, 250);
})();
</script>
        """.trimIndent()
    }

    private data class LoadAttempt(
        val result: Result? = null,
        val issues: List<String> = emptyList()
    )

    private data class ExtractionAttempt(
        val baseDir: Path? = null,
        val issues: List<String> = emptyList()
    )
}
