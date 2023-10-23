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
package com.epam.ngb.autotests;

import static com.epam.ngb.autotests.enums.Colors.BLUE;
import static com.epam.ngb.autotests.enums.Colors.BROWN;
import static com.epam.ngb.autotests.enums.Colors.GREEN;
import static com.epam.ngb.autotests.enums.Colors.LIGHTBLUE;
import static com.epam.ngb.autotests.enums.Colors.LIGHTPINK;
import static com.epam.ngb.autotests.enums.Colors.PINK;
import static com.epam.ngb.autotests.enums.Colors.RED;
import static com.epam.ngb.autotests.enums.Colors.YELLOW;
import static com.epam.ngb.autotests.menus.ViewsMenuForm.ViewsMenuItems.VARIANTS;
import static com.epam.ngb.autotests.menus.SettingsForm.SettingsTab.VCF;
import com.epam.ngb.autotests.pages.BrowserPanel;
import com.epam.ngb.autotests.pages.DatasetsPanel;
import com.epam.ngb.autotests.pages.NavigationPanel;
import com.epam.ngb.autotests.pages.TabsSelectionPanel;
import com.epam.ngb.autotests.pages.VariantsPanel;
import static com.epam.ngb.autotests.utils.AppProperties.TEST_DATASET;
import static com.epam.ngb.autotests.utils.AppProperties.VCF_DEVIATION;
import com.epam.ngb.autotests.utils.TestCase;
import static com.epam.ngb.autotests.utils.Utils.getExpectedImage;
import static com.epam.ngb.autotests.utils.Utils.takeScreenshot;
import static java.util.concurrent.TimeUnit.SECONDS;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;

public class VariantsTests extends AbstractNgbTest {

    private static final String variantsTC01_1 = "variantsTC01_1";
    private static final String variantsTC01_2 = "variantsTC01_2";
    private static final String BUBBLES_PROFILE = "Bubbles";
    private static final String COMPLEX_CONDITIONS_PROFILE = "Complex Conditions";
    private static final String SIMPLE_CONDITIONS_PROFILE = "Simple Conditions";
    private static final String vcfTrack1 = "agnX1.09-28.trim.dm606.realign.vcf";
    private static final String vcfTrack2 = "agnts3.09-28.trim.dm606.realign.vcf";
    private static final String[] testPositionTC01 = {"12585001", "12585008", "12585016"};
    private static final String vcfTestCoordinates1 = "12584436 - 12585579";

    @DataProvider(name = "complexConditions")
    public static Object[][] complexConditions() {
        return new Object[][] {
                {"12584271", BLUE.value, "variantsTC02_1"},
                {"12591627", "rgb(20, 145, 80)", "variantsTC02_2"},
                {"12591629", PINK.value, "variantsTC02_3"},
                {"12587867", "rgb(249, 41, 0)", "variantsTC02_4"},
                {"12586790", BROWN.value, "variantsTC02_5"},
                {"12590931", BROWN.value, "variantsTC02_6"}
        };
    }

    @DataProvider(name = "simpleConditions")
    public static Object[][] simpleConditions() {
        return new Object[][] {
                {"12586950", RED.value, "variantsTC03_1"},
                {"12586560", YELLOW, "variantsTC03_2"},
                {"12586790", GREEN.value, "variantsTC03_3"},
                {"12585943", LIGHTBLUE.value, "variantsTC03_4"},
                {"12587867", LIGHTBLUE.value, "variantsTC03_5"},
                {"12589324", "rgb(0, 0, 255)", "variantsTC03_6"},
                {"12589261", LIGHTPINK.value, "variantsTC03_7"},
                {"12591635", LIGHTPINK.value, "variantsTC03_8"},
                {"12589600", "rgb(255, 165, 0)", "variantsTC03_9"},
                {"12590846", "rgb(255, 165, 0)", "variantsTC03_10"}
        };
    }

