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
 * Source:
 * Created:     12/30/2015
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * A view representation for registration a new file with an index
 * in the system, supports various file types and S3 file storage
 */
public class IndexedFileRegistrationRequest extends FileRegistrationRequest {
    private String indexPath;

    //for the case when the resource is located s3
    private Long indexS3BucketId;
    //
    private BiologicalDataItemResourceType indexType;

    public String getIndexPath() {
        return indexPath;
    }

    public void setIndexPath(String indexPath) {
        this.indexPath = indexPath;
    }

    public Long getIndexS3BucketId() {
        return indexS3BucketId;
    }

    public void setIndexS3BucketId(Long s3BucketId) {
        this.indexS3BucketId = s3BucketId;
    }

    public BiologicalDataItemResourceType getIndexType() {
        return indexType;
    }

    public void setIndexType(BiologicalDataItemResourceType type) {
        this.indexType = type;
    }

}
