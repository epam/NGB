/*
 * MIT License
 *
 * Copyright (c) 2017-2022 EPAM Systems
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

package com.epam.catgenome.controller.bam;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.entity.bam.BamCoverage;
import com.epam.catgenome.entity.bam.CoverageInterval;
import com.epam.catgenome.entity.bam.CoverageQueryParams;
import com.epam.catgenome.manager.bam.BamCoverageSecurityService;
import com.epam.catgenome.util.db.Page;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Source:      BamCoverageController.java
 * Created:     2/16/2022
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code BamCoverageController} represents implementation of MVC controller which handles
 * requests to manage BAM Coverages.
 * <p>
 * It's designed to communicate with corresponding managers that provide all required
 * calls and manage all operations concerned with BAM data.
 *
 */
@Controller
@Api(value = "bam-coverage", description = "BAM Coverage Management")
public class BamCoverageController extends AbstractRESTController {

    @Autowired
    private BamCoverageSecurityService securityService;

    @ResponseBody
    @PostMapping(value = "/bam/coverage")
    @ApiOperation(
        value = "Creates coverage with given step for the BAM file",
        notes = "Creates coverage with given step for the BAM file",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<BamCoverage> createCoverage(@RequestBody final BamCoverage coverage) throws IOException {
        return Result.success(securityService.createCoverage(coverage));
    }

    @ResponseBody
    @DeleteMapping(value = "/bam/coverage")
    @ApiOperation(
        value = "Deletes BAM coverage by Bam file Id and step",
        notes = "Deletes BAM coverage by Bam file Id and step",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<Boolean> deleteCoverage(@RequestParam final Long bamId,
                                          @RequestParam(required = false) final Integer step)
            throws IOException, InterruptedException, ParseException {
        securityService.deleteCoverage(bamId, step);
        return Result.success(true);
    }

    @ResponseBody
    @PostMapping(value = "/bam/coverage/search")
    @ApiOperation(
        value = "Returns coverage intervals for a BAM file",
        notes = "Returns coverage intervals for a BAM file",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<Page<CoverageInterval>> loadCoverage(@RequestBody final CoverageQueryParams params)
            throws ParseException, IOException {
        return Result.success(securityService.loadCoverage(params));
    }

    @ResponseBody
    @GetMapping(value = "/bam/coverage")
    @ApiOperation(
        value = "Returns all registered coverages for bam files",
        notes = "Returns all registered coverages for bam files",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<List<BamCoverage>> loadByBamId(@RequestParam final Set<Long> bamIds) throws IOException {
        return Result.success(securityService.loadByBamId(bamIds));
    }

    @ResponseBody
    @GetMapping(value = "/bam/coverage/all")
    @ApiOperation(
        value = "Returns all registered coverages",
        notes = "Returns all registered coverages",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<List<BamCoverage>> loadAll() throws IOException {
        return Result.success(securityService.loadAll());
    }
}
