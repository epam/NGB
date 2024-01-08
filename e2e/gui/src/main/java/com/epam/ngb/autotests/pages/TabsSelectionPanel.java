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

import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.$x;
import com.codeborne.selenide.SelenideElement;
import com.epam.ngb.autotests.menus.VariantsTableColumnForm;
import com.epam.ngb.autotests.menus.ViewsMenuForm.ViewsMenuItems;
import static java.lang.String.format;
import static org.openqa.selenium.By.className;

public class TabsSelectionPanel implements AccessObject<TabsSelectionPanel> {

    public SelenideElement rightSelectionPanel() {
        return $$(className("lm_stack")).get(1);
    }

    public TabsSelectionPanel selectPanel(ViewsMenuItems item) {
        SelenideElement tab = rightSelectionPanel()
                .$x(format(".//span[contains(@class,'lm_title') and contains(., '%s')]", item.text)).parent();
        if (tab.isDisplayed()) {
            tab.click();
        } else {
            rightSelectionPanel().$(className("lm_tabdropdown")).click();
            $x(format("//li[contains(@class,'lm_tab') and contains(., '%s')]", item.text)).click();
        }
        return this;
    }

    public TabsSelectionPanel tabIsExists(ViewsMenuItems item) {
        SelenideElement tab = rightSelectionPanel()
                .$x(format(".//span[contains(@class,'lm_title') and contains(., '%s')]", item.text)).parent();
        return ensureVisible(tab);
    }

    public VariantsTableColumnForm openNGBVariantsTableColumn() {
        $x(".//ngb-variants-table-column//button").click();
        return new VariantsTableColumnForm(this, getMenuID());
    }

    private String getMenuID() {
        return context().$x(".//ngb-variants-table-column//button")
                .getAttribute("aria-owns");
    }
}
