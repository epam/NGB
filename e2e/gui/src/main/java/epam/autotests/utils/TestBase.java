package epam.autotests.utils;

import com.epam.commons.PropertyReader;
import com.epam.jdi.uitests.web.settings.WebSettings;
import com.epam.jdi.uitests.web.testng.testRunner.TestNGBase;
import epam.autotests.page_objects.site.NGB_Site;
import org.joda.time.LocalDate;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.epam.jdi.uitests.core.settings.JDISettings.logger;
import static com.epam.jdi.uitests.web.selenium.elements.composite.WebSite.init;
import static epam.autotests.page_objects.site.NGB_Site.mainPage;


public abstract class TestBase extends TestNGBase {

    @BeforeSuite(alwaysRun = true)
    public void setUp() {
        try {
            if (PropertyReader.getProperty("run.type").toUpperCase().contains("REMOTE")) {
                WebSettings.useDriver(() -> mainPage.remoteDriver());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        init(NGB_Site.class);
        mainPage.open();
        logger.info("Run Tests");
    }

    public boolean isExpressionMatched(String string) {
        Pattern pattern = Pattern.compile("(\\/?\\w*)$");
        Matcher matcher = pattern.matcher(string);
        return matcher.find();
    }

    @AfterSuite(alwaysRun = true)
    public void killDriver() {
        WebSettings.getDriver().quit();
    }

    public static LocalDate getLocalDateFromString(String date, String delimiter) {
        int[] dt = Arrays.asList(date.split(delimiter)).stream().mapToInt(Integer::parseInt).toArray();
        return new LocalDate(dt[2], dt[0], dt[1]);
    }


    private static boolean compareStringArrays(String[] array1, String[] array2) {
        if (array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array2.length; i++) {
            if (!array1[i].toLowerCase().equals(array2[i].toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    public static boolean compareListOfArrays(List<String[]> list1, List<String[]> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        for (int i = 0; i < list1.size(); i++) {
            if (!TestBase.compareStringArrays(list1.get(i), list2.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static String CurrentDir() {
        File currentDirFile = new File(".");
        String helper = currentDirFile.getAbsolutePath();
        return helper.substring(0, helper.length() - 2);
    }
}