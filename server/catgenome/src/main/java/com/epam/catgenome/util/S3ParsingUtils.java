package com.epam.catgenome.util;



import com.amazonaws.services.s3.AmazonS3URI;

import com.epam.catgenome.util.feature.reader.S3Helper;
import htsjdk.tribble.util.ParsingUtils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;


public class S3ParsingUtils {

    public S3ParsingUtils() {
    }

    public static Class s3HelperClass = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ParsingUtils.class);

    public static InputStream openInputStream(String path) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        InputStream inputStream;

        if (!path.startsWith("s3:")) {
            inputStream = ParsingUtils.openInputStream(path);
        } else {
            inputStream = S3ParsingUtils.getS3Helper(new AmazonS3URI(path)).openInputStream();
        }

        return inputStream;
    }

    public static boolean resourceExists(String resource) throws IOException {

        boolean resExists;

        if (!resource.startsWith("s3://")) {
            resExists = ParsingUtils.resourceExists(resource);
        } else {
            resExists = true;
        }
        return resExists;
    }

    public static S3Helper getS3Helper(AmazonS3URI s3URI) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
            Constructor constructor = s3HelperClass.getConstructor(AmazonS3URI.class);
            return (S3Helper) constructor.newInstance(s3URI);
    }

    public static void registerS3HelperClass(Class helperClass) {
        s3HelperClass = helperClass;
    }

    public static String appendToPath(String filepath, String indexExtension) {
        String tabxIndex;
        AmazonS3URI s3URI = new AmazonS3URI(filepath);
        if (s3URI != null) {
            String path = s3URI.toString();
            String indexPath = path + indexExtension;
            tabxIndex = filepath.replace(path, indexPath);
        } else {
            tabxIndex = filepath + indexExtension;
        }
        return tabxIndex;
    }

}