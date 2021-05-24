package com.epam.catgenome.manager.blast;

import com.epam.catgenome.client.blast.BlastApi;
import com.epam.catgenome.client.blast.BlastApiBuilder;
import com.epam.catgenome.exception.BlastResponseException;
import com.epam.catgenome.manager.blast.dto.Request;
import com.epam.catgenome.manager.blast.dto.RequestInfo;
import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.manager.blast.dto.RequestResult;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.util.QueryUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@Slf4j
@NoArgsConstructor
public class BlastRequestManager {

    private BlastApi blastApi;

    @Value("${blast.server.url}")
    private String blastServer;

    @PostConstruct
    public void init() {
        this.blastApi = new BlastApiBuilder(0, 0, blastServer).buildClient();
    }

    public RequestInfo createTask(final Request request) throws BlastRequestException {
        try {
            return QueryUtils.execute(blastApi.createTask(request));
        } catch (BlastResponseException e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_BLAST_REQUEST), e);
        }
    }

    public RequestInfo getTaskStatus(final long id) throws BlastRequestException {
        try {
            return QueryUtils.execute(blastApi.getTask(id));
        } catch (BlastResponseException e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_BLAST_REQUEST), e);
        }
    }

    public RequestInfo cancelTask(final long id) throws BlastRequestException {
        try {
            return QueryUtils.execute(blastApi.cancelTask(id));
        } catch (BlastResponseException e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_BLAST_REQUEST), e);
        }
    }

    public RequestResult getResult(final long taskId) throws BlastRequestException {
        try {
            return QueryUtils.execute(blastApi.getResult(taskId));
        } catch (BlastResponseException e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_BLAST_REQUEST), e);
        }
    }
}

