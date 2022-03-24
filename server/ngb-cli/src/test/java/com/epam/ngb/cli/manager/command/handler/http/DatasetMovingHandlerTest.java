package com.epam.ngb.cli.manager.command.handler.http;

import java.util.Collections;

import com.epam.ngb.cli.AbstractCliTest;
import com.epam.ngb.cli.TestDataProvider;
import com.epam.ngb.cli.TestHttpServer;
import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.entity.BiologicalDataItem;
import com.epam.ngb.cli.entity.BiologicalDataItemFormat;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.ServerParameters;
import com.epam.ngb.cli.manager.command.handler.http.dataset.DatasetMovingHandler;
import org.junit.*;

public class DatasetMovingHandlerTest extends AbstractCliTest {

    private static final String COMMAND = "move_dataset";
    private static ServerParameters serverParameters;
    private static TestHttpServer server = new TestHttpServer();

    private static final Long REF_BIO_ID = 1L;
    private static final Long REF_ID = 50L;
    private static final String PATH_TO_REFERENCE = "reference/50";
    private static final String REFERENCE_NAME = "hg38";

    private static final String DATASET_NAME = "data";
    private static final Long DATASET_ID = 1L;

    private static final String DATASET_PARENT_NAME= "data_parent";
    private static final Long DATASET_PARENT_ID = 4L;

    @BeforeClass
    public static void setUp() {
        server.start();
        serverParameters = getDefaultServerOptions(server.getPort());
    }

    @After
    public void reset() {
        server.reset();
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
    }

    @Test
    public void testRemoveParentId() {
        testRemoveParent(String.valueOf(DATASET_ID));
    }

    @Test
    public void testRemoveParentName() {
        testRemoveParent(DATASET_NAME);
    }

    private void testRemoveParent(String dataset) {
        addDatasets();
        server.addDatasetMovingWithoutParent(DATASET_ID);
        DatasetMovingHandler handler = getDatasetMoveHandler();
        handler.parseAndVerifyArguments(Collections.singletonList(dataset),
                new ApplicationOptions());
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testChangeParentId() {
        testChangeParent(String.valueOf(DATASET_ID), DATASET_PARENT_NAME);
    }

    @Test
    public void testChangeParentName() {
        testChangeParent(DATASET_NAME, String.valueOf(DATASET_PARENT_ID));
    }

    private void testChangeParent(String dataset, String parent) {
        addDatasets();
        server.addDatasetMovingWithParent(DATASET_ID, DATASET_PARENT_ID);
        DatasetMovingHandler handler = getDatasetMoveHandler();
        ApplicationOptions options = new ApplicationOptions();
        options.setParent(parent);
        handler.parseAndVerifyArguments(Collections.singletonList(dataset),
                options);
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }


    @Test(expected = IllegalArgumentException.class)
    public void testWrongArguments() {
        DatasetMovingHandler handler = getDatasetMoveHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        handler.parseAndVerifyArguments(Collections.emptyList(), applicationOptions);
    }

    @Test(expected = ApplicationException.class)
    public void testUnregisteredDataset() {
        DatasetMovingHandler handler = getDatasetMoveHandler();
        ApplicationOptions applicationOptions = new ApplicationOptions();
        handler.parseAndVerifyArguments(Collections.singletonList(DATASET_NAME), applicationOptions);
    }

    private void addDatasets() {
        BiologicalDataItem reference = TestDataProvider
                .getBioItem(REF_ID, REF_BIO_ID, BiologicalDataItemFormat.REFERENCE,
                        PATH_TO_REFERENCE, REFERENCE_NAME);
        server.addDataset(DATASET_ID, DATASET_NAME, Collections.singletonList(reference));
        server.addDataset(DATASET_PARENT_ID, DATASET_PARENT_NAME, Collections.singletonList(reference));
    }

    private DatasetMovingHandler getDatasetMoveHandler() {
        DatasetMovingHandler handler = new DatasetMovingHandler();
        handler.setServerParameters(serverParameters);
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        return handler;
    }

}
