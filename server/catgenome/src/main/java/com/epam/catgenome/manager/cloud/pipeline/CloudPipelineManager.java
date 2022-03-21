/*
 * MIT License
 *
 * Copyright (c) 2022 EPAM Systems
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
package com.epam.catgenome.manager.cloud.pipeline;

import com.epam.catgenome.client.cloud.pipeline.CloudPipelineApi;
import com.epam.catgenome.client.cloud.pipeline.CloudPipelineApiBuilder;
import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.notification.NotificationMessageVO;
import com.epam.catgenome.exception.CloudPipelineUnavailableException;
import com.epam.catgenome.exception.ResponseException;
import com.epam.catgenome.util.QueryUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@ConditionalOnProperty(value = "item.path.update", havingValue = "true")
public class CloudPipelineManager {

    private CloudPipelineApi cloudPipelineApi;

    @Value("${cloud.pipeline.server.url}")
    private String server;

    @Value("${cloud.pipeline.server.token}")
    private String token;

    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(server)) {
            return;
        }
        this.cloudPipelineApi = new CloudPipelineApiBuilder(0, 0,
                server, token, null)
                .buildClient();
    }

    public void sendNotification(final NotificationMessageVO notificationMessage)
            throws CloudPipelineUnavailableException {
        try {
            QueryUtils.execute(cloudPipelineApi.sendNotification(notificationMessage));
        } catch (ResponseException e) {
            throw new CloudPipelineUnavailableException(MessageHelper
                    .getMessage(MessagesConstants.ERROR_CLOUD_PIPELINE_NOT_AVAILABLE));
        }
    }
}
