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

package com.epam.catgenome.controller.vo.externaldb;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Source:      NCBIEntryVO
 * Created:     09.02.16, 13:52
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.0.2, JDK 1.8
 *
 * <p>
 * class for NCBI Clin-Var DB data (summary)
 * </p>
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NCBISummaryVO {

    @JsonProperty(value = "uid")
    private String uid;

    @JsonProperty(value = "title")
    private String title;

    @JsonProperty(value = "source")
    private String source;

    @JsonProperty(value = "authors")
    private List<NCBIAuthor> authors;

    @JsonProperty(value = "author")
    private NCBIAuthor author;

    @JsonProperty(value = "pubdate")
    private String date;

    @JsonProperty(value = "link")
    private String link;

    @JsonProperty(value = "multiple_authors")
    private Boolean multipleAuthors;

    @JsonProperty(value = "biosystem")
    private NCBIBiosystem biosystem;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<NCBIAuthor> getAuthors() {
        return authors;
    }

    public void setAuthors(List<NCBIAuthor> authors) {
        this.authors = authors;
    }

    public NCBIBiosystem getBiosystem() {
        return biosystem;
    }

    public void setBiosystem(NCBIBiosystem biosystem) {
        this.biosystem = biosystem;
    }

    public NCBIAuthor getAuthor() {
        return author;
    }

    public void setAuthor(NCBIAuthor author) {
        this.author = author;
    }

    public Boolean getMultipleAuthors() {
        return multipleAuthors;
    }

    public void setMultipleAuthors(Boolean multipleAuthors) {
        this.multipleAuthors = multipleAuthors;
    }

    @Override
    public String toString() {
        return "NCBISummaryVO{" +
                "uid='" + uid + '\'' +
                ", title='" + title + '\'' +
                ", source='" + source + '\'' +
                ", authors=" + authors +
                ", biosystem=" + biosystem +
                '}';
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NCBIAuthor {
        private String name;
        private String authtype;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAuthtype() {
            return authtype;
        }

        public void setAuthtype(String authtype) {
            this.authtype = authtype;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NCBIBiosystem {

        @JsonProperty(value = "biosystemname")
        private String biosystemname;

        @JsonProperty(value = "biosystemtype")
        private String biosystemtype;

        @JsonProperty(value = "description")
        private String description;

        public String getBiosystemname() {
            return biosystemname;
        }

        public void setBiosystemname(String biosystemname) {
            this.biosystemname = biosystemname;
        }

        public String getBiosystemtype() {
            return biosystemtype;
        }

        public void setBiosystemtype(String biosystemtype) {
            this.biosystemtype = biosystemtype;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return "NCBIBiosystem{" +
                    "biosystemname='" + biosystemname + '\'' +
                    ", biosystemtype='" + biosystemtype + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }
}
