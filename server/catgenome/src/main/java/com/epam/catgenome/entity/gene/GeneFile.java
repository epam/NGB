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

package com.epam.catgenome.entity.gene;

import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.externaldb.ExternalDB;
import com.epam.catgenome.entity.externaldb.ExternalDBType;
import com.epam.catgenome.entity.security.AclClass;

/**
 * Source:      GeneFile
 * Created:     04.12.15, 18:50
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * Represents gene file instance in a database.
 * </p>
 */
public class GeneFile extends FeatureFile {

    private ExternalDBType externalDBType;
    private ExternalDB externalDB;
    private String externalDBOrganism;

    public GeneFile() {
        setFormat(BiologicalDataItemFormat.GENE);
    }

    public ExternalDBType getExternalDBType() {
        return externalDBType;
    }

    public void setExternalDBType(final ExternalDBType externalDBType) {
        this.externalDBType = externalDBType;
    }

    public ExternalDB getExternalDB() {
        return externalDB;
    }

    public void setExternalDB(final ExternalDB externalDB) {
        this.externalDB = externalDB;
    }

    public String getExternalDBOrganism() {
        return externalDBOrganism;
    }

    public void setExternalDBOrganism(final String externalDBOrganism) {
        this.externalDBOrganism = externalDBOrganism;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        GeneFile geneFile = (GeneFile) o;

        if (externalDBType != geneFile.externalDBType) {
            return false;
        }
        if (externalDB != geneFile.externalDB) {
            return false;
        }
        return (externalDBOrganism != null) ?
                externalDBOrganism.equals(geneFile.externalDBOrganism) :
                (geneFile.externalDBOrganism == null);
    }

    @Override public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (externalDBType != null ? externalDBType.hashCode() : 0);
        result = 31 * result + (externalDB != null ? externalDB.hashCode() : 0);
        result = 31 * result + (externalDBOrganism != null ? externalDBOrganism.hashCode() : 0);
        return result;
    }

    @Override
    public AclClass getAclClass() {
        return  AclClass.GENE;
    }
}
