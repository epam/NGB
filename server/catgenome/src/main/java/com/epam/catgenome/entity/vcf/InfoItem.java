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

package com.epam.catgenome.entity.vcf;

import java.util.Objects;

import htsjdk.variant.vcf.VCFHeaderLineCount;
import htsjdk.variant.vcf.VCFHeaderLineType;
import htsjdk.variant.vcf.VCFInfoHeaderLine;

/**
 * {@code InfoItem} represents specification of an INFO field from a VCF file
 */
public class InfoItem {

    private String name;
    private VCFHeaderLineType type;
    private String description;

    public InfoItem() {
        //no-op
    }

    /**
     * @param name of the field
     * @param type of the field value
     * @param description of the field
     */
    public InfoItem(String name, VCFHeaderLineType type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
    }

    public InfoItem(VCFInfoHeaderLine line) {
        this.name = line.getID();
        if (line.getCountType() != VCFHeaderLineCount.INTEGER) {
            this.type = VCFHeaderLineType.String;
        } else {
            this.type = line.getType();
        }
        this.description = line.getDescription();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VCFHeaderLineType getType() {
        return type;
    }

    public void setType(VCFHeaderLineType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InfoItem infoItem = (InfoItem) o;
        return Objects.equals(name, infoItem.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}

