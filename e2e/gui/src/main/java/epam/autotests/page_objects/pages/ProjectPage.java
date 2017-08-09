package epam.autotests.page_objects.pages;

import com.epam.commons.Timer;
import com.epam.jdi.uitests.web.selenium.elements.base.Element;
import com.epam.jdi.uitests.web.selenium.elements.common.Button;
import com.epam.jdi.uitests.web.selenium.elements.common.CheckBox;
import com.epam.jdi.uitests.web.selenium.elements.common.TextField;
import com.epam.jdi.uitests.web.selenium.elements.complex.CheckList;
import com.epam.jdi.uitests.web.selenium.elements.complex.Elements;
import com.epam.jdi.uitests.web.selenium.elements.complex.TextList;
import com.epam.jdi.uitests.web.selenium.elements.composite.WebPage;
import com.epam.web.matcher.testng.Assert;
import epam.autotests.page_objects.enums.Views;
import epam.autotests.page_objects.general.Panel;
import epam.autotests.page_objects.panels.*;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.FindBy;
import org.testng.asserts.SoftAssert;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static epam.autotests.page_objects.enums.Views.*;
import static epam.autotests.page_objects.site.NGB_Site.*;
import static org.testng.Assert.*;

/**
 * Created by Vsevolod_Adrianov on 7/8/2016.
 * <p>
 * <p/>
 * <b>Refactored</b> by Aksenov Oleg in September-October 2016
 */

public class ProjectPage extends WebPage {

    @FindBy(xpath = ".//ngb-bookmark/md-menu/button")
    private Button sessionBtn;

    @FindBy(xpath = ".//md-menu-content/input")
    private TextField sessionTextField;

    @FindBy(css = "[md-svg-icon='md-close']")
    private Element chromosomeClose;

    @FindBy(css = ".ngb-tool-menu md-menu-item")
    private Elements<ViewsItemMenu> menuItem;

    @FindBy(xpath = "//div[@class='lm_header']/ul/li/span")
    private TextList<?> listTabs;

    @FindBy(xpath = "//div[@class='lm_header']/ul[@class='lm_tabs']/li")
    private Elements<Element> panelsTabs;

    @FindBy(xpath = "//ngb-variants-table-panel")
    public VariantsPanel variantsPanel;

    public FiltersPanel filterPanel;

    @FindBy(xpath = "//ngb-browser")
    public BrowserPanel browserPanel;

    @FindBy(xpath = "//ngb-track")
    public TrackListPanel trackListPanel;

    @FindBy(xpath = "//ngb-bookmarks-table")
    public SessionsPanel sessionsPanel;

    @FindBy(xpath = "//ngb-molecular-viewer")
    public MolecularViewerPanel molViewerPanel;

    @FindBy(xpath = "//div/md-menu-content//md-checkbox")
    public CheckList<?> columns;

    @FindBy(xpath = "//div/md-menu-content//md-checkbox")
    public Elements<CheckBox> chbxColumns;

    @FindBy(xpath = "//ngb-close-all-tracks//ng-md-icon")
    private Element closeAllTracksButton;

    @FindBy(xpath = ".//md-dialog-actions/button[2]")
    public Button popUpOKButton;

    @FindBy(xpath = ".//md-dialog-actions/button[1]")
    public Button popUpCANCELButton;

    public void closeAllTracks() {
        if (closeAllTracksButton.isDisplayed()) {
            closeAllTracksButton.clickCenter();
            popUpOKButton.click();
            projectPage.openPanel(DATASETS);
            assertEquals(true, mainPage.datasetsPanel.isBoxSelected(), "dataset or file stil selected");
        }

    }

    public Panel getPanel(Views panelName) {
        switch (panelName) {
            case BROWSER:
                return browserPanel;
            case VARIANTS:
                return variantsPanel;
            case FILTER:
                return filterPanel;
            case TRACK_LIST:
                return trackListPanel;
            case SESSIONS:
                return sessionsPanel;
            case MOLECULAR_VIEWER:
                return molViewerPanel;
            default:
                return null;
        }
    }

    public void openFilter() {
        projectPage.openPanel(VARIANTS);
        filterPanel.openFilter();
    }

    public void closeFilter() {
        projectPage.openPanel(VARIANTS);
        filterPanel.closeFilter();
    }


    public void setBookmark(String bkmrk) {
        sessionBtn.clickCenter();
        sessionTextField.sendKeys(bkmrk + Keys.ENTER);
    }

    public void moveMouse(Point p) {
        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();

        // Search the devices for the one that draws the specified point.
        for (GraphicsDevice device: gs) {
            GraphicsConfiguration[] configurations =
                    device.getConfigurations();
            for (GraphicsConfiguration config: configurations) {
                Rectangle bounds = config.getBounds();
                if(bounds.contains(p)) {
                    // Set point to screen coordinates.
                    Point b = bounds.getLocation();
                    Point s = new Point(p.x - b.x, p.y - b.y);

                    try {
                        Robot r = new Robot(device);
                        r.mouseMove(s.x, s.y);
                    } catch (AWTException e) {
                        e.printStackTrace();
                    }

                    return;
                }
            }
        }
        // Couldn't move to the point, it may be off screen.
        return;
    }


