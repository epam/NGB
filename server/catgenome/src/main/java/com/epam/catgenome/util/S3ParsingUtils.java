package com.epam.catgenome.util;



import htsjdk.tribble.util.ParsingUtils;

import htsjdk.tribble.util.URLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;


public class S3ParsingUtils {

    public S3ParsingUtils() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ParsingUtils.class);
    public static InputStream openInputStream(String path) throws IOException {

        InputStream inputStream;

        if (!path.startsWith("s3:")) {
            inputStream = ParsingUtils.openInputStream(path);
        } else {
            inputStream = ParsingUtils.getURLHelper(new URL(path)).openInputStream();
        }

        return inputStream;
    }

    public static boolean resourceExists(String resource) throws IOException {

        boolean resExists;

        if (!resource.startsWith("s3://")) {
            resExists = ParsingUtils.resourceExists(resource);
        } else {
            URL url = new URL(resource);
            LOGGER.debug("url:" + url);
            URLHelper helper = ParsingUtils.getURLHelper(url);
            resExists = helper.exists();
            Class helperClass = helper.getClass();
            LOGGER.debug("helperClass is :" + helperClass.getSimpleName());
        }
        return resExists;
    }
}