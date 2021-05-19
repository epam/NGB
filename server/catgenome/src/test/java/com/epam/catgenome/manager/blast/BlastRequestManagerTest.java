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
import com.epam.catgenome.manager.blast.dto.TaskResult;
import com.epam.catgenome.exception.BlastRequestException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
public class BlastRequestManagerTest {

    @Spy
    private BlastRequestManager blastRequestManager;

    @Test
    @Ignore
    public void testCreateTask() throws BlastRequestException {
        Request request = new Request();
        request.setBlastTool("Blast Tool");
        request.setAlgorithm("Algorithm");
        request.setDbName("DB");
        request.setQuery("Query");
        request.setOptions("Options");
        RequestInfo task = blastRequestManager.createTask(request);
        assertNotNull(task);
    }

    @Test
    @Ignore
    public void testGetTask() throws BlastRequestException {
        RequestInfo task = blastRequestManager.getTaskStatus(7);
        System.out.println(task);
        assertNotNull(task);
    }

    @Test
    @Ignore
    public void testGetTaskResult() throws BlastRequestException {
        TaskResult result = blastRequestManager.getResult(8);
        System.out.println(result);
        assertNotNull(result);
    }
}