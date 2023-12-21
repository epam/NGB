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

import com.epam.ngb.autotests.enums.Colors;
import static com.epam.ngb.autotests.enums.Colors.AZURE;
import static com.epam.ngb.autotests.enums.Colors.BEIGE;
import static com.epam.ngb.autotests.enums.Colors.BLANK;
import static com.epam.ngb.autotests.enums.Colors.BLUE;
import static com.epam.ngb.autotests.enums.Colors.BROWN;
import static com.epam.ngb.autotests.enums.Colors.DARK_SATURATED_RED;
import static com.epam.ngb.autotests.enums.Colors.GREEN;
import static com.epam.ngb.autotests.enums.Colors.LIGHT_BLUE;
import static com.epam.ngb.autotests.enums.Colors.LIGHT_GREEN;
import static com.epam.ngb.autotests.enums.Colors.LIGHT_PINK;
import static com.epam.ngb.autotests.enums.Colors.ORANGE;
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

public class VcfHighlightTests extends AbstractNgbTest {
    private static final String variantsTC01_1 = "variantsTC01_1";
    private static final String variantsTC01_2 = "variantsTC01_2";
    private static final String BUBBLES_PROFILE = "Bubbles";
    private static final String COMPLEX_CONDITIONS_PROFILE = "Complex Conditions";
    private static final String SIMPLE_CONDITIONS_PROFILE = "Simple Conditions";
    private static final String DIFFERENT_CONDITIONS_RED_PROFILE = "Different conditions (red highlight)";
    private static final String DIFFERENT_CONDITIONS_GREEN_PROFILE = "Different conditions (green highlight)";
    private static final String STRUCTURAL_VARIATIONS = "Structural variations";
    private static final String vcfTrack1 = "agnX1.09-28.trim.dm606.realign.vcf";
    private static final String vcfTrack2 = "agnts3.09-28.trim.dm606.realign.vcf";
    private static final String dataset1 = "SV_Sample1";
    private static final String vcfTrack3 = "sample_1-lumpy.vcf";
    private static final String[] testPositionTC01 = {"12585001", "12585008", "12585016"};
    private static final String vcfTestCoordinates1 = "12584436 - 12585579";

    @DataProvider(name = "complexConditions")
    public static Object[][] complexConditions() {
        return new Object[][] {
                {"12584271", AZURE, "variantsTC02_1", "ReadPosRankSum", "1.471", "DP", "93"},
                {"12591627", LIGHT_GREEN, "variantsTC02_2", "DP", "435", "SOR", "1.461"},
                {"12591629", PINK, "variantsTC02_3", "DP", "386", "SOR", "1.227"},
                {"12587867", DARK_SATURATED_RED, "variantsTC02_4", "DP", "572", "MQ", "92.2"},
                {"12586790", BROWN, "variantsTC02_5", "FS", "453.737", "FS", "453.737"},
                {"12590931", BROWN, "variantsTC02_6", "QD", "36.89", "QD", "36.89"}
        };
    }

    @DataProvider(name = "simpleConditions")
    public static Object[][] simpleConditions() {
        return new Object[][] {
                {"12586950", RED, "variantsTC03_1", "DP", "384"},
                {"12586560", YELLOW, "variantsTC03_2", "AC", "2"},
                {"12586790", GREEN, "variantsTC03_3", "FS", "453.737"},
                {"12585943", LIGHT_BLUE, "variantsTC03_4", "MQ", "91.95"},
                {"12587867", LIGHT_BLUE, "variantsTC03_5", "MQ", "92.2"},
                {"12589324", BLUE, "variantsTC03_6", "ReadPosRankSum", "-1.651"},
                {"12589261", LIGHT_PINK, "variantsTC03_7", "QD", "0.32"},
                {"12591635", LIGHT_PINK, "variantsTC03_8", "QD", "0.26"},
                {"12589600", ORANGE, "variantsTC03_9", "DP", "114"},
                {"12590846", ORANGE, "variantsTC03_10", "DP", "137"},
                {"12585171", BLANK, "variantsTC03_11", "DP", "206"},
                {"12585001", BEIGE, "variantsTC03_12", "DP", "216"}
        };
    }

    @DataProvider(name = "structuralVariations")
    public static Object[][] structuralVariations() {
        return new Object[][]{
                {"181050143", RED, "variantsTC06_1", "CIPOS", "-6", "2"},
                {"48459903", LIGHT_BLUE, "variantsTC06_2", "EVENT", "124"},
                {"51295112", BLUE, "variantsTC06_3", "EVENT", "30"},
                {"117314770", LIGHT_PINK, "variantsTC06_4", "EVENT", "34"},
                {"31200612", ORANGE, "variantsTC06_5", "EVENT", "96"},
                {"31226616", YELLOW, "variantsTC06_6", "PE", "5"},
                {"31182626", BEIGE, "variantsTC06_7", "PE", "4"}
        };
    }

