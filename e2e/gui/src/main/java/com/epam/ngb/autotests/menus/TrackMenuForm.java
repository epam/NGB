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
package com.epam.ngb.autotests.menus;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selectors.byId;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.Selenide.actions;
import com.codeborne.selenide.SelenideElement;
import com.epam.ngb.autotests.pages.BrowserPage;
import com.epam.ngb.autotests.pages.PopupForm;
import static java.lang.Boolean.valueOf;

public class TrackMenuForm extends PopupForm<TrackMenuForm, BrowserPage> {

    private String menuID = "";
    private SelenideElement trackElement;

    public TrackMenuForm(BrowserPage parentAO, String menuID) {
        super(parentAO);
        this.menuID = menuID;
    }

    public BrowserPage selectOptionWithCheckbox(String option, boolean isChecked) {
        $(byId(menuID)).should(cssClass("md-clickable"));
        SelenideElement menuOption = $(byId(menuID)).$(byText(option)).parent().parent();
        if(valueOf(menuOption.getAttribute("aria-checked")) != isChecked) {
            menuOption.click();
        } else {
            actions().moveToElement(trackElement, 0, 0)
                    .click().build().perform();
        }
        $(byId(menuID)).should(cssClass("md-leave"))
                .shouldNot(cssClass("md-active"));
        return new BrowserPage();
    }

    public ResizeForm selectOptionWithAdditionalMenu(String option) {
        $(byId(menuID)).should(cssClass("md-clickable"))
                .$(byText(option)).parent().parent().click();
        $(byId(menuID)).should(cssClass("md-leave")).shouldNot(cssClass("md-active"));
        return new ResizeForm(new BrowserPage());
    }

    public class ResizeForm extends PopupForm<ResizeForm, BrowserPage> {

        SelenideElement dialog = $x("//md-dialog[@aria-label='track resize settings']");
        public ResizeForm(BrowserPage parentAO) {
            super(parentAO);
        }

        public ResizeForm setTrackHeight(String height) {
            dialog.$x(".//input[@name='height']").setValue(height);
            return this;
        }
    }


}
