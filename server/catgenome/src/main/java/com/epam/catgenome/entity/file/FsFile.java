package com.epam.catgenome.entity.file;

import com.epam.catgenome.entity.BiologicalDataItemFormat;

/**
 * Represents file of NGB server's file system
 */
public class FsFile extends AbstractFsItem {

    public FsFile() {
        this.type = FsItemType.FILE;
    }

    private String name;
    private BiologicalDataItemFormat format;
    private Long size;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BiologicalDataItemFormat getFormat() {
        return format;
    }

    public void setFormat(BiologicalDataItemFormat format) {
        this.format = format;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
