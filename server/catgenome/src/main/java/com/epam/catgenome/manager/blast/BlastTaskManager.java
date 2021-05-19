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
package com.epam.catgenome.manager.blast;

import com.epam.catgenome.manager.blast.dto.Request;
import com.epam.catgenome.manager.blast.dto.RequestInfo;
import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.blast.BlastTaskDao;
import com.epam.catgenome.entity.blast.*;
import com.epam.catgenome.manager.blast.dto.TaskPage;
import com.epam.catgenome.manager.blast.dto.TaskResult;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.manager.AuthManager;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.QueryParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

@Service
public class BlastTaskManager {
    private static final int CHECK_TASK_STATUS_PERIOD_MS = 10000;
    @Autowired
    private BlastTaskDao blastTaskDao;
    @Autowired
    private BlastRequestManager blastRequestManager;
    @Autowired
    private AuthManager authManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public Task load(Long taskId) {
        Task task = blastTaskDao.loadTaskById(taskId);
        Assert.notNull(task, MessageHelper.getMessage(MessagesConstants.ERROR_TASK_NOT_FOUND, taskId));

        loadTaskOrganisms(task);
        loadTaskParameters(task);

        return task;
    }

    private void loadTaskOrganisms(Task task) {
        List<Organism> organismList = blastTaskDao.loadOrganisms(task.getId());
        List<Long> organisms = organismList.stream().map(Organism::getOrganism).collect(Collectors.toList());
        task.setOrganisms(organisms);
    }

    private void loadTaskParameters(Task task) {
        List<TaskParameter> parametersList = blastTaskDao.loadTaskParameters(task.getId());
        Map<String, String> parameters = parametersList.stream().collect(Collectors.toMap(TaskParameter::getParameter, TaskParameter::getValue));
        task.setParameters(parameters);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Task create(final Task task) throws BlastRequestException {
        Request request = new Request();
        request.setAlgorithm(task.getAlgorithm());
        request.setBlastTool("BLAST_TOOL");
        request.setDbName(task.getDatabase());
        request.setQuery(task.getQuery());
        request.setOptions(task.getOptions());
        request.setTaxIds(task.getOrganisms());
        request.setExcludedTaxIds(task.getExcludedOrganisms());
        RequestInfo requestInfo = blastRequestManager.createTask(request);
        task.setId((long) requestInfo.getPayload().getRequestId());
        task.setStatus(TaskStatus.valueOf(requestInfo.getPayload().getStatus()));
        task.setOwner(authManager.getAuthorizedUser());
        blastTaskDao.saveTask(task);
        blastTaskDao.saveOrganisms(task.getId(), task.getOrganisms());
        blastTaskDao.saveExclOrganisms(task.getId(), task.getExcludedOrganisms());
        blastTaskDao.saveTaskParameters(task.getId(), task.getParameters());
        new Thread(() -> {
            try {
                checkTaskStatus(requestInfo.getPayload().getRequestId());
            } catch (BlastRequestException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        return task;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteTask(long taskId) {
        Task task = blastTaskDao.loadTaskById(taskId);
        Assert.notNull(task, MessageHelper.getMessage(MessagesConstants.ERROR_TASK_NOT_FOUND, taskId));
        Assert.isTrue(task.getStatus().equals(TaskStatus.RUNNING), MessageHelper.getMessage(MessagesConstants.ERROR_TASK_CAN_NOT_BE_DELETED, taskId));
        blastTaskDao.deleteOrganisms(taskId);
        blastTaskDao.deleteExclOrganisms(taskId);
        blastTaskDao.deleteParameters(taskId);
        blastTaskDao.deleteTask(taskId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public TaskPage loadAllTasks(QueryParameters queryParameters) {
        TaskPage taskPage = new TaskPage();
        long totalCount = blastTaskDao.getTasksCount(queryParameters.getFilters());
        List<Task> tasks = blastTaskDao.loadAllTasks(queryParameters);
        taskPage.setTotalCount(totalCount);
        taskPage.setTasks(tasks);
        return taskPage;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateTaskStatus(TaskStatus status, String statusReason, long id) {
        Task task = blastTaskDao.loadTaskById(id);
        Assert.notNull(task, MessageHelper.getMessage(MessagesConstants.ERROR_TASK_NOT_FOUND, id));
        task.setStatus(status);
        task.setStatusReason(statusReason);
        blastTaskDao.updateTaskStatus(task);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public long getTasksCount(List<Filter> filters) {
        return blastTaskDao.getTasksCount(filters);
    }

    public RequestInfo updateTaskStatus(long id) throws BlastRequestException {
        RequestInfo requestInfo = blastRequestManager.getTaskStatus((int) id);
        Task task = blastTaskDao.loadTaskById(id);
        Assert.notNull(task, MessageHelper.getMessage(MessagesConstants.ERROR_TASK_NOT_FOUND, id));
        if (!requestInfo.getPayload().getStatus().equals(task.getStatus().name())) {
            task.setStatus(TaskStatus.getById(Long.valueOf(requestInfo.getStatus())));
            blastTaskDao.updateTaskStatus(task);
        }
        return requestInfo;
    }

    public void checkTaskStatus(long id) throws BlastRequestException, InterruptedException {
        RequestInfo task = updateTaskStatus(id);
        while (task.getPayload().getStatus().equals(TaskStatus.RUNNING.name())) {
            sleep(CHECK_TASK_STATUS_PERIOD_MS);
            task = updateTaskStatus(id);
        }
    }

    public TaskResult getResult(long taskId) throws BlastRequestException {
        return blastRequestManager.getResult(taskId);
    }
}
