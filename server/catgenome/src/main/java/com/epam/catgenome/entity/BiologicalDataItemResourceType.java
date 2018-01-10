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

package com.epam.catgenome.entity;

import java.util.HashMap;
import java.util.Map;

import com.epam.catgenome.util.NgbFileUtils;

/**
 * Source:      BiologicalItemType
 * Created:     17.12.15, 12:45
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * Represents a resource type of a biological data item, and therefore an access mode.
 * </p>
 */
public enum BiologicalDataItemResourceType {

    /**
     * Indicates that item is a regular file in the server's filesystem
     */
    FILE(1),

    /**
     * Indicates that item is available by URL
     */
    URL(2),

    /**
     * Indicates that item is provided by Amazon S3 service
     */
    S3(3),

    /**
     * Indicates that item is an online track
     */
    ONLINE(4),

    /**
     * Indicates that item is located in HDFS
     */
    HDFS(5),

    /**
     * Indicates that item is provided by GA4GH protocol
     */
    GA4GH(6),

    /**
     * Indicates that item was downloaded by NGB and is located in it's download directory
     */
    DOWNLOAD(7);

    private long id;
    private static Map<Long, BiologicalDataItemResourceType> idMap = new HashMap<>((int) DOWNLOAD.getId());

    static {
        idMap.put(FILE.id, FILE);
        idMap.put(URL.id, URL);
        idMap.put(S3.id, S3);
        idMap.put(ONLINE.id, ONLINE);
        idMap.put(HDFS.id, HDFS);
        idMap.put(GA4GH.id, GA4GH);
        idMap.put(DOWNLOAD.id, DOWNLOAD);
    }

    BiologicalDataItemResourceType(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public static BiologicalDataItemResourceType getById(Long id) {
        if (id == null) {
            return null;
        }

        return idMap.get(id);
    }

    public static BiologicalDataItemResourceType translateRequestType(BiologicalDataItemResourceType requestType) {
        return requestType == null || requestType == BiologicalDataItemResourceType.DOWNLOAD ?
               BiologicalDataItemResourceType.FILE : requestType;
    }

    /**
     * Method tries to guess {@code BiologicalDataItemResourceType} from a path to resource
     * @param path to the resource
     * @return {@code BiologicalDataItemResourceType}
     */
    public static BiologicalDataItemResourceType getTypeFromPath(final String path) {
        if (path.startsWith("s3")) {
            return S3;
        } else if (NgbFileUtils.isRemotePath(path)) {
            return URL;
        } else {
            return FILE;
        }
    }
}
