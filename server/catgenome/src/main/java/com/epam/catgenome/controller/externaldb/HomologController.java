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
package com.epam.catgenome.controller.externaldb;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.entity.externaldb.homolog.HomologGroup;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.homolog.HomologSearchRequest;
import com.epam.catgenome.manager.externaldb.homolog.HomologSecurityService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@Api(value = "homolog", description = "Homolog Data Management")
@RequiredArgsConstructor
public class HomologController extends AbstractRESTController {

    private final HomologSecurityService homologSecurityService;

    @PostMapping(value = "/homolog/search")
    @ApiOperation(
            value = "Searches homologs by gene ID",
            notes = "Searches homologs by gene ID",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<HomologGroup>> search(@RequestBody final HomologSearchRequest searchRequest)
            throws IOException, ParseException, ExternalDbUnavailableException {
        return Result.success(homologSecurityService.searchHomolog(searchRequest));
    }

    @GetMapping(value = "/homolog/search")
    @ApiOperation(
            value = "Searches homologs by gene IDs",
            notes = "Searches homologs by gene IDs",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<HomologGroup>> search(@RequestParam final List<String> geneIds)
            throws IOException, ParseException {
        return Result.success(homologSecurityService.searchHomolog(geneIds));
    }

    @PutMapping(value = "/homolog/import")
    @ApiOperation(
            value = "Imports Homolog data",
            notes = "Imports Homolog data",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> importHomologData(@RequestParam final String databaseName,
                                             @RequestParam final String databasePath)
            throws IOException, ParseException {
        homologSecurityService.importHomologData(databaseName, databasePath);
        return Result.success(null);
    }
}
