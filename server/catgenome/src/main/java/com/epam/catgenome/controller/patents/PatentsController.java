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
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@Api(value = "patents", description = "Patents Management")
@RequiredArgsConstructor
public class PatentsController extends AbstractRESTController {

    private final PatentsSecurityService ncbiPatentsSecurityService;

    @PostMapping(value = "/patents/proteins/ncbi")
    @ApiOperation(
            value = "Searches protein patents by name in NCBI database.",
            notes = "Searches protein patents by name in NCBI database.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<SequencePatent>> getProteinPatentsNcbi(@RequestBody final PatentsSearchRequest request)
            throws ExternalDbUnavailableException {
        return Result.success(ncbiPatentsSecurityService.getProteinPatents(request));
    }

    @PostMapping(value = "/patents/proteins/google")
    @ApiOperation(
            value = "Searches protein patents by name using Google Patents.",
            notes = "Searches protein patents by name using Google Patents.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<GooglePatent>> getProteinPatentsGoogle(
            @RequestBody final PatentsSearchRequest request) {
        return Result.success(ncbiPatentsSecurityService.getProteinPatentsGoogle(request));
    }

    @PostMapping(value = "/patents/drugs/ncbi")
    @ApiOperation(
            value = "Searches drug patents by name in NCBI database.",
            notes = "Searches drug patents by name in NCBI database.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<DrugPatent>> getDrugPatents(@RequestBody final PatentsSearchRequest request)
            throws ExternalDbUnavailableException, IOException {
        return Result.success(ncbiPatentsSecurityService.getDrugPatents(request));
    }

    @GetMapping(value = "/patents/drugs/ncbi")
    @ApiOperation(
            value = "Searches drug patents by id in NCBI database.",
            notes = "Searches drug patents by id in NCBI database.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<DrugPatent>> getDrugPatents(@RequestParam final String id)
            throws ExternalDbUnavailableException, IOException {
        return Result.success(ncbiPatentsSecurityService.getDrugPatents(id));
    }

    @GetMapping(value = "/patents/proteins")
    @ApiOperation(
            value = "Creates BLAST task to search protein patents by sequence.",
            notes = "Creates BLAST task to search protein patents by sequence.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<BlastTask> getPatents(@RequestParam final String sequence) throws BlastRequestException {
        return Result.success(ncbiPatentsSecurityService.getPatents(sequence));
    }

    @GetMapping(value = "/patents/proteins/{targetId}")
    @ApiOperation(
            value = "Searches protein patents by sequence.",
            notes = "Searches protein patents by sequence.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<BlastTask> getPatents(@PathVariable final Long targetId,
                                        @RequestParam final String sequenceId) {
        return Result.success(ncbiPatentsSecurityService.getPatents(targetId, sequenceId));
    }
}
