package epam.autotests.page_objects.sections;

import com.epam.jdi.uitests.web.selenium.elements.base.Element;
import com.epam.jdi.uitests.web.selenium.elements.common.Text;
import com.epam.jdi.uitests.web.selenium.elements.composite.Section;
import org.openqa.selenium.support.FindBy;

public class ColumnHeader extends Section {

    @FindBy(css = ".ui-grid-header-cell-label")
    private Text columnName;

    @FindBy(xpath = ".//div[@aria-sort='ascending']")
    private Element ascendingSortSign;

    @FindBy(xpath = ".//div[@aria-sort='descending']")
    private Element descendingSortSign;

    public String getColumnName() {
        return columnName.getText();
    }

    public String getSortingType() {
        String typeOfSort;
        typeOfSort = this.getAttribute("aria-sort").toLowerCase();
        switch (this.getAttribute("aria-sort")) {
            case "none":
                typeOfSort = "None Sorting";
                break;
            case "ascending":
                typeOfSort = "Sort Ascending";
                break;
            case "descending":
                typeOfSort = "Sort Descending";
                break;
        }
        return typeOfSort;
    }

    public boolean isSorted() {
        return ascendingSortSign.isDisplayed() || descendingSortSign.isDisplayed();
    }


    public void click() {
        columnName.clickCenter();
    }
}
