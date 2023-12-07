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
import com.epam.catgenome.entity.target.TargetIdentification;
import com.epam.catgenome.entity.target.IdentificationQueryParams;
import com.epam.catgenome.manager.target.TargetIdentificationSecurityService;
import com.epam.catgenome.util.db.Page;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Api(value = "target-identification", description = "Target Identification Management")
@RequiredArgsConstructor
public class TargetIdentificationController extends AbstractRESTController {

    private final TargetIdentificationSecurityService identificationSecurityService;

    @GetMapping(value = "/identification/{id}")
    @ApiOperation(
            value = "Returns an identification by given id",
            notes = "Returns an identification by given id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<TargetIdentification> load(@PathVariable final long id) {
        return Result.success(identificationSecurityService.load(id));
    }

    @PostMapping(value = "/identifications/filter")
    @ApiOperation(
            value = "Filters identifications",
            notes = "Filters identifications. Result can be sorted by created_date, name, and owner fields.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Page<TargetIdentification>> load(@RequestBody final IdentificationQueryParams params) {
        return Result.success(identificationSecurityService.loadTargets(params));
    }

    @GetMapping(value = "/identifications")
    @ApiOperation(
            value = "Returns all identifications",
            notes = "Returns all identifications",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<TargetIdentification>> load() {
        return Result.success(identificationSecurityService.load());
    }

    @GetMapping(value = "/identifications/{targetId}")
    @ApiOperation(
            value = "Returns identifications by target id",
            notes = "Returns identifications by target id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<TargetIdentification>> loadTargetIdentifications(@PathVariable final long targetId) {
        return Result.success(identificationSecurityService.loadTargetIdentifications(targetId));
    }

    @PostMapping(value = "/identification")
    @ApiOperation(
            value = "Registers new identification",
            notes = "Registers new identification",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<TargetIdentification> createTarget(@RequestBody final TargetIdentification identification) {
        return Result.success(identificationSecurityService.create(identification));
    }

    @PutMapping(value = "/identification")
    @ApiOperation(
            value = "Updates identification",
            notes = "Updates identification",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<TargetIdentification> updateTarget(@RequestBody final TargetIdentification identification) {
        return Result.success(identificationSecurityService.update(identification));
    }

    @DeleteMapping(value = "/identification/{id}")
    @ApiOperation(
            value = "Deletes an identification, specified by id",
            notes = "Deletes an identification, specified by id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> deleteTarget(@PathVariable final long id) {
        identificationSecurityService.delete(id);
        return Result.success(null);
    }
}
