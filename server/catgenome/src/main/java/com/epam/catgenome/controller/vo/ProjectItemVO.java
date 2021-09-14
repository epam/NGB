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
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.vcf.VcfSample;
import lombok.Data;

/**
 * Source:      ProjectItemVO
 * Created:     22.01.16, 15:27
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A View Object for simplified representation of ProjectItem entity
 * </p>
 */
@Data
public class ProjectItemVO {
    private Boolean hidden;
    private Short ordinalNumber;

    // BioDataItemProperties
    private Long id;
    private Long bioDataItemId;
    private String name;
    private BiologicalDataItemResourceType type;
    private String path;
    private String source;
    private BiologicalDataItemFormat format;
    private Long createdBy;
    private Date createdDate;
    private String prettyName;

    // FeatureFile properties
    private Long referenceId;
    private Boolean compressed;
    // VCF properties
    private List<VcfSample> samples;
    private Map<String, String> metadata;
    private Integer mask;
}
