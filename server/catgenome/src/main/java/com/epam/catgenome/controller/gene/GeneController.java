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

package com.epam.catgenome.controller.gene;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.epam.catgenome.entity.protein.ProteinSequence;
import com.epam.catgenome.entity.protein.ProteinSequenceConstructRequest;
import com.epam.catgenome.manager.gene.GeneSecurityService;
import com.epam.catgenome.manager.protein.ProteinSequenceSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.Constants;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.ExonRangeQuery;
import com.epam.catgenome.controller.vo.ExonViewPortQuery;
import com.epam.catgenome.controller.vo.Query2TrackConverter;
import com.epam.catgenome.controller.vo.TrackQuery;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.entity.externaldb.DimStructure;
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.gene.GeneHighLevel;
import com.epam.catgenome.entity.gene.GeneTranscript;
import com.epam.catgenome.entity.protein.MrnaProteinSequenceVariants;
import com.epam.catgenome.entity.protein.ProteinSequenceEntry;
import com.epam.catgenome.entity.protein.ProteinSequenceInfo;
import com.epam.catgenome.entity.track.Block;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.exception.GeneReadingException;
import com.epam.catgenome.exception.HistogramReadingException;
import com.epam.catgenome.util.Utils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Source:      GeneController
 * Created:     05.12.15, 14:44
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * Controller to provide REST API for gene file and track management
 *
 * @author Mikhail Miroliubov
 */
@Controller
@Api(value = "genes", description = "Gene Track Management")
public class GeneController extends AbstractRESTController {

