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
import com.epam.catgenome.entity.heatmap.HeatmapAnnotationType;
import com.epam.catgenome.entity.heatmap.HeatmapTree;
import com.epam.catgenome.manager.heatmap.HeatmapSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@Tag(name = "heatmap", description = "Heatmap files Management")
@RequiredArgsConstructor
public class HeatmapController extends AbstractRESTController {

    private final HeatmapSecurityService heatmapSecurityService;

    @GetMapping(value = "/heatmap/{heatmapId}")
    @Operation(
            summary = "Returns a heatmap by given id",
        description = "Returns a heatmap by given id")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<Heatmap> loadHeatmap(@PathVariable final long heatmapId,
                                       @RequestParam(required = false) final Long projectId) {
        return Result.success(heatmapSecurityService.loadHeatmap(heatmapId, projectId));
    }

    @GetMapping(value = "/heatmaps")
    @Operation(
            summary = "Returns all heatmaps",
        description = "Returns all heatmaps")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<List<Heatmap>> loadHeatmaps() {
        return Result.success(heatmapSecurityService.loadHeatmaps());
    }

    @GetMapping(value = "/heatmap/{heatmapId}/content")
    @Operation(
            summary = "Returns heatmap content",
        description = "Returns heatmap content")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<List<List<List<String>>>> getContent(@PathVariable final long heatmapId,
                                                         @RequestParam(required = false) final Long projectId)
            throws IOException {
        return Result.success(heatmapSecurityService.getContent(heatmapId, projectId));
    }

    @PutMapping(value = "/heatmap/{heatmapId}/label/annotation")
    @Operation(
            summary = "Updates heatmap annotation for labels",
        description = "Updates heatmap annotation for labels")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<Boolean> updateLabelAnnotation(
            @PathVariable final long heatmapId,
            @RequestParam(required = false) final String path,
            @RequestParam(required = false) final HeatmapAnnotationType rowAnnotationType,
            @RequestParam(required = false) final HeatmapAnnotationType columnAnnotationType) throws IOException {
        heatmapSecurityService.updateLabelAnnotation(heatmapId, path, rowAnnotationType, columnAnnotationType);
        return Result.success(null);
    }

    @PutMapping(value = "/heatmap/{heatmapId}/cell/annotation")
    @Operation(
            summary = "Updates heatmap annotation for cells",
        description = "Updates heatmap annotation for cells")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<Boolean> updateCellAnnotation(
            @PathVariable final long heatmapId,
            @RequestParam(required = false) final String path,
            @RequestParam(required = false) final HeatmapAnnotationType cellAnnotationType) throws IOException {
        heatmapSecurityService.updateCellAnnotation(heatmapId, path, cellAnnotationType);
        return Result.success(null);
    }

    @GetMapping(value = "/heatmap/{heatmapId}/tree")
    @Operation(
            summary = "Returns heatmap tree",
        description = "Returns heatmap tree")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<HeatmapTree> getTree(@PathVariable final long heatmapId,
                                       @RequestParam(required = false) final Long projectId) throws IOException {
        return Result.success(heatmapSecurityService.getTree(heatmapId, projectId));
    }

    @PutMapping(value = "/heatmap/{heatmapId}/row/tree")
    @Operation(
            summary = "Updates phylogenetic tree for heatmap rows",
        description = "Updates phylogenetic tree for heatmap rows")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<Boolean> updateRowTree(@PathVariable final long heatmapId,
                                         @RequestParam(required = false) final String path) throws IOException {
        heatmapSecurityService.updateRowTree(heatmapId, path);
        return Result.success(null);
    }

    @PutMapping(value = "/heatmap/{heatmapId}/column/tree")
    @Operation(
            summary = "Updates phylogenetic tree for heatmap columns",
        description = "Updates phylogenetic tree for heatmap columns")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<Boolean> updateColumnTree(@PathVariable final long heatmapId,
                                            @RequestParam(required = false) final String path) throws IOException {
        heatmapSecurityService.updateColumnTree(heatmapId, path);
        return Result.success(null);
    }

    @PostMapping(value = "/heatmap")
    @Operation(
            summary = "Registers new heatmap",
        description = "Registers new heatmap")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<Heatmap> createHeatmap(@RequestBody final HeatmapRegistrationRequest heatmap) throws IOException {
        return Result.success(heatmapSecurityService.createHeatmap(heatmap));
    }

    @DeleteMapping(value = "/heatmap/{heatmapId}")
    @Operation(
            summary = "Deletes a heatmap, specified by id",
        description = "Deletes a heatmap, specified by id")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<Boolean> deleteHeatmap(@PathVariable final long heatmapId) throws IOException {
        heatmapSecurityService.deleteHeatmap(heatmapId);
        return Result.success(null);
    }
}
