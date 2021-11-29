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

import com.epam.catgenome.client.blast.BlastApi;
import com.epam.catgenome.client.blast.BlastApiBuilder;
import com.epam.catgenome.manager.blast.dto.BlastRequest;
import com.epam.catgenome.manager.blast.dto.BlastRequestInfo;
import com.epam.catgenome.manager.blast.dto.BlastRequestResult;
import com.epam.catgenome.manager.blast.dto.CreateDatabaseRequest;
import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.manager.blast.dto.CreateDatabaseResponse;
import com.epam.catgenome.manager.blast.dto.Result;
import com.epam.catgenome.util.QueryUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class BlastRequestManager {

    private static final String ERROR = "ERROR";
    private BlastApi blastApi;

    @Value("${blast.server.url:}")
    private String blastServer;

    @Value("${blast.server.token:}")
    private String blastServerToken;

    @Value("${blast.server.cookie:}")
    private String blastServerCookie;

    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(blastServer)) {
            return;
        }
        this.blastApi = new BlastApiBuilder(0, 0,
                blastServer, blastServerToken, blastServerCookie)
                .buildClient();
    }

    public BlastRequestInfo createTask(final BlastRequest blastRequest) throws BlastRequestException {
        validateBlastEnabled();
        try {
            Result<BlastRequestInfo> result = QueryUtils.execute(blastApi.createTask(blastRequest));
            Assert.isTrue(!result.getStatus().equals(ERROR), result.getMessage());
            return result.getPayload();
        } catch (IOException e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_BLAST_REQUEST), e);
        }
    }

    public BlastRequestInfo getTaskStatus(final long id) throws BlastRequestException {
        validateBlastEnabled();
        try {
            return QueryUtils.execute(blastApi.getTask(id)).getPayload();
        } catch (IOException e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_BLAST_REQUEST), e);
        }
    }

    public Result<BlastRequestInfo> cancelTask(final long id) throws BlastRequestException {
        validateBlastEnabled();
        try {
            return QueryUtils.execute(blastApi.cancelTask(id));
        } catch (IOException e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_BLAST_REQUEST), e);
        }
    }

    public BlastRequestResult getResult(final long taskId) throws BlastRequestException {
        validateBlastEnabled();
        try {
            return QueryUtils.execute(blastApi.getResult(taskId)).getPayload();
        } catch (IOException e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_BLAST_REQUEST), e);
        }
    }

    public ResponseBody getRawResult(final long taskId) throws BlastRequestException {
        validateBlastEnabled();
        try {
            return QueryUtils.execute(blastApi.getRawResult(taskId));
        } catch (IOException e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_BLAST_REQUEST), e);
        }
    }

    public CreateDatabaseResponse createDatabase(final CreateDatabaseRequest request) throws BlastRequestException {
        validateBlastEnabled();
        try {
            Result<CreateDatabaseResponse> result = QueryUtils.execute(blastApi.createDatabase(request));
            Assert.isTrue(!result.getStatus().equals(ERROR), result.getMessage());
            return result.getPayload();
        } catch (IOException e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants.ERROR_BLAST_REQUEST), e);
        }
    }

    public BlastRequestInfo createListSpeciesTask(final String databaseName) throws IOException {
        validateBlastEnabled();
        final Result<BlastRequestInfo> result = QueryUtils.execute(blastApi.createListSpeciesTask(databaseName));
        Assert.isTrue(!result.getStatus().equals(ERROR), result.getMessage());
        return result.getPayload();
    }

    public List<Long> listSpecies(final long taskId) throws BlastRequestException {
        validateBlastEnabled();
        try {
            final Result<List<Long>> result = QueryUtils.execute(blastApi.listSpecies(taskId));
            Assert.isTrue(!result.getStatus().equals(ERROR), result.getMessage());
            return result.getPayload();
        } catch (IOException e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants.ERROR_BLAST_REQUEST), e);
        }
    }

    private void validateBlastEnabled() {
        Assert.notNull(blastApi, MessageHelper.getMessage(MessagesConstants.ERROR_BLAST_NOT_AVAILABLE));
    }
}
