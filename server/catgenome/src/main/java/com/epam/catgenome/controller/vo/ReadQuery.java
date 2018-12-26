package com.epam.catgenome.controller.vo;

/**
 *
 */
public class ReadQuery {
    /**
     * {@code Long} ID of a BAM track in the system.
     */
    private Long id;
    /**
     * {@code Long} ID of a chromosome in the system.
     */
    private Long chromosomeId;
    /**
     * {@code Integer} Start position of read.
     */
    private Integer startIndex;
    /**
     * {@code Integer} End position of read.
     */
    private Integer endIndex;

    /**
     * {@code String} Name of read.
     */
    private String name;

    /**
     * {@code Long} specifies the project id that the track belong. Can be null.
     */
    protected Long projectId;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(Integer endIndex) {
        this.endIndex = endIndex;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    public Long getChromosomeId() {
        return chromosomeId;
    }

    public void setChromosomeId(Long chromosomeId) {
        this.chromosomeId = chromosomeId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
}
