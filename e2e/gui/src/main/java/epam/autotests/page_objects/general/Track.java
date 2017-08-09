package epam.autotests.page_objects.general;

import com.epam.jdi.uitests.web.selenium.elements.common.Label;
import com.epam.jdi.uitests.web.selenium.elements.composite.Section;
import org.openqa.selenium.support.FindBy;


public class Track extends Section {

    @FindBy(xpath = ".//div[contains(@class, 'ngb-track-header-type')]")
    private Label trackType;

    @FindBy(xpath = ".//div[contains(@class, 'ngb-track-header-file-name')]")
    private Label trackTitle;

    public String getTrackType() {
        return trackType.getText();
    }

    public String getTrackTitle() {
        return trackTitle.getText();
    }

    public String getTrackAllText() {
        return getTrackType().concat(" ").concat(getTrackTitle());
    }
}
