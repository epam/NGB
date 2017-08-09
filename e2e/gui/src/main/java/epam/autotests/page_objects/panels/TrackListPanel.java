package epam.autotests.page_objects.panels;

import com.epam.commons.Timer;
import com.epam.jdi.uitests.web.selenium.elements.base.Element;
import com.epam.jdi.uitests.web.selenium.elements.common.CheckBox;
import com.epam.jdi.uitests.web.selenium.elements.common.TextField;
import com.epam.jdi.uitests.web.selenium.elements.complex.Elements;
import com.epam.jdi.uitests.web.selenium.elements.complex.TextList;
import epam.autotests.page_objects.enums.FiltersGroups;
import epam.autotests.page_objects.general.Panel;
import epam.autotests.page_objects.general.SetOfProperties;
import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;
import org.testng.asserts.SoftAssert;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static epam.autotests.page_objects.enums.FiltersGroups.GENE2;
import static epam.autotests.page_objects.enums.FiltersGroups.VCF;

public class TrackListPanel extends Panel {

    @FindBy(xpath = ".//input")
    private TextField searchField;

    @FindBy(xpath = ".//collapsible")
    private Elements<SetOfProperties> collapsibleSections;

    @FindBy(xpath = ".//collapsible//button[@aria-label = 'REFERENCE']//ancestor::collapsible.//collapsible//button[@aria-label = 'REFERENCE']//ancestor::collapsible")
    private SetOfProperties referenceGroup;

    @FindBy(xpath = ".//collapsible//button[@aria-label = 'VCF']//ancestor::collapsible")
    private SetOfProperties vcfFilesGroup;

    @FindBy(xpath = ".//collapsible//button[@aria-label = 'GENE']//ancestor::collapsible")
    private SetOfProperties geneGroup;

    @FindBy(xpath = ".//collapsible//md-checkbox[@aria-checked = 'true']")
    private Elements<CheckBox> selectedCheckboxes;

    @FindBy(xpath = ".//collapsible//md-checkbox//span")
    private TextList<?> checkboxesText;

    private Element getGroup(String groupName) {
        for (SetOfProperties filterGroup : collapsibleSections) {
            if (filterGroup.getGroupName().equals(groupName))
                return filterGroup;
        }
        return null;
    }

    private boolean isGroupOpened(Element group) {
        switch (group.get(By.xpath(".//ng-md-icon")).getAttribute("class")) {
            case ("rotated ng-scope rotate-back"):
                return true;
            case ("rotated ng-scope rotate"):
                return false;
            default:
                return false;
        }
    }

    public void openFilterGroup(FiltersGroups groupName) {
        Element group = getGroup(groupName.toString());
        if (!isGroupOpened(group))
            group.clickCenter();
    }

    public void closeFilterGroup(FiltersGroups groupName) {
        Element group = getGroup(groupName.toString());
        if (isGroupOpened(group))
            group.clickCenter();
    }


    public void selectFilter(FiltersGroups groupName, String... parameters) {
        openFilterGroup(groupName);
        switch (groupName) {
            case VCF: {
                if (parameters.length == 1 && parameters[0].equals("")) {
                    if (!vcfFilesGroup.isCheckListWithoutSelection())
                        vcfFilesGroup.unCheckAllOptions();
                } else
                    vcfFilesGroup.checkOptions(parameters);
                break;
            }
            case GENE: {
                if (parameters.length == 1 && parameters[0].equals("")) {
                    if (!geneGroup.isCheckListWithoutSelection())
                        geneGroup.unCheckAllOptions();
                } else
                    geneGroup.checkOptions(parameters);
                break;
            }
        }
    }

    public void selectAllFilter() {
        openFilterGroup(VCF);
        vcfFilesGroup.checkAll();
        openFilterGroup(GENE2);
        geneGroup.checkAll();
    }

    public void uncheckAllFilter() {
        searchField.clear();
        vcfFilesGroup.parametersChlst.uncheckAll();
        geneGroup.parametersChlst.uncheckAll();
    }

    public int getCountOfSelectedFilters() {
        return this.getList(By.xpath(".//md-checkbox[@aria-checked = 'true']")).size();
    }

    public List<String> getListOfSelectedFilters() {
        List<String> selectedFilters = new ArrayList<>();
        for (CheckBox checkbox : selectedCheckboxes) {
            selectedFilters.add(checkbox.get(By.xpath(".//ancestor::div[@class='animate-show u-column-padding']/preceding-sibling::button")).getText().replaceAll("\\s", "").
                    concat(" ").concat(checkbox.get(By.xpath(".//div[@class='md-label']/span")).getText()));
        }
        return selectedFilters;
    }

    public void search(String searchRequest) {
        searchField.clear();
        searchField.sendKeys(searchRequest);
//        Timer.sleep(1000);

    }

    public void checkSearchResult(String searchRequest) {
        SoftAssert soft_assert = new SoftAssert();
        List<String> checkboxesText = collectFoundItems();
        for (String item : checkboxesText) {
            soft_assert.assertTrue(Pattern.compile(Pattern.quote(searchRequest), Pattern.CASE_INSENSITIVE).matcher(item).find(), item + " doesn't contain required string '" + searchRequest + "'");
        }
        soft_assert.assertAll();
    }

    public List<String> collectFoundItems() {
        List<String> checkboxesText = new ArrayList<>();
        for (int i = 0; i < collapsibleSections.size(); i++) {
            checkboxesText.addAll(collapsibleSections.get(i).parametersChlst.getOptions());
        }
        return checkboxesText;
    }

    public boolean isSearchResultEmpty() {
        return collectFoundItems().isEmpty();
    }
}
