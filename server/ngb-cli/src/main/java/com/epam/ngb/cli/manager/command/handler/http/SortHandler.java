/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.BiologicalDataItemFormat;
import com.epam.ngb.cli.entity.ResponseResult;
import com.epam.ngb.cli.entity.SortRequest;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static com.epam.ngb.cli.constants.MessageConstants.MINIMUM_COMMAND_ARGUMENTS;

/**
 * {@code {@link SortHandler }} represents a tool handling "sort" command and
 * sending request to NGB server for file sorting. This command requires at least one arguments:
 * path to a file to be sorted. Target location of sorted file can also be given.
 */
@Command(type = Command.Type.REQUEST, command = {"sort"})
public class SortHandler extends AbstractHTTPCommandHandler {

    /**
     * Path to file to be sorted
     */
    private String originalFilePath;

    /**
     * Location for sorted file. Optional.
     */
    private String sortedFilePath;

    /**
     * Amount of memory in megabytes to use when sorting.
     */
    private int maxMemory;

    private static final Logger LOGGER = LoggerFactory.getLogger(SortHandler.class);

    /**
     * Verifies that input arguments contain the required parameters:
     * first argument must be original file name, second argument specify location of
     * target sorted file. Second parameter is optional. Method also checks that input files are
     * supported by NGB server.
     * @param arguments command line arguments for 'sort' command
     * @param options - unused
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException(MessageConstants.getMessage(MINIMUM_COMMAND_ARGUMENTS,
                    getCommand(), 1, arguments.size()));
        }

        originalFilePath = arguments.get(0);

        if (arguments.size() > 1) {
            sortedFilePath = arguments.get(1);
        }

        maxMemory = options.getMaxMemory();

        if (maxMemory < 0) {
            throw new IllegalArgumentException(MessageConstants.getMessage(MessageConstants.ERROR_NEGATIVE_MEMORY));
        }

        BiologicalDataItemFormat format = BiologicalDataItemFormat.getByFilePath(
                originalFilePath, getAdditionalFormats()
        );
        if (format != BiologicalDataItemFormat.VCF
                && format != BiologicalDataItemFormat.GENE
                && format != BiologicalDataItemFormat.BED) {
            throw new IllegalArgumentException(
                    MessageConstants.getMessage(MessageConstants.ERROR_UNSUPPORTED_FORMAT, format.toString()));
        }
    }

    /**
     * Performs a file sort request to NGB server.
     * @return 0 if request completed successfully
     */
    @Override
    public int runCommand() {
        SortRequest sortRequest = prepareSortRequest();
        ResponseResult<String> responseResult;

        try {
            HttpPost post = prepareHttpPost();
            String result = getPostResult(sortRequest, post);
            responseResult = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructParametrizedType(ResponseResult.class,
                            ResponseResult.class, String.class));
            if (!SUCCESS_STATUS.equals(responseResult.getStatus())) {
                LOGGER.error(responseResult.getMessage());
                return 1;
            }
        } catch (IOException | URISyntaxException e) {
            throw new ApplicationException(e.getMessage(), e);
        }

        LOGGER.info(MessageConstants.getMessage(MessageConstants.INFO_SORT_SUCCESS, responseResult.getPayload()));
        return 0;
    }

    private SortRequest prepareSortRequest() {
        SortRequest sortRequest = new SortRequest();
        sortRequest.setOriginalFilePath(originalFilePath);
        if (sortedFilePath != null && !sortedFilePath.isEmpty()) {
            sortRequest.setSortedFilePath(sortedFilePath);
        }

        if (maxMemory > 0) {
            sortRequest.setMaxMemory(maxMemory);
        }

        return sortRequest;
    }

    private HttpPost prepareHttpPost() throws URISyntaxException {
        URIBuilder builder = new URIBuilder(serverParameters.getServerUrl() + getRequestUrl());
        return (HttpPost) getRequestFromURLByType(HttpPost.METHOD_NAME, builder.build().toString());
    }

}
