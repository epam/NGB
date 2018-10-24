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

package com.epam.catgenome.manager.seg;

import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.entity.seg.SegFile;
import com.epam.catgenome.entity.seg.SegRecord;
import com.epam.catgenome.entity.track.SampledTrack;
import com.epam.catgenome.security.acl.aspect.AclMaskList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

import static com.epam.catgenome.security.acl.SecurityExpressions.*;

@Service
public class SegSecurityService {

    private static final String READ_SEG_FILE_BY_ID =
            "hasPermission(#track.id, com.epam.catgenome.entity.seg.SegFile, 'READ')";

    @Autowired
    private SegFileManager segFileManager;

    @Autowired
    private SegManager segManager;


    @PreAuthorize(ROLE_ADMIN + OR + ROLE_SEG_MANAGER)
    public SegFile registerSegFile(IndexedFileRegistrationRequest request) {
        return segManager.registerSegFile(request);
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_SEG_MANAGER)
    public SegFile unregisterSegFile(long segFileId) throws IOException {
        return segManager.unregisterSegFile(segFileId);
    }

    @AclMaskList
    @PostFilter(ROLE_ADMIN + OR + READ_ON_FILTER_OBJECT)
    public List<SegFile> loadSedFilesByReferenceId(Long referenceId) {
        return segFileManager.loadSedFilesByReferenceId(referenceId);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_SEG_FILE_BY_ID)
    public SampledTrack<SegRecord> loadFeatures(SampledTrack<SegRecord> track) throws IOException {
        return segManager.loadFeatures(track);
    }
}
