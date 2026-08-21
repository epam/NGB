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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
@Tag(name = "lineage", description = "Strain Lineage Tree files Management")
@RequiredArgsConstructor
public class LineageTreeController extends AbstractRESTController {

    private final LineageTreeSecurityService lineageTreeSecurityService;

    @GetMapping(value = "/lineage/trees/{referenceId}")
    @Operation(
            summary = "Returns lineage trees by given parameters",
            description = "Returns lineage trees by given parameters")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<List<LineageTree>> loadLineageTrees(@PathVariable final Long referenceId,
                                                      @RequestParam(required = false) final Long lineageTreeId,
                                                      @RequestParam(required = false) final Long projectId) {
        return Result.success(lineageTreeSecurityService.loadLineageTrees(referenceId, lineageTreeId, projectId));
    }

    @GetMapping(value = "/lineage/tree/{lineageTreeId}")
    @Operation(
            summary = "Returns a lineage tree by id",
            description = "Returns a lineage tree by id")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<LineageTree> loadLineageTree(@PathVariable final Long lineageTreeId,
                                               @RequestParam(required = false) final Long projectId) {
        return Result.success(lineageTreeSecurityService.loadLineageTree(lineageTreeId, projectId));
    }

    @GetMapping(value = "/lineage/trees/all")
    @Operation(
            summary = "Returns all lineage trees",
            description = "Returns all lineage trees")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<List<LineageTree>> loadAllLineageTrees() {
        return Result.success(lineageTreeSecurityService.loadAllLineageTrees());
    }

    @PostMapping(value = "/lineage/tree")
    @Operation(
            summary = "Registers new lineage tree",
            description = "Registers new lineage tree")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<LineageTree> createLineageTree(@RequestBody final LineageTreeRegistrationRequest request)
            throws IOException {
        return Result.success(lineageTreeSecurityService.createLineageTree(request));
    }

    @DeleteMapping(value = "/lineage/tree/{lineageTreeId}")
    @Operation(
            summary = "Deletes a lineage tree, specified by id",
            description = "Deletes a lineage tree, specified by id")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> deleteLineageTree(@PathVariable final long lineageTreeId) throws IOException {
        lineageTreeSecurityService.deleteLineageTree(lineageTreeId);
        return Result.success(null);
    }
}
