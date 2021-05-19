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
import java.util.List;

import com.epam.catgenome.controller.vo.TaskVO;
import com.epam.catgenome.entity.blast.Task;
import com.epam.catgenome.manager.blast.dto.TaskPage;
import com.epam.catgenome.manager.blast.dto.TaskResult;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.QueryParameters;
import com.epam.catgenome.manager.blast.BlastTaskSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.exception.FeatureIndexException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Controller
@Api(value = "task", description = "Task Management")
public class BlastController extends AbstractRESTController {

    @Autowired
    private BlastTaskSecurityService blastTaskSecurityService;

    @RequestMapping(value = "/task/{taskId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
            value = "Returns a task by given id",
            notes = "Returns a task by given id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Task> loadTask(@PathVariable long taskId) {
        return Result.success(blastTaskSecurityService.load(taskId));
    }

    @RequestMapping(value = "/task/result/{taskId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
            value = "Returns a task result",
            notes = "Returns a task result",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<TaskResult> getResult(@PathVariable long taskId) throws BlastRequestException {
        return Result.success(blastTaskSecurityService.getResult(taskId));
    }

    @RequestMapping(value = "/tasks/count", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Returns tasks count",
            notes = "Returns tasks count",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Long> getTasksCount(@RequestBody List<Filter> filters) {
        return Result.success(blastTaskSecurityService.getTasksCount(filters));
    }

    @RequestMapping(value = "/tasks", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Loads all tasks.",
            notes = "DB fields mapping: id - task_id, createdDate - created_date, endDate - end_date, statusReason - status_reason",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<TaskPage> loadTasks(@RequestBody QueryParameters queryParameters) {
        return Result.success(blastTaskSecurityService.loadAllTasks(queryParameters));
    }

    @RequestMapping(value = "/task", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Creates new task or updates existing one",
            notes = "Creates new task or updates existing one",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Task> createTask(@RequestBody TaskVO taskVO) throws FeatureIndexException, BlastRequestException {
        return Result.success(blastTaskSecurityService.create(taskVO));
    }

    @RequestMapping(value = "/task/cancel/{taskId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
            value = "Cancels a task with given id",
            notes = "Cancels a task with given id",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> cancelTask(@PathVariable long taskId) {
        blastTaskSecurityService.cancel(taskId);
        return Result.success(null);
    }

    @RequestMapping(value = "/task/{taskId}", method = RequestMethod.DELETE)
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
}
