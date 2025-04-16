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
import com.epam.catgenome.entity.externaldb.target.ttd.TTDDiseaseAssociation;
import com.epam.catgenome.entity.externaldb.target.ttd.TTDDrugAssociation;
import com.epam.catgenome.entity.target.*;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto.Structure;
import com.epam.catgenome.manager.externaldb.target.AssociationSearchRequest;
import com.epam.catgenome.manager.externaldb.target.dgidb.DGIDBDrugFieldValues;
import com.epam.catgenome.manager.externaldb.target.opentargets.DrugFieldValues;
import com.epam.catgenome.manager.externaldb.target.pharmgkb.PharmGKBDrugFieldValues;
import com.epam.catgenome.manager.externaldb.target.ttd.TTDDiseaseFieldValues;
import com.epam.catgenome.manager.externaldb.target.ttd.TTDDrugFieldValues;
import com.epam.catgenome.manager.target.LaunchIdentificationSecurityService;
import com.epam.catgenome.manager.target.export.TargetExportSecurityService;
import com.epam.catgenome.manager.target.export.TargetExportTable;
import com.epam.catgenome.util.FileFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@Tag(name = "target-identification", description = "Launch Target Identification Management")
@RequiredArgsConstructor
public class LaunchIdentificationController extends AbstractRESTController {

    private final LaunchIdentificationSecurityService launchIdentificationSecurityService;
    private final TargetExportSecurityService exportSecurityService;

