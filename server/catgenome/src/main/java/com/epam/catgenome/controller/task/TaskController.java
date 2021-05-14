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

package com.epam.catgenome.controller.task;

import java.io.IOException;
import java.util.Collection;

import com.epam.catgenome.controller.vo.TaskVO;
import com.epam.catgenome.entity.task.Task;
import com.epam.catgenome.manager.task.TaskSecurityService;
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
public class TaskController extends AbstractRESTController {

    @Autowired
    private TaskSecurityService taskSecurityService;

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
        return Result.success(taskSecurityService.load(taskId));
    }

    @RequestMapping(value = "/tasks", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
            value = "Loads all tasks.",
            notes = "Loads all tasks",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Collection<Task>> loadTasks() {
        return Result.success(taskSecurityService.loadAllTasks());
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
    public Result<Task> createTask(@RequestBody TaskVO taskVO) throws FeatureIndexException {
        return Result.success(taskSecurityService.create(taskVO));
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
        taskSecurityService.cancel(taskId);
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
        taskSecurityService.deleteTask(taskId);
        return Result.success(null);
    }
}
