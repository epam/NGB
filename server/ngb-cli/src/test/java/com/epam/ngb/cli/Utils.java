package com.epam.ngb.cli;

import java.io.File;

public final class Utils {

    public static final String PATH_DELIMITER = File.separator;
    public static final String ESCAPING_PATH_DELIMITER = "%2F";

    private Utils() {}


    public static String pathToEscapingView(String path) {
        return path.replaceAll(PATH_DELIMITER, ESCAPING_PATH_DELIMITER);
    }
}
