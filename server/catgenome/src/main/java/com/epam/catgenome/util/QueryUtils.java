package com.epam.catgenome.util;

import com.epam.catgenome.exception.RSCBResponseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

public interface QueryUtils {

    static String normalizeUrl(String url) {
        Assert.state(StringUtils.isNotBlank(url), "Url shall be specified");
        return url.endsWith("/") ? url : url + "/";
    }

    static <T> T execute(Call<T> call) {
        try {
            Response<T> response = call.execute();
            validateResponseStatus(response);

            return response.body();
        } catch (IOException e) {
            throw new RSCBResponseException(e);
        }
    }

    static <T> void validateResponseStatus(Response<T> response) throws IOException {
        if (!response.isSuccessful()) {
            throw new RSCBResponseException(String.format("Unexpected status code: %d, %s", response.code(),
                    response.errorBody() != null ? response.errorBody().string() : ""));
        }
    }
}
