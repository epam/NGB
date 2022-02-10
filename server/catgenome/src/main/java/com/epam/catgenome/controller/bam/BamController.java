/*
 * MIT License
 *
 * Copyright (c) 2017-2022 EPAM Systems
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

import com.epam.catgenome.entity.bam.BamCoverage;
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.entity.bam.CoverageInterval;
import com.epam.catgenome.entity.bam.CoverageQueryParams;
import com.epam.catgenome.entity.bam.Read;
import com.epam.catgenome.manager.bam.BamSecurityService;
import com.epam.catgenome.util.db.Page;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.ReadQuery;
import com.epam.catgenome.controller.vo.TrackQuery;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.track.Track;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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
    private BamSecurityService bamSecurityService;

    @ResponseBody
    @PostMapping(value = "/bam/register")
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
        return Result.success(bamSecurityService.registerBam(request));
    }

    @PostMapping(value = "/bam/track/get")
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
            bamSecurityService.sendBamTrackToEmitter(convertToTrack(query), query.getOption(), emitter);
        } else {
            bamSecurityService.sendBamTrackToEmitterFromUrl(convertToTrack(query), query.getOption(), fileUrl,
                    indexUrl, emitter);
        }
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        return new ResponseEntity<>(emitter, responseHeaders, HttpStatus.OK);
    }

    @ResponseBody
    @DeleteMapping(value = "/secure/bam/register")
    public Result<Boolean> unregisterBamFile(@RequestParam final long bamFileId) throws IOException {
        BamFile deletedFile = bamSecurityService.unregisterBamFile(bamFileId);
        return Result.success(true, getMessage(MessagesConstants.INFO_UNREGISTER, deletedFile.getName()));
    }

    @ResponseBody
    @PostMapping(value = "/bam/consensus/get")
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
        return bamSecurityService.calculateConsensusSequence(convertToTrack(query));
    }

    @ResponseBody
    @PostMapping(value = "/bam/read/load")
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
        return Result.success(bamSecurityService.loadRead(query, fileUrl, indexUrl));
    }

    @ResponseBody
    @PostMapping(value = "/bam/coverage")
    @ApiOperation(
        value = "Creates coverage with given step for the BAM file",
        notes = "Creates coverage with given step for the BAM file",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<Boolean> createCoverage(@RequestBody final BamCoverage coverage) throws IOException {
        bamSecurityService.createCoverage(coverage);
        return Result.success(true);
    }

    @ResponseBody
    @DeleteMapping(value = "/bam/coverage")
    @ApiOperation(
        value = "Deletes BAM coverage by Id",
        notes = "Deletes BAM coverage by Id",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<Boolean> deleteCoverage(@RequestParam final long bamId,
                                          @RequestParam(required = false) final Integer step)
            throws IOException, InterruptedException, ParseException {
        bamSecurityService.deleteCoverage(bamId, step);
        return Result.success(true);
    }

    @ResponseBody
    @PostMapping(value = "/bam/coverage/search")
    @ApiOperation(
        value = "Returns coverage intervals for a BAM file",
        notes = "Returns coverage intervals for a BAM file",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<Page<CoverageInterval>> loadCoverage(@RequestBody final CoverageQueryParams params)
            throws ParseException, IOException {
        return Result.success(bamSecurityService.loadCoverage(params));
    }

    @ResponseBody
    @GetMapping(value = "/bam/coverage/all")
    @ApiOperation(
        value = "Returns all registered coverages",
        notes = "Returns all registered coverages",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<List<BamCoverage>> loadCoverage() throws IOException {
        return Result.success(bamSecurityService.loadAll());
    }
}
