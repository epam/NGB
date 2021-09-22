/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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

import com.epam.catgenome.controller.vo.ItemsByProject;
import com.epam.catgenome.controller.vo.SpeciesVO;
import com.epam.catgenome.entity.gene.GeneFilterForm;
import com.epam.catgenome.entity.gene.GeneFilterInfo;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.GeneIndexEntry;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.reference.Species;
import com.epam.catgenome.entity.reference.StrandedSequence;
import com.epam.catgenome.entity.reference.motif.MotifSearchRequest;
import com.epam.catgenome.entity.reference.motif.MotifSearchResult;
import com.epam.catgenome.entity.reference.motif.MotifTrackQuery;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.manager.FeatureIndexSecurityService;
import com.epam.catgenome.manager.export.GeneExportFilterForm;
import com.epam.catgenome.manager.reference.ReferenceSecurityService;
import com.epam.catgenome.util.FileFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.TrackQuery;
import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.exception.ReferenceReadingException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;

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
    private ReferenceSecurityService referenceSecurityService;

    @Autowired
    private FeatureIndexSecurityService featureIndexSecurityService;

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
    public final Callable<Result<List<Reference>>> loadAllReferences(
            @RequestParam(required = false) String referenceName) throws IOException {
        return () -> Result.success(referenceSecurityService.loadAllReferenceGenomes(referenceName));
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
        return Result.success(referenceSecurityService.load(referenceId));
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
        return Result.success(referenceSecurityService.loadReferenceGenomeByBioItemId(bioItemID));
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
        return Result.success(referenceSecurityService.loadChromosomes(referenceId));
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
        return Result.success(referenceSecurityService.loadChromosome(chromosomeId));
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
        final Track<Sequence> track = referenceSecurityService.getNucleotidesResultFromNib(convertToTrack(query));
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
    public Result<IndexSearchResult<FeatureIndexEntry>> searchFeatureInProject(
            @PathVariable(value = "referenceId") final Long referenceId,
            @RequestParam final String featureId) throws IOException {
        return Result.success(featureIndexSecurityService.searchFeaturesByReference(featureId, referenceId));
    }

    @RequestMapping(value = "/reference/{referenceId}/filter/gene", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
        value = "Searches for given filter parameters in a reference gene file, case-insensitive",
        notes = "Searches an index of a gene file, associated with a given reference's ID for filter parameters",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<IndexSearchResult<GeneIndexEntry>> searchFeatureInProjectWithFilter(
                                                            @PathVariable(value = "referenceId") final Long referenceId,
                                                            @RequestBody final GeneFilterForm geneFilterForm)
        throws IOException {
        return Result.success(featureIndexSecurityService.searchFeaturesByReference(geneFilterForm, referenceId));
    }

    @PostMapping(value = "/reference/{referenceId}/filter/gene/export")
    @ResponseBody
    @ApiOperation(
        value = "Searches for given filter parameters in a reference gene file, case-insensitive and exports " +
                "result to CSV/TSV file",
        notes = "Searches an index of a gene file, associated with a given reference's ID for filter parameters " +
                "and exports result to CSV/TSV file",
        produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ApiResponses(value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)})
    public void exportFeatureInProjectWithFilter(@PathVariable final Long referenceId,
                                                @RequestParam final FileFormat format,
                                                @RequestParam final boolean includeHeader,
                                                @RequestBody final GeneExportFilterForm geneFilterForm,
                                                final HttpServletResponse response)
            throws IOException {
        byte[] bytes = featureIndexSecurityService.exportFeaturesByReference(geneFilterForm, referenceId,
                format, includeHeader);
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }

    @RequestMapping(value = "/reference/{referenceId}/filter/gene/info", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Returns information about possible custom gene table columns",
            notes = "Searches an index of a gene files, associated with a given reference's IDs for filter parameters",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<GeneFilterInfo> getAvailableFieldsToSearch(
            @PathVariable(value = "referenceId") final Long referenceId,
            @RequestBody(required = false) ItemsByProject fileIdsByProjectId) throws IOException {
        return Result.success(featureIndexSecurityService.getAvailableFieldsToSearch(referenceId, fileIdsByProjectId));
    }

    @PostMapping(value = "/reference/{referenceId}/filter/gene/values")
    @ResponseBody
    @ApiOperation(
            value = "Returns information about possible custom gene table columns",
            notes = "Searches an index of a gene files, associated with a given reference's IDs for filter parameters",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Set<String>> getAvailableFieldValues(
            @PathVariable final Long referenceId,
            @RequestBody(required = false) final ItemsByProject fileIdsByProjectId,
            @RequestParam final String fieldName) throws IOException {
        return Result.success(featureIndexSecurityService.getAvailableFieldValues(referenceId, fileIdsByProjectId,
                fieldName));
    }

    @ResponseBody
    @RequestMapping(value = "/secure/reference/register/fasta", method = RequestMethod.POST)
    @ApiOperation(
            value = "Handles FASTA register and parse a new genome in the system.",
            notes = "The following genome file types are supported: *.fasta, *.fasta.gz, *.fa, *.fa.gz, *.fna, " +
                    "*.fna.gz, *.txt, *.txt.gz, *.genbank, *.gbk, *.gb, *.gbf.<br/>" +
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
        return Result.success(referenceSecurityService.registerGenome(request));
    }

    @ResponseBody
    @RequestMapping(value = "/secure/reference/{referenceId}/genes", method = RequestMethod.PUT)
    public Result<Reference> updateReferenceGeneFile(@PathVariable Long referenceId,
                                                     @RequestParam(required = false) Long geneFileId) {
        return Result.success(referenceSecurityService.updateReferenceGeneFileId(referenceId, geneFileId));
    }

    @ResponseBody
    @RequestMapping(value = "/secure/reference/{referenceId}/updateAnnotation", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Update annotation file to the reference.",
            notes = "Update (add or remove) annotation file for the reference (BED, GTF, GFF).",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Reference> updateReferenceAnnotationFile(@PathVariable Long referenceId,
                                                           @RequestParam Long annotationFileId,
                                                           @RequestParam(defaultValue = "false") Boolean remove)
            throws IOException, FeatureIndexException {
        return Result.success(
                referenceSecurityService.updateReferenceAnnotationFile(referenceId, annotationFileId, remove)
        );
    }

    @ResponseBody
    @RequestMapping(value = "/secure/reference/register/fasta", method = RequestMethod.DELETE)
    @ApiOperation(value = "Unregisters a reference file in the system.",
            notes = "Delete all information about this file by id", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Boolean> unregisterFastaFile(@RequestParam final long referenceId) throws IOException {
        Reference reference = referenceSecurityService.unregisterGenome(referenceId);
        return Result.success(true, getMessage(MessagesConstants.INFO_UNREGISTER, reference.getName()));
    }

    @ResponseBody
    @RequestMapping(value = "/secure/reference/register/species", method = RequestMethod.POST)
    @ApiOperation(
        value = "Handles species register.",
        notes = "Register new species in the system.",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<Species> registerSpecies(@RequestBody SpeciesVO request) throws IOException {
        return Result.success(referenceSecurityService.registerSpecies(request.getSpecies()));
    }

    @ResponseBody
    @RequestMapping(value = "/secure/reference/register/species", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Updates an existing species object",
            notes = "Updates an existing species object",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Species> updatedSpecies(@RequestBody SpeciesVO request) {
        return Result.success(referenceSecurityService.updateSpecies(request.getSpecies()));
    }

    @ResponseBody
    @RequestMapping(value = "/secure/reference/register/species", method = RequestMethod.DELETE)
    @ApiOperation(
            value = "Unregister a species in the system.",
            notes = "Delete all information about this species by it`s version.")
    public Result<Boolean> unregisterSpecies(@RequestParam String speciesVersion) throws IOException {
        Species species = referenceSecurityService.unregisterSpecies(speciesVersion);
        return Result.success(true, getMessage(MessagesConstants.INFO_UNREGISTERED_SPECIES, species.getName(),
                species.getVersion()));
    }

    @ResponseBody
    @RequestMapping(value = "/reference/loadAllSpecies", method = RequestMethod.GET)
    @ApiOperation(
        value = "Returns all species that are available in the system at the moment.",
        notes = "Returns all species that are available in the system at the moment.",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)})
    public final Result<List<Species>> loadAllSpecies() throws IOException {
        return Result.success(referenceSecurityService.loadAllSpecies());
    }

    @ResponseBody
    @RequestMapping(value = "/secure/reference/{referenceId}/species", method = RequestMethod.PUT)
    public Result<Reference> updateSpecies(@PathVariable Long referenceId,
        @RequestParam(required = false) String speciesVersion) {
        return Result.success(referenceSecurityService.updateSpecies(referenceId, speciesVersion));
    }

    @RequestMapping(value = "/reference/motif", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Returns a track containing a list of StrandedSequence that contains positions," +
                    " sequence and strand",
            notes = "Returns information about matches found at the specified motif",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Track<StrandedSequence>> loadMotifTrack(@RequestBody MotifTrackQuery trackQuery) {
        return Result.success(referenceSecurityService.fillTrackWithMotifSearch(convertToTrack(trackQuery),
                trackQuery.getMotif(), trackQuery.getStrand()));
    }

    @RequestMapping(value = "/reference/motif/table", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Returns a table containing a list of motifs found",
            notes = "Returns table with information about matches found at the specified motif",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<MotifSearchResult> loadMotifTable(@RequestBody MotifSearchRequest motifSearchRequest) {
        return Result.success(referenceSecurityService.getTableByMotif(motifSearchRequest));
    }
}
