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

package com.epam.ngb.cli.entity;

import java.util.List;

/**
 * VO for sending registration requests to NGB server
 */
public class RegistrationRequest implements RequestPayload {
    private String path;
    private String name;
    private String indexPath;
    private BiologicalDataItemResourceType type;
    private BiologicalDataItemResourceType indexType;
    private Long referenceId;
    private List<ProjectItem> items;
    private Long geneFileId;
    private RegistrationRequest geneFileRequest;
    private Boolean doIndex;
    private Boolean noGCContent;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndexPath() {
        return indexPath;
    }

    public void setIndexPath(String indexPath) {
        this.indexPath = indexPath;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public List<ProjectItem> getItems() {
        return items;
    }

    public void setItems(List<ProjectItem> items) {
        this.items = items;
    }

    public RegistrationRequest getGeneFileRequest() {
        return geneFileRequest;
    }

    public void setGeneFileRequest(RegistrationRequest geneFileRequest) {
        this.geneFileRequest = geneFileRequest;
    }

    public Long getGeneFileId() {
        return geneFileId;
    }

    public void setGeneFileId(Long geneFileId) {
        this.geneFileId = geneFileId;
    }

    public Boolean getDoIndex() {
        return doIndex;
    }

    public void setDoIndex(Boolean doIndex) {
        this.doIndex = doIndex;
    }

    public Boolean getNoGCContent() {
        return noGCContent;
    }

    public void setNoGCContent(Boolean noGCContent) {
        this.noGCContent = noGCContent;
    }

    public BiologicalDataItemResourceType getType() {
        return type;
    }

    public void setType(BiologicalDataItemResourceType type) {
        this.type = type;
    }

    public BiologicalDataItemResourceType getIndexType() {
        return indexType;
    }

    public void setIndexType(BiologicalDataItemResourceType indexType) {
        this.indexType = indexType;
    }
}
