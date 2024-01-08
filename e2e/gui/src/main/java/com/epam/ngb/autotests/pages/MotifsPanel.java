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

import com.codeborne.selenide.Condition;
import static com.codeborne.selenide.Condition.exist;
import com.codeborne.selenide.Selectors;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import com.codeborne.selenide.SelenideElement;
import static java.lang.String.format;
import static org.openqa.selenium.By.className;
import static org.testng.Assert.assertTrue;

import java.time.Duration;

public class MotifsPanel implements AccessObject<MotifsPanel> {

    @Override
    public SelenideElement context() {
        return $(className("motifs-results-table"));
    }

    public MotifsPanel waitUntilTableAppears() {
        $(byText("Loading results..."))
                .shouldNotBe(exist, Duration.ofMinutes(2));
        return this;
    }

    public MotifsPanel checkRowsNumber(int expectedCount) {
        int count = context().$(className("ui-grid-canvas"))
                .$$(className("ui-grid-row")).size();
        assertTrue(context().$(className("ui-grid-canvas"))
                .$$(className("ui-grid-row")).size() == expectedCount,
                format("Table contains %s rows instead of %s expected", count, expectedCount));
        return this;
    }
}
