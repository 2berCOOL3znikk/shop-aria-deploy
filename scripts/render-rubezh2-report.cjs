'use strict';

// HTML print preview only. This does not validate a DOCX file.
const fs = require('node:fs');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { chromium } = require(process.env.ARIA_PLAYWRIGHT_MODULE || 'playwright');

async function main() {
  const root = path.resolve(__dirname, '..');
  const output = path.join(root, 'target', 'rubezh2-render');
  fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  try {
    const page = await browser.newPage();
    await page.goto(pathToFileURL(path.join(root, 'docs', 'rubezh2', 'RUBEZH2_REPORT.html')).href);
    await page.evaluate(() => document.fonts.ready);
    const images = await page.locator('img').evaluateAll(items => items.map(img => ({
      source: new URL(img.src).pathname.split('/').pop(),
      loaded: img.complete && img.naturalWidth > 0,
    })));
    if (images.some(img => !img.loaded)) throw new Error('Some report images did not load');
    await page.pdf({
      path: path.join(output, 'report-qa.pdf'),
      preferCSSPageSize: true,
      printBackground: true,
      displayHeaderFooter: true,
      headerTemplate: '<div></div>',
      footerTemplate: '<div style="width:100%;font-size:8px;color:black;text-align:center;">ARIA · II рубежный контроль · <span class="pageNumber"></span> / <span class="totalPages"></span></div>',
    });
    fs.writeFileSync(path.join(output, 'html-render.json'), JSON.stringify({
      format: 'HTML print preview',
      browser: await browser.version(),
      images,
      nativeDocxValidated: false,
    }, null, 2));
    console.log(`Report HTML rendered; ${images.length} images loaded`);
  } finally {
    await browser.close();
  }
}

main().catch(error => { console.error(error.message); process.exitCode = 1; });
