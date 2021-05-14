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

package com.epam.catgenome.dao;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;

import com.epam.catgenome.dao.task.TaskDao;
import com.epam.catgenome.entity.task.Task;
import com.epam.catgenome.entity.task.TaskStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class TaskDaoTest extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired
    private TaskDao taskDao;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveTask() {
        Task task = new Task();
        task.setTitle("test");
        task.setStatus(TaskStatus.CREATED);
        task.setCreatedDate(new Date());

        taskDao.saveTask(task);

        Task loadedTask = taskDao.loadTaskById(task.getId());
        Assert.assertNotNull(loadedTask);
        Assert.assertEquals(task.getId(), loadedTask.getId());
        Assert.assertEquals(task.getTitle(), loadedTask.getTitle());
        Assert.assertEquals(task.getCreatedDate(), loadedTask.getCreatedDate());
    }

    @Test
    public void testUpdateTask() {
        Task task = new Task();
        task.setTitle("test");
        task.setStatus(TaskStatus.CREATED);
        task.setCreatedDate(new Date());

        taskDao.saveTask(task);

        task.setStatus(TaskStatus.CANCELED);
        taskDao.saveTask(task);

        Task loadedTask = taskDao.loadTaskById(task.getId());
        Assert.assertNotNull(loadedTask);
        Assert.assertEquals(task.getId(), loadedTask.getId());
        Assert.assertEquals(task.getTitle(), loadedTask.getTitle());
        Assert.assertEquals(task.getCreatedDate(), loadedTask.getCreatedDate());
    }
}
