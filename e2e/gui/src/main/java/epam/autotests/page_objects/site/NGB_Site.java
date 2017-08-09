package epam.autotests.page_objects.site;

import com.epam.jdi.uitests.web.selenium.elements.base.Element;
import com.epam.jdi.uitests.web.selenium.elements.composite.WebSite;
import com.epam.jdi.uitests.web.selenium.elements.pageobjects.annotations.JPage;
import com.epam.jdi.uitests.web.settings.WebSettings;
import epam.autotests.page_objects.forms.Confirmation;
import epam.autotests.page_objects.pages.MainPage;
import epam.autotests.page_objects.pages.ProjectPage;
import epam.autotests.page_objects.sections.Header;
import epam.autotests.page_objects.sections.SettingsModalWindow;
import epam.autotests.page_objects.sections.VariationInfoModalWindow;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.epam.jdi.uitests.web.selenium.elements.composite.CheckPageTypes.CONTAINS;
import static com.epam.jdi.uitests.web.settings.WebSettings.getJSExecutor;


public class NGB_Site extends WebSite {

    @JPage(url = "/#/")
    public static MainPage mainPage;

    @JPage(urlTemplate = "/#/", urlCheckType = CONTAINS)
    public static ProjectPage projectPage;

    @FindBy(css = ".lm_header")
    public static Header header;

    @FindBy(xpath = "//md-dialog[@aria-label='Settings']")
    public static SettingsModalWindow settingWindow;

    @FindBy(xpath = "//md-dialog[@aria-label = 'variant']")
    public static VariationInfoModalWindow variationInfoWindow;

    @FindBy(xpath = "//md-dialog")
    public static Confirmation confirmWindow;


    @FindBy(xpath = "/html/body/md-backdrop")
    public static Element emptySpace;


    public static boolean compareTwoStringLists(List<String> actualList, List<String> expectedList) {
        List<String> copyOfActualList = new ArrayList<>(actualList);
        List<String> copyOfExpectedList = new ArrayList<>(expectedList);
        Collections.sort(copyOfActualList);
        Collections.sort(copyOfExpectedList);
        System.out.println("Comparison of two sorted lists: \nActualList  " + copyOfActualList.toString() + "\nExpectedList" + copyOfExpectedList.toString());
        return copyOfExpectedList.equals(copyOfActualList);
    }

    public static boolean isListSorted(List list, boolean ascending) {
        List sortedList = new ArrayList<>(list);
        Collections.sort(sortedList);
        if (!ascending)
            Collections.reverse(sortedList);
        return sortedList.equals(list);
    }

    public static void openPageByUrl(String stringUrl) {
        WebSettings.getDriver().manage().timeouts().setScriptTimeout(5, TimeUnit.SECONDS);
        getJSExecutor().executeScript("window.open(\'" + WebSettings.getDriver().getCurrentUrl() + stringUrl + "').focus();");
        ArrayList<String> tabs = new ArrayList<String>(WebSettings.getDriver().getWindowHandles());
        WebSettings.getDriver().switchTo().window(tabs.get(tabs.size() - 1));

    }
}
