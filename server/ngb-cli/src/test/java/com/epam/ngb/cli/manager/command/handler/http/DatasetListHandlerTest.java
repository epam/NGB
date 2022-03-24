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

package com.epam.ngb.cli.manager.command.handler.http;

import com.epam.ngb.cli.AbstractCliTest;
import com.epam.ngb.cli.TestDataProvider;
import com.epam.ngb.cli.TestHttpServer;
import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.entity.AclSecuredEntry;
import com.epam.ngb.cli.entity.BiologicalDataItem;
import com.epam.ngb.cli.entity.BiologicalDataItemFormat;
import com.epam.ngb.cli.entity.Project;
import com.epam.ngb.cli.entity.Role;
import com.epam.ngb.cli.entity.UserContext;
import com.epam.ngb.cli.manager.command.ServerParameters;
import com.epam.ngb.cli.manager.command.handler.http.dataset.DatasetListHandler;
import org.junit.*;

import java.util.Arrays;
import java.util.Collections;

import static com.epam.ngb.cli.TestDataProvider.buildAclSecuredEntry;

public class DatasetListHandlerTest extends AbstractCliTest {
    private static final String COMMAND = "list_datasets";
    private static ServerParameters serverParameters;
    private static TestHttpServer server = new TestHttpServer();

    private static final String DATASET_NAME_1 = "data1";
    private static final Long DATASET_ID_1 = 1L;

    private static final String DATASET_NAME_2 = "data2";
    private static final Long DATASET_ID_2 = 2L;

    private static final Long REF_BIO_ID = 1L;
    private static final Long REF_ID = 50L;
    private static final String PATH_TO_REFERENCE = "reference/50";
    private static final String REFERENCE_NAME = "hg38";

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
    public void testListingJson() {
        addDatasets();
        DatasetListHandler handler = getDatasetListHandler();
        handler.parseAndVerifyArguments(Collections.emptyList(), new ApplicationOptions());
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testListingTable() {
        addDatasets();
        DatasetListHandler handler = getDatasetListHandler();
        ApplicationOptions options = new ApplicationOptions();
        options.setPrintTable(true);
        handler.parseAndVerifyArguments(Collections.emptyList(), options);
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testListingParent() {
        addDatasetsWithParent();
        DatasetListHandler handler = getDatasetListHandler();
        ApplicationOptions options = new ApplicationOptions();
        options.setPrintTable(true);
        options.setParent(String.valueOf(DATASET_ID_1));
        handler.parseAndVerifyArguments(Collections.emptyList(), options);
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test
    public void testListingEmpty() {
        server.addDatasetListing(Collections.emptyList());
        DatasetListHandler handler = getDatasetListHandler();
        handler.parseAndVerifyArguments(Collections.emptyList(), new ApplicationOptions());
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongArguments() {
        DatasetListHandler handler = getDatasetListHandler();
        handler.parseAndVerifyArguments(Collections.singletonList(DATASET_NAME_1), new ApplicationOptions());
    }

    @Test
    public void shouldPrintPermissions() {
        UserContext user = new UserContext();
        user.setRoles(Collections.singletonList(new Role("ROLE_ADMIN")));

        Project project = TestDataProvider
                .getProject(DATASET_ID_1, DATASET_NAME_1, Collections.emptyList());

        AclSecuredEntry entry = buildAclSecuredEntry(
                AclSecuredEntry.Entity
                        .builder()
                        .id(DATASET_ID_1)
                        .mask(1)
                        .name(DATASET_NAME_1)
                        .owner(TEST_OWNER)
                        .build(), TEST_OWNER, TEST_GROUP);

        server.addDatasetListing(Collections.singletonList(project));
        server.addPermissions(entry, "PROJECT");
        server.addCurrentUserRequest(user);

        DatasetListHandler handler = getDatasetListHandler();
        ApplicationOptions options = new ApplicationOptions();
        options.setPrintTable(true);
        options.setShowPermissions(true);
        handler.parseAndVerifyArguments(Collections.emptyList(), options);
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }

    private DatasetListHandler getDatasetListHandler() {
        DatasetListHandler handler = new DatasetListHandler();
        handler.setServerParameters(serverParameters);
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        return handler;
    }

    private void addDatasets() {
        BiologicalDataItem reference = TestDataProvider
                .getBioItem(REF_ID, REF_BIO_ID, BiologicalDataItemFormat.REFERENCE,
                        PATH_TO_REFERENCE, REFERENCE_NAME);

        Project project1 = TestDataProvider
                .getProject(DATASET_ID_1, DATASET_NAME_1, Collections.singletonList(reference));
        Project project2 = TestDataProvider
                .getProject(DATASET_ID_2, DATASET_NAME_2, Collections.singletonList(reference));
        server.addDatasetListing(Arrays.asList(project1, project2));
    }

    private void addDatasetsWithParent() {
        BiologicalDataItem reference = TestDataProvider
                .getBioItem(REF_ID, REF_BIO_ID, BiologicalDataItemFormat.REFERENCE,
                        PATH_TO_REFERENCE, REFERENCE_NAME);

        Project project1 = TestDataProvider
                .getProject(DATASET_ID_1, DATASET_NAME_1, Collections.singletonList(reference));
        Project project2 = TestDataProvider
                .getProject(DATASET_ID_2, DATASET_NAME_2, Collections.singletonList(reference));
        project1.setNestedProjects(Collections.singletonList(project2));
        server.addDatasetTreeListing(project1, Arrays.asList(project1, project2));
    }
}
