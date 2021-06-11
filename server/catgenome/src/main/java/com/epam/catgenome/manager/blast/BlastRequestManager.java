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

import com.epam.catgenome.client.blast.BlastApi;
import com.epam.catgenome.client.blast.BlastApiBuilder;
import com.epam.catgenome.exception.BlastResponseException;
import com.epam.catgenome.manager.blast.dto.BlastRequest;
import com.epam.catgenome.manager.blast.dto.BlastRequestInfo;
import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.manager.blast.dto.BlastRequestResult;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.manager.blast.dto.Result;
import com.epam.catgenome.util.QueryUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;

@Service
@Slf4j
public class BlastRequestManager {

    private BlastApi blastApi;

    @Value("${blast.server.url}")
    private String blastServer;

    @PostConstruct
    public void init() {
        this.blastApi = new BlastApiBuilder(0, 0, blastServer).buildClient();
    }

    public BlastRequestInfo createTask(final BlastRequest blastRequest) throws BlastRequestException {
        try {
            Result<BlastRequestInfo> result = QueryUtils.execute(blastApi.createTask(blastRequest));
            Assert.isTrue(!result.getStatus().equals("ERROR"), result.getMessage());
            return result.getPayload();
        } catch (BlastResponseException e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_BLAST_REQUEST), e);
        }
    }

    public BlastRequestInfo getTaskStatus(final long id) throws BlastRequestException {
        try {
            return QueryUtils.execute(blastApi.getTask(id)).getPayload();
        } catch (BlastResponseException e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_BLAST_REQUEST), e);
        }
    }

    public BlastRequestInfo cancelTask(final long id) throws BlastRequestException {
        try {
            Result<BlastRequestInfo> result = QueryUtils.execute(blastApi.cancelTask(id));
            Assert.isTrue(!result.getStatus().equals("ERROR"), result.getMessage());
            return result.getPayload();
        } catch (BlastResponseException e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_BLAST_REQUEST), e);
        }
    }

    public BlastRequestResult getResult(final long taskId) throws BlastRequestException {
        try {
            return QueryUtils.execute(blastApi.getResult(taskId)).getPayload();
        } catch (BlastResponseException e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_BLAST_REQUEST), e);
        }
    }

    public ResponseBody getRawResult(final long taskId) throws BlastRequestException {
        try {
            return QueryUtils.execute(blastApi.getRawResult(taskId));
        } catch (BlastResponseException e) {
            throw new BlastRequestException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_BLAST_REQUEST), e);
        }
    }
}
