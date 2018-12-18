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

package com.epam.ngb.cli.app;

/**
 * {@code ApplicationOptions} represents a set of available supplementary options for CLI
 * tools. These options are used to set optional fields in the tools and format
 * output of the tools.
 */
public class ApplicationOptions {

    /**
     * Specifies a name for file for registration on the server
     */
    private String name;
    /**
     * Specifies the format of the output for the commands: format output as a tab delimited
     * table with a header
     */
    private boolean printTable = false;

    /**
     * Specifies the format of the output for the commands: format output as json
     */
    private boolean printJson = false;

    /**
     * Specifies the search strategy: if true strict equality search is performed, otherwise
     * substring case insensitive search is done.
     */
    private boolean strictSearch = true;

    /**
     * Specifies a parent of a dataset for registration. If parent isn't specified, the dataset
     * is considered to be a root of hierarchy.
     */
    private String parent;
    /**
     * Specifies a gene file to be added to reference during registration, it can be ID, name or path
     */
    private String geneFile;

    /**
     * Specifies if a feature index for registered VCF or GFF/GTF file should be created
     */
    private boolean doIndex = true;

    /**
     * Option for reference registration, if true, the GC content files will be created for
     * a reference during registration.
     */
    private boolean noGCContent = false;

    /**
     * Specifies if tabix index should be rewritten along with feature index during file reindexing
     */
    private boolean createTabixIndex = true;

    /**
     * Option for reference registration, specifies a version of species during registration.
     */
    private String speciesVersion;


    private String location;

    private int maxMemory;

    private boolean forceDeletion;

    private String prettyName;

    private String datasets;

    private String files;

    private String users;

    private String groups;

    /**
     * Creates object with default option's values
     */
    public ApplicationOptions() {
        //no operations, all values are default
    }

    public String getFiles() {
        return files;
    }

    public void setFiles(String files) {
        this.files = files;
    }

    public String getUsers() {
        return users;
    }

    public void setUsers(String users) {
        this.users = users;
    }

    public String getGroups() {
        return groups;
    }

    public void setGroups(String groups) {
        this.groups = groups;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPrintJson() {
        return printJson;
    }

    public void setPrintJson(boolean printJson) {
        this.printJson = printJson;
    }

    public boolean isStrictSearch() {
        return strictSearch;
    }

    public void setStrictSearch(boolean strictSearch) {
        this.strictSearch = strictSearch;
    }

    public boolean isPrintTable() {
        return printTable;
    }

    public void setPrintTable(boolean printTable) {
        this.printTable = printTable;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getGeneFile() {
        return geneFile;
    }

    public void setGeneFile(String geneFile) {
        this.geneFile = geneFile;
    }

    public boolean isDoIndex() {
        return doIndex;
    }

    public void setDoIndex(boolean doIndex) {
        this.doIndex = doIndex;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isNoGCContent() {
        return noGCContent;
    }

    public void setNoGCContent(boolean noGCContent) {
        this.noGCContent = noGCContent;
    }

    public void setMaxMemory(int maxMemory) {
        this.maxMemory = maxMemory;
    }

    public int getMaxMemory() {
        return maxMemory;
    }

    public void setForceDeletion(boolean forceDeletion) {
        this.forceDeletion = forceDeletion;
    }

    public boolean isForceDeletion() {
        return forceDeletion;
    }

    public boolean isCreateTabixIndex() {
        return createTabixIndex;
    }

    public void setCreateTabixIndex(boolean createTabixIndex) {
        this.createTabixIndex = createTabixIndex;
    }

    public String getPrettyName() {
        return prettyName;
    }

    public void setPrettyName(String prettyName) {
        this.prettyName = prettyName;
    }

    public String getSpeciesVersion() {
        return speciesVersion;
    }

    public void setSpeciesVersion(String speciesVersion) {
        this.speciesVersion = speciesVersion;
    }

    public String getDatasets() {
        return datasets;
    }

    public void setDatasets(String datasets) {
        this.datasets = datasets;
    }
}
