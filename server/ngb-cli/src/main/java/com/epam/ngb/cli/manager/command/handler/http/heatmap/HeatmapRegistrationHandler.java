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

package com.epam.ngb.cli.manager.command.handler.http.heatmap;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.heatmap.Heatmap;
import com.epam.ngb.cli.entity.heatmap.HeatmapRegistrationRequest;
import com.epam.ngb.cli.entity.ResponseResult;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.command.handler.http.AbstractHTTPCommandHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;

import java.io.IOException;
import java.util.List;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

@Slf4j
@Command(type = Command.Type.REQUEST, command = {"reg_heatmap"})
public class HeatmapRegistrationHandler extends AbstractHTTPCommandHandler {

    private HeatmapRegistrationRequest registrationRequest;

    /**
      * Verifies input arguments
      * @param arguments command line arguments for 'reg_heatmap' command
      * @param options
      */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ILLEGAL_COMMAND_ARGUMENTS, getCommand(), 1, arguments.size()));
        }
        registrationRequest = HeatmapRegistrationRequest.builder()
                .path(arguments.get(0))
                .build();
        registrationRequest.setName(options.getName());
        registrationRequest.setPrettyName(options.getPrettyName());
        registrationRequest.setCellAnnotationPath(options.getHeatmapCellAnnotationPath());
        registrationRequest.setLabelAnnotationPath(options.getHeatmapLabelAnnotationPath());
        registrationRequest.setCellAnnotationType(options.getHeatmapCellAnnotationType());
        registrationRequest.setRowAnnotationType(options.getHeatmapRowAnnotationType());
        registrationRequest.setColumnAnnotationType(options.getHeatmapColumnAnnotationType());
        registrationRequest.setSkipRows(options.getHeatmapSkipRows());
        registrationRequest.setSkipColumns(options.getHeatmapSkipColumns());
    }

    @Override public int runCommand() {
        HttpPost request = (HttpPost) getRequest(getRequestUrl());
        String result = getPostResult(registrationRequest, request);
        try {
            ResponseResult<Heatmap> responseResult = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                            Heatmap.class));
            if (!SUCCESS_STATUS.equals(responseResult.getStatus())) {
                throw new ApplicationException(responseResult.getMessage());
            }
            log.info("Heatmap was successfully registered. ID: " + responseResult.getPayload().getHeatmapId() + ".");
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
        return 0;
    }
}
