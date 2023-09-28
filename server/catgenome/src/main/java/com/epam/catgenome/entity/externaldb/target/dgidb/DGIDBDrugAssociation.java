/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
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
package com.epam.catgenome.entity.externaldb.target.dgidb;

import com.epam.catgenome.entity.externaldb.target.opentargets.UrlEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DGIDBDrugAssociation extends UrlEntity {
    public static final String URL_PATTERN = "https://www.dgidb.org/drugs/%s#_summary";
    private String geneId;
    private String entrezId;
    private String claimName;
    private String interactionTypes;
    private String interactionClaimSource;
    @Builder
    public DGIDBDrugAssociation(String id, String name, String url, String entrezId, String geneId,
                                String claimName, String interactionTypes, String interactionClaimSource) {
        super(id, name, url);
        this.entrezId = entrezId;
        this.geneId = geneId;
        this.claimName = claimName;
        this.interactionTypes = interactionTypes;
        this.interactionClaimSource = interactionClaimSource;
    }
}
