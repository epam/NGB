package com.epam.ngb.cli.manager.command.handler.http;

import java.util.Arrays;
import java.util.Collections;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.epam.ngb.cli.AbstractCliTest;
import com.epam.ngb.cli.TestDataProvider;
import com.epam.ngb.cli.TestHttpServer;
import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.entity.BiologicalDataItem;
import com.epam.ngb.cli.entity.BiologicalDataItemFormat;
import com.epam.ngb.cli.manager.command.ServerParameters;

public class UrlGeneratorHandlerTest extends AbstractCliTest {
    private static TestHttpServer server = new TestHttpServer();
    private static ServerParameters serverParameters;

    private static final Long REF_BIO_ID = 1L;
    private static final Long REF_ID = 50L;
    private static final String PATH_TO_REFERENCE = "reference/50";
    private static final String REFERENCE_NAME = "hg38";

    private static final Long VCF_BIO_ID = 2L;
    private static final Long VCF_ID = 1L;
    private static final String PATH_TO_VCF = "path/test.vcf";
    private static final String VCF_NAME = "test.vcf";

    private static final String DATASET_NAME = "data";
    private static final Long DATASET_ID = 1L;

    private static final Integer TEST_END_INDEX = 12345;

    private static final String COMMAND = "generate_url";

    @BeforeClass
    public static void setUp() {
        server.start();
        server.addReference(REF_BIO_ID, REF_ID, REFERENCE_NAME, PATH_TO_REFERENCE);
        BiologicalDataItem reference = TestDataProvider.getBioItem(REF_ID, REF_BIO_ID,
                                                                   BiologicalDataItemFormat.REFERENCE,
                                                                   PATH_TO_REFERENCE, REFERENCE_NAME);
        server.addFile(VCF_BIO_ID, VCF_ID, VCF_NAME, PATH_TO_VCF, BiologicalDataItemFormat.VCF);
        BiologicalDataItem vcfFile = TestDataProvider.getBioItem(VCF_ID, VCF_BIO_ID, BiologicalDataItemFormat.VCF,
                                                                 PATH_TO_VCF, VCF_NAME);
        server.addDataset(DATASET_ID, DATASET_NAME, Arrays.asList(reference, vcfFile));
        server.addUrlGenerationRequest(REFERENCE_NAME, "A1", 1, TEST_END_INDEX, VCF_BIO_ID, DATASET_ID);

        serverParameters = getDefaultServerOptions(server.getPort());
    }

    @Test
    public void testParseUrlGenerationParameters() {
        UrlGeneratorHandler handler = new UrlGeneratorHandler();
        handler.parseAndVerifyArguments(Collections.singletonList(VCF_NAME), new ApplicationOptions());

        ApplicationOptions options = new ApplicationOptions();
        options.setLocation("chr1:1-2124");
        handler.parseAndVerifyArguments(Collections.singletonList(VCF_NAME), options);

        options.setLocation("chr1");
        handler.parseAndVerifyArguments(Collections.singletonList(VCF_NAME), options);
    }

    @Test
    public void testGenerateUrl() {
        UrlGeneratorHandler handler = new UrlGeneratorHandler();
        handler.setServerParameters(serverParameters);
        handler.setConfiguration(getCommandConfiguration(COMMAND));

        ApplicationOptions options = new ApplicationOptions();
        options.setLocation("A1:1-12345");
        handler.parseAndVerifyArguments(Collections.singletonList(VCF_BIO_ID.toString()), options);

        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
    }
}
