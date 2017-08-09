package epam.autotests.page_objects.panels;

import com.epam.jdi.uitests.web.selenium.elements.base.Element;
import com.epam.jdi.uitests.web.selenium.elements.common.Button;
import com.epam.jdi.uitests.web.selenium.elements.common.TextField;
import com.epam.jdi.uitests.web.selenium.elements.complex.Elements;
import com.epam.web.matcher.junit.Assert;
import epam.autotests.page_objects.enums.FiltersGroups;
import epam.autotests.page_objects.general.SetOfProperties;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static epam.autotests.page_objects.enums.FiltersGroups.*;
import static epam.autotests.page_objects.enums.Views.BROWSER;
import static epam.autotests.page_objects.enums.Views.VARIANTS;
import static epam.autotests.page_objects.site.NGB_Site.emptySpace;
import static epam.autotests.page_objects.site.NGB_Site.projectPage;


public class FiltersPanel extends VariantsPanel {

    @FindBy(css = ".md-button[aria-label='SET TO DEFAULTS']")
    private Button setToDefaultsBtn;

    @FindBy(xpath = ".//button[contains(.,'Active VCF files')]//ancestor::collapsible")
    private SetOfProperties activeVCFFilesGroup;

    @FindBy(xpath = ".//div[1]/div[1]/ngb-variants-table-filter/div/ngb-variants-filter-list/input")
    private TextField typeOfVariantGroup;//TextField

    @FindBy(xpath = ".//collapsible//button")
    private Elements<Element> collapsibleSections;

    @FindBy(xpath = ".//quality//input[@id='qualityFrom']")
    private TextField quialityFrom;

    @FindBy(xpath = ".//quality//input[@id='qualityTo']")
    private TextField quialityTo;

    @FindBy(xpath = ".//div[3]/div[2]/ngb-variants-table-filter/div/ngb-variants-filter-list/input")
    private TextField addingGene;

    @FindBy(xpath = ".//div[3]/div[2]/ngb-variants-table-filter/div/ngb-variants-filter-list/input")
    private TextField addedGenes;


    @FindBy(xpath = ".//ngb-variants-table-column//button")
    public Button variantsHamburger;

    @FindBy(xpath = ".//md-menu-content/md-menu-item[1]/div/md-checkbox")
    private Element filterCheckBox;

    public void openFilter() {
        variantsHamburger.click();
        if (filterCheckBox.getAttribute("aria-checked").toUpperCase().contains("FALSE")) { //???
            filterCheckBox.clickCenter();
            emptySpace.clickCenter();
        } else
            emptySpace.clickCenter();
    }

    public void closeFilter() {
        variantsHamburger.click();
        if (!filterCheckBox.getAttribute("aria-checked").toUpperCase().contains("FALSE")) {
            filterCheckBox.clickCenter();
            emptySpace.clickCenter();
        } else
            emptySpace.clickCenter();
    }



    public int getSizeActiveVCFFile() {
        return activeVCFFilesGroup.getNumberChecks();
    }

    public List<String> getListOfSelectedFilters() {
        return activeVCFFilesGroup.getListOfSelectedFilters();
    }


    public void selectFilter(FiltersGroups groupName, String... parameters) {
        switch (groupName) {
            case GENE: {
                deleteAllAddedGenes();
                if (!parameters[0].equals("")) {
                    String geneName = "";
                    for (int i = 0; i < parameters.length; i++) {
                        geneName = geneName + parameters[i] + ",";
                    }
                    selectGene(geneName);
                }
                break;
            }
            case TYPE_VARIANT: {
                deleteAllAddedVariationType();
                if (!parameters[0].equals("")) {
                    String VariationType = "";
                    for (int i = 0; i < parameters.length; i++) {
                        VariationType = VariationType + parameters[i] + ",";
                    }
                    selectVariationType(VariationType);
                    break;
                }
            }
        }
    }

    public void selectGene(String geneName) {
        addingGene.clickCenter();
        addingGene.sendKeys(geneName + "," + Keys.ENTER);
        addingGene.sendKeys(Keys.ENTER);
    }

    public void selectVariationType(String geneName) {
        typeOfVariantGroup.clickCenter();
        typeOfVariantGroup.sendKeys(geneName + "," + Keys.ENTER);
        typeOfVariantGroup.sendKeys(Keys.ENTER);
    }

    public void deleteAllAddedGenes() {
        addedGenes.clear();
    }

    public void deleteAllAddedVariationType() {
        typeOfVariantGroup.clear();
    }

    public void clearFilterPanel() {
        selectFilter(GENE, "");
        selectFilter(TYPE_VARIANT, "");
        selectFilter(VARIANT_LOCATION, "");
    }

    public void checkCopyPaste() {
        quialityFrom.sendKeys("12");
        quialityFrom.sendKeys(Keys.LEFT_CONTROL + "a");
        quialityFrom.sendKeys(Keys.LEFT_CONTROL + "c");
        quialityTo.clickCenter();
        quialityTo.sendKeys(Keys.LEFT_CONTROL + "v");
        Assert.isTrue(quialityFrom.getText().equals(quialityTo.getText()));
    }
}
