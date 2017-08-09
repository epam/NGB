package epam.autotests.page_objects.pages;

import com.epam.commons.PropertyReader;
import com.epam.jdi.uitests.web.selenium.driver.SauceLabRunner;
import com.epam.jdi.uitests.web.selenium.elements.composite.WebPage;
import epam.autotests.page_objects.panels.DatasetsPanel;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;

import java.io.IOException;

public class MainPage extends WebPage {

    @FindBy(xpath = "//ngb-data-sets")
    public DatasetsPanel datasetsPanel;

    public void Datasets() {
        datasetsPanel.clickCenter();
    }

    public WebDriver remoteDriver() {
        try {
            String os = PropertyReader.getProperty("os");
            String browserVersion = PropertyReader.getProperty("browser.version");
            String screenResolutin = PropertyReader.getProperty("screen.resolution");
            DesiredCapabilities caps = new DesiredCapabilities();
            switch (PropertyReader.getProperty("driver").toUpperCase()) {
                case "CHROME":
                    caps = DesiredCapabilities.chrome();
                    break;
                case "FIREFOX":
                    caps = DesiredCapabilities.firefox();
                    break;
                case "EDGE":
                    caps = DesiredCapabilities.edge();
                    break;
                case "IE":
                    caps = DesiredCapabilities.internetExplorer();
                    break;
                case "SAFARI":
                    caps = DesiredCapabilities.safari();
                    break;
            }
            caps.setCapability("platform", os);
            caps.setCapability("version", browserVersion);
            caps.setCapability("screenResolution", screenResolutin);
            SauceLabRunner.authentication.setUsername(System.getenv("SAUCE_USER_NAME"));
            SauceLabRunner.authentication.setAccessKey(System.getenv("SAUCE_ACCESS_KEY"));
            RemoteWebDriver driver = new RemoteWebDriver(SauceLabRunner.getSauceUrl(), caps);
            driver.executeScript("sauce:job-name= Autotest from travis job #" + System.getenv("TRAVIS_JOB_NUMBER") + "\"");
            return driver;
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
