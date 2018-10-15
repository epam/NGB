package com.epam.catgenome.manager.bam;

import com.epam.catgenome.controller.vo.ReadQuery;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.entity.bam.BamQueryOption;
import com.epam.catgenome.entity.bam.Read;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.security.acl.aspect.AclFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.util.List;

@Service
public class BamSecurityService {

    @Autowired
    private BamFileManager bamFileManager;

    @Autowired
    private BamManager bamManager;

    @PreAuthorize("hasRole('ADMIN') or hasRole('BAM_MANAGER')")
    public BamFile registerBam(IndexedFileRegistrationRequest request) throws IOException {
        return bamManager.registerBam(request);
    }

    @PreAuthorize("hasPermission(#query.id, com.epam.catgenome.entity.bam.BamFile, 'READ')")
    public Read loadRead(ReadQuery query, String fileUrl, String indexUrl) throws IOException {
        return bamManager.loadRead(query, fileUrl, indexUrl);
    }

    @PreAuthorize("hasPermission(#bamFileId, com.epam.catgenome.entity.bam.BamFile, 'WRITE')")
    public BamFile unregisterBamFile(long bamFileId) throws IOException {
        return bamManager.unregisterBamFile(bamFileId);
    }

    public Track<Sequence> calculateConsensusSequence(Track<Sequence> convertToTrack) throws IOException {
        return bamManager.calculateConsensusSequence(convertToTrack);
    }


    @PreAuthorize("hasPermission(#track.id, com.epam.catgenome.entity.bam.BamFile, 'READ')")
    public void sendBamTrackToEmitter(Track<Read> track, BamQueryOption option, String fileUrl,
                                      String indexUrl, ResponseBodyEmitter emitter) throws IOException {
        if (fileUrl == null) {
            bamManager.sendBamTrackToEmitter(track, option, emitter);
        } else {
            bamManager.sendBamTrackToEmitterFromUrl(track, option, fileUrl, indexUrl, emitter);
        }
    }

    @AclFilter
    @PreAuthorize("hasPermission(#referenceId, com.epam.catgenome.entity.reference.Reference, 'READ')")
    public List<BamFile> loadBamFilesByReferenceId(Long referenceId) {
        return bamFileManager.loadBamFilesByReferenceId(referenceId);
    }
}
