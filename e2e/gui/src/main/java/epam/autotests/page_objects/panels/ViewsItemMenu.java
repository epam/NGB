package epam.autotests.page_objects.panels;

import com.epam.jdi.uitests.web.selenium.elements.base.Element;
import com.epam.jdi.uitests.web.selenium.elements.common.Button;
import org.openqa.selenium.support.FindBy;

/**
 * Created by Vsevolod_Adrianov on 8/16/2016.
 */
public class ViewsItemMenu extends Element {
    @FindBy(css = "button")
    private Button tButton;

    @FindBy(css = "[icon='check']")
    private Element checkMark;

    public boolean isChecked() {
        return checkMark.isDisplayed();
    }

    public void selectView() {
        tButton.click();
    }
}
