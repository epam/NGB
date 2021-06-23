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
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.Project;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.request.RequestManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

import java.util.List;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

/**
 * This class represents a tool that provides an ability to remove description for dataset
 */
@Command(type = Command.Type.REQUEST, command = {"remove_description"})
@Slf4j
public class DatasetDescriptionDeletionHandler extends AbstractHTTPCommandHandler {

    private Long projectId;

    @Override
    public void parseAndVerifyArguments(final List<String> arguments, final ApplicationOptions options) {
        if (arguments.isEmpty() || arguments.size() != 1) {
            throw new IllegalArgumentException(MessageConstants.getMessage(ILLEGAL_COMMAND_ARGUMENTS,
                    getCommand(), 1, arguments.size()));
        }
        projectId = parseProjectId(arguments.get(0));
    }

    @Override
    public int runCommand() {
        final String url = serverParameters.getServerUrl() + getRequestUrl();
        final HttpRequestBase request = getRequestFromURLByType(HttpDelete.METHOD_NAME, String.format(url, projectId));
        final String result = RequestManager.executeRequest(request);
        try {
            checkAndPrintResult(result, false, false, Project.class);
        } catch (ApplicationException e) {
            log.info(e.getMessage());
            return 1;
        }

        log.info("Description for dataset '{}' successfully removed", projectId);
        return 0;
    }
}
