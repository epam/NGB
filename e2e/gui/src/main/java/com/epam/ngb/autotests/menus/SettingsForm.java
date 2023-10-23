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

import com.codeborne.selenide.Condition;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;
import com.codeborne.selenide.SelenideElement;
import com.epam.ngb.autotests.enums.Primitive;
import static com.epam.ngb.autotests.enums.Primitive.CANCEL;
import static com.epam.ngb.autotests.enums.Primitive.SAVE;
import com.epam.ngb.autotests.pages.NavigationPanel;
import com.epam.ngb.autotests.pages.PopupForm;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.name;

import java.util.Map;

public class SettingsForm extends PopupForm<SettingsForm, NavigationPanel> {

    public SettingsForm(NavigationPanel parentAO) {
        super(parentAO);
    }

    @Override
    public SelenideElement context() {
        return $(name("settingsForm"));
    }

    private final Map<Primitive, SelenideElement> elements = initialiseElements(
            entry(CANCEL, context().$(className("md-button"))),
            entry(SAVE, context().$(className("md-primary")))
    );

    public NavigationPanel save() {
        return click(SAVE).parent();
    }

    public SettingsForm openTab(SettingsTab settingsTab) {
        context().$x(format(".//md-tab-item[contains(.,'%s')]", settingsTab.text))
                .click();
        sleep(1, SECONDS);
        return this;
    }

    public Boolean isVariantsHighlighted() {
        return Boolean.valueOf(context()
                .$x(".//md-checkbox[@ng-model='ctrl.settings.isVariantsHighlighted']")
                        .shouldBe(exist, enabled)
                .getAttribute("aria-checked"));
    }

    public String getProfile() {
        return context().$(name("highlightProfile")).getText();
    }

    public SettingsForm checkVariantsHighlighted(Boolean isHighlight) {
        if(isVariantsHighlighted().equals(isHighlight)) {
            return this;
        }
        context().$x(".//md-checkbox[@ng-model='ctrl.settings.isVariantsHighlighted']").click();
        return this;
    }

    public SettingsForm selectProfile(String profile) {
        context().$(name("highlightProfile")).click();
        $x(format(".//md-option[@value='%s']", profile)).click();
        return this;
    }

    @Override
    public Map<Primitive, SelenideElement> elements() {
        return elements;
    }

    public enum SettingsTab {
        GENERAL("GENERAL"),
        ALIGNMENTS("ALIGNMENTS"),
        GFF_GTF("GFF/GTF"),
        VCF("VCF"),
        LINEAGE("LINEAGE"),
        CUSTOMIZE("Customize");

        private final String text;

        SettingsTab(String text) {
            this.text = text;
        }
    }
}
