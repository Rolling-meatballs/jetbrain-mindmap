import { useEffect, useRef } from 'react';
import { createKityMinderEngine } from '../engine/kityminderEngine';
import type { IMindEngine } from '../engine/types';
import { createEngineStore, type EngineStore } from '../store/engineStore';

interface MindCanvasProps {
  /** Called once the engine has rendered and its reactive store is wired. */
  onReady: (engine: IMindEngine, store: EngineStore) => void;
  onError?: (err: unknown) => void;
}

export function MindCanvas({ onReady, onError }: MindCanvasProps) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    let engine: IMindEngine | undefined;
    let store: EngineStore | undefined;
    try {
      engine = createKityMinderEngine();
      engine.mount(el);
      store = createEngineStore(engine);
      onReady(engine, store);
    } catch (err) {
      // eslint-disable-next-line no-console
      console.error('[mindmap] engine init failed', err);
      onError?.(err);
    }

    // Cursor-centered Cmd/Ctrl + wheel zoom. Capture phase + stopPropagation so
    // the core's own level-stepped wheel handler doesn't also fire; non-passive
    // so preventDefault actually suppresses the host's page-zoom (the bug the old
    // GPL jumping.js never fixed). Plain wheel passes through untouched.
    const onWheel = (e: WheelEvent) => {
      if (!engine || (!e.ctrlKey && !e.metaKey)) return;
      e.preventDefault();
      e.stopPropagation();
      const current = (engine.queryCommandValue('zoom') as number) || 100;
      const next = current * Math.exp(-e.deltaY * 0.0015);
      engine.zoomAroundClientPoint(next, e.clientX, e.clientY);
    };
    el.addEventListener('wheel', onWheel, { passive: false, capture: true });

    return () => {
      el.removeEventListener('wheel', onWheel, true);
      store?.dispose();
      engine?.destroy();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return <div ref={ref} className="absolute inset-0 km-canvas" />;
}
