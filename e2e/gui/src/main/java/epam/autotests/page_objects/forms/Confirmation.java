package epam.autotests.page_objects.forms;

import com.epam.jdi.uitests.web.selenium.elements.common.Button;
import com.epam.jdi.uitests.web.selenium.elements.common.Text;
import com.epam.jdi.uitests.web.selenium.elements.composite.Form;
import org.openqa.selenium.support.FindBy;

public class Confirmation extends Form {

    @FindBy(xpath = ".//h2")
    private Text messageText;

    @FindBy(css = "button.md-primary.md-cancel-button.md-button.ng-scope.md-default-theme.md-ink-ripple")
    private Button cancelBtn;

    @FindBy(css = "button.md-primary.md-confirm-button.md-button.md-default-theme.md-ink-ripple")
    private Button OKBtn;

    public String getMessageText() {
        return messageText.getText();
    }

    public void pressCancel() {
        cancelBtn.clickCenter();
    }

    public void pressOK() {
        OKBtn.clickCenter();
    }
}
