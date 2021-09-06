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

package com.epam.catgenome.entity.project;

import java.util.Objects;

import com.epam.catgenome.entity.BiologicalDataItem;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Source:      ProjectItem
 * Created:     21.01.16, 18:33
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * Represents a track, that is included in a project.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
public class ProjectItem {
    private Long id;
    private Boolean hidden;
    private BiologicalDataItem bioDataItem;
    private Short ordinalNumber;

    public ProjectItem(BiologicalDataItem bioDataItem) {
        this.bioDataItem = bioDataItem;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == this.getClass() && Objects.equals(this.getBioDataItem(),
                ((ProjectItem) obj).getBioDataItem());
    }

    @Override
    public int hashCode() {
        return bioDataItem != null ? BiologicalDataItem.getBioDataItemId(bioDataItem).intValue() : 0;
    }
}
