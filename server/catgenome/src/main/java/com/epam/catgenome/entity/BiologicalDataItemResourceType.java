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

import java.util.Collections;
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

    /*
     * Ids 5 (HDFS) and 6 (GA4GH) were used by resource types NGB no longer supports. The gap is
     * deliberate: the ids below are persisted in BIO_DATA_ITEM.TYPE and must not be reassigned.
     */

    /**
     * Indicates that item was downloaded by NGB and is located in it's download directory
     */
    DOWNLOAD(7),

    /**
     * Azure blobs
     */
    AZ(8);

    /**
     * Resource types removed from NGB, by the id they used to be persisted under. Kept so a
     * database written by an older NGB reports what it holds instead of failing obscurely.
     */
    private static final Map<Long, String> REMOVED_TYPE_NAMES = removedTypeNames();

    private long id;
    private static Map<Long, BiologicalDataItemResourceType> idMap = new HashMap<>((int) DOWNLOAD.getId());

    static {
        idMap.put(FILE.id, FILE);
        idMap.put(URL.id, URL);
        idMap.put(S3.id, S3);
        idMap.put(ONLINE.id, ONLINE);
        idMap.put(DOWNLOAD.id, DOWNLOAD);
        idMap.put(AZ.id, AZ);
    }

    private static Map<Long, String> removedTypeNames() {
        final Map<Long, String> names = new HashMap<>();
        names.put(5L, "HDFS");
        names.put(6L, "GA4GH");
        return names;
    }

    BiologicalDataItemResourceType(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    /**
     * @param id a persisted resource type id, or {@code null}
     * @return the matching type, or {@code null} if {@code id} is {@code null}
     * @throws IllegalArgumentException if the id is not a supported resource type. Ids of removed
     *         types are named in the message, because a database written by an older NGB can still
     *         hold them and the operator has to be told which files to re-register.
     */
    public static BiologicalDataItemResourceType getById(Long id) {
        if (id == null) {
            return null;
        }
        final BiologicalDataItemResourceType type = idMap.get(id);
        if (type == null) {
            throw new IllegalArgumentException(unsupportedTypeMessage(id));
        }
        return type;
    }

    /**
     * @param id an unmappable resource type id
     * @return a message naming the removed resource type the id belonged to, if it was one
     */
    public static String unsupportedTypeMessage(final Long id) {
        final String removed = REMOVED_TYPE_NAMES.get(id);
        return removed == null
               ? String.format("Unknown biological data item resource type id: %s.", id)
               : String.format("Biological data item resource type %s (%s) is no longer supported. Files "
                               + "registered with it have to be unregistered and, if still needed, "
                               + "re-registered from a supported resource type.", id, removed);
    }

    /**
     * @return ids of the resource types NGB used to support, mapped to their former names
     */
    public static Map<Long, String> getRemovedTypeNames() {
        return Collections.unmodifiableMap(REMOVED_TYPE_NAMES);
    }

    public static BiologicalDataItemResourceType translateRequestType(BiologicalDataItemResourceType requestType) {
        return requestType == null || requestType == DOWNLOAD ?
               FILE : requestType;
    }

    /**
     * Method tries to guess {@code BiologicalDataItemResourceType} from a path to resource
     * @param path to the resource
     * @return {@code BiologicalDataItemResourceType}
     */
    public static BiologicalDataItemResourceType getTypeFromPath(final String path) {
        if (path.startsWith("s3") || path.startsWith("sws")) {
            return S3;
        } else if (path.startsWith("az")) {
            return AZ;
        } else if (NgbFileUtils.isRemotePath(path)) {
            return URL;
        } else {
            return FILE;
        }
    }
}
