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

import com.epam.catgenome.dao.blast.BlastListSpeciesTaskDao;
import com.epam.catgenome.entity.blast.BlastListSpeciesTask;
import com.epam.catgenome.entity.blast.BlastTaskStatus;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.manager.blast.dto.BlastRequestInfo;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.QueryParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.join;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRED)
public class BlastListSpeciesScheduledService {
    private final BlastListSpeciesTaskDao updateTaskDao;
    private final BlastRequestManager blastRequestManager;
    private final BlastDatabaseManager databaseManager;

    @Scheduled(fixedRateString = "${blast.update.status.rate:60000}")
    public void updateTaskStatuses() {
        final List<String> statuses = BlastTaskStatus.getNotFinalStatuses();
        final Filter filter = Filter.builder()
                .field("status")
                .operator("in")
                .value("(" + join(statuses, ",") + ")")
                .build();
        final QueryParameters parameters = QueryParameters.builder()
                .filters(Collections.singletonList(filter))
                .build();
        final List<BlastListSpeciesTask> tasks = updateTaskDao.loadTasks(parameters);
        tasks.forEach(t -> {
            try {
                final BlastRequestInfo blastRequestInfo = blastRequestManager.getTaskStatus(t.getTaskId());
                final String status = blastRequestInfo.getStatus();
                if (!status.equals(t.getStatus().name())) {
                    final BlastTaskStatus newStatus = BlastTaskStatus.valueOf(status);
                    if (BlastTaskStatus.DONE.equals(newStatus)) {
                        databaseManager.saveDatabaseOrganisms(t.getTaskId(), t.getDatabaseId());
                    }
                    t.setStatus(newStatus);
                    t.setStatusReason(blastRequestInfo.getReason());
                    if (newStatus.isFinal()) {
                        t.setEndDate(LocalDateTime.now());
                    }
                    updateTaskDao.updateTask(t);
                }
            } catch (BlastRequestException | IOException e) {
                log.debug(e.getMessage());
            }
        });
    }
}
