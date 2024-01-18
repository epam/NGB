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
package com.epam.catgenome.manager.target;

import com.epam.catgenome.entity.target.TargetGene;
import com.epam.catgenome.exception.TargetGenesException;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.index.SearchRequest;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_USER;

@Service
@RequiredArgsConstructor
public class TargetGeneSecurityService {

    private final TargetGeneManager targetGeneManager;

    @PreAuthorize(ROLE_USER)
    public void importGenes(final String path, final long targetId)
            throws IOException, ParseException, TargetGenesException {
        targetGeneManager.importData(path, targetId);
    }

    @PreAuthorize(ROLE_USER)
    public void create(final long targetId, final List<TargetGene> targetGenes) throws IOException {
        targetGeneManager.create(targetId, targetGenes);
    }

    @PreAuthorize(ROLE_USER)
    public void update(final List<TargetGene> targetGenes) throws IOException, ParseException {
        targetGeneManager.update(targetGenes);
    }

    @PreAuthorize(ROLE_USER)
    public void delete(final Long targetId, final List<String> geneIds) throws ParseException, IOException {
        targetGeneManager.delete(targetId, geneIds);
    }

    @PreAuthorize(ROLE_USER)
    public SearchResult<TargetGene> filter(final long targetId, final SearchRequest request)
            throws ParseException, IOException {
        return targetGeneManager.filter(targetId, request);
    }

    @PreAuthorize(ROLE_USER)
    public Set<String> getFields(final long targetId) throws ParseException, IOException {
        return targetGeneManager.getFields(targetId);
    }

    @PreAuthorize(ROLE_USER)
    public List<String> getFieldValues(final long targetId, final String field) throws ParseException, IOException {
        return targetGeneManager.getFieldValues(targetId, field);
    }
}
