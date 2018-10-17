/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2018 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.epam.catgenome.manager.maf;

import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.entity.maf.MafFile;
import com.epam.catgenome.entity.maf.MafRecord;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.security.acl.aspect.AclFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class MafSecurityService {

    @Autowired
    private MafManager mafManager;

    @Autowired
    private MafFileManager mafFileManager;

    @PreAuthorize("hasRole('ADMIN') or hasRole('MAF_MANAGER')")
    public MafFile registerMafFile(IndexedFileRegistrationRequest request) {
        return mafManager.registerMafFile(request);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MAF_MANAGER')")
    public MafFile unregisterMafFile(long mafFileId) throws IOException {
        return mafManager.unregisterMafFile(mafFileId);
    }

    @AclFilter
    @PreAuthorize("hasPermission(#referenceId, com.epam.catgenome.entity.reference.Reference, 'READ')")
    public List<MafFile> loadMafFilesByReferenceId(Long referenceId) {
        return mafFileManager.loadMafFilesByReferenceId(referenceId);
    }

    @PreAuthorize("hasPermission(#track.id, com.epam.catgenome.entity.maf.MafFile, 'READ')")
    public Track<MafRecord> loadFeatures(Track<MafRecord> track) throws IOException {
        return mafManager.loadFeatures(track);
    }
}
