/*
 * MIT License
 *
 * Copyright (c) 2022 EPAM Systems
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
import com.epam.ngb.cli.entity.ResponseResult;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.request.RequestManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;
import static com.epam.ngb.cli.constants.MessageConstants.getMessage;

@Slf4j
public abstract class AbstractEntityRenameHandler extends AbstractHTTPCommandHandler {

    private String name;
    private String newName;
    private String newPrettyName;

    /**
     * Verifies input arguments
     * @param arguments command line arguments
     * @param options
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException(getMessage(ILLEGAL_COMMAND_ARGUMENTS,
                    getCommand(), 1, arguments.size()));
        }
        name = arguments.get(0);
        newName = options.getName();
        newPrettyName = options.getPrettyName();
    }

    @Override
    public int runCommand() {
        try {
            String url = serverParameters.getServerUrl() + getRequestUrl();
            URIBuilder builder = new URIBuilder(String.format(url, name));
            if (StringUtils.isNotBlank(newName)) {
                builder.addParameter("newName", newName);
            }
            if (StringUtils.isNotBlank(newPrettyName)) {
                builder.addParameter("newPrettyName", newPrettyName);
            }
            HttpRequestBase request = getRequestFromURLByType(HttpPut.METHOD_NAME, builder.build().toString());
            String result = RequestManager.executeRequest(request);
            ResponseResult response = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructType(ResponseResult.class));
            if (!SUCCESS_STATUS.equals(response.getStatus())) {
                log.info(response.getStatus() + "\t" + response.getMessage());
            }
        } catch (IOException | URISyntaxException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
        return 0;
    }
}
