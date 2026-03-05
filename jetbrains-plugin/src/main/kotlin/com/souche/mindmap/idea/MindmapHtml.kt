package com.souche.mindmap.idea

object MindmapHtml {
    fun page(jsQueryCall: String): String {
        return """
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Mindmap Skeleton</title>
  <style>
    :root {
      --bg: #f5f8ff;
      --card: #ffffff;
      --ink: #0f172a;
      --line: #d6def5;
      --accent: #2a63ff;
      --accent-2: #1248d5;
    }
    html, body { margin: 0; height: 100%; font-family: "SF Mono", Menlo, Consolas, monospace; color: var(--ink); background: radial-gradient(circle at top right, #d8e4ff, var(--bg)); }
    .layout { display: grid; grid-template-rows: auto 1fr; height: 100%; gap: 10px; padding: 12px; box-sizing: border-box; }
    .toolbar { display: flex; align-items: center; gap: 8px; background: var(--card); border: 1px solid var(--line); border-radius: 10px; padding: 8px; }
    .btn { border: 1px solid var(--line); background: #fff; color: var(--ink); border-radius: 8px; padding: 6px 12px; cursor: pointer; }
    .btn.primary { background: var(--accent); color: #fff; border-color: var(--accent); }
    .btn.primary:hover { background: var(--accent-2); }
    .status { margin-left: auto; opacity: 0.72; font-size: 12px; }
    textarea { width: 100%; height: 100%; box-sizing: border-box; resize: none; border: 1px solid var(--line); border-radius: 10px; background: var(--card); padding: 12px; color: var(--ink); outline: none; }
  </style>
</head>
<body>
  <div class="layout">
    <div class="toolbar">
      <button class="btn primary" id="save">Save (Ctrl/Cmd+S)</button>
      <button class="btn" id="export">Export PNG (todo)</button>
      <span class="status" id="status">Idle</span>
    </div>
    <textarea id="editor" spellcheck="false">{}</textarea>
  </div>

  <script>
    const statusEl = document.getElementById('status');
    const editorEl = document.getElementById('editor');
    const bridgeState = {};

    function setStatus(text) {
      statusEl.textContent = text;
    }

    function sendRawPayload(payload) {
      $jsQueryCall;
    }

    const vscodeApi = {
      postMessage(message) {
        const payload = typeof message === 'string' ? message : JSON.stringify(message || {});
        sendRawPayload(payload);
      },
      setState(partial) {
        Object.assign(bridgeState, partial || {});
        return bridgeState;
      },
      getState() {
        return bridgeState;
      }
    };

    window.acquireVsCodeApi = function() {
      return vscodeApi;
    };
    window.vscode = window.acquireVsCodeApi();

    window.mindmapHost = {
      onMessage(payload) {
        try {
          const message = typeof payload === 'string' ? JSON.parse(payload) : payload;
          if (message.command === 'import') {
            editorEl.value = message.importData || '{}';
            setStatus('Loaded ' + (message.extName || ''));
          }
        } catch (e) {
          setStatus('Parse message failed');
        }
      }
    };

    function post(command, exportData) {
      window.vscode.postMessage({ command, exportData: exportData || '' });
    }

    document.getElementById('save').addEventListener('click', () => {
      post('save', editorEl.value);
      setStatus('Saved');
    });

    document.getElementById('export').addEventListener('click', () => {
      post('exportToImage');
      setStatus('Export requested');
    });

    window.addEventListener('keydown', (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
        e.preventDefault();
        post('save', editorEl.value);
        setStatus('Saved');
      }
    });

    post('loaded');
  </script>
</body>
</html>
        """.trimIndent()
    }
}
