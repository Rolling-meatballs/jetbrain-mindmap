'use strict';

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const DIST_DIR = path.join(ROOT, 'dist');
const LESS_ENTRY = path.join(ROOT, 'less', 'editor.less');
const MAIN_ENTRY = path.join(ROOT, 'main.js');

async function buildAll() {
  const startedAt = Date.now();

  cleanGenerated();
  ensureDir(DIST_DIR);

  const banner = buildBanner();
  const logicBundle = buildLogicBundle();
  const templateBundle = buildTemplateBundle();
  const appBundle = readText(path.join(ROOT, 'ui', 'kityminder.app.js'));
  const uiBundle = collectUiScriptPaths().map(readText).join('\n\n');

  const editorBundle = [
    banner,
    '(function() {',
    logicBundle,
    appBundle,
    templateBundle,
    uiBundle,
    '})();',
    '',
  ].join('\n\n');

  writeText(path.join(DIST_DIR, 'kityminder.editor.js'), editorBundle);
  writeText(
    path.join(DIST_DIR, 'kityminder.editor.min.js'),
    minifyJs(editorBundle)
  );

  await buildStyles();
  writeText(
    path.join(DIST_DIR, 'kityminder.editor.min.css'),
    minifyCss(readText(path.join(DIST_DIR, 'kityminder.editor.css')))
  );

  buildMainScript();
  copyDirectory(path.join(ROOT, 'ui', 'images'), path.join(DIST_DIR, 'images'));

  const elapsed = Date.now() - startedAt;
  console.log('[vite-legacy] Build complete in ' + elapsed + 'ms');
}

function cleanGenerated() {
  ensureDir(DIST_DIR);

  const generated = [
    'kityminder.editor.js',
    'kityminder.editor.min.js',
    'kityminder.editor.css',
    'kityminder.editor.min.css',
    'kityminder.editor.css.map',
    'main.min.js',
  ];

  for (const file of generated) {
    removeIfExists(path.join(DIST_DIR, file));
  }

  removeIfExists(path.join(DIST_DIR, 'images'));
  removeIfExists(path.join(ROOT, '.tmp'));
}

function buildBanner() {
  const pkg = JSON.parse(readText(path.join(ROOT, 'package.json')));
  const today = new Date();
  const yyyy = today.getFullYear();
  const mm = String(today.getMonth() + 1).padStart(2, '0');
  const dd = String(today.getDate()).padStart(2, '0');
  const homepage = pkg.homepage ? ' * ' + pkg.homepage + '\n' : '';

  return [
    '/*!',
    ' * ====================================================',
    ' * ' + (pkg.title || pkg.name) + ' - v' + pkg.version + ' - ' + yyyy + '-' + mm + '-' + dd,
    homepage.trimEnd(),
    ' * GitHub: ' + (pkg.repository && pkg.repository.url ? pkg.repository.url : ''),
    ' * Copyright (c) ' + yyyy + ' ' + (pkg.author || ''),
    ' * ====================================================',
    ' */',
  ]
    .filter(Boolean)
    .join('\n');
}

function buildLogicBundle() {
  const prelude = [
    'var __vintModules = {};',
    '',
    'function define(id, factory) {',
    '  __vintModules[id] = { factory: factory, exports: {}, initialized: false };',
    '}',
    '',
    'function resolveModuleId(baseId, requestId) {',
    "  if (!/^\\.\\.?\\//.test(requestId)) return requestId;",
    "  var stack = baseId.split('/');",
    '  stack.pop();',
    "  requestId.split('/').forEach(function(part) {",
    "    if (!part || part === '.') return;",
    "    if (part === '..') stack.pop();",
    '    else stack.push(part);',
    '  });',
    "  return stack.join('/');",
    '}',
    '',
    'function requireModule(moduleId) {',
    '  var record = __vintModules[moduleId];',
    "  if (!record) throw new Error('[vite-legacy] Module not found: ' + moduleId);",
    '  if (record.initialized) return record.exports;',
    '',
    '  record.initialized = true;',
    '  var module = { exports: record.exports };',
    '  if (typeof record.factory === "function") {',
    '    record.factory(function(req) {',
    '      return requireModule(resolveModuleId(moduleId, req));',
    '    }, module.exports, module);',
    '    record.exports = module.exports;',
    '  } else {',
    '    record.exports = record.factory;',
    '  }',
    '  return record.exports;',
    '}',
    '',
    'function use(moduleId) {',
    '  return requireModule(moduleId);',
    '}',
    '',
  ].join('\n');

  const moduleFiles = collectFiles(path.join(ROOT, 'src'), file => file.endsWith('.js'));
  const moduleBodies = moduleFiles
    .map(filePath => {
      const relative = toPosix(path.relative(path.join(ROOT, 'src'), filePath));
      const moduleId = relative.replace(/\.js$/, '');
      return transformCmdModule(readText(filePath), moduleId).trim();
    })
    .join('\n\n');

  return [prelude, moduleBodies, "use('expose-editor');"].join('\n\n');
}

