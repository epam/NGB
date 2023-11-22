/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
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
import com.epam.catgenome.entity.externaldb.target.UrlEntity;
import com.epam.catgenome.entity.target.GeneSequences;
import com.epam.catgenome.entity.target.PatentsSearchStatus;
import com.epam.catgenome.entity.target.Target;
import com.epam.catgenome.entity.target.TargetGene;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.blast.BlastDatabaseManager;
import com.epam.catgenome.manager.blast.BlastTaskManager;
import com.epam.catgenome.manager.blast.dto.BlastRequestResult;
import com.epam.catgenome.manager.externaldb.ncbi.NCBISequencesManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.epam.catgenome.manager.externaldb.ncbi.NCBISequencesManager.getFastaMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class PatentsSearchManager {
    public static final String TASK_NAME = "%d_%s";
    private final TargetManager targetManager;
    private final LaunchIdentificationManager launchIdentificationManager;
    private final NCBISequencesManager sequencesManager;
    private final BlastTaskManager blastTaskManager;
    private final BlastDatabaseManager blastDatabaseManager;

    public void searchPatents() {
        final List<BlastDatabase> databases = blastDatabaseManager.load(null, null);
        final BlastDatabase database = databases.stream()
                .filter(d -> d.getName().equals("pataa"))
                .findFirst().orElse(null);
        Assert.notNull(database, "Patented protein sequences database not available");
        final List<Target> targets = targetManager.getTargetsForPatentsSearch();
        for (Target target: targets) {
            target.setPatentsSearchStatus(PatentsSearchStatus.IN_PROGRESS);
            targetManager.updatePatentsSearchStatus(target);
            try {
                process(target, database.getId());
                target.setPatentsSearchStatus(PatentsSearchStatus.COMPLETED);
            } catch (IOException | ParseException | InterruptedException | ExternalDbUnavailableException e) {
                log.debug(e.getMessage());
                target.setPatentsSearchStatus(PatentsSearchStatus.ERROR);
            } finally {
                targetManager.updatePatentsSearchStatus(target);
            }
        }
    }

    public BlastRequestResult getPatents(final Long targetId, final String sequenceId) throws BlastRequestException {
        final BlastTask task = blastTaskManager.loadTask(getTaskName(targetId, sequenceId));
        Assert.notNull(task.getId(), "Blast task not found.");
        return blastTaskManager.getResult(task.getId());
    }

    private void process(final Target target, final long databaseId) throws ParseException, IOException,
            InterruptedException, ExternalDbUnavailableException {
        log.debug("Looking for protein patents for target {}", target.getTargetName());
        final List<String> geneIds = target.getTargetGenes().stream()
                .map(TargetGene::getGeneId)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(geneIds)) {
            return;
        }
        final List<GeneSequences> sequences = launchIdentificationManager.getGeneSequences(geneIds);

        final List<String> proteinIds = sequences.stream()
                .map(geneSequences -> geneSequences.getProteins().stream()
                        .map(UrlEntity::getId)
                        .collect(Collectors.toList()))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        final String fasta = sequencesManager.getProteinsFasta(proteinIds);
        final Map<String, String> proteins = getFastaMap(fasta);

        proteins.forEach((k, v) -> {
            TaskVO taskVO = TaskVO.builder()
                    .title(getTaskName(target.getTargetId(), k))
                    .algorithm("blastp")
                    .query(v)
                    .databaseId(databaseId)
                    .build();
            try {
                blastTaskManager.create(taskVO);
            } catch (BlastRequestException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String getTaskName(final long targetId, final String sequenceId) {
        return String.format(TASK_NAME, targetId, sequenceId);
    }
}
