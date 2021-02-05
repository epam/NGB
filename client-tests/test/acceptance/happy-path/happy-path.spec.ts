import { describe, it, xit, expect } from "../../test-support";
import tracksFor from "../../test-support/data-storage";
import { ITrack } from "../../test-support/types";
import { pageObject, setupPage, buildUrl } from "../../test-support/page-object";

describe("Acceptance | Happy path |", () => {
  xit("is a basic test with the page", async ({ page }) => {
    await setupPage(page);

    const tracks: ITrack[] = tracksFor("is a basic test with the page");

    const url = buildUrl("DM6", "", "", "", tracks);

    await page.goto(url);

    await page.waitForSelector(".ngb-variant-density-diagram .nvd3-svg g");

    // animations
    await page.waitForTimeout(1000);

    const diagrams = [
      "ngb-variant-density-diagram",
      "ngb-variant-type-diagram",
      "ngb-variant-quality-diagram",
    ];

    for (let diagram of diagrams) {
      const element = await page.$(`.${diagram}`);
      const screenshot = await element.screenshot({ omitBackground: true });
      expect(screenshot).toMatchSnapshot(`${diagram}.png`, { threshold: 0.01 });
    }
  });

  xit("it able to test mouse movements", async ({ page }) => {
    await setupPage(page);

    const tracks: ITrack[] = tracksFor("it able to test mouse movements");

    const url = buildUrl("DM6", "X", "1", "23542271", tracks);

    await page.goto(url);

    // animations
    await page.waitForTimeout(5000);

    const element = await page.$(`.ngb-track-renderer`);
    // const coords = await element.boundingBox();
    const screenshot = await element.screenshot({ omitBackground: true });
    expect(screenshot).toMatchSnapshot(`ngb-track-renderer.png`, {
      threshold: 0.01,
    });

    // await page.mouse.move(coords.x + coords.height / 2, coords.y + coords.width / 2);
    // await page.mouse.down();
    // await page.mouse.move(coords.x + coords.height / 2, coords.y + coords.width / 2 + 5, { steps: 5 });
    // await page.mouse.up();
    // expect(screenshot).toMatchSnapshot(`ngb-track-renderer-moved.png`, { threshold: 0.01 });
  });

  it("matches barchart graph", async ({ page }) => {
    await setupPage(page);

    const tracks: ITrack[] = tracksFor("matches barchart graph");

    const url = buildUrl("h37", "1", "150966170", "150966249", tracks);
    await page.goto(url);

    // animations
    await page.waitForTimeout(10000);
    const element = await page.$(`.tracks-panel`);
    const screenshot = await element.screenshot({ omitBackground: true });
    expect(screenshot).toMatchSnapshot(`barchart-panel.png`, { threshold: 0 });
  });

  it("matches distribution graph", async ({ page }) => {
    await setupPage(page);

    const tracks: ITrack[] = tracksFor("matches distribution graph");

    const url = buildUrl("h37", "1", "150964883", "150967542", tracks);

    await page.goto(url);

    // animations
    await page.waitForTimeout(10000);
    const element = await page.$(`.tracks-panel`);
    const screenshot = await element.screenshot({ omitBackground: true });
    expect(screenshot).toMatchSnapshot(`tracks-panel.png`, { threshold: 0 });
  });

  it("works for track renderer", async ({ page }) => {
    await setupPage(page);

    const tracks: ITrack[] = tracksFor("works for track renderer");

    const url = buildUrl("dm6", "X", "12584385", "12592193", tracks);

    await page.goto(url);

    // animations
    await page.waitForTimeout(10000);

    // group-by
    await page.click(pageObject.windows.main.trackSettings.groupBy.container);

    // by start location
    await page.click(
      pageObject.windows.main.trackSettings.groupBy.options.startLocation
    );

    // by insert size
    // await page.click();

    await page.evaluate(() => {
      var $0 = document.querySelector(
        "body > ui-view > div > div.golden-layout > ngb-golden-layout > div > div > div > div:nth-child(1) > div.lm_items > div > div > ngb-browser > ngb-tracks-view > div > div > div:nth-child(2) > ngb-track:nth-child(3) > div > div.ngb-track-renderer.js-ngb-render-container-target.flex-100 > div > canvas"
      );
      // @ts-expect-error
      var context = $0.getContext("2d");
      // @ts-expect-error
      var imageData = context.getImageData(0, 0, $0.width, $0.height);
      var items = imageData.data.length;


      // 204, 216, 221, 1 // #ccd8dd
      // 162, 171, 175, 1 // #A2ABAF

      // convert blue to white
      for (var j = 0; j < items; j += 4) {
        if (imageData.data[j] >= 150) {
          if (imageData.data[j + 1] >= 150) {
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
    expect(screenshot).toMatchSnapshot(`ngb-track-renderer-canvas.png`, {
      threshold: 0.6,
    });
  });

  it("matches with disabled downsampling", async ({ page }) => {
    await setupPage(page);

    const tracks: ITrack[] = tracksFor("matches with disabled downsampling");

    const url = buildUrl("dm6", "X", "12584385", "12592193", tracks);

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
    await page.waitForSelector(
      pageObject.windows.settings.tabs.alignments.content
    );

    // downsampling uncheck
    await page.click(
      pageObject.windows.settings.tabs.alignments.checkboxes.downsampling
    );

    // save button
    await page.click(pageObject.windows.settings.tabs.alignments.buttons.save);

    // (data loading & redraw)
    await page.waitForTimeout(10000);

    const element = await page.$(pageObject.windows.main.canvas);

    // const coords = await element.boundingBox();
    const screenshot = await element.screenshot({ omitBackground: true });
    expect(screenshot).toMatchSnapshot(
      `ngb-track-renderer-canvas-no-threshold.png`,
      {
        threshold: 0,
      }
    );
  });
});
