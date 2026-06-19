import { useEffect, useState } from 'react';
import { MindCanvas } from './canvas/MindCanvas';
import type { IMindEngine } from './engine/types';
import { getHost, onHostMessage } from './host/hostBridge';
import { type EngineStore } from './store/engineStore';
import { Toolbar } from './ui/Toolbar';

export function App() {
  const [engine, setEngine] = useState<IMindEngine | null>(null);
  const [store, setStore] = useState<EngineStore | null>(null);
  const [savedLabel, setSavedLabel] = useState('');
  const [error, setError] = useState<string | null>(null);
  const host = getHost();

  useEffect(() => {
    if (!engine) return;
    return onHostMessage((m) => {
      if (m.command === 'import') {
        try {
          engine.importJson(JSON.parse(m.importData));
        } catch (err) {
          // eslint-disable-next-line no-console
          console.error('[mindmap] import failed', err);
        }
      }
    });
  }, [engine]);

  const onReady = (e: IMindEngine, s: EngineStore) => {
    setEngine(e);
    setStore(s);
    // Kick the protocol handshake once listeners are wired.
    setTimeout(() => host.post({ command: 'loaded' }), 0);
  };

  const doSave = () => {
    const json = engine?.exportJson();
    const payload = JSON.stringify(json ?? {});
    host.post({ command: 'save', exportData: payload });
    setSavedLabel(`${(payload.length / 1024).toFixed(0)}KB`);
  };

  return (
    <div className="absolute inset-0 flex flex-col bg-white font-sans text-slate-800">
      {engine && store ? (
        <Toolbar engine={engine} store={store} onSave={doSave} savedLabel={savedLabel} />
      ) : (
        <div className="flex h-11 shrink-0 items-center border-b border-slate-200 bg-slate-50 px-3 text-sm text-slate-500">
          {error ? `引擎初始化失败：${error}` : '加载中…'}
        </div>
      )}
      <main className="relative flex-1">
        <MindCanvas onReady={onReady} onError={(e) => setError(String(e))} />
      </main>
    </div>
  );
}
