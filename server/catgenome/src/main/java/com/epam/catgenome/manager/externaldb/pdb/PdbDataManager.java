/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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

package com.epam.catgenome.manager.externaldb.pdb;

import com.epam.catgenome.client.rscb.RCSBApi;
import com.epam.catgenome.client.rscb.RCSBApiBuilder;
import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.exception.ResponseException;
import com.epam.catgenome.manager.externaldb.bindings.ecsbpdbmap.Alignment;
import com.epam.catgenome.manager.externaldb.bindings.ecsbpdbmap.Dasalignment;
import com.epam.catgenome.manager.externaldb.bindings.ecsbpdbmap.PdbBlock;
import com.epam.catgenome.manager.externaldb.bindings.ecsbpdbmap.Segment;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.Dataset;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.Record;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto.DasalignmentDTO;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto.DatasetDTO;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto.Entry;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto.PolymerEntity;
import com.epam.catgenome.util.QueryUtils;
import org.apache.catalina.util.URLEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * <p>
 * A service class, that manager connections to external database PDB
 * </p>
 */
@Service
public class PdbDataManager {
    private static final String RCSB_SERVER = "https://data.rcsb.org/";
    private static final String LOCATION = RCSB_SERVER + "graphql";
    private static final String RCSB_ENTRY_QUERY = "{entry(entry_id:\"%s\"){struct{title}struct_keywords" +
            "{pdbx_keywords}polymer_entities{uniprots{rcsb_uniprot_entry_name}rcsb_polymer_entity{pdbx_description}" +
            "rcsb_polymer_entity_container_identifiers{auth_asym_ids}}}}";
    private static final String PDB_MAP_ENTRY = "{entry(entry_id:\"%s\"){polymer_entities{polymer_entity_instances" +
            "{rcsb_polymer_instance_feature{feature_positions{beg_seq_id end_seq_id}}rcsb_id}}}}";

    private final RCSBApi rcsbApi;

    public PdbDataManager() {
        this.rcsbApi = new RCSBApiBuilder(0, 0, RCSB_SERVER).buildClient();
    }

    /**
     * @param pdbIds pdb id
     * @return dataset from query
     * @throws ExternalDbUnavailableException
     */
    public Dataset fetchRCSBEntry(final String pdbIds) throws ExternalDbUnavailableException {
        try {
            final String query = replaceHttpSymbols(RCSB_ENTRY_QUERY, pdbIds);
            return parseToDataset(QueryUtils.execute(rcsbApi.getDataset(query)), pdbIds);
        } catch (ResponseException e) {
            throw new ExternalDbUnavailableException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_UNEXPECTED_FORMAT, LOCATION), e);
        }
    }

    /**
     * @param pdbIds PDB id
     * @return data from query
     * @throws ExternalDbUnavailableException
     */
    public Dasalignment fetchPdbMapEntry(final String pdbIds) throws ExternalDbUnavailableException {
        try {
            final String query = replaceHttpSymbols(PDB_MAP_ENTRY, pdbIds);
            return parseToDasalignment(QueryUtils.execute(rcsbApi.getDasalignment(query)));

        } catch (ResponseException e) {
            throw new ExternalDbUnavailableException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_UNEXPECTED_FORMAT, LOCATION), e);
        }
    }

    private String replaceHttpSymbols(final String query, final String pdbIds) {
        return URLEncoder.DEFAULT.encode(String.format(query, pdbIds), StandardCharsets.UTF_8.toString());
    }

    private Dasalignment parseToDasalignment(final DasalignmentDTO das) {
        final List<PolymerEntity> polymerEntities = das.getData().getEntry().getEntities();

        final List<Alignment> alignments = polymerEntities.stream()
                .flatMap(entity -> entity.getEntityInstances().stream())
                .map(instance -> {
                    final List<PdbBlock> pdbBlocks = instance.getInstanceFeatures().stream()
                            .map(feature -> {
                                final List<Segment> segments = feature.getPositions().stream()
                                        .map(position -> parseSegment(
                                                position.getStart(),
                                                position.getEnd(),
                                                instance.getId()))
                                        .collect(toList());

                                return parsePdbBlock(segments);
                            }).collect(toList());

                    return parseAlignment(pdbBlocks);
                }).collect(toList());

        return parseDasalignment(alignments);
    }

    private Dataset parseToDataset(final DatasetDTO datasetDTO, final String structureId) {
        final Entry entry = datasetDTO.getData().getEntry();
        final String title = entry.getStruct().getTitle();
        final String classification = entry.getKeywords().getValue();

        final List<Record> records = entry.getEntities().stream().flatMap(entity -> {
            final String compound = entity.getRcsbPoly().getDescription();
            final String uniName = validateUniprotName(entity);

            return entity.getIdentifiers().getIds().stream()
                    .map(chainId -> parseRecord(title, classification, compound, uniName, structureId, chainId))
                    .collect(toList())
                    .stream();

        }).collect(toList());

        return parseDataset(records);
    }

    private String validateUniprotName(final PolymerEntity entity) {
        final List<PolymerEntity.Uniprot> uniprots = entity.getUniprots();
        return (uniprots == null || uniprots.isEmpty()) ? "" : uniprots.get(0).getNames().get(0);
    }

    private Record parseRecord(final String title, final String classification, final String compound,
                               final String uniprotName, final String structureId, final String chainId) {
        final Record record = new Record();
        record.setStructureTitle(title);
        record.setClassification(classification);
        record.setCompound(compound);
        record.setUniprotRecommendedName(uniprotName);
        record.setStructureId(structureId);
        record.setChainId(chainId);
        return record;
    }

    private Dataset parseDataset(final List<Record> records) {
        final Dataset dataset = new Dataset();
        dataset.setRecord(records);
        return dataset;
    }

    private Segment parseSegment(final Integer start, final Integer end, final String id) {
        final Segment segment = new Segment();
        segment.setStart(start);
        segment.setIntObjectId(id);

        //start and end coordinates are the same, if no end.
        if (end == null) {
            segment.setEnd(start);
        } else {
            segment.setEnd(end);
        }
        return segment;
    }

    private PdbBlock parsePdbBlock(final List<Segment> segments) {
        final PdbBlock block = new PdbBlock();
        block.setSegment(segments);
        return block;
    }

    private Alignment parseAlignment(final List<PdbBlock> blocks) {
        final Alignment alignment = new Alignment();
        alignment.setBlock(blocks);
        return alignment;
    }

    private Dasalignment parseDasalignment(final List<Alignment> alignments) {
        final Dasalignment dasalignment = new Dasalignment();
        dasalignment.setAlignment(alignments);
        return dasalignment;
    }
}