    public void addBookmark(String bookmarkName) {
        sessionBtn.click();
//        Timer.sleep(1000);
        sessionTextField.focus();
        sessionTextField.sendKeys(bookmarkName+"\n");
        //workaround for firefox webdriver
        try {
            Robot r = new Robot();
            sessionTextField.focus();
            r.keyPress(10);
            r.keyRelease(10);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public void checkViewAfterAddition() {
        Timer.sleep(1000);
        com.epam.web.matcher.junit.Assert.isTrue(sessionBtn.getAttribute("class").contains("success-save"), "Bookmark sign isn't green");
        com.epam.web.matcher.junit.Assert.isFalse(sessionTextField.isDisplayed(), "Text field for bookmark's name is still displayed");
    }


    public void closePanel(String Item) {
        Timer.sleep(500);
        if (menuItem.get(Item).isChecked()) {
            menuItem.get(Item).selectView();
        }
    }


    public void onMainPage() {
        header.goToMainPage();
    }
    public void sortingByCol(String ColName) {
        variantsPanel.sortColumn(ColName);
    }


    public void chromosome() {
        System.out.println("        > Chromosome Diagram <");
        int N = browserPanel.countsChromosomes();
        for (int i = 0; i < N; i++) {
            System.out.println("Chromosome[" + i + "]=" + browserPanel.nameOfChromosome(i) + " weight=" + browserPanel.weight(i));
        }
        System.out.println("        > Variants types <");
        int M = browserPanel.countsOfTypes();
        for (int i = 0; i < M; i++) {
            System.out.println("Name of Type[" + i + "]=" + browserPanel.nameOfType(i) + " weight=" + browserPanel.weightOfType(i));
        }
    }


    public void checkDefaultView() {
        SoftAssert soft_assert = new SoftAssert();
        List<String> defaultPanelsSet = new ArrayList<String>(
                Arrays.asList(BROWSER.toString(), VARIANTS.toString(), TRACK_LIST.toString()));
        soft_assert.assertTrue(header.checkView(this), "Some elements in Header are displayed or not displayed");
        soft_assert.assertTrue(compareTwoStringLists(getNamesOfOpenedPanels(), defaultPanelsSet), "Set of default opened panels is not as expected");
        soft_assert.assertAll();
    }

    private List<String> getNamesOfOpenedPanels() {
        List<String> actualPanelsSet = new ArrayList<>();
        for (int i = 0; i < listTabs.count(); i++) {
            actualPanelsSet.add(listTabs.getText(i));
        }
        return actualPanelsSet;
    }

    private Element getPanelTab(Views panelName) {
        String browser = "BROWSER";
        if (browser.equalsIgnoreCase(panelName.toString())) {
            for (Element panelTab : panelsTabs) {
                String ss1 = panelTab.get(By.cssSelector(".lm_title .ng-hide")).getAttribute("textContent").trim();
                System.out.println("Tab name> <" + ss1 + "> [" + panelName.toString().toUpperCase() + "]");
                if (panelTab.get(By.cssSelector(".lm_title .ng-hide")).getAttribute("textContent").trim().equalsIgnoreCase(panelName.toString()))
                    return panelTab;
            }
            return null;
        }
        for (Element panelTab : panelsTabs) {
            String ss1 = panelTab.get(By.xpath("./span")).getText();
            System.out.println("Tab name> <" + ss1 + "> [" + panelName.toString().toUpperCase() + "]");
            if (panelTab.get(By.xpath("./span")).getText().equalsIgnoreCase(panelName.toString()))
                return panelTab;
        }
        return null;
    }

    public boolean isPanelActive(Views panelName) {
        Element tab = getPanelTab(panelName);
        if (tab == null)
            return false;
        if (tab.getAttribute("class").contains("lm_active"))
            return true;
        return false;
    }

    public void openPanel(Views panelName) {
        if (!getNamesOfOpenedPanels().contains(panelName.value.toUpperCase())) {
            header.selectView(panelName);
        } else if (!isPanelActive(panelName)) {
            getPanelTab(panelName).clickCenter();
        }
    }

    public void checkViewingTrackList() {
        Assert.isTrue(browserPanel.getCountOfOpenedTracks() - 2 == filterPanel.getSizeActiveVCFFile(),
                "Number of displayed track and number of selected filters is not the same");
        Assert.isTrue(compareTwoStringLists(browserPanel.getTracksTitle("VCF"), filterPanel.getListOfSelectedFilters()), "Displayed tracks and selected filters don't correspond with each other.");
    }

    public void checkLastTrack(String trackTitle) {
        List<String> tracksList = browserPanel.getTracksTitle("VCF");
        Assert.isTrue(tracksList.get(tracksList.size() - 1).equalsIgnoreCase(trackTitle), "Wrong last track");
    }

    public void checkingViewOfBookmarksAndBrowser(String bookmarkName) {
        String[] bmParams = sessionsPanel.bookmarksTable.findRow("Name", bookmarkName).collectRowData2(1, 2, 3);
        String[] tabParams = new String[3];
        tabParams[0] = browserPanel.getTabTitle().replaceAll("(CHR:)|(\\n\\d+)", "");
        tabParams[1] = browserPanel.CoordMenu().getText().replaceAll("(\\w+:\\s)|(\\s-\\s\\d+)", "");
        tabParams[2] = browserPanel.CoordMenu().getText().replaceAll("\\d+\\:\\s\\d+\\s-\\s", "");
        assertTrue(bmParams[0].equals(tabParams[0]), "Wrong chromosome");
        assertTrue(bmParams[1].equals(tabParams[1]), "Wrong left coordinate");
        assertTrue(bmParams[2].equals(tabParams[2]), "Wrong right coordinate");
    }
}
