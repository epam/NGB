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

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Selenide.$x;
import com.codeborne.selenide.SelenideElement;
import static java.lang.String.format;

public class DatasetsPage implements AccessObject<DatasetsPage> {

    public DatasetsPage expandDataset(String datasetName) {
        $x(format("//span[contains(text(),'%s')]/../../span/ng-md-icon/..", datasetName))
                .shouldBe(enabled)
                .click();
        return this;
    }

    public DatasetsPage setTrackCheckbox(String trackName, boolean isChecked) {
        SelenideElement checkbox = $x(format(".//span[contains(normalize-space(),'%s')]", trackName))
                .parent().$x(".//input[@type='checkbox']");
        if (checkbox.isSelected() == isChecked) {
            return this;
        }
        checkbox.click();
        return this;
    }
}
