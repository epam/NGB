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

package com.epam.catgenome.manager.wig;

import com.epam.catgenome.controller.vo.registration.FileRegistrationRequest;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.entity.wig.WigFile;
import com.epam.catgenome.security.acl.aspect.AclMaskList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class WigSecurityService {

    @Autowired
    private WigFileManager wigFileManager;

    @Autowired
    private FacadeWigManager facadeWigManager;


    @AclMaskList
    @PostFilter("hasRole('ADMIN') OR hasPermission(filterObject, 'READ')")
    public List<WigFile> loadWigFilesByReferenceId(Long referenceId) {
        return wigFileManager.loadWigFilesByReferenceId(referenceId);
    }

    @PreAuthorize("hasRole('ADMIN') OR hasRole('WIG_MANAGER')")
    public WigFile registerWigFile(FileRegistrationRequest request) {
        return facadeWigManager.registerWigFile(request);
    }

    @PreAuthorize("hasPermission(#track.id, 'com.epam.catgenome.entity.wig.WigFile', 'READ')")
    public Track<Wig> getWigTrack(Track<Wig> track) throws IOException {
        return facadeWigManager.getWigTrack(track);
    }

    @PreAuthorize("hasRole('ADMIN') OR hasRole('WIG_MANAGER')")
    public WigFile unregisterWigFile(long wigFileId) throws IOException {
        return facadeWigManager.unregisterWigFile(wigFileId);
    }
}
