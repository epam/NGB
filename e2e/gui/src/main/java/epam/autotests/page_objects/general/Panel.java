package epam.autotests.page_objects.general;

import com.epam.jdi.uitests.web.selenium.elements.common.Button;
import com.epam.jdi.uitests.web.selenium.elements.composite.Section;
import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;

/**
 * @author Vsevolod Adrianov Created by Vsevolod_Adrianov in June, 2016.
 *         <p/>
 *         <b>Refactored</b> by Aksenov Oleg in October, 2016
 */

public abstract class Panel extends Section {

    @FindBy(xpath = ".//div[@class='lm_close_tab']")
    private Button bClose;


    public String getTitle() {
        return this.get(By.cssSelector(".lm_title")).getText();
    }

    public void closeClick() {
        bClose.click();
    }
}
