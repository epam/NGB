/*
 * MIT License
 *
 * Copyright (c) 2016-2022 EPAM Systems
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

package com.epam.catgenome.controller.project;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.epam.catgenome.entity.project.ProjectDescription;
import com.epam.catgenome.manager.FeatureIndexSecurityService;
import com.epam.catgenome.manager.project.ProjectSecurityService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.GeneSearchQuery;
import com.epam.catgenome.controller.vo.ProjectVO;
import com.epam.catgenome.controller.vo.converter.ProjectConverter;
import com.epam.catgenome.entity.index.Group;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.vcf.VcfFilterForm;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.exception.FeatureIndexException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * Source:      ProjectController
 * Created:     11.01.16, 17:41
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * A REST Controller implementation, responsible for handling project related queries
 * </p>
 */
@Controller
@Api(value = "project", description = "Project Management")
public class ProjectController extends AbstractRESTController {
    private static final String PROJECT_ID_PARAM = "projectId";
    private static final String BIOLOGICAL_ITEM_ID_PARAM = "biologicalItemId";

    @Autowired
    private ProjectSecurityService projectSecurityService;

    @Autowired
    private FeatureIndexSecurityService featureIndexSecurityService;

    @GetMapping(value = "/project/loadMy")
    @ResponseBody
    @ApiOperation(
            value = "Returns all top-level projects",
            notes = "Each summary provides only major metadata per a single project, no files information is provided" +
                    " via this service",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<ProjectVO>> loadTopLevelProjects() {
        return Result.success(ProjectConverter.convertTo(projectSecurityService.loadTopLevelProjects()));
    }

    @GetMapping(value = "/project/tree")
    @ResponseBody
    @ApiOperation(
        value = "Returns all projects in a form of tree hierarchy",
        notes = "Each project contains all it's nested projects and items",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Callable<Result<List<ProjectVO>>> loadProjectsTreeForCurrentUser(
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) String referenceName) {
        return () -> Result.success(ProjectConverter.convertTo(
                projectSecurityService.loadProjectTree(parentId, referenceName)));
    }

    @GetMapping(value = "/project/{projectId}/load")
    @ResponseBody
    @ApiOperation(
            value = "Returns a project by given ID",
            notes = "Provides extended data, including files in project",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<ProjectVO> loadProject(@PathVariable(value = PROJECT_ID_PARAM) final Long projectId) {
        return Result.success(ProjectConverter.convertTo(projectSecurityService.load(projectId)));
    }

    @GetMapping(value = "/project/load")
    @ResponseBody
    @ApiOperation(
        value = "Returns a project by given name",
        notes = "Provides extended data, including files in project",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<ProjectVO> loadProject(@RequestParam final String projectName) {
        return Result.success(ProjectConverter.convertTo(
                projectSecurityService.load(projectName))
        );
    }

    @PostMapping(value = "/project/save")
    @ResponseBody
    @ApiOperation(
            value = "Creates new project or updates existing one",
            notes = "New project should contain a name field. Updated project should contain id and name fields. <br/>"
                    + "Optional parameter parentId stands for creating a new project as a nested project for existing "
                    + "one, specified by parentId parameter. Works only for creation of a new project. <br/>"
                    + "To move an existing project to another parent project, use <b>/project/{projectId}/move</b> "
                    + "service",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<ProjectVO> saveProject(@RequestBody ProjectVO project,
                                         @RequestParam(required = false) Long parentId) throws FeatureIndexException {
        return Result.success(ProjectConverter.convertTo(projectSecurityService.create(ProjectConverter.convertFrom(
            project), parentId)));
    }

    @ResponseBody
    @PutMapping(value = "/project/{name}/rename")
    @ApiOperation(
            value = "Updates project name and/or pretty name.",
            notes = "Updates project name and/or pretty name.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public final Result<Boolean> rename(
            @PathVariable(value = "name") final String name,
            @RequestParam(value = "newName", required = false) final String newName,
            @RequestParam(value = "newPrettyName", required = false) final String newPrettyName) {
        projectSecurityService.renameProject(name, newName, newPrettyName);
        return Result.success(null);
    }

    @PutMapping(value = "/project/{projectId}/move")
    @ResponseBody
    @ApiOperation(
        value = "Moves a project to a parent project",
        notes = "Moves an existing project, specified by projectId path variable, to a parent project, specified by "
                + "parentID parameter. To move a project to top level, pass no "
                + "parameter",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<Boolean> moveProject(@PathVariable Long projectId, @RequestParam(required = false) Long parentId) {
        projectSecurityService.moveProjectToParent(projectId, parentId);
        return Result.success(true);
    }

    @PutMapping(value = "/project/{projectId}/add/{biologicalItemId}")
    @ResponseBody
    @ApiOperation(
            value = "Adds a file to project",
            notes = "Adds a file, specified by its biologicalItemId, to a project, specified by its projectId",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<ProjectVO> addProjectItem(@PathVariable(value = PROJECT_ID_PARAM) final Long projectId,
                                        @PathVariable(value = BIOLOGICAL_ITEM_ID_PARAM) final Long biologicalItemId)
        throws FeatureIndexException {
        return Result.success(ProjectConverter.convertTo(
                projectSecurityService.addProjectItem(projectId, biologicalItemId)));
    }

    @DeleteMapping(value = "/project/{projectId}/remove/{biologicalItemId}")
    @ResponseBody
    @ApiOperation(
            value = "Removes a file from a project",
            notes = "Removes a file, specified by its biologicalItemId, from a project, specified by its projectId",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<ProjectVO> removeProjectItem(@PathVariable(value = PROJECT_ID_PARAM) final Long projectId,
                                        @PathVariable(value = BIOLOGICAL_ITEM_ID_PARAM) final Long biologicalItemId)
        throws FeatureIndexException {
        return Result.success(ProjectConverter.convertTo(projectSecurityService.removeProjectItem(projectId,
                biologicalItemId)));
    }

    @PutMapping(value = "/project/{projectId}/hide/{biologicalItemId}")
    @ResponseBody
    @ApiOperation(
            value = "Hides a project file",
            notes = "Hides a file, specified by its biologicalItemId, from a project, specified by its projectId",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<ProjectVO> hideProjectItem(@PathVariable(value = PROJECT_ID_PARAM) final Long projectId,
                                          @PathVariable(value = BIOLOGICAL_ITEM_ID_PARAM) final Long biologicalItemId) {
        projectSecurityService.hideProjectItem(projectId, biologicalItemId);
        return Result.success(ProjectConverter.convertTo(projectSecurityService.load(projectId)));
    }

    @GetMapping(value = "/project/{projectId}/search")
    @ResponseBody
    @ApiOperation(
            value = "Searches for a given feature ID in a given project, case-insensitive",
            notes = "Looks up project files indexes for a given feature ID and returns an index entry",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<IndexSearchResult> searchFeatureInProject(
            @PathVariable(value = PROJECT_ID_PARAM) final Long projectId, @RequestParam String featureId)
            throws IOException {
        return Result.success(featureIndexSecurityService.searchFeaturesInProject(featureId, projectId));
    }

    @PostMapping(value = "/project/{projectId}/filter/vcf")
    @ResponseBody
    @ApiOperation(
            value = "Filters variations for a given VCF file in a given project",
            notes = "Request should contain the following fields: <br/>" +
                    "<b>vcfFileIds</b>: an array of IDs of VCF files to filter<br/>" +
                    "other fields are optional: <br/>" +
                    "<b>chromosomeId</b>: an ID of a chromosome to load variations</br>" +
                    "<b>variationTypes</b>: an object with te following fields:<br/>" +
                    "&nbsp;&nbsp;<b>variationTypes.field</b> : an array of variation types " +
                    "(SNV, MNP, INS, DEL, MIXED, DUP, INV, BND)<br/>" +
                    "&nbsp;&nbsp;<b>variationTypes.conjunction</b> : a boolean field, controlling the way values " +
                    "are combined: true for AND, false OR<br/><br/>" +
                    "<b>genes</b>: an object:<br/>" +
                    "&nbsp;&nbsp;<b>genes.field</b>: an array of gene name prefixes to filter variations that are " +
                    "located in these genes<br/>" +
                    "&nbsp;&nbsp;<b>genes.conjunction</b>: same<br/><br/>" +
                    "<b>effects</b>: an object:<br/>" +
                    "&nbsp;&nbsp;<b>effects.field</b>: an array of variation effect types: DOWNSTREAM, UPSTREAM, " +
                    "INTRON, MISSENCE, UTR3, UTR5<br/>" +
                    "&nbsp;&nbsp;<b>effects.conjunction</b>: same<br/><br/>" +
                    "<b>impacts</b>: an object:<br/>" +
                    "&nbsp;&nbsp;<b>impacts.field</b>: an array of variation impact types: " +
                    "HIGH, MODERATE, MODIFIER, LOW<br/>" +
                    "&nbsp;&nbsp;<b>impacts.conjunction</b>: same<br/><br/>" +
                    "<b>additionalFilters</b>: an object with keys from filter info and values of " +
                    "corresponding type. For range query put an array<br/>" +
                    "<b>infoFields</b>: an array of additional info fields to fetch from index<br/>" +
                    "<b>page</b>: Defines a page number to display<br/>" +
                    "<b>pageSize</b>: Defines a number of items per page. Attention: paged result is provided only if "
                    + "both <b>page</b> and <b>pageSize</b> are specified. Otherwise, a full result is returned<br/>"
                    + "<b>orderBy</b>: an array of objects of the following type:<br/>"
                    + "&nbsp;&nbsp;<b>orderBy.field</b>: name of the field to sort results. Can have the following "
                    + "values: CHROMOSOME_NAME, START_INDEX, END_INDEX, FILTER, VARIATION_TYPE, QUALITY, GENE_ID, "
                    + "GENE_NAME or any field from infoFields array<br/>"
                    + "&nbsp;&nbsp;<b>orderBy.desc</b>: determines if ascending or descending order should be applied. "
                    + "Default value is false, for ascending order" +
                    "<br/><br/>" +

                    "Response contains the following fields:<br/>" +
                    "<b>endIndex</b>: variation's end index<br/>" +
                    "<b>startIndex</b>: variation's start index<br/>" +
                    "<b>featureId</b>: variation's ID<br/>" +
                    "<b>chromosome</b>: chromosome, where variation is located<br/>" +
                    "<b>featureType</b>: a system field, containing feature type, should have only 'vcf' value<br/>" +
                    "<b>featureFileId</b>: ID of a vcf file, where variation is located<br/>" +
                    "<b>variationType</b>: a type of variation (SNV, MNP, INS, DEL, MIXED, DUP, INV, BND)<br/>" +
                    "<b>gene</b>: an ID of a gene, which is affected by variation<br/>" +
                    "<b>geneNames</b>: an ID of a gene, which is affected by variation. If variation affects " +
                    "multiple genes, this filed will contain multiple coma separated values. <b>UI should use this " +
                    "field</b><br/>" +
                    "<b>failedFilter</b>: contains filters, that variation has failed. Empty if everything is " +
                    "okay<br/>" +
                    "<b>impact</b>: an impact of a variation</br>" +
                    "<b>effect</b>: an effect of a variation</br>" +
                    "<b>info</b>: an object, containing requested additional info fields, if they are present",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<VcfIndexEntry>> filterVcf(@RequestBody final VcfFilterForm filterForm,
                                                 @PathVariable(value = PROJECT_ID_PARAM) long projectId)
            throws IOException {
        return Result.success(featureIndexSecurityService.filterVariations(filterForm, projectId).getEntries());
    }

    @PostMapping(value = "/project/{projectId}/filter/vcf/new")
    @ResponseBody
    @ApiOperation(
        value = "Filters variations for a given VCF file in a given project",
        notes = "Request should contain the following fields: <br/>" +
                "<b>vcfFileIds</b>: an array of IDs of VCF files to filter<br/>" +
                "other fields are optional: <br/>" +
                "<b>chromosomeId</b>: an ID of a chromosome to load variations</br>" +
                "<b>variationTypes</b>: an object with te following fields:<br/>" +
                "&nbsp;&nbsp;<b>variationTypes.field</b> : an array of variation types " +
                "(SNV, MNP, INS, DEL, MIXED, DUP, INV, BND)<br/>" +
                "&nbsp;&nbsp;<b>variationTypes.conjunction</b> : a boolean field, controlling the way values " +
                "are combined: true for AND, false OR<br/><br/>" +
                "<b>genes</b>: an object:<br/>" +
                "&nbsp;&nbsp;<b>genes.field</b>: an array of gene name prefixes to filter variations that are " +
                "located in these genes<br/>" +
                "&nbsp;&nbsp;<b>genes.conjunction</b>: same<br/><br/>" +
                "<b>effects</b>: an object:<br/>" +
                "&nbsp;&nbsp;<b>effects.field</b>: an array of variation effect types: DOWNSTREAM, UPSTREAM, " +
                "INTRON, MISSENCE, UTR3, UTR5<br/>" +
                "&nbsp;&nbsp;<b>effects.conjunction</b>: same<br/><br/>" +
                "<b>impacts</b>: an object:<br/>" +
                "&nbsp;&nbsp;<b>impacts.field</b>: an array of variation impact types: " +
                "HIGH, MODERATE, MODIFIER, LOW<br/>" +
                "&nbsp;&nbsp;<b>impacts.conjunction</b>: same<br/><br/>" +
                "<b>additionalFilters</b>: an object with keys from filter info and values of " +
                "corresponding type. For range query put an array<br/>" +
                "<b>infoFields</b>: an array of additional info fields to fetch from index<br/>" +
                "<b>page</b>: Defines a page number to display<br/>" +
                "<b>pageSize</b>: Defines a number of items per page. Attention: paged result is provided only if "
                + "both <b>page</b> and <b>pageSize</b> are specified. Otherwise, a full result is returned<br/>"
                + "<b>orderBy</b>: an array of objects of the following type:<br/>"
                + "&nbsp;&nbsp;<b>orderBy.field</b>: name of the field to sort results. Can have the following "
                + "values: CHROMOSOME_NAME, START_INDEX, END_INDEX, FILTER, VARIATION_TYPE, QUALITY, GENE_ID, "
                + "GENE_NAME or any field from infoFields array<br/>"
                + "&nbsp;&nbsp;<b>orderBy.desc</b>: determines if ascending or descending order should be applied. "
                + "Default value is false, for ascending order" +
                "<br/><br/>" +

                "Response contains the following fields:<br/>" +
                "<b>endIndex</b>: variation's end index<br/>" +
                "<b>startIndex</b>: variation's start index<br/>" +
                "<b>featureId</b>: variation's ID<br/>" +
                "<b>chromosome</b>: chromosome, where variation is located<br/>" +
                "<b>featureType</b>: a system field, containing feature type, should have only 'vcf' value<br/>" +
                "<b>featureFileId</b>: ID of a vcf file, where variation is located<br/>" +
                "<b>variationType</b>: a type of variation (SNV, MNP, INS, DEL, MIXED, DUP, INV, BND)<br/>" +
                "<b>gene</b>: an ID of a gene, which is affected by variation<br/>" +
                "<b>geneNames</b>: an ID of a gene, which is affected by variation. If variation affects " +
                "multiple genes, this filed will contain multiple coma separated values. <b>UI should use this " +
                "field</b><br/>" +
                "<b>failedFilter</b>: contains filters, that variation has failed. Empty if everything is " +
                "okay<br/>" +
                "<b>impact</b>: an impact of a variation</br>" +
                "<b>effect</b>: an effect of a variation</br>" +
                "<b>info</b>: an object, containing requested additional info fields, if they are present",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<IndexSearchResult<VcfIndexEntry>> filterVcfNew(@RequestBody final VcfFilterForm filterForm,
                                                 @PathVariable(value = PROJECT_ID_PARAM) long projectId)
        throws IOException {
        return Result.success(featureIndexSecurityService.filterVariations(filterForm, projectId));
    }

    @PostMapping(value = "/project/{projectId}/group/vcf")
    @ResponseBody
    @ApiOperation(
        value = "Groups variations by given field for a given project, according to filter",
        notes = "Request should contain the following fields: <br/>" +
                "<b>vcfFileIds</b>: an array of IDs of VCF files to filter<br/>" +
                "other fields are optional: <br/>" +
                "<b>chromosomeId</b>: an ID of a chromosome to load variations</br>" +
                "<b>variationTypes</b>: an object with te following fields:<br/>" +
                "&nbsp;&nbsp;<b>variationTypes.field</b> : an array of variation types " +
                "(SNV, MNP, INS, DEL, MIXED, DUP, INV, BND)<br/>" +
                "&nbsp;&nbsp;<b>variationTypes.conjunction</b> : a boolean field, controlling the way values " +
                "are combined: true for AND, false OR<br/><br/>" +
                "<b>genes</b>: an object:<br/>" +
                "&nbsp;&nbsp;<b>genes.field</b>: an array of gene name prefixes to filter variations that are " +
                "located in these genes<br/>" +
                "&nbsp;&nbsp;<b>genes.conjunction</b>: same<br/><br/>" +
                "<b>effects</b>: an object:<br/>" +
                "&nbsp;&nbsp;<b>effects.field</b>: an array of variation effect types: DOWNSTREAM, UPSTREAM, " +
                "INTRON, MISSENCE, UTR3, UTR5<br/>" +
                "&nbsp;&nbsp;<b>effects.conjunction</b>: same<br/><br/>" +
                "<b>impacts</b>: an object:<br/>" +
                "&nbsp;&nbsp;<b>impacts.field</b>: an array of variation impact types: " +
                "HIGH, MODERATE, MODIFIER, LOW<br/>" +
                "&nbsp;&nbsp;<b>impacts.conjunction</b>: same<br/><br/>" +
                "<b>additionalFilters</b>: an object with keys from filter info and values of " +
                "corresponding type. For range query put an array<br/>" +
                "<b>infoFields</b>: an array of additional info fields to fetch from index<br/><br/>" +
                "<b>orderBy</b> request parameter controls a field by which variations are grouped. It supports the "+
                "following values: CHROMOSOME_NAME, START_INDEX, END_INDEX, FILTER, VARIATION_TYPE, QUALITY, GENE_ID, "+
                "GENE_NAME or any field from infoFields array" +
                "<br/><br/>" +

                "Response contains the following fields:<br/>" +
                "<b>endIndex</b>: variation's end index<br/>" +
                "<b>startIndex</b>: variation's start index<br/>" +
                "<b>featureId</b>: variation's ID<br/>" +
                "<b>chromosome</b>: chromosome, where variation is located<br/>" +
                "<b>featureType</b>: a system field, containing feature type, should have only 'vcf' value<br/>" +
                "<b>featureFileId</b>: ID of a vcf file, where variation is located<br/>" +
                "<b>variationType</b>: a type of variation (SNV, MNP, INS, DEL, MIXED, DUP, INV, BND)<br/>" +
                "<b>gene</b>: an ID of a gene, which is affected by variation<br/>" +
                "<b>geneNames</b>: an ID of a gene, which is affected by variation. If variation affects " +
                "multiple genes, this filed will contain multiple coma separated values. <b>UI should use this " +
                "field</b><br/>" +
                "<b>failedFilter</b>: contains filters, that variation has failed. Empty if everything is " +
                "okay<br/>" +
                "<b>impact</b>: an impact of a variation</br>" +
                "<b>effect</b>: an effect of a variation</br>" +
                "<b>info</b>: an object, containing requested additional info fields, if they are present",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<List<Group>> groupVariations(@RequestBody final VcfFilterForm filterForm,
                                               @PathVariable(value = PROJECT_ID_PARAM) long projectId,
                                               @RequestParam String groupBy) throws IOException {
        return Result.success(featureIndexSecurityService.groupVariations(filterForm, projectId, groupBy));
    }


    @ResponseBody
    @PostMapping(value = "/project/{projectId}/filter/vcf/searchGenes")
    @ApiOperation(
            value = "Searches for IDs of genes, that are affected by variations",
            notes = "Searches for IDs of genes, that are affected by variations located in VCF files, specified by " +
                    "ids, in a given project, specified by project ID",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Set<String>> searchGenesInProject(@PathVariable(value = PROJECT_ID_PARAM) long projectId,
                                                    @RequestBody GeneSearchQuery geneQuery) throws IOException {
        return Result.success(
                featureIndexSecurityService.searchGenesInVcfFilesInProject(projectId, geneQuery.getSearch(),
                geneQuery.getVcfIdsByProject().values().stream().flatMap(List::stream).collect(Collectors.toList())));
    }

    @ResponseBody
    @GetMapping(value = "/project/{projectId}/filter/vcf/info")
    @ApiOperation(
            value = "Returns information for VCF filter for given project ID",
            notes = "Returns information for VCF filter for given project ID, all information taken from VCF file " +
                    "headers.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<VcfFilterInfo> erg(@PathVariable(value = PROJECT_ID_PARAM) final Long projectId) throws IOException {
        return Result.success(featureIndexSecurityService.loadVcfFilterInfoForProject(projectId));
    }

    @DeleteMapping(value = "/project/{projectId}")
    @ResponseBody
    @ApiOperation(
            value = "Deletes a project, specified by project ID",
            notes = "Deletes a project with all it's items and bookmarks",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> deleteProject(@PathVariable final long projectId,
                                         @RequestParam(name = "force", required = false, defaultValue = "false")
                                                 Boolean force) throws IOException {
        Project deletedProject = projectSecurityService.deleteProject(projectId, force);
        return Result.success(true, MessageHelper.getMessage(MessagesConstants.INFO_PROJECT_DELETED,
                deletedProject.getId(), deletedProject.getName()));
    }

    @PostMapping("/project/{projectId}/description")
    @ResponseBody
    @ApiOperation(
            value = "Creates or updates project description",
            notes = "Creates or updates project description",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<ProjectDescription> upsertProjectDescription(@PathVariable final Long projectId,
            @RequestParam(value = "path", required = false) final String path,
            @RequestParam(value = "name", required = false) final String name,
            @RequestParam(value = "file", required = false) final MultipartFile multipart) throws IOException {
        return Result.success(projectSecurityService.upsertProjectDescription(projectId, name, path, multipart));
    }

    @GetMapping("/project/description/{id}")
    @ResponseBody
    @ApiOperation(
            value = "Downloads project description file",
            notes = "Downloads project description file",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public void downloadProjectDescription(@PathVariable final Long id, final HttpServletResponse response)
            throws IOException {
        final InputStream projectDescriptionContent = projectSecurityService.loadProjectDescription(id);
        if (Objects.isNull(projectDescriptionContent)) {
            return;
        }
        IOUtils.copy(projectDescriptionContent, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/project/{projectId}/description")
    @ResponseBody
    @ApiOperation(
            value = "Returns project descriptions info",
            notes = "Returns project descriptions info",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<ProjectDescription>> getProjectDescriptions(@PathVariable final Long projectId) {
        return Result.success(projectSecurityService.loadProjectDescriptions(projectId));
    }

    @DeleteMapping("/project/description/{id}")
    @ResponseBody
    @ApiOperation(
            value = "Deletes project description specified by ID",
            notes = "Deletes project description specified by ID",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<ProjectDescription> deleteProjectDescription(@PathVariable final Long id) {
        return Result.success(projectSecurityService.deleteProjectDescription(id));
    }

    @DeleteMapping("/project/{projectId}/description")
    @ResponseBody
    @ApiOperation(
            value = "Deletes project description by project ID",
            notes = "If 'name' parameter was not specified all attached to project descriptions will be removed",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<ProjectDescription>> deleteProjectDescriptionByProject(@PathVariable final Long projectId,
            @RequestParam(value = "name", required = false) final String name) {
        return Result.success(projectSecurityService.deleteProjectDescriptions(projectId, name));
    }
}
