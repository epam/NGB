package epam.autotests.page_objects.sections;

import com.epam.jdi.uitests.web.selenium.elements.base.Element;
import com.epam.jdi.uitests.web.selenium.elements.complex.Elements;
import com.epam.jdi.uitests.web.selenium.elements.complex.TextList;
import com.epam.jdi.uitests.web.selenium.elements.composite.Section;
import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;

import static epam.autotests.page_objects.site.NGB_Site.variationInfoWindow;

public class TableRow extends Section {

    //	@FindBy(xpath = ".//div[@role='gridcell']//descendant::*[not(text()='')]")
    @FindBy(css = "[role='gridcell']")
    private TextList<?> rowValues;

    //	@FindBy(xpath = ".//div[@role='gridcell']")
    @FindBy(css = "[role='gridcell']")
//	@FindBy(css = "div .ui-grid-row")
    private Elements<Element> rowCells;


    public String getRowValue(int columnIndex) {
        return rowValues.getTextList().get(columnIndex);
    }


    public boolean isRowContainsValue(String requiredValue) {
        return rowValues.getTextList().contains(requiredValue);
    }

    public List<String> collectRowData(int... columnsIndex) {
        List<String> values = new ArrayList<>();
        List<String> rowValues = this.rowValues.getTextList();
        for (int index : columnsIndex) {
            values.add(rowValues.get(index));
        }
        return values;
    }

    public String[] collectRowData2(int... columnsIndex) {
        String[] values = new String[columnsIndex.length];
        int counter = 0;
        for (int i : columnsIndex) {
            if (rowCells.get(i).getWebElement().findElements(By.cssSelector("[aria-label='info']")).size() > 0) {
                clickCell(i);
                values[counter++] = variationInfoWindow.getId();
                variationInfoWindow.closeWindow();
            } else {
                values[counter++] = rowCells.get(i).get(By.cssSelector(".ui-grid-cell-contents,.md-label")).getText();
            }
        }
        return values;
    }

    public void collectPictData(String toPath) {
        if (rowCells.get(4).getWebElement().findElements(By.cssSelector("[aria-label='info']")).size() > 0) {
            String svtype = rowCells.get(0).getWebElement().getText();
            String location = rowCells.get(3).getWebElement().getText();
            String variationName = svtype + "_" + location;
            com.epam.commons.Timer.sleep(6000);
            clickCell(4);
            com.epam.commons.Timer.sleep(6000);
            try {
                variationInfoWindow.savePictureVCF(toPath, variationName);
            } catch (AssertionError fail) {
                fail.printStackTrace();
                variationInfoWindow.closeWindow();
            }
            variationInfoWindow.closeWindow();
        }
    }

    private Element getSpecialCell() {
        for (Element cell : rowCells) {
            if (cell.getWebElement().findElements(By.cssSelector("[aria-label='Table View']")).size() > 0) //rowValues.getElements().size()
                return cell;
        }
        return null;
    }

    /**
     * Open Annotation window or delete bookmark
     */
    public void clickInSpeacialCell() {
        rowCells.get(4).clickCenter();
    }

    public void clickCell(int columnIndex) {
        rowCells.get(columnIndex).clickCenter();
    }
}
