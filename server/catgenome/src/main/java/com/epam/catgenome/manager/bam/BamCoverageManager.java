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
package com.epam.catgenome.manager.bam;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.bam.BamCoverageDao;
import com.epam.catgenome.dao.index.field.SortedStringField;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.bam.BamCoverage;
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.entity.bam.CoverageInterval;
import com.epam.catgenome.entity.bam.CoverageQueryParams;
import com.epam.catgenome.entity.Interval;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.util.Utils;
import com.epam.catgenome.util.db.Page;
import com.epam.catgenome.util.db.PagingInfo;
import com.epam.catgenome.util.db.SortInfo;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.util.SamLocusIterator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatDocValuesField;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.util.IndexUtils.addFloatIntervalFilter;
import static com.epam.catgenome.util.IndexUtils.addIntIntervalFilter;
import static com.epam.catgenome.util.IndexUtils.deleteIndexDocument;
import static com.epam.catgenome.util.NgbFileUtils.getFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class BamCoverageManager {

    @Value("${bam.coverage.index.directory}")
    private String bamCoverageIndexDirectory;

    @Value("${coverage.top.hits:10000}")
    private int coverageTopHits;

    @Value("${coverage.batch.size:500}")
    private int batchSize;

    private final BamCoverageDao bamCoverageDao;
    private final BamFileManager bamFileManager;
    private final ReferenceGenomeManager referenceGenomeManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public BamCoverage create(final BamCoverage coverage) throws IOException {
        Assert.notNull(coverage.getStep(), getMessage("Coverage step value should be defined"));
        Assert.isTrue(coverage.getStep() > 0, getMessage("Coverage step value should be > 0"));
        final BamFile bamFile = bamFileManager.load(coverage.getBamId());
        final Map<String, Chromosome> chromosomeMap = referenceGenomeManager.loadChromosomes(bamFile.getReferenceId())
                .stream().collect(Collectors.toMap(BaseEntity::getName, c -> c));
        Assert.notNull(bamFile, getMessage(MessagesConstants.ERROR_BAM_FILE_NOT_FOUND, coverage.getBamId()));
        Assert.isTrue(bamCoverageDao.load(coverage.getBamId(), coverage.getStep()).isEmpty(),
                getMessage(MessagesConstants.ERROR_BAM_COVERAGE_NOT_UNIQUE, coverage.getStep(), coverage.getBamId()));
        final String path = bamFile.getPath();
        final File file = getFile(path);
        coverage.setCoverageId(bamCoverageDao.createId());
        writeCoverageIntervals(coverage, file, chromosomeMap);
        bamCoverageDao.save(coverage);
        return coverage;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(final Long bamId, final Integer step) throws IOException {
        final List<BamCoverage> coverages = bamCoverageDao.load(bamId, step);
        if (!CollectionUtils.isEmpty(coverages)) {
            final Set<Long> coverageIds = coverages.stream()
                    .map(BamCoverage::getCoverageId)
                    .collect(Collectors.toSet());
            bamCoverageDao.delete(coverageIds);
            for (BamCoverage coverage: coverages) {
                deleteIndexDocument(IndexField.COVERAGE_ID.getFieldName(),
                        coverage.getCoverageId(), bamCoverageIndexDirectory);
            }
        }
    }

    public List<BamCoverage> loadAll() {
        return bamCoverageDao.loadAll();
    }

    public List<BamCoverage> loadByBamId(final Set<Long> bamIds) {
        return bamCoverageDao.loadByBamId(bamIds);
    }

    public Page<CoverageInterval> search(final CoverageQueryParams params) throws ParseException, IOException {
        Assert.notNull(params.getCoverageId(), getMessage("Coverage id should be defined"));
        final Query query = buildCoverageQuery(params);
        final PagingInfo pagingInfo = params.getPagingInfo();

        final Page<CoverageInterval> page = new Page<>();
        final List<CoverageInterval> items = new LinkedList<>();
        int totalCount = 0;

        try (Directory index = new SimpleFSDirectory(Paths.get(bamCoverageIndexDirectory));
             IndexReader reader = DirectoryReader.open(index)) {
            final IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs topDocs = searcher.search(query, coverageTopHits);
            totalCount = topDocs.totalHits;

            if (totalCount > 0) {
                final int pageNum = pagingInfo == null ? 1 : pagingInfo.getPageNum() > 0 ? pagingInfo.getPageNum() : 1;
                final int pageSize = pagingInfo == null ? totalCount :
                        pagingInfo.getPageSize() > 0 ? pagingInfo.getPageSize() : totalCount;
                final int numDocs = pagingInfo == null ? totalCount : pageNum * pageSize;

                final Sort sort = getSort(params.getSortInfo());
                topDocs = searcher.search(query, numDocs, sort);

                final int from = (pageNum - 1) * pageSize;
                final int to = Math.min(from + pageSize, totalCount);

                for (int i = from; i < to; i++) {
                    int docId = topDocs.scoreDocs[i].doc;
                    Document doc = searcher.doc(docId);
                    CoverageInterval interval = CoverageInterval.builder()
                            .coverageId(Long.parseLong(doc.getField(IndexField.COVERAGE_ID.getFieldName())
                                    .stringValue()))
                            .chr(doc.getField(IndexField.CHR.getFieldName()).stringValue())
                            .start(Integer.parseInt(doc.getField(IndexField.START.getFieldName()).stringValue()))
                            .end(Integer.parseInt(doc.getField(IndexField.END.getFieldName()).stringValue()))
                            .coverage(Float.parseFloat(doc.getField(IndexField.COVERAGE.getFieldName())
                                    .stringValue()))
                            .build();
                    items.add(interval);
                }
            }
        } catch (IndexNotFoundException e) {
            log.debug(getMessage(MessagesConstants.ERROR_INDEX_DIRECTORY_IS_EMPTY), e);
        }
        page.setTotalCount(totalCount);
        page.setItems(items);
        return page;
    }

    @NotNull
    private Sort getSort(final List<SortInfo> sortInfos) {
        final ArrayList<SortField> sortFields = new ArrayList<>();
        if (sortInfos != null) {
            for (SortInfo sortInfo: sortInfos) {
                final IndexField sortField = IndexField.getByName(sortInfo.getField());
                if (sortField != null) {
                    SortField sf;
                    sf = new SortField(sortInfo.getField(), sortField.getType(), !sortInfo.isAscending());
                    sortFields.add(sf);
                }
            }
        }
        return CollectionUtils.isEmpty(sortFields) ?
                getDefaultSort() :
                new Sort(sortFields.toArray(new SortField[sortFields.size()]));
    }

    private Sort getDefaultSort() {
        return new Sort(new SortField(IndexField.COVERAGE.getFieldName(), SortField.Type.INT, true));
    }

    private void write(final List<CoverageInterval> intervals) throws IOException {
        try (Directory index = new SimpleFSDirectory(Paths.get(bamCoverageIndexDirectory));
             IndexWriter writer = new IndexWriter(index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            for (CoverageInterval interval: intervals) {
                Document doc = new Document();
                doc.add(new StringField(IndexField.COVERAGE_ID.getFieldName(),
                        String.valueOf(interval.getCoverageId()), Field.Store.YES));

                doc.add(new TextField(IndexField.CHR.getFieldName(), interval.getChr(), Field.Store.YES));
                doc.add(new SortedStringField(IndexField.CHR.getFieldName(), interval.getChr(), true));
                doc.add(new StoredField(IndexField.CHR.getFieldName(), interval.getChr()));

                doc.add(new IntPoint(IndexField.START.getFieldName(), interval.getStart()));
                doc.add(new StoredField(IndexField.START.getFieldName(), interval.getStart()));
                doc.add(new NumericDocValuesField(IndexField.START.getFieldName(), interval.getStart()));

                doc.add(new IntPoint(IndexField.END.getFieldName(), interval.getEnd()));
                doc.add(new StoredField(IndexField.END.getFieldName(), interval.getEnd()));
                doc.add(new NumericDocValuesField(IndexField.END.getFieldName(), interval.getEnd()));

                doc.add(new FloatPoint(IndexField.COVERAGE.getFieldName(), interval.getCoverage()));
                doc.add(new StoredField(IndexField.COVERAGE.getFieldName(), interval.getCoverage()));
                doc.add(new FloatDocValuesField(IndexField.COVERAGE.getFieldName(), interval.getCoverage()));

                writer.addDocument(doc);
            }
        }
    }

    private void writeCoverageIntervals(final BamCoverage bamCoverage,
                                        final File file,
                                        final Map<String, Chromosome> chromosomeMap) throws IOException {
        List<CoverageInterval> coverageAreas = new ArrayList<>();
        try (SamReader reader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.LENIENT)
                .referenceSequence(null)
                .enable(SamReaderFactory.Option.INCLUDE_SOURCE_IN_RECORDS).open(file)) {
            final SamLocusIterator samLocusIterator = new SamLocusIterator(reader);
            final Iterator<SamLocusIterator.LocusInfo> iterator = samLocusIterator.iterator();
            int from = 1;
            int to = bamCoverage.getStep();
            float coverage = 0;
            float totalCoverage = 0;
            int intervals = 0;

            while (iterator.hasNext()) {
                SamLocusIterator.LocusInfo locus = iterator.next();
                coverage = coverage + locus.getRecordAndPositions().size();
                if (locus.getPosition() >= to) {
                    final String chrName = Optional.ofNullable(Utils.getFromChromosomeMap(chromosomeMap,
                            locus.getSequenceName()))
                            .map(BaseEntity::getName)
                            .orElse(locus.getSequenceName());
                    CoverageInterval area = CoverageInterval.builder()
                            .coverageId(bamCoverage.getCoverageId())
                            .chr(chrName)
                            .start(from)
                            .end(to)
                            .coverage(coverage / (to - from + 1))
                            .build();
                    coverageAreas.add(area);
                    intervals++;
                    totalCoverage += coverage / (to - from + 1);
                    coverage = 0;
                    if (locus.getPosition() == locus.getSequenceLength()) {
                        from = 1;
                        to = bamCoverage.getStep();
                    } else {
                        from = locus.getPosition() + 1;
                        to = Math.min(locus.getPosition() + bamCoverage.getStep(), locus.getSequenceLength());
                    }
                }
                if (coverageAreas.size() == batchSize) {
                    write(coverageAreas);
                    coverageAreas = new ArrayList<>();
                }
            }
            write(coverageAreas);
            bamCoverage.setCoverage(totalCoverage / intervals);
        }
    }

    private Query buildCoverageQuery(final CoverageQueryParams params) throws ParseException {
        final StandardAnalyzer analyzer = new StandardAnalyzer();
        final BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new QueryParser(IndexField.COVERAGE_ID.getFieldName(), analyzer)
                .parse(String.valueOf(params.getCoverageId())), BooleanClause.Occur.MUST);
        addChromosomesFilter(params.getChromosomes(), analyzer, builder);
        if (params.getStart() != null) {
            final Interval<Integer> startInterval = params.getStart();
            addIntIntervalFilter(IndexField.START.getFieldName(), startInterval, builder);
        }
        if (params.getEnd() != null) {
            final Interval<Integer> endInterval = params.getEnd();
            addIntIntervalFilter(IndexField.END.getFieldName(), endInterval, builder);
        }
        if (params.getCoverage() != null) {
            final Interval<Float> coverageInterval = params.getCoverage();
            addFloatIntervalFilter(IndexField.COVERAGE.getFieldName(), coverageInterval, builder);
        }
        return builder.build();
    }

    public void deleteCoverageDocument(final long coverageId) throws IOException {
        deleteIndexDocument(IndexField.COVERAGE_ID.getFieldName(), coverageId, bamCoverageIndexDirectory);
    }

    private void addChromosomesFilter(final List<String> chromosomes,
                                      final StandardAnalyzer analyzer,
                                      final BooleanQuery.Builder builder) throws ParseException {
        if (!CollectionUtils.isEmpty(chromosomes)) {
            final BooleanQuery.Builder chrQueryBuilder = new BooleanQuery.Builder();
            for (String chr : chromosomes) {
                chrQueryBuilder.add(new QueryParser(IndexField.CHR.getFieldName(), analyzer).parse(chr),
                        BooleanClause.Occur.SHOULD);
            }
            builder.add(chrQueryBuilder.build(), BooleanClause.Occur.MUST);
        }
    }

    @Getter
    private enum IndexField {
        COVERAGE_ID("coverageId", SortField.Type.INT),
        CHR("chr", SortField.Type.STRING),
        START("start", SortField.Type.INT),
        END("end", SortField.Type.INT),
        COVERAGE("coverage", SortField.Type.FLOAT);

        private final String fieldName;
        private final SortField.Type type;

        IndexField(String fieldName, SortField.Type type) {
            this.fieldName = fieldName;
            this.type = type;
        }

        private static final Map<String, IndexField> MAP = new HashMap<>();

        static {
            MAP.put(CHR.getFieldName(), CHR);
            MAP.put(START.getFieldName(), START);
            MAP.put(END.getFieldName(), END);
            MAP.put(COVERAGE.getFieldName(), COVERAGE);
        }

        private static IndexField getByName(String name) {
            return MAP.getOrDefault(name, null);
        }
    }
}
