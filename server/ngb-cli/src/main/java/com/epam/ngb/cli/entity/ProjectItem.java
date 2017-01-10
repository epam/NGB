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

/**
 * {@code ProjectItem} is a shortened representation of a file, included in a dataset (ex.project).
 */
public class ProjectItem {

    /**
     * BiologicalDataItemID of a file
     */
    private Long bioDataItemId;
    /**
     * Determines item representation on the client, if true it isn't shown in the view
     */
    private boolean hidden;

    public ProjectItem() {
        //no op
    }

    public ProjectItem(Long bioDataItemId, boolean hidden) {
        this.bioDataItemId = bioDataItemId;
        this.hidden = hidden;
    }

    public Long getBioDataItemId() {
        return bioDataItemId;
    }

    public void setBioDataItemId(Long bioDataItemId) {
        this.bioDataItemId = bioDataItemId;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
