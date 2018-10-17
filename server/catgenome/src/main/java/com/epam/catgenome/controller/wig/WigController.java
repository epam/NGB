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

package com.epam.catgenome.controller.wig;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.controller.vo.Query2TrackConverter.convertToTrack;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import com.epam.catgenome.manager.wig.WigSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.TrackQuery;
import com.epam.catgenome.controller.vo.registration.FileRegistrationRequest;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.entity.wig.WigFile;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * <p>
 * {@code WigController} represents implementation of MVC controller which handles
 * requests to manage data about Wiggle Track Format(WIG) tracks.
 * <p>
 * It's designed to communicate with corresponded managers that provide all required
 * calls and manage all operations concerned with a WIG file.
 */
@Controller
@Api(value = "bed-graph", description = "Wig Track Management")
public class WigController extends AbstractRESTController {

    @Autowired
    private WigSecurityService wigSecurityService;

    @RequestMapping(value = "/wig/{referenceId}/loadAll", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
            value = "Returns metadata for all gene files filtered by a reference genome.",
            notes = "Each summary in the list provides metadata per a single gene file that is available in the " +
                    "system.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<WigFile>> loadWigFiles(@PathVariable(value = "referenceId") final Long referenceId) {
        return Result.success(wigSecurityService.loadWigFilesByReferenceId(referenceId));
    }

    @ResponseBody
    @RequestMapping(value = "/wig/register", method = RequestMethod.POST)
    @ApiOperation(
            value = "Registers a Wig file in the system.",
            notes = "Registers a file, stored in a file system (for now). Registration request has the following " +
                    "properties: <br/>" +
                    "1) referenceId - a reference, for which file is being registered <br/>" +
                    "2) path - a path to file </br>" +
                    "3) name - <i>optional</i> a name for WIG track",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<WigFile> registerWigFile(@RequestBody FileRegistrationRequest request) {
        return Result.success(wigSecurityService.registerWigFile(request));
    }


    @ResponseBody
    @RequestMapping(value = "/wig/track/get", method = RequestMethod.POST)
    @ApiOperation(
            value = "Returns data matched the given query to fill in a wig track.",
            notes = "It provides data for a WIG track with the given scale factor between the beginning " +
                    "position with the first base having position 1 and ending position inclusive in a target " +
                    "chromosome. All parameters are mandatory and described below:<br/><br/>" +
                    "1) <b>id</b> specifies ID of a track;<br/>" +
                    "2) <b>chromosomeId</b> specifies ID of a chromosome corresponded to a track;<br/>" +
                    "3) <b>startIndex</b> is the most left base position for a requested window. The first base in a " +
                    "chromosome always has got position 1;<br/>" +
                    "4) <b>endIndex</b> is the last base position for a requested window. " +
                    "It is treated inclusively;<br/>" +
                    "5) <b>scaleFactor</b> specifies an inverse value to number of bases per one visible element on a" +
                    " track (e.g., pixel).",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public final Callable<Result<Track<Wig>>> loadTrack(@RequestBody final TrackQuery query) throws IOException {
        final Track<Wig> track = wigSecurityService.getWigTrack(convertToTrack(query));
        return () ->  Result.success(track);
    }


    @ResponseBody
    @RequestMapping(value = "/secure/wig/register", method = RequestMethod.DELETE)
    public Result<Boolean> unregisterWigFile(@RequestParam final long wigFileId) throws IOException {
        WigFile deletedFile = wigSecurityService.unregisterWigFile(wigFileId);
        return Result.success(true, getMessage(MessagesConstants.INFO_UNREGISTER, deletedFile.getName()));
    }
}
