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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.epam.catgenome.controller.vo.externaldb.ensemblevo.EnsemblEntryVO;
import com.epam.catgenome.controller.vo.externaldb.ensemblevo.EnsemblExonVO;
import com.epam.catgenome.controller.vo.externaldb.ensemblevo.EnsemblTranscriptVO;
import com.epam.catgenome.entity.gene.BaseGeneEntity;
import com.epam.catgenome.entity.gene.PBDGaneEntity;
import com.epam.catgenome.entity.gene.Transcript;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.DbReferenceType;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.FeatureType;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.PropertyType;
import com.epam.catgenome.manager.externaldb.bindings.uniprot.Uniprot;

/**
 * <p>
 * Utils for get external bd
 * </p>
 */
public final class ExtenalDBUtils {

    private static final String SWISS_PROT = "Swiss-Prot";
    private static final String DOMAIN = "domain";
    private static final String PDB = "PDB";
    private static final long AMINO_ACID_BASE = 3;
    private static final String CHAINS = "chains";
    private static final String HELIX = "helix";
    private static final String STRAND = "strand";
    private static final String TURN = "turn";

    private ExtenalDBUtils() {
        //no-op
    }

    /**
     * @param entryVO data from ensembl
     * @return list with transcript
     */
    public static List<Transcript> ensemblEntryVO2Transcript(final EnsemblEntryVO entryVO) {
        final List<Transcript> transcriptList = new ArrayList<>();
        for (EnsemblTranscriptVO ensemblTranscriptVO : entryVO.getTranscript()) {
            final Transcript transcript = new Transcript();
            ensemblExonVO2Transcript(ensemblTranscriptVO, transcript, entryVO.getStart());
            transcript.setName(ensemblTranscriptVO.getDisplayName());
            transcript.setExon(ensemblExonVO2BaseGaneEntityList(ensemblTranscriptVO.getExon(), entryVO.getStart()));
            transcript.setUtr(ensemblUtrVO2BaseGeneEntityList(ensemblTranscriptVO.getUtr(), entryVO.getStart()));
            transcriptList.add(transcript);
        }

        return transcriptList;
    }


    /**
     * Method for fill transcript
     *
     * @param uniprot class with data from uniprot
     * @param transcript class for fill
     */
    public static void fillDomain(final Uniprot uniprot, final Transcript transcript) {
        final List<BaseGeneEntity> domainList = new ArrayList<>();
        uniprot.getEntry().stream().filter(entry -> entry.getDataset().equals(SWISS_PROT))
                .forEach(entry -> domainList.addAll(entry.getFeature().stream()
                        .filter(featureType -> featureType.getType()
                                .equals(DOMAIN))
                        .map(ExtenalDBUtils::featureType2BaseGaneEntity).collect(Collectors.toList())));
        transcript.setDomain(domainList);
    }

    /**
     *
     * fill pdp
     *
     * @param uniprot class with data from uniprot
     * @param transcript class for fill
     */
    public static void fillPBP(final Uniprot uniprot, final Transcript transcript) {
        final List<PBDGaneEntity> pdbList = new ArrayList<>();
        uniprot.getEntry().stream().filter(entry -> entry.getDataset().equals(SWISS_PROT))
                .forEach(entry -> pdbList.addAll(entry.getDbReference().stream()
                        .filter(featureType -> featureType.getType()
                                .equals(PDB))
                        .map(ExtenalDBUtils::dbReferenceType2BaseGaneEntity).collect(Collectors.toList())));
        transcript.setPdb(pdbList);
    }

    /**
     *
     * @param uniprot class with data from uniprot
     * @param transcript class for fill
     */
    public static void fillSecondaryStructure(final Uniprot uniprot, final Transcript transcript) {
        final List<BaseGeneEntity> secondaryStructure = new ArrayList<>();
        uniprot.getEntry().stream().filter(entry -> entry.getDataset().equals(SWISS_PROT))
                .forEach(entry -> secondaryStructure.addAll(entry.getFeature().stream()
                        .filter(featureType -> featureType.getType()
                                .equals(HELIX) || featureType.getType()
                                .equals(STRAND) || featureType.getType()
                                .equals(TURN))
                        .map(ExtenalDBUtils::secondaryStructure2BaseGaneEntity).collect(Collectors.toList())));
        transcript.setSecondaryStructure(secondaryStructure);
    }




