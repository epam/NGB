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

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.TaskVO;
import com.epam.catgenome.dao.blast.BlastTaskDao;
import com.epam.catgenome.entity.blast.BlastDataBase;
import com.epam.catgenome.entity.blast.BlastTask;
import com.epam.catgenome.entity.blast.BlastTaskOrganism;
import com.epam.catgenome.entity.blast.TaskParameter;
import com.epam.catgenome.entity.blast.TaskStatus;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.manager.AuthManager;
import com.epam.catgenome.manager.blast.dto.BlastRequest;
import com.epam.catgenome.manager.blast.dto.BlastRequestInfo;
import com.epam.catgenome.manager.blast.dto.BlastRequestResult;
import com.epam.catgenome.manager.blast.dto.TaskPage;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.QueryParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlastTaskManager {

    public static final String MAX_TARGET_SEQUENCE = "MAX_TARGET_SEQUENCE";
    public static final String EXPECTED_THRESHOLD = "EXPECTED_THRESHOLD";

    private final BlastTaskDao blastTaskDao;

    private final BlastRequestManager blastRequestManager;

    private final AuthManager authManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public BlastTask load(final Long taskId) {
        BlastTask blastTask = blastTaskDao.loadTaskById(taskId);
        Assert.notNull(blastTask, MessageHelper.getMessage(MessagesConstants.ERROR_TASK_NOT_FOUND, taskId));

        loadTaskOrganisms(blastTask);
        loadTaskParameters(blastTask);

        return blastTask;
    }

    private void loadTaskOrganisms(final BlastTask blastTask) {
        List<BlastTaskOrganism> blastTaskOrganismList = blastTaskDao.loadOrganisms(blastTask.getId());
        List<Long> organisms = blastTaskOrganismList.stream().
                map(BlastTaskOrganism::getOrganism).collect(Collectors.toList());
        blastTask.setOrganisms(organisms);
    }

    private void loadTaskParameters(final BlastTask blastTask) {
        List<TaskParameter> parametersList = blastTaskDao.loadTaskParameters(blastTask.getId());
        Map<String, String> parameters = parametersList.stream().collect(Collectors.
                toMap(TaskParameter::getParameter, TaskParameter::getValue));
        blastTask.setParameters(parameters);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public BlastTask create(final TaskVO taskVO) throws BlastRequestException {
        if ((taskVO.getOrganisms() == null || taskVO.getOrganisms().isEmpty())
            && (taskVO.getExcludedOrganisms() == null || taskVO.getExcludedOrganisms().isEmpty())) {
            Assert.isTrue(false, MessageHelper.getMessage(MessagesConstants.ERROR_BLAST_ORGANISMS));
        }
        BlastRequest blastRequest = new BlastRequest();
        blastRequest.setAlgorithm(taskVO.getAlgorithm());
        blastRequest.setBlastTool(taskVO.getExecutable());
        blastRequest.setDbName(taskVO.getDatabase());
        blastRequest.setQuery(taskVO.getQuery());
        blastRequest.setOptions(taskVO.getOptions());
        blastRequest.setTaxIds(taskVO.getOrganisms());
        blastRequest.setExcludedTaxIds(taskVO.getExcludedOrganisms());
        Assert.isTrue(taskVO.getParameters().containsKey(MAX_TARGET_SEQUENCE),
                MessageHelper.getMessage(MessagesConstants.ERROR_BLAST_TASK_PARAMETERS));
        Assert.isTrue(taskVO.getParameters().containsKey(EXPECTED_THRESHOLD),
                MessageHelper.getMessage(MessagesConstants.ERROR_BLAST_TASK_PARAMETERS));
        blastRequest.setMaxTargetSequence(Long.parseLong(taskVO.getParameters().get(MAX_TARGET_SEQUENCE)));
        blastRequest.setExpectedThreshold(Long.parseLong(taskVO.getParameters().get(EXPECTED_THRESHOLD)));
        BlastRequestInfo blastRequestInfo = blastRequestManager.createTask(blastRequest);
        if (!blastRequestInfo.getStatus().equals("ERROR")) {
            BlastTask blastTask = new BlastTask();
            blastTask.setId(blastRequestInfo.getRequestId());
            blastTask.setQuery(taskVO.getQuery());
            blastTask.setDatabase(taskVO.getDatabase());
            blastTask.setOrganisms(taskVO.getOrganisms());
            blastTask.setExcludedOrganisms(taskVO.getExcludedOrganisms());
            blastTask.setExecutable(taskVO.getExecutable());
            blastTask.setAlgorithm(taskVO.getAlgorithm());
            blastTask.setParameters(taskVO.getParameters());
            blastTask.setOptions(taskVO.getOptions());
            blastTask.setStatus(TaskStatus.valueOf(blastRequestInfo.getStatus()));
            blastTask.setCreatedDate(LocalDateTime.parse(blastRequestInfo.getCreatedDate(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
            blastTask.setOwner(authManager.getAuthorizedUser());
            blastTaskDao.saveTask(blastTask);
            blastTaskDao.saveOrganisms(blastTask.getId(), blastTask.getOrganisms());
            blastTaskDao.saveExclOrganisms(blastTask.getId(), blastTask.getExcludedOrganisms());
            blastTaskDao.saveTaskParameters(blastTask.getId(), blastTask.getParameters());
            return blastTask;
        }
        return null;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteTask(final long taskId) {
        BlastTask blastTask = blastTaskDao.loadTaskById(taskId);
        Assert.notNull(blastTask, MessageHelper.getMessage(MessagesConstants.ERROR_TASK_NOT_FOUND, taskId));
        Assert.isTrue(blastTask.getStatus().equals(TaskStatus.RUNNING),
                MessageHelper.getMessage(MessagesConstants.ERROR_TASK_CAN_NOT_BE_DELETED, taskId));
        blastTaskDao.deleteOrganisms(taskId);
        blastTaskDao.deleteExclOrganisms(taskId);
        blastTaskDao.deleteParameters(taskId);
        blastTaskDao.deleteTask(taskId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public TaskPage loadAllTasks(final QueryParameters queryParameters) {
        TaskPage taskPage = new TaskPage();
        long totalCount = blastTaskDao.getTasksCount(queryParameters.getFilters());
        List<BlastTask> blastTasks = blastTaskDao.loadAllTasks(queryParameters);
        taskPage.setTotalCount(totalCount);
        taskPage.setBlastTasks(blastTasks);
        return taskPage;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateTask(final BlastTask blastTask) {
        blastTaskDao.updateTask(blastTask);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public long getTasksCount(final List<Filter> filters) {
        return blastTaskDao.getTasksCount(filters);
    }

    public void cancelTask(final long id) throws BlastRequestException {
        BlastTask blastTask = blastTaskDao.loadTaskById(id);
        Assert.notNull(blastTask, MessageHelper.getMessage(MessagesConstants.ERROR_TASK_NOT_FOUND, id));
        BlastRequestInfo blastRequestInfo = blastRequestManager.cancelTask(id);
        if (blastRequestInfo.getStatus().equals("CANCELED")) {
            blastTask.setStatus(TaskStatus.CANCELED);
            blastTaskDao.updateTask(blastTask);
        }
    }

    public BlastRequestResult getResult(final long taskId) throws BlastRequestException {
        return blastRequestManager.getResult(taskId);
    }

    public ResponseBody getRawResult(final long taskId) throws BlastRequestException {
        return blastRequestManager.getRawResult(taskId);
    }

    public List<BlastDataBase> loadDataBases(final Optional<Long> type) {
        return Collections.singletonList(new BlastDataBase(1L, "Homo_sapiens.GRCh38",
                "Homo_sapiens.GRCh38", "NUCLEOTIDE"));
    }
}
