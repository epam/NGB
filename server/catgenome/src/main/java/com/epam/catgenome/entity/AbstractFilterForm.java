package com.epam.catgenome.entity;

import com.epam.catgenome.entity.vcf.Pointer;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public abstract class AbstractFilterForm {
    protected Integer pageSize;
    protected List<OrderBy> orderBy;

    protected Pointer pointer;

    public abstract boolean filterEmpty();

    public abstract List<String> getAdditionalFields();

    public abstract Integer getPage();

    public static class OrderBy {

        private String field;
        private boolean desc = false;

        public OrderBy() {
            // no-op
        }

        public OrderBy(String field, boolean desc) {
            this.field = field;
            this.desc = desc;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public boolean isDesc() {
            return desc;
        }

        public void setDesc(boolean desc) {
            this.desc = desc;
        }
    }
}
