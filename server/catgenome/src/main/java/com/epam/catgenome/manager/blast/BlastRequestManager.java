package com.epam.catgenome.manager.blast;

import com.epam.catgenome.client.blast.BlastApi;
import com.epam.catgenome.client.blast.BlastApiBuilder;
import com.epam.catgenome.manager.blast.dto.Request;
import com.epam.catgenome.manager.blast.dto.RequestInfo;
import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.manager.blast.dto.TaskResult;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.util.QueryUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BlastRequestManager {
    private static final String BLAST_SERVER = "http://34.237.52.139:8090/";

    private final BlastApi blastApi;

    public BlastRequestManager() {
        this.blastApi = new BlastApiBuilder(0, 0, BLAST_SERVER).buildClient();
    }

    public RequestInfo createTask(Request request) throws BlastRequestException {
        try {
            return QueryUtils.execute(blastApi.createTask(request));
        } catch (Exception e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_BLAST_REQUEST, BLAST_SERVER), e);
        }
    }

    public RequestInfo getTaskStatus(long id) throws BlastRequestException {
        try {
            return QueryUtils.execute(blastApi.getTask(id));
        } catch (Exception e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_BLAST_REQUEST, BLAST_SERVER), e);
        }
    }

    public TaskResult getResult(long taskId) throws BlastRequestException {
        try {
            return QueryUtils.execute(blastApi.getResult(taskId));
        } catch (Exception e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_BLAST_REQUEST, BLAST_SERVER), e);
        }
    }
}

