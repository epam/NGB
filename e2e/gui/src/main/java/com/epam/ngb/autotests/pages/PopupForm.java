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

public abstract class PopupForm<ELEMENT_AO extends PopupForm<ELEMENT_AO, PARENT_AO>, PARENT_AO>
        implements AccessObject<ELEMENT_AO> {

    private final PARENT_AO parentAO;

    public PopupForm(PARENT_AO parentAO) {
        this.parentAO = parentAO;
    }

    public PARENT_AO saveIfNeeded() {
        if ($x("//span[contains(normalize-space(),'Save')]").isEnabled()) {
            $x("//span[contains(normalize-space(),'Save')]").click();
        }else{
            $x("//span[contains(normalize-space(),'Cancel')]").click();
        }
        return parentAO;
    }

    public PARENT_AO parent() {
        return parentAO;
    }
}
