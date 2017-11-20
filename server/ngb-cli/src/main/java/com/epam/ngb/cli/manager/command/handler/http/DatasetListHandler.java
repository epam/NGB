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

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.Project;
import com.epam.ngb.cli.entity.ResponseResult;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.printer.AbstractResultPrinter;
import com.epam.ngb.cli.manager.request.RequestManager;

/**
 * {@code {@link DatasetListHandler}} represents a tool for handling 'list_datasets' command and
 * listing datasets (ex-projects) , registered on NGB server. This command doesn't support input arguments.
 * By default only top-level datasets are listed, the nested datasets aren't listed.
 * Loading a single project hierarchy is enabled by the 'parent' option, if it is specified,
 * the command will list the part of the whole dataset tree, with the specified 'parent' in the root.
 */
@Command(type = Command.Type.REQUEST, command = {"list_datasets"})
public class DatasetListHandler extends AbstractHTTPCommandHandler {

    /**
     * If true command will output list of datasets in a table format, otherwise
     * json format will be used
     */
    private boolean printTable;

    /**
     * Specifies the root of the dataset hierarchy to load
     */
    private Long parentId;

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetListHandler.class);

    /**
     * Verifies that input arguments are empty and gets printing options
     * @param arguments command line arguments for 'list_datasets' command
     * @param options to specify output options
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (!arguments.isEmpty()) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ILLEGAL_COMMAND_ARGUMENTS, getCommand(), 0, arguments.size()));
        }
        this.printTable = options.isPrintTable();
        if (options.getParent() != null) {
            this.parentId = parseProjectId(options.getParent());
        }
    }

    /**
     * Performs a dataset list request to NGB server and its result to
     * StdOut. The performed request varies according to the presence of 'parent' option.
     * @return 0 if request completed successfully
     */
    @Override public int runCommand() {
        HttpRequestBase request = parentId == null ? createListingRequest() : createTreeRequest(parentId);
        String result = RequestManager.executeRequest(request);
        ResponseResult<List<Project>> responseResult;
        try {
            responseResult = getMapper().readValue(result, getMapper().getTypeFactory()
                    .constructParametrizedType(ResponseResult.class, ResponseResult.class,
                            getMapper().getTypeFactory()
                                    .constructParametrizedType(List.class, List.class,
                                            Project.class)));
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
        if (!SUCCESS_STATUS.equals(responseResult.getStatus())) {
            throw new ApplicationException(responseResult.getMessage());
        }
        if (responseResult.getPayload() == null || responseResult.getPayload().isEmpty()) {
            LOGGER.info("No datasets registered on the server.");
        } else {
            List<Project> items = responseResult.getPayload();
            items.sort(Comparator.comparing(Project::getId));
            AbstractResultPrinter printer = AbstractResultPrinter
                    .getPrinter(printTable, items.get(0).getFormatString(items));
            printer.printHeader(items.get(0));
            items.forEach(printer::printItem);
        }
        return 0;
    }

    private HttpRequestBase createTreeRequest(Long parentId) {
        try {
            URIBuilder builder = new URIBuilder(serverParameters.getServerUrl()
                    + serverParameters.getProjectTreeUrl());
            builder.addParameter("parentId", String.valueOf(parentId));
            return getRequestFromURLByType(HttpGet.METHOD_NAME, builder.build().toString());
        } catch (URISyntaxException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    private HttpRequestBase createListingRequest() {
        return getRequest(getRequestUrl());
    }
}
