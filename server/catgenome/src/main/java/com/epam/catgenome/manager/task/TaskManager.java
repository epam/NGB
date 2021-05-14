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
package com.epam.catgenome.manager.task;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.task.TaskDao;
import com.epam.catgenome.entity.task.Organism;
import com.epam.catgenome.entity.task.Task;
import com.epam.catgenome.entity.task.TaskParameter;
import com.epam.catgenome.entity.task.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskManager {
    @Autowired
    private TaskDao taskDao;

    @Transactional(propagation = Propagation.REQUIRED)
    public Task load(Long taskId) {
        Task task = taskDao.loadTaskById(taskId);
        Assert.notNull(task, MessageHelper.getMessage(MessagesConstants.ERROR_TASK_NOT_FOUND, taskId));

        loadTaskOrganisms(task);
        loadTaskParameters(task);

        return task;
    }

    private void loadTaskOrganisms(Task task) {
        List<Organism> organismList = taskDao.loadOrganisms(task.getId());
        List<String> organisms = organismList.stream().map(Organism::getOrganism).collect(Collectors.toList());
        task.setOrganisms(organisms);
    }

    private void loadTaskParameters(Task task) {
        List<TaskParameter> parametersList = taskDao.loadTaskParameters(task.getId());
        Map<String, String> parameters = parametersList.stream().collect(Collectors.toMap(TaskParameter::getParameter, TaskParameter::getValue));
        task.setParameters(parameters);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Task create(final Task task) {
        taskDao.saveTask(task);
        taskDao.saveOrganisms(task.getId(), task.getOrganisms());
        taskDao.saveTaskParameters(task.getId(), task.getParameters());
        return task;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteTask(long taskId) {
        taskDao.deleteOrganisms(taskId);
        taskDao.deleteParameters(taskId);
        taskDao.deleteTask(taskId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public List<Task> loadAllTasks() {

        return taskDao.loadAllTasks();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void cancelTask(long taskId) {
        Task task = taskDao.loadTaskById(taskId);
        Assert.notNull(task, MessageHelper.getMessage(MessagesConstants.ERROR_TASK_NOT_FOUND, taskId));
        task.setStatus(TaskStatus.CANCELED);
        taskDao.saveTask(task);
    }
}
