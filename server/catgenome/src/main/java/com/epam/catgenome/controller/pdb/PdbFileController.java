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

package com.epam.catgenome.controller.pdb;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.entity.pdb.PdbFile;
import com.epam.catgenome.entity.pdb.PdbFileQueryParams;
import com.epam.catgenome.manager.pdb.PdbFileSecurityService;
import com.epam.catgenome.util.db.Page;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@Api(value = "pdb", description = "PDB Files Management")
@RequiredArgsConstructor
public class PdbFileController extends AbstractRESTController {

    private final PdbFileSecurityService service;

    @GetMapping(value = "/pdb/{pdbFileId}")
    @ApiOperation(
            value = "Returns a pdb file by given id",
            notes = "Returns a pdb file by given id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<PdbFile> load(@PathVariable final long pdbFileId) {
        return Result.success(service.load(pdbFileId));
    }

    @GetMapping(value = "/pdb")
    @ApiOperation(
            value = "Returns pdb files",
            notes = "Returns pdb files",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<PdbFile>> load() {
        return Result.success(service.load());
    }

    @PostMapping(value = "/pdb/filter")
    @ApiOperation(
            value = "Filters pdb files",
            notes = "Filters pdb files. Result can be sorted by gene_id, name(default) and pretty_name fields.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Page<PdbFile>> loadTarget(@RequestBody final PdbFileQueryParams params) {
        return Result.success(service.load(params));
    }

    @PostMapping(value = "/pdb")
    @ApiOperation(
            value = "Registers new pdb file",
            notes = "Registers new pdb file",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<PdbFile> create(@RequestBody final PdbFile pdbFile) throws IOException {
        return Result.success(service.create(pdbFile));
    }

    @PutMapping(value = "/pdb/{pdbFileId}")
    @ApiOperation(
            value = "Updates pdb file metadata",
            notes = "Updates pdb file metadata",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> updateMetadata(@PathVariable final long pdbFileId,
                                          @RequestBody final Map<String, String> metadata) {
        service.updateMetadata(pdbFileId, metadata);
        return Result.success(null);
    }

    @DeleteMapping(value = "/pdb/{pdbFileId}")
    @ApiOperation(
            value = "Deletes a pdb file, specified by id",
            notes = "Deletes a pdb file, specified by id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> delete(@PathVariable final long pdbFileId) {
        service.delete(pdbFileId);
        return Result.success(null);
    }

    @GetMapping(value = "/pdb/content/{pdbFileId}")
    @ApiOperation(
            value = "Gets pdb file content",
            notes = "Gets pdb file content",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public void load(@PathVariable final long pdbFileId, final HttpServletResponse response) throws IOException {
        byte[] bytes = service.loadContent(pdbFileId);
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }
}
