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
package com.epam.catgenome.manager.pdb;

import com.epam.catgenome.entity.pdb.PdbFile;
import com.epam.catgenome.entity.pdb.PdbFileQueryParams;
import com.epam.catgenome.util.db.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_USER;

@Service
@RequiredArgsConstructor
public class PdbFileSecurityService {

    private final PdbFileManager manager;

    @PreAuthorize(ROLE_USER)
    public PdbFile create(final PdbFile pdbFile) throws IOException {
        return manager.create(pdbFile);
    }

    @PreAuthorize(ROLE_USER)
    public void updateMetadata(final long pdbFileId, final Map<String, String> metadata) {
        manager.updateMetadata(pdbFileId, metadata);
    }

    @PreAuthorize(ROLE_USER)
    public void delete(final long pdbFileId) {
        manager.delete(pdbFileId);
    }

    @PreAuthorize(ROLE_USER)
    public PdbFile load(final long pdbFileId) {
        return manager.load(pdbFileId);
    }

    @PreAuthorize(ROLE_USER)
    public List<PdbFile> load() {
        return manager.load();
    }

    @PreAuthorize(ROLE_USER)
    public Page<PdbFile> load(final PdbFileQueryParams params) {
        return manager.load(params);
    }

    @PreAuthorize(ROLE_USER)
    public byte[] loadContent(final long pdbFileId) {
        return manager.loadContent(pdbFileId);
    }
}
