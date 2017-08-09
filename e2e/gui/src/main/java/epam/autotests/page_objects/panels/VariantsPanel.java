package epam.autotests.page_objects.panels;

import com.epam.commons.Timer;
import com.epam.jdi.uitests.web.selenium.elements.common.Button;
import com.epam.web.matcher.junit.Assert;
import epam.autotests.page_objects.general.Panel;
import epam.autotests.page_objects.general.PropertyVCF;
import epam.autotests.page_objects.sections.CustomTable;
import epam.autotests.page_objects.sections.GridPanel;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.testng.asserts.SoftAssert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static epam.autotests.page_objects.site.NGB_Site.compareTwoStringLists;
import static epam.autotests.page_objects.site.NGB_Site.variationInfoWindow;
import static org.testng.Assert.assertTrue;

/**
 * Created by Vsevolod_Adrianov on 8/8/2016.
 * <br>
 * Refactored by Aksenov Oleg. October, 2016
 */

public class VariantsPanel extends Panel {
    @FindBy(css = ".md-transition-in")
    private PropertyVCF VCFPanel;

    @FindBy(xpath = ".//ngb-columns//button")
    private Button addColumns;

    @FindBy(xpath = ".//div[@role='grid']")
    public GridPanel gridPanel;

    @FindBy(css = ".ui-grid-contents-wrapper>.ui-grid-render-container-body")
    public CustomTable variantsTable;

    public int getNumRows() {
        return gridPanel.getNumRows();
    }

    public void clickCell(int r, int c) {
        gridPanel.cell(r, c).click();
    }

    public String valueCell(int r, int c) {
        return gridPanel.cell(r, c).getText();
    }

    public void sortColumn(String ColName) {
        gridPanel.sorting(ColName);
    }


    public void visualizerVCF(int row) {
        gridPanel.cell(row, 4).click();
        VCFPanel.selectGeneFile(2);
        Timer.sleep(2000);
        VCFPanel.waitPict();
        WebElement pWnd = getDriver().findElement(By.cssSelector(".md-transition-in"));
        pWnd.sendKeys(Keys.ESCAPE);
    }

    public void scanVCFPanels() {
        int N = gridPanel.getNumRows();
        int M = gridPanel.getNumCols();
        for (int i = 0; i < N; i = i + M) {
            gridPanel.cell(i, 2).click();
            visualizerVCF(i);
        }
    }

    public void checkSetOfColumns(String... columns) {
        Assert.isTrue(compareTwoStringLists(gridPanel.columnsList.getTextList(), Arrays.asList(columns)), "Wrong set of columns");
    }


    public void checkVarQuality(String... rangeValues) {
        SoftAssert soft_assert = new SoftAssert();
        Assert.isFalse(variantsTable.tableRows.size() == 0, "There are no records in the 'Variants' table");
        for (int i = 0; i < variantsTable.tableRows.size(); i++) {
            variantsTable.tableRows.get(i).clickInSpeacialCell();
            soft_assert.assertTrue(variationInfoWindow.isQualityWithinRange(rangeValues), "In " + (i + 1) + " row is incorrect quality.");
            variationInfoWindow.closeWindow();
        }
        soft_assert.assertAll();
    }


    public void checkVarGenes(String... genes) {
        List<String> valuesFromTable;
        List<Boolean> boolList = new ArrayList<>();
        for (int i = 0; i < variantsTable.tableRows.size(); i++) {
            valuesFromTable = Arrays.asList(variantsTable.tableRows.get(i).getRowValue(variantsTable.getColumnIndex("Gene")).replaceAll("\\s", "").split(","));
            for (int j = 0; j < genes.length; j++) {
                boolList.add(valuesFromTable.contains(genes[j]));
            }
            Assert.isTrue(boolList.contains(true), "There is no required gene among values from table: " + valuesFromTable.toString());
            boolList.clear();
        }
    }

    public void checkPictWithFile(String ProjectDir) {
        try {
            variantsTable.collectAllPictData(ProjectDir);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(e.toString().isEmpty(), "Can't take picture");
        }
    }
}
