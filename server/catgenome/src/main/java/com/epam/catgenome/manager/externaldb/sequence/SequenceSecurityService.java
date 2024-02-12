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
package com.epam.catgenome.manager.externaldb.sequence;

import com.epam.catgenome.controller.vo.sequence.LocalSequenceRequest;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.exception.ReferenceReadingException;
import com.epam.catgenome.exception.TargetGenesException;
import com.epam.catgenome.manager.externaldb.ncbi.util.NCBISequenceDatabase;
import com.epam.catgenome.manager.sequence.SequencesManager;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_USER;

@Service
@RequiredArgsConstructor
public class SequenceSecurityService {

    private final NCBISequenceManager manager;
    private final SequencesManager sequencesManager;

    @PreAuthorize(ROLE_USER)
    public String getFasta(final NCBISequenceDatabase database, final String id) throws ExternalDbUnavailableException {
        return manager.getFasta(database, id);
    }

    @PreAuthorize(ROLE_USER)
    public String getSequence(final LocalSequenceRequest request) throws ExternalDbUnavailableException,
            TargetGenesException, ReferenceReadingException, ParseException, IOException {
        return sequencesManager.getSequence(request);
    }
}