    @Test
    @TestCase ({"TC-VARIANTS_TEST-01"})
    public void highlightBubblesDependingOnHighlightedVariations() throws IOException {
        String track = vcfTrack1.substring(0, 20);
        new DatasetsPanel()
                .expandDataset(TEST_DATASET)
                .setTrackCheckbox(vcfTrack1, true)
                .sleep(2, SECONDS);
        new NavigationPanel()
                .settings()
                .openTab(VCF)
                .checkVariantsHighlighted(true)
                .selectProfile(BUBBLES_PROFILE)
                .save()
                .toolWindows()
                .selectMainMenuItem(VARIANTS, true);
        new TabsSelectionPanel()
                .selectPanel(VARIANTS);
        new VariantsPanel()
                .checkBackgroundColorRowByPosition(testPositionTC01[0], RED.value)
                .checkBackgroundColorRowByPosition(testPositionTC01[1], GREEN.value)
                .checkBackgroundColorRowByPosition(testPositionTC01[2], LIGHTBLUE.value)
                .openVariantByPosition(testPositionTC01[0])
                .waitTrackDownloaded(track)
                .trackImageCompare(getExpectedImage(variantsTC01_1),
                        track, variantsTC01_1, VCF_DEVIATION)
                .setCoordinates(vcfTestCoordinates1)
                .waitTrackDownloaded(track)
                .trackImageCompare(getExpectedImage(variantsTC01_2),
                        track, variantsTC01_2, VCF_DEVIATION);
    }

    @Test(dataProvider = "complexConditions")
    @TestCase ({"TC-VARIANTS_TEST-02"})
    public void highlightVcfWithComplexConditions(String position, String color, String fileName) throws IOException {
        String track = vcfTrack2.substring(0, 20);
        new DatasetsPanel()
                .expandDataset(TEST_DATASET)
                .setTrackCheckbox(vcfTrack2, true)
                .sleep(2, SECONDS);
        new NavigationPanel()
                .settings()
                .openTab(VCF)
                .checkVariantsHighlighted(true)
                .selectProfile(COMPLEX_CONDITIONS_PROFILE)
                .save()
                .toolWindows()
                .selectMainMenuItem(VARIANTS, true);
        new TabsSelectionPanel()
                .selectPanel(VARIANTS)
                .openNGBVariantsTableColumn()
                .selectVariantsTableColumnFormItem("Show filters", true);
        new VariantsPanel()
                .filterVariantsBy("Position", position)
                .checkBackgroundColorRowByPosition(position, color)
                .openVariantByPosition(position);
        BrowserPanel browserPanel = new BrowserPanel();
        browserPanel.waitTrackDownloaded(track)
                .trackImageCompare(getExpectedImage(fileName),
                        track, fileName, VCF_DEVIATION);
//        takeScreenshot(browserPanel.getTrack(track), fileName);]
    }

    @Test(dataProvider = "simpleConditions")
    @TestCase ({"TC-VARIANTS_TEST-03"})
    public void highlightVcfWithSimpleConditions(String position, String color, String fileName) throws IOException {
        String track = vcfTrack2.substring(0, 20);
        new DatasetsPanel()
                .expandDataset(TEST_DATASET)
                .setTrackCheckbox(vcfTrack2, true)
                .sleep(2, SECONDS);
        new NavigationPanel()
                .settings()
                .openTab(VCF)
                .checkVariantsHighlighted(true)
                .selectProfile(SIMPLE_CONDITIONS_PROFILE)
                .save()
                .toolWindows()
                .selectMainMenuItem(VARIANTS, true);
        new TabsSelectionPanel()
                .selectPanel(VARIANTS)
                .openNGBVariantsTableColumn()
                .selectVariantsTableColumnFormItem("Show filters", true);
        new VariantsPanel()
                .filterVariantsBy("Position", position)
                .checkBackgroundColorRowByPosition(position, color)
                .openVariantByPosition(position);
        BrowserPanel browserPanel = new BrowserPanel();
        browserPanel.waitTrackDownloaded(track)
                .trackImageCompare(getExpectedImage(fileName),
                        track, fileName, VCF_DEVIATION);
//        takeScreenshot(browserPanel.getTrack(track), fileName);
    }

}
