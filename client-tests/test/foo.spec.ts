import { describe, it, xit, expect } from './index';

async function setupPage(page) {
  page.setViewportSize({width: 1500, height: 800});
}

interface ITrack {
  b: string;
  h?: number;
  p: string;
  n?: string;
  l?: boolean;
  projectIdNumber?: number;
  f?: 'WIG' | 'BAM' | 'VCF' | 'GENE' | 'REFERENCE',
  s?: {
    rsfs?: boolean;
    rsrs?: boolean;
    rt?: false;
    a?: boolean;
    d?: boolean;
    aa?: boolean;
    m?: boolean;
    c?: 'noColor' | 'insertSize';
    i?: boolean;
    g?: 'collapsed' | 'expanded';
    s?: boolean;
    v?: 'Collapsed';
    c1?: boolean;
    g1?: 'default';
    r?: string | number; // ERROR!
    s1?: boolean;
    s2?: boolean;
    s3?: boolean;
    v1?: boolean;
    cdm?: 'Bar Graph' | 'Heat Map';
    cls?: boolean;
    csm?: 'default';
    csf?: number;
    cst?: number;
    he?: {
      fontSize: string;
    },
    wigColors?: {
      negative: number;
      positive: number;
    }
  }
}

const hostname = `http://localhost:8080`;


const pageObject = {
  buttons: {
    settings: 'body > ui-view > div > div.left-menu > ngb-main-toolbar > div > ul > li:nth-child(5) > ngb-main-settings > button'
  },
  windows: {
    settings:  {
      container: 'body > div.md-dialog-container.ng-scope > md-dialog > form > md-toolbar > div > h2',
      tabs: {
        alignments: {
          container: 'md-tabs > md-tabs-wrapper > md-tabs-canvas > md-pagination-wrapper > md-tab-item:nth-child(2) > span',
          content: '[role="tabpanel"]',
          checkboxes: {
            downsampling: '[aria-label="Downsample reads"]'
          },
          buttons: {
            save: 'body > div.md-dialog-container.ng-scope > md-dialog > form > md-dialog-actions > button.md-primary.md-button.md-ink-ripple > span'
          }
        }
      },
    },
    main: {
      trackSettings: {
        groupBy: {
          container: 'body > ui-view > div > div.golden-layout > ngb-golden-layout > div > div > div > div:nth-child(1) > div.lm_items > div > div > ngb-browser > ngb-tracks-view > div > div > div:nth-child(2) > ngb-track:nth-child(3) > div > div.ngb-track-header.layout-align-start-center.layout-row.flex-100 > ngb-track-settings > md-menu:nth-child(5) > button > a:nth-child(1)',
          options: {
            startLocation: '#menu_container_11 > md-menu-content > div:nth-child(2) > md-menu-item > button > span:nth-child(1)',
            insertSize: '#menu_container_11 > md-menu-content > div:nth-child(6) > md-menu-item > button > span:nth-child(1)',
          }
        },
      },
      canvas: 'body > ui-view > div > div.golden-layout > ngb-golden-layout > div > div > div > div:nth-child(1) > div.lm_items > div > div > ngb-browser > ngb-tracks-view > div > div > div:nth-child(2) > ngb-track:nth-child(3) > div > div.ngb-track-renderer.js-ngb-render-container-target.flex-100 > div > canvas'
    }
  }, 
}

function buildUrl(referenceId: string, chromosome: string = '', start: string, end: string, tracks: ITrack[] = []) {
  return `${hostname}/#/${[referenceId, chromosome, start, end].filter(el => el.length).join('/')}?tracks=${encodeURIComponent(JSON.stringify(tracks))}`;
}

