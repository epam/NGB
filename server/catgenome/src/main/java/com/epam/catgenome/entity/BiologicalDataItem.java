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

package com.epam.catgenome.entity;

import java.util.Date;
import java.util.Objects;

import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.security.AbstractSecuredEntity;
import com.epam.catgenome.entity.security.AclClass;

/**
 * Source:      BiologicalItem
 * Created:     17.12.15, 12:44
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * Represents a biological data item instance from the database, which encapsulates common metadata properties of
 * biological data files, registered in the system.
 * </p>
 */
public class BiologicalDataItem extends AbstractSecuredEntity {

    private BiologicalDataItemResourceType type;
    private String path;
    private BiologicalDataItemFormat format;
    private Date createdDate;
    private Long bucketId;

    public BiologicalDataItem() {
        // no-op
    }

    public BiologicalDataItem(Long id) {
        setId(id);
    }

    @Override
    public AbstractSecuredEntity getParent() {
        return null;
    }

    @Override
    public AclClass getAclClass() {
        return null;
    }

    public BiologicalDataItemResourceType getType() {
        return type;
    }

    public void setType(BiologicalDataItemResourceType type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public BiologicalDataItemFormat getFormat() {
        return format;
    }

    public final void setFormat(BiologicalDataItemFormat format) {
        this.format = format;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Long getBucketId() {
        return bucketId;
    }

    public void setBucketId(Long bucketId) {
        this.bucketId = bucketId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return obj.getClass() == this.getClass() && Objects.equals(getId(), ((BiologicalDataItem) obj).getId());
    }

    @Override
    public int hashCode() {
        return (int) BiologicalDataItem.getBioDataItemId(this).longValue();
    }

    /**
     * Returns biological data item ID of any {@link BiologicalDataItem} ancestor
     * @param item an {@link BiologicalDataItem} ancestor
     * @return {@link BiologicalDataItem} ancestor's biological data item ID
     */
    public static Long getBioDataItemId(BiologicalDataItem item) {
        if (item instanceof FeatureFile) {
            return ((FeatureFile) item).getBioDataItemId();
        } else {
            if (item instanceof Reference) {
                return ((Reference) item).getBioDataItemId();
            } else {
                return item.getId();
            }
        }
    }
}
