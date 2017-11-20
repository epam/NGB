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

package com.epam.ngb.cli.manager.command;

/**
 * {@code ServerParameters} represents an NGB server configuration, required for
 * HTTP commands execution
 */
public class ServerParameters {

    private String serverUrl;
    private String searchUrl;
    private String registrationUrl;
    private String projectLoadUrl;
    private String projectTreeUrl;
    private String versionUrl;
    private String projectLoadByIdUrl;
    private String fileFindUrl;
    private String serverVersion;
    private String existingIndexSearchUrl;
    private String jwtAuthenticationToken;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getSearchUrl() {
        return searchUrl;
    }

    public void setSearchUrl(String searchUrl) {
        this.searchUrl = searchUrl;
    }

    public String getRegistrationUrl() {
        return registrationUrl;
    }

    public void setRegistrationUrl(String registrationUrl) {
        this.registrationUrl = registrationUrl;
    }

    public String getProjectLoadUrl() {
        return projectLoadUrl;
    }

    public void setProjectLoadUrl(String projectLoadUrl) {
        this.projectLoadUrl = projectLoadUrl;
    }

    public String getVersionUrl() {
        return versionUrl;
    }

    public void setVersionUrl(String versionUrl) {
        this.versionUrl = versionUrl;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public String getProjectLoadByIdUrl() {
        return projectLoadByIdUrl;
    }

    public void setProjectLoadByIdUrl(String projectLoadByIdUrl) {
        this.projectLoadByIdUrl = projectLoadByIdUrl;
    }

    public String getFileFindUrl() {
        return fileFindUrl;
    }

    public void setFileFindUrl(String fileFindUrl) {
        this.fileFindUrl = fileFindUrl;
    }

    public String getProjectTreeUrl() {
        return projectTreeUrl;
    }

    public void setProjectTreeUrl(String projectTreeUrl) {
        this.projectTreeUrl = projectTreeUrl;
    }

    public String getExistingIndexSearchUrl() {
        return existingIndexSearchUrl;
    }

    public void setExistingIndexSearchUrl(String existingIndexSearchUrl) {
        this.existingIndexSearchUrl = existingIndexSearchUrl;
    }

    public String getJwtAuthenticationToken() {
        return jwtAuthenticationToken;
    }

    public void setJwtAuthenticationToken(String jwtAuthenticationToken) {
        this.jwtAuthenticationToken = jwtAuthenticationToken;
    }
}
