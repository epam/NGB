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

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.Selenide.screenshot;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.impl.Screenshot;
import static java.lang.String.format;
import static org.openqa.selenium.By.className;
import static org.testng.Assert.assertTrue;

import java.util.NoSuchElementException;

public class VariantsPanel implements AccessObject<VariantsPanel> {

    @Override
    public SelenideElement context() {
        return $(className("ui-grid-render-container-body"));
    }

    public SelenideElement findVariantByPosition(String position) {
        return context()
                .shouldBe(exist, enabled)
                .$$(className("ui-grid-row"))
                .stream()
                .filter(row -> row.$$x(".//div[contains(@role,'gridcell')]").get(4)
                        .has(text(position)))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
    }

    public VariantsPanel checkBackgroundColorRowByPosition(String position, String color) {
        assertTrue(findVariantByPosition(position).$x("./div[@role='row']/div")
                .shouldBe(enabled)
                .has(attribute("style", format("background-color: %s;", color))),
                format("Expected: %s, actual: %s ", color,
                        findVariantByPosition(position).$x("./div[@role='row']/div").getAttribute("style")));
        return this;
    }

    public BrowserPanel openVariantByPosition(String position) {
        findVariantByPosition(position).shouldBe(visible, enabled).click();
        return new BrowserPanel();
    }

    public VariantsPanel filterVariantsBy(String column, String value) {
        SelenideElement columnField = context()
                .shouldBe(exist, enabled)
                .$$(className("ui-grid-header-cell"))
                .stream()
                .filter(row -> row.$x(".//span[contains(@class,'ui-grid-header-cell-label')]")
                        .has(text(column)))
                .findFirst()
                .orElseThrow(NoSuchElementException::new)
                .$x(".//ngb-variants-table-filter").$x(".//input");
        setValue(columnField, value);
        columnField.pressEnter();
        return this;
    }

}
