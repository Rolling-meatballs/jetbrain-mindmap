// Host <-> webview protocol, mirroring the existing extension (loaded/import/
// save/exportToImage). In a real host window.vscode is injected by the JCEF
// scheme handler shim (JetBrains) or acquireVsCodeApi() (VS Code); in the
// browser dev/standalone case we fall back to window.postMessage.
export type HostMessage =
  | { command: 'loaded' }
  | { command: 'import'; importData: string; extName?: string }
  | { command: 'save'; exportData: string }
  | { command: 'exportToImage'; exportData: string };

export interface Host {
  post(msg: HostMessage): void;
}

export function getHost(): Host {
  const vscode = window.vscode;
  if (vscode?.postMessage) {
    return { post: (m) => vscode.postMessage(m) };
  }
  return { post: (m) => window.postMessage(m, '*') };
}

export function onHostMessage(handler: (m: HostMessage) => void): () => void {
  const fn = (e: MessageEvent) => {
    if (e.data && typeof e.data === 'object' && 'command' in e.data) {
      handler(e.data as HostMessage);
    }
  };
  window.addEventListener('message', fn);
  return () => window.removeEventListener('message', fn);
}
