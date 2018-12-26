package com.epam.catgenome.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class IndexedDataItem extends BiologicalDataItem {

    @JsonIgnore
    private BiologicalDataItem index;

    public BiologicalDataItem getIndex() {
        return index;
    }

    public void setIndex(BiologicalDataItem index) {
        this.index = index;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        IndexedDataItem that = (IndexedDataItem) o;

        return index != null ? index.equals(that.index) : that.index == null;
    }

    @Override public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (index != null ? index.hashCode() : 0);
        return result;
    }
}
