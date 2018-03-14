/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

package com.epam.catgenome.manager.bam;

import com.epam.catgenome.entity.bam.PSLRecord;
import com.epam.catgenome.entity.reference.Species;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.HttpDataManager;
import com.epam.catgenome.manager.externaldb.ParameterNameValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class BlatSearchManager {

    @Value("#{catgenome['blat.search.url']}")
    private String blatURL;
    @Value("#{catgenome['blat.search.type']}")
    private String searchType;
    @Value("#{catgenome['blat.search.sort.order']}")
    private String sortOrder;
    @Value("#{catgenome['blat.search.output.type']}")
    private String outputType;

    @Autowired
    private PSLRecordParser pslRecordParser;

    @Autowired
    private HttpDataManager httpDataManager;

    public List<PSLRecord> find(String readSequence, Species species)
            throws ExternalDbUnavailableException, IOException {
        String response = httpDataManager.fetchData(blatURL + "?",
                createURLParameters(readSequence, species));
        return pslRecordParser.parse(response);
    }

    private ParameterNameValue[] createURLParameters(String sequense, Species species) {
        return new ParameterNameValue[]{
            new ParameterNameValue("org", species.getName()),
            new ParameterNameValue("db", species.getVersion()),
            new ParameterNameValue("type", searchType),
            new ParameterNameValue("sort", sortOrder),
            new ParameterNameValue("output", outputType),
            new ParameterNameValue("userSeq", sequense)
        };
    }
}
