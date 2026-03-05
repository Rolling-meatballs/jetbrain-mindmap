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
  if (window.acquireVsCodeApi && window.vscode) {
    return;
  }

  var bridgeState = {};
  function sendRawPayload(payload) {
    $jsQueryCall;
  }

  var api = {
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

  window.acquireVsCodeApi = function() {
    return api;
  };
  window.vscode = window.acquireVsCodeApi();
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
