package epam.autotests.page_objects.panels;

import com.epam.jdi.uitests.web.selenium.elements.base.Element;
import com.epam.jdi.uitests.web.selenium.elements.common.Button;
import com.epam.jdi.uitests.web.selenium.elements.common.Label;
import com.epam.jdi.uitests.web.selenium.elements.common.Link;
import com.epam.jdi.uitests.web.selenium.elements.complex.Elements;
import com.epam.jdi.uitests.web.selenium.elements.composite.Section;
import com.epam.web.matcher.junit.Assert;
import epam.autotests.page_objects.general.HistogramElement;
import epam.autotests.page_objects.general.Panel;
import epam.autotests.page_objects.general.Track;
import epam.autotests.page_objects.sections.VariantDensity;
import epam.autotests.page_objects.sections.VariantsType;
import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;
import org.testng.asserts.SoftAssert;

import java.util.ArrayList;
import java.util.List;

import static epam.autotests.page_objects.enums.TrackType.BAM;


/**
 * Created by Vsevolod_Adrianov on 8/5/2016.
 * <br>
 * Refactored by Aksenov Oleg in October 2016
 */

public class BrowserPanel extends Panel {
    @FindBy(css = ".ngb-variant-density-diagram")
    private VariantDensity densityDiagram;

    @FindBy(css = ".ngb-variant-type-diagram")
    private VariantsType typeDiagram;

    //    @FindBy(xpath = ".//div[@id = 'densityChart']")
    @FindBy(css = ".ngb-variant-density-diagram .nv-group")
    private Section densityChart;

    //    @FindBy(xpath = ".//ngb-variant-density-diagram//*[local-name()='svg']/*[local-name()='g']/*[local-name()='g']/*[local-name()='rect']")
    @FindBy(xpath = "//ngb-variant-density-diagram//*[contains(@class,'nv-barsWrap')]//*[contains(@class,'discreteBar')]")
    private Elements<HistogramElement> variationHistogramElements;

    @FindBy(xpath = ".//div[@ng-show]//ngb-project-summary//h2")
    private Label openedProjectTitle;

    @FindBy(xpath = ".//div[@ng-show]//ngb-project-summary//md-content/div")
    private Elements<Section> attachedToProject;

    @FindBy(xpath = ".//div[@ng-show]//div[@id='typeChart']")
    private Section variantsTypes;

    @FindBy(xpath = ".//div[@ng-show]//div[@id = 'qualityChart']")
    private Section variantsQuality;

    @FindBy(css = ".lm_tab ngb-coordinates")
    private Label coordinates;

    @FindBy(xpath = ".//ngb-tracks-view//section/button//span[text()='+']/parent::button")
    private Button zoomIn;

    @FindBy(xpath = ".//ngb-tracks-view//section/button//span[text()='-']/parent::button")
    private Button zoomOut;


    @FindBy(xpath = ".//ngb-tracks-view//button[@aria-label='save screenshot']") //screenshot
    private Button makeScreenshotBtn;

    @FindBy(xpath = ".//ngb-track")
    private Elements<Track> tracks;

    @FindBy(xpath = ".//ancestor::div[@class = 'lm_items']/preceding-sibling::div[@class='lm_header']//li[@title='Browser']//a")
    //chromosome
    private Elements<Link> tabTitle;

    //    @FindBy(css = "button[aria-label='menu'] .ng-binding")
    @FindBy(css = "button[aria-label='menu'] .capitalize")
    private Elements<Link> menuItems;

    //  @FindBy(css = ".md-active md-menu-content[role='menu'] md-menu-item .md-label span:not(.info-hotkey)")
    @FindBy(xpath = ".//ancestor::body//*[contains(@class,'md-active')]//md-menu-item//*[contains(@class,'track-menu-button') or contains(@class,'md-label')]/span[1]")
    private Elements<Button> menuSubitems;

    public int weight(int index) {
        return densityDiagram.getWeight(index);
    }

    public int weightOfType(int index) {
        return typeDiagram.getWeight(index);
    }

    public int countsChromosomes() {
        return densityDiagram.getNumberChromosome();
    }

    public int countsOfTypes() {
        return typeDiagram.NumTypes();
    }

    public void onViewOfChromosome(int index) {
        densityDiagram.selectChromosome(index);
    }

    public String nameOfChromosome(int index) {
        return densityDiagram.nameChromosome(index);
    }

    public String nameOfType(int index) {
        return typeDiagram.NameOfType(index);
    }

