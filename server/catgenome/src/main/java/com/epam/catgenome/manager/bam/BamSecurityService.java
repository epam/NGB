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

import com.epam.catgenome.controller.vo.ReadQuery;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.entity.bam.BamQueryOption;
import com.epam.catgenome.entity.bam.Read;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.track.Track;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;

import static com.epam.catgenome.security.acl.SecurityExpressions.*;

@Service
@RequiredArgsConstructor
public class BamSecurityService {

    private static final String READ_BAM_BY_TRACK_ID =
            "hasPermissionOnFileOrParentProject(#track.id, 'com.epam.catgenome.entity.bam.BamFile', " +
                    "#track.projectId, 'READ')";

    private static final String READ_BAM_BY_QUERY_ID =
            "hasPermissionOnFileOrParentProject(#query.id, 'com.epam.catgenome.entity.bam.BamFile', " +
                    "#query.projectId, 'READ')";

    private final BamManager bamManager;

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_BAM_MANAGER)
    public BamFile registerBam(IndexedFileRegistrationRequest request) throws IOException {
        return bamManager.registerBam(request);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_BAM_BY_QUERY_ID)
    public Read loadRead(ReadQuery query, String fileUrl, String indexUrl) throws IOException {
        return bamManager.loadRead(query, fileUrl, indexUrl);
    }

    @PreAuthorize(ROLE_ADMIN + OR + ROLE_BAM_MANAGER)
    public BamFile unregisterBamFile(long bamFileId) throws IOException {
        return bamManager.unregisterBamFile(bamFileId);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_BAM_BY_TRACK_ID)
    public Track<Sequence> calculateConsensusSequence(Track<Sequence> track) throws IOException {
        return bamManager.calculateConsensusSequence(track);
    }

    @PreAuthorize(ROLE_ADMIN + OR + READ_BAM_BY_TRACK_ID)
    public void sendBamTrackToEmitter(Track<Read> track, BamQueryOption option,
                                      ResponseBodyEmitter emitter) throws IOException {
        bamManager.sendBamTrackToEmitter(track, option, emitter);
    }

    @PreAuthorize(ROLE_USER)
    public void sendBamTrackToEmitterFromUrl(Track<Read> track, BamQueryOption option, String fileUrl, String indexUrl,
                                             ResponseBodyEmitter emitter) throws IOException {
        bamManager.sendBamTrackToEmitterFromUrl(track, option, fileUrl, indexUrl, emitter);
    }
}
