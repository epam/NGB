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

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.ResponseResult;
import com.epam.ngb.cli.entity.SpeciesEntity;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.printer.AbstractResultPrinter;
import com.epam.ngb.cli.manager.request.RequestManager;
import java.io.IOException;
import java.util.List;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *{@code {@link SpeciesListHandler}} represents a tool for handling 'list_species' command and
 * listing species, registered on NGB server. This command doesn't support input arguments.
 */
@Command(type = Command.Type.REQUEST, command = {"list_species"})
public class SpeciesListHandler extends AbstractHTTPCommandHandler {

    private boolean printTable;

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeciesListHandler.class);

    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (!arguments.isEmpty()) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                ILLEGAL_COMMAND_ARGUMENTS, getCommand(), 0, arguments.size()));
        }
        this.printTable = options.isPrintTable();
    }

    @Override
    public int runCommand() {
        HttpRequestBase request = getRequest(getRequestUrl());
        String result = RequestManager.executeRequest(request);
        ResponseResult<List<SpeciesEntity>> responseResult;
        try {
            responseResult = getMapper().readValue(result,
                getMapper().getTypeFactory().constructParametrizedType(
                    ResponseResult.class, ResponseResult.class,
                    getMapper().getTypeFactory().constructParametrizedType(
                        List.class, List.class, SpeciesEntity.class)));
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
        if (!SUCCESS_STATUS.equals(responseResult.getStatus())) {
            throw new ApplicationException(responseResult.getMessage());
        }
        if (responseResult.getPayload() == null ||
            responseResult.getPayload().isEmpty()) {
            LOGGER.info("No species registered on the server.");
        } else {
            List<SpeciesEntity> items = responseResult.getPayload();
            AbstractResultPrinter printer = AbstractResultPrinter
                .getPrinter(printTable, items.get(0).getFormatString(items));
            printer.printHeader(items.get(0));
            items.forEach(printer::printItem);
        }
        return 0;
    }
}
