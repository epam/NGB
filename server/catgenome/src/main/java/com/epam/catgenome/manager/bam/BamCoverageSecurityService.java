/*
 * MIT License
 *
 * Copyright (c) 2018-2022 EPAM Systems
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

package com.epam.catgenome.manager.bam;

import com.epam.catgenome.entity.bam.BamCoverage;
import com.epam.catgenome.entity.bam.CoverageInterval;
import com.epam.catgenome.entity.bam.CoverageQueryParams;
import com.epam.catgenome.util.db.Page;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.epam.catgenome.security.acl.SecurityExpressions.*;

@Service
@RequiredArgsConstructor
public class BamCoverageSecurityService {

    private final BamCoverageManager coverageManager;

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_BAM_MANAGER)
    public BamCoverage createCoverage(final BamCoverage coverage) throws IOException {
        return coverageManager.create(coverage);
    }

    @PreAuthorize(ROLE_USER)
    public List<BamCoverage> loadAll() throws IOException {
        return coverageManager.loadAll();
    }

    @PreAuthorize(ROLE_USER)
    public List<BamCoverage> loadByBamId(final Set<Long> bamIds) throws IOException {
        return coverageManager.loadByBamId(bamIds);
    }

    @PreAuthorize(ROLE_USER)
    public Page<CoverageInterval> loadCoverage(final CoverageQueryParams params) throws ParseException, IOException {
        return coverageManager.search(params);
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_BAM_MANAGER)
    public void deleteCoverage(final Long bamId, final Integer step) throws IOException, ParseException {
        coverageManager.delete(bamId, step);
    }
}
