/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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

package com.epam.ngb.cli.manager.command.handler.http;

import com.epam.ngb.cli.AbstractCliTest;
import com.epam.ngb.cli.TestDataProvider;
import com.epam.ngb.cli.TestHttpServer;
import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.entity.BiologicalDataItem;
import com.epam.ngb.cli.entity.BiologicalDataItemFormat;
import com.epam.ngb.cli.manager.command.ServerParameters;
import com.epam.ngb.cli.manager.command.handler.http.dataset.DatasetItemAddingHandler;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class DatasetItemAddingHandlerTest extends AbstractCliTest {

    private static final String COMMAND = "add_to_dataset";
    private static ServerParameters serverParameters;
    private static TestHttpServer server = new TestHttpServer();
    private static final Long REF_BIO_ID = 1L;
    private static final Long REF_ID = 50L;
    private static final String PATH_TO_REFERENCE = "reference/50";
    private static final String REFERENCE_NAME = "hg38";

    private static final String DATASET_NAME = "data";
    private static final Long DATASET_ID = 1L;

    private static final Long VCF_BIO_ID = 2L;
    private static final Long VCF_ID = 1L;
    private static final String PATH_TO_VCF = "path/test.vcf";
    private static final String VCF_NAME = "test.vcf";

    private static final Long BAM_BIO_ID = 3L;
    private static final Long BAM_ID = 1L;
    private static final String PATH_TO_BAM = "/path/test.bam";
    private static final String PATH_TO_BAI = "/path/test.bam.bai";
    private static final String BAM_NAME = "test.bam";

    @BeforeClass
    public static void setUp() {
        server.start();

        server.addReference(REF_BIO_ID, REF_ID, REFERENCE_NAME, PATH_TO_REFERENCE);
        BiologicalDataItem reference = TestDataProvider.getBioItem(REF_ID, REF_BIO_ID,
                BiologicalDataItemFormat.REFERENCE, PATH_TO_REFERENCE, REFERENCE_NAME);
        server.addDataset(DATASET_ID, DATASET_NAME, Collections.singletonList(reference));

        BiologicalDataItem vcf = TestDataProvider.getBioItem(VCF_ID, VCF_BIO_ID,
                BiologicalDataItemFormat.VCF, PATH_TO_VCF, VCF_NAME);
        server.addFile(VCF_BIO_ID, VCF_ID, VCF_NAME, PATH_TO_VCF, BiologicalDataItemFormat.VCF);

        BiologicalDataItem bam = TestDataProvider.getBioItem(BAM_ID, BAM_BIO_ID,
                BiologicalDataItemFormat.BAM, PATH_TO_BAM, BAM_NAME);
        server.addFileRegistration(REF_ID, PATH_TO_BAM, PATH_TO_BAI, null, BAM_ID, BAM_BIO_ID,
                BiologicalDataItemFormat.BAM);

        server.addItemToDataset(DATASET_ID, DATASET_NAME, Collections.singletonList(reference), vcf);
        server.addItemToDataset(DATASET_ID, DATASET_NAME, Collections.singletonList(reference), bam);
        server.addGetFormatsRequest();
        serverParameters = getDefaultServerOptions(server.getPort());
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongArguments() {
        DatasetItemAddingHandler handler = getDatasetItemAddingHandler();
        handler.parseAndVerifyArguments(Collections.singletonList(DATASET_NAME), new ApplicationOptions());
    }

    @Test
    public void testAddFileByName() {
        DatasetItemAddingHandler handler = getDatasetItemAddingHandler();
        handler.parseAndVerifyArguments(Arrays.asList(DATASET_NAME, VCF_NAME), new ApplicationOptions());
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testAddFileByID() {
        DatasetItemAddingHandler handler = getDatasetItemAddingHandler();
        handler.parseAndVerifyArguments(Arrays.asList(String.valueOf(DATASET_ID), String.valueOf(VCF_BIO_ID)),
                new ApplicationOptions());
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testAddFilesWithRegistration() {
        DatasetItemAddingHandler handler = getDatasetItemAddingHandler();
        handler.parseAndVerifyArguments(Arrays.asList(DATASET_NAME, VCF_NAME, PATH_TO_BAM + "?" + PATH_TO_BAI),
                new ApplicationOptions());
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    private DatasetItemAddingHandler getDatasetItemAddingHandler() {
        DatasetItemAddingHandler handler = new DatasetItemAddingHandler();
        handler.setServerParameters(serverParameters);
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        return handler;
    }
}
