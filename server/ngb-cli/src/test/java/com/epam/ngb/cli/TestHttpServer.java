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

package com.epam.ngb.cli;

import com.epam.ngb.cli.entity.BiologicalDataItem;
import com.epam.ngb.cli.entity.BiologicalDataItemFormat;
import com.epam.ngb.cli.entity.Project;
import com.epam.ngb.cli.entity.ProjectItem;
import com.epam.ngb.cli.manager.command.handler.http.AbstractHTTPCommandHandler;
import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.jadler.Jadler.*;

public class TestHttpServer extends AbstractCliTest{

    private static final String AUTHORISATION = "authorization";
    private static final String BEARER = "Bearer ";
    private static final String NAME_PARAMETER = "name";

    public void start() {
        initJadler();
    }

    public void stop() {
        closeJadler();
    }

    public int getPort() {
        return port();
    }

    public void reset() {
        resetJadler();
    }

    /**
     *  Mocks presence of a registered reference on the server
     */
    public void addReference(Long refBioId, Long refId, String name, String path) {
        //load reference by bioitem ID
        onRequest()
                .havingMethodEqualTo(HTTP_GET)
                .havingPathEqualTo(FILE_FIND_URL)
                .havingParameterEqualTo("id", String.valueOf(refBioId))
                .respond()
                .withBody(TestDataProvider.getFilePayloadJson(refId, refBioId,
                        BiologicalDataItemFormat.REFERENCE, path, name))
                .withStatus(HTTP_STATUS_OK);

        //load reference by name
        //when loading files by name we receive only BioItemId
        onRequest()
                .havingMethodEqualTo(HTTP_GET)
                .havingPathEqualTo(SEARCH_URL)
                .havingParameterEqualTo(NAME_PARAMETER, name)
                .respond()
                .withBody(TestDataProvider.getFilePayloadJson(refId, refBioId,
                        BiologicalDataItemFormat.REFERENCE, path, name, true))
                .withStatus(HTTP_STATUS_OK);
    }

    /**
     * Enables file registration on the server
     */
    public void addFileRegistration(Long refId, String path, String index, String name, Long fileId,
            Long fileBioId, BiologicalDataItemFormat format) {
        onRequest()
                .havingMethodEqualTo(HTTP_POST)
                .havingPathEqualTo(String.format(REGISTRATION_URL,
                        format.name().toLowerCase()))
                .havingBodyEqualTo(TestDataProvider.getRegistrationJson(refId, path,
                        name, index, null))
                .respond()
                .withBody(TestDataProvider.getFilePayloadJson(fileId, fileBioId, format,
                        path, name == null ? FilenameUtils.getName(path) : name))
                .withStatus(HTTP_STATUS_OK);
    }

    public void addFeatureIndexedFileRegistration(Long refId, String path, String index, String name, Long fileId,
                                    Long fileBioId, BiologicalDataItemFormat format, boolean doIndex) {
        onRequest()
            .havingMethodEqualTo(HTTP_POST)
            .havingPathEqualTo(String.format(REGISTRATION_URL,
                                             format.name().toLowerCase()))
            .havingBodyEqualTo(TestDataProvider.getRegistrationJson(refId, path,
                                                                    name, index, doIndex))
            .respond()
            .withBody(TestDataProvider.getFilePayloadJson(fileId, fileBioId, format,
                                                          path, name == null ? FilenameUtils.getName(path) : name))
            .withStatus(HTTP_STATUS_OK);
    }

    /**
     * Enables dataset registration on the server
     */
    public void addDatasetRegistration(String name, List<BiologicalDataItem> items, Long id) {
        onRequest()
                .havingMethodEqualTo(HTTP_POST)
                .havingPathEqualTo(DATASET_REGISTRATION_URL)
                .havingBodyEqualTo(TestDataProvider.getRegistrationJson(null, null,
                        name, null, items.stream()
                                .map(i -> new ProjectItem(i.getBioDataItemId(), false))
                                .collect(Collectors.toList())))
                .respond()
                .withBody(TestDataProvider.getProjectPayloadJson(id, name, items))
                .withStatus(HTTP_STATUS_OK);
    }

    /**
     *  Mocks presence of a registered file on the server
     */
    public void addFile(Long fileBioID, Long id, String name, String path, BiologicalDataItemFormat format) {
        //load reference by name
        //when loading files by name we receive only BioItemId
        onRequest()
                .havingMethodEqualTo(HTTP_GET)
                .havingPathEqualTo(SEARCH_URL)
                .havingParameterEqualTo(NAME_PARAMETER, name)
                .respond()
                .withBody(TestDataProvider.getFilePayloadJson(id, fileBioID,
                        format, path, name, true))
                .withStatus(HTTP_STATUS_OK);

        onRequest()
            .havingMethodEqualTo(HTTP_GET)
            .havingPathEqualTo(FILE_FIND_URL)
            .havingParameterEqualTo("id", String.valueOf(fileBioID))
            .respond()
            .withBody(TestDataProvider.getFilePayloadJson(id, fileBioID,
                                                          format, path, name, false))
            .withStatus(HTTP_STATUS_OK);
    }

