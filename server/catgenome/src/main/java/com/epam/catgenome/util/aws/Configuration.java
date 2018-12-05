/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2018 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.epam.catgenome.util.aws;

import java.io.IOException;

/**
 * Contains default values for global variables that affect how the S3HtsjdkReaderFactory operates.
 * Default values are encoded in the class and also can be overridden using system properties.
 */
public final class Configuration {

    /**
     *  Number of connection-threads for downloading data from S3 cloud.
     */
    private static  int numberOfConnections;

    /**
     *  Min download chunk size - minimal part size of a file will be downloaded by single thread.
     */
    private static int minDownloadPartSize;

    /**
     *  Quantitative characteristic of the ability to reconnect to the server while downloading in case the connection is lost.
     */
    private static int customRetryCount;

    /**
     *  Index file URL for BAM file.
     */

    public static final String CONNECTIONS_NUMBER_PARAMETER = "samjdk.s3plugin.number_of_connections";
    public static final String MIN_CHUNK_SIZE_PARAMETER = "samjdk.s3plugin.min_download_chunk_size";
    public static final String CUSTOM_RETRY_COUNT_PARAMETER = "samjdk.s3plugin.custom_retry_count";
    public static final int DEFAULT_CONNECTIONS_NUMBER = 50;
    public static final int DEFAULT_MIN_CHUNK_SIZE = 64 * 1024;
    public static final int DEFAULT_CUSTOM_RETRY_COUNT = 3;

    private Configuration() {
        //no operations
    }

    public static int getNumberOfConnections() {
        return numberOfConnections;
    }

    public static int getMinDownloadPartSize() {
        return minDownloadPartSize;
    }

    static {
        init();
    }

    /**
     * Init is made a standalone method for testing purposes only.
     * It isn't supposed to be invoked more than once during the actual plugin run
     */
    public static void init() {
        int conNum = getIntProperty(CONNECTIONS_NUMBER_PARAMETER, DEFAULT_CONNECTIONS_NUMBER);
        if (conNum > 0) {
            numberOfConnections = conNum;
        } else {
            throw new IllegalArgumentException("Negative number of connections value",
                    new IOException());
        }

        int retryCount = getIntProperty(CUSTOM_RETRY_COUNT_PARAMETER, DEFAULT_CUSTOM_RETRY_COUNT);
        if (retryCount > 0) {
            customRetryCount = retryCount;
        } else {
            throw new IllegalArgumentException("Negative retry count value",
                    new IOException());
        }

        int minPartSize = getIntProperty(MIN_CHUNK_SIZE_PARAMETER, DEFAULT_MIN_CHUNK_SIZE);
        if (minPartSize > 0) {
            minDownloadPartSize = minPartSize;
        } else {
            throw new IllegalArgumentException("Negative download part size value",
                    new IOException());
        }

    }

    /**
     * @param name name of the property to be set.
     * @param def  default value.
     * @return a value of a system property if it was set,
     * otherwise return the default value.
     */
    private static int getIntProperty(final String name, final int def) {
        final String value = System.getProperty(name, Integer.toString(def));
        try {
            return Integer.parseInt(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(name + " value is incorrect, value = " + value, e);
        }
    }

    /**
     * @return a set number of connection retries.
     */
    public static int getCustomRetryCount() {
        return customRetryCount;
    }

    /**
     * This method resets the configuration. Currently is being used in tests only.
     */
    public static void resetToDefault() {
        System.setProperty(Configuration.CONNECTIONS_NUMBER_PARAMETER,
                Integer.toString(Configuration.DEFAULT_CONNECTIONS_NUMBER));
        System.setProperty(Configuration.MIN_CHUNK_SIZE_PARAMETER,
                Integer.toString(Configuration.DEFAULT_MIN_CHUNK_SIZE));
        System.setProperty(Configuration.CUSTOM_RETRY_COUNT_PARAMETER,
                Integer.toString(Configuration.DEFAULT_CUSTOM_RETRY_COUNT));
        init();
    }
}