    private static final String REFERENCE_ID_FIELD = "referenceId";
    private static final String GENE_URL = "/gene/{";
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneController.class);

    @Autowired
    private GeneSecurityService geneSecurityService;

    @Autowired
    private ProteinSequenceSecurityService proteinSecurityService;

    @ResponseBody
    @RequestMapping(value = "/gene/register", method = RequestMethod.POST)
    @ApiOperation(
            value = "Registers a gene file in the system.",
            notes = "Registers a file, stored in a file system (for now). Registration request has the following " +
                    "properties: <br/>" +
                    "1) " + REFERENCE_ID_FIELD + " - a reference, for which file is being registered <br/>" +
                    "2) path - a path to file </br>" +
                    "3) indexPath - <i>optional</i> a path to an index file<br/>" +
                    "4) name - <i>optional</i> a name for gene track",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<GeneFile> registerGeneFile(@RequestBody
                                                 FeatureIndexedFileRegistrationRequest request) {
        return Result.success(geneSecurityService.registerGeneFile(request));
    }

    @ResponseBody
    @RequestMapping(value = "/secure/gene/register", method = RequestMethod.DELETE)
    @ApiOperation(value = "Unregisters a gene file in the system.",
            notes = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Boolean> unregisterGeneFile(@RequestParam final long geneFileId) throws IOException {
        GeneFile deletedFile = geneSecurityService.unregisterGeneFile(geneFileId);
        return Result.success(true, MessageHelper.getMessage(MessagesConstants.INFO_UNREGISTER, deletedFile.getName()));
    }

    @ResponseBody
    @RequestMapping(value = "/gene/{geneFileId}/index", method = RequestMethod.GET)
    @ApiOperation(value = "Rebuilds a gene file feature index",
        notes = "Rebuilds a gene file feature index.</br>" +
                "<b>full</b> parameter specifies if full original file should be reindexed, or " +
                "preprocessed large scale and transcript files should be used for indexing.</br>",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Boolean> reindexGeneFile(@PathVariable long geneFileId,
            @RequestParam(defaultValue = "false") boolean full,
            @RequestParam(defaultValue = "false") boolean createTabixIndex) throws IOException {
        GeneFile geneFile = geneSecurityService.reindexGeneFile(geneFileId, full, createTabixIndex);
        return Result.success(true, MessageHelper.getMessage(MessagesConstants.INFO_FEATURE_INDEX_DONE,
                                                             geneFile.getId(), geneFile.getName()));
    }

    @ResponseBody
    @RequestMapping(value = GENE_URL + REFERENCE_ID_FIELD + "}/track/get", method = RequestMethod.POST)
    @ApiOperation(
            value = "Returns data matched the given query to fill in a gene track.",
            notes = "It provides data for a gene track with the given scale factor between the beginning " +
                    "position with the first base having position 1 and ending position inclusive in a target " +
                    "chromosome. All parameters are mandatory and described below:<br/><br/>" +
                    "1) <b>id</b> specifies ID of a track;<br/>" +
                    "2) <b>chromosomeId</b> specifies ID of a chromosome corresponded to a track;<br/>" +
                    "3) <b>startIndex</b> is the most left base position for a requested window. The first base in a " +
                    "chromosome always has got position = 1;<br/>" +
                    "4) <b>endIndex</b> is the last base position for a requested window. <br/>" +
                    "5) <b>scaleFactor</b> specifies an inverse value to number of bases per one visible element on a" +
                    " track (e.g., pixel) - IS IGNORED FOR NOW",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Track<GeneHighLevel>> loadTrack(@RequestBody final TrackQuery trackQuery,
            @PathVariable(value = REFERENCE_ID_FIELD) final Long referenceId,
            @RequestParam(required = false) final String fileUrl,
            @RequestParam(required = false) final String indexUrl) throws GeneReadingException {
        final Track<Gene> geneTrack = Query2TrackConverter.convertToTrack(trackQuery);
        boolean collapsed = trackQuery.getCollapsed() != null && trackQuery.getCollapsed();

        Track<Gene> genes;
        if (fileUrl == null) {
            genes = geneSecurityService.loadGenes(geneTrack, collapsed);
        } else {
            genes = geneSecurityService.loadGenes(geneTrack, collapsed, fileUrl, indexUrl);
        }

        Map<Gene, List<ProteinSequenceEntry>> aminoAcids = null;
        double time1 = Utils.getSystemTimeMilliseconds();
        if (geneTrack.getScaleFactor() > Constants.AA_SHOW_FACTOR && !collapsed) {
            aminoAcids = proteinSecurityService
                    .loadProteinSequenceWithoutGrouping(genes, referenceId, collapsed);
        }
        double time2 = Utils.getSystemTimeMilliseconds();
        LOGGER.debug("Loading aminoacids took {} ms", time2 - time1);

        final Track<GeneHighLevel> result = new Track<>(geneTrack);
        time1 = Utils.getSystemTimeMilliseconds();
        result.setBlocks(geneSecurityService.convertGeneTrackForClient(genes.getBlocks(), aminoAcids));
        time2 = Utils.getSystemTimeMilliseconds();
        LOGGER.debug("Simplifying genes took {} ms", time2 - time1);

        return Result.success(result);
    }

    @ResponseBody
    @RequestMapping(value = "/gene/transcript/track/get", method = RequestMethod.POST)
    @ApiOperation(
            value = "Returns data matched the given query to fill in a gene track with transcripts.",
            notes = "It provides data for a gene track with the given scale factor between the beginning position " +
                    "with the first base having position 1 and ending position inclusive in a target chromosome. " +
                    "All parameters are mandatory and described below:<br/><br/>" +
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
    public Result<Track<GeneTranscript>> loadTrackWithTranscript(@RequestBody final TrackQuery trackQuery,
            @RequestParam(required = false) final String fileUrl,
            @RequestParam(required = false) final String indexUrl)
        throws GeneReadingException {
        return Result.success(geneSecurityService.loadGenesTranscript(Query2TrackConverter.convertToTrack(trackQuery),
                fileUrl, indexUrl));
    }

    @ResponseBody
    @RequestMapping(value = "gene/pbd/{pbdID}/get", method = RequestMethod.POST)
    @ApiOperation(
            value = "Returns a list of entity from PBD",
            notes = "param is PBD ID",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<DimStructure> getPBDentity(@PathVariable(value = "pbdID") String pbdID)
            throws ExternalDbUnavailableException {
        return Result.success(geneSecurityService.getPBDItemsFromBD(pbdID));
    }

    @ResponseBody
    @RequestMapping(value = "/gene/track/histogram", method = RequestMethod.POST)
    @ApiOperation(
            value = "Returns a histogram of genes amount on regions of chromosome",
            notes = "It provides histogram for a gene track with the given scale factor between the " +
                    "beginning position with the first base having position 1 and ending position inclusive " +
                    "in a target chromosome. All parameters are mandatory and described below:<br/><br/>" +
                    "1) <b>id</b> specifies ID of a track;<br/>" +
                    "2) <b>chromosomeId</b> specifies ID of a chromosome corresponded to a track;<br/>" +
                    "3) <b>startIndex</b> is the most left base position for a requested window. The first base in a " +
                    "chromosome always has got position 1;<br/>" +
                    "4) <b>endIndex</b> is the last base position for a requested window. " +
                    "It is treated inclusively;<br/>" +
                    "5) <b>scaleFactor</b> specifies an inverse value to number of bases per one visible element" +
                    " on a track (e.g., pixel) - IS IGNORED FOR NOW",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Track<Wig>> loadHistogram(@RequestBody final TrackQuery trackQuery) throws HistogramReadingException {
        final Track<Wig> geneTrack = Query2TrackConverter.convertToTrack(trackQuery);
        return Result.success(geneSecurityService.loadHistogram(geneTrack));
    }

    @ResponseBody
    @RequestMapping(value = GENE_URL + REFERENCE_ID_FIELD + "}/protein/get", method = RequestMethod.POST)
    @ApiOperation(
            value = "Returns reconstructed protein sequence matched to the given query of gene track.",
            notes = "It provides data for a protein sequence with the given scale factor between the beginning " +
                    "position with the first base having position 1 and ending position inclusive in a target " +
                    "chromosome. All parameters are mandatory and described below:<br/><br/>" +
                    "1) <b>" + REFERENCE_ID_FIELD + "</b> specifies ID of reference genome;<br/>" +
                    "2) <b>id</b> specifies ID of a track;<br/>" +
                    "3) <b>chromosomeId</b> specifies ID of a chromosome corresponded to a track;<br/>" +
                    "4) <b>startIndex</b> is the most left base position for a requested window. The first base in a " +
                    "chromosome always has got position 1;<br/>" +
                    "5) <b>endIndex</b> is the last base position for a requested window. " +
                    "It is treated inclusively;<br/>" +
                    "6) <b>scaleFactor</b> specifies an inverse value to number of bases per one visible element on a" +
                    " track (e.g., pixel) - IS IGNORED FOR NOW",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Track<ProteinSequenceInfo>> loadProteinSequence(@RequestBody final TrackQuery trackQuery,
                      @PathVariable(value = REFERENCE_ID_FIELD) final Long referenceId) throws GeneReadingException {
        return Result.success(proteinSecurityService.loadProteinSequence(Query2TrackConverter.convertToTrack(
            trackQuery), referenceId));
    }

    @RequestMapping(value = "/gene/aminoacids", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Generate amino acids sequence for the given feature ID, case-insensitive",
            notes = "Generate amino acids sequence for the given feature ID, case-insensitive",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<ProteinSequence> constructAminoAcidSequence(
            @RequestBody ProteinSequenceConstructRequest request) throws IOException {
        return Result.success(geneSecurityService.loadProteinSequenceForFeature(request));
    }


    @ResponseBody
    @RequestMapping(value = GENE_URL + REFERENCE_ID_FIELD + "}/variation/protein/get", method = RequestMethod.POST)
    @ApiOperation(
            value = "Returns reconstructed protein sequence matched to the given query of gene track, "
                    + "taking into account variations.",
            notes = "It provides data for a protein sequence with the given scale factor between the beginning " +
                    "position with the first base having position 1 and ending position inclusive in a target " +
                    "chromosome. All parameters are mandatory and described below:<br/><br/>" +
                    "Body: <br/><br/>" +
                    "<b>variations: result of call /gene/{" + REFERENCE_ID_FIELD + "}/protein/get <br/>" +
                    "<b>trackquery:<br/>" +
                    "1) <b>" + REFERENCE_ID_FIELD + "</b> specifies ID of reference genome;<br/>" +
                    "2) <b>id</b> specifies ID of a track;<br/>" +
                    "3) <b>chromosomeId</b> specifies ID of a chromosome corresponded to a track;<br/>" +
                    "4) <b>startIndex</b> is the most left base position for a requested window. The first base in a " +
                    "chromosome always has got position 1;<br/>" +
                    "5) <b>endIndex</b> is the last base position for a requested window. " +
                    "It is treated inclusively;<br/>" +
                    "6) <b>scaleFactor</b> specifies an inverse value to number of bases per one visible element on a" +
                    " track (e.g., pixel)",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Track<MrnaProteinSequenceVariants>> loadProteinSequenceForVariations(
            @RequestBody final ProteinSequenceVariationQuery psVariationQuery, @PathVariable final Long referenceId)
        throws GeneReadingException {
        return Result.success(proteinSecurityService.loadProteinSequenceWithVariations(psVariationQuery, referenceId));
    }

    @RequestMapping(value = "/gene/{trackId}/{chromosomeId}/next", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
            value = "Returns the next feature for a given track",
            notes = "Returns the next feature for a given track in a given chromosome. <br/>" +
                    "Searches from given parameter 'fromPosition' (required)",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Gene> jumpToNextGene(@RequestParam int fromPosition,
                                       @PathVariable(value = "trackId") long geneFileId,
                                       @PathVariable(value = "chromosomeId") long chromosomeId,
                                       @PathVariable(value = "projectId", required = false) Long projectId)
            throws IOException {
        return Result.success(geneSecurityService.getNextOrPreviousFeature(
                fromPosition, geneFileId, chromosomeId, true, projectId));
    }

    @RequestMapping(value = "/gene/{trackId}/{chromosomeId}/prev", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
            value = "Returns the previous feature for a given track",
            notes = "Returns the previous feature for a given track in a given chromosome. <br/>" +
                    "Searches from given parameter 'fromPosition' (required), from a given sample (parameter " +
                    "'sampleId', optional)",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Gene> jumpToPrevGene(@RequestParam int fromPosition,
                                       @PathVariable(value = "trackId") long geneFileId,
                                       @PathVariable(value = "chromosomeId") long chromosomeId,
                                       @PathVariable(value = "projectId", required = false) Long projectId)
            throws IOException {
        return Result.success(geneSecurityService.getNextOrPreviousFeature(
                fromPosition, geneFileId, chromosomeId, false, projectId));
    }

    @RequestMapping(value = "/gene/exons/viewport", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Returns exons of the track",
            notes = "Returns all exons of the track on a given interval, specified by:<br/>" +
                    "<ul><li>centerPosition - a position of a view port's center on the reference</li>" +
                    "<li>viewPortSize - a size of a view port in bps</li>" +
                    "<li>intronLength - a value, determine how much of intron region lengths should be shown in bps" +
                    "Affects the amount of exons fitted in a view port</li></ul>",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<Block>> fetchExons(@RequestBody ExonViewPortQuery query) throws IOException {
        return Result.success(geneSecurityService.loadExonsInViewPort(query.getId(), query.getChromosomeId(),
                query.getCenterPosition(), query.getViewPortSize(), query.getIntronLength(), query.getProjectId()));
    }

    @RequestMapping(value = "/gene/exons/range", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Returns exons of the track",
            notes = "Returns all exons of the track on a given interval, specified by:<br/>" +
                    "<ul><li>startIndex - a start of a range on a chromosome</li>" +
                    "<li>endIndex - an end of a range on a chromosome</li>" +
                    "<li>intronLength - a value, determine how much of intron region lengths should be shown in bps" +
                    "Affects the amount of exons fitted in a view port</li></ul>",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<Block>> fetchExons(@RequestBody ExonRangeQuery query) throws IOException {
        return Result.success(geneSecurityService.loadExonsInTrack(query.getId(), query.getChromosomeId(),
                query.getStartIndex(), query.getEndIndex(), query.getIntronLength(), query.getProjectId()));
    }

    @GetMapping("/gene/{fileId}/doc")
    @ResponseBody
    @ApiOperation(
            value = "Loads specific gene feature content",
            notes = "Loads specific gene feature content",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<GeneHighLevel> loadGeneFeatureByUid(@PathVariable(value = "fileId") final Long fileId,
                                                      @RequestParam(value = "uid") final String uid) {
        return Result.success(geneSecurityService.loadGeneFeatureByUid(fileId, uid));
    }

    @PutMapping("/gene/{fileId}/doc")
    @ResponseBody
    @ApiOperation(
            value = "Updates specific gene feature content",
            notes = "Updates specific gene feature content",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<GeneHighLevel> updateGeneFeatureByUid(@PathVariable(value = "fileId") final Long fileId,
                                                        @RequestParam(value = "uid") final String uid,
                                                        @RequestBody final GeneHighLevel geneContent) {
        return Result.success(geneSecurityService.updateGeneFeatureByUid(fileId, uid, geneContent));
    }
}
