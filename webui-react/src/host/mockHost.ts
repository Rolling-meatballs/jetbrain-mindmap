import { sampleKm } from '../sample';

// Browser-only stand-in for the IDE host. Answers `loaded` with an `import`
// payload and records `save` payloads, so the full round-trip works in `npm
// run dev` without an actual JetBrains / VS Code host. Installed only when no
// window.vscode is present (see main.tsx).
export function installMockHost(): void {
  window.addEventListener('message', (e: MessageEvent) => {
    const m = e.data;
    if (!m || typeof m !== 'object') return;
    if (m.command === 'loaded') {
      window.postMessage(
        { command: 'import', importData: JSON.stringify(sampleKm), extName: '.km' },
        '*',
      );
    }
    if (m.command === 'save') {
      (window as unknown as { __lastSave?: string }).__lastSave = m.exportData;
      // eslint-disable-next-line no-console
      console.log('[mock-host] save received, bytes =', m.exportData.length);
    }
  });
}
