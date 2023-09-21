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
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.target.opentargets.DiseaseSecurityService;
import com.epam.catgenome.manager.index.SearchRequest;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@Api(value = "disease", description = "Disease Management")
@RequiredArgsConstructor
public class DiseaseController extends AbstractRESTController {

    private final DiseaseSecurityService diseaseSecurityService;

    @GetMapping(value = "/disease")
    @ApiOperation(
            value = "Searches diseases by name",
            notes = "Searches diseases by name",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Map<String, String>> search(@RequestParam final String name) throws IOException, ParseException {
        return Result.success(diseaseSecurityService.search(name));
    }

    @GetMapping(value = "/disease/{diseaseId}")
    @ApiOperation(
            value = "Returns a disease by given id",
            notes = "Returns a disease by given id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Disease> searchById(@PathVariable final String diseaseId) throws IOException, ParseException {
        return Result.success(diseaseSecurityService.searchById(diseaseId));
    }

    @PostMapping(value = "/disease/drugs/{diseaseId}")
    @ApiOperation(
            value = "Returns a disease drugs",
            notes = "Returns a disease drugs" +
                    "Available field names for sorting and filtering: GENE_ID, DRUG_NAME, DISEASE_NAME, DRUG_TYPE, " +
                    "MECHANISM_OF_ACTION, ACTION_TYPE, PHASE, STATUS, SOURCE.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<DrugAssociation>> searchDrugs(@RequestBody final SearchRequest request,
                                                             @PathVariable final String diseaseId)
            throws IOException, ParseException {
        return Result.success(diseaseSecurityService.searchDrugs(request, diseaseId));
    }

    @PostMapping(value = "/disease/targets/{diseaseId}")
    @ApiOperation(
            value = "Returns a disease targets",
            notes = "Returns a disease targets" +
                    "Available field names for sorting: GENE_ID, DISEASE_NAME, OVERALL_SCORE, " +
                    "GENETIC_ASSOCIATIONS_SCORE, SOMATIC_MUTATIONS_SCORE, DRUGS_SCORE, PATHWAYS_SCORE, " +
                    "TEXT_MINING_SCORE, RNA_EXPRESSION_SCORE, RNA_EXPRESSION_SCORE, ANIMAL_MODELS_SCORE.\n" +
                    "Available field names for filtering: GENE_ID, DISEASE_NAME.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<DiseaseAssociation>> searchTargets(@RequestBody final SearchRequest request,
                                                                  @PathVariable final String diseaseId)
            throws IOException, ParseException {
        return Result.success(diseaseSecurityService.searchTargets(request, diseaseId));
    }
}
