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

package com.epam.catgenome.manager.externaldb.ncbi;

import static com.epam.catgenome.component.MessageHelper.getMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.controller.vo.externaldb.NCBIClinVarVO;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.ncbi.util.NCBIDatabase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * <p>
 * A service class to get NCBI data objects from NCBI json
 * </p>
 */
@Service
public class NCBIClinVarManager {

    private static final String CLINVAR_LINK_TEMPLATE = "https://www.ncbi.nlm.nih.gov/clinvar?term=rs%s";

    private JsonMapper mapper = new JsonMapper();

    @Autowired
    private NCBIAuxiliaryManager ncbiAuxiliaryManager;

    /**
     * Method, fetching variation from NCBI's clinVar
     *
     * @param id variation id
     * @return variation
     * @throws ExternalDbUnavailableException
     */
    public NCBIClinVarVO fetchVariationById(String id)
            throws ExternalDbUnavailableException {
        try {
            JsonNode root = ncbiAuxiliaryManager.summaryEntityById(NCBIDatabase.CLINVAR.name(), id);
            NCBIClinVarVO ncbiClinVarVO = mapper.treeToValue(root, NCBIClinVarVO.class);
            if (ncbiClinVarVO.getClinicalSignificance() != null
                    && ncbiClinVarVO.getClinicalSignificance().isPathogenic()) {
                generateClinVarLink(id, ncbiClinVarVO);
            }
            return ncbiClinVarVO;
        } catch (JsonProcessingException e) {
            throw new ExternalDbUnavailableException(getMessage(MessagesConstants
                    .ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
        }

    }

    public void generateClinVarLink(String id, NCBIClinVarVO ncbiClinVarVO) {
        ncbiClinVarVO.setClinvarLink(String.format(CLINVAR_LINK_TEMPLATE, id));
    }

}