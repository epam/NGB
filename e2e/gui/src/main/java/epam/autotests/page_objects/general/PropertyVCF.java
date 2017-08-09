package epam.autotests.page_objects.general;

import com.epam.jdi.uitests.web.selenium.elements.base.Element;
import com.epam.jdi.uitests.web.selenium.elements.complex.RadioButtons;
import com.epam.jdi.uitests.web.selenium.elements.composite.Section;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Created by Vsevolod_Adrianov on 7/18/2016.
 */
public class PropertyVCF extends Section {
    @FindBy(css = ".md-variant-property-single-value")
    private List<Element> pValue;

    @FindBy(css = "md-radio-button")
    private RadioButtons listGeneFiles;

    @FindBy(css = "canvas")
    private Element pict;


    public void selectGeneFile(int ix) {
        listGeneFiles.select(ix);
    }

    public boolean isLoadedProperties() {
        return pValue.isEmpty();
    }

    public void waitPict() {
        pict.waitDisplayed();
    }
}
