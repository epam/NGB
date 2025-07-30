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

package com.epam.catgenome.controller.patents;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.target.PatentsSearchRequest;
import com.epam.catgenome.entity.blast.BlastTask;
import com.epam.catgenome.entity.externaldb.patents.DrugPatent;
import com.epam.catgenome.entity.externaldb.patents.SequencePatent;
import com.epam.catgenome.entity.externaldb.patents.google.GooglePatent;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.patents.PatentsSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@Tag(name = "patents", description = "Patents Management")
@RequiredArgsConstructor
public class PatentsController extends AbstractRESTController {

    private final PatentsSecurityService ncbiPatentsSecurityService;

    @PostMapping(value = "/patents/proteins/ncbi")
    @Operation(
            summary = "Searches protein patents by name in NCBI database.",
        description = "Searches protein patents by name in NCBI database.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<SearchResult<SequencePatent>> getProteinPatentsNcbi(@RequestBody final PatentsSearchRequest request)
            throws ExternalDbUnavailableException {
        return Result.success(ncbiPatentsSecurityService.getProteinPatents(request));
    }

    @PostMapping(value = "/patents/proteins/google")
    @Operation(
            summary = "Searches protein patents by name using Google Patents.",
        description = "Searches protein patents by name using Google Patents.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<SearchResult<GooglePatent>> getProteinPatentsGoogle(
            @RequestBody final PatentsSearchRequest request) {
        return Result.success(ncbiPatentsSecurityService.getProteinPatentsGoogle(request));
    }

    @PostMapping(value = "/patents/drugs/ncbi")
    @Operation(
            summary = "Searches drug patents by name in NCBI database.",
        description = "Searches drug patents by name in NCBI database.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<SearchResult<DrugPatent>> getDrugPatents(@RequestBody final PatentsSearchRequest request)
            throws ExternalDbUnavailableException, IOException {
        return Result.success(ncbiPatentsSecurityService.getDrugPatents(request));
    }

    @GetMapping(value = "/patents/drugs/ncbi")
    @Operation(
            summary = "Searches drug patents by id in NCBI database.",
        description = "Searches drug patents by id in NCBI database.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<List<DrugPatent>> getDrugPatents(@RequestParam final String id)
            throws ExternalDbUnavailableException, IOException {
        return Result.success(ncbiPatentsSecurityService.getDrugPatents(id));
    }

    @GetMapping(value = "/patents/proteins")
    @Operation(
            summary = "Creates BLAST task to search protein patents by sequence.",
        description = "Creates BLAST task to search protein patents by sequence.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<BlastTask> getPatents(@RequestParam final String sequence) throws BlastRequestException {
        return Result.success(ncbiPatentsSecurityService.getPatents(sequence));
    }

    @GetMapping(value = "/patents/proteins/{targetId}")
    @Operation(
            summary = "Searches protein patents by sequence.",
        description = "Searches protein patents by sequence.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<BlastTask> getPatents(@PathVariable final Long targetId,
                                        @RequestParam final String sequenceId) {
        return Result.success(ncbiPatentsSecurityService.getPatents(targetId, sequenceId));
    }
}
