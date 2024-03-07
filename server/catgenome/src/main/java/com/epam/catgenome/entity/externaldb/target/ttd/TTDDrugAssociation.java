/*
 * MIT License
 *
 * Copyright (c) 2024 EPAM Systems
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
package com.epam.catgenome.entity.externaldb.target.ttd;

import com.epam.catgenome.entity.externaldb.target.Association;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TTDDrugAssociation extends Association {
    public static final String URL_PATTERN = "https://idrblab.net/ttd/data/drug/details/%s";
    private String ttdGeneId;
    private String ttdTarget;
    private String company;
    private String type;
    private String therapeuticClass;
    private String inChI;
    private String inChIKey;
    private String canonicalSmiles;
    private String status;
    private String compoundClass;
    @Builder
    public TTDDrugAssociation(String id, String name, String url, String geneId, String target,
                              String ttdGeneId, String ttdTarget, String company, String type,
                              String therapeuticClass, String inChI, String inChIKey,
                              String canonicalSmiles, String status, String compoundClass) {
        super(id, name, url, geneId, target);
        this.ttdGeneId = ttdGeneId;
        this.ttdTarget = ttdTarget;
        this.company = company;
        this.type = type;
        this.therapeuticClass = therapeuticClass;
        this.inChI = inChI;
        this.inChIKey = inChIKey;
        this.canonicalSmiles = canonicalSmiles;
        this.status = status;
        this.compoundClass = compoundClass;
    }
}
