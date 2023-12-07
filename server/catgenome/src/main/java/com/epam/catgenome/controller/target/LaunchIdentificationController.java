/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
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

package com.epam.catgenome.controller.target;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.externaldb.NCBISummaryVO;
import com.epam.catgenome.controller.vo.target.PublicationSearchRequest;
import com.epam.catgenome.controller.vo.target.StructuresSearchRequest;
import com.epam.catgenome.entity.externaldb.ncbi.GeneInfo;
import com.epam.catgenome.entity.externaldb.target.dgidb.DGIDBDrugAssociation;
import com.epam.catgenome.entity.externaldb.target.opentargets.BareDisease;
import com.epam.catgenome.entity.externaldb.target.opentargets.DiseaseAssociationAggregated;
import com.epam.catgenome.entity.externaldb.target.opentargets.DrugAssociation;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDisease;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDrug;
import com.epam.catgenome.entity.target.*;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto.Structure;
import com.epam.catgenome.manager.externaldb.target.AssociationSearchRequest;
import com.epam.catgenome.manager.externaldb.target.dgidb.DGIDBDrugFieldValues;
import com.epam.catgenome.manager.externaldb.target.opentargets.DrugFieldValues;
import com.epam.catgenome.manager.externaldb.target.pharmgkb.PharmGKBDrugFieldValues;
import com.epam.catgenome.manager.target.LaunchIdentificationSecurityService;
import com.epam.catgenome.manager.target.export.TargetExportSecurityService;
import com.epam.catgenome.manager.target.export.TargetExportTable;
import com.epam.catgenome.util.FileFormat;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@Api(value = "target-identification", description = "Launch Target Identification Management")
@RequiredArgsConstructor
public class LaunchIdentificationController extends AbstractRESTController {

    private final LaunchIdentificationSecurityService launchIdentificationSecurityService;
    private final TargetExportSecurityService exportSecurityService;

