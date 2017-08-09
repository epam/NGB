package epam.autotests.page_objects.sections;

import com.epam.jdi.uitests.web.selenium.elements.common.Button;
import com.epam.jdi.uitests.web.selenium.elements.complex.Elements;
import com.epam.jdi.uitests.web.selenium.elements.complex.TextList;
import com.epam.jdi.uitests.web.selenium.elements.composite.Section;
import org.openqa.selenium.support.FindBy;

/**
 * Created by Vsevolod_Adrianov on 7/18/2016.
 */
public class GridPanel extends Section {

    @FindBy(css = ".lm_item_container[style*='block'] .jqx-widget-header.jqx-grid-header [role='columnheader']")
    private Elements<Button> columnButton;

    //	@FindBy(xpath = ".//div[@class='jqx-widget-header jqx-grid-header']//span")
    @FindBy(css = ".ui-grid-header-cell-row .ui-grid-header-cell-label")
    public TextList<?> columnsList;

    @FindBy(css = ".lm_item_container[style*='block'] [role='row'] .jqx-grid-cell.jqx-item")
    private Elements<Button> vRows;


    public int getNumCols() {
        return columnButton.size();
    }

    public int getNumRows() {
        return vRows.size() / this.getNumCols();
    }

    public Button cell(int row, int col) {
        return vRows.get(row * this.getNumCols() + col);
    }

    public Button columnHeader(int i) {
        return columnButton.get(i);
    }



    public void sorting(String colName) {
        System.out.println("Sort out column: " + colName);
        for (int i = 0; i < getNumCols(); i++) {
            if (colName.equals(columnHeader(i).getText())) {
                columnHeader(i).click();
                System.out.println("Sort out column: " + colName + "[" + i + "] .... Success");
                return;
            }
        }
    }
}
