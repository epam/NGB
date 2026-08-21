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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "target-identification", description = "Target Identification Management")
@RequiredArgsConstructor
public class TargetIdentificationController extends AbstractRESTController {

    private final TargetIdentificationSecurityService identificationSecurityService;

    @GetMapping(value = "/identification/{id}")
    @Operation(
            summary = "Returns an identification by given id",
            description = "Returns an identification by given id")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<TargetIdentification> load(@PathVariable final long id) {
        return Result.success(identificationSecurityService.load(id));
    }

    @PostMapping(value = "/identifications/filter")
    @Operation(
            summary = "Filters identifications",
            description = "Filters identifications. Result can be sorted by created_date, name, and owner fields.")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<Page<TargetIdentification>> load(@RequestBody final IdentificationQueryParams params) {
        return Result.success(identificationSecurityService.loadTargets(params));
    }

    @GetMapping(value = "/identifications")
    @Operation(
            summary = "Returns all identifications",
            description = "Returns all identifications")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<List<TargetIdentification>> load() {
        return Result.success(identificationSecurityService.load());
    }

    @GetMapping(value = "/identifications/{targetId}")
    @Operation(
            summary = "Returns identifications by target id",
            description = "Returns identifications by target id")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<List<TargetIdentification>> loadTargetIdentifications(@PathVariable final long targetId) {
        return Result.success(identificationSecurityService.loadTargetIdentifications(targetId));
    }

    @PostMapping(value = "/identification")
    @Operation(
            summary = "Registers new identification",
            description = "Registers new identification")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<TargetIdentification> createTarget(@RequestBody final TargetIdentification identification) {
        return Result.success(identificationSecurityService.create(identification));
    }

    @PutMapping(value = "/identification")
    @Operation(
            summary = "Updates identification",
            description = "Updates identification")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<TargetIdentification> updateTarget(@RequestBody final TargetIdentification identification) {
        return Result.success(identificationSecurityService.update(identification));
    }

    @DeleteMapping(value = "/identification/{id}")
    @Operation(
            summary = "Deletes an identification, specified by id",
            description = "Deletes an identification, specified by id")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> deleteTarget(@PathVariable final long id) {
        identificationSecurityService.delete(id);
        return Result.success(null);
    }
}
