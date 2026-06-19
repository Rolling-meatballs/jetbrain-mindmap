// IMindEngine — the single seam between the React editor and the rendering
// engine. React talks ONLY to this interface, never to window.kityminder. This
// is what lets us swap the engine later (e.g. to simple-mind-map) without
// touching the UI: a new adapter implementing IMindEngine is all it takes.

/** A kityminder `.km` JSON document (root node tree + template/theme metadata). */
export interface KMJson {
  root: unknown;
  template?: string;
  theme?: string;
  version?: string;
  [k: string]: unknown;
}

/** Engine events the UI subscribes to. Names mirror kityminder-core's events. */
export type EngineEvent =
  | 'interactchange' // a command's enabled/active state may have changed -> re-query toolbar
  | 'contentchange' // the document content changed -> mark dirty / autosave
  | 'selectionchange' // selected node set changed
  | 'modechange' // edit/readonly mode changed
  | 'zoom' // viewport zoom changed
  | 'viewchange'; // pan/scroll changed

/**
 * Command query state from kityminder-core:
 *   -1 disabled, 0 enabled/inactive, 1 active/checked.
 * Exposed as an enum-like type so toolbar code reads clearly.
 */
export const CommandState = {
  Disabled: -1,
  Normal: 0,
  Active: 1,
} as const;

export interface IMindEngine {
  /** Render into the given element. Call once after the element is in the DOM. */
  mount(el: HTMLElement): void;
  /** Tear down listeners / rendering. Safe to call multiple times. */
  destroy(): void;

  /** Load a full `.km` JSON document, replacing current content. */
  importJson(data: KMJson): void;
  /** Serialize current content to `.km` JSON. */
  exportJson(): KMJson;

  /**
   * Import via a named protocol (e.g. 'text', 'markdown', 'json'); resolves
   * once parsed and rendered. Used by node import dialogs.
   */
  importData(protocol: string, data: string): Promise<void>;
  /**
   * Export via a named protocol. 'png'/'svg' resolve to a data URL / markup
   * string; 'markdown'/'text' to plain text. Used by export + image features.
   */
  exportData(protocol: string, option?: unknown): Promise<string>;

  /**
   * Smooth zoom to `percent`, keeping the world point under the given client
   * coordinate fixed (cursor-centered zoom). Drives the kity viewport directly;
   * keeps the engine's zoom value + events in sync so the toolbar updates.
   */
  zoomAroundClientPoint(percent: number, clientX: number, clientY: number): void;

  /** Execute an engine command (the 22 core command modules). */
  execCommand(cmd: string, ...args: unknown[]): unknown;
  /** Query whether a command is disabled/enabled/active (see CommandState). */
  queryCommandState(cmd: string): number;
  /** Query a command's current value (e.g. font size, theme name). */
  queryCommandValue(cmd: string): unknown;

  /** Subscribe to an engine event; returns an unsubscribe function. */
  on(event: EngineEvent, cb: (e: unknown) => void): () => void;

  /** Root node of the current document (opaque; access via engine helpers). */
  getRoot(): unknown;
  /** Currently selected nodes. */
  getSelectedNodes(): unknown[];

  /** Whether window.kity + window.kityminder globals are present. */
  isReady(): boolean;

  /** Escape hatch to the raw Minder instance. Use sparingly; prefer the API. */
  raw(): unknown;
}
