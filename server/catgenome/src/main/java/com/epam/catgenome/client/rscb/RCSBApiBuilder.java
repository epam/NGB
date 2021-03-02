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

package com.epam.catgenome.client.rscb;

import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.util.QueryUtils;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class RCSBApiBuilder {
    private static final JacksonConverterFactory CONVERTER =
            JacksonConverterFactory
                    .create(new JsonMapper());

    private final int connectTimeout;
    private final int readTimeout;
    private final String apiHost;

    public RCSBApi buildClient() {
        return new Retrofit.Builder()
                .baseUrl(QueryUtils.normalizeUrl(apiHost))
                .addConverterFactory(CONVERTER)
                .client(buildHttpClient())
                .build()
                .create(RCSBApi.class);
    }

    private OkHttpClient buildHttpClient() {
        return new OkHttpClient.Builder()
                .hostnameVerifier((hostname, session) -> true)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .hostnameVerifier((s, sslSession) -> true)
                .build();
    }
}
