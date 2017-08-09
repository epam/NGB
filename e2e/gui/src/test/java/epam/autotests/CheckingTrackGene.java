package epam.autotests;

import com.epam.commons.Timer;
import epam.autotests.utils.TestBase;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static epam.autotests.page_objects.enums.Views.RESTORE_DEFAULT;
import static epam.autotests.page_objects.site.NGB_Site.*;


public class CheckingTrackGene extends TestBase {

    @BeforeClass
    public void preparations() {
        projectPage.closeAllTracks();
        projectPage.openPanel(RESTORE_DEFAULT);
        mainPage.datasetsPanel.select("/SV_Sample1", true);
        System.out.println("=== CheckingTrackGene.preparation(); @BeforeClass");
    }

    @Test(priority = 3)
    public void checkingGeneSearchByNameBGLAP() {
        header.chooseBookmark("BGLAP");
        String coordOfKRAS = projectPage.browserPanel.CoordMenu().getText();
        Assert.assertEquals(coordOfKRAS, "1: 156241962 - 156243321");
        System.out.println("=== CheckingTrackGene.checkingGeneSearchByNameBGLAP(); @Test(priority=3)");
    }

    @Test(priority = 4)
    public void checkingGeneSearchByNameALK() {
        header.chooseBookmark("ALK");
        String coordOfKRAS = projectPage.browserPanel.CoordMenu().getText();
        Assert.assertEquals(coordOfKRAS, "2: 29192774 - 29921566");
        System.out.println("=== CheckingTrackGene.checkingGeneSearchByNameALK(); @Test(priority=4)");
    }

    @Test(priority = 5)
    public void checkingGeneSearchByNameFOXP1() {
        header.chooseBookmark("FOXP1");
        String coordOfKRAS = projectPage.browserPanel.CoordMenu().getText();
        Assert.assertEquals(coordOfKRAS, "3: 70954693 - 71583989");
        System.out.println("=== CheckingTrackGene.checkingGeneSearchByNameFOXP1(); @Test(priority=5)");
    }


    @Test(priority = 6)
    public void checkingGeneSearchByNameELN() {
        header.chooseBookmark("ELN");
        String coordOfKRAS = projectPage.browserPanel.CoordMenu().getText();
        Assert.assertEquals(coordOfKRAS, "7: 74027789 - 74069907");
        System.out.println("=== CheckingTrackGene.checkingGeneSearchByNameELN(); @Test(priority=6)");
    }

    @Test(priority = 7)
    public void checkingGeneSearchByNameH1F0() {
        header.chooseBookmark("H1F0");
        String coordOfKRAS = projectPage.browserPanel.CoordMenu().getText();
        Assert.assertEquals(coordOfKRAS, "22: 37805093 - 37807436");
        System.out.println("=== CheckingTrackGene.checkingGeneSearchByNameH1F0(); @Test(priority=7)");
    }

    @AfterClass(alwaysRun = true)
    public void resetFiltration() {
        projectPage.closeAllTracks();
        projectPage.openPanel(RESTORE_DEFAULT);
        System.out.println("=== CheckingTrackGene.checkingTracksSearchUpperCase(); @AfterClass(alwaysRun=true)");
    }
}
