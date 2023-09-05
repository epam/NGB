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

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.controller.vo.externaldb.ensemblevo.EnsemblEntryVO;
import com.epam.catgenome.controller.vo.externaldb.ensemblevo.EnsemblVariationEntryVO;
import com.epam.catgenome.exception.ExternalDbUnavailableException;

/**
 * <p>
 * Manager for get data from ensembl
 * </p>
 */
@Service
@Slf4j
public class EnsemblDataManager {

    private static final String ENSEMBL_TOOL = "lookup/id";

    private static final String ENSEMBL_VARIATION_TOOL = "variation";
    private static final String ENSEMBL_EXPAND_TOOL = "expand";

    private static final String ENSEMBL_OVERLAP_TOOL = "overlap/region";
    private static final String CONTENT_TYPE = "content-type";
    private static final String APPLICATION_JSON = "application/json";

    private JsonMapper objectMapper = new JsonMapper();

    @Autowired
    private HttpDataManager httpDataManager;

    @Value("${ensembl.base.url:http://rest.ensembl.org/}")
    private String ensemblServer;

    /**
     * Method fetching gene data from Ensemble
     *
     * @param geneId gene id
     * @return Ensemble entry from query
     * @throws ExternalDbUnavailableException
     */
    public EnsemblEntryVO fetchEnsemblEntry(final String geneId) throws ExternalDbUnavailableException {
        final ParameterNameValue[] params = new ParameterNameValue[]{
            new ParameterNameValue(CONTENT_TYPE, APPLICATION_JSON),
            new ParameterNameValue(ENSEMBL_EXPAND_TOOL, "1"),
            new ParameterNameValue("utr", "1")};

        final String location = ensemblServer + ENSEMBL_TOOL + "/" + geneId + "?";
        final String geneData = httpDataManager.fetchData(location, params);
        final EnsemblEntryVO ensemblEntryVO;
        try {
            ensemblEntryVO = objectMapper.readValue(geneData, EnsemblEntryVO.class);
        } catch (IOException e) {
            throw new ExternalDbUnavailableException("Unexpected result format", e);
        }
        return ensemblEntryVO;
    }

    public String fetchNcbiId(final String geneId) {
        final ParameterNameValue[] params = new ParameterNameValue[]{
            new ParameterNameValue(CONTENT_TYPE, APPLICATION_JSON)};

        final String location = ensemblServer + ENSEMBL_TOOL + "/" + geneId + "?";
        try {
            final String geneData = httpDataManager.fetchData(location, params);
            final EnsemblEntryVO ensemblEntryVO;
            ensemblEntryVO = objectMapper.readValue(geneData, EnsemblEntryVO.class);
            String[] desc = ensemblEntryVO.getDescription().split("Acc:");
            if (desc.length > 1) {
                return desc[1].replace("]", "");
            }
        } catch (ExternalDbUnavailableException | JsonProcessingException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * Method fetching variation data from Ensemble
     * <p>
     * http://rest.ensembl.org/variation/human/rs56116432?content-type=application/json
     *
     * @param variationId variation id - example: rs56116432
     * @param species     species id - example: homo_sapiens
     * @return Ensemble variation entry from query
     * @throws ExternalDbUnavailableException
     */
    public EnsemblVariationEntryVO fetchVariationEntry(final String variationId, final String species)
            throws ExternalDbUnavailableException {
        final ParameterNameValue[] params = new ParameterNameValue[]{
            new ParameterNameValue(CONTENT_TYPE, APPLICATION_JSON),
            new ParameterNameValue(ENSEMBL_EXPAND_TOOL, "1")};

        final String location = String.format("%s%s/%s/%s?", ensemblServer, ENSEMBL_VARIATION_TOOL, species,
                variationId);
        final String variationData = httpDataManager.fetchData(location, params);
        try {
            return objectMapper.readValue(variationData, EnsemblVariationEntryVO.class);
        } catch (IOException e) {
            throw new ExternalDbUnavailableException(MessageHelper.getMessage(MessagesConstants.ERROR_NO_DATA_FOR_URL,
                                                                              location), e);
        }
    }

    /**
     * Method fetching variation data from Ensemble
     * <p>
     * http://rest.ensembl.org/overlap/region/human/7:140424943-140624564?
     * feature=gene;
     * feature=transcript;
     * feature=cds;
     * feature=exon;content-type=application/json
     *
     * @param species    species id - example: homo_sapiens
     * @param chromosome chromosome name
     * @param start      coordinate
     * @param finish     coordinate
     * @return List of entry from query
     * @throws ExternalDbUnavailableException
     */
    public List<EnsemblEntryVO> fetchVariationsOnRegion(final String species, final String chromosome,
                                    final String start, final String finish) throws ExternalDbUnavailableException {
        final ParameterNameValue[] params = new ParameterNameValue[]{
            new ParameterNameValue("feature", ENSEMBL_VARIATION_TOOL),
            new ParameterNameValue(CONTENT_TYPE, APPLICATION_JSON)};

        final String location = String.format("%s%s/%s/%s", ensemblServer, ENSEMBL_OVERLAP_TOOL, species,
                chromosome + ":" + start + "-" + finish + "?");
        final String variationData = httpDataManager.fetchData(location, params);
        List<EnsemblEntryVO> variationEntryVOList = Collections.emptyList();

        if (StringUtils.isNotEmpty(variationData)) {
            try {
                variationEntryVOList = objectMapper.readValue(variationData,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, EnsemblEntryVO.class));
            } catch (IOException e) {
                throw new ExternalDbUnavailableException(MessageHelper.getMessage(MessagesConstants
                        .ERROR_NO_DATA_FOR_URL, location), e);
            }
        }
        return variationEntryVOList;
    }
}
