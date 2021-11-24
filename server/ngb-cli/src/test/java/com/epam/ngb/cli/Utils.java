package com.epam.ngb.cli;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class Utils {

    private Utils() {}

    public static String pathToEscapingView(String path) throws UnsupportedEncodingException {
        return URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
    }
}
