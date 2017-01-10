/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.epam.catgenome.controller.vo;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.epam.catgenome.entity.BiologicalDataItemFormat;

/**
 * Source:      ProjectVO
 * Created:     22.01.16, 15:03
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A View Object for Project entity representation
 * </p>
 */
public class ProjectVO {
    private Long id;
    private String name;
    private Long createdBy;
    private Date createdDate;
    private List<ProjectItemVO> items;
    private Integer itemsCount;
    private Map<BiologicalDataItemFormat, Integer> itemsCountPerFormat;
    private Date lastOpenedDate;
    private List<ProjectVO> nestedProjects;
    private Long parentId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public List<ProjectItemVO> getItems() {
        return items;
    }

    public void setItems(List<ProjectItemVO> items) {
        this.items = items;
    }

    public Integer getItemsCount() {
        return itemsCount;
    }

    public void setItemsCount(Integer itemsCount) {
        this.itemsCount = itemsCount;
    }

    public Map<BiologicalDataItemFormat, Integer> getItemsCountPerFormat() {
        return itemsCountPerFormat;
    }

    public void setItemsCountPerFormat(Map<BiologicalDataItemFormat, Integer> itemsCountPerFormat) {
        this.itemsCountPerFormat = itemsCountPerFormat;
    }

    public Date getLastOpenedDate() {
        return lastOpenedDate;
    }

    public void setLastOpenedDate(Date lastOpenedDate) {
        this.lastOpenedDate = lastOpenedDate;
    }

    public List<ProjectVO> getNestedProjects() {
        return nestedProjects;
    }

    public void setNestedProjects(List<ProjectVO> nestedProjects) {
        this.nestedProjects = nestedProjects;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
