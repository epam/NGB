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
import com.epam.catgenome.entity.externaldb.opentarget.BareDisease;
import com.epam.catgenome.entity.externaldb.pharmgkb.PharmGKBDisease;
import com.epam.catgenome.manager.externaldb.AssociationSearchRequest;
import com.epam.catgenome.manager.externaldb.dgidb.DGIDBDrugField;
import com.epam.catgenome.manager.externaldb.opentarget.DiseaseSearchRequest;
import com.epam.catgenome.manager.externaldb.opentarget.DrugField;
import com.epam.catgenome.manager.externaldb.opentarget.DrugSearchRequest;
import com.epam.catgenome.entity.externaldb.dgidb.DGIDBDrugAssociation;
import com.epam.catgenome.entity.externaldb.opentarget.DiseaseAssociationAggregated;
import com.epam.catgenome.entity.externaldb.opentarget.DrugAssociation;
import com.epam.catgenome.manager.externaldb.dgidb.DGIDBDrugSearchRequest;
import com.epam.catgenome.entity.externaldb.pharmgkb.PharmGKBDrug;
import com.epam.catgenome.entity.target.IdentificationRequest;
import com.epam.catgenome.entity.target.IdentificationResult;
import com.epam.catgenome.entity.target.Target;
import com.epam.catgenome.entity.target.TargetQueryParams;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.pharmgkb.PharmGKBDiseaseSearchRequest;
import com.epam.catgenome.manager.externaldb.pharmgkb.PharmGKBDrugField;
import com.epam.catgenome.manager.externaldb.pharmgkb.PharmGKBDrugSearchRequest;
import com.epam.catgenome.manager.target.TargetField;
import com.epam.catgenome.manager.target.TargetIdentificationSecurityService;
import com.epam.catgenome.manager.target.TargetSecurityService;
import com.epam.catgenome.util.db.Page;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@Api(value = "target", description = "Target Management")
@RequiredArgsConstructor
public class TargetController extends AbstractRESTController {

    private final TargetSecurityService targetSecurityService;
    private final TargetIdentificationSecurityService targetIdentificationSecurityService;

    @GetMapping(value = "/target/{targetId}")
    @ApiOperation(
            value = "Returns a target by given id",
            notes = "Returns a target by given id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Target> loadTarget(@PathVariable final long targetId) {
        return Result.success(targetSecurityService.loadTarget(targetId));
    }

    @PostMapping(value = "/target/filter")
    @ApiOperation(
            value = "Filters targets",
            notes = "Filters targets. Result can be sorted by target_name field.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Page<Target>> loadTarget(@RequestBody final TargetQueryParams queryParameters) {
        return Result.success(targetSecurityService.loadTargets(queryParameters));
    }

