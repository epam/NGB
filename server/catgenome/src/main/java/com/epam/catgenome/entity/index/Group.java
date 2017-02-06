package com.epam.catgenome.entity.index;

/**
 * Represents result of grouping query in feature index
 */
public class Group {
    private String groupName;
    private Integer entriesCount;

    public Group() {
        // no-op
    }

    public Group(String groupName, Integer entriesCount) {
        this.groupName = groupName;
        this.entriesCount = entriesCount;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Integer getEntriesCount() {
        return entriesCount;
    }

    public void setEntriesCount(Integer entriesCount) {
        this.entriesCount = entriesCount;
    }
}
