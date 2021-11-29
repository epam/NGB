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

package com.epam.catgenome.dao.blast;

import com.epam.catgenome.entity.blast.BlastDatabase;
import com.epam.catgenome.entity.blast.BlastDatabaseSource;
import com.epam.catgenome.entity.blast.BlastDatabaseType;
import com.epam.catgenome.entity.blast.BlastListSpeciesTask;
import com.epam.catgenome.entity.blast.BlastTaskStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
@Transactional
public class BlastListSpeciesTaskDaoTest extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired
    private BlastListSpeciesTaskDao listSpeciesTaskDao;
    @Autowired
    private BlastDatabaseDao  blastDatabaseDao;

    @Test
    public void testCreateTask() {
        BlastListSpeciesTask task = createTask();
        task = listSpeciesTaskDao.loadTask(task.getTaskId());
        Assert.assertNotNull(task);
    }

    @Test
    public void testDeleteTasks() {
        BlastListSpeciesTask task = createTask();
        task = listSpeciesTaskDao.loadTask(task.getTaskId());
        Assert.assertNotNull(task);
        listSpeciesTaskDao.deleteTasks();
        task = listSpeciesTaskDao.loadTask(task.getTaskId());
        Assert.assertNull(task);
    }

    private BlastListSpeciesTask createTask() {
        final BlastDatabase database = BlastDatabase.builder()
                .name("Human")
                .path("Human")
                .source(BlastDatabaseSource.CUSTOM)
                .type(BlastDatabaseType.NUCLEOTIDE)
                .build();
        blastDatabaseDao.saveDatabase(database);
        final BlastListSpeciesTask task = BlastListSpeciesTask.builder()
                .taskId(1L)
                .createdDate(LocalDateTime.now())
                .status(BlastTaskStatus.CREATED)
                .databaseId(database.getId())
                .build();
        listSpeciesTaskDao.saveTask(task);
        return task;
    }
}
