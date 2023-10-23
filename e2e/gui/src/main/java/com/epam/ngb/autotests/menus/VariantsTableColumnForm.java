package com.epam.ngb.autotests.menus;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.actions;
import com.codeborne.selenide.SelenideElement;
import com.epam.ngb.autotests.pages.PopupForm;
import com.epam.ngb.autotests.pages.TabsSelectionPanel;
import static org.openqa.selenium.By.id;

public class VariantsTableColumnForm extends PopupForm<VariantsTableColumnForm, TabsSelectionPanel> {

    private String menuID = "";

    public VariantsTableColumnForm(TabsSelectionPanel parentAO, String menuID) {
        super(parentAO);
        this.menuID = menuID;
    }

    @Override
    public SelenideElement context() {
        return $(id(menuID));
    }

    public TabsSelectionPanel selectVariantsTableColumnFormItem(String item, Boolean isSelected) {
        SelenideElement menuItem = context()
                .$$x(".//md-checkbox").filter(text(item)).first();
        if(!Boolean.valueOf(menuItem.$x(".//ng-md-icon[@ng-if='panel.displayed']").has(cssClass("md-checked")))
                .equals(isSelected)) {
            menuItem.click();
        }
        actions().moveToElement(parent().context(), 0, 0)
                .click().build().perform();
        return parent();
    }

}
