package com.souche.mindmap.idea

import org.cef.CefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCallback
import org.cef.callback.CefSchemeHandlerFactory
import org.cef.handler.CefResourceHandler
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import java.nio.charset.StandardCharsets
import kotlin.math.min

// JCEF loading PoC (修法①): serve the React build over a non-file origin so ES
// modules are allowed (file:// is blocked by Chromium's module CORS rule).
// We register a handler on the built-in `http` scheme for a fixed pseudo-host,
// which avoids the custom-scheme registration-timing problem in a plugin.
const val MINDMAP_REACT_SCHEME = "http"
const val MINDMAP_REACT_HOST = "mindmap"
const val MINDMAP_REACT_ORIGIN = "http://mindmap"
private const val RESOURCE_ROOT = "react-poc"

class MindmapReactSchemeHandlerFactory : CefSchemeHandlerFactory {
    override fun create(
        browser: CefBrowser?,
        frame: CefFrame?,
        schemeName: String?,
        request: CefRequest?
    ): CefResourceHandler = MindmapReactResourceHandler(MindmapReactBridgeRegistry.shimFor(browser))
}

private class MindmapReactResourceHandler(private val shim: String?) : CefResourceHandler {
    private var data: ByteArray = ByteArray(0)
    private var offset: Int = 0
    private var mimeType: String = "application/octet-stream"
    private var httpStatus: Int = 200

    override fun processRequest(request: CefRequest?, callback: CefCallback?): Boolean {
        val path = urlToResourcePath(request?.url ?: "")
        val raw = javaClass.classLoader.getResourceAsStream("$RESOURCE_ROOT/$path")?.use { it.readBytes() }
        if (raw != null) {
            data = if (path == "index.html" && shim != null) {
                injectReactShim(raw.toString(StandardCharsets.UTF_8), shim).toByteArray(StandardCharsets.UTF_8)
            } else {
                raw
            }
            mimeType = mimeFor(path)
            httpStatus = 200
        } else {
            data = "Not found: $path".toByteArray(StandardCharsets.UTF_8)
            mimeType = "text/plain"
            httpStatus = 404
        }
        callback?.Continue()
        return true
    }

    override fun getResponseHeaders(response: CefResponse?, responseLength: IntRef?, redirectUrl: StringRef?) {
        response?.mimeType = mimeType
        response?.status = httpStatus
        responseLength?.set(data.size)
    }

    override fun readResponse(dataOut: ByteArray?, bytesToRead: Int, bytesRead: IntRef?, callback: CefCallback?): Boolean {
        if (dataOut == null || offset >= data.size) {
            bytesRead?.set(0)
            return false
        }
        val toCopy = min(bytesToRead, data.size - offset)
        System.arraycopy(data, offset, dataOut, 0, toCopy)
        offset += toCopy
        bytesRead?.set(toCopy)
        return true
    }

    override fun cancel() {
        // no-op
    }
}

private fun urlToResourcePath(url: String): String {
    var p = url.removePrefix(MINDMAP_REACT_ORIGIN).removePrefix("/")
    p = p.substringBefore('?').substringBefore('#')
    if (p.isEmpty()) p = "index.html"
    return p
}

private fun mimeFor(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
    "html", "htm" -> "text/html"
    "js", "mjs" -> "text/javascript" // critical: JS MIME required for ESM strict-MIME check
    "css" -> "text/css"
    "json", "map" -> "application/json"
    "svg" -> "image/svg+xml"
    "png" -> "image/png"
    "jpg", "jpeg" -> "image/jpeg"
    "gif" -> "image/gif"
    "ico" -> "image/x-icon"
    "woff" -> "font/woff"
    "woff2" -> "font/woff2"
    "ttf" -> "font/ttf"
    else -> "application/octet-stream"
}

// --- Real-editor integration helpers (修法① 主线) --------------------------

@Volatile
private var schemeRegistered = false

/** Idempotently register the http://mindmap scheme handler (app-level, once). */
fun ensureMindmapReactSchemeRegistered() {
    if (schemeRegistered) return
    synchronized(MindmapReactSchemeHandlerFactory::class.java) {
        if (schemeRegistered) return
        CefApp.getInstance().registerSchemeHandlerFactory(
            MINDMAP_REACT_SCHEME,
            MINDMAP_REACT_HOST,
            MindmapReactSchemeHandlerFactory()
        )
        schemeRegistered = true
    }
}

/**
 * Per-editor bridge shims, keyed by the requesting CefBrowser. The scheme
 * handler is app-global, but each editor has its own JBCefJSQuery; the editor
 * registers its shim before navigating, and the handler injects it into the
 * served index.html so the React app's window.vscode talks back to that
 * editor's bridge. loadHTML(html, url) does NOT route sub-resources to the
 * handler (verified), so we must loadURL and serve everything through it.
 */
object MindmapReactBridgeRegistry {
    private val shims = java.util.concurrent.ConcurrentHashMap<CefBrowser, String>()
    fun register(browser: CefBrowser, shim: String) { shims[browser] = shim }
    fun unregister(browser: CefBrowser) { shims.remove(browser) }
    fun shimFor(browser: CefBrowser?): String? = browser?.let { shims[it] }
}

/** Inject the bridge shim as a classic script before </head> (runs before the deferred module). */
private fun injectReactShim(html: String, shim: String): String {
    val marker = "</head>"
    val idx = html.indexOf(marker)
    return if (idx >= 0) html.substring(0, idx) + shim + "\n" + html.substring(idx) else "$shim\n$html"
}

/** Build the slim window.vscode -> JBCefJSQuery bridge shim for one editor. */
fun buildReactShim(jsQueryCall: String): String = """
<script>
(function() {
  function sendRawPayload(payload) { $jsQueryCall; }
  var api = {
    postMessage: function(message) {
      sendRawPayload(typeof message === 'string' ? message : JSON.stringify(message || {}));
    },
    setState: function() {},
    getState: function() { return {}; }
  };
  window.acquireVsCodeApi = function() { return api; };
  window.vscode = api;
})();
</script>
""".trimIndent()
