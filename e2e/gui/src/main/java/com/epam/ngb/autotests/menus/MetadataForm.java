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

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import com.codeborne.selenide.SelenideElement;
import com.epam.ngb.autotests.enums.Primitive;
import static com.epam.ngb.autotests.enums.Primitive.ADD;
import static com.epam.ngb.autotests.enums.Primitive.SAVE;
import static com.epam.ngb.autotests.enums.Primitive.TITLE;
import com.epam.ngb.autotests.pages.DatasetsPage;
import com.epam.ngb.autotests.pages.PopupForm;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.name;

import java.util.Map;
import java.util.NoSuchElementException;

public class MetadataForm extends PopupForm<MetadataForm, DatasetsPage> {

    @Override
    public SelenideElement context() {
        return $(name("metadataForm"));
    }
    private final Map<Primitive, SelenideElement> elements = initialiseElements(
            entry(TITLE, context().$(className("md-toolbar-tools")).$x("./h2")),
            entry(ADD, context().$(className("md-primary"))),
            entry(SAVE, context().$(className("save-metadata-btn")))
    );

    public MetadataForm(DatasetsPage parentAO) {
        super(parentAO);
    }

    public MetadataForm checkMetadataFormEmpty() {
        return ensure(context(), text("This file does not contain any attributes\n" +
                "Click the ADD button to create new one"));
    }

    public MetadataForm addAttribute(String key, String value, int number) {
        click(ADD);
        setValue($$(name("attrKey")).get(number-1), key);
        setValue($$(name("attrValue")).get(number-1), value);
        return this;
    }

    private SelenideElement getAttributeByKey(String key) {
        return $$(name("attrKey")).stream()
                .filter(el -> el.getAttribute("value").equals(key.toUpperCase()))
                .findFirst()
                .orElseThrow(NoSuchElementException::new)
                .parent().parent();
    }

    public MetadataForm updateAttribute(String oldKey, String newKey, String newValue) {
        SelenideElement element = getAttributeByKey(oldKey);
        setValue(element.$(name("attrKey")), newKey);
        setValue(element.$(name("attrValue")), newValue);
        return this;
    }

    public MetadataForm deleteAttribute(String key) {
        getAttributeByKey(key)
                .$x(".//button").click();
        return this;
    }

    public DatasetsPage save() {
        return click(SAVE).parent();
    }

    @Override
    public Map<Primitive, SelenideElement> elements() {
        return elements;
    }
}
