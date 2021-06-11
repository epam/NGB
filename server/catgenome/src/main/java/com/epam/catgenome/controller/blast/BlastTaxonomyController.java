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
package com.epam.catgenome.controller.blast;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.manager.blast.BlastTaxonomySecurityService;
import com.epam.catgenome.manager.blast.dto.BlastTaxonomy;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@Api(value = "blast-taxonomy", description = "BLAST Taxonomy Data Management")
@RequiredArgsConstructor
public class BlastTaxonomyController extends AbstractRESTController {

    private final BlastTaxonomySecurityService blastTaxonomySecurityService;

    @GetMapping(value = "/taxonomies/{term}")
    @ApiOperation(
            value = "Returns list of Organisms by term",
            notes = "Returns list of Organisms by term",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<BlastTaxonomy>> loadTaxonomies(@PathVariable final String term)
            throws IOException, ParseException {
        return Result.success(blastTaxonomySecurityService.searchOrganisms(term));
    }

    @GetMapping(value = "/taxonomy/{taxId}")
    @ApiOperation(
            value = "Returns Organism by taxId",
            notes = "Returns Organism by taxId",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<BlastTaxonomy> loadTaxonomy(@PathVariable final long taxId) throws IOException, ParseException {
        return Result.success(blastTaxonomySecurityService.searchOrganismById(taxId));
    }

    @PutMapping(value = "/taxonomy/upload")
    @ApiOperation(
            value = "Creates Taxonomy Lucene Index from Taxonomy names.dmp file",
            notes = "Creates Taxonomy Lucene Index from Taxonomy names.dmp file",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> uploadTaxonomyDatabase(@RequestParam final String filename)
            throws IOException, ParseException {
        blastTaxonomySecurityService.writeLuceneTaxonomyIndex(filename);
        return Result.success(null);
    }
}
