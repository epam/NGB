/*
 * Copyright 2023 EPAM Systems, Inc. (https://www.epam.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.ngb.autotests.pages;

import static com.assertthat.selenium_shutterbug.core.Shutterbug.shootElement;
import com.codeborne.selenide.Condition;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import com.codeborne.selenide.HoverOptions;
import static com.codeborne.selenide.Selectors.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.Selenide.actions;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import com.epam.ngb.autotests.menus.TrackMenuForm;
import static com.epam.ngb.autotests.utils.AppProperties.DEFAULT_TIMEOUT;
import static com.epam.ngb.autotests.utils.AppProperties.RESULTS_PATH;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openqa.selenium.By.className;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.NoSuchElementException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class BrowserPage implements AccessObject<BrowserPage> {

    private static final SelenideElement chromosomeBlock = $(className("chromosome-block"));
    private static final SelenideElement chromosomeInputField = chromosomeBlock.$x("./md-input-container//input");
    private static final SelenideElement coordinatesBlock = $(className("coordinates-block"));
    private static final SelenideElement coordinatesInputField = coordinatesBlock.$x("./md-input-container/input");

    // Track Coordinates

    public BrowserPage setChromosome(String chromosome) {
        chromosomeBlock.shouldBe(exist, ofSeconds(DEFAULT_TIMEOUT)).click();
        chromosomeInputField.shouldBe(exist, ofSeconds(DEFAULT_TIMEOUT))
                .shouldBe(enabled).setValue(chromosome).pressEnter();
        sleep(1, SECONDS);
        ensure(coordinatesBlock, text(format("%s: 1 - ", chromosome)));
        return this;
    }

    public BrowserPage setCoordinates(String coordinates) {
        coordinatesBlock.shouldBe(visible, enabled).click();
        coordinatesInputField.shouldBe(visible, enabled).sendKeys(coordinates);
        coordinatesInputField.shouldBe(visible, enabled).pressEnter();
        return this;
    }

    public BrowserPage checkTrackAttributes(String trackName, Boolean isExist, String ... attributes) {
        SelenideElement track =
                $x(format(".//div[contains(@class,'summary-item') and contains(., '%s')]", trackName))
                        .shouldBe(exist);
        Arrays.stream(attributes)
                .forEach(attribute -> assertEquals(track.$$x("./div/span")
                        .filter(Condition.matchText(attribute.toUpperCase())).size() == 1, isExist,
                        format("Attribute %s should be exist - %s", attribute, isExist)));
        return this;
    }

    private SelenideElement getTrackByName(String trackName) {
        return $(className("tracks-panel"))
                .$$(className("ngb-track-header"))
                .stream()
                .filter(el -> el.text().contains(trackName))
                .findFirst()
                .orElseThrow(NoSuchElementException::new)
                .parent();
    }

    public SelenideElement getTrack(String trackName) {
        return getTrackByName(trackName)
                .$(className("tether-target"));
    }

    public BrowserPage trackImageCompare(BufferedImage expectedImage,
                                         String trackName,
                                         String resultImage,
                                         double deviation) {
        if (!shootElement(getWebDriver(), getTrack(trackName)).equals(expectedImage,deviation)) {
            sleep(2, SECONDS);
        }
        assertTrue(shootElement(getWebDriver(), getTrack(trackName))
                .equalsWithDiff(expectedImage, format("%s%s", RESULTS_PATH, resultImage), deviation),
                format("Screenshot doesn't match expected with deviation = %s. See screenshot: %s%s",
                        deviation, RESULTS_PATH, resultImage));
        return this;
    }

    public BrowserPage waitTrackDownloaded(SelenideElement trackPanel) {
        int attempt = 0;
        int maxAttempts = 10;
        BufferedImage screen = trackPanel.screenshotAsImage();
        while (shootElement(getWebDriver(), trackPanel).equals(screen,0) && attempt < maxAttempts) {
            sleep(1, SECONDS);
            attempt += 1;
        }
        return this;
    }

    public TrackMenuForm openTrackMenu(String track, String menu) {
        trackMenu(track, menu).click();
        return new TrackMenuForm(this, getMenuID(track, menu));
    }

    private SelenideElement trackMenu(String track, String menu) {
        return getTrackByName(track)
                .$("ngb-track-settings")
                .$$(className("track-menu"))
                .stream()
                .filter(el -> el.text().contains(menu))
                .findFirst()
                .orElseThrow(NoSuchElementException::new)
                .$(className("ngb-track-settings-button"));
    }

    private String getMenuID(String track, String menu) {
        return  trackMenu(track, menu)
                .getAttribute("aria-owns");
    }

    public BrowserPage waitMenuDisappears(String track, String menu) {
        $(byId(getMenuID(track, menu))).should(cssClass("md-leave"));
        return this;
    }

    public BrowserPage clickOnZoomInButtonNumberOfTimes(int numberOfTimes) {
        for (int i = 0; i < numberOfTimes; i++) {
            $x("//button[contains(@ng-click,'zoomIn')]")
                    .shouldBe(visible).shouldBe(enabled).click();
        }
        actions().moveToElement($(className("ngb-track-ruler-container")))
                .perform();
        return this;
    }

    public SelenideElement hoverOverTrackByCoordinates(String track, int coordX, int coordY) {
        getTrack(track).shouldBe(visible).shouldBe(enabled)
                .hover(HoverOptions.withOffset(+ coordX, + coordY));
        return $("table").shouldBe(exist);
    }

    public BrowserPage maximazeBrowser() {
        $$(className("lm_stack")).first()
                .$(className("lm_maximise")).$(className("ngb-maximize")).shouldBe(exist).click();
        return this;
    }



}