function transformCmdModule(sourceCode, moduleId) {
  const defineIndex = sourceCode.indexOf('define(');
  if (defineIndex < 0) {
    return sourceCode;
  }

  const defineSnippet = sourceCode.slice(defineIndex, defineIndex + 60);
  if (/define\s*\(\s*['"]/m.test(defineSnippet)) {
    return sourceCode;
  }

  return (
    sourceCode.slice(0, defineIndex) +
    "define('" + moduleId + "', " +
    sourceCode.slice(defineIndex + 'define('.length)
  );
}

function buildTemplateBundle() {
  const templateFiles = [
    ...collectFiles(path.join(ROOT, 'ui', 'directive'), file => file.endsWith('.html')),
    ...collectFiles(path.join(ROOT, 'ui', 'dialog'), file => file.endsWith('.html')),
  ].sort();

  const templateLines = templateFiles.map(filePath => {
    const templateKey = toPosix(path.relative(ROOT, filePath));
    const templateValue = escapeForSingleQuote(readText(filePath));
    return "  $templateCache.put('" + templateKey + "', '" + templateValue + "');";
  });

  return [
    "angular.module('kityminderEditor').run(['$templateCache', function($templateCache) {",
    ...templateLines,
    '}]);',
  ].join('\n');
}

function collectUiScriptPaths() {
  const groups = ['service', 'filter', 'dialog', 'directive'];
  const files = [];
  for (const group of groups) {
    files.push(
      ...collectFiles(path.join(ROOT, 'ui', group), file => file.endsWith('.js'))
    );
  }
  return files.sort();
}

async function buildStyles() {
  const less = tryRequire('less');
  if (!less) {
    throw new Error('Missing `less` dependency. Run `npm install` in webui.');
  }

  const input = readText(LESS_ENTRY);
  const outputFile = path.join(DIST_DIR, 'kityminder.editor.css');
  const sourceMapFile = path.join(DIST_DIR, 'kityminder.editor.css.map');

  const result = await less.render(input, {
    filename: LESS_ENTRY,
    sourceMap: {
      sourceMapFileInline: false,
      outputSourceFiles: true,
      sourceMapURL: path.basename(sourceMapFile),
      sourceMapFilename: sourceMapFile,
    },
  });

  writeText(outputFile, result.css || '');
  if (result.map) {
    writeText(sourceMapFile, result.map);
  }
}

function buildMainScript() {
  const mainSource = readText(MAIN_ENTRY);
  writeText(path.join(DIST_DIR, 'main.min.js'), mainSource);
}

function minifyJs(sourceCode) {
  const uglifyJs = tryRequire('uglify-js');
  if (!uglifyJs) {
    return sourceCode;
  }

  const options = {
    mangle: false,
    compress: true,
    output: {
      comments: /^!/,
    },
    fromString: true,
  };

  try {
    const result = uglifyJs.minify(sourceCode, options);
    if (result.error) {
      console.warn('[vite-legacy] JS minify skipped: ' + result.error.message);
      return sourceCode;
    }
    return result.code || sourceCode;
  } catch (err) {
    console.warn('[vite-legacy] JS minify skipped: ' + err.message);
    return sourceCode;
  }
}

function minifyCss(sourceCode) {
  return sourceCode
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/\s+/g, ' ')
    .replace(/\s*([{}:;,])\s*/g, '$1')
    .replace(/;}/g, '}')
    .trim();
}

function collectFiles(rootDir, filterFn) {
  if (!fs.existsSync(rootDir)) {
    return [];
  }

  const output = [];
  const walk = dir => {
    const entries = fs.readdirSync(dir).sort();
    for (const entry of entries) {
      const abs = path.join(dir, entry);
      const stat = fs.statSync(abs);
      if (stat.isDirectory()) {
        walk(abs);
      } else if (filterFn(abs)) {
        output.push(abs);
      }
    }
  };

  walk(rootDir);
  return output;
}

function copyDirectory(sourceDir, targetDir) {
  if (!fs.existsSync(sourceDir)) {
    return;
  }
  ensureDir(targetDir);

  const entries = fs.readdirSync(sourceDir);
  for (const entry of entries) {
    const src = path.join(sourceDir, entry);
    const dst = path.join(targetDir, entry);
    const stat = fs.statSync(src);
    if (stat.isDirectory()) {
      copyDirectory(src, dst);
    } else {
      ensureDir(path.dirname(dst));
      fs.copyFileSync(src, dst);
    }
  }
}

function removeIfExists(targetPath) {
  fs.rmSync(targetPath, { recursive: true, force: true });
}

function ensureDir(dirPath) {
  fs.mkdirSync(dirPath, { recursive: true });
}

function readText(filePath) {
  return fs.readFileSync(filePath, 'utf8');
}

function writeText(filePath, content) {
  ensureDir(path.dirname(filePath));
  fs.writeFileSync(filePath, content, 'utf8');
}

function escapeForSingleQuote(content) {
  return content
    .replace(/\\/g, '\\\\')
    .replace(/\r/g, '')
    .replace(/\n/g, '\\n')
    .replace(/'/g, "\\'");
}

function toPosix(filePath) {
  return filePath.split(path.sep).join('/');
}

function tryRequire(name) {
  try {
    return require(name);
  } catch (err) {
    return null;
  }
}

if (require.main === module) {
  buildAll().catch(err => {
    console.error('[vite-legacy] Build failed:', err && err.stack ? err.stack : err);
    process.exit(1);
  });
}

module.exports = {
  buildAll,
};
