/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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
package com.epam.catgenome.manager.externaldb.homologene;

import com.epam.catgenome.dao.homolog.HomologGeneAliasDao;
import com.epam.catgenome.dao.homolog.HomologGeneDescDao;
import com.epam.catgenome.dao.homolog.HomologGeneDomainDao;
import com.epam.catgenome.entity.externaldb.homologene.Alias;
import com.epam.catgenome.entity.externaldb.homologene.Domain;
import com.epam.catgenome.entity.externaldb.homologene.Gene;
import com.epam.catgenome.entity.externaldb.homologene.HomologeneEntry;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIGeneIdsManager;
import com.epam.catgenome.manager.externaldb.taxonomy.TaxonomyManager;
import com.epam.catgenome.manager.externaldb.taxonomy.Taxonomy;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.epam.catgenome.util.NgbFileUtils.getFile;
import static com.epam.catgenome.util.Utils.DEFAULT_PAGE_SIZE;
import static org.apache.commons.lang3.StringUtils.join;

@Service
@Slf4j
public class HomologeneManager {

    private static final String TERM_SPLIT_TOKEN = " ";
    private static final String GENE_FIELDS_LINE_DELIMITER = "|";
    private static final String INCORRECT_XML_FORMAT = "Incorrect XML format";

    @Value("${homologene.index.directory}")
    private String indexDirectory;

    @Autowired
    private TaxonomyManager taxonomyManager;
    @Autowired
    private HomologGeneDomainDao domainDao;
    @Autowired
    private HomologGeneAliasDao aliasDao;
    @Autowired
    private HomologGeneDescDao geneDescDao;
    @Autowired
    private NCBIGeneIdsManager ncbiGeneIdsManager;

