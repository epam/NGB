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

package com.epam.catgenome.controller.heatmap;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.TaskVO;
import com.epam.catgenome.entity.blast.BlastTask;
import com.epam.catgenome.entity.blast.result.BlastSequence;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.manager.blast.dto.BlastRequestResult;
import com.epam.catgenome.manager.heatmap.HeatmapSecurityService;
import com.epam.catgenome.util.db.Filter;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

@RestController
@Api(value = "heatmap", description = "Heatmap files Management")
@RequiredArgsConstructor
public class HeatmapController extends AbstractRESTController {

    private final HeatmapSecurityService heatmapSecurityService;

    @GetMapping(value = "/task/{taskId}")
    @ApiOperation(
            value = "Returns a task by given id",
            notes = "Returns a task by given id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<BlastTask> loadTask(@PathVariable final long taskId) {
        return Result.success(heatmapSecurityService.load(taskId));
    }

    @GetMapping(value = "/task/{taskId}/result")
    @ApiOperation(
            value = "Returns a task result",
            notes = "Returns a task result",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<BlastRequestResult> getResult(@PathVariable final long taskId) throws BlastRequestException {
        return Result.success(heatmapSecurityService.getResult(taskId));
    }

    @GetMapping(value = "/task/{taskId}/raw")
    @ApiOperation(
            value = "Returns a file with task result",
            notes = "Returns a file with task result",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public void getRawResult(@PathVariable final long taskId, final HttpServletResponse response)
            throws BlastRequestException, IOException {
        okhttp3.ResponseBody body = heatmapSecurityService.getRawResult(taskId);
        InputStream is = body.byteStream();
        org.apache.commons.io.IOUtils.copy(is, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping(value = "/task/{taskId}/group")
    @ApiOperation(
            value = "Returns BLAST tasks results grouped by sequence",
            notes = "Returns BLAST tasks results grouped by sequence",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Collection<BlastSequence>> getGroupedResult(@PathVariable final long taskId)
            throws BlastRequestException {
        return Result.success(heatmapSecurityService.getGroupedResult(taskId));
    }

    @PostMapping(value = "/tasks/count")
    @ApiOperation(
            value = "Returns tasks count",
            notes = "Returns tasks count",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Long> getTasksCount(@RequestBody final List<Filter> filters) {
        return Result.success(heatmapSecurityService.getTasksCount(filters));
    }

    @DeleteMapping(value = "/tasks")
    @ApiOperation(
            value = "Delete all not running tasks for current user",
            notes = "Delete all not running tasks for current user",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> deleteTasks() {
        heatmapSecurityService.deleteTasks();
        return Result.success(null);
    }

    @PostMapping(value = "/heatmap")
    @ApiOperation(
            value = "Registers new heatmap",
            notes = "Registers new heatmap",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<BlastTask> createTask(@RequestBody final TaskVO taskVO)
            throws FeatureIndexException, BlastRequestException {
        return Result.success(heatmapSecurityService.create(taskVO));
    }

    @PutMapping(value = "/task/{taskId}/cancel")
    @ApiOperation(
            value = "Cancels a task with given id",
            notes = "Cancels a task with given id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> cancelTask(@PathVariable final long taskId) throws BlastRequestException {
        heatmapSecurityService.cancel(taskId);
        return Result.success(null);
    }

    @DeleteMapping(value = "/task/{taskId}")
    @ApiOperation(
            value = "Deletes a task, specified by task ID",
            notes = "Deletes a task, specified by task ID",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> deleteTask(@PathVariable final long taskId) throws IOException {
        heatmapSecurityService.deleteTask(taskId);
        return Result.success(null);
    }
}
