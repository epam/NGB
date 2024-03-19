/*
 * MIT License
 *
 * Copyright (c) 2023-2024 EPAM Systems
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

package com.epam.catgenome.controller.target;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.entity.target.Target;
import com.epam.catgenome.entity.target.TargetGene;
import com.epam.catgenome.entity.target.TargetQueryParams;
import com.epam.catgenome.exception.TargetGenesException;
import com.epam.catgenome.exception.TargetUpdateException;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.index.FieldInfo;
import com.epam.catgenome.manager.index.SearchRequest;
import com.epam.catgenome.manager.target.AlignmentSecurityService;
import com.epam.catgenome.manager.target.TargetField;
import com.epam.catgenome.manager.target.TargetGeneSecurityService;
import com.epam.catgenome.manager.target.TargetSecurityService;
import com.epam.catgenome.util.db.Page;
import com.opencsv.exceptions.CsvValidationException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import htsjdk.samtools.reference.ReferenceSequence;
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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static com.epam.catgenome.util.Utils.DEFAULT_PAGE_SIZE;

@RestController
@Api(value = "target", description = "Target Management")
@RequiredArgsConstructor
public class TargetController extends AbstractRESTController {

    private final TargetSecurityService targetSecurityService;
    private final TargetGeneSecurityService targetGeneSecurityService;
    private final AlignmentSecurityService alignmentSecurityService;

    @GetMapping(value = "/target/{targetId}")
    @ApiOperation(
            value = "Returns a target by given id",
            notes = "Returns a target by given id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Target> loadTargets(@PathVariable final long targetId) {
        return Result.success(targetSecurityService.loadTarget(targetId));
    }

    @GetMapping(value = "/target/alignment/{targetId}")
    @ApiOperation(
            value = "Returns target alignment by sequence ids",
            notes = "Returns target alignment by sequence ids",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<ReferenceSequence>> getAlignment(@PathVariable final Long targetId,
                                                        @RequestParam final String firstSequenceId,
                                                        @RequestParam final String secondSequenceId,
                                                        final HttpServletResponse response) throws IOException {
        return Result.success(alignmentSecurityService.getAlignment(targetId, firstSequenceId, secondSequenceId));
    }

    @PostMapping(value = "/target/filter")
    @ApiOperation(
            value = "Filters targets",
            notes = "Filters targets. Result can be sorted by target_name field.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Page<Target>> loadTargets(@RequestBody final TargetQueryParams queryParameters) {
        final List<Target> targets = targetSecurityService.loadTargets(queryParameters);
        final int pageNum = queryParameters.getPagingInfo() == null ? 1 :
                Math.max(queryParameters.getPagingInfo().getPageNum(), 1);
        final int pageSize = queryParameters.getPagingInfo() == null ? DEFAULT_PAGE_SIZE :
                Math.max(queryParameters.getPagingInfo().getPageSize(), 1);
        return Result.success(Page.<Target>builder()
                .totalCount(targets.size())
                .items(targets.subList(Math.min((pageNum - 1) * pageSize, targets.size()),
                        Math.min((pageNum * pageSize), targets.size())))
                .build());
    }

    @GetMapping(value = "/target")
    @ApiOperation(
            value = "Returns targets with given gene name and taxonomy id",
            notes = "Returns targets with given gene name and taxonomy id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<Target>> loadTargets(@RequestParam final String geneName,
                                            @RequestParam(required = false) final Long taxId)
            throws ParseException, IOException {
        return Result.success(targetSecurityService.loadTargets(geneName, taxId));
    }

    @GetMapping(value = "/target/all")
    @ApiOperation(
            value = "Returns all targets",
            notes = "Returns all targets",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<Target>> loadTargets() {
        return Result.success(targetSecurityService.loadTargets());
    }

    @PostMapping(value = "/target")
    @ApiOperation(
            value = "Registers new target",
            notes = "Registers new target",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Target> createTarget(@RequestBody final Target target) throws IOException {
        return Result.success(targetSecurityService.createTarget(target));
    }

    @PutMapping(value = "/target")
    @ApiOperation(
            value = "Updates target",
            notes = "Updates target",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Target> updateTarget(@RequestBody final Target target) throws TargetUpdateException, IOException {
        return Result.success(targetSecurityService.updateTarget(target));
    }

    @DeleteMapping(value = "/target/{targetId}")
    @ApiOperation(
            value = "Deletes a target, specified by id",
            notes = "Deletes a target, specified by id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> deleteTarget(@PathVariable final long targetId) throws ParseException, IOException {
        targetSecurityService.deleteTarget(targetId);
        return Result.success(null);
    }

    @GetMapping(value = "/target/fieldValues")
    @ApiOperation(
            value = "Returns field values for target filter",
            notes = "Returns field values for target filter",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<String>> loadFieldValues(@RequestParam final TargetField field) {
        return Result.success(targetSecurityService.loadFieldValues(field));
    }

    @PostMapping(value = "/target/genes/import/{targetId}")
    @ApiOperation(
            value = "Imports genes from xlsx, csv and tsv files",
            notes = "Imports genes from xlsx, csv and tsv files",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> importGenes(@RequestParam(required = false) final String path,
                                       @RequestParam(value = "file", required = false) final MultipartFile multipart,
                                       @PathVariable final long targetId)
            throws IOException, ParseException, TargetGenesException, CsvValidationException {
        targetGeneSecurityService.importGenes(targetId, path, multipart);
        return Result.success(null);
    }

    @PostMapping(value = "/target/genes/{targetId}")
    @ApiOperation(
            value = "Adds genes to target",
            notes = "Adds genes to target",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> create(@PathVariable final long targetId,
                                  @RequestBody final List<TargetGene> targetGenes)
            throws IOException, ParseException, TargetGenesException {
        targetGeneSecurityService.create(targetId, targetGenes);
        return Result.success(null);
    }

    @PutMapping(value = "/target/genes")
    @ApiOperation(
            value = "Updates target genes",
            notes = "Updates target genes",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> update(@RequestBody final List<TargetGene> targetGenes)
            throws IOException, ParseException, TargetGenesException {
        targetGeneSecurityService.update(targetGenes);
        return Result.success(null);
    }

    @DeleteMapping(value = "/target/genes/{targetId}")
    @ApiOperation(
            value = "Deletes all target genes",
            notes = "Deletes all target genes",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> delete(@PathVariable final long targetId) throws IOException, ParseException {
        targetGeneSecurityService.delete(targetId);
        return Result.success(null);
    }

    @DeleteMapping(value = "/target/genes")
    @ApiOperation(
            value = "Deletes target genes",
            notes = "Deletes target genes",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> delete(@RequestParam(required = false) List<Long> targetGeneIds)
            throws IOException, ParseException {
        targetGeneSecurityService.delete(targetGeneIds);
        return Result.success(null);
    }

    @PostMapping(value = "/target/genes/filter/{targetId}")
    @ApiOperation(
            value = "Filters targets genes",
            notes = "Filters targets genes. Available fields info is available by GET /target/genes/fields/{targetId}.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SearchResult<TargetGene>> loadTargetGenes(@PathVariable final long targetId,
                                                            @RequestBody final SearchRequest request)
            throws ParseException, IOException {
        return Result.success(targetGeneSecurityService.filter(targetId, request));
    }

    @GetMapping(value = "/target/genes/fields/{targetId}")
    @ApiOperation(
            value = "Returns fields for target genes",
            notes = "Returns fields for target genes",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<FieldInfo>> getFields(@PathVariable final long targetId)
            throws ParseException, IOException {
        return Result.success(targetGeneSecurityService.getFieldInfos(targetId));
    }

    @GetMapping(value = "/target/genes/fieldValues/{targetId}")
    @ApiOperation(
            value = "Returns values for OPTIONAL target genes table filed",
            notes = "Returns values for OPTIONAL target genes table filed",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<String>> getFieldValues(@PathVariable final long targetId,
                                               @RequestParam final String field) throws ParseException, IOException {
        return Result.success(targetGeneSecurityService.getFieldValues(targetId, field));
    }

    @GetMapping(value = "/target/genes")
    @ApiOperation(
            value = "Returns target genes by internal ids.",
            notes = "Returns target genes by internal ids.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<TargetGene>> load(@RequestParam final List<Long> targetGeneIds)
            throws ParseException, IOException {
        return Result.success(targetGeneSecurityService.load(targetGeneIds));
    }

    @GetMapping(value = "/target/{targetId}/genes")
    @ApiOperation(
            value = "Returns target genes by string ids.",
            notes = "Returns target genes by string ids.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<TargetGene>> load(@PathVariable final Long targetId,
                                         @RequestParam final List<String> geneIds)
            throws ParseException, IOException {
        return Result.success(targetGeneSecurityService.load(targetId, geneIds));
    }
}
