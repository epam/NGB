/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2021 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.epam.ngb.cli.manager.command.handler.http;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.BlastDatabase;
import com.epam.ngb.cli.entity.BlastDatabaseVO;
import com.epam.ngb.cli.entity.ResponseResult;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.printer.AbstractResultPrinter;
import com.epam.ngb.cli.manager.request.RequestManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;
import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_DATABASE_TYPE;
import static org.apache.commons.lang3.StringUtils.join;

/**
 *{@code {@link BlastDatabasesListHandler }} represents a tool for handling 'list_blast_db' command and
 * listing Blast databases, registered on NGB server.
 */
@Command(type = Command.Type.REQUEST, command = {"list_blast_db"})
@Slf4j
public class BlastDatabasesListHandler extends AbstractHTTPCommandHandler {

    public static final String TYPE_PARAM = "type=%s";
    public static final String PATH_PARAM = "path=%s";
    /**
     * If true command will output registered databases in a table format, otherwise
     * json format will be used
     */
    private boolean printTable;

    private String type;
    private String path;

    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() > 0) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ILLEGAL_COMMAND_ARGUMENTS, getCommand(), 0, arguments.size()));
        }
        this.type = options.getDatabaseType();
        if (type != null && !BlastDatabaseVO.BLAST_DATABASE_TYPES.contains(type)) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ILLEGAL_DATABASE_TYPE, type));
        }
        this.path = options.getDatabasePath();
        this.printTable = options.isPrintTable();
    }

    @Override
    public int runCommand() {
        List<String> options = new ArrayList<>();
        if (type != null) {
            options.add(String.format(TYPE_PARAM, type));
        }
        if (path != null) {
            options.add(String.format(PATH_PARAM, path));
        }
        HttpRequestBase request = getRequest(getRequestUrl()
                + (options.isEmpty() ? "" : "?") + join(options, "&"));
        String result = RequestManager.executeRequest(request);
        ResponseResult<List<BlastDatabase>> responseResult;
        try {
            responseResult = getMapper().readValue(result,
                getMapper().getTypeFactory().constructParametrizedType(
                    ResponseResult.class, ResponseResult.class,
                    getMapper().getTypeFactory().constructParametrizedType(
                        List.class, List.class, BlastDatabase.class)));
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
        if (!SUCCESS_STATUS.equals(responseResult.getStatus())) {
            throw new ApplicationException(responseResult.getMessage());
        }
        if (responseResult.getPayload() == null ||
            responseResult.getPayload().isEmpty()) {
            log.info("No databases registered on the server.");
        } else {
            List<BlastDatabase> items = responseResult.getPayload();
            AbstractResultPrinter printer = AbstractResultPrinter
                .getPrinter(printTable, items.get(0).getFormatString(items));
            printer.printHeader(items.get(0));
            items.forEach(printer::printItem);
        }
        return 0;
    }
}
