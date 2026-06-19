import { useCallback, useSyncExternalStore } from 'react';
import type { EngineEvent, IMindEngine } from '../engine/types';

// The engine is the single source of truth. Instead of mirroring its state into
// a separate store, we subscribe to its change events and bump a version
// counter; React components re-query the engine (queryCommandState / selection
// / value) during render. This is the §5.2 decision: useSyncExternalStore over
// engine events, no Zustand until UI-only state actually needs it.

// Events that should make the UI re-query command/selection state.
const REACTIVE_EVENTS: EngineEvent[] = [
  'interactchange',
  'contentchange',
  'selectionchange',
  'zoom',
  'viewchange',
];

export class EngineStore {
  private version = 0;
  private listeners = new Set<() => void>();
  private offs: Array<() => void> = [];

  constructor(engine: IMindEngine) {
    this.offs = REACTIVE_EVENTS.map((ev) => engine.on(ev, () => this.bump()));
  }

  private bump = () => {
    this.version++;
    this.listeners.forEach((l) => l());
  };

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  getSnapshot = (): number => this.version;

  dispose(): void {
    this.offs.forEach((off) => off());
    this.offs = [];
    this.listeners.clear();
  }
}

export function createEngineStore(engine: IMindEngine): EngineStore {
  return new EngineStore(engine);
}

/** Re-renders the caller whenever the engine reports a reactive change. */
export function useEngineVersion(store: EngineStore | null): number {
  const subscribe = useCallback(
    (onChange: () => void) => (store ? store.subscribe(onChange) : () => {}),
    [store],
  );
  const getSnapshot = useCallback(() => store?.getSnapshot() ?? 0, [store]);
  return useSyncExternalStore(subscribe, getSnapshot);
}

/** Reactive command state (-1 disabled / 0 normal / 1 active). */
export function useCommandState(
  store: EngineStore | null,
  engine: IMindEngine | null,
  cmd: string,
): number {
  useEngineVersion(store);
  return engine ? engine.queryCommandState(cmd) : -1;
}

/** Reactive command value (e.g. font size, theme name). */
export function useCommandValue<T = unknown>(
  store: EngineStore | null,
  engine: IMindEngine | null,
  cmd: string,
): T | undefined {
  useEngineVersion(store);
  return engine ? (engine.queryCommandValue(cmd) as T) : undefined;
}
