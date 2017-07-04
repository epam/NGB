package com.epam.ngb.cli.app;

import java.nio.file.FileSystems;

public final class Utils {

    private Utils() {}

    /**
     * Returns the absolute path of the given one
     * */
    public static String getNormalizeAndAbsolutePath(String path) {
        return FileSystems.getDefault()
                .getPath(path)
                .normalize()
                .toAbsolutePath()
                .toString();
    }
}
