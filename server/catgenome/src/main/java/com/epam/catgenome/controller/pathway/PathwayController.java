/*
 * MIT License
 *
 * Copyright (c) 2022 EPAM Systems
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

package com.epam.catgenome.controller.pathway;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.registration.BioPAXRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.PathwayRegistrationRequest;
import com.epam.catgenome.entity.pathway.NGBPathway;
import com.epam.catgenome.entity.pathway.PathwayQueryParams;
import com.epam.catgenome.entity.pathway.SpeciesDescription;
import com.epam.catgenome.util.db.Page;
import com.epam.catgenome.manager.pathway.PathwaySecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@Tag(name = "pathway", description = "Metabolic Pathways Management")
@RequiredArgsConstructor
public class PathwayController extends AbstractRESTController {

    private final PathwaySecurityService pathwaySecurityService;

    @GetMapping(value = "/pathway/{pathwayId}")
    @Operation(
            summary = "Returns a pathway by id",
            description = "Returns a pathway by id")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<NGBPathway> loadPathway(@PathVariable final Long pathwayId,
                                          @RequestParam(required = false) final Long projectId) {
        return Result.success(pathwaySecurityService.loadPathway(pathwayId, projectId));
    }

    @GetMapping(value = "/pathway/all")
    @Operation(
            summary = "Returns all registered pathways",
            description = "Returns all registered pathways")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<List<NGBPathway>> loadPathways(@RequestParam(required = false) final Long projectId) {
        return Result.success(pathwaySecurityService.loadPathways(projectId));
    }

    @GetMapping(value = "/pathway/content/{pathwayId}")
    @Operation(
            summary = "Returns a pathway file content by pathway id",
            description = "Returns a pathway file content by pathway id")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public void loadPathwayContent(@PathVariable final Long pathwayId,
                                   @RequestParam(required = false) final Long projectId,
                                   final HttpServletResponse response) throws IOException {
        byte[] bytes = pathwaySecurityService.loadPathwayContent(pathwayId, projectId);
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }

    @PostMapping(value = "/pathways")
    @Operation(
            summary = "Returns pathways page",
            description = "Returns pathways page")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<Page<NGBPathway>> loadPathways(@RequestBody final PathwayQueryParams params)
            throws IOException, ParseException {
        return Result.success(pathwaySecurityService.loadPathways(params));
    }

    @PostMapping(value = "/pathway")
    @Operation(
            summary = "Registers new pathway",
            description = "Registers new pathway")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<NGBPathway> registerPathway(@RequestBody final PathwayRegistrationRequest request)
            throws IOException {
        return Result.success(pathwaySecurityService.registerPathway(request));
    }

    @PostMapping(value = "/biopax")
    @Operation(
            summary = "Registers new BioPAX file",
            description = "Registers new BioPAX file")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> registerBioPAX(@RequestBody final BioPAXRegistrationRequest request) throws IOException {
        pathwaySecurityService.registerBioPAX(request);
        return Result.success(null);
    }

    @PutMapping(value = "/pathway/index")
    @Operation(
            summary = "Rebuilds the pathway Lucene index",
            description = "Rebuilds the pathway Lucene index for every registered pathway, from the "
                    + "database and the pathway files. Needed after an NGB upgrade that changes the "
                    + "Lucene index format - see the reindex procedure in the installation docs. "
                    + "Returns the number of pathways indexed.")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<Integer> reindexPathways() throws IOException {
        return Result.success(pathwaySecurityService.reindexPathways());
    }

    @DeleteMapping(value = "/pathway/{pathwayId}")
    @Operation(
            summary = "Deletes a pathway, specified by id",
            description = "Deletes a pathway, specified by id")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> deletePathway(@PathVariable final long pathwayId) throws IOException {
        pathwaySecurityService.deletePathway(pathwayId);
        return Result.success(null);
    }

    @GetMapping(value = "/pathway/species")
    @Operation(
            summary = "Returns list of unique species associated with pathways",
            description = "Returns list of unique species associated with pathways")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<List<SpeciesDescription>> loadSpecies() {
        return Result.success(pathwaySecurityService.loadSpecies());
    }

}
