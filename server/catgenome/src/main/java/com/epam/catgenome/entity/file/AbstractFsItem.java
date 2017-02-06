package com.epam.catgenome.entity.file;

/**
 * Represents file system objects
 */
public abstract class AbstractFsItem {
    protected FsItemType type;

    public FsItemType getType() {
        return type;
    }

    enum FsItemType {
        DIRECTORY,
        FILE

    }
}
