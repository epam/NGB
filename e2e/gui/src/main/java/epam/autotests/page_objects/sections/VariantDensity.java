package epam.autotests.page_objects.sections;

import com.epam.commons.Timer;
import com.epam.jdi.uitests.web.selenium.elements.base.Element;
import com.epam.jdi.uitests.web.selenium.elements.complex.Elements;
import com.epam.jdi.uitests.web.selenium.elements.composite.Section;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Created by Vsevolod_Adrianov on 7/25/2016.
 */
public class VariantDensity extends Section {
    //
    @FindBy(css = ".jqx-chart-label-text")
    private List<Element> weight;
    //
    @FindBy(css = ".jqx-chart-axis-text")
    private List<Element> usedChromosome;

    @FindBy(css = "ngb-variant-density-diagram g>g>rect")
    private static Elements<Element> bar;

    public int getNumberChromosome() {
        return usedChromosome.size();
    }

    public String nameChromosome(int indx) {
        return usedChromosome.get(indx).getWebElement().getText();
    }

    public int getWeight(int idx) {
        Timer.waitCondition(() -> weight.get(idx).isDisplayed());
        return Integer.valueOf(weight.get(idx).getWebElement().getText());
    }

    public static void selectChromosome(int ix) {
        bar.get(ix).clickCenter();
    }
}
