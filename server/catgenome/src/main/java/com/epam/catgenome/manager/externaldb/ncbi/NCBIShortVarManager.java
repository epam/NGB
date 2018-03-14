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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.externaldb.NCBIClinVarVO;
import com.epam.catgenome.controller.vo.externaldb.NCBIShortVarVO;
import com.epam.catgenome.controller.vo.externaldb.NCBITaxonomyVO;
import com.epam.catgenome.controller.vo.externaldb.NCBIVariationVO;
import com.epam.catgenome.entity.vcf.GenotypeData;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.bindings.dbsnp.Assembly;
import com.epam.catgenome.manager.externaldb.bindings.dbsnp.Component;
import com.epam.catgenome.manager.externaldb.bindings.dbsnp.ExchangeSet;
import com.epam.catgenome.manager.externaldb.bindings.dbsnp.MapLoc;
import com.epam.catgenome.manager.externaldb.bindings.dbsnp.Rs;
import com.epam.catgenome.manager.externaldb.ncbi.parser.NCBIGeneInfoParser;
import com.epam.catgenome.manager.externaldb.ncbi.util.NCBIDatabase;
import com.epam.catgenome.manager.externaldb.ncbi.util.NCBIUtility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * <p>
 * Manager for get data from NCBI BD
 * </p>
 */
@Service
public class NCBIShortVarManager {

    private static final int MAX_RESULT = 20;

    private NCBIGeneInfoParser ncbiGeneInfoParser = new NCBIGeneInfoParser();

    @Autowired
    private NCBIClinVarManager ncbiClinVarManager;

    @Autowired
    private NCBIAuxiliaryManager ncbiAuxiliaryManager;

    /**
     * Retrieves variation info from multiple NCBI databases
     *
     * @param id rsId of variation
     * @return variation info
     * @throws IOException
     * @throws InterruptedException
     * @throws ExternalDbUnavailableException
     */
    public NCBIVariationVO fetchAggregatedVariationById(String id) throws ExternalDbUnavailableException {

        NCBIVariationVO result = new NCBIVariationVO();
        NCBIShortVarVO snp = this.fetchVariationById(id);

        String contigLabel = snp.getContigLabel();

        if (StringUtils.isNotBlank(contigLabel)) {
            String annotationId = ncbiAuxiliaryManager.searchDbForId("annotinfo", contigLabel);
            if (StringUtils.isNotBlank(annotationId)) {
                JsonNode jsonNode = ncbiAuxiliaryManager.fetchAnnotationInfoById(annotationId);
                String annotreleaseid = jsonNode.get("annotreleaseid").toString();
                snp.setAssemblyNumber(annotreleaseid);
            }
        }

        result.setNcbiShortVar(snp);

        if (StringUtils.isNotBlank(snp.getClinicalSignificance())) {
            NCBIClinVarVO ncbiClinVarVO = ncbiClinVarManager.fetchVariationById(id);
            if (result.isPathogenic()) {
                ncbiClinVarManager.generateClinVarLink(id, ncbiClinVarVO);
            }
            result.setNcbiClinVar(ncbiClinVarVO);
        }

        String taxId = snp.getTaxId();
        NCBITaxonomyVO ncbiTaxonomyVO = ncbiAuxiliaryManager.fetchTaxonomyInfoById(taxId);
        result.setNcbiTaxonomy(ncbiTaxonomyVO);

        return result;
    }

    /**
     * Retrieves variation info from NCBI's dbSNP
     *
     * @param id rsId of variation
     * @return variation info
     * @throws ExternalDbUnavailableException
     */
    public NCBIShortVarVO fetchVariationById(String id) throws ExternalDbUnavailableException {

        JsonNode dbSnpSummary = ncbiAuxiliaryManager.summaryEntityById(NCBIDatabase.SNP.name(), id);
        NCBIShortVarVO shortVarVO;
        try {
            shortVarVO = ncbiAuxiliaryManager.getMapper().treeToValue(dbSnpSummary, NCBIShortVarVO.class);
        } catch (JsonProcessingException e) {
            throw new ExternalDbUnavailableException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
        }

        String fasta = ncbiAuxiliaryManager.fetchTextById(NCBIDatabase.SNP.name(), id, "fasta");
        shortVarVO.setFasta(fasta);

        String dbSnpFetchXml = ncbiAuxiliaryManager.fetchXmlById(NCBIDatabase.SNP.name(), id, null);
        ncbiGeneInfoParser.parseSnpInfo(dbSnpFetchXml, shortVarVO);

        return shortVarVO;
    }

