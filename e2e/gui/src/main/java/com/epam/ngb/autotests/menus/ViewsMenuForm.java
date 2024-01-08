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
import static com.codeborne.selenide.Selenide.actions;
import com.codeborne.selenide.SelenideElement;
import com.epam.ngb.autotests.pages.NavigationPanel;
import com.epam.ngb.autotests.pages.PopupForm;
import static java.lang.Boolean.valueOf;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openqa.selenium.By.id;

public class ViewsMenuForm extends PopupForm<ViewsMenuForm, NavigationPanel> {

    private String menuID = "";

    public ViewsMenuForm(NavigationPanel parentAO, String menuID) {
        super(parentAO);
        this.menuID = menuID;
    }

    @Override
    public SelenideElement context() {
        return $(id(menuID));
    }

    public NavigationPanel selectMainMenuItem(ViewsMenuItems item, Boolean isSelected) {
        sleep(1, SECONDS);
        SelenideElement menuItem = context()
                .$$x(".//md-menu-item").filter(text(item.text)).first();
        if(valueOf(menuItem.$x(".//ng-md-icon[@ng-if='panel.displayed']").exists())
                .equals(isSelected)) {
            actions().moveToElement(parent().context(), 0, 0)
                    .click().build().perform();
            return parent();
        }
        menuItem.click();
        return parent();
    }

    public enum ViewsMenuItems {
        SESSIONS("Sessions"),
        BROWSER("Browser"),
        DATASETS("Datasets"),
        MOLECULAR_VIEWER("Molecular Viewer"),
        VARIANTS("Variants"),
        GENES("Genes"),
        BLAST("BLAST"),
        HEATMAP("Heatmap"),
        LINEAGE("Lineage"),
        PATHWAYS("Pathways"),
        TARGET_IDENTIFICATION("Target identification"),
        RESTORE_DEFAULT_LAYOUT("Restore Default Layout"),
        MOTIFS("Motifs");

        public final String text;

        ViewsMenuItems(String text) {
            this.text = text;
        }
    }
}
