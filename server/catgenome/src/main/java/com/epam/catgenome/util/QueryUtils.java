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

package com.epam.catgenome.util;

import com.epam.catgenome.exception.RSCBResponseException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

public interface QueryUtils {

    static String normalizeUrl(final String url) {
        Assert.state(StringUtils.isNotBlank(url), "Url shall be specified");
        return url.endsWith("/") ? url : url + "/";
    }

    static <T> T execute(final Call<T> call) {
        try {
            final Response<T> response = call.execute();
            validateResponseStatus(response);

            return response.body();
        } catch (IOException e) {
            throw new RSCBResponseException(e);
        }
    }

    static <T> void validateResponseStatus(final Response<T> response) throws IOException {
        if (!response.isSuccessful()) {
            throw new RSCBResponseException(String.format("Unexpected status code: %d, %s", response.code(),
                    response.errorBody() != null ? response.errorBody().string() : ""));
        }
    }

    static String buildContentDispositionHeader(final String filePath) {
        return String.format("attachment; filename=%s", FilenameUtils.getName(filePath));
    }
}
