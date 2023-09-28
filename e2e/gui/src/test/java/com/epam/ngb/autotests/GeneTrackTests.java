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
import static com.epam.ngb.autotests.enums.TrackMenus.TRANSCRIPT_VIEW;
import com.epam.ngb.autotests.pages.BrowserPage;
import com.epam.ngb.autotests.pages.DatasetsPage;
import static com.epam.ngb.autotests.utils.AppProperties.GENE_DEVIATION;
import static com.epam.ngb.autotests.utils.AppProperties.TEST_DATASET;
import com.epam.ngb.autotests.utils.TestCase;
import static com.epam.ngb.autotests.utils.Utils.getExpectedImage;
import static com.epam.ngb.autotests.utils.Utils.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import org.testng.annotations.Test;

import java.io.IOException;

public class GeneTrackTests  extends AbstractNgbTest {

    private static final String bamTrack = "CantonS.09-28.trim.dm606.realign.bam";
    private static final String geneTrackTC01_1 = "geneTrackTC01_1";
    private static final String geneTrackTC01_2 = "geneTrackTC01_2";
    private static final String GENE_TRACK = "GENE";
    private static final String testChromosome = "X";
    private static final String geneTestCoordinates = "124135 - 126948";
    private static final String geneInfo1 = "gene CG17636\nStart 124370\nEnd 126714\n" +
            "Strand NEGATIVE\nScore -1\ngene_symbol CG17636\ngene_id FBgn0025837";
    private static final String geneInfo2 = "CG17636-RB";
    private static final String geneInfo3 = "CG17636-RC";
    private static final String geneInfo4 = "CG17636-RA";

    @Test(invocationCount = 1)
    @TestCase({"TC-GENE_TRACKS_TEST-01"})
    public void geneTrackTest() throws IOException {
        DatasetsPage datasetsPage = new DatasetsPage();
        datasetsPage.expandDataset(TEST_DATASET)
                .setTrackCheckbox(bamTrack, true)
                .sleep(1, SECONDS)
                .setTrackCheckbox(bamTrack, false);
        sleep(2, SECONDS);
        BrowserPage browserPage = new BrowserPage();
        browserPage
                .setChromosome(testChromosome)
                .setCoordinates(geneTestCoordinates)
                .waitTrackDownloaded(browserPage.getTrack(GENE_TRACK))
                .trackImageCompare(getExpectedImage(geneTrackTC01_1),
                        browserPage.getTrack(GENE_TRACK), geneTrackTC01_1, GENE_DEVIATION);
        browserPage
                .openTrackMenu(GENE_TRACK, GENERAL.value)
                .selectOptionWithAdditionalMenu("Resize")
                .setTrackHeight("400")
                .saveIfNeeded()
                .openTrackMenu(GENE_TRACK, TRANSCRIPT_VIEW.value)
                .selectOptionWithCheckbox("Expanded", true)
                .trackImageCompare(getExpectedImage(geneTrackTC01_2),
                        browserPage.getTrack(GENE_TRACK), geneTrackTC01_2, GENE_DEVIATION)
                .ensure(browserPage.hoverOverTrackByCoordinates(GENE_TRACK, 0, -90), text(geneInfo4))
                .ensure(browserPage.hoverOverTrackByCoordinates(GENE_TRACK, 0, -120), text(geneInfo3))
                .ensure(browserPage.hoverOverTrackByCoordinates(GENE_TRACK, 0, -140), text(geneInfo1))
                .ensure(browserPage.hoverOverTrackByCoordinates(GENE_TRACK, 0, -150), text(geneInfo2));
    }
}
