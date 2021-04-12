
import fs from 'fs';

import playwright from 'playwright';

import path from 'path';

(async () => {

  
    const input = fs.readFileSync('input.txt', 'utf8');

    const urls = input.split(/\r?\n/).map(url => {
        let raw = url.trim();
        let correctStart = raw.slice(raw.indexOf('http'), raw.length);
        return correctStart.split(' ')[0];
    });

    if (!urls.length) {
      throw new Error('Unable to find URL');
    }

    let url = urls[0];

    let dirname = path.resolve(path.dirname(''));

    const browser = await playwright['chromium'].launch({
      headless: false
    });
    console.log({dirname, url});
    const context = await browser.newContext({
      recordHar: { 
        omitContent: false,
        path: path.join(dirname, 'har.har')
      }
    });
    const page = await context.newPage();
    await page.goto(url);
    await page.waitForTimeout(10000);
    await context.close();
    await browser.close();
  })();