    /**
     *  Mocks presence of a registered dataset on the server
     */
    public void addDataset(Long id, String name, List<BiologicalDataItem> items) {
        onRequest()
                .havingMethodEqualTo(HTTP_GET)
                .havingPathEqualTo(DATASET_LOAD_BY_NAME_URL)
                .havingParameterEqualTo("projectName", name)
                .respond()
                .withBody(TestDataProvider.getProjectPayloadJson(id, name,
                        items))
                .withStatus(HTTP_STATUS_OK);
        onRequest()
                .havingMethodEqualTo(HTTP_GET)
                .havingPathEqualTo(String.format(DATASET_LOAD_BY_ID_URL, id))
                .respond()
                .withBody(TestDataProvider.getProjectPayloadJson(id, name,
                        items))
                .withStatus(HTTP_STATUS_OK);
    }

    /**
     *  Mocks presence of a registered dataset on the server
     */
    public void addDatasetDeletion(Long id, String name) {
        onRequest()
                .havingMethodEqualTo(HTTP_DELETE)
                .havingPathEqualTo(String.format(DATASET_DELETE_URL, id))
                .respond()
                .withBody(TestDataProvider.getPayloadJson("OK " + id + " Project " + name + " deleted."))
                .withStatus(HTTP_STATUS_OK);
    }

    public void addDatasetRegistrationWithParent(String name,
            List<BiologicalDataItem> items, Long id, Long parentId) {
        onRequest()
                .havingMethodEqualTo(HTTP_POST)
                .havingPathEqualTo(DATASET_REGISTRATION_URL)
                .havingParameterEqualTo("parentId", String.valueOf(parentId))
                .havingBodyEqualTo(TestDataProvider.getRegistrationJson(null, null,
                    name, null, items.stream()
                            .map(i -> new ProjectItem(i.getBioDataItemId(), false))
                            .collect(Collectors.toList())))
                .respond()
                .withBody(TestDataProvider.getProjectPayloadJson(id, name, items))
                .withStatus(HTTP_STATUS_OK);
    }

    public void addItemToDataset(Long datasetId, String datasetName, List<BiologicalDataItem> items,
            BiologicalDataItem newItem) {
        List<BiologicalDataItem> newItems = new ArrayList<>(items);
        newItems.add(newItem);
        onRequest()
                .havingMethodEqualTo(HTTP_PUT)
                .havingPathEqualTo(String.format(DATASET_ADDING_URL, datasetId, newItem.getBioDataItemId()))
                .respond()
                .withBody(TestDataProvider.getProjectPayloadJson(datasetId, datasetName, newItems))
                .withStatus(HTTP_STATUS_OK);
    }

    public void removeFileFromDataset(Long datasetId, String datasetName,
            List<BiologicalDataItem> items, BiologicalDataItem file) {
        List<BiologicalDataItem> newItems = new ArrayList<>(items);
        newItems.remove(file);
        onRequest()
                .havingMethodEqualTo(HTTP_DELETE)
                .havingPathEqualTo(String.format(DATASET_REMOVE_URL, datasetId, file.getBioDataItemId()))
                .respond()
                .withBody(TestDataProvider.getProjectPayloadJson(datasetId, datasetName, newItems))
                .withStatus(HTTP_STATUS_OK);
    }

    public void addDatasetListing(List<Project> projects) {
        onRequest()
                .havingMethodEqualTo(HTTP_GET)
                .havingPathEqualTo(DATASET_LIST_URL)
                .respond()
                .withBody(TestDataProvider.getPayloadJson(projects))
                .withStatus(HTTP_STATUS_OK);
    }

    public void addFileDeletion(Long id, String name) {
        onRequest()
                .havingMethodEqualTo(HTTP_DELETE)
                .havingPathEqualTo(FILE_DELETE_URL)
                .havingParameterEqualTo("id", String.valueOf(id))
                .respond()
                .withBody(TestDataProvider.getPayloadJson("OK " + id + " File " + name + " deleted."))
                .withStatus(HTTP_STATUS_OK);
    }

    public void addFileIndexing(Long id, String name, BiologicalDataItemFormat format) {
        String url = String.format(INDEXATION_URL, format.toString().toLowerCase(), id);
        onRequest()
            .havingMethodEqualTo(HTTP_GET)
            .havingPathEqualTo(url)
            .respond()
            .withBody(TestDataProvider.getPayloadJson("Created feature index for file " + id + " : '" +
                                                      name + "'"))
            .withStatus(HTTP_STATUS_OK);
    }

