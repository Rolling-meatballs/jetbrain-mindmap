/// <reference types="vite/client" />

// kity + kityminder-core are loaded as classic global <script> tags (see
// index.html). We only ever read the resulting globals; never ESM-import them.
declare global {
  interface Window {
    kity?: unknown;
    kityminder?: {
      Minder: new () => KMinderInstance;
      [k: string]: unknown;
    };
    // Host bridge shim (injected by JCEF scheme handler / VS Code webview).
    vscode?: { postMessage(message: unknown): void };
    acquireVsCodeApi?: () => { postMessage(message: unknown): void };
  }
}

// Minimal structural type for the kityminder-core Minder we drive through the
// adapter. Kept narrow on purpose — the adapter is the only place that touches
// these methods; everything else goes through IMindEngine.
export interface KMinderInstance {
  renderTo(target: HTMLElement | string): void;
  importJson(json: unknown): KMinderInstance;
  exportJson(): unknown;
  importData(protocol: string, data: unknown, option?: unknown): Promise<unknown>;
  exportData(protocol: string, option?: unknown): Promise<string>;
  execCommand(name: string, ...args: unknown[]): unknown;
  queryCommandState(name: string): number;
  queryCommandValue(name: string): unknown;
  on(name: string, callback: (e: unknown) => void): void;
  off(name: string, callback: (e: unknown) => void): void;
  fire(type: string, params?: unknown): void;
  getRoot(): unknown;
  getSelectedNodes(): unknown[];
  setStatus(status: string, force?: boolean): void;
  getPaper(): KPaper;
  destroy?(): void;
}

// Minimal slice of the kity Paper used for cursor-centered zoom math.
export interface KPaper {
  shapeNode: { ownerSVGElement: SVGSVGElement | null };
  getViewPort(): { zoom: number; center: { x: number; y: number } };
  setViewPort(cx: number, cy: number, zoom: number): void;
}

export {};
