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

package com.epam.ngb.cli.manager.command.handler.http.heatmap;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.ResponseResult;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.command.handler.http.AbstractHTTPCommandHandler;
import com.epam.ngb.cli.manager.request.RequestManager;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

@Command(type = Command.Type.REQUEST, command = {"del_heatmap"})
@Slf4j
public class HeatmapDeletionHandler extends AbstractHTTPCommandHandler {

    /**
     * ID of heatmap to delete
     */
    private Long heatmapId;

    /**
     * Verifies that input arguments contain the required parameter:
     * the first and the only one argument must be heatmap ID or name.
     * If heatmap's name is provided, it's ID will be loaded from the NGB server.
     * @param arguments command line arguments for 'del_heatmap' command
     * @param options aren't used in this command
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException(MessageConstants.getMessage(ILLEGAL_COMMAND_ARGUMENTS,
                    getCommand(), 1, arguments.size()));
        }
        heatmapId = loadItemId(arguments.get(0));
    }
    /**

     * Performs a heatmap deletion request to NGB server
     * @return 0 if request completed successfully
     */
    @Override public int runCommand() {
        try {
            String result = RequestManager.executeRequest(getRequest(String.format(getRequestUrl(), heatmapId)));
            ResponseResult responseResult = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructType(ResponseResult.class));
            if (!SUCCESS_STATUS.equals(responseResult.getStatus())) {
                throw new ApplicationException(responseResult.getMessage());
            }
            log.info("Heatmap " + heatmapId + " was successfully deleted.");
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
        return 0;
    }
}
