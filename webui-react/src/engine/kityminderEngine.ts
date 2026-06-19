import type { KMinderInstance } from '../vite-env';
import type { EngineEvent, IMindEngine, KMJson } from './types';

// Adapter over the self-owned BSD kityminder-core. The globals window.kity /
// window.kityminder come from classic <script> tags (see index.html); this file
// is the ONLY place that reads them. Everything else uses IMindEngine.

// Cursor-zoom bounds (percent). Wider than the toolbar's level stack so smooth
// wheel zoom feels continuous.
const MIN_ZOOM = 10;
const MAX_ZOOM = 400;

export function isCoreLoaded(): boolean {
  return !!window.kity && !!window.kityminder?.Minder;
}

class KityMinderEngine implements IMindEngine {
  private minder: KMinderInstance;
  // Track wrapped listeners so destroy() can detach them all.
  private listeners = new Set<{ event: string; cb: (e: unknown) => void }>();

  constructor() {
    const km = window.kityminder;
    if (!km?.Minder) {
      throw new Error('window.kityminder.Minder missing — check kity load order');
    }
    this.minder = new km.Minder();
  }

  mount(el: HTMLElement): void {
    this.minder.renderTo(el);
    // Fit content into view on first render.
    this.minder.execCommand('camera');
  }

  destroy(): void {
    this.listeners.forEach(({ event, cb }) => {
      try {
        this.minder.off(event, cb);
      } catch {
        /* engine may already be torn down */
      }
    });
    this.listeners.clear();
    try {
      this.minder.destroy?.();
    } catch {
      /* core may not expose destroy */
    }
  }

  importJson(data: KMJson): void {
    this.minder.importJson(data);
    this.minder.execCommand('camera');
  }

  exportJson(): KMJson {
    return this.minder.exportJson() as KMJson;
  }

  importData(protocol: string, data: string): Promise<void> {
    return Promise.resolve(this.minder.importData(protocol, data)).then(() => {
      this.minder.execCommand('camera');
    });
  }

  exportData(protocol: string, option?: unknown): Promise<string> {
    return Promise.resolve(this.minder.exportData(protocol, option));
  }

  zoomAroundClientPoint(percent: number, clientX: number, clientY: number): void {
    const p1 = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, percent));
    const paper = this.minder.getPaper();
    const svg = paper.shapeNode?.ownerSVGElement;
    if (!svg) return;
    const ctm = svg.getScreenCTM();
    if (!ctm) return;
    const inv = ctm.inverse();
    const toUser = (cx: number, cy: number) => {
      const pt = svg.createSVGPoint();
      pt.x = cx;
      pt.y = cy;
      return pt.matrixTransform(inv);
    };
    const cursor = toUser(clientX, clientY);
    const rect = svg.getBoundingClientRect();
    const boxCenter = toUser(rect.left + rect.width / 2, rect.top + rect.height / 2);

    const vp = paper.getViewPort();
    const z0 = vp.zoom || 1;
    const z1 = p1 / 100;
    if (z0 === z1) return;

    // Keep the world point under the cursor fixed:
    //   screen(P) = boxCenter + zoom * (P - center)
    //   => center1 = center0 + (cursor - boxCenter) * (1/z0 - 1/z1)
    const k = 1 / z0 - 1 / z1;
    paper.setViewPort(
      vp.center.x + (cursor.x - boxCenter.x) * k,
      vp.center.y + (cursor.y - boxCenter.y) * k,
      z1,
    );

    // Keep engine zoom value + reactive events in sync (toolbar reads 'zoom').
    (this.minder as unknown as { _zoomValue: number })._zoomValue = p1;
    this.minder.fire('zoom', { zoom: p1 });
    this.minder.fire('viewchange');
  }

  execCommand(cmd: string, ...args: unknown[]): unknown {
    return this.minder.execCommand(cmd, ...args);
  }

  queryCommandState(cmd: string): number {
    return this.minder.queryCommandState(cmd);
  }

  queryCommandValue(cmd: string): unknown {
    return this.minder.queryCommandValue(cmd);
  }

  on(event: EngineEvent, cb: (e: unknown) => void): () => void {
    const entry = { event, cb };
    this.minder.on(event, cb);
    this.listeners.add(entry);
    return () => {
      this.minder.off(event, cb);
      this.listeners.delete(entry);
    };
  }

  getRoot(): unknown {
    return this.minder.getRoot();
  }

  getSelectedNodes(): unknown[] {
    return this.minder.getSelectedNodes();
  }

  isReady(): boolean {
    return isCoreLoaded();
  }

  raw(): unknown {
    return this.minder;
  }
}

export function createKityMinderEngine(): IMindEngine {
  return new KityMinderEngine();
}
