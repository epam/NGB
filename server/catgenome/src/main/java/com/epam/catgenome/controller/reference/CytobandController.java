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

package com.epam.catgenome.controller.reference;

import static com.epam.catgenome.component.MessageHelper.getMessage;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.entity.reference.cytoband.Cytoband;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.manager.reference.CytobandManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * {@code CytobandController} represents implementation of MVC controller which handles
 * requests to manage data about cytobands pointing to reference genomes.
 * <p>
 * It's designed to communicate with corresponded managers that provide all required
 * calls and manage all operations concerned with cytobands.
 */
@Controller
@Api(value = "cytobands", description = "Cytobands Management")
public class CytobandController extends AbstractRESTController {

    @Autowired
    private CytobandManager cytobandManager;

    @ResponseBody
    @RequestMapping(value = "/cytobands/upload", method = RequestMethod.POST,
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(
        value = "Handles cytobands upload pointing to a corresponded genome that has to be available in the system.",
        notes = "The following cytobands file types are supported: *.txt and *.txt.gz.<br/>" +
            "It results in a payload that provides corresponded genome ID, also a message about succeeded upload " +
            "is sent back.",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public final Result<Long> saveCytobands(@RequestParam("referenceId") final Long referenceId,
                                            @RequestParam("saveFile") final MultipartFile multipart)
        throws IOException {
        final File tmpFile = transferToTempFile(multipart);
        cytobandManager.saveCytobands(referenceId, tmpFile);
        return Result.success(referenceId, getMessage("info.cytobands.upload.done",
            multipart.getOriginalFilename()));
    }

    @ResponseBody
    @RequestMapping(value = "/cytobands/{chromosomeId}/get", method = RequestMethod.GET)
    @ApiOperation(
        value = "Returns data to fill in a cytogenetic ideogram for a particular chromosome.",
        notes = "It provides summary of cytobands for a particular chromosome. In a case when no cytobands can be " +
            "found in the system, this call results in a response with WARN status.",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = "It results in a response with HTTP status OK, but " +
            "you should always check $.status, which can take several values:<br/>" +
            "<b>OK</b> means call was done without any problems;<br/>" +
            "<b>ERROR</b> means call was aborted due to errors;<br/>" +
            "<b>WARN</b> means call was done without any problems, but there is no an ideogram that is available for " +
            "a particular chromosome.<br/>" +
            "In both cases - ERROR or WARN - see $.message for additional information.")
        })
    public final Result<Track<Cytoband>> loadTrack(@PathVariable final Long chromosomeId) throws IOException {
        final Track<Cytoband> track = cytobandManager.loadCytobands(chromosomeId);
        if (track == null) {
            return Result.warn(getMessage("info.no.cytobands.found"), null);
        }
        return Result.success(track);
    }

}
