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
 *
 */

package com.epam.ngb.cli.manager.command.handler.http;

import static com.epam.ngb.cli.constants.MessageConstants.MINIMUM_COMMAND_ARGUMENTS;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.BiologicalDataItem;
import com.epam.ngb.cli.entity.ResponseResult;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.request.RequestManager;

/**
 * Source:      IndexationHandler
 * Created:     16.12.16, 16:04
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 15.0.3, JDK 1.8
 *
 * {@code {@link FileIndexingHandler }} represents a tool handling "index_file" command and
 * sending request to NGB server for file reindexing. This command requires at least one argument:
 * file ID or name
 */
@Command(type = Command.Type.REQUEST, command = {"index_file"})
public class FileIndexingHandler extends AbstractHTTPCommandHandler {
    private BiologicalDataItem item;
    private boolean createTabixIndex;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileIndexingHandler.class);

    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.isEmpty() || arguments.size() != 1) {
            throw new IllegalArgumentException(MessageConstants.getMessage(MINIMUM_COMMAND_ARGUMENTS,
                                                                           getCommand(), 1, arguments.size()));
        }
        item = loadFileByNameOrBioID(arguments.get(0));
        createTabixIndex = options.isCreateTabixIndex();
    }

    @Override
    public int runCommand() {
        String url = String.format(getRequestUrl(), item.getFormat().toString().toLowerCase(), item.getId());
        if (createTabixIndex) {
            url +="?createTabixIndex=true";
        }
        HttpRequestBase request = getRequest(url);
        String result = RequestManager.executeRequest(request);

        try {
            ResponseResult response = getMapper().readValue(result, getMapper().getTypeFactory().constructType(
                ResponseResult.class));
            LOGGER.info(response.getStatus() + "\t" + response.getMessage());
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }

        return 0;
    }
}
