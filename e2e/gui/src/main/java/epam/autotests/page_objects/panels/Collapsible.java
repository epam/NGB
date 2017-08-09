package epam.autotests.page_objects.panels;

import com.epam.jdi.uitests.web.selenium.elements.base.Element;
import com.epam.jdi.uitests.web.selenium.elements.common.Button;
import com.epam.jdi.uitests.web.selenium.elements.common.CheckBox;
import com.epam.jdi.uitests.web.selenium.elements.complex.Elements;
import org.openqa.selenium.support.FindBy;

/**
 * Created by Vsevolod_Adrianov on 8/15/2016.
 */
public class Collapsible extends Element {
    @FindBy(css = ".md-button_block")
    private Button tName;
    // md-content .md-primary
    @FindBy(css = "md-content .md-primary") // ngb-filter-panel .md-primary
    private Elements<CheckBox> checkBox;

    //
    public String getGroupName() {
        return tName.getText();
    }

    //
    public void DisplayCheckBox() {
        System.out.println("**********Filter Group  <" + tName.getText() + ">  **************");
//        String ss=tName.getText();
        if (!checkBox.isEmpty()) {
            for (int i = 0; i < checkBox.size(); i++) {
                System.out.println("CheckBox[" + i + "]=" + checkBox.get(i).getWebElement().getText());
                if (checkBox.get(i).isChecked())
                    System.out.println("Checked");
                else
                    System.out.println("not Checked");
            }
        }
    }
}
