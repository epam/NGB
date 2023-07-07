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

package com.epam.catgenome.controller.externaldb;

import static com.epam.catgenome.component.MessageHelper.getMessage;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.epam.catgenome.controller.vo.ReadSequenceVO;
import com.epam.catgenome.entity.bam.PSLRecord;
import com.epam.catgenome.manager.bam.BlatSearchManager;
import com.epam.catgenome.manager.externaldb.ncbi.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.RegionQuery;
import com.epam.catgenome.controller.vo.converter.UniprotConverter;
import com.epam.catgenome.controller.vo.externaldb.NCBIClinVarVO;
import com.epam.catgenome.controller.vo.externaldb.NCBIGeneVO;
import com.epam.catgenome.controller.vo.externaldb.NCBIShortVarVO;
import com.epam.catgenome.controller.vo.externaldb.NCBIStructVarVO;
import com.epam.catgenome.controller.vo.externaldb.NCBITaxonomyVO;
import com.epam.catgenome.controller.vo.externaldb.NCBIVariationVO;
import com.epam.catgenome.controller.vo.externaldb.UniprotEntryVO;
import com.epam.catgenome.controller.vo.externaldb.ensemblevo.EnsemblEntryVO;
import com.epam.catgenome.controller.vo.externaldb.ensemblevo.EnsemblVariationEntryVO;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.EnsemblDataManager;
import com.epam.catgenome.manager.externaldb.UniprotDataManager;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.Entry;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.Uniprot;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * <p>
 * {@code ExternalDBController} represents implementation of MVC controller which handles
 * requests to manage External DB.
 * </p>
 * */
@Controller
@Api(value = "externaldb", description = "External DB Management")
public class ExternalDBController extends AbstractRESTController {

    private static final String SUCCESS = "info.database.successful.get";
    private static final String NCBI = "NCBI";
    private static final String VARIATION_PATTERN = "rs\\d+";
    private static final String VARIATION_ID_NAME = "rsId";
    private static final String PARAMETERS_NOTE = "Parameters:<br/>";
    private static final String RETURNS_NOTE = "Returns:<br/>";
    private static final String RSID_DESCRIPTION_NOTE =
            "<b>rsId</b> - NCBI variation identifier starting with \"rs\": e.g. rs7412<br/>";
    private static final String ENSEMBL = "Ensembl";

    @Autowired
    private UniprotDataManager uniprotDataManager;

    @Autowired
    private EnsemblDataManager ensemblDataManager;

    @Autowired
    private NCBIGeneManager ncbiGeneManager;

    @Autowired
    private NCBIGeneIdsManager ncbiGeneIdsManager;

    @Autowired
    private NCBIShortVarManager ncbiShortVarManager;

    @Autowired
    private NCBIStructVarManager ncbiStructVarManager;

    @Autowired
    private NCBIClinVarManager ncbiClinVarManager;

    @Autowired
    private NCBIAuxiliaryManager ncbiAuxiliaryManager;

    @Autowired
    private BlatSearchManager blatSearchManager;

