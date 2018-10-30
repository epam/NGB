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

package com.epam.catgenome.controller.maf;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.controller.vo.Query2TrackConverter.convertToTrack;

import java.io.IOException;

import com.epam.catgenome.manager.maf.MafSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.TrackQuery;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.entity.maf.MafFile;
import com.epam.catgenome.entity.maf.MafRecord;
import com.epam.catgenome.entity.track.Track;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/** *
 * {@code MafController} represents an implementation of MVC controller which handles
 * requests to manage MAF (Mutation Annotation Format) tracks.
 * <p>
 * It's designed to communicate with corresponding managers that provide all required
 * calls and manage all operations concerned with MAF data.
 */
@Controller
public class MafController extends AbstractRESTController {

    @Autowired
    private MafSecurityService mafSecurityService;

    @ResponseBody
    @RequestMapping(value = "/maf/register", method = RequestMethod.POST)
    @ApiOperation(
            value = "Registers a MAF file in the system.",
            notes = "Registers a file, stored in a file system (for now). Can register a single file or a directory, " +
                    "containing multiple MAF files. In this case they will be merged to a single BigMaf file.<br/>" +
                    "Registration request has the following properties: <br/>" +
                    "1) referenceId - a reference, for which file is being registered <br/>" +
                    "2) path - a path to a file or directory</br>" +
                    "3) indexPath - <i>optional</i> a path to an index file<br/>" +
                    "4) name - <i>optional</i> a name for gene track",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<MafFile> registerMafFile(@RequestBody IndexedFileRegistrationRequest request) {
        return Result.success(mafSecurityService.registerMafFile(request));
    }

    @ResponseBody
    @RequestMapping(value = "/secure/maf/register", method = RequestMethod.DELETE)
    @ApiOperation(value = "Unregisters a MAF file from the system.", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Boolean> unregisterSegFile(@RequestParam final long mafFileId) throws IOException {
        MafFile deletedFile = mafSecurityService.unregisterMafFile(mafFileId);
        return Result.success(true, getMessage(MessagesConstants.INFO_UNREGISTER, deletedFile.getName()));
    }

    @ResponseBody
    @RequestMapping(value = "/maf/track/get", method = RequestMethod.POST)
    @ApiOperation(
            value = "Returns data matched the given query to fill in a MAF track.",
            notes = "It provides data for a MAF track with the given scale factor between the beginning " +
                    "position with the first base having position 1 and ending position inclusive in a target " +
                    "chromosome. All parameters are mandatory and described below:<br/><br/>" +
                    "1) <b>id</b> specifies ID of a track;<br/>" +
                    "2) <b>chromosomeId</b> specifies ID of a chromosome corresponded to a track;<br/>" +
                    "3) <b>startIndex</b> is the most left base position for a requested window. The first base in a " +
                    "chromosome always has got position  = 1;<br/>" +
                    "4) <b>endIndex</b> is the last base position for a requested window. <br/>" +
                    "5) <b>scaleFactor</b> specifies an inverse value to number of bases per one visible element on a" +
                    " track (e.g., pixel) - IS IGNORED FOR NOW",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Track<MafRecord>> loadTrack(@RequestBody final TrackQuery trackQuery)
            throws IOException {
        final Track<MafRecord> track = convertToTrack(trackQuery);
        return Result.success(mafSecurityService.loadFeatures(track));
    }
}
