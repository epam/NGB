/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2021 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */
package com.epam.catgenome.manager.heatmap;

import com.epam.catgenome.controller.vo.TaskVO;
import com.epam.catgenome.entity.blast.BlastTask;
import com.epam.catgenome.entity.blast.result.BlastSequence;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.manager.blast.BlastTaskManager;
import com.epam.catgenome.manager.blast.dto.BlastRequestResult;
import com.epam.catgenome.manager.blast.dto.TaskPage;
import com.epam.catgenome.security.acl.aspect.AclMask;
import com.epam.catgenome.security.acl.aspect.AclTree;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.QueryParameters;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_USER;

@Service
public class HeatmapSecurityService {

    @Autowired
    private BlastTaskManager blastTaskManager;

    @AclTree
    @AclMask
    @PreAuthorize(ROLE_USER)
    public BlastTask load(final Long taskId) {
        return blastTaskManager.loadTask(taskId);
    }

    @PreAuthorize(ROLE_USER)
    public BlastTask create(final TaskVO taskVO) throws BlastRequestException {
        return blastTaskManager.create(taskVO);
    }

    @PreAuthorize(ROLE_USER)
    public void deleteTask(final long taskId) throws IOException {
        blastTaskManager.deleteTask(taskId);
    }

    @PreAuthorize(ROLE_USER)
    public TaskPage loadAllTasks(final QueryParameters queryParameters) {
        return blastTaskManager.loadCurrentUserTasks(queryParameters);
    }

    @PreAuthorize(ROLE_USER)
    public long getTasksCount(final List<Filter> filters) {
        return blastTaskManager.getTasksCount(filters);
    }

    @PreAuthorize(ROLE_USER)
    public void cancel(final long taskId) throws BlastRequestException {
        blastTaskManager.cancelTask(taskId);
    }

    @PreAuthorize(ROLE_USER)
    public BlastRequestResult getResult(final long taskId) throws BlastRequestException {
        return blastTaskManager.getResult(taskId);
    }

    @PreAuthorize(ROLE_USER)
    public Collection<BlastSequence> getGroupedResult(final long taskId) throws BlastRequestException {
        return blastTaskManager.getGroupedResult(taskId);
    }

    @PreAuthorize(ROLE_USER)
    public ResponseBody getRawResult(final long taskId) throws BlastRequestException {
        return blastTaskManager.getRawResult(taskId);
    }

    @PreAuthorize(ROLE_USER)
    public void deleteTasks() {
        blastTaskManager.deleteTasks();
    }

}
