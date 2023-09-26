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
package com.epam.ngb.autotests.utils;

import com.assertthat.selenium_shutterbug.core.Shutterbug;
import static com.assertthat.selenium_shutterbug.core.Shutterbug.shootElement;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.epam.ngb.autotests.utils.AppProperties.TEMPLATES_PATH;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Utils {

    public static void sleep(long duration, TimeUnit units) {
        Selenide.sleep(MILLISECONDS.convert(duration, units));
    }

    public static BufferedImage getExpectedImage(String fileName) throws IOException {
        return ImageIO.read(new File(format("%s%s.png", TEMPLATES_PATH, fileName)));
    }

    public static void takeScreenshot(SelenideElement track, String fileName) throws IOException {
        shootElement(getWebDriver(), track)
                .withName(fileName).save(TEMPLATES_PATH);
    }
}
