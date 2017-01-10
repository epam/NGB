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
 * Source:      FileRegistrationRequest
 * Created:     17.12.15, 18:02
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * A view representation for registration a new file in the system, supports various
 * file types and S3 file storage
 * </p>
 */
public class FileRegistrationRequest extends DefaultFileRegistrationRequest {
    private Long referenceId;

    //for the case when the resource is located s3
    private Long fileS3BucketId;
    //
    private BiologicalDataItemResourceType fileType;

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public Long getS3BucketId() {
        return fileS3BucketId;
    }

    public void setS3BucketId(Long s3BucketId) {
        this.fileS3BucketId = s3BucketId;
    }

    public BiologicalDataItemResourceType getType() {
        return fileType;
    }

    public void setType(BiologicalDataItemResourceType type) {
        this.fileType = type;
    }
}
