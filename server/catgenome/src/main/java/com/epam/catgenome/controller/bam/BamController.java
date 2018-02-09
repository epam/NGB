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

package com.epam.catgenome.controller.bam;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.controller.vo.Query2TrackConverter.convertToTrack;

import java.io.IOException;
import java.util.List;

import com.epam.catgenome.controller.vo.ReadSequenceVO;
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.entity.bam.PSLRecord;
import com.epam.catgenome.entity.bam.Read;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import com.epam.catgenome.controller.vo.ReadQuery;
import com.epam.catgenome.controller.vo.TrackQuery;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.manager.bam.BamFileManager;
import com.epam.catgenome.manager.bam.BamManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

/**
 * Source:      BamController.java
 * Created:     12/21/2015
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code BamController} represents implementation of MVC controller which handles
 * requests to manage BAM tracks.
 * <p>
 * It's designed to communicate with corresponding managers that provide all required
 * calls and manage all operations concerned with BAM data.
 *
 */
@Controller
@Api(value = "BAM", description = "BAM Track Management")
public class BamController extends AbstractRESTController {

    private static final String NOTES = "It provides data for a BAM track with the given scale factor between the " +
            "beginning position with the first base having position 1 and ending position "
            +
            "inclusive in a target " +
            "chromosome. All parameters are mandatory and described below:<br/><br/>" +
            "query:<br/>" +
            "1) <b>id</b> specifies ID of a track;<br/>" +
            "2) <b>chromosomeId</b> specifies ID of a chromosome corresponded to a track;<br/>"
            +
            "3) <b>startIndex</b> is the most left base position for a requested window. The first base in a "
            +
            "chromosome always has got position 1;<br/>" +
            "4) <b>endIndex</b> is the last base position for a requested window. " +
            "It is treated inclusively;<br/>" +
            "5) <b>scaleFactor</b> specifies an inverse value to number of bases per one visible element on a"
            +
            " track (e.g., pixel).";
    public static final long EMITTER_TIMEOUT = 100000L;

    @Autowired
    private BamFileManager bamFileManager;

    @Autowired
    private BamManager bamManager;

    @ResponseBody
    @RequestMapping(value = "/bam/{referenceId}/loadAll", method = RequestMethod.GET)
    @ApiOperation(
            value = "Returns metadata for all BAM files filtered by a reference genome.",
            notes = "Each summary in the list provides metadata per a single BAM file that is available in the system.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<BamFile>> loadBamFiles(@PathVariable(value = "referenceId") final Long referenceId) {
        return Result.success(bamFileManager.loadBamFilesByReferenceId(referenceId));
    }

