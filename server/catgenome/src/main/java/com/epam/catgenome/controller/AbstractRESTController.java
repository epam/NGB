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

package com.epam.catgenome.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import com.epam.catgenome.manager.FileManager;

/**
 * Source:      AbstractRESTController.java
 * Created:     11/6/15, 7:40 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code AbstractRESTController} represents REST controller abstraction which only
 * provides miscellaneous utilities.
 * <p>
 * {@code AbstractRESTController} is designed to collect different REST controller
 * activities shared between different controller oriented to deal with certain
 * business entity.
 */
@Controller
@RequestMapping("/restapi")
public abstract class AbstractRESTController {
    private static final int BUF_SIZE = 2 * 1024;

    /**
     * Declares HTTP status OK code value, used to specify this code when REST API
     * is described, using Swagger-compliant annotations. It allows create nice
     * documentation automatically.
     */
    protected static final int HTTP_STATUS_OK = 200;

    /**
     * {@code String} specifies API responses description that explains meaning of different values
     * for $.status JSON path. It's required and used with swagger ApiResponses annotation.
     */
    protected static final String API_STATUS_DESCRIPTION = "It results in a response with HTTP status OK, but " +
        "you should always check $.status, which can take several values:<br/>" +
        "<b>OK</b> means call has been done without any problems;<br/>" +
        "<b>ERROR</b> means call has been aborted due to errors (see $.message for details in this case).";

    @Autowired
    private FileManager fileManager;

    /**
     * Transfers content of the given {@code Multipart} entity to a temporary file.
     *
     * @param multipart {@code Multipart} used to handle file uploads
     * @return {@code File} represents a reference on a file that has been created
     * from the given {@code Multipart}
     * @throws IOException
     */
    protected File transferToTempFile(final MultipartFile multipart) throws IOException {
        Assert.notNull(multipart);
        final File tmp = File.createTempFile(UUID.randomUUID().toString(), multipart.getOriginalFilename(),
            fileManager.getTempDir());
        multipart.transferTo(tmp);
        FileUtils.forceDeleteOnExit(tmp);
        return tmp;
    }

    /**
     * Joins back the file, transferred by chunks.
     *
     * @param multipart {@code Multipart} used to handle file chunk upload
     * @param id {@code Integer} identifies a specific file
     * @param fileName {@code String} original file name
     * @param chunk {@code int} current chunk number
     * @param chunks {@code int} overall number of chunks
     * @return {@code File} represents a reference on a temporary file that has been created
     * @throws IOException
     */
    protected File saveChunkedFile(MultipartFile multipart, Integer id, String fileName, int chunk, int chunks)
            throws IOException {
        File dst = new File(fileManager.getTempDir(), id + fileName);

        if (Objects.equals(chunk, chunks - 1)) {
            FileUtils.forceDeleteOnExit(dst);
        }

        try (
                InputStream inputStream = multipart.getInputStream();
                OutputStream out = dst.exists() ? new BufferedOutputStream(new FileOutputStream(dst, true), BUF_SIZE) :
                    new BufferedOutputStream(new FileOutputStream(dst), BUF_SIZE);
        ) {
            byte[] buffer = new byte[BUF_SIZE];
            int len = 0;
            while ((len = inputStream.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        }

        return dst;
    }
}
