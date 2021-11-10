/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

package com.epam.catgenome.controller.lineage;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.registration.LineageTreeRegistrationRequest;
import com.epam.catgenome.entity.lineage.LineageTree;
import com.epam.catgenome.manager.lineage.LineageTreeSecurityService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@Api(value = "lineage", description = "Strain Lineage Tree files Management")
@RequiredArgsConstructor
public class LineageTreeController extends AbstractRESTController {

    private final LineageTreeSecurityService lineageTreeSecurityService;

    @GetMapping(value = "/lineage/trees/{referenceId}")
    @ApiOperation(
            value = "Returns lineage trees by given parameters",
            notes = "Returns lineage trees by given parameters",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<LineageTree>> loadLineageTrees(@PathVariable final Long referenceId,
                                                      @RequestParam(required = false) final Long lineageTreeId,
                                                      @RequestParam(required = false) final Long projectId) {
        return Result.success(lineageTreeSecurityService.loadLineageTrees(referenceId, lineageTreeId, projectId));
    }

    @GetMapping(value = "/lineage/tree/{lineageTreeId}")
    @ApiOperation(
            value = "Returns a lineage tree by id",
            notes = "Returns a lineage tree by id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<LineageTree> loadLineageTree(@PathVariable final Long lineageTreeId,
                                               @RequestParam(required = false) final Long projectId) {
        return Result.success(lineageTreeSecurityService.loadLineageTree(lineageTreeId, projectId));
    }

    @GetMapping(value = "/lineage/trees/all")
    @ApiOperation(
            value = "Returns all lineage trees",
            notes = "Returns all lineage trees",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<LineageTree>> loadAllLineageTrees() {
        return Result.success(lineageTreeSecurityService.loadAllLineageTrees());
    }

    @PostMapping(value = "/lineage/tree")
    @ApiOperation(
            value = "Registers new lineage tree",
            notes = "Registers new lineage tree",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<LineageTree> createLineageTree(@RequestBody final LineageTreeRegistrationRequest request)
            throws IOException {
        return Result.success(lineageTreeSecurityService.createLineageTree(request));
    }

    @DeleteMapping(value = "/lineage/tree/{lineageTreeId}")
    @ApiOperation(
            value = "Deletes a lineage tree, specified by id",
            notes = "Deletes a lineage tree, specified by id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> deleteLineageTree(@PathVariable final long lineageTreeId) throws IOException {
        lineageTreeSecurityService.deleteLineageTree(lineageTreeId);
        return Result.success(null);
    }
}