    private static List<BaseGeneEntity> ensemblExonVO2BaseGaneEntityList(final EnsemblExonVO[] exonVO,
                                                                         final long geneStart) {
        final List<BaseGeneEntity> baseList = new ArrayList<>();
        for (EnsemblExonVO ensemblExonVO : exonVO) {
            final BaseGeneEntity baseGaneEntity = new BaseGeneEntity();
            ensemblExonVO2BaseGaneEntity(ensemblExonVO, baseGaneEntity, geneStart);
            baseList.add(baseGaneEntity);
        }
        return baseList;
    }

    private static void ensemblExonVO2Transcript(final EnsemblTranscriptVO exonVO, final Transcript transcript,
                                                 final long geneStart){
        ensemblExonVO2BaseGaneEntity(exonVO, transcript, geneStart);
        transcript.setBioType(exonVO.getBioType());
    }

    private static void ensemblExonVO2BaseGaneEntity(final EnsemblExonVO exonVO, final BaseGeneEntity baseGaneEntity,
                                                     final long geneStart) {
        baseGaneEntity.setName(exonVO.getAssemblyName());
        baseGaneEntity.setId(exonVO.getId());
        baseGaneEntity.setStart(exonVO.getStart() - geneStart);
        baseGaneEntity.setEnd(exonVO.getEnd() - geneStart);
    }


    private static List<BaseGeneEntity> ensemblUtrVO2BaseGeneEntityList(final EnsemblExonVO[] utrVO,
                                                                        final long geneStart) {
        final List<BaseGeneEntity> baseList = new ArrayList<>();
        for (EnsemblExonVO ensemblExonVO : utrVO) {
            final BaseGeneEntity baseGaneEntity = new BaseGeneEntity();
            ensemblUtrVO2BaseGaneEntity(ensemblExonVO, baseGaneEntity, geneStart);
            baseList.add(baseGaneEntity);
        }
        return baseList;
    }

    private static void ensemblUtrVO2BaseGaneEntity(final EnsemblExonVO utrVO, final BaseGeneEntity baseGaneEntity,
                                                    final long geneStart) {
        baseGaneEntity.setType(utrVO.getObjectType());
        baseGaneEntity.setStrand(utrVO.getStrand());
        baseGaneEntity.setStart(utrVO.getStart() - geneStart);
        baseGaneEntity.setEnd(utrVO.getEnd() - geneStart);
    }

    private static BaseGeneEntity featureType2BaseGaneEntity(final FeatureType featureType) {
        final BaseGeneEntity entity = new BaseGeneEntity();
        entity.setName(featureType.getDescription());
        entity.setId(featureType.getId());
        entity.setStart(featureType.getLocation().getBegin().getPosition().longValue() * AMINO_ACID_BASE);
        entity.setEnd(featureType.getLocation().getEnd().getPosition().longValue() * AMINO_ACID_BASE);
        return entity;
    }

    private static BaseGeneEntity secondaryStructure2BaseGaneEntity(final FeatureType featureType) {
        final BaseGeneEntity entity = featureType2BaseGaneEntity(featureType);
        entity.setType(featureType.getType());
        return entity;
    }

    private static PBDGaneEntity dbReferenceType2BaseGaneEntity(final DbReferenceType dbReferenceType) {
        final PBDGaneEntity entity = new PBDGaneEntity();
        entity.setType(dbReferenceType.getType());
        entity.setId(dbReferenceType.getId());
        parseStartEnd(entity, dbReferenceType.getProperty());
        return entity;
    }

    private static void parseStartEnd(final PBDGaneEntity entity, final List<PropertyType> property) {
        for (PropertyType propertyType : property) {
            if (propertyType.getType().equals(CHAINS)) {
                entity.setPosition(propertyType.getValue());
            }
        }
    }


}
