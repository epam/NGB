/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.epam.catgenome.common;

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.epam.catgenome.controller.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Source:      AbstractControllerTest.java
 * Created:     10/21/15, 2:20 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code AbstractControllerTest} represents a test template, provides required procedures
 * to setup and configure each test for application controllers.
 *
 * @author Denis Medvedev
 */
public abstract class AbstractControllerTest extends AbstractJUnitTest {

    protected static final String JPATH_STATUS = "$.status";
    protected static final String JPATH_PAYLOAD = "$.payload";
    protected static final String JPATH_MESSAGE = "$.message";

    protected static final String UPLOAD_FILE_PARAM = "saveFile";
    protected static final String REFERENCE_ID_PARAM = "referenceId";

    protected static final String EXPECTED_CONTENT_TYPE = "application/json;charset=UTF-8";

    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    private TypeFactory typeFactory;

    @Autowired
    protected WebApplicationContext wac;

    @Before
    public void setup() throws Exception {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // checks that all required dependencies are provided.
        assertNotNull("WebApplicationContext isn't provided.", wac);
        assertNotNull("ObjectMapper isn't provided.", objectMapper);

        typeFactory = TypeFactory.defaultInstance();
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @After
    public void tearDown() throws IOException {
        System.gc();
        FileUtils.deleteDirectory(new File(fileManager.getBaseDirPath()));
    }

    protected final MockMvc mvc() {
        return mockMvc;
    }

    protected final JsonMapper getObjectMapper() {
        return objectMapper;
    }

    protected final TypeFactory getTypeFactory() {
        return typeFactory;
    }

    protected final Resource getTemplateResource(final String name) {
        return trimToNull(name) == null ? null : wac.getResource(TEMPLATES_CLASSPATH + name);
    }

}
