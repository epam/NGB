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
package com.epam.ngb.autotests.enums;

public enum Colors {

    RED("rgb(255, 0, 0)"),
    GREEN("rgb(0, 128, 0)"),
    LIGHTBLUE("rgb(173, 216, 230)"),
    BLUE("rgb(0, 131, 255)"),
    BROWN("rgb(255, 0, 178)"),
    PINK("rgb(206, 99, 45)"),
    LIGHTPINK("rgb(255, 192, 203)"),
    YELLOW("rgb(255, 255, 0)");

    public final String value;

    Colors(final String value) {
        this.value = value;
    }
}
