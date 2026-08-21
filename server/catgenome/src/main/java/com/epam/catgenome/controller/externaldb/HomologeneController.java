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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "homologene", description = "Homologene Data Management")
@RequiredArgsConstructor
public class HomologeneController extends AbstractRESTController {

    private final HomologeneSecurityService homologeneSecurityService;

    @PostMapping(value = "/homologene/search")
    @Operation(
            summary = "Returns list of Homologenes",
            description = "Returns list of Homologenes")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<HomologeneEntry>> search(
            @RequestBody final HomologeneSearchRequest query)
            throws IOException, ParseException {
        return Result.success(homologeneSecurityService.searchHomologenes(query));
    }

    @GetMapping(value = "/homologene/search")
    @Operation(
            summary = "Returns list of Homologenes by gene ids",
            description = "Returns list of Homologenes by gene ids")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<Map<String, List<HomologeneEntry>>> search(@RequestParam final List<String> geneIds)
            throws IOException, ParseException {
        return Result.success(homologeneSecurityService.searchHomologenes(geneIds));
    }

    @PutMapping(value = "/homologene/import")
    @Operation(
            summary = "Creates Homologene Lucene Index from Homologene file",
            description = "Creates Homologene Lucene Index from Homologene file")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> importHomologeneDatabase(@RequestParam final String databasePath)
            throws IOException, ParseException {
        homologeneSecurityService.importHomologeneDatabase(databasePath);
        return Result.success(null);
    }
}
