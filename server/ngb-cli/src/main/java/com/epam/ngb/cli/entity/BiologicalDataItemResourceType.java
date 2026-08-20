package com.epam.ngb.cli.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kite on 27.02.17.
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
     * deliberate: the ids below are persisted server-side and must not be reassigned.
     */

    /**
     * Indicates that item was downloaded by NGB and is located in it's download directory
     */
    DOWNLOAD(7),

    AZ(8);

    private long id;
    private static Map<Long, BiologicalDataItemResourceType> idMap = new HashMap<>((int) DOWNLOAD.getId());

    static {
        idMap.put(FILE.id, FILE);
        idMap.put(URL.id, URL);
        idMap.put(S3.id, S3);
        idMap.put(ONLINE.id, ONLINE);
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
        if (path.startsWith("s3") || path.startsWith("sws")) {
            return S3;
        } else if (path.startsWith("az://")) {
            return AZ;
        } else if (path.startsWith("http") || path.startsWith("https") || path.startsWith("ftsp")) {
            return URL;
        } else {
            return FILE;
        }
    }
}
