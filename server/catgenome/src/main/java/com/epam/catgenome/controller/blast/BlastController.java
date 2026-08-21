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
import java.util.Collection;
import java.util.List;

import com.epam.catgenome.controller.vo.TaskVO;
import com.epam.catgenome.manager.blast.dto.CreateDatabaseRequest;
import com.epam.catgenome.entity.blast.BlastTask;
import com.epam.catgenome.entity.blast.result.BlastSequence;
import com.epam.catgenome.manager.blast.dto.BlastRequestResult;
import com.epam.catgenome.manager.blast.dto.CreateDatabaseResponse;
import com.epam.catgenome.manager.blast.dto.TaskPage;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.QueryParameters;
import com.epam.catgenome.manager.blast.BlastTaskSecurityService;
import lombok.RequiredArgsConstructor;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.exception.FeatureIndexException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@Tag(name = "blast", description = "BLAST Task Management")
@RequiredArgsConstructor
public class BlastController extends AbstractRESTController {

    private final BlastTaskSecurityService blastTaskSecurityService;

    @GetMapping(value = "/task/{taskId}")
    @Operation(
            summary = "Returns a task by given id",
            description = "Returns a task by given id")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<BlastTask> loadTask(@PathVariable final long taskId) {
        return Result.success(blastTaskSecurityService.load(taskId));
    }

    @GetMapping(value = "/task/{taskId}/result")
    @Operation(
            summary = "Returns a task result",
            description = "Returns a task result")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<BlastRequestResult> getResult(@PathVariable final long taskId) throws BlastRequestException {
        return Result.success(blastTaskSecurityService.getResult(taskId));
    }

    @GetMapping(value = "/task/{taskId}/raw")
    @Operation(
            summary = "Returns a file with task result",
            description = "Returns a file with task result")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public void getRawResult(@PathVariable final long taskId, final HttpServletResponse response)
            throws BlastRequestException, IOException {
        okhttp3.ResponseBody body = blastTaskSecurityService.getRawResult(taskId);
        InputStream is = body.byteStream();
        org.apache.commons.io.IOUtils.copy(is, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping(value = "/task/{taskId}/group")
    @Operation(
            summary = "Returns BLAST tasks results grouped by sequence",
            description = "Returns BLAST tasks results grouped by sequence")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<Collection<BlastSequence>> getGroupedResult(@PathVariable final long taskId)
            throws BlastRequestException {
        return Result.success(blastTaskSecurityService.getGroupedResult(taskId));
    }

    @PostMapping(value = "/tasks/count")
    @Operation(
            summary = "Returns tasks count",
            description = "Returns tasks count")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<Long> getTasksCount(@RequestBody final List<Filter> filters) {
        return Result.success(blastTaskSecurityService.getTasksCount(filters));
    }

    @PostMapping(value = "/tasks")
    @Operation(
            summary = "Loads all tasks",
            description = "DB fields mapping: id - task_id, "
                    + "createdDate - created_date, "
                    + "endDate - end_date, statusReason - status_reason")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<TaskPage> loadTasks(@RequestBody final QueryParameters queryParameters) {
        return Result.success(blastTaskSecurityService.loadAllTasks(queryParameters));
    }

    @DeleteMapping(value = "/tasks")
    @Operation(
            summary = "Delete all not running tasks for current user",
            description = "Delete all not running tasks for current user")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> deleteTasks() {
        blastTaskSecurityService.deleteTasks();
        return Result.success(null);
    }

    @PostMapping(value = "/task")
    @Operation(
            summary = "Creates new task or updates existing one",
            description = "Creates new task or updates existing one")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<BlastTask> createTask(@RequestBody final TaskVO taskVO)
            throws FeatureIndexException, BlastRequestException {
        return Result.success(blastTaskSecurityService.create(taskVO));
    }

    @PostMapping(value = "/createdb")
    @Operation(
            summary = "Schedules a task for BLAST database creation",
            description = "Schedules a task for BLAST database creation")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<CreateDatabaseResponse> createDatabase(@RequestBody final CreateDatabaseRequest request)
            throws BlastRequestException {
        return Result.success(blastTaskSecurityService.createDatabase(request));
    }

    @PutMapping(value = "/task/{taskId}/cancel")
    @Operation(
            summary = "Cancels a task with given id",
            description = "Cancels a task with given id")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> cancelTask(@PathVariable final long taskId) throws BlastRequestException {
        blastTaskSecurityService.cancel(taskId);
        return Result.success(null);
    }

    @DeleteMapping(value = "/task/{taskId}")
    @Operation(
            summary = "Deletes a task, specified by task ID",
            description = "Deletes a task, specified by task ID")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> deleteTask(@PathVariable final long taskId) throws IOException {
        blastTaskSecurityService.deleteTask(taskId);
        return Result.success(null);
    }
}
