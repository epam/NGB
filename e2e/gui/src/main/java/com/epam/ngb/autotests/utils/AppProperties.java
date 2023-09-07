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

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Application properties
 */
public class AppProperties {

    public static final String CONF_PATH_PROPERTY = "com.epam.bfx.e2e.ui.property.path";
    static {
        String propFilePath = System.getProperty(CONF_PATH_PROPERTY, "default.conf");

        java.util.Properties conf = new java.util.Properties();
        try {
            conf.load(new FileInputStream(propFilePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ROOT_ADDRESS = conf.getProperty("e2e.ui.root.address");
        DEFAULT_TIMEOUT = Integer.parseInt(conf.getProperty("e2e.ui.default.timeout"));
        TEST_DATASET = conf.getProperty("e2e.ui.test.dataset");
        TEST_REFERENCE = conf.getProperty("e2e.ui.test.reference");
        TEMPLATES_PATH = conf.getProperty("e2e.ui.root.templates.path");
        RESULTS_PATH = conf.getProperty("e2e.ui.root.results.path");
    }

    public static final String ROOT_ADDRESS;
    public static final int DEFAULT_TIMEOUT;
    public static final String TEST_DATASET;
    public static final String TEST_REFERENCE;
    public static final String TEMPLATES_PATH;
    public static final String RESULTS_PATH;
}