    public SearchResult<HomologeneEntry> searchHomologenes(final HomologeneSearchRequest query)
            throws IOException, ParseException {
        final List<HomologeneEntry> entries = new ArrayList<>();
        final SearchResult<HomologeneEntry> searchResult = new SearchResult<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {

            final int page = (query.getPage() == null || query.getPage() <= 0) ? 1 : query.getPage();
            final int pageSize = (query.getPageSize() == null || query.getPage() <= 0) ? DEFAULT_PAGE_SIZE
                    : query.getPageSize();
            final int hits = page * pageSize;

            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(buildSearchQuery(query.getQuery() == null ? "" : query.getQuery()), hits);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            final int from = (page - 1) * pageSize;
            final int to = Math.min(from + pageSize, scoreDocs.length);

            final Set<Long> taxIds = new HashSet<>();
            final Set<String> allGeneIds = new HashSet<>();
            for (int i = from; i < to; i++) {
                Document doc = searcher.doc(scoreDocs[i].doc);
                List<Gene> genes = getGenes(doc);
                List<Long> geneTaxIds = genes.stream().map(Gene::getTaxId).collect(Collectors.toList());
                List<String> geneIds = genes.stream().map(g -> g.getGeneId().toString()).collect(Collectors.toList());
                allGeneIds.addAll(geneIds);
                taxIds.addAll(geneTaxIds);
            }
            final List<Taxonomy> organisms = taxIds.isEmpty() ? Collections.emptyList()
                    : taxonomyManager.searchOrganismsByIds(taxIds);
            final Map<String, String> geneIds = ncbiGeneIdsManager.searchByEntrezIds(new ArrayList<>(allGeneIds));

            for (int i = from; i < to; i++) {
                Document doc = searcher.doc(scoreDocs[i].doc);
                List<Gene> genes = getGenes(doc);
                setGeneSpeciesNames(genes, organisms);
                setEnsemblIds(genes, geneIds);
                entries.add(
                    HomologeneEntry.builder()
                        .groupId(getGroupId(doc))
                        .taxId(getTaxId(doc))
                        .version(getVersion(doc))
                        .caption(getCaption(doc))
                        .genes(genes)
                        .build()
                );
            }
            searchResult.setItems(entries);
            searchResult.setTotalCount(topDocs.totalHits);
        }
        return searchResult;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void importHomologeneDatabase(final String databasePath) throws IOException, ParseException {
        getFile(databasePath);
        List<Gene> genes = new ArrayList<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            for (HomologeneEntry entry: readHomologenes(databasePath)) {
                addDoc(writer, entry);
                genes.addAll(entry.getGenes());
            }
        }
        deleteGenes();
        saveGenes(genes);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveGenes(final List<Gene> genes) {
        List<Alias> aliases = new ArrayList<>();
        List<Domain> domains = new ArrayList<>();
        List<Gene> distinctGenes = genes.stream()
                .collect(Collectors.groupingBy(Gene::getGeneId))
                .values()
                .stream()
                .flatMap(group -> group.stream().limit(1))
                .collect(Collectors.toList());
        for (Gene gene: distinctGenes) {
            gene.getAliases().forEach(a -> aliases.add(Alias.builder()
                    .geneId(gene.getGeneId())
                    .name(a)
                    .build()));
        }
        for (Gene gene: distinctGenes) {
            List<Domain> geneDomains = gene.getDomains();
            geneDomains.forEach(d -> d.setGeneId(gene.getGeneId()));
            domains.addAll(geneDomains);
        }
        geneDescDao.save(distinctGenes);
        aliasDao.save(aliases);
        domainDao.save(domains);
    }

    public void deleteGenes() {
        domainDao.deleteAll();
        aliasDao.deleteAll();
        geneDescDao.deleteAll();
    }

    @SneakyThrows
    public List<HomologeneEntry> readHomologenes(final String path) {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        InputStream inputStream = new FileInputStream(path);
        XMLStreamReader streamReader = xmlInputFactory.createXMLStreamReader(inputStream);
        List<HomologeneEntry> homologeneEntries = new ArrayList<>();
        List<Gene> genes = new ArrayList<>();
        Set<String> aliases = new HashSet<>();
        List<Domain> domains = new ArrayList<>();
        HomologeneEntry homologeneEntry = null;
        Gene gene = null;
        Domain domain = null;
        while (streamReader.hasNext()) {
            streamReader.next();
            if (streamReader.getEventType() == XMLStreamReader.START_ELEMENT) {
                switch (streamReader.getLocalName()) {
                    case "HG-Entry":
                        homologeneEntry = new HomologeneEntry();
                        break;
                    case "HG-Entry_hg-id":
                        requireNonNull(homologeneEntry);
                        homologeneEntry.setGroupId(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Entry_version":
                        requireNonNull(homologeneEntry);
                        homologeneEntry.setVersion(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Entry_caption":
                        requireNonNull(homologeneEntry);
                        homologeneEntry.setCaption(streamReader.getElementText());
                        break;
                    case "HG-Entry_taxid":
                        requireNonNull(homologeneEntry);
                        homologeneEntry.setTaxId(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Gene":
                        gene = new Gene();
                        break;
                    case "HG-Gene_geneid":
                        requireNonNull(gene);
                        gene.setGeneId(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Gene_symbol":
                        requireNonNull(gene);
                        gene.setSymbol(streamReader.getElementText());
                        break;
                    case "HG-Gene_title":
                        requireNonNull(gene);
                        gene.setTitle(streamReader.getElementText());
                        break;
                    case "HG-Gene_taxid":
                        requireNonNull(gene);
                        gene.setTaxId(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Gene_prot-gi":
                        requireNonNull(gene);
                        gene.setProtGi(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Gene_prot-acc":
                        requireNonNull(gene);
                        gene.setProtAcc(streamReader.getElementText());
                        break;
                    case "HG-Gene_prot-len":
                        requireNonNull(gene);
                        gene.setProtLen(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Gene_nuc-gi":
                        requireNonNull(gene);
                        gene.setNucGi(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Gene_nuc-acc":
                        requireNonNull(gene);
                        gene.setNucAcc(streamReader.getElementText());
                        break;
                    case "HG-Gene_locus-tag":
                        requireNonNull(gene);
                        gene.setLocusTag(streamReader.getElementText());
                        break;
                    case "HG-Gene_aliases_E":
                        aliases.add(streamReader.getElementText());
                        break;
                    case "HG-Domain":
                        domain = new Domain();
                        break;
                    case "HG-Domain_begin":
                        requireNonNull(domain);
                        domain.setBegin(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Domain_end":
                        requireNonNull(domain);
                        domain.setEnd(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Domain_pssm-id":
                        requireNonNull(domain);
                        domain.setPssmId(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Domain_cdd-id":
                        requireNonNull(domain);
                        domain.setCddId(streamReader.getElementText());
                        break;
                    case "HG-Domain_cdd-name":
                        requireNonNull(domain);
                        domain.setCddName(streamReader.getElementText());
                        break;
                    default:
                        break;
                }
            } else if (streamReader.getEventType() == XMLStreamReader.END_ELEMENT) {
                switch (streamReader.getLocalName()) {
                    case "HG-Entry":
                        requireNonNull(homologeneEntry);
                        homologeneEntry.setGenes(genes);
                        homologeneEntries.add(homologeneEntry);
                        genes = new ArrayList<>();
                        homologeneEntry = null;
                        break;
                    case "HG-Gene":
                        requireNonNull(gene);
                        gene.setAliases(aliases);
                        gene.setDomains(domains);
                        genes.add(gene);
                        aliases = new HashSet<>();
                        domains = new ArrayList<>();
                        gene = null;
                        break;
                    case "HG-Domain":
                        domains.add(domain);
                        domain = null;
                        break;
                    default:
                        break;
                }
            }
        }
        streamReader.close();
        inputStream.close();
        return homologeneEntries;
    }

    public static void setGeneSpeciesNames(final List<Gene> genes, List<Taxonomy> organisms) {
        for (Gene gene: genes) {
            Taxonomy organism = organisms
                    .stream()
                    .filter(o -> o.getTaxId().equals(gene.getTaxId()))
                    .findFirst()
                    .orElse(null);
            if (organism != null) {
                gene.setSpeciesCommonName(organism.getCommonName());
                gene.setSpeciesScientificName(organism.getScientificName());
            }
        }
    }

    public static void setEnsemblIds(final List<Gene> genes, final Map<String, String> geneIds) {
        for (Gene gene: genes) {
            final String ensembleId = geneIds.get(gene.getGeneId().toString());
            if (ensembleId != null) {
                gene.setEnsemblId(ensembleId);
            }
        }
    }

    private Query buildSearchQuery(final String terms) {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        for (String term: terms.split(TERM_SPLIT_TOKEN)) {
            mainBuilder.add(
                new BooleanQuery.Builder()
                    .add(buildPrefixQuery(term, IndexFields.QUERY_FIELDS), BooleanClause.Occur.SHOULD)
                    .build(),
                BooleanClause.Occur.MUST
            );
        }
        return mainBuilder.build();
    }

    private PrefixQuery buildPrefixQuery(final String term, final IndexFields field) {
        return new PrefixQuery(new Term(field.getFieldName(), term.toLowerCase()));
    }

    private long getGroupId(final Document doc) {
        return Long.parseLong(doc.getField(IndexFields.GROUP_ID.getFieldName()).stringValue());
    }

    private long getVersion(final Document doc) {
        return Long.parseLong(doc.getField(IndexFields.VERSION.getFieldName()).stringValue());
    }

    private String getCaption(final Document doc) {
        return doc.getField(IndexFields.CAPTION.getFieldName()).stringValue();
    }

    private long getTaxId(final Document doc) {
        return Long.parseLong(doc.getField(IndexFields.TAX_ID.getFieldName()).stringValue());
    }

    private List<Gene> getGenes(final Document doc) {
        return doc.getField(IndexFields.GENES.getFieldName()) == null ? null
                : deserializeGenes(doc.getField(IndexFields.GENES.getFieldName()).stringValue());
    }

    @Getter
    private enum IndexFields {
        GROUP_ID("groupId"),
        VERSION("version"),
        CAPTION("caption"),
        TAX_ID("taxId"),
        QUERY_FIELDS("queryFields"),
        GENES("genes");

        private final String fieldName;

        IndexFields(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    private static <T> void requireNonNull(T obj) {
        if (obj == null) {
            throw new IllegalStateException(INCORRECT_XML_FORMAT);
        }
    }

    private static void addDoc(final IndexWriter writer, final HomologeneEntry entry) throws IOException {
        final Document doc = new Document();

        doc.add(new StringField(IndexFields.GROUP_ID.getFieldName(),
                String.valueOf(entry.getGroupId()), Field.Store.YES));

        doc.add(new StringField(IndexFields.VERSION.getFieldName(),
                String.valueOf(entry.getVersion()), Field.Store.YES));

        doc.add(new StringField(IndexFields.CAPTION.getFieldName(),
                String.valueOf(entry.getCaption()), Field.Store.YES));

        doc.add(new StringField(IndexFields.TAX_ID.getFieldName(),
                String.valueOf(entry.getTaxId()), Field.Store.YES));

        if (entry.getGenes() != null) {
            doc.add(new TextField(IndexFields.GENES.getFieldName(),
                    serializeGenes(entry.getGenes()), Field.Store.YES));
        }

        if (entry.getGenes() != null) {
            doc.add(new TextField(IndexFields.QUERY_FIELDS.getFieldName(),
                    serializeQueryFields(entry.getGenes()), Field.Store.YES));
        }
        writer.addDocument(doc);
    }

    @SneakyThrows
    private static List<Gene> deserializeGenes(final String encoded) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(encoded, mapper.getTypeFactory().constructCollectionType(List.class, Gene.class));
    }

    private static String serializeGenes(final List<Gene> genes) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(genes);
    }

    private static String serializeQueryFields(final List<Gene> genes) {
        List<String> geneStrings = new ArrayList<>();
        for (Gene gene: genes) {
            geneStrings.add(gene.getSymbol() + (CollectionUtils.isEmpty(gene.getAliases()) ? ""
                    : GENE_FIELDS_LINE_DELIMITER + join(gene.getAliases(), GENE_FIELDS_LINE_DELIMITER)));
        }
        return join(geneStrings, GENE_FIELDS_LINE_DELIMITER);
    }
}
