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

import static com.epam.ngb.autotests.enums.TrackMenus.GENERAL;
import com.epam.ngb.autotests.pages.BrowserPanel;
import com.epam.ngb.autotests.pages.DatasetsPanel;
import static com.epam.ngb.autotests.utils.AppProperties.REFERENCE_DEVIATION;
import static com.epam.ngb.autotests.utils.AppProperties.TEST_DATASET;
import com.epam.ngb.autotests.utils.TestCase;
import static com.epam.ngb.autotests.utils.Utils.sleep;
import static com.epam.ngb.autotests.utils.Utils.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import org.testng.annotations.Test;

import java.io.IOException;

public class ReferenceTrackTests extends AbstractNgbTest {

    private static final String referenceTrackTC01_1 = "referenceTrackTC01_1";
    private static final String referenceTrackTC01_2 = "referenceTrackTC01_2";
    private static final String referenceTrackTC02 = "referenceTrackTC02";
    private static final String referenceTrackTC03 = "referenceTrackTC03";
    private static final String bamTrack = "CantonS.09-28.trim.dm606.realign.bam";
    private static final String REFERENCE = "REFERENCE";
    private static final String testChromosome = "X";
    private static final String referenceTestCoordinates1 = "12585901 - 12585934";

    @Test
    @TestCase({"TC-REFERENCE_TRACKS_TEST-01"})
    public void referenceTrackTestShowForwardStrand() throws IOException {
        DatasetsPanel datasetsPanel = new DatasetsPanel();
        datasetsPanel.expandDataset(TEST_DATASET)
                .setTrackCheckbox(bamTrack, true)
                .sleep(1, SECONDS)
                .setTrackCheckbox(bamTrack, false);
        sleep(2, SECONDS);
        BrowserPanel browserPanel = new BrowserPanel();
        browserPanel
                .setChromosome(testChromosome)
                .waitTrackDownloaded(REFERENCE)
                .trackImageCompare(getExpectedImage(referenceTrackTC01_1),
                        REFERENCE, referenceTrackTC01_1, REFERENCE_DEVIATION)
                .setCoordinates(referenceTestCoordinates1)
                .waitTrackDownloaded(REFERENCE)
                .trackImageCompare(getExpectedImage(referenceTrackTC01_2),
                        REFERENCE, referenceTrackTC01_2, REFERENCE_DEVIATION);
    }

    @Test
    @TestCase({"TC-REFERENCE_TRACKS_TEST-02"})
    public void referenceTrackTestShowTranslation() throws IOException {
        new DatasetsPanel()
                .expandDataset(TEST_DATASET)
                .setTrackCheckbox(bamTrack, true)
                .sleep(1, SECONDS)
                .setTrackCheckbox(bamTrack, false);
        sleep(2, SECONDS);
        new BrowserPanel()
                .setChromosome(testChromosome)
                .setCoordinates(referenceTestCoordinates1)
                .waitTrackDownloaded(REFERENCE)
                .openTrackMenu(REFERENCE, GENERAL.value)
                .selectOptionWithCheckbox("Show translation", true)
                .trackImageCompare(getExpectedImage(referenceTrackTC02),
                        REFERENCE, referenceTrackTC02, REFERENCE_DEVIATION);
    }

    @Test(invocationCount = 1)
    @TestCase({"TC-REFERENCE_TRACKS_TEST-03"})
    public void referenceTrackTestShowReverseStrand() throws IOException {
        new DatasetsPanel()
                .expandDataset(TEST_DATASET)
                .setTrackCheckbox(bamTrack, true)
                .sleep(1, SECONDS)
                .setTrackCheckbox(bamTrack, false);
        sleep(2, SECONDS);
        new BrowserPanel()
                .setChromosome(testChromosome)
                .setCoordinates(referenceTestCoordinates1)
                .waitTrackDownloaded(REFERENCE)
                .openTrackMenu(REFERENCE, GENERAL.value)
                .selectOptionWithCheckbox("Show reverse strand", true)
                .trackImageCompare(getExpectedImage(referenceTrackTC03),
                        REFERENCE, referenceTrackTC03, REFERENCE_DEVIATION);
    }
}
