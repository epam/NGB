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

package com.epam.catgenome.controller.vcf;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.controller.vo.Query2TrackConverter.convertToTrack;
import static java.util.Collections.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Callable;

import com.epam.catgenome.manager.vcf.VcfSecurityService;
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
import com.epam.catgenome.controller.vo.VcfTrackQuery;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.entity.vcf.VariationQuery;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.exception.FeatureFileReadingException;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.exception.VcfReadingException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;


/**
 * Source:      VcfController.java
 * Created:     10/23/15, 2:38 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code VcfController} represents implementation of MVC controller which handles
 * requests to manage data about VCF tracks.
 * <p>
 * It's designed to communicate with corresponded managers that provide all required
 * calls and manage all operations concerned with a VCF.
 */
@Controller
@Api(value = "VCF", description = "VCF Track Management")
public class VcfController extends AbstractRESTController {

    @Autowired
    private VcfSecurityService vcfSecurityService;

    @ResponseBody
    @RequestMapping(value = "/vcf/register", method = RequestMethod.POST)
    @ApiOperation(
            value = "Registers a VCF file in the system.",
            notes = "Registers a file, stored in a file system (for now). Registration request has the following " +
                    "properties: <br/>" +
                    "1) referenceId - a reference, for which file is being registered <br/>" +
                    "2) path - a path to file </br>" +
                    "3) indexPath - <i>optional</i> a path to an index file<br/>" +
                    "4) name - <i>optional</i> a name for VCF track",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<VcfFile> registerVcfFile(@RequestBody
                                               FeatureIndexedFileRegistrationRequest request) {
        return Result.success(vcfSecurityService.registerVcfFile(request));
    }

    @ResponseBody
    @RequestMapping(value = "/vcf/{vcfFileId}/index", method = RequestMethod.GET)
    @ApiOperation(value = "Rebuilds a VCF feature index",
        notes = "Rebuilds a VCF feature index", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Boolean> reindexVcf(@PathVariable long vcfFileId,
            @RequestParam(defaultValue = "false") boolean createTabixIndex)
            throws FeatureIndexException {
        VcfFile file = vcfSecurityService.reindexVcfFile(vcfFileId, createTabixIndex);
        return Result.success(true, getMessage(MessagesConstants.INFO_FEATURE_INDEX_DONE, file.getId(),
                                               file.getName()));
    }

    @ResponseBody
    @RequestMapping(value = "/secure/vcf/register", method = RequestMethod.DELETE)
    @ApiOperation(value = "Unregisters a vcf file in the system.",
            notes = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Boolean> unregisterVcfFile(@RequestParam final long vcfFileId) throws IOException {
        VcfFile deletedFile = vcfSecurityService.unregisterVcfFile(vcfFileId);
        return Result.success(true, getMessage(MessagesConstants.INFO_UNREGISTER, deletedFile.getName()));
    }

    @ResponseBody
    @RequestMapping(value = "/vcf/track/get", method = RequestMethod.POST)
    @ApiOperation(
            value = "Returns data matched the given query to fill in a VCF track.",
            notes = "It provides data for a VCF track with the given scale factor between the beginning " +
                    "position with the first base having position 1 and ending position inclusive in a target " +
                    "chromosome. All parameters are mandatory and described below:<br/><br/>" +
                    "1) <b>id</b> specifies ID of a track;<br/>" +
                    "2) <b>chromosomeId</b> specifies ID of a chromosome corresponded to a track;<br/>" +
                    "3) <b>startIndex</b> is the most left base position for a requested window. The first base in a " +
                    "chromosome always has got position 1;<br/>" +
                    "4) <b>endIndex</b> is the last base position for a requested window. " +
                    "It is treated inclusively;<br/>" +
                    "5) <b>scaleFactor</b> specifies an inverse value to number of bases per one visible element on a" +
                    " track (e.g., pixel)." +
                    "6) <b>sampleId</b> optional sample id to load track for a specific sample. " +
                    "If is absent, the first sample track will be returned",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Callable<Result<Track<Variation>>> loadTrack(@RequestBody final VcfTrackQuery trackQuery,
                                              @RequestParam(required = false) final String fileUrl,
                                              @RequestParam(required = false) final String indexUrl)
        throws VcfReadingException {
        return () -> {
            final Track<Variation> variationTrack = convertToTrack(trackQuery);
            final boolean collapsed = trackQuery.getCollapsed() == null || trackQuery.getCollapsed();

            if (fileUrl == null) {
                return Result.success(vcfSecurityService
                        .loadVariations(variationTrack, trackQuery.getSampleId(), false, collapsed));
            } else {
                return Result.success(vcfSecurityService.loadVariations(variationTrack, fileUrl, indexUrl,
                        trackQuery.getSampleId() != null ?
                                trackQuery.getSampleId().intValue() :
                                null, false, collapsed));
            }
        };
    }

    @ResponseBody
    @RequestMapping(value = "/vcf/variation/load", method = RequestMethod.POST)
    @ApiOperation(
            value = "Returns extended data for a variation",
            notes = "Provides extended data about the particular variation: </br>" +
                    "info field : Additional information that is presented in INFO column</br>" +
                    "genotypeInfo field : Genotype information for a specific sample</br>",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Variation> loadVariation(@RequestBody final VariationQuery query,
                                           @RequestParam(required = false) final String fileUrl,
                                           @RequestParam(required = false) final String indexUrl)
        throws FeatureFileReadingException {
        if (fileUrl == null) {
            return Result.success(vcfSecurityService.loadVariation(query));
        } else {
            return Result.success(vcfSecurityService.loadVariation(query, fileUrl, indexUrl));
        }
    }

    @RequestMapping (value = "/vcf/{chromosomeId}/next", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
            value = "Returns the next feature for a given track",
            notes = "Returns the next feature for a given track in a given chromosome. </br>" +
                    "Searches from given parameter 'fromPosition' (required), from a given sample (parameter " +
                    "'sampleId', optional)",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Variation> jumpToNextGene(@RequestParam int fromPosition,
                                        @PathVariable(value = "chromosomeId") long chromosomeId,
                                        @RequestParam(required = false) Long trackId,
                                        @RequestParam(required = false) Long sampleId,
                                        @RequestParam(required = false) final String fileUrl,
                                        @RequestParam(required = false) final String indexUrl,
                                        @RequestParam(required = false) final  Long projectId)
            throws VcfReadingException {
        return Result.success(vcfSecurityService.getNextOrPreviousVariation(fromPosition, trackId, sampleId,
                                                                            chromosomeId, true, fileUrl,
                                                                            indexUrl, projectId));
    }

    @RequestMapping (value = "/vcf/{chromosomeId}/prev", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
            value = "Returns the previous feature for a given track",
            notes = "Returns the previous feature for a given track in a given chromosome. </br>" +
                    "Searches from given parameter 'fromPosition' (required), from a given sample (parameter " +
                    "'sampleId', optional)",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Variation> jumpToPrevGene(@RequestParam int fromPosition,
                                        @PathVariable(value = "chromosomeId") long chromosomeId,
                                        @RequestParam(required = false) Long trackId,
                                        @RequestParam(required = false) Long sampleId,
                                        @RequestParam(required = false) final String fileUrl,
                                        @RequestParam(required = false) final String indexUrl,
                                        @RequestParam(required = false) final  Long projectId)
            throws VcfReadingException {
        return Result.success(vcfSecurityService.getNextOrPreviousVariation(fromPosition, trackId, sampleId,
                chromosomeId, false, fileUrl, indexUrl, projectId));
    }

    @ResponseBody
    @RequestMapping(value = "/vcf/{vcfFileId}/fieldInfo", method = RequestMethod.GET)
    @ApiOperation(
            value = "Returns information about VCF filter by file ID.",
            notes = "Returns information about VCF filter by file ID, all information taken from file header.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<VcfFilterInfo> erg(@PathVariable(value = "vcfFileId") final Long vcfFileId,
                                     @RequestParam(required = false) final  Long projectId) throws IOException {
        return Result.success(vcfSecurityService.getFiltersInfo(
                //here we need to create new HashMap to be able to filter this map in SecurityServices classes
                new HashMap<>(singletonMap(projectId, singletonList(vcfFileId)))));
    }
}
