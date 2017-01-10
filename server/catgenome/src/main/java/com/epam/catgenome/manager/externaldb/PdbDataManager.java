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

package com.epam.catgenome.manager.externaldb;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.bindings.ecsbpdbmap.Dasalignment;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.Dataset;

/**
 * <p>
 * A service class, that manager connections to external database PDB
 * </p>
 */
@Service
public class PdbDataManager {

    private static final String RCSB_SERVER = "http://www.rcsb.org/";
    private static final String RCSB_TOOL = "pdb/rest/customReport";
    private static final String RCSB_PDP_MAP_TOOL = "pdb/rest/das/pdb_uniprot_mapping/alignment";

    @Autowired
    private HttpDataManager httpDataManager;

    /**
     *
     * @param pdbids pdb id
     * @return dataset from query
     * @throws ExternalDbUnavailableException
     */
    public Dataset fetchRCSBEntry(final String pdbids) throws ExternalDbUnavailableException {

        ParameterNameValue[] params = new ParameterNameValue[]{new ParameterNameValue("pdbids", pdbids),
            new ParameterNameValue("format", "xml"),
            new ParameterNameValue("customReportColumns", "structureTitle,expressionHost,classification,compound")};

        String location = RCSB_SERVER + RCSB_TOOL + "?";

        String rcsbpbdData = httpDataManager.fetchData(location, params);

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance("com.epam.catgenome.manager.externaldb.bindings.rcsbpbd");
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            StringReader reader = new StringReader(rcsbpbdData);
            Object uniprotObject = unmarshaller.unmarshal(reader);

            Dataset fetchedDatasetData = null;
            if (uniprotObject instanceof Dataset) {
                fetchedDatasetData = (Dataset) uniprotObject;
            }

            return fetchedDatasetData;
        } catch (JAXBException e) {
            throw new ExternalDbUnavailableException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_UNEXPECTED_FORMAT, location), e);
        }
    }


    /**
     *
     * @param pdbIds PDB id
     * @return data from query
     * @throws ExternalDbUnavailableException
     */
    public Dasalignment fetchPdbMapEntry(final String pdbIds) throws ExternalDbUnavailableException {

        ParameterNameValue[] params = new ParameterNameValue[]{new ParameterNameValue("query", pdbIds)};

        String location = RCSB_SERVER + RCSB_PDP_MAP_TOOL + "?";

        String rcsbpbdData = httpDataManager.fetchData(location, params);

        try {
            JAXBContext jaxbContext = JAXBContext
                    .newInstance("com.epam.catgenome.manager.externaldb.bindings.ecsbpdbmap");
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            StringReader reader = new StringReader(rcsbpbdData);
            Object pdbObject = unmarshaller.unmarshal(reader);

            Dasalignment fetchedDasalignmentData = null;
            if (pdbObject instanceof Dasalignment) {
                fetchedDasalignmentData = (Dasalignment) pdbObject;
            }

            return fetchedDasalignmentData;
        } catch (JAXBException e) {
            throw new ExternalDbUnavailableException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_UNEXPECTED_FORMAT, location), e);
        }
    }
}
