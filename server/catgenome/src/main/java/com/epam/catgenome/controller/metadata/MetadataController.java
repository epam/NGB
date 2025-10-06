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

package com.epam.catgenome.controller.metadata;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.entity.metadata.MetadataVO;
import com.epam.catgenome.manager.metadata.MetadataSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "metadata", description = "Metadata Management")
public class MetadataController extends AbstractRESTController {

    private final MetadataSecurityService metadataSecurityService;

    @PostMapping(value = "/metadata")
    @Operation(
            summary = "Creates or updates metadata",
        description = "Creates or updates metadata")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<MetadataVO> upsertMetadata(@RequestBody final MetadataVO metadataVO) {
        return Result.success(metadataSecurityService.upsert(metadataVO));
    }

    @GetMapping(value = "/metadata")
    @Operation(
            summary = "Loads metadata by entity ID and class",
        description = "Loads metadata by entity ID and class")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<MetadataVO> getMetadata(@RequestParam final Long id, @RequestParam final String entityClass) {
        return Result.success(metadataSecurityService.get(id, entityClass));
    }
}
