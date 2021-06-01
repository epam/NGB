package com.epam.catgenome.controller.blast;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.entity.blast.BlastDataBase;
import com.epam.catgenome.entity.blast.BlastDataBaseType;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.manager.blast.BlastDataBaseSecurityService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Api(value = "blast-database", description = "BLAST Data Bases Management")
@RequiredArgsConstructor
public class BlastDataBaseController extends AbstractRESTController {

    private final BlastDataBaseSecurityService blastDataBaseSecurityService;

    @PostMapping(value = "/database")
    @ResponseBody
    @ApiOperation(
            value = "Creates new Data Base record or updates existing one",
            notes = "Creates new Data Base record or updates existing one",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> saveDataBase(@RequestBody final BlastDataBase blastDataBase) throws FeatureIndexException {
        blastDataBaseSecurityService.save(blastDataBase);
        return Result.success(null);
    }

    @GetMapping(value = "/database/{id}")
    @ResponseBody
    @ApiOperation(
            value = "Gets Data Base by Id",
            notes = "Gets Data Base by Id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<BlastDataBase> loadDataBase(@PathVariable final long id) throws FeatureIndexException {
        return Result.success(blastDataBaseSecurityService.loadById(id));
    }

    @GetMapping(value = {"/databases/{type}", "/databases/"})
    @ResponseBody
    @ApiOperation(
            value = "Gets Data Bases",
            notes = "Gets Data Bases",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<BlastDataBase>> loadDataBases(@PathVariable(required = false) final BlastDataBaseType type) throws FeatureIndexException {
        return Result.success(blastDataBaseSecurityService.load(type));
    }

    @DeleteMapping(value = "/database/{id}")
    @ResponseBody
    @ApiOperation(
            value = "Deletes Data Base by Id",
            notes = "Deletes Data Base by Id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<BlastDataBase> deleteDataBase(@PathVariable final long id) throws FeatureIndexException {
        blastDataBaseSecurityService.delete(id);
        return Result.success(null);
    }
}