    @PostMapping(value = "/target/identification")
    @ApiOperation(
            value = "Launches Target Identification",
            notes = "Launches Target Identification",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<TargetIdentificationResult> launchIdentification(@RequestBody final IdentificationRequest request)
            throws ExternalDbUnavailableException, ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.launchIdentification(request));
    }

    @PostMapping(value = "/target/dgidb/drugs")
    @ApiOperation(
            value = "Launches Identification for dgidb datasource drug associations",
            notes = "Launches Identification for dgidb datasource drug associations." +
                    "Available field names for sorting and filtering: GENE_ID, DRUG_NAME, DRUG_CLAIM_NAME, " +
                    "INTERACTION_TYPES, INTERACTION_CLAIM_SOURCE.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<DGIDBDrugAssociation>> getDGIDbDrugs(@RequestBody final AssociationSearchRequest request)
            throws ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.getDGIDbDrugs(request));
    }

    @PostMapping(value = "/target/pharmgkb/drugs")
    @ApiOperation(
            value = "Launches Identification for PharmGKB datasource drug associations",
            notes = "Launches Identification for PharmGKB datasource drug associations." +
            "Available field names for sorting and filtering: GENE_ID, DRUG_NAME, SOURCE.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<PharmGKBDrug>> getPharmGKBDrugs(@RequestBody final AssociationSearchRequest request)
            throws ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.getPharmGKBDrugs(request));
    }

    @PostMapping(value = "/target/pharmgkb/diseases")
    @ApiOperation(
            value = "Launches Identification for PharmGKB datasource disease associations",
            notes = "Launches Identification for PharmGKB datasource disease associations." +
                    "Available field names for sorting and filtering: GENE_ID, DISEASE_NAME.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<PharmGKBDisease>> getPharmGKBDiseases(
            @RequestBody final AssociationSearchRequest request) throws ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.getPharmGKBDiseases(request));
    }

    @PostMapping(value = "/target/opentargets/drugs")
    @ApiOperation(
            value = "Launches Identification for Open Targets datasource drug associations",
            notes = "Launches Identification for Open Targets datasource drug associations." +
                    "Available field names for sorting and filtering: GENE_ID, DRUG_NAME, DISEASE_NAME, DRUG_TYPE, " +
                    "MECHANISM_OF_ACTION, ACTION_TYPE, PHASE, STATUS, SOURCE.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<DrugAssociation>> getOpenTargetsDrugs(
            @RequestBody final AssociationSearchRequest request) throws ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.getOpenTargetsDrugs(request));
    }

    @PostMapping(value = "/target/opentargets/diseases")
    @ApiOperation(
            value = "Launches Identification for Open Targets datasource disease associations",
            notes = "Launches Identification for Open Targets datasource disease associations." +
                    "Available field names for sorting and filtering: GENE_ID, DISEASE_NAME, OVERALL_SCORE, " +
                    "GENETIC_ASSOCIATIONS_SCORE, SOMATIC_MUTATIONS_SCORE, DRUGS_SCORE, PATHWAYS_SCORE, " +
                    "TEXT_MINING_SCORE, RNA_EXPRESSION_SCORE, RNA_EXPRESSION_SCORE, ANIMAL_MODELS_SCORE.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<DiseaseAssociationAggregated>> getOpenTargetsDiseases(
            @RequestBody final AssociationSearchRequest request) throws ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.getOpenTargetsDiseases(request));
    }

    @PostMapping(value = "/target/opentargets/diseases/all")
    @ApiOperation(
            value = "Launches Identification for Open Targets datasource disease associations",
            notes = "Launches Identification for Open Targets datasource disease associations " +
                    "for bubbles and tree views",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<DiseaseAssociationAggregated>> getAllOpenTargetsDiseases(
            @RequestBody final AssociationSearchRequest request) throws ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.getAllOpenTargetsDiseases(request));
    }

    @GetMapping(value = "/target/opentargets/diseases/ontology")
    @ApiOperation(
            value = "Returns all diseases with parents",
            notes = "Returns all diseases with parents",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<BareDisease>> getDiseasesTree() throws IOException {
        return Result.success(launchIdentificationSecurityService.getDiseasesTree());
    }

    @PutMapping(value = "/target/import/opentargets")
    @ApiOperation(
            value = "Imports data from Open Targets datasource",
            notes = "Imports data from Open Targets datasource",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> importOpenTargetsData(@RequestParam final String path) throws IOException, ParseException {
        launchIdentificationSecurityService.importOpenTargetsData(path);
        return Result.success(null);
    }

    @PutMapping(value = "/target/import/dgidb")
    @ApiOperation(
            value = "Imports data from DGIdb datasource",
            notes = "Imports data from DGIdb datasource",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> importDGIdbData(@RequestParam final String path) throws IOException, ParseException {
        launchIdentificationSecurityService.importDGIdbData(path);
        return Result.success(null);
    }

    @PutMapping(value = "/target/import/pharmGKB")
    @ApiOperation(
            value = "Imports data from PharmGKB datasource",
            notes = "Imports data from PharmGKB datasource",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> importPharmGKBData(
            @RequestParam final String genePath,
            @RequestParam final String drugPath,
            @RequestParam final String drugAssociationPath,
            @RequestParam final String diseaseAssociationPath) throws IOException, ParseException {
        launchIdentificationSecurityService.importPharmGKBData(genePath, drugPath,
                drugAssociationPath, diseaseAssociationPath);
        return Result.success(null);
    }

    @GetMapping(value = "/target/pharmGKB/drugs/fieldValues")
    @ApiOperation(
            value = "Returns filed values for PharmGKB drugs data",
            notes = "Returns filed values for PharmGKB drugs data",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<PharmGKBDrugFieldValues> getPharmGKBDrugFieldValues(
            @RequestParam final List<String> geneIds) throws IOException, ParseException {
        return Result.success(launchIdentificationSecurityService.getPharmGKBDrugFieldValues(geneIds));
    }

    @GetMapping(value = "/target/dgidb/drugs/fieldValues")
    @ApiOperation(
            value = "Returns filed values for DGIDB drugs data",
            notes = "Returns filed values for DGIDB drugs data",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<DGIDBDrugFieldValues> getDGIDBDrugFieldValues(
            @RequestParam final List<String> geneIds) throws IOException, ParseException {
        return Result.success(launchIdentificationSecurityService.getDGIDBDrugFieldValues(geneIds));
    }

    @GetMapping(value = "/target/opentargets/drugs/fieldValues")
    @ApiOperation(
            value = "Returns filed values for Open Targets drugs data",
            notes = "Returns filed values for Open Targets drugs data",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<DrugFieldValues> getDrugFieldValues(@RequestParam final List<String> geneIds)
            throws IOException, ParseException {
        return Result.success(launchIdentificationSecurityService.getDrugFieldValues(geneIds));
    }

    @PostMapping(value = "/target/publications")
    @ApiOperation(
            value = "Returns publications for specified gene ids",
            notes = "Returns publications for specified gene ids",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<NCBISummaryVO>> getPublications(@RequestBody final PublicationSearchRequest request) {
        return Result.success(launchIdentificationSecurityService.getPublications(request));
    }

    @PostMapping(value = "/target/abstracts")
    @ApiOperation(
            value = "Returns merged abstracts for specified gene ids",
            notes = "Returns merged abstracts for specified gene ids",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<String> getAbstracts(@RequestBody final PublicationSearchRequest request) {
        return Result.success(launchIdentificationSecurityService.getArticleAbstracts(request));
    }

    @GetMapping(value = "/target/sequences")
    @ApiOperation(
            value = "Returns data for Gene Sequences block",
            notes = "Returns data for Gene Sequences block",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<GeneSequences>> getGeneSequences(@RequestParam final List<String> geneIds)
            throws IOException, ParseException {
        return Result.success(launchIdentificationSecurityService.getGeneSequences(geneIds));
    }

    @GetMapping(value = "/target/sequences/table")
    @ApiOperation(
            value = "Returns data for Gene Sequences block as a table",
            notes = "Returns data for Gene Sequences block as a table",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<GeneRefSection>> getGeneSequencesTable(@RequestParam final List<String> geneIds,
                                                              @RequestParam final Boolean getComments)
            throws IOException, ParseException, ExternalDbUnavailableException {
        return Result.success(launchIdentificationSecurityService.getGeneSequencesTable(geneIds, getComments));
    }

    @PostMapping(value = "/target/structures")
    @ApiOperation(
            value = "Loads structures entities from RCSB PDB",
            notes = "Loads structures entities from RCSB PDB. Available field names for sorting: ENTRY_ID, RESOLUTION.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<Structure>> getStructures(@RequestBody final StructuresSearchRequest request) {
        return Result.success(launchIdentificationSecurityService.getStructures(request));
    }

    @GetMapping(value = "/target/export")
    @ApiOperation(
            value = "Exports data to CSV/TSV file",
            notes = "Exports data to CSV/TSV file",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public void export(@RequestParam final List<String> genesOfInterest,
                       @RequestParam final List<String> translationalGenes,
                       @RequestParam final FileFormat format,
                       @RequestParam final TargetExportTable source,
                       @RequestParam final boolean includeHeader,
                       HttpServletResponse response)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final byte[] bytes = exportSecurityService.export(genesOfInterest, translationalGenes,
                format, source, includeHeader);
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }

    @GetMapping(value = "/target/export/{geneId}")
    @ApiOperation(
            value = "Exports data to CSV/TSV file",
            notes = "Exports data to CSV/TSV file",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public void export(@PathVariable final String geneId,
                       @RequestParam final FileFormat format,
                       @RequestParam final TargetExportTable source,
                       @RequestParam final boolean includeHeader,
                       HttpServletResponse response)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final byte[] bytes = exportSecurityService.export(geneId, source, format, includeHeader);
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }

    @GetMapping(value = "/target/report")
    @ApiOperation(
            value = "Exports data to Excel file",
            notes = "Exports data to Excel file",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public void report(@RequestParam final List<String> genesOfInterest,
                       @RequestParam final List<String> translationalGenes,
                       HttpServletResponse response)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final InputStream inputStream = exportSecurityService.report(genesOfInterest, translationalGenes);
        writeStreamToResponse(response, inputStream, "Target_Identification_Report.xlsx");
    }

    @GetMapping(value = "/target/report/{geneId}")
    @ApiOperation(
            value = "Exports data to Excel file",
            notes = "Exports data to Excel file",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public void report(@PathVariable final String geneId, HttpServletResponse response)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final InputStream inputStream = exportSecurityService.report(geneId);
        writeStreamToResponse(response, inputStream, "Target_Identification_Report.xlsx");
    }

    @GetMapping(value = "/target/html")
    @ApiOperation(
            value = "Downloads target identification html export",
            notes = "Downloads target identification html export",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public void html(@RequestParam final List<String> genesOfInterest,
                     @RequestParam final List<String> translationalGenes,
                     @RequestParam final long targetId,
                     HttpServletResponse response)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final InputStream inputStream = exportSecurityService.html(genesOfInterest, translationalGenes, targetId);
        writeStreamToResponse(response, inputStream, "Target_Identification.html");
    }

    @GetMapping(value = "/target/html/{geneId}")
    @ApiOperation(
            value = "Downloads target identification html export",
            notes = "Downloads target identification html export",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public void html(@PathVariable final String geneId, HttpServletResponse response)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final InputStream inputStream = exportSecurityService.html(geneId);
        writeStreamToResponse(response, inputStream, "Target_Identification.html");
    }

    @GetMapping(value = "/target/genes/{prefix}")
    @ApiOperation(
            value = "Searched genes by specified prefix",
            notes = "Searched genes by specified prefix",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<GeneInfo>> report(@PathVariable final String prefix)
            throws IOException, ParseException, ExternalDbUnavailableException {
        return Result.success(launchIdentificationSecurityService.getGenes(prefix));
    }

    @GetMapping(value = "/target/drugs")
    @ApiOperation(
            value = "Returns drugs list for target identification.",
            notes = "Returns drugs list for target identification.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<String>> getDrugs(@RequestParam final List<String> geneIds)
            throws IOException, ParseException, ExternalDbUnavailableException {
        return Result.success(launchIdentificationSecurityService.getDrugs(geneIds));
    }
}
