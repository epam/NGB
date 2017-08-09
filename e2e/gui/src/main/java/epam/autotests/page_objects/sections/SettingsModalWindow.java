package epam.autotests.page_objects.sections;

import com.epam.jdi.uitests.web.selenium.elements.base.Element;
import com.epam.jdi.uitests.web.selenium.elements.common.Button;
import com.epam.jdi.uitests.web.selenium.elements.common.TextField;
import com.epam.jdi.uitests.web.selenium.elements.complex.Tabs;
import com.epam.jdi.uitests.web.selenium.elements.complex.table.Table;
import com.epam.jdi.uitests.web.selenium.elements.composite.Section;
import epam.autotests.page_objects.enums.SettingTabs;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.FindBy;

import static com.epam.jdi.uitests.core.settings.JDISettings.logger;

public class SettingsModalWindow extends Section {

    @FindBy(xpath = ".//div[@class='md-toolbar-tools']//button")
    private Button closeWindow;

    @FindBy(xpath = ".//md-tab-item")
    private Tabs<SettingTabs> settingTabs;

    @FindBy(xpath = ".//button[@ng-click='ctrl.save()']")
    private Button saveBtn;

    @FindBy(xpath = ".//button[@aria-label='Cancel']")
    private Button cancelBtn;

    @FindBy(xpath = ".//button[@aria-label='LAYOUT']/ancestor::collapsible")
    private SettingsCellsSection layoutSettings;

    @FindBy(xpath = ".//button[@aria-label='LAYOUT']/ancestor::collapsible//table")
    private Table layoutTable;

    @FindBy(css = "md-checkbox[aria-label='Downsample reads']")
    private Element Downsample;

    @FindBy(name = "maxRangeBpBam")
    private TextField MaximumRange;


    public void openTab(SettingTabs tabName) {
        settingTabs.select(tabName);
    }

    public void changeHotKey(String parameterName, Keys key, String secondKey) {
        int rowIndex = layoutTable.rows().count();
        for (int i = 1; i <= rowIndex; i++) {
            if (layoutTable.cell(1, i).getText().equals(parameterName)) {
                layoutTable.cell(2, i).get(SettingsCellsSection.class).setNewHotKey(key, secondKey);
                logger.info(layoutTable.cell(2, i).get(SettingsCellsSection.class).getCurrentHotKey());
                break;
            }
        }
    }

    public void checkDownsample() {
        Boolean check = Downsample.getAttribute("class").contains("md-checked");
        if (!check)
            Downsample.clickCenter();
    }

    public void uncheckDownsample() {
        Boolean check = Downsample.getAttribute("class").contains("md-checked");
        if (check)
            Downsample.clickCenter();
    }

    public void changeMaximumRange(String rangeValue) {
        MaximumRange.focus();
        MaximumRange.clear();
        MaximumRange.input(rangeValue);
    }


    public void saveSettings() {
        saveBtn.clickCenter();
    }

    public void cancel() {
        cancelBtn.clickCenter();
    }

    public void close() {
        closeWindow.clickCenter();
    }

}
