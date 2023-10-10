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
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.not;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import com.codeborne.selenide.SelenideElement;
import com.epam.ngb.autotests.enums.Primitive;
import com.epam.ngb.autotests.utils.Utils;
import static java.util.stream.Collectors.toMap;
import org.openqa.selenium.By;
import static org.openqa.selenium.By.tagName;
import org.openqa.selenium.Keys;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface AccessObject <ELEMENT_TYPE extends AccessObject>{

    default Map<Primitive, SelenideElement> elements() {
        return Collections.emptyMap();
    }

    default SelenideElement context() {
        return $(tagName("body"));
    }

    default SelenideElement get(Primitive primitive) {
        return Optional.ofNullable(elements().get(primitive))
                .orElseThrow(() ->
                        new NoSuchElementException(String.format(
                                "Primitive %s is not defined in %s.", primitive, this.getClass().getSimpleName())));
    }

    default Entry entry(Primitive primitive, SelenideElement element) {
        return new Entry(primitive, element);
    }

    default Map<Primitive, SelenideElement> initialiseElements(Entry... entries) {
        return Arrays.stream(entries).collect(toMap(Entry::getPrimitive, Entry::getElement));
    }

    default Map<Primitive, SelenideElement> initialiseElements(Map<Primitive, SelenideElement> elements, Entry... entries) {
        Map<Primitive, SelenideElement> map = new HashMap<>(elements);
        Arrays.stream(entries).forEach(entry -> map.put(entry.getPrimitive(), entry.getElement()));
        return map;
    }

    default ELEMENT_TYPE click(Primitive primitive) {
        get(primitive).shouldBe(visible, enabled).click();
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE click(final By qualifier) {
        context().find(qualifier).shouldBe(visible, enabled).click();
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE click(final By target, final By scope) {
        $(scope).find(target).shouldBe(visible, enabled).click();
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE ensure(final By qualifier, final Condition... conditions) {
        context().find(qualifier).should(conditions);
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE ensure(final SelenideElement element, final Condition... conditions) {
        element.should(conditions);
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE ensure(final Primitive primitive, final Condition... conditions) {
        get(primitive).should(conditions);
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE ensureVisible(final SelenideElement... elements) {
        Arrays.stream(elements).forEach(el -> ensure(el, visible));
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE ensureVisible(final Primitive... elements) {
        Arrays.stream(elements).forEach(el -> ensure(el, visible));
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE ensureNotVisible(final SelenideElement... elements) {
        Arrays.stream(elements).forEach(el -> ensure(el, not(visible)));
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE ensureNotVisible(final Primitive... elements) {
        Arrays.stream(elements).forEach(el -> ensure(el, not(visible)));
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE ensureDisable(final SelenideElement... elements) {
        Arrays.stream(elements).forEach(el -> ensure(el, disabled));
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE ensureDisable(final Primitive... elements) {
        Arrays.stream(elements).forEach(el -> ensure(el, disabled));
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE ensureAll(final Condition condition, final Primitive... elements) {
        Arrays.stream(elements).forEach(el -> ensure(el, condition));
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE sleep(final long duration, final TimeUnit units) {
        Utils.sleep(duration, units);
        return (ELEMENT_TYPE) this;
    }

    default ELEMENT_TYPE setValue(final By qualifier, final String value) {
        return setValue($(qualifier), value);
    }

    default ELEMENT_TYPE setValue(Primitive primitive, String value) {
        return setValue(get(primitive), value);
    }

    default ELEMENT_TYPE setValue(final SelenideElement element, final String value) {
        element.shouldBe(visible, enabled)
                .sendKeys(Keys.chord(Keys.CONTROL, "a"), value);
        return (ELEMENT_TYPE) this;
    }

    class Entry {
        private final Primitive primitive;
        private final SelenideElement selenideElement;

        public Entry(Primitive primitive, SelenideElement selenideElement) {
            this.primitive = primitive;
            this.selenideElement = selenideElement;
        }

        public Primitive getPrimitive() {
            return primitive;
        }

        public SelenideElement getElement() {
            return selenideElement;
        }
    }
}
