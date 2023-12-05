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
package com.epam.catgenome.manager.externaldb.patents;

import com.epam.catgenome.controller.vo.target.PatentsSearchRequest;
import com.epam.catgenome.entity.blast.BlastTask;
import com.epam.catgenome.entity.blast.result.BlastSequence;
import com.epam.catgenome.entity.externaldb.patents.SequencePatent;
import com.epam.catgenome.exception.BlastRequestException;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.target.ProteinPatentsManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Collection;

import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_USER;

@Service
@RequiredArgsConstructor
public class NCBIPatentsSecurityService {

    private final NCBIPatentsManager manager;
    private final ProteinPatentsManager proteinPatentsManager;

    @PreAuthorize(ROLE_USER)
    public SearchResult<SequencePatent> getPatents(final PatentsSearchRequest request)
            throws ExternalDbUnavailableException {
        return manager.getPatents(request);
    }

    @PreAuthorize(ROLE_USER)
    public BlastTask getPatents(final String sequence) throws BlastRequestException {
        return proteinPatentsManager.getPatents(sequence);
    }

    @PreAuthorize(ROLE_USER)
    public Collection<BlastSequence> getPatents(final Long targetId, final String sequenceId)
            throws BlastRequestException {
        return proteinPatentsManager.getPatents(targetId, sequenceId);
    }
}