    @PostMapping(value = "/target/identification")
    @Operation(
            summary = "Launches Target Identification",
        description = "Launches Target Identification")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<TargetIdentificationResult> launchIdentification(@RequestBody final IdentificationRequest request)
            throws ExternalDbUnavailableException, ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.launchIdentification(request));
    }

    @PostMapping(value = "/target/dgidb/drugs")
    @Operation(
            summary = "Launches Identification for dgidb datasource drug associations",
        description = "Launches Identification for dgidb datasource drug associations." +
                    "Available field names for sorting and filtering: GENE_ID, DRUG_NAME, DRUG_CLAIM_NAME, " +
                    "INTERACTION_TYPES, INTERACTION_CLAIM_SOURCE.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<SearchResult<DGIDBDrugAssociation>> getDGIDbDrugs(@RequestBody final AssociationSearchRequest request)
            throws ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.getDGIDbDrugs(request));
    }

    @PostMapping(value = "/target/ttd/drugs")
    @Operation(
            summary = "Launches Identification for TTD datasource target - drug associations",
        description = "Launches Identification for TTD datasource target - drug associations." +
                    "Available field names for sorting and filtering: TTD_TARGET, DRUG_NAME, COMPANY, " +
                    "TYPE, THERAPEUTIC_CLASS, INCHI, INCHI_KEY, CANONICAL_SMILES, STATUS, COMPOUND_CLASS. " +
                    "The following fields are optional: TTD_TARGET, COMPANY, TYPE, THERAPEUTIC_CLASS, " +
                    "STATUS, COMPOUND_CLASS.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<SearchResult<TTDDrugAssociation>> getTTDDrugs(@RequestBody final AssociationSearchRequest request)
            throws ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.getTTDDrugs(request));
    }

    @GetMapping(value = "/target/ttd/drugs/fieldValues")
    @Operation(
            summary = "Returns filed values for TTD drugs data",
        description = "Returns filed values for TTD drugs data")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<TTDDrugFieldValues> getTTDDrugFieldValues(@RequestParam(required = false) final Long targetId,
                                                            @RequestParam final List<String> geneIds)
            throws ParseException, IOException{
        return Result.success(launchIdentificationSecurityService.getTTDDrugFieldValues(targetId, geneIds));
    }

    @PostMapping(value = "/target/ttd/diseases")
    @Operation(
            summary = "Launches Identification for TTD datasource target - disease associations",
        description = "Launches Identification for TTD datasource target - disease associations." +
                    "Available field names for sorting and filtering: TTD_TARGET, DISEASE_NAME, CLINICAL_STATUS. " +
                    "The following fields are optional: TTD_TARGET, CLINICAL_STATUS.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<SearchResult<TTDDiseaseAssociation>> getTTDDiseases(
            @RequestBody final AssociationSearchRequest request) throws ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.getTTDDiseases(request));
    }

    @GetMapping(value = "/target/ttd/diseases/fieldValues")
    @Operation(
            summary = "Returns filed values for TTD diseases data",
        description = "Returns filed values for TTD diseases data")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<TTDDiseaseFieldValues> getTTDDiseaseFieldValues(@RequestParam(required = false) final Long targetId,
                                                                  @RequestParam final List<String> geneIds)
            throws ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.getTTDDiseaseFieldValues(targetId, geneIds));
    }

    @PostMapping(value = "/target/pharmgkb/drugs")
    @Operation(
            summary = "Launches Identification for PharmGKB datasource drug associations",
        description = "Launches Identification for PharmGKB datasource drug associations." +
            "Available field names for sorting and filtering: GENE_ID, DRUG_NAME, SOURCE.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<SearchResult<PharmGKBDrug>> getPharmGKBDrugs(@RequestBody final AssociationSearchRequest request)
            throws ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.getPharmGKBDrugs(request));
    }

    @PostMapping(value = "/target/pharmgkb/diseases")
    @Operation(
            summary = "Launches Identification for PharmGKB datasource disease associations",
        description = "Launches Identification for PharmGKB datasource disease associations." +
                    "Available field names for sorting and filtering: GENE_ID, DISEASE_NAME.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<SearchResult<PharmGKBDisease>> getPharmGKBDiseases(
            @RequestBody final AssociationSearchRequest request) throws ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.getPharmGKBDiseases(request));
    }

    @PostMapping(value = "/target/opentargets/drugs")
    @Operation(
            summary = "Launches Identification for Open Targets datasource drug associations",
        description = "Launches Identification for Open Targets datasource drug associations." +
                    "Available field names for sorting and filtering: GENE_ID, DRUG_NAME, DISEASE_NAME, DRUG_TYPE, " +
                    "MECHANISM_OF_ACTION, ACTION_TYPE, PHASE, STATUS, SOURCE.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<SearchResult<DrugAssociation>> getOpenTargetsDrugs(
            @RequestBody final AssociationSearchRequest request) throws ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.getOpenTargetsDrugs(request));
    }

    @PostMapping(value = "/target/opentargets/diseases")
    @Operation(
            summary = "Launches Identification for Open Targets datasource disease associations",
        description = "Launches Identification for Open Targets datasource disease associations." +
                    "Available field names for sorting and filtering: GENE_ID, DISEASE_NAME, OVERALL_SCORE, " +
                    "GENETIC_ASSOCIATIONS_SCORE, SOMATIC_MUTATIONS_SCORE, DRUGS_SCORE, PATHWAYS_SCORE, " +
                    "TEXT_MINING_SCORE, RNA_EXPRESSION_SCORE, RNA_EXPRESSION_SCORE, ANIMAL_MODELS_SCORE.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<SearchResult<DiseaseAssociationAggregated>> getOpenTargetsDiseases(
            @RequestBody final AssociationSearchRequest request) throws ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.getOpenTargetsDiseases(request));
    }

    @PostMapping(value = "/target/opentargets/diseases/all")
    @Operation(
            summary = "Launches Identification for Open Targets datasource disease associations",
        description = "Launches Identification for Open Targets datasource disease associations " +
                    "for bubbles and tree views")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<List<DiseaseAssociationAggregated>> getAllOpenTargetsDiseases(
            @RequestBody final AssociationSearchRequest request) throws ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.getAllOpenTargetsDiseases(request));
    }

    @GetMapping(value = "/target/opentargets/diseases/ontology")
    @Operation(
            summary = "Returns all diseases with parents",
        description = "Returns all diseases with parents")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<List<BareDisease>> getDiseasesTree() throws IOException {
        return Result.success(launchIdentificationSecurityService.getDiseasesTree());
    }

    @PutMapping(value = "/target/import/opentargets")
    @Operation(
            summary = "Imports data from Open Targets datasource",
        description = "Imports data from Open Targets datasource")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<Boolean> importOpenTargetsData(@RequestParam final String path) throws IOException, ParseException {
        launchIdentificationSecurityService.importOpenTargetsData(path);
        return Result.success(null);
    }

    @PutMapping(value = "/target/import/dgidb")
    @Operation(
            summary = "Imports data from DGIdb datasource",
        description = "Imports data from DGIdb datasource")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<Boolean> importDGIdbData(@RequestParam final String path) throws IOException, ParseException {
        launchIdentificationSecurityService.importDGIdbData(path);
        return Result.success(null);
    }

    @PutMapping(value = "/target/import/pharmGKB")
    @Operation(
            summary = "Imports data from PharmGKB datasource",
        description = "Imports data from PharmGKB datasource")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<Boolean> importPharmGKBData(
            @RequestParam final String genePath,
            @RequestParam final String drugPath,
            @RequestParam final String drugAssociationPath,
            @RequestParam final String diseaseAssociationPath) throws IOException, ParseException {
        launchIdentificationSecurityService.importPharmGKBData(genePath, drugPath,
                drugAssociationPath, diseaseAssociationPath);
        return Result.success(null);
    }

    @PutMapping(value = "/target/import/ttd")
    @Operation(
            summary = "Imports data from TTD datasource.",
        description = "Imports data from TTD datasource. Data can be found here: " +
                    "drugs: https://idrblab.net/ttd/sites/default/files/ttd_database/P1-02-TTD_drug_download.txt," +
                    "targets: https://idrblab.net/ttd/sites/default/files/ttd_database/P1-01-TTD_target_download.txt," +
                    "diseases: https://idrblab.net/ttd/sites/default/files/ttd_database/P1-06-Target_disease.txt")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<Boolean> importTTDData(
            @RequestParam final String drugsPath,
            @RequestParam final String targetsPath,
            @RequestParam final String diseasesPath) throws IOException, ParseException {
        launchIdentificationSecurityService.importTTDData(drugsPath, targetsPath, diseasesPath);
        return Result.success(null);
    }

    @GetMapping(value = "/target/pharmGKB/drugs/fieldValues")
    @Operation(
            summary = "Returns filed values for PharmGKB drugs data",
        description = "Returns filed values for PharmGKB drugs data")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<PharmGKBDrugFieldValues> getPharmGKBDrugFieldValues(
            @RequestParam(required = false) final Long targetId,
            @RequestParam final List<String> geneIds) throws IOException, ParseException {
        return Result.success(launchIdentificationSecurityService.getPharmGKBDrugFieldValues(targetId, geneIds));
    }

    @GetMapping(value = "/target/dgidb/drugs/fieldValues")
    @Operation(
            summary = "Returns filed values for DGIDB drugs data",
        description = "Returns filed values for DGIDB drugs data")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<DGIDBDrugFieldValues> getDGIDBDrugFieldValues(@RequestParam(required = false) final Long targetId,
                                                                @RequestParam final List<String> geneIds)
            throws IOException, ParseException {
        return Result.success(launchIdentificationSecurityService.getDGIDBDrugFieldValues(targetId, geneIds));
    }

    @GetMapping(value = "/target/opentargets/drugs/fieldValues")
    @Operation(
            summary = "Returns filed values for Open Targets drugs data",
        description = "Returns filed values for Open Targets drugs data")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<DrugFieldValues> getDrugFieldValues(@RequestParam(required = false) final Long targetId,
                                                      @RequestParam final List<String> geneIds)
            throws IOException, ParseException {
        return Result.success(launchIdentificationSecurityService.getDrugFieldValues(targetId, geneIds));
    }

    @PostMapping(value = "/target/publications")
    @Operation(
            summary = "Returns publications for specified gene ids",
        description = "Returns publications for specified gene ids")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<SearchResult<NCBISummaryVO>> getPublications(@RequestBody final PublicationSearchRequest request)
            throws ParseException, IOException, ExternalDbUnavailableException {
        return Result.success(launchIdentificationSecurityService.getPublications(request));
    }

    @PostMapping(value = "/target/abstracts")
    @Operation(
            summary = "Returns merged abstracts for specified gene ids",
        description = "Returns merged abstracts for specified gene ids")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<String> getAbstracts(@RequestBody final PublicationSearchRequest request)
            throws ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.getArticleAbstracts(request));
    }

    @GetMapping(value = "/target/sequences/table")
    @Operation(
            summary = "Returns data for Gene Sequences block as a table",
        description = "Returns data for Gene Sequences block as a table")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<List<GeneRefSection>> getGeneSequencesTable(
            @RequestParam(required = false) final Long targetId,
            @RequestParam final List<String> geneIds,
            @RequestParam final Boolean getComments,
            @RequestParam(required = false, defaultValue = "false") final Boolean includeLocal,
            @RequestParam(required = false, defaultValue = "false") final Boolean includeAdditionalGenes)
            throws IOException, ParseException, ExternalDbUnavailableException {
        return Result.success(launchIdentificationSecurityService.getGeneSequencesTable(targetId, geneIds, getComments,
                includeLocal, includeAdditionalGenes));
    }

    @PostMapping(value = "/target/structures")
    @Operation(
            summary = "Loads structures entities from RCSB PDB",
        description = "Loads structures entities from RCSB PDB. Available field names for sorting: ENTRY_ID, RESOLUTION.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<SearchResult<Structure>> getStructures(@RequestBody final StructuresSearchRequest request)
            throws ParseException, IOException {
        return Result.success(launchIdentificationSecurityService.getStructures(request));
    }

    @GetMapping(value = "/target/export")
    @Operation(
            summary = "Exports data to CSV/TSV file",
        description = "Exports data to CSV/TSV file")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public void export(@RequestParam(required = false) final Long targetId,
                       @RequestParam final List<String> genesOfInterest,
                       @RequestParam(required = false) final List<String> translationalGenes,
                       @RequestParam final FileFormat format,
                       @RequestParam final TargetExportTable source,
                       @RequestParam final boolean includeHeader,
                       HttpServletResponse response)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final byte[] bytes = exportSecurityService.export(targetId, genesOfInterest, translationalGenes,
                format, source, includeHeader);
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }

    @GetMapping(value = "/target/export/{geneId}")
    @Operation(
            summary = "Exports data to CSV/TSV file for single gene",
        description = "Exports data to CSV/TSV file for single gene")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
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
    @Operation(
            summary = "Exports data to Excel file",
        description = "Exports data to Excel file")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public void report(@RequestParam(required = false) final Long targetId,
                       @RequestParam final List<String> genesOfInterest,
                       @RequestParam(required = false) final List<String> translationalGenes,
                       HttpServletResponse response)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final InputStream inputStream = exportSecurityService.report(targetId, genesOfInterest, translationalGenes);
        writeStreamToResponse(response, inputStream, "Target_Identification_Report.xlsx");
    }

    @GetMapping(value = "/target/report/{geneId}")
    @Operation(
            summary = "Exports data to Excel file for single gene",
        description = "Exports data to Excel file for single gene")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public void report(@PathVariable final String geneId, HttpServletResponse response)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final InputStream inputStream = exportSecurityService.report(geneId);
        writeStreamToResponse(response, inputStream, "Target_Identification_Report.xlsx");
    }

    @GetMapping(value = "/target/html")
    @Operation(
            summary = "Downloads target identification html export",
        description = "Downloads target identification html export")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public void html(@RequestParam final List<String> genesOfInterest,
                     @RequestParam(required = false) final List<String> translationalGenes,
                     @RequestParam final long targetId,
                     HttpServletResponse response)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final InputStream inputStream = exportSecurityService.html(genesOfInterest, translationalGenes, targetId);
        writeStreamToResponse(response, inputStream, "Target_Identification.html");
    }

    @GetMapping(value = "/target/html/{geneId}")
    @Operation(
            summary = "Downloads target identification html export for single gene",
        description = "Downloads target identification html export for single gene")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public void html(@PathVariable final String geneId, HttpServletResponse response)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final InputStream inputStream = exportSecurityService.html(geneId);
        writeStreamToResponse(response, inputStream, "Target_Identification.html");
    }

    @GetMapping(value = "/target/genes/{prefix}")
    @Operation(
            summary = "Searched genes by specified prefix",
        description = "Searched genes by specified prefix")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<List<GeneInfo>> getGenes(@PathVariable final String prefix)
            throws IOException, ParseException {
        return Result.success(launchIdentificationSecurityService.getGenes(prefix));
    }

    @GetMapping(value = "/target/drugs")
    @Operation(
            summary = "Returns drugs list for target identification.",
        description = "Returns drugs list for target identification.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<List<String>> getDrugs(@RequestParam(required = false) final Long targetId,
                                         @RequestParam final List<String> geneIds)
            throws IOException, ParseException {
        return Result.success(launchIdentificationSecurityService.getDrugs(targetId, geneIds));
    }
}
