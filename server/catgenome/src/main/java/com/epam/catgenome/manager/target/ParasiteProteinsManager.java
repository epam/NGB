/*
 * MIT License
 *
 * Copyright (c) 2024 EPAM Systems
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
package com.epam.catgenome.manager.target;

import com.epam.catgenome.controller.vo.TaskVO;
import com.epam.catgenome.entity.blast.BlastDatabase;
import com.epam.catgenome.entity.blast.BlastTask;
import com.epam.catgenome.entity.blast.BlastTaskStatus;
import com.epam.catgenome.entity.blast.result.BlastSequence;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.manager.blast.BlastDatabaseManager;
import com.epam.catgenome.manager.blast.BlastTaskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParasiteProteinsManager {
    private static final String BLASTP = "blastp";
    private static final String OPTIONS = "-max_target_seqs 100 -evalue 0.05 -num_threads 2";
    public static final String TASK_NAME = "TTD";
    public static final int MAX_RETRIES_COUNT = 100;
    public static final int TASK_STATUS_CHECK_RETRY_DELAY = 1000;
    private final BlastTaskManager blastTaskManager;
    private final BlastDatabaseManager blastDatabaseManager;

    @Value("${targets.parasite.proteins.blast.db:TTD}")
    private String parasiteProteinsDatabaseName;
    @Value("${targets.parasite.proteins.min.query.coverage:80}")
    private int minQueryCoverage;
    @Value("${targets.parasite.proteins.min.percent.identity:80}")
    private int minPercentIdentity;

    public List<String> getSequences(final String sequence) throws InterruptedException, BlastRequestException {
        final BlastDatabase database = getBlastDatabase();
        final BlastTask createdTask = blastTaskManager.create(getTaskVO(sequence, database.getId()));
        final long taskId = createdTask.getId();
        int retriesCount = 0;
        final List<BlastTaskStatus> errorStatuses = new ArrayList<>();
        errorStatuses.add(BlastTaskStatus.CANCELED);
        errorStatuses.add(BlastTaskStatus.FAILED);
        while (retriesCount < MAX_RETRIES_COUNT) {
            Thread.sleep(TASK_STATUS_CHECK_RETRY_DELAY);
            final BlastTask task = blastTaskManager.getBlastTask(taskId);
            Assert.isTrue(!errorStatuses.contains(task.getStatus()), "Parasite proteins search task failed.");
            if (task.getStatus().equals(BlastTaskStatus.DONE)) {
                break;
            } else {
                retriesCount++;
            }
        }
        final Collection<BlastSequence> result = blastTaskManager.getGroupedResult(taskId);
        return result.stream()
                .filter(s -> s.getQueryCoverage() >= minQueryCoverage &&
                        s.getPercentIdentity() >= minPercentIdentity)
                .map(BlastSequence::getSequenceId)
                .collect(Collectors.toList());
    }

    private static TaskVO getTaskVO(final String sequence, final Long databaseId) {
        return TaskVO.builder()
                .title(TASK_NAME)
                .algorithm(BLASTP)
                .executable(BLASTP)
                .query(sequence)
                .options(OPTIONS)
                .databaseId(databaseId)
                .build();
    }

    private BlastDatabase getBlastDatabase() {
        final List<BlastDatabase> databases = blastDatabaseManager.load(null, null);
        final BlastDatabase database = databases.stream()
                .filter(d -> d.getName().equals(parasiteProteinsDatabaseName))
                .findFirst().orElse(null);
        Assert.notNull(database, "Parasite protein sequences database not available");
        return database;
    }
}
