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
import java.util.List;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.ResponseResult;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.request.RequestManager;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code {@link DatasetDeletionHandler}} represents a tool for handling 'delete_dataset' command and
 * deleting a dataset(ex-projects) from NGB server. This command requires strictly one argument:
 * dataset ID or name.
 */
@Command(type = Command.Type.REQUEST, command = {"delete_dataset"})
public class DatasetDeletionHandler extends AbstractHTTPCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetDeletionHandler.class);

    /**
     * ID of dataset to delete
     */
    private Long projectId;

    private boolean force;

    /**
     * Verifies that input arguments contain the required parameter:
     * the first and the only one argument must be dataset ID or name.
     * If dataset's name is provided, it's ID will be loaded from the NGB server.
     * @param arguments command line arguments for 'delete_dataset' command
     * @param options aren't used in this command
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.isEmpty() || arguments.size() != 1) {
            throw new IllegalArgumentException(MessageConstants.getMessage(ILLEGAL_COMMAND_ARGUMENTS,
                    getCommand(), 1, arguments.size()));
        }
        projectId = parseProjectId(arguments.get(0));
        force = options.isForceDeletion();
    }

    /**
     * Performs a dataset deletion request to NGB server
     * @return 0 if request completed successfully
     */
    @Override public int runCommand() {
        runDeletion(projectId);
        return 0;
    }

    @Override protected void runDeletion(Long id) {
        String url = String.format(getRequestUrl(), id);
        try {
            URIBuilder requestBuilder = new URIBuilder(String.format(url, projectId));
            requestBuilder.setParameter("force", String.valueOf(force));
            HttpRequestBase request = getRequest(requestBuilder.build().toString());

            setDefaultHeader(request);
            if (isSecure()) {
                addAuthorizationToRequest(request);
            }

            String result = RequestManager.executeRequest(request);
            ResponseResult response = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructType(ResponseResult.class));
            LOGGER.info(response.getStatus() + "\t" + response.getMessage());
        } catch (IOException | URISyntaxException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }
}
