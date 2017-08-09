package epam.autotests.page_objects.sections;

import com.epam.jdi.uitests.web.selenium.elements.base.Element;
import com.epam.jdi.uitests.web.selenium.elements.composite.Section;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Created by Vsevolod_Adrianov on 7/25/2016.
 */
public class VariantsType extends Section {
    @FindBy(css = ".jqx-chart-label-text")
    private List<Element> varTypes;
    //
    @FindBy(css = ".jqx-chart-axis-text")
    private List<Element> TypeName;

    //
    public int NumTypes() {
        return varTypes.size();
    }

    public String NameOfType(int indx) {
        return TypeName.get(indx).getWebElement().getText();
    }

    public int getWeight(int idx) {
        return Integer.valueOf(varTypes.get(idx).getWebElement().getText());
    }
}
