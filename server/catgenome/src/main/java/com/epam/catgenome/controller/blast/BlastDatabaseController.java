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
import com.epam.catgenome.entity.blast.BlastDatabase;
import com.epam.catgenome.entity.blast.BlastDatabaseType;
import com.epam.catgenome.manager.blast.BlastDatabaseSecurityService;
import com.epam.catgenome.manager.externaldb.taxonomy.Taxonomy;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
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
@Api(value = "blast-database", description = "BLAST Databases Management")
@RequiredArgsConstructor
public class BlastDatabaseController extends AbstractRESTController {

    private final BlastDatabaseSecurityService blastDatabaseSecurityService;

    @PostMapping(value = "/blast/database")
    @ApiOperation(
            value = "Creates new database record or updates existing one",
            notes = "Creates new database record or updates existing one",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<BlastDatabase> saveDatabase(@RequestBody final BlastDatabase database) throws IOException {
        return Result.success(blastDatabaseSecurityService.save(database));
    }

    @GetMapping(value = "/blast/database/{id}")
    @ApiOperation(
            value = "Gets database by Id",
            notes = "Gets database by Id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<BlastDatabase> loadDatabase(@PathVariable final long id) {
        return Result.success(blastDatabaseSecurityService.loadById(id));
    }

    @GetMapping(value = "/blast/database/{id}/organisms")
    @ApiOperation(
            value = "Returns list of Organisms by term",
            notes = "Returns list of Organisms by term",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<Taxonomy>> loadTaxonomies(@RequestParam final String term,
                                                 @PathVariable final long id)
            throws IOException, ParseException {
        return Result.success(blastDatabaseSecurityService.searchOrganisms(term, id));
    }

    @PutMapping(value = "/blast/database/{id}/organisms")
    @ApiOperation(
            value = "Updates database organisms",
            notes = "Updates database organisms for given database Id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> updateDatabaseOrganisms(@PathVariable final long id) throws IOException {
        blastDatabaseSecurityService.updateDatabaseOrganisms(id);
        return Result.success(null);
    }

    @GetMapping(value = "/blast/databases")
    @ApiOperation(
            value = "Gets databases",
            notes = "Gets databases",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<BlastDatabase>> loadDatabases(@RequestParam(required = false) final BlastDatabaseType type,
                                                     @RequestParam(required = false) final String path) {
        return Result.success(blastDatabaseSecurityService.load(type, path));
    }

    @DeleteMapping(value = "/blast/database/{id}")
    @ApiOperation(
            value = "Deletes database by Id",
            notes = "Deletes database by Id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> deleteDatabase(@PathVariable final long id) {
        blastDatabaseSecurityService.delete(id);
        return Result.success(null);
    }
}
