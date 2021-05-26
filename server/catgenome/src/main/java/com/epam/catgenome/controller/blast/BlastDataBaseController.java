package com.epam.catgenome.controller.blast;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.BlastDataBaseVO;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.manager.blast.BlastDataBaseSecurityService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(value = "blast", description = "Blast Data Bases Management")
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
    public Result<Boolean> saveDataBase(@RequestBody final BlastDataBaseVO dataBaseVO) throws FeatureIndexException {
        blastDataBaseSecurityService.save(dataBaseVO);
        return Result.success(null);
    }
}
