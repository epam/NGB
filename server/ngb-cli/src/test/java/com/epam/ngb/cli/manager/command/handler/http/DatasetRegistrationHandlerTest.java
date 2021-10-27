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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class DatasetRegistrationHandlerTest extends AbstractCliTest {

    private static final Long REF_BIO_ID = 1L;
    private static final Long REF_ID = 50L;
    private static final String PATH_TO_REFERENCE = "reference/50";
    private static final String REFERENCE_NAME = "hg38";

    private static final String DATASET_NAME = "data";
    private static final Long DATASET_ID = 1L;

    private static final String DATASET_WITH_VCF_NAME = "data_vcf";
    private static final Long DATASET_WITH_VCF_ID = 2L;

    private static final String DATASET_WITH_BAM_NAME = "data_vcf_bam";
    private static final Long DATASET_WITH_BAM_ID = 3L;

    private static final String DATASET_WITH_PARENT_NAME= "data_parent";
    private static final Long DATASET_WIH_PARENT_ID = 4L;

    private static final String DATASET_PRETTY = "dataWithPretty";
    private static final Long DATASET_PRETTY_ID = 5L;

    private static final Long VCF_BIO_ID = 2L;
    private static final Long VCF_ID = 1L;
    private static final String PATH_TO_VCF = "path/test.vcf";
    private static final String VCF_NAME = "test.vcf";

    private static final Long BAM_BIO_ID = 3L;
    private static final Long BAM_ID = 1L;
    private static final String PATH_TO_BAM = "path/test.bam";
    private static final String PATH_TO_BAI = "path/test.bam.bai";
    private static final String BAM_NAME = "test.bam";

    private static final String COMMAND = "register_dataset";
    private static ServerParameters serverParameters;
    private static TestHttpServer server = new TestHttpServer();

    @BeforeClass
    public static void setUp() {
        server.start();
        server.addReference(REF_BIO_ID, REF_ID, REFERENCE_NAME, PATH_TO_REFERENCE);
        //simple case - empty dataset
        BiologicalDataItem reference = TestDataProvider.getBioItem(REF_ID, REF_BIO_ID,
                BiologicalDataItemFormat.REFERENCE, PATH_TO_REFERENCE, REFERENCE_NAME);
        server.addDatasetRegistration(DATASET_NAME, Collections.singletonList(reference), DATASET_ID);

        //dataset + parent ID
        server.addDataset(DATASET_ID, DATASET_NAME, Collections.singletonList(reference));
        server.addDatasetRegistrationWithParent(DATASET_WITH_PARENT_NAME, Collections.singletonList(reference),
                DATASET_WIH_PARENT_ID, DATASET_ID);

        //dataset with registered file
        BiologicalDataItem vcf = TestDataProvider.getBioItem(VCF_ID, VCF_BIO_ID,
                BiologicalDataItemFormat.VCF, PATH_TO_VCF, VCF_NAME);
        server.addFile(VCF_BIO_ID, VCF_ID, VCF_NAME, PATH_TO_VCF, BiologicalDataItemFormat.VCF);
        server.addDatasetRegistration(DATASET_WITH_VCF_NAME,
                Arrays.asList(reference, vcf), DATASET_WITH_VCF_ID);

        //dataset with file registration
        BiologicalDataItem bam = TestDataProvider.getBioItem(BAM_ID, BAM_BIO_ID,
                BiologicalDataItemFormat.BAM, PATH_TO_BAM, BAM_NAME);
        server.addFileRegistration(REF_ID, PATH_TO_BAM, PATH_TO_BAI, null, BAM_ID, BAM_BIO_ID,
                BiologicalDataItemFormat.BAM);
        server.addDatasetRegistration(DATASET_WITH_BAM_NAME,
                Arrays.asList(reference, vcf, bam), DATASET_WITH_BAM_ID);
        serverParameters = getDefaultServerOptions(server.getPort());

        server.addGetFormatsRequest();

        // Data set with prettyName
        server.addDatasetRegistrationWithPrettyName(
                DATASET_PRETTY,
                Collections.singletonList(reference),
                DATASET_PRETTY_ID,
                "pretty"
        );
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDatasetRegistrationWrongArguments() {
        DatasetRegistrationHandler handler = getDatasetRegistrationHandler();
        handler.parseAndVerifyArguments(Collections.singletonList(String.valueOf(REFERENCE_NAME)),
                new ApplicationOptions());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testDatasetRegistrationNoArguments() {
        DatasetRegistrationHandler handler = getDatasetRegistrationHandler();
        handler.parseAndVerifyArguments(Collections.emptyList(), new ApplicationOptions());
    }

    @Test
    public void testDatasetRegistrationWithPrettyName() {
        ApplicationOptions options = new ApplicationOptions();
        options.setPrettyName("pretty");
        DatasetRegistrationHandler handler = getDatasetRegistrationHandler();
        handler.parseAndVerifyArguments(Arrays.asList(String.valueOf(REFERENCE_NAME),
                DATASET_PRETTY), options);
        assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testDatasetRegistrationWithoutFiles() {
        DatasetRegistrationHandler handler = getDatasetRegistrationHandler();
        handler.parseAndVerifyArguments(Arrays.asList(String.valueOf(REFERENCE_NAME),
                DATASET_NAME), new ApplicationOptions());
        assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testDatasetRegistrationWithFile() {
        DatasetRegistrationHandler handler = getDatasetRegistrationHandler();
        handler.parseAndVerifyArguments(Arrays.asList(String.valueOf(REF_BIO_ID),
                DATASET_WITH_VCF_NAME, VCF_NAME), new ApplicationOptions());
        assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testDatasetRegistrationWithFileRegistration() {
        DatasetRegistrationHandler handler = getDatasetRegistrationHandler();
        handler.parseAndVerifyArguments(Arrays.asList(String.valueOf(REF_BIO_ID),
                DATASET_WITH_BAM_NAME, VCF_NAME, PATH_TO_BAM + "?" + PATH_TO_BAI),
                new ApplicationOptions());
        assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testDatasetRegistrationWithParentName() {
        DatasetRegistrationHandler handler = getDatasetRegistrationHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        applicationOptions.setParent(DATASET_NAME);
        handler.parseAndVerifyArguments(Arrays.asList(String.valueOf(REF_BIO_ID),
                DATASET_WITH_PARENT_NAME), applicationOptions);
        assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testDatasetRegistrationWithParentID() {
        DatasetRegistrationHandler handler = getDatasetRegistrationHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        applicationOptions.setParent(String.valueOf(DATASET_ID));
        handler.parseAndVerifyArguments(Arrays.asList(String.valueOf(REF_BIO_ID),
                DATASET_WITH_PARENT_NAME), applicationOptions);
        assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    private DatasetRegistrationHandler getDatasetRegistrationHandler() {
        DatasetRegistrationHandler handler = new DatasetRegistrationHandler();
        handler.setServerParameters(serverParameters);
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        return handler;
    }
}
