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

package com.epam.catgenome.manager.bed;

import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.entity.bed.BedFile;
import com.epam.catgenome.entity.bed.BedRecord;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.exception.FeatureFileReadingException;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.exception.HistogramReadingException;
import com.epam.catgenome.security.acl.aspect.AclMask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.AccessDeniedException;

import static com.epam.catgenome.security.acl.SecurityExpressions.*;

@Service
public class BedSecurityService {

    private static final String READ_ON_FILE_OR_PROJECT_BY_TRACK = "hasPermissionOnFileOrParentProject(#track.id, " +
            "'com.epam.catgenome.entity.bed.BedFile', #track.projectId, 'READ')";

    @Autowired
    private BedManager bedManager;

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_BED_MANAGER)
    public BedFile registerBed(IndexedFileRegistrationRequest request) {
        return bedManager.registerBed(request);
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_BED_MANAGER)
    public BedFile unregisterBedFile(long bedFileId) throws IOException {
        return bedManager.unregisterBedFile(bedFileId);
    }

    @PreAuthorize(ROLE_USER)
    public Track<BedRecord> loadFeatures(Track<BedRecord> track, String fileUrl, String indexUrl)
            throws FeatureFileReadingException, AccessDeniedException {
        return bedManager.loadFeatures(track, fileUrl, indexUrl);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_ON_FILE_OR_PROJECT_BY_TRACK)
    public Track<BedRecord> loadFeatures(Track<BedRecord> track) throws IOException {
        return bedManager.loadFeatures(track);
    }

    @AclMask
    @PreAuthorize(ROLE_ADMIN + OR + ROLE_BED_MANAGER)
    public BedFile reindexBedFile(long bedFileId) throws FeatureIndexException, IOException {
        return bedManager.reindexBedFile(bedFileId);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_ON_FILE_OR_PROJECT_BY_TRACK)
    public Track<Wig> loadHistogram(Track<Wig> track) throws HistogramReadingException {
        return bedManager.loadHistogram(track);
    }
}
