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

package com.epam.ngb.cli.manager.command.handler.http.blast;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.BlastDatabaseVO;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.command.handler.http.AbstractHTTPCommandHandler;
import org.apache.http.client.methods.HttpPost;

import java.util.List;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;
import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_DATABASE_SOURCE;
import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_DATABASE_TYPE;

@Command(type = Command.Type.REQUEST, command = {"reg_blast_db"})
public class BlastDatabaseRegistrationHandler extends AbstractHTTPCommandHandler {

    private BlastDatabaseVO database;

    /**
      * Verifies input arguments
      * @param arguments command line arguments for 'reg_blast_db' command
      * @param options
      */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 4) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ILLEGAL_COMMAND_ARGUMENTS, getCommand(), 3, arguments.size()));
        }
        String type = arguments.get(2);
        String source = arguments.get(3);
        if (!BlastDatabaseVO.BLAST_DATABASE_TYPES.contains(type)) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ILLEGAL_DATABASE_TYPE, type));
        }
        if (!BlastDatabaseVO.BLAST_DATABASE_SOURCES.contains(source)) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ILLEGAL_DATABASE_SOURCE, source));
        }
        database = new BlastDatabaseVO(arguments.get(0), arguments.get(1), type, source);
    }

    @Override public int runCommand() {
        HttpPost request = (HttpPost) getRequest(getRequestUrl());
        getResult(getPostResult(database, request), Boolean.class);
        return 0;
    }
}
