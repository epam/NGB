import { describe, it, xit, expect } from './index';

async function setupPage(page) {
  page.setViewportSize({width: 1500, height: 800});
}

describe('foo', () => {

  xit("is a basic test with the page", async ({ page }) => {

    await setupPage(page);

    const url = 'http://localhost:8080/#/DM6/?tracks=%5B%7B%22b%22:%22DM6%22,%22p%22:%22Fruitfly%22,%22f%22:%22REFERENCE%22,%22projectIdNumber%22:1%7D,%7B%22b%22:%22agnts3.09-28.trim.dm606.realign.bam%22,%22p%22:%22Fruitfly%22,%22f%22:%22BAM%22,%22projectIdNumber%22:1%7D,%7B%22b%22:%22agnX1.09-28.trim.dm606.realign.bam%22,%22p%22:%22Fruitfly%22,%22f%22:%22BAM%22,%22projectIdNumber%22:1%7D,%7B%22b%22:%22agnX1.09-28.trim.dm606.realign.vcf%22,%22p%22:%22Fruitfly%22,%22f%22:%22VCF%22,%22projectIdNumber%22:1%7D,%7B%22b%22:%22CantonS.09-28.trim.dm606.realign.bam%22,%22p%22:%22Fruitfly%22,%22f%22:%22BAM%22,%22projectIdNumber%22:1%7D,%7B%22b%22:%22CantonS.09-28.trim.dm606.realign.vcf%22,%22p%22:%22Fruitfly%22,%22f%22:%22VCF%22,%22projectIdNumber%22:1%7D,%7B%22b%22:%22DM6_Genes%22,%22p%22:%22%22,%22l%22:true,%22f%22:%22GENE%22%7D%5D';
    await page.goto(url);

    await page.waitForSelector('.ngb-variant-density-diagram .nvd3-svg g');

    // animations
    await page.waitForTimeout(1000);
  
    const diagrams = [
      'ngb-variant-density-diagram', 
      'ngb-variant-type-diagram',
      'ngb-variant-quality-diagram'
    ];

    for (let diagram of diagrams) {
      const element = await page.$(`.${diagram}`);
      const screenshot = await element.screenshot({ omitBackground: true });
      expect(screenshot).toMatchSnapshot(`${diagram}.png`, { threshold: 0.01 });
    }

  });


  xit('it able to test mouse movements', async ( { page }) => {

    await setupPage(page);

    const url = 'http://localhost:8080/#/DM6/X/1/23542271?tracks=%5B%7B%22b%22:%22agnts3.09-28.trim.dm606.realign.bam%22,%22f%22:%22BAM%22,%22h%22:250,%22p%22:%22Fruitfly%22,%22s%22:%7B%22a%22:true,%22aa%22:true,%22c%22:%22noColor%22,%22c1%22:true,%22d%22:true,%22g1%22:%22default%22,%22i%22:true,%22m%22:true,%22r%22:%221%22,%22s1%22:false,%22s2%22:true,%22s3%22:false,%22v1%22:false,%22cdm%22:%22Bar%20Graph%22,%22cls%22:false,%22csm%22:%22default%22%7D,%22projectIdNumber%22:1%7D,%7B%22b%22:%22agnX1.09-28.trim.dm606.realign.bam%22,%22f%22:%22BAM%22,%22h%22:250,%22p%22:%22Fruitfly%22,%22s%22:%7B%22a%22:true,%22aa%22:true,%22c%22:%22noColor%22,%22c1%22:true,%22d%22:true,%22g1%22:%22default%22,%22i%22:true,%22m%22:true,%22r%22:%221%22,%22s1%22:false,%22s2%22:true,%22s3%22:false,%22v1%22:false,%22cdm%22:%22Bar%20Graph%22,%22cls%22:false,%22csm%22:%22default%22%7D,%22projectIdNumber%22:1%7D,%7B%22b%22:%22DM6%22,%22f%22:%22REFERENCE%22,%22h%22:100,%22p%22:%22Fruitfly%22,%22s%22:%7B%22rt%22:false,%22rsfs%22:true,%22rsrs%22:false%7D,%22projectIdNumber%22:1%7D,%7B%22b%22:%22agnX1.09-28.trim.dm606.realign.vcf%22,%22f%22:%22VCF%22,%22h%22:70,%22p%22:%22Fruitfly%22,%22s%22:%7B%22v%22:%22Collapsed%22%7D,%22projectIdNumber%22:1%7D,%7B%22b%22:%22CantonS.09-28.trim.dm606.realign.bam%22,%22f%22:%22BAM%22,%22h%22:250,%22p%22:%22Fruitfly%22,%22s%22:%7B%22a%22:true,%22aa%22:true,%22c%22:%22noColor%22,%22c1%22:true,%22d%22:true,%22g1%22:%22default%22,%22i%22:true,%22m%22:true,%22r%22:%221%22,%22s1%22:false,%22s2%22:true,%22s3%22:false,%22v1%22:false,%22cdm%22:%22Bar%20Graph%22,%22cls%22:false,%22csm%22:%22default%22%7D,%22projectIdNumber%22:1%7D,%7B%22b%22:%22CantonS.09-28.trim.dm606.realign.vcf%22,%22f%22:%22VCF%22,%22h%22:70,%22p%22:%22Fruitfly%22,%22s%22:%7B%22v%22:%22Collapsed%22%7D,%22projectIdNumber%22:1%7D,%7B%22b%22:%22DM6_Genes%22,%22f%22:%22GENE%22,%22h%22:158,%22l%22:true,%22p%22:%22%22,%22s%22:%7B%22g%22:%22collapsed%22%7D%7D%5D';
      
    await page.goto(url);

    // animations
    await page.waitForTimeout(5000);

    const element = await page.$(`.ngb-track-renderer`);
    // const coords = await element.boundingBox();
    const screenshot = await element.screenshot({ omitBackground: true });
    expect(screenshot).toMatchSnapshot(`ngb-track-renderer.png`, { threshold: 0.01 });

    // await page.mouse.move(coords.x + coords.height / 2, coords.y + coords.width / 2);
    // await page.mouse.down();
    // await page.mouse.move(coords.x + coords.height / 2, coords.y + coords.width / 2 + 5, { steps: 5 });
    // await page.mouse.up();
    // expect(screenshot).toMatchSnapshot(`ngb-track-renderer-moved.png`, { threshold: 0.01 });


  });



  it('matches barchart graph', async ({page}) => {

    await setupPage(page);

    const url = 'http://localhost:8080/#/h37/1/150966170/150966249?tracks=%5B%7B%22b%22:%22h37%22,%22p%22:%22h37_data%22,%22f%22:%22REFERENCE%22,%22projectIdNumber%22:1,%22h%22:100,%22s%22:%7B%22rt%22:false,%22rsfs%22:true,%22rsrs%22:false%7D%7D,%7B%22b%22:%22sv_sample_2.bw%22,%22h%22:86,%22p%22:%22h37_data%22,%22projectIdNumber%22:1,%22s%22:%7B%22cdm%22:%22Bar%20Graph%22,%22cls%22:false,%22csm%22:%22default%22,%22csf%22:100,%22cst%22:900,%22wigColors%22:%7B%22negative%22:16731471,%22positive%22:3265094%7D%7D,%22n%22:%22sv_sample_2.bw%22,%22f%22:%22WIG%22%7D%5D';
    await page.goto(url);

    // animations
    await page.waitForTimeout(10000);
    const element = await page.$(`.tracks-panel`);
    const screenshot = await element.screenshot({ omitBackground: true });
    expect(screenshot).toMatchSnapshot(`barchart-panel.png`, { threshold: 0 });
  });


  it('matches distribution graph', async ({page}) => {

    await setupPage(page);

    const url = 'http://localhost:8080/#/h37/1/150964883/150967542?tracks=%5B%7B%22b%22:%22h37%22,%22p%22:%22h37_data%22,%22f%22:%22REFERENCE%22,%22projectIdNumber%22:1,%22h%22:100,%22s%22:%7B%22rt%22:false,%22rsfs%22:true,%22rsrs%22:false%7D%7D,%7B%22b%22:%22Homo_sapiens.GRCh37.gtf.gz%22,%22h%22:84,%22l%22:true,%22p%22:%22%22,%22s%22:%7B%22g%22:%22expanded%22%7D,%22f%22:%22GENE%22%7D,%7B%22b%22:%22sv_sample_2.bw%22,%22h%22:150,%22p%22:%22h37_data%22,%22projectIdNumber%22:1,%22s%22:%7B%22cdm%22:%22Bar%20Graph%22,%22cls%22:false,%22csm%22:%22default%22,%22csf%22:100,%22cst%22:900,%22wigColors%22:%7B%22negative%22:16731471,%22positive%22:3265094%7D%7D,%22n%22:%22sv_sample_2.bw%22,%22f%22:%22WIG%22%7D%5D';
    await page.goto(url);

    // animations
    await page.waitForTimeout(10000);
    const element = await page.$(`.tracks-panel`);
    const screenshot = await element.screenshot({ omitBackground: true });
    expect(screenshot).toMatchSnapshot(`tracks-panel.png`, { threshold: 0 });
  });

  it('works for track renderer', async ({page}) => {
    
    page.setViewportSize({width: 1500, height: 800});
    const url = 'http://localhost:8080/#/dm6/X/12584385/12592193?tracks=%5B%7B%22b%22:%22dm6%22,%22p%22:%22dm6_data%22,%22f%22:%22REFERENCE%22,%22projectIdNumber%22:2,%22h%22:100,%22s%22:%7B%22rt%22:false,%22rsfs%22:true,%22rsrs%22:false%7D%7D,%7B%22b%22:%22agnX1.09-28.trim.dm606.realign.bam%22,%22h%22:576,%22p%22:%22dm6_data%22,%22projectIdNumber%22:2,%22s%22:%7B%22a%22:true,%22aa%22:true,%22c%22:%22insertSize%22,%22c1%22:true,%22d%22:true,%22g1%22:%22default%22,%22he%22:%7B%22fontSize%22:%2216px%22%7D,%22i%22:true,%22m%22:true,%22r%22:2,%22s1%22:true,%22s2%22:true,%22s3%22:false,%22v1%22:false,%22cdm%22:%22Bar%20Graph%22,%22cls%22:false,%22csm%22:%22default%22%7D,%22n%22:%22agnX1.09-28.trim.dm606.realign.bam%22,%22f%22:%22BAM%22%7D%5D';
  
    await page.goto(url);

    // animations
    await page.waitForTimeout(10000);

    // group-by
    await page.click('body > ui-view > div > div.golden-layout > ngb-golden-layout > div > div > div > div:nth-child(1) > div.lm_items > div > div > ngb-browser > ngb-tracks-view > div > div > div:nth-child(2) > ngb-track:nth-child(3) > div > div.ngb-track-header.layout-align-start-center.layout-row.flex-100 > ngb-track-settings > md-menu:nth-child(5) > button > a:nth-child(1)');
    
    // by start location
    await page.click('#menu_container_11 > md-menu-content > div:nth-child(2) > md-menu-item > button > span:nth-child(1)');

    // by insert size
    // await page.click('#menu_container_11 > md-menu-content > div:nth-child(6) > md-menu-item > button > span:nth-child(1)');

    await page.evaluate(() => {
      var $0 = document.querySelector('body > ui-view > div > div.golden-layout > ngb-golden-layout > div > div > div > div:nth-child(1) > div.lm_items > div > div > ngb-browser > ngb-tracks-view > div > div > div:nth-child(2) > ngb-track:nth-child(3) > div > div.ngb-track-renderer.js-ngb-render-container-target.flex-100 > div > canvas');
      var context = $0.getContext('2d');
      var imageData = context.getImageData(0, 0, $0.width, $0.height);
      var items = imageData.data.length;
      for  (var j = 0; j < items; j+= 4) { 
        if (imageData.data[j] >= 150) { 
          if (imageData.data[j + 1 ] >= 150) {
            if (imageData.data[j + 2] >= 150) {
              imageData.data[j] = 255;
              imageData.data[j + 1] = 255;
              imageData.data[j + 2] = 255;
            }
          }
        }
      }      
      
      context.putImageData(imageData, 0, 0);
    });

    const element = await page.$(`body > ui-view > div > div.golden-layout > ngb-golden-layout > div > div > div > div:nth-child(1) > div.lm_items > div > div > ngb-browser > ngb-tracks-view > div > div > div:nth-child(2) > ngb-track:nth-child(3) > div > div.ngb-track-renderer.js-ngb-render-container-target.flex-100 > div > canvas`);

    // const coords = await element.boundingBox();
    const screenshot = await element.screenshot({ omitBackground: true });
    // 0.7
    expect(screenshot).toMatchSnapshot(`ngb-track-renderer-canvas.png`, { threshold: 0.6 });
  });
  

  it('matches with disabled downsampling', async ({page}) => {
    page.setViewportSize({width: 1500, height: 800});
    const url = 'http://localhost:8080/#/dm6/X/12584385/12592193?tracks=%5B%7B%22b%22:%22dm6%22,%22p%22:%22dm6_data%22,%22f%22:%22REFERENCE%22,%22projectIdNumber%22:2,%22h%22:100,%22s%22:%7B%22rt%22:false,%22rsfs%22:true,%22rsrs%22:false%7D%7D,%7B%22b%22:%22agnX1.09-28.trim.dm606.realign.bam%22,%22h%22:576,%22p%22:%22dm6_data%22,%22projectIdNumber%22:2,%22s%22:%7B%22a%22:true,%22aa%22:true,%22c%22:%22insertSize%22,%22c1%22:true,%22d%22:true,%22g1%22:%22default%22,%22he%22:%7B%22fontSize%22:%2216px%22%7D,%22i%22:true,%22m%22:true,%22r%22:2,%22s1%22:true,%22s2%22:true,%22s3%22:false,%22v1%22:false,%22cdm%22:%22Bar%20Graph%22,%22cls%22:false,%22csm%22:%22default%22%7D,%22n%22:%22agnX1.09-28.trim.dm606.realign.bam%22,%22f%22:%22BAM%22%7D%5D';
  
    await page.goto(url);

    // animations
    await page.waitForTimeout(10000);
    
    // settings button
    await page.click('body > ui-view > div > div.left-menu > ngb-main-toolbar > div > ul > li:nth-child(5) > ngb-main-settings > button');
  
    // settings window
    await page.waitForSelector('body > div.md-dialog-container.ng-scope > md-dialog > form > md-toolbar > div > h2');
 

    // aligments tab in settings

    await page.click('md-tabs > md-tabs-wrapper > md-tabs-canvas > md-pagination-wrapper > md-tab-item:nth-child(2) > span');
    

    // wait for tab
    await page.waitForSelector('[role="tabpanel"]');

    // downsampling uncheck
    await page.click('[aria-label="Downsample reads"]');
  
    // save button
    await page.click('body > div.md-dialog-container.ng-scope > md-dialog > form > md-dialog-actions > button.md-primary.md-button.md-ink-ripple > span');
  
    // (data loading & redraw)
    await page.waitForTimeout(10000);

    const element = await page.$(`body > ui-view > div > div.golden-layout > ngb-golden-layout > div > div > div > div:nth-child(1) > div.lm_items > div > div > ngb-browser > ngb-tracks-view > div > div > div:nth-child(2) > ngb-track:nth-child(3) > div > div.ngb-track-renderer.js-ngb-render-container-target.flex-100 > div > canvas`);

    // const coords = await element.boundingBox();
    const screenshot = await element.screenshot({ omitBackground: true });
    expect(screenshot).toMatchSnapshot(`ngb-track-renderer-canvas-no-threshold.png`, { threshold: 0 });
  });
});