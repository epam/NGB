/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

package com.epam.catgenome.controller.bucket;

import java.util.List;

import com.epam.catgenome.manager.bucket.BucketSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.entity.bucket.Bucket;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * Controller to provide REST API for S3 bucket management
 */
@Controller
@Api(value = "BUCKET", description = "Bucket Management")
public class BucketController extends AbstractRESTController {

    @Autowired
    private BucketSecurityService bucketSecurityService;

    @ResponseBody
    @RequestMapping(value = "/bucket/{bucketId}/load", method = RequestMethod.GET)
    @ApiOperation(
            value = "Returns a bucket by given ID",
            notes = "Provides extended data, including files in project",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public final Result<Bucket> loadBucketById(@PathVariable(value = "bucketId") final Long bucketId) {
        return Result.success(bucketSecurityService.load(bucketId));
    }

    @ResponseBody
    @RequestMapping(value = "/bucket/save", method = RequestMethod.POST)
    @ApiOperation(
            value = "Registers a bucket in the system.",
            notes = "Registers a bucket, stored in a BD. Registration request has the following " +
                    "properties: <br/>" +
                    "1) accessKeyId - access key ID <br/>" +
                    "2) secretAccessKey - secret access key </br>" +
                    "3) bucketName - <i>optional</i> a name for bucket.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Bucket> saveBucket(@RequestBody Bucket bucket) {
        return Result.success(bucketSecurityService.save(bucket));
    }

    @ResponseBody
    @RequestMapping(value = "/bucket/loadAll", method = RequestMethod.GET)
    @ApiOperation(
            value = "Returns all bucket that are available in the system at the moment.",
            notes = "Only the names and id.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public final Result<List<Bucket>> loadAllBucket() {
        return Result.success(bucketSecurityService.loadAllBucket());
    }
}
