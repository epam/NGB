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
 *
 */

package com.epam.catgenome.controller.vo.registration;

import com.epam.catgenome.entity.BiologicalDataItemResourceType;

/**
 * Source:      ReferenceRegistrationRequest
 * Created:     09.12.16, 18:07
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 15.0.3, JDK 1.8
 * <p>
 * A file registration request, designed for reference genome registration. A gene file can be specified for reference
 * being registered through geneFileId field (if a GeneFIle already exists in the system) of by geneFileRequest, if it
 * needs to be registered
 * </p>
 */
public class ReferenceRegistrationRequest extends DefaultFileRegistrationRequest {
    private BiologicalDataItemResourceType fileType;
    private Long geneFileId;
    private FeatureIndexedFileRegistrationRequest geneFileRequest;

    public BiologicalDataItemResourceType getType() {
        return fileType;
    }

    public void setType(BiologicalDataItemResourceType type) {
        this.fileType = type;
    }

    public Long getGeneFileId() {
        return geneFileId;
    }

    public void setGeneFileId(Long geneFileId) {
        this.geneFileId = geneFileId;
    }

    public FeatureIndexedFileRegistrationRequest getGeneFileRequest() {
        return geneFileRequest;
    }

    public void setGeneFileRequest(FeatureIndexedFileRegistrationRequest geneFileRequest) {
        this.geneFileRequest = geneFileRequest;
    }
}
