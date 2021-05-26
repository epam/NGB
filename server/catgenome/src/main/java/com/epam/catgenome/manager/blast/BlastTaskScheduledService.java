package com.epam.catgenome.manager.blast;

import com.epam.catgenome.dao.blast.BlastTaskDao;
import com.epam.catgenome.entity.blast.BlastTask;
import com.epam.catgenome.entity.blast.TaskStatus;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.manager.blast.dto.BlastRequestInfo;
import com.epam.catgenome.util.db.Filter;
import com.epam.catgenome.util.db.QueryParameters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class BlastTaskScheduledService {
    @Autowired
    private BlastTaskDao blastTaskDao;
    @Autowired
    private BlastRequestManager blastRequestManager;

    @Scheduled(fixedRateString = "${blast.update.status.rate}")
    public void updateTaskStatuses() {
        Filter filter = new Filter("status", "=", String.valueOf(TaskStatus.RUNNING.getId()));
        QueryParameters parameters = new QueryParameters();
        parameters.setFilters(Collections.singletonList(filter));
        List<BlastTask> tasks = blastTaskDao.loadAllTasks(parameters);
        tasks.forEach(t -> {
            try {
                BlastRequestInfo blastRequestInfo = blastRequestManager.getTaskStatus(t.getId());
                String status = blastRequestInfo.getStatus();
                if (!status.equals(t.getStatus().name())) {
                    t.setStatus(TaskStatus.valueOf(status));
                    blastTaskDao.updateTask(t);
                }
            } catch (BlastRequestException e) {
                log.debug(e.getMessage());
            }
        });
    }
}
