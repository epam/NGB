package com.epam.catgenome.controller.vo;

import java.util.List;


public class UrlRequestVO {
    private String dataset;
    private List<String> ids;

    public UrlRequestVO() {
      //no op
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }
}
