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

import static com.codeborne.selenide.Condition.text;
import static com.epam.ngb.autotests.enums.Primitive.TITLE;
import com.epam.ngb.autotests.pages.BrowserPage;
import com.epam.ngb.autotests.pages.DatasetsPage;
import static com.epam.ngb.autotests.utils.AppProperties.TEST_DATASET;
import com.epam.ngb.autotests.utils.TestCase;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import org.testng.annotations.Test;

public class DatasetsTests extends AbstractNgbTest {

    private static final String[][] attributes = {
            {"key1", "value1"},
            {"key2", "value2"},
            {"new_key1", "new_value1"}};
    private static final String bamTrack = "CantonS.09-28.trim.dm606.realign.bam";

    @Test
    @TestCase({"TC-DATASETS_TEST-01"})
    public void addAttributesToTrack() {
        String attribute = format("%s: %s", attributes[0][0], attributes[0][1]);
        DatasetsPage datasetsPage = new DatasetsPage();
        datasetsPage
                .expandDataset(TEST_DATASET)
                .openMetadataForm(bamTrack)
                .ensure(TITLE, text(bamTrack))
                .checkMetadataFormEmpty()
                .addAttribute(attributes[0][0], attributes[0][1], 1)
                .save()
                .expandDataset(TEST_DATASET)
                .checkTrackTooltipContains(bamTrack, true, attribute)
                .setTrackCheckbox(bamTrack, true)
                .sleep(2, SECONDS);
        BrowserPage browserPage = new BrowserPage();
        browserPage
                .checkTrackAttributes(bamTrack, true, attribute);
    }

    @Test (dependsOnMethods = "addAttributesToTrack")
    @TestCase({"TC-DATASETS_TEST-02"})
    public void editAttributesToTrack() {
        String[] attribute = {
                format("%s: %s", attributes[2][0], attributes[2][1]),
                format("%s: %s", attributes[1][0], attributes[1][1])
        };
        DatasetsPage datasetsPage = new DatasetsPage();
        datasetsPage
                .expandDataset(TEST_DATASET)
                .openMetadataForm(bamTrack)
                .updateAttribute(attributes[0][0], attributes[2][0], attributes[2][1])
                .addAttribute(attributes[1][0], attributes[1][1], 2)
                .save()
                .expandDataset(TEST_DATASET)
                .checkTrackTooltipContains(bamTrack, true, attribute)
                .setTrackCheckbox(bamTrack, true)
                .sleep(2, SECONDS);
        BrowserPage browserPage = new BrowserPage();
        browserPage
                .checkTrackAttributes(bamTrack, true, attribute);
    }

    @Test (dependsOnMethods = "editAttributesToTrack")
    @TestCase({"TC-DATASETS_TEST-03"})
    public void deleteAttributesToTrack() {
        String[] attribute = {
                format("%s: %s", attributes[2][0], attributes[2][1]),
                format("%s: %s", attributes[1][0], attributes[1][1])
        };
        DatasetsPage datasetsPage = new DatasetsPage();
        datasetsPage
                .expandDataset(TEST_DATASET)
                .openMetadataForm(bamTrack)
                .deleteAttribute(attributes[2][0])
                .deleteAttribute(attributes[1][0])
                .save()
                .expandDataset(TEST_DATASET)
                .checkTrackTooltipContains(bamTrack, false, attribute)
                .setTrackCheckbox(bamTrack, true)
                .sleep(2, SECONDS);
        BrowserPage browserPage = new BrowserPage();
        browserPage
                .checkTrackAttributes(bamTrack, false, attribute);
    }

}