describe('foo', () => {

  xit("is a basic test with the page", async ({ page }) => {

    await setupPage(page);

    const tracks: ITrack[] = [
      {"b":"DM6","p":"Fruitfly","f":"REFERENCE","projectIdNumber":1},
      {"b":"agnts3.09-28.trim.dm606.realign.bam","p":"Fruitfly","f":"BAM","projectIdNumber":1},
      {"b":"agnX1.09-28.trim.dm606.realign.bam","p":"Fruitfly","f":"BAM","projectIdNumber":1},
      {"b":"agnX1.09-28.trim.dm606.realign.vcf","p":"Fruitfly","f":"VCF","projectIdNumber":1},
      {"b":"CantonS.09-28.trim.dm606.realign.bam","p":"Fruitfly","f":"BAM","projectIdNumber":1},
      {"b":"CantonS.09-28.trim.dm606.realign.vcf","p":"Fruitfly","f":"VCF","projectIdNumber":1},
      {"b":"DM6_Genes","p":"","l":true,"f":"GENE"}];

    const url = buildUrl('DM6', '', '', '', tracks);

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

    const tracks: ITrack[] = [
        {"b":"agnts3.09-28.trim.dm606.realign.bam","f":"BAM","h":250,"p":"Fruitfly","s":{"a":true,"aa":true,"c":"noColor","c1":true,"d":true,"g1":"default","i":true,"m":true,"r":"1","s1":false,"s2":true,"s3":false,"v1":false,"cdm":"Bar Graph","cls":false,"csm":"default"},"projectIdNumber":1},
        {"b":"agnX1.09-28.trim.dm606.realign.bam","f":"BAM","h":250,"p":"Fruitfly","s":{"a":true,"aa":true,"c":"noColor","c1":true,"d":true,"g1":"default","i":true,"m":true,"r":"1","s1":false,"s2":true,"s3":false,"v1":false,"cdm":"Bar Graph","cls":false,"csm":"default"},"projectIdNumber":1},
        {"b":"DM6","f":"REFERENCE","h":100,"p":"Fruitfly","s":{"rt":false,"rsfs":true,"rsrs":false},"projectIdNumber":1},{"b":"agnX1.09-28.trim.dm606.realign.vcf","f":"VCF","h":70,"p":"Fruitfly","s":{"v":"Collapsed"},"projectIdNumber":1},
        {"b":"CantonS.09-28.trim.dm606.realign.bam","f":"BAM","h":250,"p":"Fruitfly","s":{"a":true,"aa":true,"c":"noColor","c1":true,"d":true,"g1":"default","i":true,"m":true,"r":"1","s1":false,"s2":true,"s3":false,"v1":false,"cdm":"Bar Graph","cls":false,"csm":"default"},"projectIdNumber":1},
        {"b":"CantonS.09-28.trim.dm606.realign.vcf","f":"VCF","h":70,"p":"Fruitfly","s":{"v":"Collapsed"},"projectIdNumber":1},{"b":"DM6_Genes","f":"GENE","h":158,"l":true,"p":"","s":{"g":"collapsed"}}];

    const url = buildUrl('DM6', 'X', '1', '23542271', tracks);

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

    const tracks: ITrack[] = [
      {"b":"h37","p":"h37_data","f":"REFERENCE","projectIdNumber":1,"h":100,"s":{"rt":false,"rsfs":true,"rsrs":false}},
      {"b":"sv_sample_2.bw","h":86,"p":"h37_data","projectIdNumber":1,"s":{"cdm":"Bar Graph","cls":false,"csm":"default","csf":100,"cst":900,"wigColors":{"negative":16731471,"positive":3265094}},"n":"sv_sample_2.bw","f":"WIG"}];
    
    const url = buildUrl('h37', '1', '150966170', '150966249', tracks);
    await page.goto(url);

    // animations
    await page.waitForTimeout(10000);
    const element = await page.$(`.tracks-panel`);
    const screenshot = await element.screenshot({ omitBackground: true });
    expect(screenshot).toMatchSnapshot(`barchart-panel.png`, { threshold: 0 });
  });


  it('matches distribution graph', async ({page}) => {

    await setupPage(page);

    const tracks: ITrack[] = [
      {"b":"h37","p":"h37_data","f":"REFERENCE","projectIdNumber":1,"h":100,"s":{"rt":false,"rsfs":true,"rsrs":false}},{"b":"Homo_sapiens.GRCh37.gtf.gz","h":84,"l":true,"p":"","s":{"g":"expanded"},"f":"GENE"},
      {"b":"sv_sample_2.bw","h":150,"p":"h37_data","projectIdNumber":1,"s":{"cdm":"Bar Graph","cls":false,"csm":"default","csf":100,"cst":900,"wigColors":{"negative":16731471,"positive":3265094}},"n":"sv_sample_2.bw","f":"WIG"}];
    
    const url = buildUrl('h37', '1', '150964883', '150967542', tracks);

    await page.goto(url);

    // animations
    await page.waitForTimeout(10000);
    const element = await page.$(`.tracks-panel`);
    const screenshot = await element.screenshot({ omitBackground: true });
    expect(screenshot).toMatchSnapshot(`tracks-panel.png`, { threshold: 0 });
  });

  it('works for track renderer', async ({page}) => {
    
    await setupPage(page);

    const tracks: ITrack[] = [
      {"b":"dm6","p":"dm6_data","f":"REFERENCE","projectIdNumber":2,"h":100,"s":{"rt":false,"rsfs":true,"rsrs":false}},
      {"b":"agnX1.09-28.trim.dm606.realign.bam","h":576,"p":"dm6_data","projectIdNumber":2,"s":{"a":true,"aa":true,"c":"insertSize","c1":true,"d":true,"g1":"default","he":{"fontSize":"16px"},"i":true,"m":true,"r":2,"s1":true,"s2":true,"s3":false,"v1":false,"cdm":"Bar Graph","cls":false,"csm":"default"},"n":"agnX1.09-28.trim.dm606.realign.bam","f":"BAM"}];
    
    const url = buildUrl('dm6', 'X', '12584385', '12592193', tracks);
  
    await page.goto(url);

    // animations
    await page.waitForTimeout(10000);

    // group-by
    await page.click(pageObject.windows.main.trackSettings.groupBy.container);
    
    // by start location
    await page.click(pageObject.windows.main.trackSettings.groupBy.options.startLocation);

    // by insert size
    // await page.click();

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

    const element = await page.$(pageObject.windows.main.canvas);

    // const coords = await element.boundingBox();
    const screenshot = await element.screenshot({ omitBackground: true });
    // 0.7
    expect(screenshot).toMatchSnapshot(`ngb-track-renderer-canvas.png`, { threshold: 0.6 });
  });
  

  it('matches with disabled downsampling', async ({page}) => {

    await setupPage(page);

    const tracks: ITrack[] = [{"b":"dm6","p":"dm6_data","f":"REFERENCE","projectIdNumber":2,"h":100,"s":{"rt":false,"rsfs":true,"rsrs":false}},{"b":"agnX1.09-28.trim.dm606.realign.bam","h":576,"p":"dm6_data","projectIdNumber":2,"s":{"a":true,"aa":true,"c":"insertSize","c1":true,"d":true,"g1":"default","he":{"fontSize":"16px"},"i":true,"m":true,"r":2,"s1":true,"s2":true,"s3":false,"v1":false,"cdm":"Bar Graph","cls":false,"csm":"default"},"n":"agnX1.09-28.trim.dm606.realign.bam","f":"BAM"}];
    
    const url = buildUrl('dm6', 'X', '12584385', '12592193', tracks);
  
    await page.goto(url);

    // animations
    await page.waitForTimeout(10000);

    // settings button
    await page.click(pageObject.buttons.settings);
  
    // settings window
    await page.waitForSelector(pageObject.windows.settings.container);
 
    // aligments tab in settings
    await page.click(pageObject.windows.settings.tabs.alignments.container);
    
    // wait for tab
    await page.waitForSelector(pageObject.windows.settings.tabs.alignments.content);

    // downsampling uncheck
    await page.click(pageObject.windows.settings.tabs.alignments.checkboxes.downsampling);
  
    // save button
    await page.click(pageObject.windows.settings.tabs.alignments.buttons.save);
  
    // (data loading & redraw)
    await page.waitForTimeout(10000);

    const element = await page.$(pageObject.windows.main.canvas);

    // const coords = await element.boundingBox();
    const screenshot = await element.screenshot({ omitBackground: true });
    expect(screenshot).toMatchSnapshot(`ngb-track-renderer-canvas-no-threshold.png`, { threshold: 0 });
  });
});