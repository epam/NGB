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

import static com.codeborne.selenide.Selenide.$x;
import com.epam.ngb.autotests.menus.VariantsTableColumnForm;
import com.epam.ngb.autotests.menus.ViewsMenuForm.ViewsMenuItems;
import static java.lang.String.format;

public class TabsSelectionPanel implements AccessObject<TabsSelectionPanel> {

    public TabsSelectionPanel selectPanel(ViewsMenuItems item) {
        $x(format("//span[contains(@class,'lm_title') and contains(., '%s')]", item.text))
                .parent().click();
        return this;
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
