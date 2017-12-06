/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

package com.epam.catgenome.controller.filter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.GeneSearchQuery;
import com.epam.catgenome.entity.index.Group;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.vcf.VcfFilterForm;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.manager.FeatureIndexManager;
import com.epam.catgenome.manager.vcf.VcfManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * A REST controller implementation, responsible for VCF filter services
 */
@RestController
@Api(value = "Filter", description = "Filtering operations")
public class FilterController extends AbstractRESTController {
    @Autowired
    private FeatureIndexManager featureIndexManager;

    @Autowired
    private VcfManager vcfManager;

    @RequestMapping(value = "/filter/searchGenes", method = RequestMethod.POST)
    @ApiOperation(
        value = "Searches for IDs of genes, that are affected by variations",
        notes = "Searches for IDs of genes, that are affected by variations located in VCF files, specified by ids",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<Set<String>> searchGenesInProject(@RequestBody GeneSearchQuery geneQuery) throws IOException {
        return Result.success(featureIndexManager.searchGenesInVcfFiles(geneQuery.getSearch(), geneQuery.getVcfIds()));
    }

    @RequestMapping(value = "/filter/info", method = RequestMethod.POST)
    @ApiOperation(
        value = "Returns information about VCF filter by file IDs.",
        notes = "Returns information about VCF filter by file IDs, all information taken from file header.",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<VcfFilterInfo> getFieldInfo(@RequestBody List<Long> vcfFileIds) throws IOException {
        return Result.success(vcfManager.getFiltersInfo(vcfFileIds));
    }

    @RequestMapping(value = "/filter", method = RequestMethod.POST)
    @ApiOperation(
        value = "Filters variations for a given VCF file",
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
    public Callable<Result<IndexSearchResult<VcfIndexEntry>>> filterVcf(@RequestBody final VcfFilterForm filterForm)
        throws IOException {
        return () -> Result.success(featureIndexManager.filterVariations(filterForm));
    }

    @RequestMapping(value = "/filter/group", method = RequestMethod.POST)
    @ApiOperation(
        value = "Groups variations by given field for a given set of VCF files, according to filter",
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
    public Callable<Result<List<Group>>> groupVariations(
                                                @RequestBody final VcfFilterForm filterForm,
                                                @RequestParam String groupBy) {
        return () -> Result.success(featureIndexManager.groupVariations(filterForm, groupBy));
    }
}
