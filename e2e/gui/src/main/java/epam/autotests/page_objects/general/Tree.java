package epam.autotests.page_objects.general;

import com.epam.jdi.uitests.web.selenium.elements.complex.Elements;
import com.epam.jdi.uitests.web.selenium.elements.composite.Section;
import org.openqa.selenium.support.FindBy;

public class Tree extends Section {

    @FindBy(css = ".dataset-item-row")
    private Elements<Node> dataset;

    public int Nodes() {
        return dataset.size();
    }

    public Node getBy(String name) {
        for (int i = 0; i < Nodes(); i++)
            if (dataset.get(i).getToggleLabel().getText().toUpperCase().contains(name.toUpperCase()))
                return dataset.get(i);
        return null;
    }

    public boolean isSelected() {
        boolean isSelect = false;
        try {
            for (int i = 0; i < Nodes(); i++) {
                if (dataset.get(i).cBox.isDisplayed() && dataset.get(i).cBox.isChecked()) {
                    isSelect = false;
                    break;
                } else
                    isSelect = true;
            }
        } catch (Exception e) {
            return false;
        }
        return isSelect;
    }
}
