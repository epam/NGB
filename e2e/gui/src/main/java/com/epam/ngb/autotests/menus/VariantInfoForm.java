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

import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.screenshot;
import com.codeborne.selenide.SelenideElement;
import com.epam.ngb.autotests.enums.Primitive;
import static com.epam.ngb.autotests.enums.Primitive.CLOSE;
import com.epam.ngb.autotests.pages.PopupForm;
import com.epam.ngb.autotests.pages.VariantsPanel;
import static org.openqa.selenium.By.className;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VariantInfoForm extends PopupForm<VariantInfoForm, VariantsPanel> {

    @Override
    public SelenideElement context() {
        return $("ngb-variant-panel");
    }

    private final Map<Primitive, SelenideElement> elements = initialiseElements(
            entry(CLOSE, context().$x(".//button[@aria-label='Close']"))
    );

    public VariantInfoForm(VariantsPanel parentAO) {
        super(parentAO);
    }

    public VariantInfoForm checkPropertyValue(String property, String ... values) {
        screenshot("propertyScreen");
        List<String> listValues =
                context()
                    .find(byXpath(String.format(".//div[contains(@class, 'md-variant-property-title') and (.='%s')]",
                        property)))
                    .find(byXpath("following-sibling::div"))
                    .$$(className("md-variant-property-single-value"))
                    .texts();
        Arrays.stream(values).forEach(val ->
                assertTrue(listValues.contains(val), String.format("Value %s isn't found in '%s'", val, listValues.toString())));
        return this;
    }

    public VariantsPanel close() {
        click(CLOSE);
        return new VariantsPanel();
    }

    @Override
    public Map<Primitive, SelenideElement> elements() {
        return elements;
    }
}