    @Test
    @TestCase ({"TC-VCF_HIGHLIGHT-01"})
    public void highlightBubblesDependingOnHighlightedVariations() throws IOException {
        String track = vcfTrack1.substring(0, 20);

        openTrack(TEST_DATASET, vcfTrack1);
        setProfile(BUBBLES_PROFILE);
        new TabsSelectionPanel()
                .selectPanel(VARIANTS);
        new VariantsPanel()
                .checkBackgroundColorRowByPosition(testPositionTC01[0], RED)
                .checkBackgroundColorRowByPosition(testPositionTC01[1], GREEN)
                .checkBackgroundColorRowByPosition(testPositionTC01[2], LIGHT_BLUE)
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
    @TestCase ({"TC-VCF_HIGHLIGHT-02"})
    public void highlightVcfWithComplexConditions(String position,
                                                  Colors color,
                                                  String fileName,
                                                  String property1,
                                                  String value1,
                                                  String property2,
                                                  String value2) throws IOException {
        String track = vcfTrack2.substring(0, 20);

        openTrack(TEST_DATASET, vcfTrack2);
        setProfile(COMPLEX_CONDITIONS_PROFILE);
        openFiltersPanel();
        new VariantsPanel()
                .filterVariantsBy("Position", position)
                .checkBackgroundColorRowByPosition(position, color)
                .openVariantInfoByPosition(position)
                .checkPropertyValue(property1,value1)
                .checkPropertyValue(property2,value2)
                .close()
                .openVariantByPosition(position);
        new BrowserPanel()
                .waitTrackDownloaded(track)
                .trackImageCompare(getExpectedImage(fileName),
                        track, fileName, VCF_DEVIATION);
    }

    @Test(dataProvider = "simpleConditions")
    @TestCase ({"TC-VCF_HIGHLIGHT-03"})
    public void highlightVcfWithSimpleConditions(String position,
                                                 Colors color,
                                                 String fileName,
                                                 String property,
                                                 String ... values) throws IOException {
        String track = vcfTrack2.substring(0, 20);

        openTrack(TEST_DATASET, vcfTrack2);
        setProfile(SIMPLE_CONDITIONS_PROFILE);
        openFiltersPanel();
        new VariantsPanel()
                .filterVariantsBy("Position", position)
                .checkBackgroundColorRowByPosition(position, color)
                .openVariantInfoByPosition(position)
                .checkPropertyValue(property,values)
                .close()
                .openVariantByPosition(position);
        BrowserPanel browserPanel = new BrowserPanel()
                .waitTrackDownloaded(track);
//                .trackImageCompare(getExpectedImage(fileName),
//                        track, fileName, VCF_DEVIATION);
        takeScreenshot(browserPanel.getTrack(track), fileName);
    }

    @Test
    @TestCase ({"TC-VCF_HIGHLIGHT-04"})
    public void redHighlightTheSameVariationThatMatchesToDifferentConditions() throws IOException {
        String position = "12585943";
        String track = vcfTrack2.substring(0, 20);

        openTrack(TEST_DATASET, vcfTrack2);
        setProfile(DIFFERENT_CONDITIONS_RED_PROFILE);
        openFiltersPanel();
        new VariantsPanel()
                .filterVariantsBy("Position", position)
                .checkBackgroundColorRowByPosition(position, RED)
                .openVariantByPosition(position);
        new BrowserPanel()
                .waitTrackDownloaded(track)
                .trackImageCompare(getExpectedImage("variantsTC04_1"),
                        track, "variantsTC04_1", VCF_DEVIATION);
    }

    @Test
    @TestCase ({"TC-VCF_HIGHLIGHT-05"})
    public void greenHighlightTheSameVariationThatMatchesToDifferentConditions() throws IOException {
        String position = "12585943";
        String track = vcfTrack2.substring(0, 20);

        openTrack(TEST_DATASET, vcfTrack2);
        setProfile(DIFFERENT_CONDITIONS_GREEN_PROFILE);
        openFiltersPanel();
        new VariantsPanel()
                .filterVariantsBy("Position", position)
                .checkBackgroundColorRowByPosition(position, GREEN)
                .openVariantByPosition(position);
        new BrowserPanel()
                .waitTrackDownloaded(track)
                .trackImageCompare(getExpectedImage("variantsTC05_1"),
                        track, "variantsTC05_1", VCF_DEVIATION);
    }
    @Test(dataProvider = "structuralVariations")
    @TestCase ({"TC-VCF_HIGHLIGHT-06"})
    public void structuralVariationsBND(String position,
                                        Colors color,
                                        String fileName,
                                        String property,
                                        String ... values) throws IOException {
        String track = (vcfTrack3.length() >= 20) ? vcfTrack3.substring(0, 20) : vcfTrack3;

        openTrack(dataset1, vcfTrack3);
        setProfile(STRUCTURAL_VARIATIONS);
        openFiltersPanel();
        BrowserPanel browserPanel = new VariantsPanel()
                .filterVariantsBy("Position", position)
                .checkBackgroundColorRowByPosition(position, color)
                .openVariantInfoByPosition(position)
                .checkPropertyValue(property,values)
                .close()
                .openVariantByPosition(position);
        new BrowserPanel()
                .waitTrackDownloaded(track);
//                .trackImageCompare(getExpectedImage(fileName),
//                        track, fileName, VCF_DEVIATION);
        takeScreenshot(browserPanel.getTrack(track), fileName);
    }

    private void openTrack(String dataset, String track) {
        new DatasetsPanel()
                .expandDataset(dataset)
                .setTrackCheckbox(track, true)
                .sleep(2, SECONDS);
    }

    private void setProfile(String profile) {
        new NavigationPanel()
                .settings()
                .openTab(VCF)
                .checkVariantsHighlighted(true)
                .selectProfile(profile)
                .save()
                .toolWindows()
                .selectMainMenuItem(VARIANTS, true);
    }

    private void openFiltersPanel() {
        new TabsSelectionPanel()
                .selectPanel(VARIANTS)
                .openNGBVariantsTableColumn()
                .selectVariantsTableColumnFormItem("Show filters", true);
    }
}
