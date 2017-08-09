package epam.autotests;

import com.epam.web.matcher.testng.Assert;
import epam.autotests.utils.TestBase;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static epam.autotests.page_objects.enums.FiltersGroups.GENE;
import static epam.autotests.page_objects.enums.FiltersGroups.TYPE_VARIANT;
import static epam.autotests.page_objects.enums.Views.RESTORE_DEFAULT;
import static epam.autotests.page_objects.enums.Views.VARIANTS;
import static epam.autotests.page_objects.site.NGB_Site.mainPage;
import static epam.autotests.page_objects.site.NGB_Site.projectPage;


public class CheckingFilterPanelTest extends TestBase {

    @BeforeClass
    public void preparations() {
        projectPage.closeAllTracks();
        projectPage.openPanel(RESTORE_DEFAULT);
        mainPage.datasetsPanel.select("/SV_Sample1", true);
        mainPage.datasetsPanel.select("/SV_Sample2", true);
        mainPage.datasetsPanel.select("/PIK3CA-E545K-Sample", true);
        projectPage.openPanel(VARIANTS);
        System.out.println("=== CheckingFilterPanelTest.preparation(); @BeforeClass");
    }

    @Test(priority = 1)
    public void checkingTypeOfVarGroup() {
        projectPage.openFilter();
        projectPage.filterPanel.selectFilter(TYPE_VARIANT, "INV");
        projectPage.variantsPanel.variantsTable.checkVarVariationType("INV");
        projectPage.filterPanel.selectFilter(TYPE_VARIANT, "DEL");
        projectPage.variantsPanel.variantsTable.checkVarVariationType("DEL");
        projectPage.filterPanel.selectFilter(TYPE_VARIANT, "BND");
        projectPage.variantsPanel.variantsTable.checkVarVariationType("BND");
        projectPage.filterPanel.selectFilter(TYPE_VARIANT, "INS");
        projectPage.variantsPanel.variantsTable.checkVarVariationType("INS");
        projectPage.filterPanel.selectFilter(TYPE_VARIANT, "DEL","INV");
        projectPage.variantsPanel.variantsTable.checkVarVariationType("DEL","INV");
        projectPage.filterPanel.clearFilterPanel();
        System.out.println("=== CheckingFilterPanelTest.checkingTypeOfVarGroup(); @Test(priority=1)");
    }

    @Test(priority = 3)
    public void checkGeneGroup() {
        projectPage.openFilter();
        projectPage.filterPanel.selectFilter(GENE, "EML4");
        projectPage.variantsPanel.checkVarGenes("EML4");
        projectPage.filterPanel.deleteAllAddedGenes();
        projectPage.filterPanel.selectFilter(GENE, "BCOR");
        projectPage.variantsPanel.checkVarGenes("BCOR");
        projectPage.filterPanel.deleteAllAddedGenes();
        projectPage.filterPanel.selectFilter(GENE, "BCOR", "EML4");
        projectPage.variantsPanel.checkVarGenes("BCOR", "EML4");
        projectPage.filterPanel.clearFilterPanel();
        System.out.println("=== CheckingFilterPanelTest.checkGeneGroup(); @Test(priority=3)");
    }

    @AfterClass
    public void unSelectAllDatasets() {
        projectPage.filterPanel.closeFilter();
        projectPage.openPanel(RESTORE_DEFAULT);
        projectPage.closeAllTracks();
        System.out.println("=== CheckingFilterPanelTest.unSelectAllDatasets(); @AfterMethod");
    }
}