    public int getCountOfOpenedTracks() {
        return tracks.size();
    }

    public List<String> getTracksTitle(String sType) {
        List<String> openedTracks = new ArrayList<>();

        for (Track track : tracks) {
            String st = track.getTrackAllText();
            if (st.contains(sType)) {
                st = st.substring(4, st.length());
                openedTracks.add(st);
            }
        }

        return openedTracks;
    }

    public String getChrTitle() {
        String[] cc;
        String ss = tabTitle.get(0).getText();
        cc = ss.split("(:)");
        return cc[1];
    }

    public Link ChrMenu() {
        return tabTitle.get(0);
    }

    public Link CoordMenu() {
        return tabTitle.get(1);
    }

    public String getTabTitle() {
        String sTitle = this.get(By.xpath(".//ancestor::div[@class = 'lm_items']/preceding-sibling::div[@class='lm_header']//li[@title='Browser']//div")).getText();
        if (!sTitle.equals("BROWSER"))
            sTitle = sTitle.replaceAll(":\\s\\d+\\s-\\s\\d+", "");

        return sTitle;
    }

    public String getCoordinates() {
        String coords = this.get(By.xpath(".//ancestor::div[@class = 'lm_items']/preceding-sibling::div[@class='lm_header']//li[@title='Browser']//a")).getText();
        coords = coords.replaceAll("\\w+:\\s", "");
        return coords;
    }

    public void increaseZoom(int times) {
        for (int i = 0; i < times; i++) {
            zoomOut.clickCenter();
        }
    }

    public void decreaseZoom(int times) {
        for (int i = 0; i < times; i++) {
            zoomIn.clickCenter();
        }
    }



    public void checkDefaultView() {
        Assert.isTrue(getTabTitle().equals("BROWSER"));

        SoftAssert soft_assert = new SoftAssert();
        for (Section section : attachedToProject) {
            soft_assert.assertTrue(section.isDisplayed(), section.get(By.xpath(".//h5")).getText() + " isn't displayed");
        }
        soft_assert.assertTrue(variantsTypes.isDisplayed());
        soft_assert.assertTrue(variantsQuality.isDisplayed());
        soft_assert.assertAll();
    }

    private Element getHistogramElement(String name) {
        for (int i = 0; i < variationHistogramElements.size(); i++) {
            if (variationHistogramElements.get(i).get(By.xpath("./parent::*[local-name()='g']/parent::*[local-name()='g']/*[local-name()='text'][" + (i + 1) + "]")).getText().equals(name))
                return variationHistogramElements.get(i);
        }
        return null;
    }

    public void clickOnHistogram(int number) {
        variationHistogramElements.get(number).clickCenter();

    }

    public void clickOnHistogram(String name) {
        getHistogramElement(name).clickCenter();
    }

    public void checkOpenedChromosome(String name) {
        SoftAssert soft_assert = new SoftAssert();
        soft_assert.assertTrue(getTabTitle().equals(name));

        soft_assert.assertAll();
    }

    public List<String> checkTrackMenuItem() {
        List<String> trackMenuState = new ArrayList<>();
        int i = 0;
        while (i != -1) {
            try {
                trackMenuState.add(menuItems.get(i).getText());
                i++;
            } catch (Exception e) {
                i = -1;
            }

        }
        return trackMenuState;
    }

    public void selectTrackMenuItem(String svtype, String TrackMenuName, String menuItem) {
        String trackMenuNameCasted = TrackMenuName;
        switch (svtype) {
            case "BAM":
                switch (TrackMenuName) {
                    case "Color mode":
                        trackMenuNameCasted = menuItems.get(0).getText();
                        break;
                    case "Group":
                        trackMenuNameCasted = menuItems.get(1).getText();
                        break;
                    case "Reads view":
                        trackMenuNameCasted = menuItems.get(2).getText();
                        break;
                }
                break;
            case "GENE":
                switch (TrackMenuName) {
                    case "Transcript view":
                        trackMenuNameCasted = menuItems.get(0).getText();
                        break;
                }
            case "VCF":
                switch (TrackMenuName) {
                    case "Variants view":
                        trackMenuNameCasted = menuItems.get(0).getText();
                        break;
                }
                break;
        }

        menuItems.get(trackMenuNameCasted).click();
        List<String> tracksList = getTracksTitle(svtype);
        menuSubitems.get(menuItem).click();
        menuSubitems.waitVanished();
    }
}
