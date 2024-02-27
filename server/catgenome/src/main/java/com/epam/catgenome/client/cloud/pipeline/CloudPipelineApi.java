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

package com.epam.catgenome.client.cloud.pipeline;

import com.epam.catgenome.entity.llm.CustomChatCompletion;
import com.epam.catgenome.entity.llm.CustomLLMMessage;
import com.epam.catgenome.entity.llm.CustomLLMResponse;
import com.epam.catgenome.entity.notification.NotificationMessageVO;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface CloudPipelineApi {
    @Headers("Content-type: application/json")
    @POST("notification/message")
    Call<Void> sendNotification(@Body NotificationMessageVO notificationMessage);

    @Headers("Content-type: application/json")
    @POST("chat")
    Call<CustomLLMResponse> chatMessage(@Body CustomLLMMessage message);

    @Headers("Content-type: application/json")
    @POST("v1/completions")
    Call<CustomLLMResponse> chatMessage(@Body CustomChatCompletion message);
}