    @ResponseBody
    @RequestMapping(value = "/externaldb/uniprot/{geneId}/get", method = RequestMethod.GET)
    @ApiOperation(value = "UniProt: Retrieves information on protein using geneId.",
        notes = "UniProt database (http://www.uniprot.org/) is being queried for information by gene " +
                "identifier <i>in any database</i>. Protein information in XML form is returned by UniProt, " +
                "this information is converted to presentation required by NGGB.<br/><br/>" +
                "Examples of id which could be used as a parameter to this service:<br/><br/>" +
                "ENSG00000106683 -- Ensembl ID<br/>" +
                "FBgn0000008 -- Fly Base ID<br/>",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = { @ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION) })
    public Result<List<UniprotEntryVO>> fetchUniprotData(@PathVariable(value = "geneId") final String geneId)
            throws ExternalDbUnavailableException {

        Assert.notNull(geneId, getMessage(MessagesConstants.ERROR_GENEID_NOT_SPECIFIED));
        Uniprot jaxbUniprotData = uniprotDataManager.fetchUniprotEntry(geneId);
        List<Entry> jaxbUniprotEntries = jaxbUniprotData.getEntry();
        List<UniprotEntryVO> uniprotEntriesVO = jaxbUniprotEntries.stream().map(UniprotConverter::convertTo)
                .collect(Collectors.toList());
        return Result.success(uniprotEntriesVO, getMessage(SUCCESS, "UniProt"));
    }

    @ResponseBody
    @RequestMapping(value = "/externaldb/ensembl/{geneId}/get", method = RequestMethod.GET)
    @ApiOperation(value = "Ensembl: Retrieves information on gene using geneId.",
            notes = "Ensembl database (http://rest.ensembl.org/) is being queried for information by gene " +
                    "identifier <i>in any database</i>. Gene information in JSON form is returned by Ensembl, " +
                    "this information is converted to presentation required by NGGB.<br/><br/>" +
                    "Examples of id which could be used as a parameter to this service:<br/><br/>" +
                    "ENSG00000106683 -- Ensembl ID<br/>" +
                    "FBgn0000008 -- Fly Base ID<br/>",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = { @ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION) })
    public Result<EnsemblEntryVO> fetchEnsemblData(@PathVariable(value = "geneId") final String geneId)
            throws ExternalDbUnavailableException {

        Assert.notNull(geneId, getMessage(MessagesConstants.ERROR_GENEID_NOT_SPECIFIED));
        EnsemblEntryVO ensemblEntryVO = ensemblDataManager.fetchEnsemblEntry(geneId);

        return Result.success(ensemblEntryVO, getMessage(SUCCESS, ENSEMBL));
    }

    @ResponseBody
    @RequestMapping(value = "/externaldb/ensembl/variation/variationId={rsId}&species={species}",
            method = RequestMethod.GET)
    @ApiOperation(value = "Ensembl: Retrieves information on variation for certain species using variation id(rsId).",
            notes = "Ensembl database (http://rest.ensembl.org/) is being queried by rsId and species " +
                    "type. Information of variation for certain species is returned from Ensembl db.<br/></br> " +
                    PARAMETERS_NOTE +
                    RSID_DESCRIPTION_NOTE + "<br/>" +
                    "<b>species</b> - species name: human<br/>",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = { @ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION) })
    public Result<EnsemblVariationEntryVO> fetchEnsemblVariationData(
            @PathVariable(value = VARIATION_ID_NAME) final String rsId,
            @PathVariable(value = "species") final String species)
            throws ExternalDbUnavailableException {

        Assert.notNull(rsId, getMessage(MessagesConstants.ERROR_VARIATIONID_NOT_SPECIFIED));
        EnsemblVariationEntryVO ensemblVariationEntryVO = ensemblDataManager.fetchVariationEntry(rsId, species);

        return Result.success(ensemblVariationEntryVO, getMessage(SUCCESS, ENSEMBL));
    }

    @ResponseBody
    @RequestMapping(value = "/externaldb/ensembl/variation/region", method = RequestMethod.POST)
    @ApiOperation(value = "Ensembl: Retrieves information on variations located within certain chromosomal coordiates.",
            notes = "Ensembl database (http://rest.ensembl.org/) is being queried for variations located " +
                    "in between certain coordinates (start and finish) for certain chromosome " +
                    "for the given species. <br/><br/>" +
                    PARAMETERS_NOTE +
                    "<b>species</b> - species name (e.g. \"human\")<br/>" +
                    "<b>chromosome</b> - chromosome name (e.g. \"1\")<br/>" +
                    "<b>start</b> - start coordinate in a given chromosome (e.g. 140424943)<br/>" +
                    "<b>finish</b> - end coordinate in a given chromosome (e.g. 140624564)<br/>",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = { @ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION) })
    public Result<List<EnsemblEntryVO>> fetchEnsemblVariationOnRegionData(
            @RequestBody final RegionQuery regionQuery)
            throws ExternalDbUnavailableException {

        String species = regionQuery.getSpecies();
        String chromosome = regionQuery.getChromosome();
        String start = regionQuery.getStart();
        String finish = regionQuery.getFinish();

        Assert.notNull(species, getMessage(MessagesConstants.ERROR_SPECIES_NOT_SPECIFIED));
        Assert.notNull(chromosome, getMessage(MessagesConstants.ERROR_CHROMOSOME_NOT_SPECIFIED));
        Assert.notNull(start, getMessage(MessagesConstants.ERROR_STARTPOSITION_NOT_SPECIFIED));
        Assert.notNull(finish, getMessage(MessagesConstants.ERROR_FINISHPOSITION_NOT_SPECIFIED));

        List<EnsemblEntryVO> variationEntryVOList =
                ensemblDataManager.fetchVariationsOnRegion(species, chromosome, start, finish);

        return Result.success(variationEntryVOList, getMessage(SUCCESS, ENSEMBL));
    }

    @ResponseBody
    @RequestMapping(value = "/externaldb/ncbi/gene/{geneId}/get", method = RequestMethod.GET)
    @ApiOperation(value = "NCBI: Retrieves gene information using gene id.",
            notes = "NCBI database (http://eutils.ncbi.nlm.nih.gov/entrez/eutils/) " +
                    "is being queried by gene id.<br/><br/>" +
                    PARAMETERS_NOTE +
                    "<b>geneId</b> - NCBI gene id or Ensembl gene id (e.g. 3985, ENSG00000106683)<br/><br/>" +
                    RETURNS_NOTE +
                    "NCBIGeneVO containing following information:<br/>" +
                    "<b>Organism</b> -- organism name (e.g. human (Homo sapiens))<br/>" +
                    "<b>Primary source</b> -- primary source of information on gene (e.g. HGNC:6614)<br/>" +
                    "<b>Gene type</b> -- type of the gene (e.g. protein coding)<br/>" +
                    "<b>RefSeq status</b> -- status (e.g. REVIEWED)<br/>" +
                    "<b>Summary</b> -- text description of gene<br/>" +
                    "<b>Related articles in PubMed</b> -- pubmed db articles<br/>" +
                    "<b>Pathways from BioSystems</b> -- biosystems reference<br/>" +
                    "<b>Interactions</b> -- interactions information<br/>",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = { @ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION) })
    public Result<NCBIGeneVO> fetchNCBIGene(@PathVariable(value = "geneId") final String geneId)
            throws ExternalDbUnavailableException {

        Assert.notNull(geneId, getMessage(MessagesConstants.ERROR_GENEID_NOT_SPECIFIED));
        NCBIGeneVO gene = ncbiGeneManager.fetchGeneById(geneId);

        return Result.success(gene, getMessage(SUCCESS, NCBI));
    }

    @ResponseBody
    @RequestMapping(value = "/externaldb/ncbi/variation/short/{rsId}/get", method = RequestMethod.GET)
    @ApiOperation(value = "NCBI: Retrieves information on short variation using rsId.",
            notes = "NCBI database (http://eutils.ncbi.nlm.nih.gov/entrez/eutils/) is being queried by variation id" +
                    "(rsId) for short variation information.<br/><br/>" +
                    PARAMETERS_NOTE +
                    RSID_DESCRIPTION_NOTE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = { @ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION) })
    public Result<NCBIShortVarVO> fetchNCBIShortVariation(@PathVariable(value = VARIATION_ID_NAME) final String rsId)
            throws ExternalDbUnavailableException {

        Assert.notNull(rsId, getMessage(MessagesConstants.ERROR_VARIATIONID_NOT_SPECIFIED));
        Assert.isTrue(rsId.matches(VARIATION_PATTERN), getMessage(MessagesConstants.ERROR_VARIATIONID_NOT_SPECIFIED));
        NCBIShortVarVO snp = ncbiShortVarManager.fetchVariationById(rsId.substring(2));
        return Result.success(snp, getMessage(SUCCESS, NCBI));
    }

    @ResponseBody
    @RequestMapping(value = "/externaldb/ncbi/variation/struct/{rsId}/get", method = RequestMethod.GET)
    @ApiOperation(value = "NCBI: Retrieves information on structural variation using rsId.",
            notes = "NCBI structural variations db is being queried by rsId.<br/><br/>" +
                    PARAMETERS_NOTE +
                    RSID_DESCRIPTION_NOTE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = { @ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION) })
    public Result<NCBIStructVarVO> fetchNCBIStructVariation(@PathVariable(value = VARIATION_ID_NAME) final String rsId)
            throws ExternalDbUnavailableException {

        Assert.notNull(rsId, getMessage(MessagesConstants.ERROR_VARIATIONID_NOT_SPECIFIED));
        Assert.isTrue(rsId.matches(VARIATION_PATTERN), getMessage(MessagesConstants.ERROR_VARIATIONID_NOT_SPECIFIED));
        NCBIStructVarVO var = ncbiStructVarManager.fetchVariationById(rsId.substring(2));
        return Result.success(var, getMessage(SUCCESS, NCBI));
    }

    @ResponseBody
    @RequestMapping(value = "/externaldb/ncbi/variation/clin/{rsId}/get", method = RequestMethod.GET)
    @ApiOperation(value = "NCBI: Retrieves clinical information on variation using rsId.",
            notes = "NCBI ClinVar is being queried by rsId<br/><br/>" +
                    PARAMETERS_NOTE +
                    RSID_DESCRIPTION_NOTE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = { @ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION) })
    public Result<NCBIClinVarVO> fetchNCBIClinVariation(@PathVariable(value = VARIATION_ID_NAME) final String rsId)
            throws ExternalDbUnavailableException {

        Assert.notNull(rsId, getMessage(MessagesConstants.ERROR_VARIATIONID_NOT_SPECIFIED));
        Assert.isTrue(rsId.matches(VARIATION_PATTERN), getMessage(MessagesConstants.ERROR_VARIATIONID_NOT_SPECIFIED));

        NCBIClinVarVO clin = ncbiClinVarManager.fetchVariationById(rsId.substring(2));
        return Result.success(clin, getMessage(SUCCESS, NCBI));
    }

    @ResponseBody
    @RequestMapping(value = "/externaldb/ncbi/taxonomy/{taxId}/get", method = RequestMethod.GET)
    @ApiOperation(value = "NCBI: Retrieves information on species from taxonomy database.",
            notes = "NCBI Taxonomy database is being queried by taxId for organism information.<br/><br/>" +
                    PARAMETERS_NOTE +
                    "<b>taxId</b> - id of species in taxonomy database (e.g. 9606)<br/><br/>" +
                    RETURNS_NOTE +
                    "<b>commonName</b> - common species name (e.g. \"human\")<br/>" +
                    "<b>scientificName</b> - scientific species name (e.g. \"homo sapiens\")<br/>",
                    produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = { @ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION) })
    public Result<NCBITaxonomyVO> fetchNCBITaxonomyInfo(@PathVariable(value = "taxId") final String taxId)
            throws ExternalDbUnavailableException {

        Assert.notNull(taxId, getMessage(MessagesConstants.ERROR_VARIATIONID_NOT_SPECIFIED));
        NCBITaxonomyVO ncbiTaxonomyVO = ncbiAuxiliaryManager.fetchTaxonomyInfoById(taxId);
        return Result.success(ncbiTaxonomyVO, getMessage(SUCCESS, NCBI));
    }

    @ResponseBody
    @RequestMapping(value = "/externaldb/ncbi/taxonomy/{term}", method = RequestMethod.GET)
    @ApiOperation(value = "NCBI: Retrieves list of organisms for search query",
            notes = "NCBI: Retrieves list of organisms for search query",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = { @ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION) })
    public Result<List<NCBITaxonomyVO>> getOrganismsByTerm(@PathVariable final String term)
            throws ExternalDbUnavailableException, JsonProcessingException {
        return Result.success(ncbiAuxiliaryManager.fetchTaxonomyInfosByTermMock(term));
    }

    @ResponseBody
    @RequestMapping(value = "/externaldb/ncbi/variation/{rsId}/get", method = RequestMethod.GET)
    @ApiOperation(value = "NCBI: Retrieves aggregated information on variation using rsId.",
            notes = "Several NCBI databases are being queried by rsId, result information is being aggregated" +
                    "in a proper way and returned by service.<br/><br/>" +
                    PARAMETERS_NOTE +
                    RSID_DESCRIPTION_NOTE +
                    "Following sequence of queries is peroformed:<br/>" +
                    "1) Short variation db poll<br/>" +
                    "2) Clin variation db poll<br/>" +
                    "3) Taxonomy db poll<br/>", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = { @ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION) })
    public Result<NCBIVariationVO> fetchNCBIAggregatedVariationInfo(@PathVariable(value = VARIATION_ID_NAME)
                                                                        final String rsId)
            throws ExternalDbUnavailableException {

        Assert.notNull(rsId, getMessage(MessagesConstants.ERROR_VARIATIONID_NOT_SPECIFIED));
        Assert.isTrue(rsId.matches(VARIATION_PATTERN), getMessage(MessagesConstants.ERROR_VARIATIONID_NOT_SPECIFIED));

        String variationId = rsId.substring(2);
        NCBIVariationVO result = ncbiShortVarManager.fetchAggregatedVariationById(variationId);
        return Result.success(result, getMessage(SUCCESS, NCBI));
    }

    @ResponseBody
    @RequestMapping(value = "/externaldb/ncbi/variation/region", method = RequestMethod.POST)
    @ApiOperation(
            value = "NCBI: Retrieves information on variations located in given interval",
            notes = "NCBI snp database is being queried for variations in given interval" +
                    "in a proper way and returned by service.<br/><br/>" +
                    PARAMETERS_NOTE +
                    "<b>species</b> - species name (e.g. \"human\")<br/>" +
                    "<b>chromosome</b> - chromosome name (e.g. \"1\")<br/>" +
                    "<b>start</b> - start coordinate in a given chromosome (e.g. 140424943)<br/>" +
                    "<b>finish</b> - end coordinate in a given chromosome (e.g. 140624564)<br/>" +
                    RSID_DESCRIPTION_NOTE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = { @ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION) })
    public Result<List<Variation>> fetchNCBIVariationInfoOnInterval(@RequestBody final RegionQuery regionQuery)
            throws ExternalDbUnavailableException {

        String chromosome = regionQuery.getChromosome();
        String start = regionQuery.getStart();
        String finish = regionQuery.getFinish();
        String species = regionQuery.getSpecies();

        List<Variation> variations = ncbiShortVarManager.fetchVariationsOnRegion(species, start, finish, chromosome);
        return Result.success(variations, getMessage(SUCCESS, NCBI));
    }

    @ResponseBody
    @RequestMapping(value = "/externaldb/blat/search", method = RequestMethod.POST)
    @ApiOperation(
        value = "Returns statistics generated by BLAT search.",
        notes = "Provides statistics generated by BLAT search performed on a read sequence. ReferenceId is required "
                + "to search for required species",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<List<PSLRecord>> blatReadSequence(@RequestParam
                                                    final Long referenceId,
                                                    @RequestBody final ReadSequenceVO readSequence)
        throws IOException, ExternalDbUnavailableException {
        return Result.success(blatSearchManager.findBlatReadSequence(referenceId, readSequence.getReadSequence()));
    }

    @ResponseBody
    @RequestMapping(value = "/externaldb/ncbi/genes/import", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Imports gene ids data from NCBI",
            notes = "Imports gene ids data from NCBI",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> importNCBIGeneIdsData(@RequestParam final String path) throws IOException, ParseException {
        ncbiGeneIdsManager.importData(path);
        return Result.success(null);
    }
}
