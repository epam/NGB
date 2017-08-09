package epam.autotests.page_objects.sections;

import com.epam.jdi.uitests.web.selenium.elements.common.Button;
import com.epam.jdi.uitests.web.selenium.elements.composite.Section;
import org.openqa.selenium.Keys;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;

public class SettingsCellsSection extends Section {

    @FindBy(xpath = ".//button")
    private Button hotkey;

    public void setNewHotKey(Keys funcKey, String charKey) {
        hotkey.clickCenter();
        new Actions(getDriver()).keyDown(funcKey).sendKeys(charKey).keyUp(funcKey).build().perform();
    }

    public String getCurrentHotKey() {
        return hotkey.getText();
    }
}
