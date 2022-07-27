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

package com.epam.catgenome.controller.dataitem;

import static com.epam.catgenome.component.MessageHelper.getMessage;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemDownloadUrl;
import com.epam.catgenome.entity.BiologicalDataItemFile;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.manager.dataitem.DataItemManager;
import com.epam.catgenome.manager.dataitem.DataItemSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 *  <p>
 * {@code DataItemController} represents implementation of MVC controller which handles
 * requests to manage data common to all types of files, registered on the server.
 * <p>
 * It's designed to communicate with corresponded managers that provide all required
 * calls and manage all operations concerned with such data.
 *
 */
@Controller
@Api(value = "DATAITEM", description = "Data Item Management")
public class DataItemController extends AbstractRESTController {

    @Autowired
    private DataItemSecurityService dataItemSecurityService;

    @Autowired
    private DataItemManager dataItemManager;

    @ResponseBody
    @GetMapping(value = "/dataitem/search")
    @ApiOperation(
            value = "Finds all files registered on the server by a specified file name",
            notes = "Finds all files registered on the server by a specified file name</br>" +
                    "Input arguments:</br>" +
                    "<b>name</b> - search query for the file name,</br>" +
                    "<b>strict</b> - if true a strict, case sensitive search is performed, " +
                    "otherwise a substring, case insensitive search is performed.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public final Result<List<BiologicalDataItem>> findFilesByName(@RequestParam(value = "name") final String name,
            @RequestParam(value = "strict", required = false, defaultValue = "true") final boolean strict) {
        return Result.success(dataItemSecurityService.findFilesByName(name, strict));
    }

    @ResponseBody
    @GetMapping(value = "/dataitem/formats")
    @ApiOperation(
            value = "Get all available bed formats.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Map<String, BiologicalDataItemFormat>> getBedFormats() {
        return Result.success(dataItemSecurityService.getFormats());
    }

    @ResponseBody
    @DeleteMapping(value = "/dataitem/delete")
    @ApiOperation(
            value = "Deletes a file, specified by biological item id from the database",
            notes = "Deletes a file, specified by biological item id from the database",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public final Result<Boolean>  deleteFileBiBioItemId(@RequestParam(value = "id") final Long id)
            throws IOException {
        BiologicalDataItem deletedFile = dataItemSecurityService.deleteFileByBioItemId(id);
        return Result.success(true, getMessage(MessagesConstants.INFO_UNREGISTER, deletedFile.getName()));
    }

    @ResponseBody
    @GetMapping(value = "/dataitem/find")
    @ApiOperation(
            value = "Finds a file, specified by biological item id from the database",
            notes = "Finds a file, specified by biological item id from the database",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public final Result<BiologicalDataItem> findFileBiBioItemId(@RequestParam(value = "id") final Long id)
            throws IOException {
        return Result.success(dataItemSecurityService.findFileByBioItemId(id));
    }

    @ResponseBody
    @PutMapping(value = "/dataitem/rename")
    @ApiOperation(
            value = "Updates file name and/or pretty name.",
            notes = "Updates file name and/or pretty name.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public final Result<Boolean> rename(
            @RequestParam(value = "name") final String name,
            @RequestParam(value = "newName", required = false) final String newName,
            @RequestParam(value = "newPrettyName", required = false) final String newPrettyName) {
        dataItemSecurityService.renameFile(name, newName, newPrettyName);
        return Result.success(null);
    }

    @GetMapping("/dataitem/{id}/download")
    @ApiOperation(
            value = "Downloads a file specified by biological item id",
            notes = "Downloads a file specified by biological item id",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadFileByBiologicalItemId(
            @PathVariable(value = "id") final Long id,
            @RequestParam(value = "source", defaultValue = "true") final Boolean source,
            final HttpServletResponse response) throws IOException {
        final BiologicalDataItemFile biologicalDataItemFile = dataItemSecurityService.loadItemFile(id, source);
        writeStreamToResponse(response, biologicalDataItemFile.getContent(), biologicalDataItemFile.getFileName());
    }

    @ResponseBody
    @GetMapping("/dataitem/{id}/downloadUrl")
    @ApiOperation(
            value = "Generates download url for file specified by biological item id",
            notes = "Generates download url for file specified by biological item id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public final Result<BiologicalDataItemDownloadUrl> generateDownloadUrl(@PathVariable(value = "id") final Long id)
            throws AccessDeniedException {
        return Result.success(dataItemSecurityService.generateDownloadUrl(id));
    }

    @ResponseBody
    @GetMapping(value = "/dataitem/download/allowed")
    @ApiOperation(
            value = "Checks if files download is allowed",
            notes = "Returns true if files download is allowed and false if not",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> isFileDownloadAllowed() {
        return Result.success(dataItemManager.isFileDownloadAllowed());
    }
}