    @ResponseBody
    @RequestMapping(value = "/bam/register", method = RequestMethod.POST)
    @ApiOperation(
            value = "Registers a BAM file in the system.",
            notes = "Registers a file, stored in a file system (for now). Registration request has the following " +
                    "properties: <br/>" +
                    "1) referenceId - a reference, for which file is being registered <br/>" +
                    "2) path - a path to file </br>" +
                    "3) type - resource type of file: FILE / URL / S3<br/>" +
                    "4) indexPath - <i>optional</i> a path to an index file (.bai)<br/>" +
                    "5) name - <i>optional</i> a name for BAM track<br/>" +
                    "6) s3BucketId - <i>optional</i> necessarily for cases when type is S3",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<BamFile> registerBamFile(@RequestBody IndexedFileRegistrationRequest request) throws IOException {
        return Result.success(bamManager.registerBam(request));
    }

    @RequestMapping(value = "/bam/track/get", method = RequestMethod.POST)
    @ApiOperation(
            value = "Returns data (chunked) matching the given query to fill in a bam track. Returns all information " +
                    "about reads.",
            notes = NOTES +
                    "<br/>option:<br/>" +
                    "all the following params are <b>optional</b>, if any of the params is incorrect, " +
                    "it will be set to default value:<br/><br/>" +
                    "1) <b>trackDirection</b> - can be 'LEFT', 'MIDDLE' or 'RIGHT', responsible for direction <br/>" +
                    "default - MIDDLE" +
                    "2) <b>showClipping</b> - if true handles the track with soft clipping, default is false;<br/>" +
                    "3) <b>showSpliceJunction</b> - return a information about splice junction, " +
                    "default is false;<br/>" +
                    "4) <b>frame</b> - size of frame for downsampling,  default is null;<br/>" +
                    "5) <b>count</b> - count of read in frame, default is null ;<br/>" +
                    "if frame or count default or incorrect, return track without downsampling<br/>" +
                    "6) <b>mode</b> controls BAM display mode: REGIONS - return only regions of possible read " +
                    "location; <br/>" +
                    "COVERAGE - return only BAM coverage;<br/>" +
                    "FULL - return both reads and coverage",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public final ResponseEntity<ResponseBodyEmitter> loadTrackStream(
            @RequestBody final TrackQuery query,
            @RequestParam(required = false) final String fileUrl,
            @RequestParam(required = false) final String indexUrl)
            throws IOException {

        final ResponseBodyEmitter emitter = new ResponseBodyEmitter(EMITTER_TIMEOUT);
        if (fileUrl == null) {
            bamManager.sendBamTrackToEmitter(convertToTrack(query), query.getOption(), emitter);
        } else {
            bamManager.sendBamTrackToEmitterFromUrl(convertToTrack(query), query.getOption(), fileUrl,
                    indexUrl, emitter);
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        return new ResponseEntity<>(emitter, responseHeaders, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/secure/bam/register", method = RequestMethod.DELETE)
    public Result<Boolean> unregisterBamFile(@RequestParam final long bamFileId) throws IOException {
        BamFile deletedFile = bamManager.unregisterBamFile(bamFileId);
        return Result.success(true, getMessage(MessagesConstants.INFO_UNREGISTER, deletedFile.getName()));
    }

    @ResponseBody
    @RequestMapping(value = "/bam/consensus/get", method = RequestMethod.POST)
    @ApiOperation(
            value = "Returns consensus sequence for specified BAM file range.",
            notes = "It provides data about consensus sequence for specified BAM file range " +
                    "with the given scale factor between the " +
                    "beginning position with the first base having position 1 and ending position inclusive in a " +
                    "target chromosome. All parameters are mandatory and described below:<br/><br/>" +
                    "1) <b>id</b> specifies ID of a track;<br/>" +
                    "2) <b>chromosomeId</b> specifies ID of a chromosome corresponded to a track;<br/>" +
                    "3) <b>startIndex</b> is the most left base position for a requested window. The first base " +
                    "in a chromosome always has got position 1;<br/>" +
                    "4) <b>endIndex</b> is the last base position for a requested window. It is treated " +
                    "inclusively;<br/>" +
                    "5) <b>scaleFactor</b> specifies an inverse value to number of bases per one visible " +
                    "element on a track (e.g., pixel).",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Track<Sequence> loadConsensusSequence(@RequestBody final TrackQuery query) throws IOException {
        return bamManager.calculateConsensusSequence(convertToTrack(query));
    }

    @ResponseBody
    @RequestMapping(value = "/bam/read/load", method = RequestMethod.POST)
    @ApiOperation(
        value = "Returns extended data for a read",
        notes = "Provides extended data about the particular read",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<Read> loadRead(@RequestBody final ReadQuery query,
                                 @RequestParam(required = false) final String fileUrl,
                                 @RequestParam(required = false) final String indexUrl) throws IOException {
        return Result.success(bamManager.loadRead(query, fileUrl, indexUrl));
    }

    @ResponseBody
    @RequestMapping(value = "/bam/read/blat", method = RequestMethod.POST)
    @ApiOperation(
            value = "Returns statistics generated by BLAT search.",
            notes = "Provides statistics generated by BLAT search performed on a read sequence.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<PSLRecord>> blatReadSequence(@RequestParam final Long bamTrackId,
                                                    @RequestBody final ReadSequenceVO readSequence)
            throws IOException, ExternalDbUnavailableException {
        return Result.success(bamManager.findBlatReadSequence(bamTrackId, readSequence.getReadSequence()));
    }
}
