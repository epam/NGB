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

package com.epam.catgenome.controller.heatmap;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.registration.HeatmapRegistrationRequest;
import com.epam.catgenome.entity.heatmap.Heatmap;
import com.epam.catgenome.entity.heatmap.HeatmapTree;
import com.epam.catgenome.manager.heatmap.HeatmapSecurityService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@Api(value = "heatmap", description = "Heatmap files Management")
@RequiredArgsConstructor
public class HeatmapController extends AbstractRESTController {

    private final HeatmapSecurityService heatmapSecurityService;

    @GetMapping(value = "/heatmap/{heatmapId}")
    @ApiOperation(
            value = "Returns a heatmap by given id",
            notes = "Returns a heatmap by given id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Heatmap> loadHeatmap(@PathVariable final long heatmapId,
                                       @RequestParam(required = false) final Long projectId) {
        return Result.success(heatmapSecurityService.loadHeatmap(heatmapId, projectId));
    }

    @GetMapping(value = "/heatmaps")
    @ApiOperation(
            value = "Returns all heatmaps",
            notes = "Returns all heatmaps",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<Heatmap>> loadHeatmaps() {
        return Result.success(heatmapSecurityService.loadHeatmaps());
    }

    @GetMapping(value = "/heatmap/{heatmapId}/content")
    @ApiOperation(
            value = "Returns heatmap content",
            notes = "Returns heatmap content",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<List<Map<?, String>>>> getContent(@PathVariable final long heatmapId,
                                                         @RequestParam(required = false) final Long projectId)
            throws IOException {
        return Result.success(heatmapSecurityService.getContent(heatmapId, projectId));
    }

    @GetMapping(value = "/heatmap/{heatmapId}/label/annotation")
    @ApiOperation(
            value = "Returns heatmap annotation for labels",
            notes = "Returns heatmap annotation for labels",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Map<String, String>> getLabelAnnotation(@PathVariable final long heatmapId,
                                                          @RequestParam(required = false) final Long projectId)
            throws IOException {
        return Result.success(heatmapSecurityService.getLabelAnnotation(heatmapId, projectId));
    }

    @PutMapping(value = "/heatmap/{heatmapId}/label/annotation")
    @ApiOperation(
            value = "Updates heatmap annotation for labels",
            notes = "Updates heatmap annotation for labels",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> updateLabelAnnotation(@PathVariable final long heatmapId,
                                                 @RequestParam(required = false) final String path) throws IOException {
        heatmapSecurityService.updateLabelAnnotation(heatmapId, path);
        return Result.success(null);
    }

    @PutMapping(value = "/heatmap/{heatmapId}/cell/annotation")
    @ApiOperation(
            value = "Updates heatmap annotation for cells",
            notes = "Updates heatmap annotation for cells",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> updateCellAnnotation(@PathVariable final long heatmapId,
                                                @RequestParam(required = false) final String path) throws IOException {
        heatmapSecurityService.updateCellAnnotation(heatmapId, path);
        return Result.success(null);
    }

    @GetMapping(value = "/heatmap/{heatmapId}/tree")
    @ApiOperation(
            value = "Returns heatmap tree",
            notes = "Returns heatmap tree",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<HeatmapTree> getTree(@PathVariable final long heatmapId,
                                       @RequestParam(required = false) final Long projectId) {
        return Result.success(heatmapSecurityService.getTree(heatmapId, projectId));
    }

    @PostMapping(value = "/heatmap")
    @ApiOperation(
            value = "Registers new heatmap",
            notes = "Registers new heatmap",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Heatmap> createHeatmap(@RequestBody final HeatmapRegistrationRequest heatmap) throws IOException {
        return Result.success(heatmapSecurityService.createHeatmap(heatmap));
    }

    @DeleteMapping(value = "/heatmap/{heatmapId}")
    @ApiOperation(
            value = "Deletes a heatmap, specified by id",
            notes = "Deletes a heatmap, specified by id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> deleteHeatmap(@PathVariable final long heatmapId) throws IOException {
        heatmapSecurityService.deleteHeatmap(heatmapId);
        return Result.success(null);
    }
}
