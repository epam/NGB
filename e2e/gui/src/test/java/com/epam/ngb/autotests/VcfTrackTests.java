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
import static com.epam.ngb.autotests.enums.TrackMenus.VARIANTS_VIEW;
import com.epam.ngb.autotests.pages.BrowserPage;
import com.epam.ngb.autotests.pages.DatasetsPage;
import static com.epam.ngb.autotests.utils.AppProperties.TEST_DATASET;
import com.epam.ngb.autotests.utils.TestCase;
import static com.epam.ngb.autotests.utils.Utils.sleep;
import static com.epam.ngb.autotests.utils.Utils.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import org.testng.annotations.Test;

import java.io.IOException;

public class VcfTrackTests  extends AbstractNgbTest {

    private static final String vcfTrackTC01_1 = "vcfTrackTC01_1";
    private static final String vcfTrackTC01_2 = "vcfTrackTC01_2";
    private static final String vcfTrack = "agnX1.09-28.trim.dm606.realign.vcf";
    private static final String testChromosome = "X";
    private static final String vcfTestCoordinates1 = "12585814 - 12589558";
    private static final String vcfTestCoordinate1 = "12591700";
    private static final String vcfTestCoordinate2 = "12590807";

    @Test(invocationCount = 1)
    @TestCase({"TC-VCF_TRACKS_TEST-01"})
    public void vcfTrackTest() throws IOException {
        String track = vcfTrack.substring(0, 20);
        DatasetsPage datasetsPage = new DatasetsPage();
        datasetsPage.expandDataset(TEST_DATASET)
                .setTrackCheckbox(vcfTrack, true);
        sleep(2, SECONDS);
        BrowserPage browserPage = new BrowserPage();
        browserPage
                .setChromosome(testChromosome)
                .setCoordinates(vcfTestCoordinates1)
                .waitTrackDownloaded(browserPage.getTrack(track))
                .trackImageCompare(getExpectedImage(vcfTrackTC01_1),
                        browserPage.getTrack(track), vcfTrackTC01_1, 0)
                .openTrackMenu(track, GENERAL.value)
                .selectOptionWithAdditionalMenu("Resize")
                .setTrackHeight("200")
                .saveIfNeeded()
                .openTrackMenu(track, VARIANTS_VIEW.value)
                .selectOptionWithCheckbox("Expanded", true)
                .waitTrackDownloaded(browserPage.getTrack(track))
                .trackImageCompare(getExpectedImage(vcfTrackTC01_2),
                        browserPage.getTrack(track), vcfTrackTC01_2, 0)
        ;


//        takeScreenshot(browserPage.getTrack(track), vcfTrackTC01_2);

//                .setCoordinates(vcfTestCoordinate1)
//                .waitTrackDownloaded(browserPage.getTrack(track))
//                .trackImageCompare(getExpectedImage("vcfTrackTest2"),
//                        browserPage.getTrack(track), "vcfTrackTest2", 0)
//                .setCoordinates(vcfTestCoordinate2)
//                .waitTrackDownloaded(browserPage.getTrack(track))
//                .trackImageCompare(getExpectedImage("vcfTrackTest3"),
//                        browserPage.getTrack(track), "vcfTrackTest3", 0);
    }

}
