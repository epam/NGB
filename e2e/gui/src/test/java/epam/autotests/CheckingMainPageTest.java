package epam.autotests;

import epam.autotests.utils.TestBase;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import static com.epam.jdi.uitests.core.preconditions.PreconditionsState.isInState;
import static epam.autotests.page_objects.enums.ProjectPagePreconditions.OPEN_SETTING;
import static epam.autotests.page_objects.enums.SettingTabs.*;
import static epam.autotests.page_objects.enums.Views.RESTORE_DEFAULT;
import static epam.autotests.page_objects.site.NGB_Site.projectPage;
import static epam.autotests.page_objects.site.NGB_Site.settingWindow;


public class CheckingMainPageTest extends TestBase {

    @Test(priority = 0)
    public void checkingSettingsContent() {
        isInState(OPEN_SETTING);
        if (settingWindow.isDisplayed()) {
            settingWindow.openTab(ALIGNMENTS);
            settingWindow.openTab(GFF_GTF);
            settingWindow.openTab(CUSTOMIZE);
            settingWindow.openTab(GENERAL);
            settingWindow.close();
        }
        System.out.println("=== CheckingMainPageTest.checkingDataPresence(); @Test(priority=0)");
    }

    @AfterClass(alwaysRun = true)
    public void resetBrowser() {
        projectPage.openPanel(RESTORE_DEFAULT);
        System.out.println("=== CheckingMainPageTest.resetBrowser(); @AfterClass(alwaysRun=true)");
    }
}
