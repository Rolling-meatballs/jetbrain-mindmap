import type { KMJson } from './engine/types';

// Minimal valid KityMinder .km JSON (KMRootNode schema). Used by the dev mock
// host; the real host imports the opened file instead.
export const sampleKm: KMJson = {
  root: {
    data: { id: 'root', text: 'Mindmap React' },
    children: [
      {
        data: { id: 'a', text: 'BSD 引擎挂进 React' },
        children: [
          { data: { id: 'a1', text: 'kityminder-core' }, children: [] },
          { data: { id: 'a2', text: 'kity (SVG)' }, children: [] },
        ],
      },
      {
        data: { id: 'b', text: 'import / save 协议' },
        children: [
          { data: { id: 'b1', text: 'loaded → import' }, children: [] },
          { data: { id: 'b2', text: 'save 往返' }, children: [] },
        ],
      },
      { data: { id: 'c', text: 'JCEF 渲染' }, children: [] },
    ],
  },
  template: 'default',
  theme: 'fresh-blue',
  version: '1.4.43',
};
