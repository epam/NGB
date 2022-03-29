/*
 * MIT License
 *
 * Copyright (c) 2022 EPAM Systems
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
package com.epam.catgenome.manager.pathway;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.registration.BioPAXRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.PathwayRegistrationRequest;
import com.epam.catgenome.dao.pathway.PathwayDao;
import com.epam.catgenome.dao.pathway.PathwayOrganismDao;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.pathway.NGBPathway;
import com.epam.catgenome.entity.pathway.PathwayDatabaseSource;
import com.epam.catgenome.entity.pathway.PathwayQueryParams;
import com.epam.catgenome.entity.pathway.PathwayOrganism;
import com.epam.catgenome.exception.FileParsingException;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.manager.externaldb.taxonomy.Taxonomy;
import com.epam.catgenome.manager.externaldb.taxonomy.TaxonomyManager;
import com.epam.catgenome.util.Utils;
import com.epam.catgenome.util.db.Page;
import com.epam.catgenome.util.db.SortInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.util.TextUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.biopax.paxtools.controller.Cloner;
import org.biopax.paxtools.controller.Completer;
import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.io.sbgn.L3ToSBGNPDConverter;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Pathway;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sbgn.bindings.Glyph;
import org.sbgn.bindings.Map;
import org.sbgn.bindings.Sbgn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.util.IndexUtils.deserialize;
import static com.epam.catgenome.util.IndexUtils.serialize;
import static com.epam.catgenome.util.NgbFileUtils.getBioDataItemName;
import static com.epam.catgenome.util.NgbFileUtils.getFile;
import static org.apache.commons.lang3.StringUtils.join;

@Service
@RequiredArgsConstructor
@Slf4j
public class PathwayManager {

    private static final String PATHWAY = "Pathway";
    public static final String BIO_PAX = "BioPAX";

    private final PathwayDao pathwayDao;
    private final PathwayOrganismDao pathwayOrganismDao;
    private final BiologicalDataItemManager biologicalDataItemManager;
    private final TaxonomyManager taxonomyManager;

    @Value("${biopax.directory}")
    private String bioPAXDirectory;

    @Value("${pathway.index.directory}")
    private String pathwayIndexDirectory;

    @Value("${pathway.top.hits:10000}")
    private int pathwayTopHits;

    @Transactional(propagation = Propagation.REQUIRED)
    public NGBPathway registerPathway(final PathwayRegistrationRequest request) throws IOException {
        final String extension = Utils.getFileExtension(request.getPath());
        final PathwayDatabaseSource databaseSource = PathwayDatabaseSource.getByExtension(extension);
        Assert.isTrue(PathwayDatabaseSource.PATHWAY_EXTENSIONS.contains(extension),
                getMessage(MessagesConstants.ERROR_UNSUPPORTED_FILE_EXTENSION, PATHWAY,
                join(PathwayDatabaseSource.PATHWAY_EXTENSIONS, " or ")));
        return createPathway(request, databaseSource, request.getPath());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void registerBioPAX(final BioPAXRegistrationRequest request) throws IOException {
        final String extension = Utils.getFileExtension(request.getPath());
        Assert.isTrue(PathwayDatabaseSource.BIO_PAX_EXTENSIONS.contains(extension),
                getMessage(MessagesConstants.ERROR_UNSUPPORTED_FILE_EXTENSION, BIO_PAX,
                        join(PathwayDatabaseSource.BIO_PAX_EXTENSIONS, " or ")));
        createBioPAX(request);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public NGBPathway createPathway(final PathwayRegistrationRequest request,
                                    final PathwayDatabaseSource databaseSource,
                                    final String source) throws IOException {
        final String path = request.getPath();
        final File pathwayFile = getFile(path);
        final NGBPathway pathway = getPathway(request, source);
        final long pathwayId = pathwayDao.createPathwayId();
        pathway.setPathwayId(pathwayId);
        pathway.setDatabaseSource(databaseSource);
        final List<PathwayOrganism> species = buildSpecies(pathwayId, request);
        writeLucenePathwayIndex(pathway, species, readPathway(pathwayFile, databaseSource));
        try {
            biologicalDataItemManager.createBiologicalDataItem(pathway);
            pathway.setBioDataItemId(pathway.getId());
            pathwayDao.savePathway(pathway);
            final List<PathwayOrganism> organisms = pathwayOrganismDao.savePathwayOrganisms(species,
                    pathwayId);
            pathway.setOrganisms(organisms);
        } finally {
            if (pathway.getBioDataItemId() == null) {
                deleteDocument(pathway.getPathwayId());
            }
        }
        return pathway;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deletePathway(final long pathwayId) throws IOException {
        final NGBPathway pathway = getPathway(pathwayId);
        pathwayOrganismDao.deletePathwayOrganisms(pathwayId);
        pathwayDao.deletePathway(pathwayId);
        biologicalDataItemManager.deleteBiologicalDataItem(pathway.getBioDataItemId());
        deleteDocument(pathwayId);
    }

    public NGBPathway loadPathway(final long pathwayId) {
        return pathwayDao.loadPathway(pathwayId);
    }

    public byte[] loadPathwayContent(final Long pathwayId) throws IOException {
        final NGBPathway pathway = getPathway(pathwayId);
        final String path = pathway.getPath();
        final File pathwayFile = getFile(path);
        return FileUtils.readFileToByteArray(pathwayFile);
    }

    public Page<NGBPathway> loadPathways(final PathwayQueryParams params) throws IOException, ParseException {
        final Page<NGBPathway> page = new Page<>();
        final List<NGBPathway> items = new ArrayList<>();
        int totalCount = 0;
        try (Directory index = new SimpleFSDirectory(Paths.get(pathwayIndexDirectory));
             IndexReader reader = DirectoryReader.open(index)) {
            final IndexSearcher searcher = new IndexSearcher(reader);
            final Query query = buildPathwaySearchQuery(params);
            TopDocs topDocs = searcher.search(query, pathwayTopHits);
            totalCount = topDocs.totalHits;

            if (totalCount > 0) {
                final Sort sort = getSortBySortInfo(params.getSortInfo());

                final int pageNum = params.getPagingInfo() == null ? 1 :
                        params.getPagingInfo().getPageNum() > 0 ? params.getPagingInfo().getPageNum() : 1;
                final int pageSize = params.getPagingInfo() == null ? totalCount :
                        params.getPagingInfo().getPageSize() > 0 ? params.getPagingInfo().getPageSize() : totalCount;
                final int numDocs = params.getPagingInfo() == null ? totalCount : pageNum * pageSize;

                topDocs = searcher.search(query, numDocs, sort);

                final int from = (pageNum - 1) * pageSize;
                final int to = Math.min(from + pageSize, totalCount);

                for (int i = from; i < to; i++) {
                    int docId = topDocs.scoreDocs[i].doc;
                    Document doc = searcher.doc(docId);
                    NGBPathway pathway = NGBPathway.builder().pathwayId(getId(doc)).build();
                    pathway.setName(getDocValue(doc, PathwayIndexFields.NAME));
                    pathway.setPrettyName(getDocValue(doc, PathwayIndexFields.PRETTY_NAME));
                    pathway.setPathwayDesc(getDocValue(doc, PathwayIndexFields.DESCRIPTION));
                    pathway.setDatabaseSource(getDatabaseSource(doc));
                    pathway.setOrganisms(getOrganisms(doc));
                    items.add(pathway);
                }
            }
        } catch (IndexNotFoundException e) {
            log.debug(getMessage(MessagesConstants.ERROR_INDEX_DIRECTORY_IS_EMPTY), e);
        }
        page.setTotalCount(totalCount);
        page.setItems(items);
        return page;
    }

    public List<NGBPathway> loadPathways() {
        return pathwayDao.loadAllPathways(null);
    }

    private void createBioPAX(final BioPAXRegistrationRequest request) throws IOException {
        final String directory = getBioPAXDirectory(request.getPath());
        BioPAXIOHandler reader = new SimpleIOHandler();
        final Model model = reader.convertFromOWL(new FileInputStream(request.getPath()));
        final L3ToSBGNPDConverter converter = new L3ToSBGNPDConverter(null, null, true);
        for (final Pathway pathway : model.getObjects(Pathway.class)) {
            final String id = pathway.getUri().split(PATHWAY)[1];
            final Path sbgnPath = Paths.get(directory, id + PathwayDatabaseSource.CUSTOM.getExtension());
            final String sbgnFile = sbgnPath.toString();
            Set<BioPAXElement> eles = new HashSet<>(Collections.singleton(pathway));
            EditorMap editorMap = (new SimpleIOHandler(BioPAXLevel.L3)).getEditorMap();
            eles = (new Completer(editorMap)).complete(eles);
            Model pathwayModel = (new Cloner(editorMap, BioPAXLevel.L3.getDefaultFactory())).clone(eles);
            converter.writeSBGN(pathwayModel, sbgnFile);
            final PathwayRegistrationRequest pathwayRegistrationRequest = PathwayRegistrationRequest.builder()
                    .name(id)
                    .path(sbgnFile)
                    .prettyName(pathway.getStandardName())
                    .pathwayDesc(request.getPathwayDesc())
                    .taxIds(request.getTaxIds())
                    .build();
            createPathway(pathwayRegistrationRequest, PathwayDatabaseSource.BIOCYC, request.getPath());
        }
    }

    @NotNull
    private String getBioPAXDirectory(final String path) {
        final File owlFile = getFile(path);
        final Path dirPath = Paths.get(bioPAXDirectory, FilenameUtils.removeExtension(owlFile.getName()));
        final String directory = dirPath.toString();
        final File dirFile = new File(directory);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        return directory;
    }

    private Sort getSortBySortInfo(final SortInfo sortInfo) {
        return sortInfo == null ? new Sort(new SortField("name", SortField.Type.STRING, false)) :
                new Sort(new SortField(sortInfo.getField(), SortField.Type.STRING, !sortInfo.isAscending()));
    }

    private void writeLucenePathwayIndex(final NGBPathway pathway,
                                         final List<PathwayOrganism> species,
                                         final String content) throws IOException {
        try (Directory index = new SimpleFSDirectory(Paths.get(pathwayIndexDirectory));
             IndexWriter writer = new IndexWriter(index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            addDoc(writer, pathway, species, content);
        }
    }

    private void deleteDocument(final long pathwayId) throws IOException {
        try (Directory index = new SimpleFSDirectory(Paths.get(pathwayIndexDirectory));
             IndexWriter writer = new IndexWriter(index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            final Term term = new Term(PathwayIndexFields.PATHWAY_ID.getFieldName(), String.valueOf(pathwayId));
            writer.deleteDocuments(term);
        }
    }

    private String readPathway(final File pathwayFile, final PathwayDatabaseSource databaseSource)
            throws IOException {
        return databaseSource.equals(PathwayDatabaseSource.COLLAGE) ?
                readCollage(pathwayFile) :
                readSBGN(pathwayFile);
    }

    @NotNull
    private String readSBGN(final File pathwayFile) {
        final List<String> entries = new ArrayList<>();
        try {
            final Sbgn sbgn = readFromFile(pathwayFile);
            final Map map = sbgn.getMap();
            for (Glyph glyph : map.getGlyph()) {
                if (glyph.getLabel() != null) {
                    entries.add(glyph.getLabel().getText());
                }
                glyph.getGlyph().forEach(g -> {
                    if (g.getLabel() != null) {
                        entries.add(g.getLabel().getText());
                    }
                });
            }
        } catch (JAXBException e) {
            throw new FileParsingException(getMessage(MessagesConstants.ERROR_FILE_PARSING,
                    PathwayDatabaseSource.CUSTOM.getExtension()), e);
        }
        return join(entries, ",");
    }

    @NotNull
    private String readCollage(final File pathwayFile) throws IOException {
        final String data = FileUtils.readFileToString(pathwayFile, "UTF-8");
        final List<String> entries = new ArrayList<>();
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            final JsonNode jsonNodes;
            jsonNodes = objectMapper.readTree(data);
            for (JsonNode node : jsonNodes.at("/elements/nodes")) {
                entries.add(node.at("/data/label").asText());
            }
        } catch (JsonProcessingException e) {
            throw new FileParsingException(getMessage(MessagesConstants.ERROR_FILE_PARSING,
                    PathwayDatabaseSource.COLLAGE.getExtension()), e);
        }
        return join(entries, ",");
    }

    private static Sbgn readFromFile(final File f) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance("org.sbgn.bindings");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return (Sbgn)unmarshaller.unmarshal(f);
    }

    private static void addDoc(final IndexWriter writer,
                               final NGBPathway pathway,
                               final List<PathwayOrganism> species,
                               final String content) throws IOException {
        final Document doc = new Document();
        doc.add(new StringField(PathwayIndexFields.PATHWAY_ID.getFieldName(),
                String.valueOf(pathway.getPathwayId()), Field.Store.YES));

        doc.add(new StringField(PathwayIndexFields.DATABASE_SOURCE.getFieldName(),
                String.valueOf(pathway.getDatabaseSource().getSourceId()), Field.Store.YES));

        doc.add(new SortedDocValuesField(PathwayIndexFields.NAME.getFieldName(), new BytesRef(pathway.getName())));
        doc.add(new StoredField(PathwayIndexFields.NAME.getFieldName(), pathway.getName()));
        doc.add(new TextField(PathwayIndexFields.NAME.getFieldName(),
                pathway.getName(), Field.Store.YES));

        if (!TextUtils.isBlank(pathway.getPrettyName())) {
            doc.add(new TextField(PathwayIndexFields.PRETTY_NAME.getFieldName(),
                    pathway.getPrettyName(), Field.Store.YES));
        }

        if (!TextUtils.isBlank(pathway.getPathwayDesc())) {
            doc.add(new TextField(PathwayIndexFields.DESCRIPTION.getFieldName(),
                    pathway.getPathwayDesc(), Field.Store.YES));
        }

        if (!CollectionUtils.isEmpty(species)) {
            doc.add(new TextField(PathwayIndexFields.SPECIES.getFieldName(),
                    serialize(species.stream().map(s -> String.valueOf(s.getTaxId()))
                            .collect(Collectors.toList())), Field.Store.YES));
            doc.add(new TextField(PathwayIndexFields.SPECIES_NAME.getFieldName(),
                    serialize(species.stream().map(PathwayOrganism::getSpeciesName)
                            .collect(Collectors.toList())), Field.Store.YES));
        }

        doc.add(new TextField(PathwayIndexFields.CONTENT.getFieldName(), content, Field.Store.YES));
        writer.addDocument(doc);
    }

    @Getter
    private enum PathwayIndexFields {
        PATHWAY_ID("pathwayId"),
        NAME("name"),
        PRETTY_NAME("prettyName"),
        DESCRIPTION("description"),
        DATABASE_SOURCE("databaseSource"),
        SPECIES("species"),
        SPECIES_NAME("species_name"),
        CONTENT("content");

        private final String fieldName;

        PathwayIndexFields(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    private String getDocValue(final Document doc, final PathwayIndexFields field) {
        return doc.getField(field.getFieldName()) == null ? null : doc.getField(field.getFieldName()).stringValue();
    }

    private long getId(final Document doc) {
        return Long.parseLong(doc.getField(PathwayIndexFields.PATHWAY_ID.getFieldName()).stringValue());
    }

    private PathwayDatabaseSource getDatabaseSource(final Document doc) {
        return Optional.ofNullable(doc.getField(PathwayIndexFields.DATABASE_SOURCE.getFieldName()))
                .map(field -> {
                    final String value = field.stringValue();
                    if (NumberUtils.isNumber(value)) {
                        return PathwayDatabaseSource.getById(Long.parseLong(value));
                    }
                    return PathwayDatabaseSource.CUSTOM;
                }).orElse(PathwayDatabaseSource.CUSTOM);
    }

    @Nullable
    private List<PathwayOrganism> getOrganisms(final Document doc) {
        if (doc.getField(PathwayIndexFields.SPECIES.getFieldName()) == null) {
            return null;
        }
        final List<String> taxIds = deserialize(doc.getField(
                PathwayIndexFields.SPECIES.getFieldName()).stringValue());
        final List<String> speciesNames = deserialize(doc.getField(
                PathwayIndexFields.SPECIES_NAME.getFieldName()).stringValue());
        final List<PathwayOrganism> organisms = new ArrayList<>();
        for (int i = 0; i < taxIds.size(); i++) {
            final String taxId = taxIds.get(i);
            if (NumberUtils.isDigits(taxId)) {
                final String speciesName = speciesNames.size() == taxIds.size() ? speciesNames.get(i) : null;
                final PathwayOrganism build = PathwayOrganism.builder()
                        .taxId(Long.parseLong(taxId))
                        .speciesName(speciesName)
                        .build();
                organisms.add(build);
            }
        }
        return organisms;
    }

    private Query buildPathwaySearchQuery(final PathwayQueryParams params) throws ParseException {
        final String term = params.getTerm();
        final List<Long> taxIds = params.getTaxIds();
        final PathwayDatabaseSource source = params.getDatabaseSource();
        if (TextUtils.isBlank(term) && CollectionUtils.isEmpty(taxIds) && source == null) {
            return new MatchAllDocsQuery();
        }

        final StandardAnalyzer analyzer = new StandardAnalyzer();
        final BooleanQuery.Builder builder = new BooleanQuery.Builder();

        if (!TextUtils.isBlank(term)) {
            final BooleanQuery.Builder termBuilder = new BooleanQuery.Builder();

            termBuilder.add(buildQuery(PathwayIndexFields.NAME.getFieldName(), term, analyzer),
                    BooleanClause.Occur.SHOULD);
            termBuilder.add(buildQuery(PathwayIndexFields.PRETTY_NAME.getFieldName(), term, analyzer),
                    BooleanClause.Occur.SHOULD);
            termBuilder.add(buildQuery(PathwayIndexFields.DESCRIPTION.getFieldName(), term, analyzer),
                    BooleanClause.Occur.SHOULD);
            termBuilder.add(buildQuery(PathwayIndexFields.CONTENT.getFieldName(), term, analyzer),
                    BooleanClause.Occur.SHOULD);
            builder.add(termBuilder.build(), BooleanClause.Occur.MUST);
        }

        if (!CollectionUtils.isEmpty(taxIds)) {
            final BooleanQuery.Builder speciesQuery = new BooleanQuery.Builder();
            taxIds.forEach(taxId -> speciesQuery.add(buildQuery(PathwayIndexFields.SPECIES.getFieldName(),
                            String.valueOf(taxId), analyzer),
                    BooleanClause.Occur.SHOULD));
            builder.add(speciesQuery.build(), BooleanClause.Occur.MUST);
        }

        if (source != null) {
            builder.add(buildQuery(PathwayIndexFields.DATABASE_SOURCE.getFieldName(),
                            String.valueOf(source.getSourceId()), analyzer), BooleanClause.Occur.MUST);
        }
        return builder.build();
    }

    @SneakyThrows(ParseException.class)
    private Query buildQuery(final String fieldName, final String fieldValue, final StandardAnalyzer analyzer) {
        return new QueryParser(fieldName, analyzer).parse(fieldValue);
    }

    @NotNull
    private NGBPathway getPathway(final PathwayRegistrationRequest request, final String source) {
        final NGBPathway pathway = NGBPathway.builder()
                .pathwayDesc(request.getPathwayDesc())
                .build();
        pathway.setPath(request.getPath());
        pathway.setName(getBioDataItemName(request.getName(), request.getPath()));
        pathway.setPrettyName(request.getPrettyName());
        pathway.setType(BiologicalDataItemResourceType.FILE);
        pathway.setFormat(BiologicalDataItemFormat.PATHWAY);
        pathway.setCreatedDate(new Date());
        pathway.setSource(source);
        return pathway;
    }

    private NGBPathway getPathway(final long pathwayId) {
        final NGBPathway pathway = pathwayDao.loadPathway(pathwayId);
        Assert.notNull(pathway, getMessage(MessagesConstants.ERROR_PATHWAY_NOT_FOUND, pathwayId));
        return pathway;
    }

    private List<PathwayOrganism> buildSpecies(long pathwayId, final PathwayRegistrationRequest request) {
        if (CollectionUtils.isEmpty(request.getTaxIds())) {
            return Collections.emptyList();
        }
        final List<Taxonomy> species = taxonomyManager.searchOrganismsByIds(request.getTaxIds());
        return request.getTaxIds()
                .stream()
                .map(taxId ->
                    PathwayOrganism.builder()
                            .pathwayId(pathwayId)
                            .taxId(taxId)
                            .speciesName(findName(taxId, species))
                            .build()
                )
                .collect(Collectors.toList());
    }

    private String findName(final Long taxId, final List<Taxonomy> species) {
        return species.stream()
                .filter(s -> taxId.equals(s.getTaxId()))
                .findFirst()
                .map(s -> Optional.ofNullable(s.getCommonName()).orElse(s.getScientificName()))
                .orElse(null);
    }
}