    public void addReferenceRegistration(Long refId, Long refBioId, String path,
            String name) {
        onRequest()
                .havingMethodEqualTo(HTTP_POST)
                .havingPathEqualTo(REF_REGISTRATION_URL)
                .havingHeaderEqualTo(AUTHORISATION, BEARER + TOKEN)
                .havingBodyEqualTo(TestDataProvider.getRegistrationJson(null, path,
                        name, null, null))
                .respond()
                .withBody(TestDataProvider.getFilePayloadJson(refId, refBioId, BiologicalDataItemFormat.REFERENCE,
                        path, name == null ? FilenameUtils.getName(path) : name))
                .withStatus(HTTP_STATUS_OK);
    }

    public void addAuthorization() {
        AbstractHTTPCommandHandler.Authentication authentication =
                new AbstractHTTPCommandHandler.Authentication();
        authentication.setAccessToken(TOKEN);
        onRequest()
                .havingMethodEqualTo(HTTP_POST)
                .havingPathEqualTo(AUTHENTICATION_URL)
                .havingBodyEqualTo(AUTHENTICATION_PAYLOAD)
                .respond()
                .withBody(TestDataProvider.getJson(authentication))
                .withStatus(HTTP_STATUS_OK);
    }

    public void addReferenceDeletion(Long refId, String name) {
        onRequest()
                .havingMethodEqualTo(HTTP_DELETE)
                .havingPathEqualTo(REF_DELETION_URL)
                .havingParameterEqualTo("referenceId", String.valueOf(refId))
                .havingHeaderEqualTo(AUTHORISATION, BEARER + TOKEN)
                .respond()
                .withBody(TestDataProvider.getPayloadJson("OK " + refId + " Reference " + name + " deleted."))
                .withStatus(HTTP_STATUS_OK);
    }

    public void addReferenceListing(List<BiologicalDataItem> references) {
        onRequest()
                .havingMethodEqualTo(HTTP_GET)
                .havingPathEqualTo(REFERENCE_LIST_URL)
                .respond()
                .withBody(TestDataProvider.getPayloadJson(references))
                .withStatus(HTTP_STATUS_OK);
    }

    public void addDatasetTreeListing(Project root, List<Project> hierarchy) {
        onRequest()
                .havingMethodEqualTo(HTTP_GET)
                .havingPathEqualTo(DATASET_TREE_URL)
                .havingParameterEqualTo("parentId", String.valueOf(root.getId()))
                .respond()
                .withBody(TestDataProvider.getPayloadJson(hierarchy))
                .withStatus(HTTP_STATUS_OK);
    }

    public void addGeneAdding(BiologicalDataItem reference, Long geneId) {
        onRequest()
                .havingMethodEqualTo(HTTP_PUT)
                .havingPathEqualTo(String.format(GENE_ADDING_URL, reference.getId()))
                .havingParameterEqualTo("geneFileId", String.valueOf(geneId))
                .havingHeaderEqualTo(AUTHORISATION, BEARER + TOKEN)
                .respond()
                .withBody(TestDataProvider.getPayloadJson(reference))
                .withStatus(HTTP_STATUS_OK);
    }

    public void addGeneRemoving(BiologicalDataItem reference) {
        onRequest()
                .havingMethodEqualTo(HTTP_PUT)
                .havingPathEqualTo(String.format(GENE_ADDING_URL, reference.getId()))
                .havingHeaderEqualTo(AUTHORISATION, BEARER + TOKEN)
                .respond()
                .withBody(TestDataProvider.getPayloadJson(reference))
                .withStatus(HTTP_STATUS_OK);
    }

    public void addSearchQuery(String query, List<BiologicalDataItem> items) {
        onRequest()
                .havingMethodEqualTo(HTTP_GET)
                .havingPathEqualTo(SEARCH_URL)
                .havingParameterEqualTo(NAME_PARAMETER, query)
                .havingParameterEqualTo("strict", "false")
                .respond()
                .withBody(TestDataProvider.getPayloadJson(items))
                .withStatus(HTTP_STATUS_OK);
    }

    public void addEmptySearch(String query) {
        onRequest()
                .havingMethodEqualTo(HTTP_GET)
                .havingPathEqualTo(SEARCH_URL)
                .havingParameterEqualTo(NAME_PARAMETER, query)
                .respond()
                .withBody(TestDataProvider.getPayloadJson(Collections.emptyList()))
                .withStatus(HTTP_STATUS_OK);
    }

    public void addDatasetMovingWithParent(Long datasetId, Long parentId) {
        onRequest()
                .havingMethodEqualTo(HTTP_PUT)
                .havingPathEqualTo(String.format(DATASET_MOVING_URL, datasetId))
                .havingParameterEqualTo("parentId", String.valueOf(parentId))
                .respond()
                .withBody(TestDataProvider.getPayloadJson("OK"))
                .withStatus(HTTP_STATUS_OK);
    }

    public void addDatasetMovingWithoutParent(Long datasetId) {
        onRequest()
                .havingMethodEqualTo(HTTP_PUT)
                .havingPathEqualTo(String.format(DATASET_MOVING_URL, datasetId))
                .respond()
                .withBody(TestDataProvider.getPayloadJson("OK"))
                .withStatus(HTTP_STATUS_OK);
    }
}
