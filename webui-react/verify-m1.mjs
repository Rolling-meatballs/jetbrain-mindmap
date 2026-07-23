// Headless smoke test for the M1 navigation toolbar. Drives the built app
// (served by `vite preview` on :5188, mock host loads the sample map) in real
// Chrome and asserts the toolbar -> engine command wiring actually works.
// Run: npm run build && npm run preview &  then  node verify-m1.mjs
import puppeteer from 'puppeteer-core';
import os from 'node:os';
import path from 'node:path';
import fs from 'node:fs';

const CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const URL = 'http://127.0.0.1:5188/';
const userDataDir = fs.mkdtempSync(path.join(os.tmpdir(), 'm1-chrome-'));
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const checks = [];
const assert = (name, cond, detail) => checks.push({ name, pass: !!cond, detail });

const browser = await puppeteer.launch({
  executablePath: CHROME,
  headless: true,
  userDataDir,
  args: ['--no-sandbox', '--disable-gpu'],
});

try {
  const page = await browser.newPage();
  await page.setViewport({ width: 1200, height: 800 });
  const errors = [];
  page.on('console', (m) => m.type() === 'error' && errors.push(m.text()));
  page.on('pageerror', (e) => errors.push(`pageerror: ${e.message}`));

  await page.goto(URL, { waitUntil: 'domcontentloaded', timeout: 30000 });
  const rendered = await page
    .waitForSelector('.km-canvas svg text', { timeout: 15000 })
    .then(() => true)
    .catch(() => false);
  await sleep(1200); // settle handshake + import + layout

  // Count VISIBLE text nodes (non-zero bounding box). Collapsed subtrees are
  // hidden, not necessarily removed from the DOM, so a raw count wouldn't move.
  const textCount = () =>
    page.evaluate(
      () =>
        Array.from(document.querySelectorAll('.km-canvas svg text')).filter((t) => {
          const r = t.getBoundingClientRect();
          return r.width > 0 && r.height > 0;
        }).length,
    );
  const zoom = () =>
    page.evaluate(
      () => document.querySelector('[data-testid="zoom-readout"]')?.textContent?.trim() ?? '',
    );
  const clickLabel = async (label) => {
    await page.click(`[aria-label="${label}"]`);
    await sleep(450);
  };

  assert('engine rendered svg', rendered);
  const nodes0 = await textCount();
  assert('sample map has nodes', nodes0 >= 6, `count=${nodes0}`);
  assert('toolbar M1 badge present', await page.evaluate(() => document.body.innerText.includes('React · M1')));

  const zoom0 = await zoom();
  assert('zoom readout shows %', /\d+%/.test(zoom0), `zoom=${zoom0}`);

  // Zoom in -> % increases
  await clickLabel('放大');
  const zoom1 = await zoom();
  assert('zoom-in raises %', parseInt(zoom1) > parseInt(zoom0), `${zoom0} -> ${zoom1}`);

  // Zoom out -> back down
  await clickLabel('缩小');
  const zoom2 = await zoom();
  assert('zoom-out lowers %', parseInt(zoom2) < parseInt(zoom1), `${zoom1} -> ${zoom2}`);

  // Cursor-centered Ctrl+wheel zoom (smooth, not level-stepped).
  const wheelAt = async (deltaY) => {
    await page.evaluate((dy) => {
      const el = document.querySelector('.km-canvas');
      const r = el.getBoundingClientRect();
      el.dispatchEvent(
        new WheelEvent('wheel', {
          deltaY: dy,
          ctrlKey: true,
          clientX: r.left + r.width * 0.35,
          clientY: r.top + r.height * 0.5,
          bubbles: true,
          cancelable: true,
        }),
      );
    }, deltaY);
    await sleep(250);
  };
  const wz0 = parseInt(await zoom());
  await wheelAt(-300); // wheel up = zoom in
  const wz1 = parseInt(await zoom());
  assert('ctrl+wheel zooms in (smooth)', wz1 > wz0, `${wz0} -> ${wz1}`);
  await wheelAt(300); // wheel down = zoom out
  const wz2 = parseInt(await zoom());
  assert('ctrl+wheel zooms out', wz2 < wz1, `${wz1} -> ${wz2}`);
  await page.click('[aria-label="适应窗口"]'); // reset framing
  await sleep(300);

  // Cursor-centering invariant: anchor the wheel on an OFF-CENTER node and assert
  // it stays under the cursor. If zoom were viewport-centered, it would drift.
  const nodeCenter = (substr) =>
    page.evaluate((s) => {
      const t = Array.from(document.querySelectorAll('.km-canvas svg text')).find((n) =>
        (n.textContent || '').includes(s),
      );
      if (!t) return null;
      const r = t.getBoundingClientRect();
      return { cx: r.left + r.width / 2, cy: r.top + r.height / 2 };
    }, substr);
  const anchorBefore = await nodeCenter('JCEF');
  if (anchorBefore) {
    await page.evaluate((pt) => {
      const el = document.querySelector('.km-canvas');
      el.dispatchEvent(
        new WheelEvent('wheel', {
          deltaY: -300,
          ctrlKey: true,
          clientX: pt.cx,
          clientY: pt.cy,
          bubbles: true,
          cancelable: true,
        }),
      );
    }, anchorBefore);
    await sleep(250);
    const anchorAfter = await nodeCenter('JCEF');
    const drift = Math.hypot(anchorAfter.cx - anchorBefore.cx, anchorAfter.cy - anchorBefore.cy);
    assert('cursor-centered: anchor node stays put', drift < 12, `drift=${drift.toFixed(1)}px`);
  } else {
    assert('cursor-centered: anchor node found', false, 'JCEF node not found');
  }
  await page.click('[aria-label="适应窗口"]');
  await sleep(300);

  // Fit -> no crash, still rendered
  await clickLabel('适应窗口');
  assert('fit keeps map rendered', (await textCount()) >= 6);

  // Collapse all -> fewer visible text nodes; Expand all -> back
  await clickLabel('全部收起');
  const collapsed = await textCount();
  assert('collapse-all hides children', collapsed < nodes0, `${nodes0} -> ${collapsed}`);
  await clickLabel('全部展开');
  const expanded = await textCount();
  assert('expand-all restores nodes', expanded >= nodes0, `${collapsed} -> ${expanded}`);

  // Save -> mock host records the payload
  await page.click('[data-testid="save-btn"]');
  await sleep(400);
  const saveBytes = await page.evaluate(() => window.__lastSave?.length ?? 0);
  assert('save round-trips to host', saveBytes > 0, `bytes=${saveBytes}`);

  assert('no console errors', errors.length === 0, errors.slice(0, 3).join(' | '));

  // ---- Image Editor smoke tests ----
  const TEST_IMAGE_URL = '/verify-image.svg';

  // The image trigger button should be in the toolbar with aria-label="图片"
  const imageTrigger = await page.$('[aria-label="图片"]');
  assert('image toolbar button exists', !!imageTrigger);

  // Select a node first (click the root node text) so image command is enabled
  const rootNodeText = await page.$('.km-canvas svg text');
  if (rootNodeText) {
    await rootNodeText.click();
    await sleep(300);
  }

  // Open the image dialog
  await page.click('[aria-label="图片"]');
  await sleep(400);

  // Dialog should appear with the URL input
  const urlInput = await page.$('[data-testid="image-url-input"]');
  assert('image dialog URL input visible', !!urlInput);

  const titleInput = await page.$('[data-testid="image-title-input"]');
  assert('image dialog title input visible', !!titleInput);

  // Insert image: type URL + title, then save
  await page.type('[data-testid="image-url-input"]', TEST_IMAGE_URL);
  await page.type('[data-testid="image-title-input"]', 'test-image');
  await page.click('[data-testid="image-save-btn"]');
  await sleep(600); // let engine load the data URI and render

  // Click the toolbar save button to capture current state
  await page.click('[data-testid="save-btn"]');
  await sleep(400);

  // Verify image was set on the node via export JSON
  const imgAfterInsert = await page.evaluate(() => {
    const w = window.__lastSave;
    if (!w) return null;
    const json = JSON.parse(w);
    return json?.root?.data?.image;
  });
  assert('image insert sets node data', imgAfterInsert === TEST_IMAGE_URL, `url=${imgAfterInsert?.slice(0, 30)}`);

  const imageSizeAfterInsert = await page.evaluate(() => {
    const w = window.__lastSave;
    if (!w) return null;
    const json = JSON.parse(w);
    return json?.root?.data?.imageSize;
  });
  assert(
    'image insert records image size',
    imageSizeAfterInsert?.width > 0 && imageSizeAfterInsert?.height > 0,
    `size=${JSON.stringify(imageSizeAfterInsert)}`,
  );

  await page.evaluate(() => {
    HTMLAnchorElement.prototype.click = function () {
      window.__lastPngExport = {
        download: this.download,
        href: this.href,
      };
    };
  });
  await page.click('[aria-label="导出 PNG"]');
  await sleep(1000);
  const pngAfterImageInsert = await page.evaluate(() => window.__lastPngExport ?? null);
  assert(
    'png export works after image insert',
    pngAfterImageInsert?.download === 'mindmap.png' &&
      pngAfterImageInsert?.href?.startsWith('data:image/png;base64,'),
    `download=${pngAfterImageInsert?.download}, href=${pngAfterImageInsert?.href?.slice(0, 30)}`,
  );

  // Edit: re-open dialog, change title, save
  await page.click('[aria-label="图片"]');
  await sleep(400);
  // Clear the URL field (it should be populated) and type new title
  const titleValBefore = await page.evaluate(() => {
    const el = document.querySelector('[data-testid="image-title-input"]');
    return el?.value ?? '';
  });
  assert('image dialog populates current title', titleValBefore === 'test-image', `title="${titleValBefore}"`);

  await page.evaluate(() => {
    const el = document.querySelector('[data-testid="image-title-input"]');
    el.value = '';
    el.dispatchEvent(new Event('input', { bubbles: true }));
  });
  await page.type('[data-testid="image-title-input"]', 'edited-image');
  await page.click('[data-testid="image-save-btn"]');
  await sleep(600);

  await page.click('[data-testid="save-btn"]');
  await sleep(400);

  const imgAfterEdit = await page.evaluate(() => {
    const w = window.__lastSave;
    if (!w) return null;
    const json = JSON.parse(w);
    return json?.root?.data?.imageTitle;
  });
  assert('image edit updates title', imgAfterEdit === 'edited-image', `title=${imgAfterEdit}`);

  // Remove: open dialog, click remove
  await page.click('[aria-label="图片"]');
  await sleep(400);
  await page.click('[data-testid="image-remove-btn"]');
  await sleep(600);

  await page.click('[data-testid="save-btn"]');
  await sleep(400);

  const imgAfterRemove = await page.evaluate(() => {
    const w = window.__lastSave;
    if (!w) return null;
    const json = JSON.parse(w);
    return json?.root?.data?.image;
  });
  assert('image remove clears node data', imgAfterRemove == null || imgAfterRemove === undefined, `url=${imgAfterRemove}`);

  // Final error check after image tests
  assert('no console errors after image tests', errors.length === 0, errors.slice(0, 3).join(' | '));

  await page.screenshot({ path: 'verify-m1-proof.png' });

  const passed = checks.filter((c) => c.pass).length;
  console.log(JSON.stringify({ passed, total: checks.length, checks, errors }, null, 2));
  process.exitCode = passed === checks.length ? 0 : 1;
} finally {
  await browser.close();
  fs.rmSync(userDataDir, { recursive: true, force: true });
}
