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
import com.epam.catgenome.entity.externaldb.opentarget.AssociatedDiseaseAggregated;
import com.epam.catgenome.entity.externaldb.opentarget.AssociatedDrug;
import com.epam.catgenome.entity.target.IdentificationRequest;
import com.epam.catgenome.entity.target.IdentificationResult;
import com.epam.catgenome.entity.target.Target;
import com.epam.catgenome.entity.target.TargetQueryParams;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.opentarget.AssociatedDiseaseSecurityService;
import com.epam.catgenome.manager.externaldb.opentarget.AssociatedDrugSecurityService;
import com.epam.catgenome.manager.externaldb.opentarget.AssociationSearchRequest;
import com.epam.catgenome.manager.externaldb.opentarget.AssociationSource;
import com.epam.catgenome.manager.externaldb.opentarget.DiseaseSecurityService;
import com.epam.catgenome.manager.externaldb.opentarget.TargetDetailsSecurityService;
import com.epam.catgenome.manager.target.TargetField;
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
    private final TargetDetailsSecurityService targetDetailsSecurityService;
    private final DiseaseSecurityService diseaseSecurityService;
    private final AssociatedDiseaseSecurityService associationsSecurityService;
    private final AssociatedDrugSecurityService drugsSecurityService;

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
            value = "Launches Identification",
            notes = "Launches Identification",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<IdentificationResult> launchIdentification(@RequestBody final IdentificationRequest request)
            throws ExternalDbUnavailableException, ParseException, IOException {
        return Result.success(targetSecurityService.launchIdentification(request));
    }

    @PostMapping(value = "/target/associated/drugs")
    @ApiOperation(
            value = "Launches Identification for associated drugs",
            notes = "Launches Identification for associated drugs",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<AssociatedDrug>> searchAssociatedDrugs(
            @RequestBody final AssociationSearchRequest request) throws ParseException, IOException {
        return Result.success(drugsSecurityService.search(request));
    }

    @PostMapping(value = "/target/associated/diseases")
    @ApiOperation(
            value = "Launches Identification for associated diseases",
            notes = "Launches Identification for associated diseases",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<AssociatedDiseaseAggregated>> searchAssociatedDiseases(
            @RequestBody final AssociationSearchRequest request,
            @RequestParam final AssociationSource source) throws ParseException, IOException {
        return Result.success(associationsSecurityService.search(request));
    }

    @PutMapping(value = "/target/import/targets")
    @ApiOperation(
            value = "Import targets from Open Targets Datasource",
            notes = "Import targets from Open Targets Datasource",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> importTargets(@RequestParam final String path) throws IOException {
        targetDetailsSecurityService.importData(path);
        return Result.success(null);
    }

    @PutMapping(value = "/target/import/diseases")
    @ApiOperation(
            value = "Import diseases from Open Targets Datasource",
            notes = "Import diseases from Open Targets Datasource",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> importDiseases(@RequestParam final String path) throws IOException {
        diseaseSecurityService.importData(path);
        return Result.success(null);
    }

    @PutMapping(value = "/target/import/associations")
    @ApiOperation(
            value = "Import associations from Open Targets Datasource",
            notes = "Import associations from Open Targets Datasource",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> importAssociations(@RequestParam final String path,
                                              @RequestParam final String overallPath) throws IOException {
        associationsSecurityService.importData(path, overallPath);
        return Result.success(null);
    }

    @PutMapping(value = "/target/import/drugs")
    @ApiOperation(
            value = "Import drugs from Open Targets Datasource",
            notes = "Import drugs from Open Targets Datasource",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> importDrugs(@RequestParam final String path) throws IOException {
        drugsSecurityService.importData(path);
        return Result.success(null);
    }
}
