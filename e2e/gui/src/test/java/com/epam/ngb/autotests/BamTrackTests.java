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

public class BamTrackTests  extends AbstractNgbTest {

    private static final String bamTrackTC01 = "bamTrackTC01";
    private static final String bamTrackTC02 = "bamTrackTC02";
    private static final String bamTrack = "CantonS.09-28.trim.dm606.realign.bam";
    private static final String testChromosome = "X";
    private static final String bamTestCoordinate1 = "12584341";
    private static final String bamTestCoordinate2 = "12586062";

    private static final String bamInfo1 =
            "Count 171\nA: 8 (4.68%)\nC: 160 (93.57%)\nG: 0 (0%)\nT: 3 (1.76%)\nN: 0 (0%)\nDEL: 3\nINS: 33";
    private static final String bamInfo2 =
            "Count 341\nA: 166 (48.69%)\nC: 174 (51.03%)\nG: 0 (0%)\nT: 1 (0.3%)\nN: 0 (0%)\nDEL: 0\nINS: 0";

    @Test(invocationCount = 1)
    @TestCase({"TC-BAM_TRACKS_TEST-01"})
    public void bamTrackCoverageTest() throws IOException {
        String track = bamTrack.substring(0, 20);
        DatasetsPage datasetsPage = new DatasetsPage();
        datasetsPage.expandDataset(TEST_DATASET)
                .setTrackCheckbox(bamTrack, true);
        sleep(2, SECONDS);
        BrowserPage browserPage = new BrowserPage();
        browserPage
                .setChromosome(testChromosome)
                .setCoordinates(bamTestCoordinate1)
                .waitTrackDownloaded(browserPage.getTrack(track))
                .clickOnZoomInButtonNumberOfTimes(2)
                .openTrackMenu(track, GENERAL.value)
                .selectOptionWithCheckbox("Show alignments", false)
//        takeScreenshot(browserPage.getTrack(track), bamTrackTC01);
                .trackImageCompare(getExpectedImage(bamTrackTC01),
                        browserPage.getTrack(track), bamTrackTC01, 0.0001)
                .ensure(browserPage.hoverOverTrackByCoordinates(track, 0, 0), text(bamInfo1));
    }

    @Test(invocationCount = 1)
    @TestCase({"TC-BAM_TRACKS_TEST-02"})
    public void bamTrackWithExceededCertainThresholdCoverageTest() throws IOException {
        String track = bamTrack.substring(0, 20);
        DatasetsPage datasetsPage = new DatasetsPage();
        datasetsPage.expandDataset(TEST_DATASET)
                .setTrackCheckbox(bamTrack, true);
        sleep(2, SECONDS);
        BrowserPage browserPage = new BrowserPage();
        browserPage
                .setChromosome(testChromosome)
                .setCoordinates(bamTestCoordinate2)
                .waitTrackDownloaded(browserPage.getTrack(track))
                .clickOnZoomInButtonNumberOfTimes(2)
                .openTrackMenu(track, GENERAL.value)
                .selectOptionWithCheckbox("Show alignments", false)
//                .waitTrackDownloaded(browserPage.getTrack(track))
                .trackImageCompare(getExpectedImage(bamTrackTC02),
                        browserPage.getTrack(track), bamTrackTC02, 0.0001)
                .ensure(browserPage.hoverOverTrackByCoordinates(track, 0, 0), text(bamInfo2));
    }

}
