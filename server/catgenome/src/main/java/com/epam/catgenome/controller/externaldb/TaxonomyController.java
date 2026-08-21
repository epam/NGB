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
import com.epam.catgenome.manager.externaldb.taxonomy.TaxonomySecurityService;
import com.epam.catgenome.manager.externaldb.taxonomy.Taxonomy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@Tag(name = "taxonomy", description = "Taxonomy Data Management")
@RequiredArgsConstructor
public class TaxonomyController extends AbstractRESTController {

    private final TaxonomySecurityService taxonomySecurityService;

    @GetMapping(value = "/taxonomies/{term}")
    @Operation(
            summary = "Returns list of Organisms by term",
            description = "Returns list of Organisms by term")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<List<Taxonomy>> loadTaxonomies(@PathVariable final String term)
            throws IOException, ParseException {
        return Result.success(taxonomySecurityService.searchOrganisms(term));
    }

    @GetMapping(value = "/taxonomy/{taxId}")
    @Operation(
            summary = "Returns Organism by taxId",
            description = "Returns Organism by taxId")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<Taxonomy> loadTaxonomy(@PathVariable final long taxId) throws IOException, ParseException {
        return Result.success(taxonomySecurityService.searchOrganismById(taxId));
    }

    @PutMapping(value = "/taxonomy/upload")
    @Operation(
            summary = "Creates Taxonomy Lucene Index from Taxonomy file",
            description = "Creates Taxonomy Lucene Index from Taxonomy file")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> uploadTaxonomyDatabase(@RequestParam final String taxonomyFilePath)
            throws IOException, ParseException {
        taxonomySecurityService.writeLuceneTaxonomyIndex(taxonomyFilePath);
        return Result.success(null);
    }
}
