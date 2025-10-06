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

package com.epam.catgenome.controller.sequence;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.sequence.LocalSequenceRequest;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.exception.ReferenceReadingException;
import com.epam.catgenome.exception.TargetGenesException;
import com.epam.catgenome.manager.externaldb.ncbi.util.NCBISequenceDatabase;
import com.epam.catgenome.manager.externaldb.sequence.SequenceSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@Tag(name = "sequence", description = "Sequences Management")
@RequiredArgsConstructor
public class SequenceController extends AbstractRESTController {

    private final SequenceSecurityService service;

    @GetMapping(value = "/sequence/{id}")
    @Operation(
            summary = "Returns a gene sequence by given ncbi sequence id and database type",
        description = "Returns a gene sequence by given ncbi sequence id and database type")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<String> getFasta(@RequestParam final NCBISequenceDatabase database, @PathVariable final String id)
            throws ExternalDbUnavailableException {
        return Result.success(service.getFasta(database, id));
    }

    @PostMapping(value = "/sequence/local")
    @Operation(
            summary = "Returns a gene sequence.",
        description = "Returns a gene sequence.")
    @ApiResponse(responseCode = HTTP_STATUS_OK,
            description = API_STATUS_DESCRIPTION,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public Result<String> getSequence(@RequestBody final LocalSequenceRequest request)
            throws ExternalDbUnavailableException, TargetGenesException, ReferenceReadingException,
            ParseException, IOException {
        return Result.success(service.getSequence(request));
    }
}
