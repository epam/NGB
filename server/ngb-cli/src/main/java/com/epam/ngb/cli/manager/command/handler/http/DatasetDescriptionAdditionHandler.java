/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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
import com.epam.ngb.cli.app.Utils;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.Project;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.request.RequestManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

/**
 * This class represents a tool that provides an ability to add description file to a dataset
 */
@Command(type = Command.Type.REQUEST, command = {"add_description"})
@Slf4j
public class DatasetDescriptionAdditionHandler extends AbstractHTTPCommandHandler {

    private Long projectId;
    private String path;

    @Override
    public void parseAndVerifyArguments(final List<String> arguments, final ApplicationOptions options) {
        if (arguments.isEmpty() || arguments.size() != 2) {
            throw new IllegalArgumentException(MessageConstants.getMessage(ILLEGAL_COMMAND_ARGUMENTS,
                    getCommand(), 2, arguments.size()));
        }
        projectId = parseProjectId(arguments.get(0));
        path = Utils.getNormalizeAndAbsolutePath(arguments.get(1));
    }

    @Override
    public int runCommand() {
        final String url = serverParameters.getServerUrl() + getRequestUrl();
        try {
            final HttpPost request = buildMultipartRequest(String.format(url, projectId), path);
            final String result = RequestManager.executeRequest(request);
            checkAndPrintResult(result, false, false, Project.class);

            log.info("Description successfully added to dataset '{}'", projectId);
            return 0;
        } catch (IOException | ApplicationException e) {
            log.info(e.getMessage());
            return 1;
        }
    }

}
