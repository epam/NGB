/*
 * Copyright 2023 EPAM Systems, Inc. (https://www.epam.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.ngb.autotests;

import com.codeborne.selenide.Browsers;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import com.epam.ngb.autotests.utils.AppProperties;
import static com.epam.ngb.autotests.utils.AppProperties.ROOT_ADDRESS;
import com.epam.ngb.autotests.utils.TestCase;
import static com.epam.ngb.autotests.utils.Utils.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.io.TemporaryFilesystem;
import org.openqa.selenium.remote.CapabilityType;
import org.testng.ITest;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;

public class AbstractNgbTest implements ITest {

    protected String methodName = "";

    @BeforeMethod
    public void setUp() {
        Configuration.timeout= AppProperties.DEFAULT_TIMEOUT;
        Configuration.browser= Browsers.CHROME;

        System.setProperty("webdriver.chrome.driver","/usr/local/bin/chromedriver");
        ChromeOptions chromeOptions=new ChromeOptions();
        chromeOptions.addArguments("--window-size=1920,1080");
        chromeOptions.addArguments("--remote-allow-origins=*");
        chromeOptions.setCapability(CapabilityType.PAGE_LOAD_STRATEGY, "normal");

        WebDriver webDriver=new ChromeDriver(chromeOptions);
        WebDriverRunner.setWebDriver(webDriver);
        Selenide.clearBrowserCookies();
        Selenide.open(ROOT_ADDRESS);

        sleep(3, SECONDS);
    }

    @BeforeMethod(alwaysRun = true)
    public void setMethodName(Method method, Object[] testData) {
        if (method.isAnnotationPresent(TestCase.class)) {
            final TestCase testCaseAnnotation = method.getAnnotation(TestCase.class);
            for (final String testCase : testCaseAnnotation.value()) {
                this.methodName = String.format("%s - %s", method.getName(), testCase);
            }
        } else {
            this.methodName = method.getName();
        }
    }

    public void restartBrowser(final String address) {
        Selenide.clearBrowserCookies();
        Selenide.open(address);
    }

    @Override
    public String getTestName() {
        return this.methodName;
    }

    @AfterMethod
    public static void closeDriverAfterMethod() {
        getWebDriver().close();
    }

    @AfterClass(alwaysRun=true)
    public static void tearDownAfterClass() {
        final TemporaryFilesystem tempFS = TemporaryFilesystem.getDefaultTmpFS();
        tempFS.deleteTemporaryFiles();
        getWebDriver().quit();
    }

    @AfterMethod(alwaysRun = true)
    public void logging(ITestResult result) {
        StringBuilder testCasesString = new StringBuilder();

        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        if (method.isAnnotationPresent(TestCase.class)) {
            TestCase testCaseAnnotation = method.getAnnotation(TestCase.class);
            for (String testCase : testCaseAnnotation.value()) {
                testCasesString.append(testCase).append(" ");
            }
        }

        System.out.println(String.format("%s::%s [ %s]: %s",
                result.getTestClass().getRealClass().getSimpleName(),
                result.getMethod().getMethodName(),
                testCasesString.toString(),
                getStatus(result.getStatus()))
        );
    }

    private String getStatus(final int numericStatus) {
        String status;
        switch (numericStatus) {
            case 1:
                status = "SUCCESS";
                break;
            case 3:
                status = "SKIP";
                break;
            default:
                status = "FAILS";
                break;
        }
        return status;
    }

}
