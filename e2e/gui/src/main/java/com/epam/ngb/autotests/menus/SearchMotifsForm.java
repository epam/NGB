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

import com.codeborne.selenide.Selectors;
import static com.codeborne.selenide.Selenide.$x;
import com.codeborne.selenide.SelenideElement;
import com.epam.ngb.autotests.enums.Primitive;
import static com.epam.ngb.autotests.enums.Primitive.CANCEL;
import static com.epam.ngb.autotests.enums.Primitive.SEARCH;
import com.epam.ngb.autotests.pages.BrowserPanel;
import com.epam.ngb.autotests.pages.MotifsPanel;
import com.epam.ngb.autotests.pages.PopupForm;

import java.util.Map;

public class SearchMotifsForm extends PopupForm<SearchMotifsForm, BrowserPanel> {

    public SearchMotifsForm(BrowserPanel parentAO) {
        super(parentAO);
    }

    @Override
    public SelenideElement context() {
        return $x(".//md-dialog[@aria-label='Motifs search']");
    }

    private final Map<Primitive, SelenideElement> elements = initialiseElements(
            entry(SEARCH, context().$x(".//button[@aria-label='find']")),
            entry(CANCEL, context().$x(".//button[@aria-label='cancel']"))
    );

    public SearchMotifsForm markSearchWholeReferenceCheckbox() {
        context().$x(".//md-checkbox").click();
        return this;
    }

    public SearchMotifsForm setPattern(String pattern) {
        context().$(Selectors.byText("Pattern"))
                .parent().$x(".//input").setValue(pattern);
        return this;
    }

    public SearchMotifsForm setTitle(String title) {
        context().$(Selectors.byText("Title"))
                .parent().$x(".//input").setValue(title);
        return this;
    }

    public MotifsPanel search() {
        click(SEARCH).parent();
        return new MotifsPanel();
    }

    public BrowserPanel cancel() {
        return click(CANCEL).parent();
    }

    @Override
    public Map<Primitive, SelenideElement> elements() {
        return elements;
    }
}
