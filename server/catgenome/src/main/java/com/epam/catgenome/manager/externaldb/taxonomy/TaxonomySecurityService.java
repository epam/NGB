/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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
package com.epam.catgenome.manager.externaldb.taxonomy;

import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_USER;

@Service
public class TaxonomySecurityService {

    @Autowired
    private TaxonomyManager taxonomyManager;

    @PreAuthorize(ROLE_USER)
    public List<Taxonomy> searchOrganisms(final String term)
            throws IOException, ParseException {
        return taxonomyManager.searchOrganisms(term, null);
    }

    @PreAuthorize(ROLE_USER)
    public Taxonomy searchOrganismById(final long taxId) throws IOException, ParseException {
        return taxonomyManager.searchOrganismById(taxId);
    }

    @PreAuthorize(ROLE_USER)
    public void writeLuceneTaxonomyIndex(final String taxonomyFilePath) throws IOException, ParseException {
        taxonomyManager.writeLuceneTaxonomyIndex(taxonomyFilePath);
    }
}
