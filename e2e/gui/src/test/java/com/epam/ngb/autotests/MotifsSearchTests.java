package com.epam.ngb.autotests;

import static com.codeborne.selenide.Condition.enabled;
import static com.epam.ngb.autotests.enums.Primitive.SEARCH;
import com.epam.ngb.autotests.pages.BrowserPanel;
import com.epam.ngb.autotests.pages.DatasetsPanel;
import com.epam.ngb.autotests.utils.TestCase;
import static java.util.concurrent.TimeUnit.SECONDS;
import org.testng.annotations.Test;

public class MotifsSearchTests extends AbstractNgbTest {
    private static final String dataset = "SV_Sample1";
    private static final String vcfTrack = "sample_1-lumpy.vcf";
    private static final String REFERENCE = "REFERENCE";
    private static final String testSequence = "gggttcatgaggaagggcaggaggagggtgtgggatggtg";

    @Test
    @TestCase({"TC-Motifs-01"})
    public void motifsSearchForWholeReference() {
        new DatasetsPanel()
                .expandDataset(dataset)
                .setTrackCheckbox(vcfTrack, true)
                .sleep(2, SECONDS);
        new BrowserPanel()
                .setChromosome("1")
                .waitTrackDownloaded(REFERENCE)
                .openSearchMotifsForm(REFERENCE)
                .markSearchWholeReferenceCheckbox()
                .setPattern(testSequence)
                .ensure(SEARCH, enabled)
                .search()
                .waitUntilTableAppears()
                .checkRowsNumber(8);

    }
}
