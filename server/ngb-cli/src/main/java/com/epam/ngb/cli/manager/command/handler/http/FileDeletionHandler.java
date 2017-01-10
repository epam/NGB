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

import java.util.List;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.manager.command.handler.Command;

/**
 * {@code {@link FileDeletionHandler}} represents a tool for handling 'delete_file' command and
 * deleting files from NGB server. This command requires strictly one argument:
 * file ID or name.
 */
@Command(type = Command.Type.REQUEST, command = {"delete_file"})
public class FileDeletionHandler extends AbstractHTTPCommandHandler {

    /**
     * ID of a file to delete
     */
    private Long fileId;

    /**
     * Verifies that input arguments contain the required parameter:
     * the first and the only one argument must be file ID or name.
     * If file's name is provided, it's ID will be loaded from the NGB server.
     * @param arguments command line arguments for 'delete_file' command
     * @param options aren't used in this command
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.isEmpty() || arguments.size() != 1) {
            throw new IllegalArgumentException(MessageConstants.getMessage(ILLEGAL_COMMAND_ARGUMENTS,
                    getCommand(), 1, arguments.size()));
        }
        fileId = parseFileId(arguments.get(0));
    }

    /**
     * Performs a file deletion request to NGB server
     * @return 0 if request completed successfully
     */
    @Override public int runCommand() {
        runDeletion(fileId);
        return 0;
    }
}
