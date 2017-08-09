package epam.autotests;

import com.epam.web.matcher.testng.Assert;
import epam.autotests.page_objects.enums.VarTableColumns;
import epam.autotests.utils.TestBase;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import static epam.autotests.page_objects.enums.SortingTypes.ASC;
import static epam.autotests.page_objects.enums.SortingTypes.DESC;
import static epam.autotests.page_objects.enums.VarTableColumns.*;
import static epam.autotests.page_objects.enums.Views.RESTORE_DEFAULT;
import static epam.autotests.page_objects.enums.Views.VARIANTS;
import static epam.autotests.page_objects.site.NGB_Site.mainPage;
import static epam.autotests.page_objects.site.NGB_Site.projectPage;


public class CheckingProjectPageTest extends TestBase {

    @Test(priority = 0)
    public void variationPanel() {
        projectPage.closeAllTracks();
        projectPage.openPanel(RESTORE_DEFAULT);
        mainPage.datasetsPanel.select("/SV_Sample1", true);
		mainPage.datasetsPanel.select("/SV_Sample2", true);
        projectPage.openPanel(VARIANTS);
        projectPage.variantsPanel.checkSetOfColumns("Type", "Chr", "Gene", "Position", "Info");
        System.out.println("=== CheckingProjectPageTest.variationPanel(); @Test(priority=0)");
    }

    @Test(priority = 7)
    public void checkingVariantsSorting() {
        projectPage.openPanel(VARIANTS);
        projectPage.variantsPanel.variantsTable.setSorting(TYPE, ASC);
        Assert.isTrue(projectPage.variantsPanel.variantsTable.isColumnSorted(TYPE, false, true));
        projectPage.variantsPanel.variantsTable.setSorting(TYPE, DESC);
        Assert.isTrue(projectPage.variantsPanel.variantsTable.isColumnSorted(TYPE, false, false));
        projectPage.variantsPanel.variantsTable.setSorting(POSITION, ASC);
        Assert.isTrue(projectPage.variantsPanel.variantsTable.isColumnSorted(POSITION, true, true));
        projectPage.variantsPanel.variantsTable.setSorting(POSITION, DESC);
        Assert.isTrue(projectPage.variantsPanel.variantsTable.isColumnSorted(POSITION, true, false));
        projectPage.variantsPanel.variantsTable.setSorting(VarTableColumns.GENE, ASC);
        Assert.isTrue(projectPage.variantsPanel.variantsTable.isColumnSorted(VarTableColumns.GENE, false, true));
        projectPage.variantsPanel.variantsTable.setSorting(VarTableColumns.GENE, DESC);
        Assert.isTrue(projectPage.variantsPanel.variantsTable.isColumnSorted(VarTableColumns.GENE, false, false));
        projectPage.variantsPanel.variantsTable.setSorting(CHR, ASC);
        Assert.isTrue(projectPage.variantsPanel.variantsTable.isColumnSorted(CHR, false, true));
        projectPage.variantsPanel.variantsTable.setSorting(CHR, DESC);
        Assert.isTrue(projectPage.variantsPanel.variantsTable.isColumnSorted(CHR, false, false));
        System.out.println("=== CheckingProjectPageTest.checkingVariantsSorting(); @Test(priority=7)");
    }

    @AfterClass(alwaysRun = true)
    public void resetBrowser() {
        projectPage.closeAllTracks();
        projectPage.openPanel(RESTORE_DEFAULT);
        System.out.println("=== CheckingProjectPageTest.resetBrowser(); @AfterClass(alwaysRun=true)");
        ;
    }
}