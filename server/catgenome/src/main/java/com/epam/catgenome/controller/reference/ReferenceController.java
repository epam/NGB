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
import static com.epam.catgenome.controller.vo.Query2TrackConverter.convertToTrack;

import java.io.IOException;
import java.util.List;

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
import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.exception.ReferenceReadingException;
import com.epam.catgenome.manager.FeatureIndexManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.reference.ReferenceManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * <p>
 * {@code ReferenceController} represents implementation of MVC controller which handles
 * requests to manage data about references.
 * <p>
 * It's designed to communicate with corresponded managers that provide all required
 * calls and manage all operations concerned with a reference.
 */
@Controller
@Api(value = "reference-genome", description = "Reference Genomes Management")
public class ReferenceController extends AbstractRESTController {

    @Autowired
    private ReferenceManager referenceManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private FeatureIndexManager featureIndexManager;

    @ResponseBody
    @RequestMapping(value = "/reference/loadAll", method = RequestMethod.GET)
    @ApiOperation(
            value = "Returns all reference genomes that are available in the system at the moment.",
            notes = "Each summary provides only major metadata per a single reference genome, excluding detailed " +
                    "information about its chromosomes. That means $.payload.chromosomes is always empty or null in " +
                    "this case.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public final Result<List<Reference>> loadAllReferences(
            @RequestParam(required = false) String referenceName) throws IOException {
        return Result.success(referenceGenomeManager.loadAllReferenceGenomes(referenceName));
    }

    @ResponseBody
    @RequestMapping(value = "/reference/{referenceId}/load", method = RequestMethod.GET)
    @ApiOperation(
            value = "Returns detailed information about a reference genome associated with the given ID.",
            notes = "It provides major metadata per a single reference genome, including detailed information " +
                    "about all its chromosomes.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public final Result<Reference> loadReference(@PathVariable final Long referenceId) throws IOException {
        return Result.success(referenceGenomeManager.loadReferenceGenome(referenceId));
    }

    @ResponseBody
    @RequestMapping(value = "/reference/{bioItemID}/loadByBioID", method = RequestMethod.GET)
    @ApiOperation(
            value = "Returns meta information about a reference genome associated with the given "
                    + "BiologicalDataItem ID.",
            notes = "It provides major metadata per a single reference genome, without information " +
                    "about reference chromosomes.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public final Result<Reference> loadReferenceBuBioId(@PathVariable final Long bioItemID) throws IOException {
        return Result.success(referenceGenomeManager.loadReferenceGenomeByBioItemId(bioItemID));
    }

    @ResponseBody
    @RequestMapping(value = "/reference/{referenceId}/loadChromosomes", method = RequestMethod.GET)
    @ApiOperation(
            value = "Returns all chromosomes for a reference genome associated with the given ID.",
            notes = "It provides summaries of all chromosomes that are available in the system for the given " +
                    "reference genome.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public final Result<List<Chromosome>> loadChromosomes(@PathVariable final Long referenceId) throws IOException {
        return Result.success(referenceGenomeManager.loadChromosomes(referenceId));
    }

    @ResponseBody
    @RequestMapping(value = "/reference/chromosomes/{chromosomeId}/load", method = RequestMethod.GET)
    @ApiOperation(
            value = "Returns detailed information about a single chromosome associated with the given ID.",
            notes = "It provides major metadata per a single chromosome.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public final Result<Chromosome> loadChromosome(@PathVariable final Long chromosomeId) throws IOException {
        return Result.success(referenceGenomeManager.loadChromosome(chromosomeId));
    }

    @ResponseBody
    @RequestMapping(value = "/reference/track/get", method = RequestMethod.POST)
    @ApiOperation(
            value = "Returns data matched the given query to fill in a reference genome track.",
            notes = "It provides data for a reference genome track with the given scale factor between the " +
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
    public final Result<Track<Sequence>> loadTrack(@RequestBody final TrackQuery query) throws
            ReferenceReadingException {
        final Track<Sequence> track = referenceManager.getNucleotidesResultFromNib(convertToTrack(query));
        return Result.success(track);
    }

    @RequestMapping(value = "/reference/{referenceId}/search", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
        value = "Searches for a given feature ID in a reference gene file, case-insensitive",
        notes = "Searches an index of a gene file, associated with a given reference's ID for a specified feature ID",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<IndexSearchResult> searchFeatureInProject(@PathVariable(value = "referenceId") final Long referenceId,
                                                            @RequestParam String featureId)
        throws IOException {
        return Result.success(featureIndexManager.searchFeaturesByReference(featureId, referenceId));
    }

    @ResponseBody
    @RequestMapping(value = "/secure/reference/register/fasta", method = RequestMethod.POST)
    @ApiOperation(
            value = "Handles FASTA register and parse a new genome in the system.",
            notes = "The following genome file types are supported: *.fasta, *.fasta.gz, *.fa, *.fa.gz, *.fna, " +
                    "*.fna.gz, *.txt, *.txt.gz.<br/>" +
                    "Optionally you can specify a user-friendly name for an uploaded genome through request " +
                    "parameter <b>name</b>, by default the original file name with omitted extension is used.<br/>" +
                    "It results in a payload that provides detailed information about a genome, including all " +
                    "its chromosomes.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Reference> registerFastaFile(@RequestBody
                                                   ReferenceRegistrationRequest request) throws IOException {
        return Result.success(referenceManager.registerGenome(request));
    }

    @ResponseBody
    @RequestMapping(value = "/secure/reference/{referenceId}/genes", method = RequestMethod.PUT)
    public Result<Reference> updateReferenceGeneFile(@PathVariable Long referenceId,
                                                     @RequestParam(required = false) Long geneFileId) {
        return Result.success(referenceGenomeManager.updateReferenceGeneFileId(referenceId, geneFileId));
    }

    @ResponseBody
    @RequestMapping(value = "/secure/reference/register/fasta", method = RequestMethod.DELETE)
    @ApiOperation(value = "Unregisters a reference file in the system.",
            notes = "Delete all information about this file by id", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Boolean> unregisterFastaFile(@RequestParam final long referenceId) throws IOException {
        Reference reference = referenceManager.unregisterGenome(referenceId);
        return Result.success(true, getMessage(MessagesConstants.INFO_UNREGISTER, reference.getName()));
    }
}
