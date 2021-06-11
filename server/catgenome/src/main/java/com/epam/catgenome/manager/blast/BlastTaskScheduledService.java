/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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

import com.epam.catgenome.dao.blast.BlastTaskDao;
import com.epam.catgenome.entity.blast.BlastTask;
import com.epam.catgenome.entity.blast.TaskStatus;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.manager.blast.dto.BlastRequestInfo;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.QueryParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.apache.commons.lang3.StringUtils.join;

@Service
@Slf4j
@RequiredArgsConstructor
public class BlastTaskScheduledService {
    private final BlastTaskManager blastTaskManager;
    private final BlastTaskDao blastTaskDao;
    private final BlastRequestManager blastRequestManager;


    @Scheduled(fixedRateString = "${blast.update.status.rate:60000}")
    public void updateTaskStatuses() {
        List<String> statuses = new ArrayList<>();
        statuses.add(String.valueOf(TaskStatus.CREATED.getId()));
        statuses.add(String.valueOf(TaskStatus.SUBMITTED.getId()));
        statuses.add(String.valueOf(TaskStatus.RUNNING.getId()));

        Filter filter = new Filter("status", "in", "(" + join(statuses, ",") + ")");
        QueryParameters parameters = new QueryParameters();
        parameters.setFilters(Collections.singletonList(filter));
        List<BlastTask> tasks = blastTaskDao.loadAllTasks(parameters);
        tasks.forEach(t -> {
            try {
                BlastRequestInfo blastRequestInfo = blastRequestManager.getTaskStatus(t.getId());
                String status = blastRequestInfo.getStatus();
                if (!status.equals(t.getStatus().name())) {
                    final TaskStatus newStatus = TaskStatus.valueOf(status);
                    t.setStatus(newStatus);
                    t.setStatusReason(blastRequestInfo.getReason());
                    if (newStatus.isFinal()) {
                        t.setEndDate(LocalDateTime.now());
                    }
                    blastTaskManager.updateTask(t);
                }
            } catch (BlastRequestException e) {
                log.debug(e.getMessage());
            }
        });
    }
}
