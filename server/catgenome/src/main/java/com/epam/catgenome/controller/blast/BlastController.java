/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

package com.epam.catgenome.controller.blast;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import com.epam.catgenome.controller.vo.TaskVO;
import com.epam.catgenome.entity.blast.BlastDataBase;
import com.epam.catgenome.entity.blast.BlastTask;
import com.epam.catgenome.manager.blast.dto.BlastRequestResult;
import com.epam.catgenome.manager.blast.dto.TaskPage;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.QueryParameters;
import com.epam.catgenome.manager.blast.BlastTaskSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
@Api(value = "blast", description = "BLAST Task Management")
@RequiredArgsConstructor
public class BlastController extends AbstractRESTController {

    private final BlastTaskSecurityService blastTaskSecurityService;

    @GetMapping(value = "/task/{taskId}")
    @ResponseBody
    @ApiOperation(
            value = "Returns a task by given id",
            notes = "Returns a task by given id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<BlastTask> loadTask(@PathVariable final long taskId) {
        return Result.success(blastTaskSecurityService.load(taskId));
    }

    @GetMapping(value = "/task/{taskId}/result")
    @ResponseBody
    @ApiOperation(
            value = "Returns a task result",
            notes = "Returns a task result",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<BlastRequestResult> getResult(@PathVariable final long taskId) throws BlastRequestException {
        return Result.success(blastTaskSecurityService.getResult(taskId));
    }

    @GetMapping(value = "/task/{taskId}/raw")
    @ResponseBody
    @ApiOperation(
            value = "Returns a file with task result",
            notes = "Returns a file with task result",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public void getRawResult(@PathVariable final long taskId, final HttpServletResponse response)
            throws BlastRequestException, IOException {
        okhttp3.ResponseBody body = blastTaskSecurityService.getRawResult(taskId);
        InputStream is = body.byteStream();
        org.apache.commons.io.IOUtils.copy(is, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping(value = "/tasks/count")
    @ResponseBody
    @ApiOperation(
            value = "Returns tasks count",
            notes = "Returns tasks count",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Long> getTasksCount(@RequestBody final List<Filter> filters) {
        return Result.success(blastTaskSecurityService.getTasksCount(filters));
    }

    @PostMapping(value = "/tasks")
    @ResponseBody
    @ApiOperation(
            value = "Loads all tasks",
            notes = "DB fields mapping: id - task_id, "
                    + "createdDate - created_date, "
                    + "endDate - end_date, statusReason - status_reason",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<TaskPage> loadTasks(@RequestBody final QueryParameters queryParameters) {
        return Result.success(blastTaskSecurityService.loadAllTasks(queryParameters));
    }

    @PostMapping(value = "/task")
    @ResponseBody
    @ApiOperation(
            value = "Creates new task or updates existing one",
            notes = "Creates new task or updates existing one",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<BlastTask> createTask(@RequestBody final TaskVO taskVO)
            throws FeatureIndexException, BlastRequestException {
        return Result.success(blastTaskSecurityService.create(taskVO));
    }

    @PutMapping(value = "/task/{taskId}/cancel")
    @ResponseBody
    @ApiOperation(
            value = "Cancels a task with given id",
            notes = "Cancels a task with given id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> cancelTask(@PathVariable final long taskId) throws BlastRequestException {
        blastTaskSecurityService.cancel(taskId);
        return Result.success(null);
    }

    @DeleteMapping(value = "/task/{taskId}")
    @ResponseBody
    @ApiOperation(
            value = "Deletes a task, specified by task ID",
            notes = "Deletes a task, specified by task ID",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> deleteTask(@PathVariable final long taskId) throws IOException {
        blastTaskSecurityService.deleteTask(taskId);
        return Result.success(null);
    }

    @GetMapping(value = {"/databases/{type}", "/database"})
    @ResponseBody
    @ApiOperation(
            value = "Returns databases by type",
            notes = "Returns databases by type",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<BlastDataBase>> loadDataBases(@PathVariable final Optional<Long> type) {
        return Result.success(blastTaskSecurityService.loadDataBases(type));
    }
}
