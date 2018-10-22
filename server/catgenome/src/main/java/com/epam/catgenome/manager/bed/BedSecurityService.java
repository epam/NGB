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
import com.epam.catgenome.security.acl.aspect.AclMaskList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class BedSecurityService {

    @Autowired
    private BedManager bedManager;

    @Autowired
    private BedFileManager bedFileManager;

    @PreAuthorize("hasRole('ADMIN') OR hasRole('BED_MANAGER')")
    public BedFile registerBed(IndexedFileRegistrationRequest request) {
        return bedManager.registerBed(request);
    }

    @PreAuthorize("hasRole('ADMIN') OR hasRole('BED_MANAGER')")
    public BedFile unregisterBedFile(long bedFileId) throws IOException {
        return bedManager.unregisterBedFile(bedFileId);
    }

    @PreAuthorize("hasRole('USER')")
    public Track<BedRecord> loadFeatures(Track<BedRecord> track, String fileUrl, String indexUrl)
            throws FeatureFileReadingException {
        return bedManager.loadFeatures(track, fileUrl, indexUrl);
    }

    @PreAuthorize("hasPermission(#track.id, com.epam.catgenome.entity.bed.BedFile, 'READ')")
    public Track<BedRecord> loadFeatures(Track<BedRecord> track) throws FeatureFileReadingException {
        return bedManager.loadFeatures(track);
    }

    @AclMaskList
    @PostFilter("hasRole('ADMIN') OR hasPermission(filterObject, 'READ')")
    public List<BedFile> loadBedFilesByReferenceId(Long referenceId) {
        return bedFileManager.loadBedFilesByReferenceId(referenceId);
    }

    @PreAuthorize("hasPermission(#bedFileId, com.epam.catgenome.entity.bed.BedFile, 'WRITE')")
    public BedFile reindexBedFile(long bedFileId) throws FeatureIndexException {
        return bedManager.reindexBedFile(bedFileId);
    }

    @PreAuthorize("hasPermission(#histogramTrack.id, com.epam.catgenome.entity.bed.BedFile, 'READ')")
    public Track<Wig> loadHistogram(Track<Wig> histogramTrack) throws HistogramReadingException {
        return bedManager.loadHistogram(histogramTrack);
    }
}