    @PostMapping(value = "/target")
    @ApiOperation(
            value = "Registers new target",
            notes = "Registers new target",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Target> createTarget(@RequestBody final Target target) {
        return Result.success(targetSecurityService.createTarget(target));
    }

    @PutMapping(value = "/target")
    @ApiOperation(
            value = "Updates target",
            notes = "Updates target",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Target> updateTarget(@RequestBody final Target target) {
        return Result.success(targetSecurityService.updateTarget(target));
    }

    @DeleteMapping(value = "/target/{targetId}")
    @ApiOperation(
            value = "Deletes a target, specified by id",
            notes = "Deletes a target, specified by id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> deleteTarget(@PathVariable final long targetId) {
        targetSecurityService.deleteTarget(targetId);
        return Result.success(null);
    }

    @GetMapping(value = "/target/fieldValues")
    @ApiOperation(
            value = "Returns field values for target filter",
            notes = "Returns field values for target filter",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<String>> loadFieldValues(@RequestParam final TargetField field) {
        return Result.success(targetSecurityService.loadFieldValues(field));
    }

    @PostMapping(value = "/target/identification")
    @ApiOperation(
            value = "Launches Target Identification",
            notes = "Launches Target Identification",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<IdentificationResult> launchIdentification(@RequestBody final IdentificationRequest request)
            throws ExternalDbUnavailableException, ParseException, IOException {
        return Result.success(targetIdentificationSecurityService.launchIdentification(request));
    }

    @PostMapping(value = "/target/dgidb/drugs")
    @ApiOperation(
            value = "Launches Identification for dgidb datasource drug associations",
            notes = "Launches Identification for dgidb datasource drug associations",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<DGIDBDrugAssociation>> getDGIDbDrugs(
            @RequestBody final DGIDBDrugSearchRequest request) throws
            ParseException, IOException, ExternalDbUnavailableException {
        return Result.success(targetIdentificationSecurityService.getDGIDbDrugs(request));
    }

    @PostMapping(value = "/target/pharmgkb/drugs")
    @ApiOperation(
            value = "Launches Identification for PharmGKB datasource drug associations",
            notes = "Launches Identification for PharmGKB datasource drug associations",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<PharmGKBDrug>> getPharmGKBDrugs(
            @RequestBody final PharmGKBDrugSearchRequest request)
            throws ParseException, IOException {
        return Result.success(targetIdentificationSecurityService.getPharmGKBDrugs(request));
    }

    @PostMapping(value = "/target/pharmgkb/diseases")
    @ApiOperation(
            value = "Launches Identification for PharmGKB datasource disease associations",
            notes = "Launches Identification for PharmGKB datasource disease associations",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<PharmGKBDisease>> getPharmGKBDiseases(
            @RequestBody final PharmGKBDiseaseSearchRequest request)
            throws ParseException, IOException {
        return Result.success(targetIdentificationSecurityService.getPharmGKBDiseases(request));
    }

    @PostMapping(value = "/target/opentargets/drugs")
    @ApiOperation(
            value = "Launches Identification for Open Targets datasource drug associations",
            notes = "Launches Identification for Open Targets datasource drug associations",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<DrugAssociation>> getOpenTargetsDrugs(
            @RequestBody final DrugSearchRequest request) throws ParseException, IOException {
        return Result.success(targetIdentificationSecurityService.getOpenTargetsDrugs(request));
    }

    @PostMapping(value = "/target/opentargets/diseases")
    @ApiOperation(
            value = "Launches Identification for Open Targets datasource disease associations",
            notes = "Launches Identification for Open Targets datasource disease associations for table view",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<DiseaseAssociationAggregated>> getOpenTargetsDiseases(
            @RequestBody final DiseaseSearchRequest request) throws ParseException, IOException {
        return Result.success(targetIdentificationSecurityService.getOpenTargetsDiseases(request));
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
        return Result.success(targetIdentificationSecurityService.getAllOpenTargetsDiseases(request));
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
        return Result.success(targetIdentificationSecurityService.getDiseasesTree());
    }

    @PutMapping(value = "/target/import/opentargets")
    @ApiOperation(
            value = "Imports data from Open Targets datasource",
            notes = "Imports data from Open Targets datasource",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> importOpenTargetsData(
            @RequestParam final String targetsPath,
            @RequestParam final String diseasesPath,
            @RequestParam final String drugsPath,
            @RequestParam final String overallScoresPath,
            @RequestParam final String scoresPath) throws IOException, ParseException {
        targetIdentificationSecurityService.importOpenTargetsData(targetsPath, diseasesPath, drugsPath,
                overallScoresPath, scoresPath);
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
    public Result<Boolean> importDGIdbData(@RequestParam final String path) throws IOException {
        targetIdentificationSecurityService.importDGIdbData(path);
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
            @RequestParam final String drugAssociationPath) throws IOException, ParseException {
        targetIdentificationSecurityService.importPharmGKBData(genePath, drugPath, drugAssociationPath);
        return Result.success(null);
    }

    @GetMapping(value = "/target/pharmGKB/drugs/fieldValues")
    @ApiOperation(
            value = "Returns all values for specified PharmGKB drugs data field",
            notes = "Returns all values for specified PharmGKB drugs data field",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<String>> getPharmGKBDrugsFieldValues(
            @RequestParam final PharmGKBDrugField field) throws IOException {
        return Result.success(targetIdentificationSecurityService.getPharmGKBDrugsFieldValues(field));
    }

    @GetMapping(value = "/target/dgidb/drugs/fieldValues")
    @ApiOperation(
            value = "Returns all values for specified DGIDB drugs data field",
            notes = "Returns all values for specified DGIDB drugs data field",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<String>> getDGIDBDrugsFieldValues(
            @RequestParam final DGIDBDrugField field) throws IOException {
        return Result.success(targetIdentificationSecurityService.getDGIDBDrugsFieldValues(field));
    }

    @GetMapping(value = "/target/opentargets/drugs/fieldValues")
    @ApiOperation(
            value = "Returns all values for specified Open Targets drugs data field",
            notes = "Returns all values for specified Open Targets drugs data field",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<String>> getDrugsFieldValues(
            @RequestParam final DrugField field) throws IOException {
        return Result.success(targetIdentificationSecurityService.getDrugsFieldValues(field));
    }
}
