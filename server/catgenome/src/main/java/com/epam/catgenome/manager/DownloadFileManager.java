/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

package com.epam.catgenome.manager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.Constants;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.exception.DownloadingTimeoutException;
import com.epam.catgenome.util.Utils;
import com.google.common.io.Files;

/**
 * Source:
 * Created:     8/15/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.1, JDK 1.8
 *
 * <p>
 * A service class to perform file download operations
 * </p>
 */
@Service
public class DownloadFileManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadFileManager.class);

    private static final int COEFFICIENT = 4;
    private static final int BUFFER_SIZE = 1024 * COEFFICIENT;
    private static final String DELIMITER = "/";
    @Value("#{catgenome['files.download.max.m.byte.size'] ?: 0}")
    private int maxFileSizeInMByte;
    @Value("#{catgenome['files.download.max.minutes'] ?: 0}")
    private long maxFileDownloadTimeMin;

    @Value("#{catgenome['file.download.whitelist.host']}")
    private String[] whiteHosArray;
    private List<String> whiteHostList;

    @Value("#{catgenome['files.download.directory.path']}")
    private String downloadDirPath;

    @Autowired
    private FileManager fileManager;


    /**
     * Download file from URL to file in our system !!!DANGEROUS!!!
     * This method just copy all information from file to file.
     * URL must be from white list, this must be checked where the method is called.
     *
     * @param urlString URL string of resource to download
     * @return downloaded File object
     * @throws IOException
     */
    public File downloadFromURL(final String urlString) throws IOException {
        final File newFile = createFileFromURL(urlString);
        final URL url = new URL(urlString);
        checkURL(url);
        final File tmpFile = createTmpFileFromURL(urlString);
        String errorString = downloadFileFromURL(url, tmpFile);
        Assert.isNull(errorString, errorString);
        Files.copy(tmpFile, newFile);
        return newFile;
    }

    /**
     * Download file from URL to file in our system !!!DANGEROUS!!!
     * This method just copy all information from file to file.
     * URL must be from white list, this must be checked where the method is called.
     *
     * @param url  {@code URL} from what resource download the file
     * @param file {@code File} where the file from URL copy, It must be temporary!
     * @return errorString{@code String} if no error return null else return error message.
     * @throws IOException
     */
    private String downloadFileFromURL(final URL url, final File file) throws IOException {
        int count = 0;
        final byte[] buff = new byte[BUFFER_SIZE];
        final long maxFileSizeInByte = getMaxFileSizeInMByte();
        LOGGER.info(MessageHelper.getMessage(MessagesConstants.INFO_START_DOWNLOAD_FILE, url.toString()));
        final long endTime = System.currentTimeMillis() + getMaxFileDownloadTimeSec();

        try (BufferedInputStream bis = new BufferedInputStream(url.openStream(), BUFFER_SIZE);
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file), BUFFER_SIZE)) {
            int position;

            while ((position = bis.read(buff, 0, BUFFER_SIZE)) != -1) {
                bos.write(buff, 0, position);
                count += position;
                if (count > maxFileSizeInByte) {
                    break;
                }
                count++;
                if (System.currentTimeMillis() > endTime) {
                    throw new DownloadingTimeoutException(url);
                }
            }
        }
        String errorString = null;
        if (count >= maxFileSizeInByte) {
            errorString = MessageHelper.getMessage(MessagesConstants.ERROR_LARGE_FILE_FOR_DOWNLOAD, url.toString());
        }
        return errorString;
    }

    public String getFileNameFromUrlString(final String url) {
        return FilenameUtils.getBaseName(url);
    }

    private File createFileFromURL(final String urlString) throws IOException {
        final String newPath = createPathFromUrlString(urlString);
        File file = new File(newPath);
        Assert.isTrue(!file.exists(), MessageHelper.getMessage(MessagesConstants.INFO_FILES_STATUS_ALREADY_EXISTS,
                                                               urlString));
        Assert.isTrue(file.createNewFile());
        return file;
    }

    private String createPathFromUrlString(final String urlString) {
        final String hash = Utils.getHashFromUrlString(urlString);
        final String dir = downloadDirPath + Utils.getPathFromHash(hash);
        makeDir(dir);
        return dir + DELIMITER + FilenameUtils.getBaseName(urlString) + hash + FilenameUtils.getExtension(urlString);
    }


    private File createTmpFileFromURL(final String urlString) throws IOException {
        Assert.notNull(urlString);
        return File.createTempFile(UUID.randomUUID().toString(), FilenameUtils.getBaseName(urlString),
                fileManager.getTempDir());
    }

    private void checkURL(final URL url) {
        final String urlHost = url.getHost();
        final List<String> hostList = getFilterWhiteHostList();
        boolean flag = false;
        for (String s : hostList) {
            if (s.equals(urlHost)) {
                flag = true;
            }
        }
        Assert.isTrue(flag, MessageHelper.getMessage(MessagesConstants.ERROR_UNKNOWN_HOST));
    }

    private List<String> getFilterWhiteHostList() {
        if (whiteHostList == null) {
            if (whiteHosArray == null) {
                return Collections.emptyList();
            } else {
                whiteHostList = Arrays.asList(whiteHosArray);
            }
        }
        return whiteHostList;
    }

    private long getMaxFileSizeInMByte() {
        return (long)maxFileSizeInMByte * Constants.KILO_BYTE_SIZE * Constants.KILO_BYTE_SIZE;
    }

    private long getMaxFileDownloadTimeSec() {
        return TimeUnit.MINUTES.toMillis(maxFileDownloadTimeMin);
    }

    private File makeDir(final String path) {
        final File directory = new File(path);
        final boolean result = directory.exists() || directory.mkdirs();
        LOGGER.info(MessageHelper.getMessage(MessagesConstants.INFO_FILES_STATUS_RESOURCE_AT_PATH, path, result));
        Assert.isTrue(result, MessageHelper.getMessage(MessagesConstants.ERROR_FILES_MISSING_RESOURCE_AT_PATH, path));
        return directory;
    }
}
