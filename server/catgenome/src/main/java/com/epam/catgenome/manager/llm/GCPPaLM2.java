/*
 * MIT License
 *
 * Copyright (c) 2016-2023 EPAM Systems
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

package com.epam.catgenome.manager.llm;

import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.entity.llm.LLMMessage;
import com.epam.catgenome.entity.llm.LLMProvider;
import com.google.cloud.aiplatform.v1beta1.EndpointName;
import com.google.cloud.aiplatform.v1beta1.PredictResponse;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceSettings;
import com.google.protobuf.util.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class GCPPaLM2 implements LLMHandler {
    private final String project;
    private final String location;
    private final String model;
    private final int promptSize;
    private final int responseSize;
    private final String promptTemplate;
    private final PredictionServiceClient client;

    public GCPPaLM2(final @Value("${llm.google.palm2.location:us-central1}") String location,
                    final @Value("${llm.google.palm2.project:}") String project,
                    final @Value("${llm.google.palm2.model:text-bison@001}") String model,
                    final @Value("${llm.google.palm2.prompt.size:8196}") int promptSize,
                    final @Value("${llm.google.palm2.response.size:500}") int responseSize,
                    final @Value("${llm.google.palm2.prompt.template:}") String promptTemplate) {
        this.project = project;
        this.location = location;
        this.model = model;
        this.promptSize = promptSize;
        this.responseSize = responseSize;
        this.promptTemplate = promptTemplate;
        this.client = buildClient();
    }

    @Override
    public String getSummary(final String text,
                             final double temperature) {
        return getSummary(promptTemplate, text, temperature);
    }

    @Override
    public String getSummary(final String prompt, final String text, final double temperature) {
        Assert.notNull(this.client, "Google PALM client is not available");
        Assert.isTrue(StringUtils.isNotBlank(project), "Project is not configured for GCP request");
        final String instance = JsonMapper.convertDataToJsonStringForQuery(
                new PalmPrompt(buildPrompt(prompt, text, promptSize)));

        final String parameters =
                String.format("{\"temperature\": %.2f,\"maxOutputTokens\": %d,\"topP\": 0.95,\"topK\": 40}",
                        temperature, responseSize);

        final EndpointName endpointName =
                EndpointName.ofProjectLocationPublisherModelName(project, location, "google", model);
        final List<com.google.protobuf.Value> instances = Collections.singletonList(buildValue(instance));
        final com.google.protobuf.Value parameterValue = buildValue(parameters);

        final PredictResponse predictResponse = client.predict(endpointName, instances, parameterValue);
        return predictResponse.getPredictions(0)
                .getStructValue()
                .getFieldsMap()
                .get("content")
                .getStringValue();
    }

    @Override
    public String getChatResponse(List<LLMMessage> messages, double temperature) {
        throw new UnsupportedOperationException("Chat is not supported for GCP model yet.");
    }

    @Override
    public LLMProvider getProvider() {
        return LLMProvider.GOOGLE_PALM_2;
    }

    @SneakyThrows
    private com.google.protobuf.Value buildValue(final String json) {
        final com.google.protobuf.Value.Builder value = com.google.protobuf.Value.newBuilder();
        JsonFormat.parser().merge(json, value);
        return value.build();
    }

    private PredictionServiceClient buildClient() {
        try {
            final String endpoint = String.format("%s-aiplatform.googleapis.com:443", location);
            final PredictionServiceSettings predictionServiceSettings =
                    PredictionServiceSettings.newBuilder().setEndpoint(endpoint).build();
            return PredictionServiceClient.create(predictionServiceSettings);
        } catch (IOException e) {
            log.error("Failed to initialize Google PALM client", e);
            return null;
        }
    }

    @Data
    @AllArgsConstructor
    private static class PalmPrompt {
        private String prompt;
    }
}
