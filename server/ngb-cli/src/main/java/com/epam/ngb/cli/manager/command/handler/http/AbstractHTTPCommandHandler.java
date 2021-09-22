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

import static com.epam.ngb.cli.constants.MessageConstants.ERROR_DATAITEM_FORMATS_NOT_FOUND;
import static com.epam.ngb.cli.constants.MessageConstants.ERROR_FAILED_TO_LOAD_USER;
import static com.epam.ngb.cli.constants.MessageConstants.ERROR_FILE_NOT_FOUND;
import static com.epam.ngb.cli.constants.MessageConstants.ERROR_INDEX_REQUIRED;
import static com.epam.ngb.cli.constants.MessageConstants.ERROR_PERMISSIONS_NOT_FOUND;
import static com.epam.ngb.cli.constants.MessageConstants.ERROR_PROJECT_NOT_FOUND;
import static com.epam.ngb.cli.constants.MessageConstants.ERROR_REFERENCE_NOT_FOUND;
import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_PATH_FORMAT;
import static com.epam.ngb.cli.constants.MessageConstants.SEVERAL_RESULTS_FOR_QUERY;
import static com.epam.ngb.cli.constants.MessageConstants.getMessage;
import static com.epam.ngb.cli.entity.BiologicalDataItemResourceType.FILE;
import static com.epam.ngb.cli.entity.BiologicalDataItemResourceType.getTypeFromPath;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.epam.ngb.cli.app.Utils;
import com.epam.ngb.cli.entity.AclClass;
import com.epam.ngb.cli.entity.AclSecuredEntry;
import com.epam.ngb.cli.entity.BiologicalDataItem;
import com.epam.ngb.cli.entity.BiologicalDataItemFormat;
import com.epam.ngb.cli.entity.IDList;
import com.epam.ngb.cli.entity.NgbUser;
import com.epam.ngb.cli.entity.Project;
import com.epam.ngb.cli.entity.Reference;
import com.epam.ngb.cli.entity.RequestPayload;
import com.epam.ngb.cli.entity.ResponseResult;
import com.epam.ngb.cli.entity.Role;
import com.epam.ngb.cli.entity.SpeciesEntity;
import com.epam.ngb.cli.entity.UserContext;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.JsonMapper;
import com.epam.ngb.cli.manager.command.CommandConfiguration;
import com.epam.ngb.cli.manager.command.ServerParameters;
import com.epam.ngb.cli.manager.command.handler.simple.AbstractSimpleCommandHandler;
import com.epam.ngb.cli.manager.printer.AbstractResultPrinter;
import com.epam.ngb.cli.manager.request.RequestManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {code {@link AbstractHTTPCommandHandler}} represents a basic class for all
 * commands that send HTTP requests to NGB server. This class provides methods common to
 * all HTTP commands for retrieving supplementary data from the server, request authorization
 * and printing requests results.
 */
public abstract class AbstractHTTPCommandHandler extends AbstractSimpleCommandHandler {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * For serialization and deserialization objects to JSON format
     */
    private final ObjectMapper mapper = JsonMapper.getMapper();

    /**
     * Constant used to determine that an error occurred on the server in response to request
     */
    protected static final String ERROR_STATUS = "ERROR";

    /**
     * Constant used to determine that no errors occurred on the server in response to request
     */
    protected static final String SUCCESS_STATUS = "OK";

    /**
     * Default values for request header initialization
     */
    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE = "content-type";
    private static final String CACHE_CONTROL = "cache-control";
    private static final String CACHE_CONTROL_NO_CACHE = "no-cache";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";
    private static final String PERMISSIONS_URL = "/restapi/grant?id=%s&aclClass=%s";
    private static final String CURRENT_USER_URL = "/restapi/user/current";
    private static final String IS_ROLE_MODEL_ENABLED_URL = "/restapi/isRoleModelEnabled";

    /**
     * Delimiter between path to file and path to index in the input argument string
     */
    private static final String INDEX_DELIMITER = "\\?";