    /**
     * Retrieves variations from snp database in given region
     *
     * @param organism   -- organism name
     * @param start      -- start position
     * @param finish     -- end position
     * @param chromosome -- chromosome number
     * @return list of found variations
     * @throws ExternalDbUnavailableException
     */
    public List<Variation> fetchVariationsOnRegion(
            String organism, String start, String finish, String chromosome)
            throws ExternalDbUnavailableException {

        String term = String.format("%s:%s[Base Position] AND \"%s\"[CHR] AND \"%s\"[ORGN]",
                start, finish, chromosome, organism);
        String searchResultXml = ncbiAuxiliaryManager.searchWithHistory(NCBIDatabase.SNP.name(), term, MAX_RESULT);

        Pair<String, String> stringStringPair =
                ncbiGeneInfoParser.parseHistoryResponse(searchResultXml, NCBIUtility.NCBI_SEARCH);

        String queryKey = stringStringPair.getLeft();
        String webEnv = stringStringPair.getRight();

        String dataXml = ncbiAuxiliaryManager.fetchWithHistory(queryKey, webEnv, NCBIDatabase.SNP);

        ExchangeSet snpResultJaxb = null;

        try {

            JAXBContext jaxbContext = JAXBContext.newInstance("com.epam.catgenome.manager.externaldb.bindings.dbsnp");
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            StringReader reader = new StringReader(dataXml);
            Object uniprotObject = unmarshaller.unmarshal(reader);

            if (uniprotObject instanceof ExchangeSet) {
                snpResultJaxb = (ExchangeSet) uniprotObject;
            }

        } catch (JAXBException e) {
            throw new ExternalDbUnavailableException("Error parsing ncbi snp response", e);
        }
        return getResultList(snpResultJaxb);
    }

    /**
     * Converts NCBI variation type to NGGB variation type
     *
     * @param ncbiVariationType string representation of NCBI variation
     * @return
     */

    public static VariationType convertVariationType(String ncbiVariationType) {

        VariationType variationType = null;

        if ("snp".equalsIgnoreCase(ncbiVariationType)) {
            variationType = VariationType.SNV;
        } else if ("multinucleotide-polymorphism".equalsIgnoreCase(ncbiVariationType)) {
            variationType = VariationType.MNP;
        } else if ("mixed".equalsIgnoreCase(ncbiVariationType)) {
            variationType = VariationType.MIXED;
        } else if ("in-del".equalsIgnoreCase(ncbiVariationType)) {
            variationType = VariationType.INS;
        }

        return variationType;
    }

    public void setNcbiClinVarManager(NCBIClinVarManager ncbiClinVarManager) {
        this.ncbiClinVarManager = ncbiClinVarManager;
    }


    private List<Variation> getResultList(final ExchangeSet snpResultJaxb) {

        List<Variation> resultList;

        if (snpResultJaxb != null && snpResultJaxb.getRs() != null && !snpResultJaxb.getRs().isEmpty()) {
            resultList = new ArrayList<>(snpResultJaxb.getRs().size());

            fillVariationList(resultList, snpResultJaxb);
            return resultList;
        }

        return Collections.<Variation>emptyList();

    }

    private void fillVariationList(final List<Variation> resultList, final ExchangeSet snpResultJaxb) {
        for (Rs rs : snpResultJaxb.getRs()) {
            Variation nggbVariation = new Variation();
            GenotypeData genotypeData = new GenotypeData();

            nggbVariation.setIdentifier("rs" + rs.getRsId());

            String snpType = rs.getSnpType();

            VariationType variationType = convertVariationType(snpType);

            if (com.epam.catgenome.entity.vcf.OrganismType.HETEROZYGOUS.name().equalsIgnoreCase(snpType)) {
                genotypeData.setOrganismType(com.epam.catgenome.entity.vcf.OrganismType.HETEROZYGOUS);
            } else if ("no-variation".equalsIgnoreCase(snpType)) {
                genotypeData.setOrganismType(com.epam.catgenome.entity.vcf.OrganismType.NO_VARIATION);
            } else {
                genotypeData.setOrganismType(com.epam.catgenome.entity.vcf.OrganismType.HOMOZYGOUS);
            }

            nggbVariation.setType(variationType);

            Rs.Sequence sequence = rs.getSequence();
            if (sequence != null) {
                String genotypeString = sequence.getObserved();
                if (StringUtils.isNotBlank(genotypeString)) {
                    String nggbGenotypeString = genotypeString.replace('/', '|');
                    genotypeData.setGenotypeString(nggbGenotypeString);
                }
            }
            correctNggbVariation(rs.getAssembly().get(0), nggbVariation);
            nggbVariation.setGenotypeData(genotypeData);
            resultList.add(nggbVariation);
        }
    }

    private void correctNggbVariation(final Assembly assembly, final Variation nggbVariation) {

        if (assembly != null) {
            List<Component> component = assembly.getComponent();

            if (!component.isEmpty()) {
                Component component1 = component.get(0);
                List<MapLoc> mapLocList = component1.getMapLoc();

                if (!mapLocList.isEmpty()) {
                    MapLoc mapLoc = mapLocList.get(0);
                    String refAllele = mapLoc.getRefAllele();
                    nggbVariation.setReferenceAllele(refAllele);
                    nggbVariation.setStartIndex(mapLoc.getPhysMapInt());
                }
            }
        }
    }
}
