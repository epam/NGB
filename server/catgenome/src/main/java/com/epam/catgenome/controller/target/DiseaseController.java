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
import com.epam.catgenome.entity.externaldb.target.opentargets.Disease;
import com.epam.catgenome.entity.externaldb.target.opentargets.DiseaseAssociation;
import com.epam.catgenome.entity.externaldb.target.opentargets.DrugAssociation;
import com.epam.catgenome.entity.target.DiseaseIdentificationResult;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.target.opentargets.DiseaseSecurityService;
import com.epam.catgenome.manager.externaldb.target.opentargets.DrugFieldValues;
import com.epam.catgenome.manager.index.SearchRequest;
import com.epam.catgenome.util.FileFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestController
@Tag(name = "disease", description = "Disease Management")
@RequiredArgsConstructor
public class DiseaseController extends AbstractRESTController {

    private final DiseaseSecurityService diseaseSecurityService;

    @GetMapping(value = "/disease")
    @Operation(
            summary = "Searches diseases by name",
        description = "Searches diseases by name")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<Map<String, String>> search(@RequestParam final String name) throws IOException, ParseException {
        return Result.success(diseaseSecurityService.search(name));
    }

    @GetMapping(value = "/disease/{diseaseId}")
    @Operation(
            summary = "Returns a disease by given id",
        description = "Returns a disease by given id")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<Disease> searchById(@PathVariable final String diseaseId) throws IOException, ParseException {
        return Result.success(diseaseSecurityService.searchById(diseaseId));
    }

    @GetMapping(value = "/disease/identification/{diseaseId}")
    @Operation(
            summary = "Launches Disease Identification",
        description = "Launches Disease Identification")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<DiseaseIdentificationResult> launchIdentification(@PathVariable final String diseaseId)
            throws IOException, ParseException {
        return Result.success(diseaseSecurityService.launchIdentification(diseaseId));
    }

    @PostMapping(value = "/disease/drugs/{diseaseId}")
    @Operation(
            summary = "Returns a disease drugs",
        description = "Returns a disease drugs" +
                    "Available field names for sorting and filtering: GENE_ID, GENE_SYMBOL, GENE_NAME, DRUG_NAME, " +
                    "DRUG_TYPE, MECHANISM_OF_ACTION, ACTION_TYPE, PHASE, STATUS, SOURCE.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<SearchResult<DrugAssociation>> searchDrugs(@RequestBody final SearchRequest request,
                                                             @PathVariable final String diseaseId)
            throws IOException, ParseException {
        return Result.success(diseaseSecurityService.searchDrugs(request, diseaseId));
    }

    @GetMapping(value = "/disease/drugs/fieldValues/{diseaseId}")
    @Operation(
            summary = "Returns filed values for Open Targets drugs data",
        description = "Returns filed values for Open Targets drugs data")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<DrugFieldValues> getDrugFieldValues(@PathVariable final String diseaseId)
            throws IOException, ParseException {
        return Result.success(diseaseSecurityService.getDrugFieldValues(diseaseId));
    }

    @PostMapping(value = "/disease/targets/{diseaseId}")
    @Operation(
            summary = "Returns a disease targets",
        description = "Returns a disease targets" +
                    "Available field names for sorting and filtering: GENE_ID, GENE_SYMBOL, GENE_NAME, " +
                    "OVERALL_SCORE, GENETIC_ASSOCIATIONS_SCORE, SOMATIC_MUTATIONS_SCORE, DRUGS_SCORE, " +
                    "PATHWAYS_SCORE, TEXT_MINING_SCORE, RNA_EXPRESSION_SCORE, RNA_EXPRESSION_SCORE, " +
                    "ANIMAL_MODELS_SCORE.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<SearchResult<DiseaseAssociation>> searchTargets(@RequestBody final SearchRequest request,
                                                                  @PathVariable final String diseaseId)
            throws IOException, ParseException {
        return Result.success(diseaseSecurityService.searchTargets(request, diseaseId));
    }

    @GetMapping(value = "/disease/drugs/export")
    @Operation(
            summary = "Exports drugs data to CSV/TSV file",
        description = "Exports drugs data to CSV/TSV file")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public void exportDrugs(@RequestParam final String diseaseId,
                            @RequestParam final FileFormat format,
                            @RequestParam final boolean includeHeader,
                            HttpServletResponse response) throws IOException, ParseException {
        final byte[] bytes = diseaseSecurityService.exportDrugs(diseaseId, format, includeHeader);
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }

    @GetMapping(value = "/disease/targets/export")
    @Operation(
            summary = "Exports targets data to CSV/TSV file",
        description = "Exports targets data to CSV/TSV file")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public void exportTargets(@RequestParam final String diseaseId,
                              @RequestParam final FileFormat format,
                              @RequestParam final boolean includeHeader,
                              HttpServletResponse response) throws IOException, ParseException {
        final byte[] bytes = diseaseSecurityService.exportTargets(diseaseId, format, includeHeader);
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }
}
