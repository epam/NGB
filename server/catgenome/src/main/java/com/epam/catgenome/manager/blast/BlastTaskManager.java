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
import com.epam.catgenome.entity.blast.BlastDatabase;
import com.epam.catgenome.entity.blast.BlastTask;
import com.epam.catgenome.entity.blast.BlastTaskOrganism;
import com.epam.catgenome.entity.blast.BlastTaskStatus;
import com.epam.catgenome.entity.blast.TaskParameter;
import com.epam.catgenome.entity.blast.result.BlastSequence;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.manager.AuthManager;
import com.epam.catgenome.manager.blast.dto.BlastRequest;
import com.epam.catgenome.manager.blast.dto.BlastRequestInfo;
import com.epam.catgenome.manager.blast.dto.BlastRequestResult;
import com.epam.catgenome.manager.blast.dto.CreateDatabaseRequest;
import com.epam.catgenome.manager.blast.dto.CreateDatabaseResponse;
import com.epam.catgenome.manager.blast.dto.Entry;
import com.epam.catgenome.manager.blast.dto.Result;
import com.epam.catgenome.manager.blast.dto.TaskPage;
import com.epam.catgenome.manager.externaldb.taxonomy.Taxonomy;
import com.epam.catgenome.manager.externaldb.taxonomy.TaxonomyManager;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.QueryParameters;
import com.epam.catgenome.util.db.SortInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.epam.catgenome.constant.Constants.DATE_TIME_FORMAT;
import static org.apache.commons.lang3.StringUtils.join;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlastTaskManager {

    public static final String MAX_TARGET_SEQS = "max_target_seqs";
    public static final String EVALUE = "evalue";

    private final BlastTaskDao blastTaskDao;
    private final BlastRequestManager blastRequestManager;
    private final AuthManager authManager;
    private final BlastDatabaseManager databaseManager;

    private final TaxonomyManager taxonomyManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public BlastTask loadTask(final long taskId) {
        final BlastTask blastTask = getBlastTask(taskId);
        blastTask.setOrganisms(loadTaskOrganisms(taskId));
        blastTask.setExcludedOrganisms(loadTaskExclOrganisms(taskId));
        blastTask.setParameters(loadTaskParameters(taskId));
        return blastTask;
    }

    public BlastTask getBlastTask(long taskId) {
        final BlastTask blastTask = blastTaskDao.loadTaskById(taskId);
        Assert.notNull(blastTask, MessageHelper.getMessage(MessagesConstants.ERROR_TASK_NOT_FOUND, taskId));
        return blastTask;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public BlastTask create(final TaskVO taskVO) throws BlastRequestException {
        validateOrganisms(taskVO);
        final BlastDatabase blastDatabase = databaseManager.loadById(taskVO.getDatabaseId());
        final BlastRequest blastRequest = buildBlastRequest(taskVO, blastDatabase);
        BlastRequestInfo blastRequestInfo = blastRequestManager.createTask(blastRequest);
        if (blastRequestInfo == null || blastRequestInfo.getStatus().equals("ERROR")) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants.ERROR_BLAST_REQUEST));
        }
        final BlastTask blastTask = buildBlastTask(taskVO, blastDatabase, blastRequestInfo);
        blastTaskDao.saveTask(blastTask);
        blastTaskDao.saveOrganisms(blastTask.getId(), taskVO.getOrganisms());
        blastTaskDao.saveExclOrganisms(blastTask.getId(), taskVO.getExcludedOrganisms());
        blastTaskDao.saveTaskParameters(blastTask.getId(), taskVO.getParameters());
        return blastTask;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteTask(final long taskId) {
        final BlastTask blastTask = getBlastTask(taskId);
        Assert.isTrue(!BlastTaskStatus.RUNNING.equals(blastTask.getStatus()),
                MessageHelper.getMessage(MessagesConstants.ERROR_TASK_CAN_NOT_BE_DELETED, taskId));
        blastTaskDao.deleteOrganisms(taskId);
        blastTaskDao.deleteExclOrganisms(taskId);
        blastTaskDao.deleteParameters(taskId);
        blastTaskDao.deleteTask(taskId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteTasks() {
        QueryParameters params = new QueryParameters();
        List<Filter> getFilters = new ArrayList<>();
        getFilters.add(new Filter("status", "<>", String.valueOf(BlastTaskStatus.RUNNING.getId())));
        getFilters.add(new Filter("owner", "=", "'" + authManager.getAuthorizedUser() + "'"));
        params.setFilters(getFilters);
        List<Long> taskIdsList = blastTaskDao.loadAllTasks(params).stream().map(BlastTask::getId)
                .collect(Collectors.toList());
        String taskIds = "(" + join(taskIdsList, ",") + ")";
        List<Filter> deleteFilters = Collections.singletonList(new Filter("task_id", "in", taskIds));
        blastTaskDao.deleteOrganisms(deleteFilters);
        blastTaskDao.deleteExclOrganisms(deleteFilters);
        blastTaskDao.deleteParameters(deleteFilters);
        blastTaskDao.deleteTasks(deleteFilters);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public TaskPage loadTasks(final QueryParameters queryParameters) {
        TaskPage taskPage = new TaskPage();
        long totalCount = blastTaskDao.getTasksCount(queryParameters.getFilters());
        List<BlastTask> blastTasks = blastTaskDao.loadAllTasks(queryParameters);
        taskPage.setTotalCount(totalCount);
        taskPage.setBlastTasks(blastTasks);
        return taskPage;
    }

    public BlastTask loadTask(final String taskName) {
        final QueryParameters queryParameters = new QueryParameters();
        final List<Filter> filters = new ArrayList<>();
        final Filter filter = new Filter("title", "=", "'" + taskName + "'");
        filters.add(filter);
        queryParameters.setFilters(filters);
        final List<BlastTask> blastTasks = blastTaskDao.loadAllTasks(queryParameters);
        return blastTasks.size() > 0 ? blastTasks.get(0) : new BlastTask();
    }

    public TaskPage loadCurrentUserTasks(final QueryParameters queryParameters) {
        List<Filter> filters = new ArrayList<>();
        Filter currentUserFilter = new Filter("owner", "=",
                "'" + authManager.getAuthorizedUser() + "'");
        filters.add(currentUserFilter);
        filters.addAll(ListUtils.emptyIfNull(queryParameters.getFilters()));
        SortInfo sortByCreatedDate = new SortInfo("created_date", false);
        List<SortInfo> sortInfos = ListUtils.defaultIfNull(queryParameters.getSortInfos(),
                Collections.singletonList(sortByCreatedDate));
        queryParameters.setFilters(filters);
        queryParameters.setSortInfos(sortInfos);
        return loadTasks(queryParameters);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateTask(final BlastTask blastTask) {
        blastTaskDao.updateTask(blastTask);
    }

    public long getTasksCount(final List<Filter> filters) {
        return blastTaskDao.getTasksCount(filters);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void cancelTask(final long id) throws BlastRequestException {
        BlastTask blastTask = blastTaskDao.loadTaskById(id);
        Assert.notNull(blastTask, MessageHelper.getMessage(MessagesConstants.ERROR_TASK_NOT_FOUND, id));
        Result<BlastRequestInfo> result = blastRequestManager.cancelTask(id);
        BlastTaskStatus status = getStatus(result);
        if (status != null && !blastTask.getStatus().equals(status)) {
            blastTask.setStatus(status);
            if (status.isFinal()) {
                blastTask.setEndDate(LocalDateTime.now());
            }
            blastTaskDao.updateTask(blastTask);
        }
    }

    public BlastRequestResult getResult(final long taskId) throws BlastRequestException {
        return blastRequestManager.getResult(taskId);
    }

    public Collection<BlastSequence> getGroupedResult(final long taskId) throws BlastRequestException {
        return groupResult(blastRequestManager.getResult(taskId));
    }

    public ResponseBody getRawResult(final long taskId) throws BlastRequestException {
        return blastRequestManager.getRawResult(taskId);
    }

    public CreateDatabaseResponse createDatabase(final CreateDatabaseRequest request) throws BlastRequestException {
        return blastRequestManager.createDatabase(request);
    }

    private BlastTaskStatus getStatus(final Result<BlastRequestInfo> result) {
        if ("ERROR".equals(result.getStatus())) {
            String message = result.getMessage();
            BlastTaskStatus status = null;
            for (BlastTaskStatus taskStatus: BlastTaskStatus.values()) {
                if (message.contains(taskStatus.name())) {
                    status = taskStatus;
                    break;
                }
            }
            return status;
        } else {
            return BlastTaskStatus.CANCELED;
        }
    }

    private void validateOrganisms(final TaskVO taskVO) {
        final List<Long> organisms = ListUtils.emptyIfNull(taskVO.getOrganisms());
        final List<Long> excludedOrganisms = ListUtils.emptyIfNull(taskVO.getExcludedOrganisms());
        Assert.state(!(!CollectionUtils.isEmpty(organisms) && !CollectionUtils.isEmpty(excludedOrganisms)),
                MessageHelper.getMessage(MessagesConstants.ERROR_BLAST_ORGANISMS));
        Assert.state(mapTaxonomyIds(organisms).size() == organisms.size(),
                MessageHelper.getMessage(MessagesConstants.ERROR_BLAST_ORGANISMS_MAPPING));
        Assert.state(mapTaxonomyIds(excludedOrganisms).size() == excludedOrganisms.size(),
                MessageHelper.getMessage(MessagesConstants.ERROR_BLAST_ORGANISMS_MAPPING));
    }

    private List<Taxonomy> loadTaskOrganisms(final long taskId) {
        return mapTaxonomyIds(
                blastTaskDao.loadOrganisms(taskId)
                        .stream().map(BlastTaskOrganism::getOrganism)
                        .collect(Collectors.toList()));
    }

    private List<Taxonomy> loadTaskExclOrganisms(final long taskId) {
        return mapTaxonomyIds(
                blastTaskDao.loadExclOrganisms(taskId)
                        .stream().map(BlastTaskOrganism::getOrganism)
                        .collect(Collectors.toList()));
    }

    private Map<String, String> loadTaskParameters(final long taskId) {
        return blastTaskDao.loadTaskParameters(taskId).stream().collect(Collectors.
                toMap(TaskParameter::getParameter, TaskParameter::getValue));
    }

    @NotNull
    private List<Taxonomy> mapTaxonomyIds(final List<Long> organisms) {
        return ListUtils.emptyIfNull(organisms).stream()
                .map(taxonomyManager::searchOrganismById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Collection<BlastSequence> groupResult(final BlastRequestResult result) {
        if (CollectionUtils.isEmpty(result.getEntries())) {
            return Collections.emptyList();
        }
        return result.getEntries()
                .stream()
                .collect(Collectors.groupingBy(Entry::getSeqSeqId))
                .values()
                .stream()
                .map(BlastSequence::fromEntries)
                .sorted(Comparator.comparing(BlastSequence::getEValue))
                .collect(Collectors.toList());
    }

    @NotNull
    private BlastTask buildBlastTask(final TaskVO taskVO,
                                     final BlastDatabase blastDatabase,
                                     final BlastRequestInfo blastRequestInfo) {
        final BlastTask blastTask = new BlastTask();
        blastTask.setId(blastRequestInfo.getRequestId());
        blastTask.setTitle(taskVO.getTitle());
        blastTask.setQuery(taskVO.getQuery());
        blastTask.setDatabase(blastDatabase);
        blastTask.setOrganisms(mapTaxonomyIds(taskVO.getOrganisms()));
        blastTask.setExcludedOrganisms(mapTaxonomyIds(taskVO.getExcludedOrganisms()));
        blastTask.setExecutable(taskVO.getExecutable());
        blastTask.setAlgorithm(taskVO.getAlgorithm());
        blastTask.setParameters(taskVO.getParameters());
        blastTask.setOptions(taskVO.getOptions());
        blastTask.setStatus(BlastTaskStatus.valueOf(blastRequestInfo.getStatus()));
        blastTask.setCreatedDate(LocalDateTime.parse(blastRequestInfo.getCreatedDate(),
                DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
        blastTask.setOwner(authManager.getAuthorizedUser());
        return blastTask;
    }

    @NotNull
    private BlastRequest buildBlastRequest(final TaskVO taskVO, final BlastDatabase blastDatabase) {
        final BlastRequest blastRequest = new BlastRequest();
        blastRequest.setAlgorithm(taskVO.getAlgorithm());
        blastRequest.setBlastTool(taskVO.getExecutable());
        blastRequest.setDbName(blastDatabase.getPath());
        blastRequest.setQuery(taskVO.getQuery());
        blastRequest.setOptions(taskVO.getOptions());
        blastRequest.setTaxIds(taskVO.getOrganisms());
        blastRequest.setExcludedTaxIds(taskVO.getExcludedOrganisms());
        Map<String, String> params = MapUtils.emptyIfNull(taskVO.getParameters());
        if (params.containsKey(MAX_TARGET_SEQS)) {
            blastRequest.setMaxTargetSequence(Long.parseLong(params.get(MAX_TARGET_SEQS)));
        }
        if (params.containsKey(EVALUE)) {
            blastRequest.setExpectedThreshold(Double.parseDouble(params.get(EVALUE)));
        }
        return blastRequest;
    }
}
