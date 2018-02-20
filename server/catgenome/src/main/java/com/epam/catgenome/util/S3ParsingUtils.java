package com.epam.catgenome.util;


import htsjdk.tribble.util.RemoteURLHelper;
import htsjdk.tribble.util.URLHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;


public class S3ParsingUtils {

    public S3ParsingUtils() {
    }

    private static final Class defaultUrlHelperClass = RemoteURLHelper.class;
    public static Class urlHelperClass = defaultUrlHelperClass;

    public static void registerHelperClass(Class helperClass) {
        if (!URLHelper.class.isAssignableFrom(helperClass)) {
            throw new IllegalArgumentException("helperClass must implement URLHelper");
        }
        urlHelperClass = helperClass;
    }

    public static InputStream openInputStream(String path)
            throws IOException {

        InputStream inputStream;

        if (path.startsWith("http:") || path.startsWith("https:") || path.startsWith("ftp:")) {
            inputStream = getURLHelper(new URL(path)).openInputStream();
        } else {
            File file = new File(path);
            inputStream = new FileInputStream(file);
        }

        return inputStream;
    }

    public static URLHelper getURLHelper(URL url) {
        try {
            return getURLHelper(urlHelperClass, url);
        } catch (Exception e) {
            return getURLHelper(defaultUrlHelperClass, url);
        }
    }

    private static URLHelper getURLHelper(Class helperClass, URL url) {
        try {
            Constructor constr = helperClass.getConstructor(URL.class);
            return (URLHelper) constr.newInstance(url);
        } catch (Exception e) {
            String errMsg = "Error instantiating url helper for class: " + helperClass;
            throw new IllegalStateException(errMsg, e);
        }
    }
}
