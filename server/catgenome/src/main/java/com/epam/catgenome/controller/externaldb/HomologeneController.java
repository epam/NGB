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
import com.epam.catgenome.entity.externaldb.homologene.HomologeneEntry;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.homologene.HomologeneSecurityService;
import com.epam.catgenome.manager.externaldb.homologene.HomologeneSearchRequest;
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
@Api(value = "homologene", description = "Homologene Data Management")
@RequiredArgsConstructor
public class HomologeneController extends AbstractRESTController {

    private final HomologeneSecurityService homologeneSecurityService;

    @PostMapping(value = "/homologene/search")
    @ApiOperation(
            value = "Returns list of Homologenes",
            notes = "Returns list of Homologenes",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<HomologeneEntry>> search(
            @RequestBody final HomologeneSearchRequest query)
            throws IOException, ParseException {
        return Result.success(homologeneSecurityService.searchHomologenes(query));
    }

    @GetMapping(value = "/homologene/search")
    @ApiOperation(
            value = "Returns list of Homologenes by gene ids",
            notes = "Returns list of Homologenes by gene ids",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<HomologeneEntry>> search(@RequestParam final List<String> geneIds) throws IOException {
        return Result.success(homologeneSecurityService.searchHomologenes(geneIds));
    }

    @PutMapping(value = "/homologene/import")
    @ApiOperation(
            value = "Creates Homologene Lucene Index from Homologene file",
            notes = "Creates Homologene Lucene Index from Homologene file",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> importHomologeneDatabase(@RequestParam final String databasePath)
            throws IOException, ParseException {
        homologeneSecurityService.importHomologeneDatabase(databasePath);
        return Result.success(null);
    }
}
