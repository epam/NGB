package com.epam.catgenome.controller.vo;

import java.io.File;
import java.util.List;

import com.epam.catgenome.entity.file.AbstractFsItem;

/**
 * A value object, containing list of files, browsed by NGB and root folder for file browsing
 */
public class FilesVO {
    private List<AbstractFsItem> files;
    private String root;
    private String separator;

    public FilesVO() {
        // no-op
    }

    public FilesVO(List<AbstractFsItem> files, String root) {
        this.files = files;
        this.root = root;
        this.separator = File.separator;
    }

    public List<AbstractFsItem> getFiles() {
        return files;
    }

    public void setFiles(List<AbstractFsItem> files) {
        this.files = files;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }
}
