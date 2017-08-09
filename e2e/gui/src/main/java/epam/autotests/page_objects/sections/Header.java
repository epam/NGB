package epam.autotests.page_objects.sections;

import com.epam.commons.Timer;
import com.epam.jdi.uitests.web.selenium.elements.base.Element;
import com.epam.jdi.uitests.web.selenium.elements.common.Button;
import com.epam.jdi.uitests.web.selenium.elements.common.Label;
import com.epam.jdi.uitests.web.selenium.elements.common.TextField;
import com.epam.jdi.uitests.web.selenium.elements.complex.Elements;
import com.epam.jdi.uitests.web.selenium.elements.complex.Menu;
import com.epam.jdi.uitests.web.selenium.elements.composite.Section;
import com.epam.jdi.uitests.web.selenium.elements.composite.WebPage;
import com.epam.jdi.uitests.web.settings.WebSettings;
import com.epam.web.matcher.testng.Assert;
import epam.autotests.page_objects.enums.Views;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.List;

import static epam.autotests.page_objects.site.NGB_Site.projectPage;
import static org.testng.Assert.assertTrue;


public class Header extends Section {


    @FindBy(css = ".md-title.page-title.hide.show-gt-sm")
    private Label appTitle;

    @FindBy(xpath = ".//ngb-version/h3")
    private Label appVersion;

    @FindBy(css = ".md-title.page-title.hide.show-gt-sm")
    private Label pageTitle;

    @FindBy(css = "h3.md-title.project-title")
    public Label projectTitle;

    @FindBy(xpath = ".//ngb-search//input")
    private TextField searchInput;

    @FindBy(xpath = ".//gene-search//button")
    private Button searchBtn;


    @FindBy(css = "md-input-container .md-input[aria-label='chromosome input']")
    private TextField chromosomeChooser;


    @FindBy(css = ".coordinates-menu-item")
    private Menu<?> chromosomeMenu;

    @FindBy(css = "md-input-container .md-input[aria-label='coordinates input']")
    private TextField coordinatesChooser;

    @FindBy(css = ".coordinates-menu-item .context-menu-item")
    private Menu<?> geneMenu;

    @FindBy(css = "button[aria-owns='menu_container_2']")
    private Button menuViewsBtn;

    @FindBy(xpath = ".//div[contains(@class, 'md-open-menu-container')]/md-menu-content[contains(@class,'ngb-tool-menu')]//md-menu-item")
    private Elements<Element> viewsMenuItems;

    @FindBy(xpath = ".//ngb-main-settings/button")
    private Button settingBtn;

    @FindBy(xpath = ".//ngb-bookmark/md-menu/button")
    private Menu<?> sessionMenu;

    public void searchField(String ss) {
        searchInput.setValue(ss);
    }

    public void typeSearchValue(String value) {
        searchInput.sendKeys(value);
    }

    public String getSearchFieldText() {
        return searchInput.getText();
    }

    public boolean isAppTitlePresent() {
        return appTitle.isDisplayed();
    }

    public String getAppVersion() {
        return appVersion.getText();
    }

    public String getVersion() {
        Timer.waitCondition(() -> isAppTitlePresent());
        System.out.println("getVersion(" + getAppVersion() + ");");
        return getAppVersion();
    }

    public boolean checkView(WebPage page) {
        List<Boolean> checkList = new ArrayList<>();
        checkList.add(appTitle.isDisplayed());
        checkList.add(appVersion.isDisplayed());
        checkList.add(pageTitle.isDisplayed());
        switch (page.getClass().getSimpleName()) {
            case "MainPage": {
                checkList.add(searchInput.isDisplayed());
                break;
            }
            case "ProjectPage": {
                checkList.add(projectTitle.isDisplayed());
                checkList.add(searchBtn.isDisplayed());
                checkList.add(menuViewsBtn.isDisplayed());
                checkList.add(settingBtn.isDisplayed());
                break;
            }
            default:
                throw new IllegalArgumentException("Wrong class was transmitted");
        }
        return checkList.contains(false);
    }

    public void goToMainPage() {
        appTitle.clickCenter();
    }

    private Element getViewMenuItem(Views viewsName) {
        for (Element menuItem : viewsMenuItems) {
            if (menuItem.get(By.xpath(".//span")).getText().equals(viewsName.value))
                return menuItem;
        }
        return null;
    }

    public void selectView(Views viewsName) {
        menuViewsBtn.clickCenter();
        Timer.sleep(1500);
        Element menuItem = getViewMenuItem(viewsName);
        if (!isMenuItemChecked(menuItem)) {

            menuItem.clickCenter();
        }
        else
            menuViewsBtn.clickCenter();
    }

    private boolean isMenuItemChecked(Element viewsMenuItem) {
        try {
            return viewsMenuItem.get(By.xpath(".//ng-md-icon[@icon='check']")).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public void hideView(Views viewsName) {
        menuViewsBtn.clickCenter();
        Element menuItem = getViewMenuItem(viewsName);
        if (isMenuItemChecked(menuItem)) {
            menuItem.clickCenter();
        }
    }

    public void chooseChromosome(String chr) {
        projectPage.browserPanel.ChrMenu().click();
        chromosomeChooser.sendKeys(chr);
//		chromosomeChooser.sendKeys(Keys.LEFT_CONTROL+"a" + string); i don't know what this row do
        chromosomeMenu.select(chr);
    }

    public void chooseCoordinates(String string) {
        projectPage.browserPanel.CoordMenu().click();
        coordinatesChooser.sendKeys(string);
        coordinatesChooser.sendKeys(Keys.ENTER);
    }


    public boolean isChromosomeSelected() {
        return projectPage.browserPanel.getChrTitle().equals("NONE");
    }

    public void resetChSelection() {
        if (isChromosomeSelected()) {
            String Chromosome = "NONE";
            projectPage.browserPanel.ChrMenu().click();
            chromosomeChooser.sendKeys(Keys.LEFT_CONTROL + "a" + Chromosome);
            chromosomeMenu.select(Chromosome);
        }
    }

    public void chooseBookmark(String string) {
        projectPage.browserPanel.CoordMenu().click();
        coordinatesChooser.sendKeys(string);
        geneMenu.select(string);
    }

    public void openSetting() {
        settingBtn.clickCenter();
    }
}