    /**
     * Parameters of a NGB server connection along with some URLs common to all HTTP command handlers
     */
    protected ServerParameters serverParameters;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHTTPCommandHandler.class);

    private Map<String, BiologicalDataItemFormat> additionalFormats;

    /**
     * Default constructor to enable reflexion instance creation
     */
    public AbstractHTTPCommandHandler() {
        //no operations
    }

    /**
     * Creates a new {@code {@link AbstractHTTPCommandHandler}} with a configuration of an
     * external  {@code {@link AbstractHTTPCommandHandler}}
     * @param handler to copy configuration from
     */
    protected AbstractHTTPCommandHandler(AbstractHTTPCommandHandler handler) {
        this.serverParameters = handler.getServerParameters();
        this.configuration = new CommandConfiguration(handler.getConfiguration());
    }

    public ServerParameters getServerParameters() {
        return serverParameters;
    }

    public void setServerParameters(ServerParameters serverParameters) {
        this.serverParameters = serverParameters;
    }

    public String getRequestType() {
        return configuration.getRequestType();
    }

    public String getRequestUrl() {
        return configuration.getRequestUrl();
    }

    public void setRequestUrl(String requestUrl) {
        this.configuration.setRequestUrl(requestUrl);
    }

    public void setRequestType(String requestType) {
        this.configuration.setRequestType(requestType);
    }

    /**
     * Creates an empty {@code {@link HttpRequestBase}} with a specified url according to command
     * configuration
     * @param request URL
     * @return {@code {@link HttpRequestBase}} with a specified URL and type
     */
    protected HttpRequestBase getRequest(String request) {
        String url = serverParameters.getServerUrl() + request;
        return getRequestFromURL(url);
    }

    /**
     * Creates an empty {@code {@link HttpRequestBase}} with a specified url according to command
     * configuration
     * @param url URL
     * @return {@code {@link HttpRequestBase}} with a specified URL and type
     */
    protected HttpRequestBase getRequestFromURL(String url) {
        return getRequestFromURLByType(getRequestType(), url);
    }

    /**
     * Creates an empty {@code {@link HttpRequestBase}} with a specified url according to command
     * configuration
     * @param requestType
     * @param url
     * @return {@code {@link HttpRequestBase}} with a specified URL and type
     */
    protected HttpRequestBase getRequestFromURLByType(String requestType, String url) {
        HttpRequestBase result = selectRequestType(requestType, url);
        setDefaultHeader(result);
        return result;
    }

    /**
     * Prepares {@link HttpPost} multipart request
     * @param url the request URL
     * @param filePath the path to local description file that shall be uploaded to project
     * @return {@link HttpPost} with specified file and URL
     */
    protected HttpPost buildMultipartRequest(final String url, final String filePath) throws IOException {
        final HttpPost request = new HttpPost(url);
        final MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        multipartEntityBuilder.addBinaryBody("file", Files.newInputStream(Paths.get(filePath)),
                ContentType.DEFAULT_BINARY, FilenameUtils.getName(filePath));
        multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        request.setEntity(multipartEntityBuilder.build());

        request.setHeader(CACHE_CONTROL, CACHE_CONTROL_NO_CACHE);
        if (!serverParameters.getJwtAuthenticationToken().isEmpty()) {
            request.setHeader(AUTHORIZATION, BEARER + serverParameters.getJwtAuthenticationToken());
        }

        return request;
    }

    private HttpRequestBase selectRequestType(String requestType, String url) {
        switch (requestType) {
            case HttpPost.METHOD_NAME:
                return new HttpPost(url);
            case HttpGet.METHOD_NAME:
                return new HttpGet(url);
            case HttpDelete.METHOD_NAME:
                return new HttpDelete(url);
            case HttpPut.METHOD_NAME:
                return new HttpPut(url);
            default:
                throw new IllegalArgumentException("Unsupported request type: " + getRequestType());
        }
    }

    /**
     * @return a JSON mapper for objects (de)serialization
     */
    protected ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * Adds a default header for HTTP request
     * @param request to add header
     */
    protected void setDefaultHeader(HttpRequestBase request) {
        request.setHeader(CONTENT_TYPE, APPLICATION_JSON);
        request.setHeader(CACHE_CONTROL, CACHE_CONTROL_NO_CACHE);
        if (!serverParameters.getJwtAuthenticationToken().isEmpty()) {
            request.setHeader(AUTHORIZATION, BEARER + serverParameters.getJwtAuthenticationToken());
        }
    }

    /**
     * Retrieves BiologicalDataItemID for a file from an input String.
     * If input String might be interpreted as a number, this number will be returned as a result.
     * If input String isn't a number, method interprets it as a file name and tries to find a file
     * with such a name in the NGB server and retrieves its ID from a loaded data.
     * @param strId input String for file identification
     * @return BiologicalDataItemID of a file
     * @throws ApplicationException if method fails to find a file
     */
    protected Long parseFileId(String strId) {
        if (NumberUtils.isDigits(strId)) {
            return Long.parseLong(strId);
        } else {
            List<BiologicalDataItem> items = loadItemsByName(strId);
            if (items == null || items.isEmpty()) {
                throw new ApplicationException(getMessage(ERROR_FILE_NOT_FOUND, strId));
            }
            if (items.size() > 1) {
                LOGGER.error(getMessage(SEVERAL_RESULTS_FOR_QUERY, strId));
            }
            return items.get(0).getBioDataItemId();
        }
    }

    /**
     * Loads BiologicalDataItem file from NGB server by an input String.
     * If input String might be interpreted as a number, item will be loaded by BiologicalDataItemID.
     * If input String isn't a number, method interprets it as a file name and tries to find a file
     * with such a name in the NGB server.
     * @param strId input String for file identification
     * @return BiologicalDataItem representing file
     * @throws ApplicationException if method fails to find a file
     */
    protected BiologicalDataItem loadFileByNameOrBioID(String strId) {
        if (NumberUtils.isDigits(strId)) {
            return loadFileByBioID(strId);
        } else {
            List<BiologicalDataItem> items = loadItemsByName(strId);
            if (items == null || items.isEmpty()) {
                throw new ApplicationException(getMessage(ERROR_FILE_NOT_FOUND, strId));
            }
            if (items.size() > 1) {
                LOGGER.error(getMessage(SEVERAL_RESULTS_FOR_QUERY, strId));
            }
            return items.get(0);
        }
    }

    /**
     * Retrieves reference ID from an input String.
     * If input String might be interpreted as a number, this number is assumed to be a BiologicalDataItemID
     * for a reference and thus a reference is loaded from the server by this BiologicalDataItemID.
     * If input String isn't a number, method interprets it as a file name, tries to find a reference
     * with such a name in the NGB server and retrieves its ID from a loaded data.
     * @param strId input String for reference identification
     * @return ID of a reference
     */
    //TODO: remove this method and all connected logic when ID and BiologicalDataItemID are merged on the server
    protected Long loadReferenceId(String strId) {
        if (NumberUtils.isDigits(strId)) {
            BiologicalDataItem reference = loadFileByBioID(strId);
            return reference.getId();
        } else {
            List<BiologicalDataItem> items = loadItemsByName(strId);
            checkLoadedItems(strId, items);
            return items.get(0).getId();
        }
    }

    /**
     * Retrieves ID for a dataset(project) from an input String.
     * If input String might be interpreted as a number, this number will be returned as a result.
     * If input String isn't a number, method interprets it as a dataset name and tries to find a file
     * with such a name in the NGB server and retrieves its ID from a loaded data.
     * @param strId input String for dataset identification
     * @return ID of a dataset
     */
    protected Long parseProjectId(String strId) {
        if (NumberUtils.isDigits(strId)) {
            return Long.parseLong(strId);
        } else {
            Project project = loadProjectByName(strId);
            return project.getId();
        }
    }

    /**
     * Loads a reference ID for a dataset(project) specified by ID.
     * Method loads a dataset from NGB by ID and finds a reference in it.
     * @param datasetId find reference for
     * @return ID of a reference
     */
    protected Long loadReferenceIdFromDataset(Long datasetId) {
        Project project = loadProject(datasetId);
        for (BiologicalDataItem item : project.getItems()) {
            if (item.getFormat() == BiologicalDataItemFormat.REFERENCE) {
                return item.getId();
            }
        }
        throw new ApplicationException(String.format("No reference specified for dataset with id %d.",
                datasetId));
    }

    Map<String, BiologicalDataItemFormat> getAdditionalFormats() {
        if (additionalFormats == null) {
            additionalFormats = fetchAdditionalFormats();
        }
        return additionalFormats;
    }

    protected Map<String, BiologicalDataItemFormat> fetchAdditionalFormats() {
        final HttpRequestBase request = getRequestFromURLByType(
                HttpGet.METHOD_NAME, serverParameters.getServerUrl() + serverParameters.getFormatsUrl()
        );
        final String result = RequestManager.executeRequest(request);
        try {
            final ResponseResult<Map<String, BiologicalDataItemFormat>> responseResult = getMapper().readValue(result,
                    new TypeReference<ResponseResult<Map<String, BiologicalDataItemFormat>>>() {});
            if (responseResult == null || responseResult.getPayload() == null) {
                throw new ApplicationException(getMessage(ERROR_DATAITEM_FORMATS_NOT_FOUND));
            }
            return responseResult.getPayload();
        } catch (IOException e) {
            throw new ApplicationException(getMessage(ERROR_DATAITEM_FORMATS_NOT_FOUND));
        }
    }

    /**
     * Finds files on the NGB server with a name matching input query.
     * @param strId query to find
     * @param strict determines type of search, if true a strict equality by name search is performed
     *               otherwise a substring case-insensitive search is done
     * @return list of files matching a query
     */
    protected List<BiologicalDataItem> loadItemsByName(String strId, boolean strict) {
        try {
            URI uri = new URIBuilder(serverParameters.getServerUrl() + serverParameters.getSearchUrl())
                    .addParameter("name", strId)
                    .addParameter("strict", String.valueOf(strict))
                    .build();
            HttpRequestBase request = getRequestFromURLByType(HttpGet.METHOD_NAME, uri.toString());
            String result = RequestManager.executeRequest(request);
            ResponseResult<List<BiologicalDataItem>> responseResult = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructParametrizedType(
                            ResponseResult.class, ResponseResult.class,
                            getMapper().getTypeFactory().constructParametrizedType(
                                    List.class, List.class, BiologicalDataItem.class)));
            if (responseResult == null) {
                throw new ApplicationException(getMessage(ERROR_FILE_NOT_FOUND, strId));
            }
            return responseResult.getPayload();
        } catch (IOException | URISyntaxException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Performs a POST HTTP request to register some data on NGB server
     * @param requestObject filled request object for the server
     * @param post request to performs
     * @return response value as a String
     */
    protected String getPostResult(RequestPayload requestObject, HttpPost post) {
        try {
            post.setEntity(new StringEntity(getMapper().writeValueAsString(requestObject)));
            return RequestManager.executeRequest(post);
        } catch (UnsupportedEncodingException | JsonProcessingException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Performs an HTTP request to delete some data, specified by ID, from NGB server
     * @param id of the data to delete
     */
    protected void runDeletion(Long id) {
        String url = String.format(getRequestUrl(), id);
        HttpRequestBase request = getRequest(url);
        String result = RequestManager.executeRequest(request);
        try {
            ResponseResult response = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructType(ResponseResult.class));
            LOGGER.info(response.getStatus() + "\t" + response.getMessage());
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Checks that file registration request completed successfully,
     * serializes a request result to an {@code {@link BiologicalDataItem}} object and prints it
     * to StdOut, id it's required by the print options
     * @param result of a registration result
     * @param printJson if true, result wil be printed in Json format
     * @param printTable if true, result wil be printed in table format
     */
    protected void checkAndPrintRegistrationResult(String result, boolean printJson, boolean printTable) {
        try {
            ResponseResult<BiologicalDataItem> responseResult = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                            BiologicalDataItem.class));
            if (!SUCCESS_STATUS.equals(responseResult.getStatus())) {
                throw new ApplicationException(responseResult.getMessage());
            }
            if (printJson || printTable) {
                List<BiologicalDataItem> items =
                        Collections.singletonList(responseResult.getPayload());
                AbstractResultPrinter printer = AbstractResultPrinter.getPrinter(printTable,
                        items.get(0).getFormatString(items));
                printer.printHeader(items.get(0));
                items.forEach(printer::printItem);
            }
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    protected <T> void checkAndPrintResult(String result, boolean printJson, boolean printTable, Class<T> respClass) {
        try {
            ResponseResult<T> responseResult = getMapper().readValue(result, getMapper().getTypeFactory()
                .constructParametrizedType(ResponseResult.class, ResponseResult.class, respClass));
            if (!SUCCESS_STATUS.equals(responseResult.getStatus())) {
                throw new ApplicationException(responseResult.getMessage());
            }
            if (printJson || printTable) {
                AbstractResultPrinter printer = AbstractResultPrinter.getPrinter(printTable, "%s");
                printer.printSimple(responseResult.getPayload().toString());
            }
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    protected List<NgbUser> loadListOfUsers(List<String> userNames) {
        IDList names = new IDList(userNames);
        HttpPost request = (HttpPost) getRequestFromURLByType("POST",
                getServerParameters().getServerUrl() + getServerParameters().getFindUsersUrl());
        String result = getPostResult(names, request);
        try {
            ResponseResult<List<NgbUser>> responseResult = getMapper()
                    .readValue(result, getMapper().getTypeFactory()
                            .constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                    getMapper().getTypeFactory()
                                            .constructParametrizedType(List.class, List.class,
                                                    NgbUser.class)));
            if (responseResult.getPayload() == null) {
                return Collections.emptyList();
            }
            return responseResult.getPayload();
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    protected Role loadRoleByName(String name) {
        HttpRequestBase request = getRequestFromURLByType("GET", getServerParameters().getServerUrl()
                + String.format(getServerParameters().getRoleUrl(), name));
        String result = RequestManager.executeRequest(request);
        try {
            ResponseResult<Role> responseResult = getMapper()
                    .readValue(result, getMapper().getTypeFactory()
                            .constructParametrizedType(ResponseResult.class, ResponseResult.class, Role.class));
            return responseResult.getPayload();
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Checks if result contains no errors. If it does, throws {@link ApplicationException}
     * @param result request result
     */
    protected void isResultOk(String result) {
        try {
            ResponseResult<Object> responseResult = getMapper().readValue(result, getMapper().getTypeFactory()
                .constructParametrizedType(ResponseResult.class, ResponseResult.class, Object.class));
            if (!SUCCESS_STATUS.equals(responseResult.getStatus())) {
                throw new ApplicationException(responseResult.getMessage());
            }
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    protected <T> T getResult(String result, Class<T> respClass) {
        try {
            ResponseResult<T> responseResult = getMapper().readValue(result, getMapper().getTypeFactory()
                .constructParametrizedType(ResponseResult.class, ResponseResult.class, respClass));
            if (!SUCCESS_STATUS.equals(responseResult.getStatus())) {
                throw new ApplicationException(responseResult.getMessage());
            }
            return responseResult.getPayload();
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Checks that species registration request completed successfully,
     * serializes a request result to an {@code {@link SpeciesEntity}} object and prints it
     * to StdOut, id it's required by the print options
     * @param result of a registration result
     * @param printJson if true, result wil be printed in Json format
     * @param printTable if true, result wil be printed in table format
     */
    protected void checkAndPrintSpeciesResult(String result, boolean printJson, boolean printTable) {
        try {
            ResponseResult<SpeciesEntity> responseResult = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                            SpeciesEntity.class));
            if (!SUCCESS_STATUS.equals(responseResult.getStatus())) {
                throw new ApplicationException(responseResult.getMessage());
            } else {
                if (printJson || printTable) {
                    List<SpeciesEntity> items =
                            Collections.singletonList(responseResult.getPayload());
                    AbstractResultPrinter printer = AbstractResultPrinter.getPrinter(printTable,
                            items.get(0).getFormatString(items));
                    printer.printHeader(items.get(0));
                    items.forEach(printer::printItem);
                }
            }
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Checks that dataset(project) registration request completed successfully,
     * serializes a request result to an {@code {@link Project}} object and prints it
     * to StdOut, id it's required by the print options
     * @param result of a registration result
     * @param printJson if true, result wil be printed in Json format
     * @param printTable if true, result wil be printed in table format
     */
    protected void checkAndPrintDatasetResult(String result, boolean printJson, boolean printTable) {
        try {
            ResponseResult<Project> responseResult = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                            Project.class));
            if (!SUCCESS_STATUS.equals(responseResult.getStatus())) {
                throw new ApplicationException(responseResult.getMessage());
            }
            if (printJson || printTable) {
                List<Project> items =
                        Collections.singletonList(responseResult.getPayload());
                AbstractResultPrinter printer = AbstractResultPrinter.getPrinter(printTable,
                        items.get(0).getFormatString(items));
                printer.printHeader(items.get(0));
                items.forEach(printer::printItem);
            }
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Performs a request to add or remove files from a dataset on NGB server
     * @param items to add or remove from a dataset
     * @param datasetId specifies dataset
     * @param printJson if true, result wil be printed in Json format
     * @param printTable if true, result wil be printed in table format
     */
    protected void addOrDeleteItemsFromDataset(List<Long> items, Long datasetId,
            boolean printJson, boolean printTable) {
        String result = null;
        for (Long id : items) {
            String url = String.format(getRequestUrl(), datasetId, id);
            HttpRequestBase request = getRequest(url);
            result = RequestManager.executeRequest(request);
        }
        if (result != null) {
            checkAndPrintDatasetResult(result, printJson, printTable);
        }
    }

    protected Project loadProject(Long datasetId) {
        HttpRequestBase request = getRequestFromURLByType(HttpGet.METHOD_NAME, serverParameters.getServerUrl()
                + String.format(serverParameters.getProjectLoadByIdUrl(), datasetId));
        String result = RequestManager.executeRequest(request);
        try {
            ResponseResult<Project> responseResult = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructParametrizedType(ResponseResult.class,
                            ResponseResult.class, Project.class));
            if (responseResult == null || responseResult.getPayload() == null) {
                throw new ApplicationException(getMessage(ERROR_PROJECT_NOT_FOUND, datasetId));
            }
            return responseResult.getPayload();
        } catch (IOException e) {
            throw new ApplicationException(getMessage(ERROR_PROJECT_NOT_FOUND, datasetId));
        }

    }

    /**
     * Performs an HTTP request to load permissions for specified entity.
     * @param entityId entity ID
     * @param entityClass entity acl class
     * @return entity and it's acl permissions
     */
    protected AclSecuredEntry loadPermissions(Long entityId, AclClass entityClass) {
        HttpRequestBase request = getRequestFromURLByType(HttpGet.METHOD_NAME, serverParameters.getServerUrl()
                + String.format(PERMISSIONS_URL, entityId, entityClass));
        try {
            String result = RequestManager.executeRequest(request);
            ResponseResult<AclSecuredEntry> responseResult = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructParametrizedType(ResponseResult.class,
                            ResponseResult.class, AclSecuredEntry.class));
            if (responseResult == null || responseResult.getPayload() == null) {
                throw new ApplicationException(getMessage(ERROR_PERMISSIONS_NOT_FOUND, entityClass, entityId));
            }
            return responseResult.getPayload();
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Performs an HTTP request to load user info.
     * @return user info
     */
    protected UserContext loadCurrentUser() throws IOException {
        HttpRequestBase request = getRequestFromURLByType(HttpGet.METHOD_NAME, serverParameters.getServerUrl()
                + CURRENT_USER_URL);
        String result = RequestManager.executeRequest(request);
        ResponseResult<UserContext> responseResult = getMapper().readValue(result,
                getMapper().getTypeFactory().constructParametrizedType(ResponseResult.class,
                        ResponseResult.class, UserContext.class));
        if (responseResult == null || responseResult.getPayload() == null) {
            throw new ApplicationException(ERROR_FAILED_TO_LOAD_USER);
        }
        return responseResult.getPayload();
    }

    protected Reference loadReferenceById(Long referenceId) {
        try {
            HttpRequestBase request = getRequestFromURLByType(HttpGet.METHOD_NAME, serverParameters.getServerUrl()
                    + String.format(serverParameters.getLoadReferenceUrl(), referenceId));
            String result = RequestManager.executeRequest(request);
            ResponseResult<Reference> responseResult = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                            Reference.class));
            if (responseResult == null) {
                throw new ApplicationException(getMessage(ERROR_FILE_NOT_FOUND, referenceId));
            }
            return responseResult.getPayload();
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    protected void removeGenes(Long referenceId) {
        HttpRequestBase request = getRequestFromURLByType(HttpPut.METHOD_NAME, serverParameters.getServerUrl()
                + String.format(serverParameters.getRemoveGeneUrl(), referenceId));
        RequestManager.executeRequest(request);
    }

    protected void removeAnnotation(Long referenceId, Long annotationFileId) {
        try {
            URI uri = new URIBuilder(serverParameters.getServerUrl()
                    + String.format(serverParameters.getUpdateAnnotationUrl(), referenceId))
                    .addParameter("annotationFileId", String.valueOf(annotationFileId))
                    .addParameter("remove", String.valueOf(true))
                    .build();
            HttpRequestBase request = getRequestFromURLByType(HttpPut.METHOD_NAME, uri.toString());
            String result = RequestManager.executeRequest(request);
            ResponseResult response = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructType(ResponseResult.class));
            LOGGER.info(response.getStatus() + "\t" + response.getMessage());
        } catch (IOException | URISyntaxException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    protected void deleteGeneFile(Long geneId) {
        try {
            URI uri = new URIBuilder(serverParameters.getServerUrl() + serverParameters.getDeleteGeneUrl())
                    .addParameter("geneFileId", String.valueOf(geneId))
                    .build();
            HttpRequestBase request = getRequestFromURLByType(HttpDelete.METHOD_NAME, uri.toString());
            String result = RequestManager.executeRequest(request);
            ResponseResult response = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructType(ResponseResult.class));
            LOGGER.info(response.getStatus() + "\t" + response.getMessage());
        } catch (IOException | URISyntaxException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    protected void deleteItem(Long bioDataItemId) {
        try {
            URI uri = new URIBuilder(serverParameters.getServerUrl() + serverParameters.getDeleteItemUrl())
                    .addParameter("id", String.valueOf(bioDataItemId))
                    .build();
            HttpRequestBase request = getRequestFromURLByType(HttpDelete.METHOD_NAME, uri.toString());
            String result = RequestManager.executeRequest(request);
            ResponseResult response = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructType(ResponseResult.class));
            LOGGER.info(response.getStatus() + "\t" + response.getMessage());
        } catch (IOException | URISyntaxException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Performs an HTTP request to check that role model enabled on the server.
     * @return user info
     */
    protected boolean isRoleModelEnable() {
        HttpRequestBase request = getRequestFromURLByType(HttpGet.METHOD_NAME, serverParameters.getServerUrl()
                + IS_ROLE_MODEL_ENABLED_URL);
        String result = RequestManager.executeRequest(request);
        try {
            ResponseResult<Boolean> responseResult = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructParametrizedType(ResponseResult.class,
                            ResponseResult.class, Boolean.class));
            if (responseResult == null || responseResult.getPayload() == null) {
                return false;
            }
            return responseResult.getPayload();
        } catch (IOException e) {
            return false;
        }
    }

    private BiologicalDataItem loadFileByBioID(String id) {
        try {
            URI uri = new URIBuilder(serverParameters.getServerUrl() + serverParameters.getFileFindUrl())
                    .addParameter("id", id)
                    .build();
            HttpRequestBase request = getRequestFromURLByType(HttpGet.METHOD_NAME, uri.toString());
            String result = RequestManager.executeRequest(request);
            ResponseResult<BiologicalDataItem> responseResult = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                            BiologicalDataItem.class));
            if (responseResult == null || responseResult.getPayload() == null) {
                throw new ApplicationException("Failed to find a file by Bio item ID: " + id + ".");
            }
            return responseResult.getPayload();
        } catch (IOException | URISyntaxException e) {
            throw new ApplicationException("", e);
        }
    }

    protected Project loadProjectByName(String strId) {
        try {
            URI uri = new URIBuilder(serverParameters.getServerUrl() + serverParameters.getProjectLoadUrl())
                    .addParameter("projectName", strId)
                    .build();
            HttpGet get = new HttpGet(uri);
            setDefaultHeader(get);
            String result = RequestManager.executeRequest(get);
            ResponseResult<Project> responseResult = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                            Project.class));
            if (responseResult == null || responseResult.getPayload() == null) {
                throw new ApplicationException("Failed to find a project by name: " + strId + ".");
            }
            return responseResult.getPayload();
        } catch (IOException | URISyntaxException e) {
            throw new ApplicationException("", e);
        }
    }

    private List<BiologicalDataItem> loadItemsByName(String strId) {
        return loadItemsByName(strId, true);
    }

    private void checkLoadedItems(String strId, List<BiologicalDataItem> items) {
        if (items == null || items.isEmpty()) {
            throw new ApplicationException(getMessage(ERROR_REFERENCE_NOT_FOUND, strId));
        }
        if (items.size() > 1) {
            LOGGER.info(getMessage(SEVERAL_RESULTS_FOR_QUERY, strId));
        }
        if (items.get(0).getFormat() != BiologicalDataItemFormat.REFERENCE) {
            throw new ApplicationException(getMessage(ERROR_REFERENCE_NOT_FOUND, strId));
        }
    }

    /**
     * Method splits path for file registartion into file-index pair
     * @param path input CLI argument
     * @return pair, where left part is path to file and rights one optional path to index
     */
    protected Pair<String, String> parseAndVerifyFilePath(final String path) {
        Pair<String, String> fileWithIndex = splitFilePath(path);
        String fileAbsolutePath = fileWithIndex.getLeft();
        if (getTypeFromPath(fileAbsolutePath) == FILE) {
            fileAbsolutePath = Utils.getNormalizeAndAbsolutePath(fileWithIndex.getLeft());
        }
        if (fileWithIndex.getRight() == null) {
            fileWithIndex = setIndexPathFromServer(fileAbsolutePath, fileWithIndex);
        }

        final BiologicalDataItemFormat format = BiologicalDataItemFormat.getByFilePath(
                fileWithIndex.getLeft(), getAdditionalFormats()
        );
        if (format.isRequireIndex() && fileWithIndex.getRight() == null) {
            throw new IllegalArgumentException(getMessage(ERROR_INDEX_REQUIRED, fileWithIndex.getLeft()));
        }

        String index = null;
        if (fileWithIndex.getRight() != null) {
            boolean indexSupported = format.verifyIndex(fileWithIndex.getRight());
            //if server doesn't support a given index, but index is also not required
            //we don't pass it to server
            if (indexSupported) {
                index = fileWithIndex.getRight();
                if (getTypeFromPath(index) == FILE) {
                    index = Utils.getNormalizeAndAbsolutePath(index);
                }
            }
        }
        return Pair.of(fileAbsolutePath, index);
    }

    /**
     * Checks if user is admin
     * @return true if current user has ROLE_ADMIN
     */
    public boolean isCurrentUserIsAdmin() {
        try {
            return loadCurrentUser().getRoles().stream()
                    .anyMatch(role -> role.getName().equalsIgnoreCase(ROLE_ADMIN));
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    private Pair<String, String> setIndexPathFromServer(String path, Pair<String, String> fileWithIndex) {
        Pair<String, String> fileWithIndexFromServer = fileWithIndex;
        try {
            URI existingIndexGetterUri = new URIBuilder(serverParameters.getServerUrl()
                    + serverParameters.getExistingIndexSearchUrl()).addParameter("filePath", path).build();
            HttpRequestBase request = getRequestFromURLByType(HttpGet.METHOD_NAME, existingIndexGetterUri.toString());
            String result = RequestManager.executeRequest(request);
            ResponseResult<String> responseResult = getMapper().readValue(result, getMapper().getTypeFactory()
                    .constructParametrizedType(ResponseResult.class, ResponseResult.class,
                            getMapper().getTypeFactory()
                                    .constructType(String.class)));
            String indexFromServer = responseResult.getPayload();
            if (indexFromServer != null) {
                fileWithIndexFromServer = Pair.of(fileWithIndex.getLeft(), indexFromServer);
            }
        } catch (URISyntaxException | IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
        return fileWithIndexFromServer;
    }

    private Pair<String, String> splitFilePath(String path) {
        String[] paths = path.split(INDEX_DELIMITER);
        if (paths.length > 2) {
            throw new IllegalArgumentException(getMessage(ILLEGAL_PATH_FORMAT, path));
        }
        if (paths.length == 1) {
            return new ImmutablePair<>(paths[0], null);
        } else {
            return new ImmutablePair<>(paths[0], paths[1]);
        }
    }
}
