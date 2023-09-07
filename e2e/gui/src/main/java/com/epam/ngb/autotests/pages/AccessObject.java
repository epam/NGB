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
package com.epam.ngb.autotests.pages;

import com.codeborne.selenide.Condition;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.not;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import com.codeborne.selenide.SelenideElement;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.epam.ngb.autotests.utils.AppProperties.DEFAULT_TIMEOUT;
import com.epam.ngb.autotests.utils.Utils;
import static java.time.Duration.ofSeconds;
import org.openqa.selenium.By;
import static org.openqa.selenium.By.tagName;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public interface AccessObject <ELEMENT_TYPE extends AccessObject>{

    default SelenideElement context() {
        return $(tagName("body"));
    }

    default ELEMENT_TYPE click(final By qualifier) {
        context().find(qualifier).shouldBe(visible, enabled).click();
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE click(final By target, final By scope) {
        $(scope).find(target).shouldBe(visible, enabled).click();
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE ensure(final By qualifier, final Condition... conditions) {
        context().find(qualifier).should(conditions);
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE ensure(final SelenideElement element, final Condition... conditions) {
        element.should(conditions);
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE ensureVisible(final SelenideElement... elements) {
        Arrays.stream(elements).forEach(el -> ensure(el, visible));
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE ensureNotVisible(final SelenideElement... elements) {
        Arrays.stream(elements).forEach(el -> ensure(el, not(visible)));
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE ensureDisable(final SelenideElement... elements) {
        Arrays.stream(elements).forEach(el -> ensure(el, disabled));
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE sleep(final long duration, final TimeUnit units) {
        Utils.sleep(duration, units);
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE waitForReady() {
        new WebDriverWait(getWebDriver(), ofSeconds(DEFAULT_TIMEOUT)).until(
                webDriver -> ((JavascriptExecutor) webDriver)
                        .executeScript("return document.readyState").equals("complete"));
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE waitForLoad() {
//        ExpectedCondition<Boolean> pageLoadCondition = new
//                ExpectedCondition<Boolean>() {
//                    public Boolean apply(WebDriver driver) {
//                        return ((JavascriptExecutor)getWebDriver()).executeScript("return document.readyState").equals("complete");
//                    }
//                };
//        WebDriverWait wait = new WebDriverWait(getWebDriver(), ofSeconds(DEFAULT_TIMEOUT));
//        wait.until(pageLoadCondition);

        try{
            // Javascript Executor for ready state
            JavascriptExecutor j = (JavascriptExecutor)getWebDriver();
            if (j.executeScript("return document.readyState").toString().equals("complete")){
                System.out.println("Page in ready state"); }
        } catch(Exception exe) {
            System.out.println("Page not in ready state");
        }


        return (ELEMENT_TYPE) this;
    }
}
