import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
  build: {
    lib: {
      entry: resolve(__dirname, 'src/extension.ts'),
      formats: ['cjs'],
      fileName: () => 'extension.js',
    },
    outDir: 'out',
    rollupOptions: {
      external: ['vscode', 'path', 'fs'],
      output: {
        entryFileNames: 'extension.js',
      },
    },
    sourcemap: true,
    minify: false,
    target: 'node16',
  },
  resolve: {
    extensions: ['.ts', '.js'],
  },
});
