import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

// Served in JetBrains JCEF via the `http://mindmap/` scheme handler, so base
// must be '/' (absolute asset URLs resolve to that origin). The PoC confirmed
// the runtime is Chromium 137, so we target modern output; the conservative
// chrome87 target from the spike is no longer needed. Tailwind v4 emits modern
// CSS (cascade layers, @property), hence a recent cssTarget.
export default defineConfig({
  base: '/',
  plugins: [react(), tailwindcss()],
  server: { port: 5188, host: '127.0.0.1' },
  build: {
    target: 'chrome120',
    cssTarget: 'chrome120',
    outDir: 'dist',
    emptyOutDir: true,
  },
});
