package com.epam.catgenome.entity.file;

/**
 * Source:      FsDirectory
 * Created:     19.01.17, 17:27
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 15.0.3, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
public class FsDirectory extends AbstractFsItem {

    public FsDirectory() {
        this.type = FsItemType.DIRECTORY;
    }

    private String path;
    private Integer fileCount;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getFileCount() {
        return fileCount;
    }

    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }
}
