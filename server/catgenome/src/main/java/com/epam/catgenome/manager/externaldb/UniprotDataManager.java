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

import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.Uniprot;

/**
 * <p>
 * Manager for fetching data from Uniprot external DB
 * </p>
 */
@Service
public class UniprotDataManager {

    private static final String UNIPROT_SERVER = "http://www.uniprot.org/";

    private static final String UNIPROT_TOOL = "uniprot";

    private Uniprot fetchedUniprotData = null;

    @Autowired
    private HttpDataManager httpDataManager;

    /**
     * Method fetching data from UniProt
     *
     * @param geneId gene id in any database
     * @throws ExternalDbUnavailableException
     */
    public Uniprot fetchUniprotEntry(String geneId) throws ExternalDbUnavailableException {

        ParameterNameValue[] params = new ParameterNameValue[]{new ParameterNameValue("query", geneId),
            new ParameterNameValue("format", "xml")};

        String location = UNIPROT_SERVER + UNIPROT_TOOL + "/?";

        String uniprotData = httpDataManager.fetchData(location, params);

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance("com.epam.catgenome.manager.externaldb.bindings.uniprot");
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            StringReader reader = new StringReader(uniprotData);
            Object uniprotObject = unmarshaller.unmarshal(reader);

            if (uniprotObject instanceof Uniprot) {
                fetchedUniprotData = (Uniprot) uniprotObject;
            }

            return fetchedUniprotData;
        } catch (JAXBException e) {
            throw new ExternalDbUnavailableException("Unexpected result format", e);
        }
    }
}
