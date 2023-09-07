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
import com.epam.ngb.autotests.pages.BrowserPage;
import com.epam.ngb.autotests.pages.DatasetsPage;
import static com.epam.ngb.autotests.utils.AppProperties.TEST_DATASET;
import com.epam.ngb.autotests.utils.TestCase;
import static com.epam.ngb.autotests.utils.Utils.sleep;
import static com.epam.ngb.autotests.utils.Utils.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import org.testng.annotations.Test;

import java.io.IOException;

public class ReferenceTrackTests  extends AbstractNgbTest {

    private static final String referenceTrackTC01 = "referenceTrackTC01";
    private static final String referenceTrackTC02_1 = "referenceTrackTC02_1";
    private static final String referenceTrackTC02_2 = "referenceTrackTC02_2";
    private static final String bamTrack = "CantonS.09-28.trim.dm606.realign.bam";
    private static final String REFERENCE = "REFERENCE";
    private static final String testChromosome = "X";
    private static final String referenceTestCoordinates1 = "12585901 - 12585934";

    @Test(invocationCount = 1)
    @TestCase({"TC-REFERENCE_TRACKS_TEST-01"})
    public void referenceTrackTest() throws IOException {
        DatasetsPage datasetsPage = new DatasetsPage();
        datasetsPage.expandDataset(TEST_DATASET)
                .setTrackCheckbox(bamTrack, true)
                .sleep(1, SECONDS)
                .setTrackCheckbox(bamTrack, false);
        sleep(2, SECONDS);
        BrowserPage browserPage = new BrowserPage();
        browserPage
                .setChromosome(testChromosome)
                .setCoordinates(referenceTestCoordinates1)
                .waitTrackDownloaded(browserPage.getTrack(REFERENCE))
                .trackImageCompare(getExpectedImage(referenceTrackTC01),
                        browserPage.getTrack(REFERENCE), referenceTrackTC01, 0)
                .openTrackMenu(REFERENCE, GENERAL.value)
                .selectOptionWithCheckbox("Show translation", true)
                .trackImageCompare(getExpectedImage(referenceTrackTC02_1),
                        browserPage.getTrack(REFERENCE), referenceTrackTC02_1, 0)
                .openTrackMenu(REFERENCE, GENERAL.value)
                .selectOptionWithCheckbox("Show reverse strand", true)
                .trackImageCompare(getExpectedImage(referenceTrackTC02_2),
                        browserPage.getTrack(REFERENCE), referenceTrackTC02_2, 0);
    }

}
