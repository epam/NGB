/*
 * MIT License
 *
 * Copyright (c) 2022 EPAM Systems
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

package com.epam.catgenome.controller.pathway;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.registration.PathwayRegistrationRequest;
import com.epam.catgenome.entity.pathway.Pathway;
import com.epam.catgenome.manager.pathway.PathwaySecurityService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@Api(value = "pathway", description = "Metabolic Pathways Management")
@RequiredArgsConstructor
public class PathwayController extends AbstractRESTController {

    private final PathwaySecurityService pathwaySecurityService;

    @GetMapping(value = "/pathway/{pathwayId}")
    @ApiOperation(
            value = "Returns a pathway by id",
            notes = "Returns a pathway by id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Pathway> loadPathway(@PathVariable final Long pathwayId,
                                             @RequestParam(required = false) final Long projectId) {
        return Result.success(pathwaySecurityService.loadPathway(pathwayId, projectId));
    }

    @GetMapping(value = "/pathways")
    @ApiOperation(
            value = "Returns all pathways",
            notes = "Returns all pathways",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<Pathway>> loadPathways() {
        return Result.success(pathwaySecurityService.loadPathways());
    }

    @PostMapping(value = "/pathway")
    @ApiOperation(
            value = "Registers new pathway",
            notes = "Registers new pathway",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Pathway> createPathway(@RequestBody final PathwayRegistrationRequest request)
            throws IOException, ParseException {
        return Result.success(pathwaySecurityService.createPathway(request));
    }

    @DeleteMapping(value = "/pathway/{pathwayId}")
    @ApiOperation(
            value = "Deletes a pathway, specified by id",
            notes = "Deletes a pathway, specified by id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> deletePathway(@PathVariable final long pathwayId) throws IOException {
        pathwaySecurityService.deletePathway(pathwayId);
        return Result.success(null);
    }
}
