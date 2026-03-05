'use strict';

const path = require('path');
const { defineConfig } = require('vite');
const { buildAll } = require('./scripts/build-core');

function isRelevantFile(file) {
  const rel = file.split(path.sep).join('/');
  return (
    rel.includes('/src/') ||
    rel.includes('/ui/') ||
    rel.includes('/less/') ||
    rel.endsWith('/main.js') ||
    rel.endsWith('/mindmap.html')
  );
}

function legacyBundlePlugin() {
  let running = false;
  let pending = false;

  const runBuild = async logger => {
    if (running) {
      pending = true;
      return;
    }

    running = true;
    try {
      await buildAll();
    } catch (err) {
      logger.error(err && err.stack ? err.stack : String(err));
      throw err;
    } finally {
      running = false;
      if (pending) {
        pending = false;
        await runBuild(logger);
      }
    }
  };

  return {
    name: 'mindmap-legacy-bundle',
    apply: 'serve',
    async configureServer(server) {
      await runBuild(server.config.logger);

      const rebuild = async file => {
        if (!isRelevantFile(file)) {
          return;
        }
        await runBuild(server.config.logger);
        server.ws.send({ type: 'full-reload' });
      };

      server.watcher.on('add', rebuild);
      server.watcher.on('change', rebuild);
      server.watcher.on('unlink', rebuild);
    },
  };
}

function legacyBuildPlugin() {
  return {
    name: 'mindmap-legacy-build',
    apply: 'build',
    async buildStart() {
      await buildAll();
    },
  };
}

module.exports = defineConfig({
  root: __dirname,
  publicDir: false,
  server: {
    host: '127.0.0.1',
    port: 8910,
  },
  build: {
    outDir: '.vite-dist',
    emptyOutDir: true,
    rollupOptions: {
      input: path.resolve(__dirname, 'index.html'),
    },
  },
  plugins: [legacyBundlePlugin(), legacyBuildPlugin()],
});
