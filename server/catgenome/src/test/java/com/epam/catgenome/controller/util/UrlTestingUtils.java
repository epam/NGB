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

package com.epam.catgenome.controller.util;

import java.io.File;
import java.io.IOException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * Source:      UrlTestingUtils
 * Created:     28.10.16, 14:11
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
public final class UrlTestingUtils {
    public static final int TEST_FILE_SERVER_PORT = 8875;
    public static final String TEST_FILE_SERVER_URL = "http://localhost:8875";

    private static final Logger LOGGER = LoggerFactory.getLogger(UrlTestingUtils.class);

    private UrlTestingUtils() {
    }

    /**
     * A local HTTP file server over {@code classpath:templates}, used by the tests that register a
     * track from a URL rather than from a path.
     *
     * <p>This used to be written against Jetty's {@code AbstractHandler}. Jetty 12, which Boot 3.5
     * manages, deleted that class along with the rest of the servlet API in
     * {@code jetty-server}: core handlers now see Jetty's own {@code Request}/{@code Response}/{@code
     * Callback}, and anything that wants {@code HttpServletRequest} goes through
     * {@code org.eclipse.jetty.ee10.servlet}. Since the body only ever needed the request URI and the
     * two servlet objects to hand to {@link MultipartFileSender}, a plain {@link HttpServlet} in a
     * {@link ServletContextHandler} is the smaller of the two translations.
     */
    public static Server getFileServer(ApplicationContext context) {
        Resource resource = context.getResource("classpath:templates");

        Server server = new Server(TEST_FILE_SERVER_PORT);
        ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath("/");
        handler.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) {
                String uri = request.getRequestURI();
                LOGGER.info(uri);
                try {
                    File file = new File(resource.getFile().getAbsolutePath() + uri);
                    MultipartFileSender.fromFile(file).with(request).with(response).serveResource();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }), "/*");
        server.setHandler(handler);

        return server;
    }
}
