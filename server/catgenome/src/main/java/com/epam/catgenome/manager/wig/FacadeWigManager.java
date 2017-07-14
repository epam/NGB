/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

package com.epam.catgenome.manager.wig;

import java.io.IOException;

import com.epam.catgenome.util.NgbFileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.epam.catgenome.controller.vo.registration.FileRegistrationRequest;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.entity.wig.WigFile;

/**
 * Source:      FacadeWigManager.java
 * Created:     1/21/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * {@code FacadeWigManager} represents a service class designed to encapsulate all business
 * logic operations required to manage {@code WigFile} and corresponded tracks, e.g. to process
 * variants uploads, position-based and/or zoom queries etc.
 */
@Service
public class FacadeWigManager {

    @Autowired
    private WigFileManager wigFileManager;

    @Autowired
    private WigManager wigManager;

    @Autowired
    private BedGraphManager bedGraphManager;


    /**
     * Loads the wig data for a track
     * @param track to load data
     * @return track filled with data
     * @throws IOException
     */
    public Track<Wig> getWigTrack(Track<Wig> track) throws IOException {
        final WigFile wigFile = wigFileManager.loadWigFile(track.getId());
        return fetchWigManager(wigFile.getPath()).getWigTrack(track);
    }

    /**
     * Saves a {@code WigFile} on the server. File metadata is saved in the database
     * @param request
     * @return
     */
    public WigFile registerWigFile(final FileRegistrationRequest request) {
        return fetchWigManager(request.getPath()).registerWigFile(request);
    }

    /**
     * Removes wig file metadata from the system, deleting all additional files that were created
     *
     * @param wigFileId {@code long} a wig fiel ID
     * @return deleted {@code WigFile} entity
     * @throws IOException
     */
    public WigFile unregisterWigFile(final long wigFileId) throws IOException {
        WigFile fileToDelete = wigFileManager.loadWigFile(wigFileId);
        return fetchWigManager(fileToDelete.getPath()).unregisterWigFile(wigFileId);
    }

    private AbstractWigManager fetchWigManager(String path) {
        String fileExtension = NgbFileUtils.getFileExtension(path);
        boolean isBedGraph = AbstractWigManager.BED_GRAPH_EXTENSIONS
                .stream()
                .anyMatch(fileExtension::endsWith);
        if (isBedGraph) {
            return bedGraphManager;
        } else {
            return wigManager;
        }
    }

}
