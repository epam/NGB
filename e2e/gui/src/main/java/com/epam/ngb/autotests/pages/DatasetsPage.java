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
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.Selenide.$;
import com.codeborne.selenide.SelenideElement;
import com.epam.ngb.autotests.enums.Primitive;
import static com.epam.ngb.autotests.enums.Primitive.SEARCH;
import com.epam.ngb.autotests.menus.MetadataForm;
import static java.lang.String.format;
import static org.openqa.selenium.By.className;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;

public class DatasetsPage implements AccessObject<DatasetsPage> {

    private final Map<Primitive, SelenideElement> elements = initialiseElements(
            entry(SEARCH, $x("//input[@ng-model='$ctrl.searchPattern']"))
    );

    public DatasetsPage expandDataset(String datasetName) {
        $x(format("//span[contains(text(),'%s')]/../../span/ng-md-icon/..", datasetName))
                .shouldBe(enabled)
                .click();
        return this;
    }

    public SelenideElement getTrack(String trackName) {
        return $x(format(".//span[contains(normalize-space(),'%s')]", trackName));
    }

    public DatasetsPage setTrackCheckbox(String trackName, boolean isChecked) {
        SelenideElement checkbox = getTrack(trackName)
                .parent().$x(".//input[@type='checkbox']");
        if (checkbox.isSelected() == isChecked) {
            return this;
        }
        checkbox.click();
        return this;
    }

    public MetadataForm openMetadataForm(String trackName) {
        getTrack(trackName).shouldBe(exist).contextClick();
        $x(".//span[contains(normalize-space(),'Edit attributes')]").click();
        return new MetadataForm(this);
    }

    public DatasetsPage checkTrackTooltipContains(String trackName, Boolean isContains, String ... tooltips) {
        SelenideElement track =
                $x(format(".//div[contains(@class,'ivh-treeview-node-content') and contains(@title, '%s')]", trackName));
        Arrays.stream(tooltips)
                .forEach(tooltip -> assertEquals(track.getAttribute("title")
                        .contains(tooltip.toUpperCase()), isContains,
                        format("Attribute %s should be exist - %s", tooltip, isContains)));
        return this;
    }

    public DatasetsPage inputSearchValue(String searchValue) {
        return setValue(SEARCH, searchValue);
    }

    public DatasetsPage checkTracksNumber(int number) {
        assertTrue($(className("md-virtual-repeat-offsetter")).$$x("./div").size() == number);
        return this;
    }

    public DatasetsPage checkTrackMetadataAtSearch(String metadata) {
        return ensure(getTrack("CantonS.09-28.trim.dm606.realign.bam").parent()
                .$x(".//span[@class='metadata-info-row']"), text(metadata));
    }

    @Override
    public Map<Primitive, SelenideElement> elements() {
        return elements;
    }
}
