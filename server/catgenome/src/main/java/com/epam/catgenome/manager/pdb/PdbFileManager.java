/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
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
package com.epam.catgenome.manager.pdb;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.pdb.PdbFileDao;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.pdb.PdbFile;
import com.epam.catgenome.entity.pdb.PdbFileQueryParams;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.util.db.Condition;
import com.epam.catgenome.util.Utils;
import com.epam.catgenome.util.db.Page;
import com.epam.catgenome.util.db.SortInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.util.NgbFileUtils.getBioDataItemName;
import static com.epam.catgenome.util.NgbFileUtils.getFile;
import static com.epam.catgenome.util.db.DBQueryUtils.getGeneIdsClause;
import static org.apache.commons.lang3.StringUtils.join;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdbFileManager {

    private final PdbFileDao pdbFileDao;
    private final BiologicalDataItemManager biologicalDataItemManager;
    private static final String NAME = "name";
    private static final String PRETTY_NAME = "pretty_name";
    private static final String OWNER = "owner";
    private static final String TITLE = "TITLE";
    private static final String REMARK = "REMARK";
    private static final int LINE_VALUE_START_INDEX = 11;
    private static final int LINE_KEY_END_INDEX = 10;

    @Transactional(propagation = Propagation.REQUIRED)
    public PdbFile create(final PdbFile pdbFile) throws IOException {
        final String path = pdbFile.getPath();
        getFile(path);
        setData(pdbFile);
        biologicalDataItemManager.createBiologicalDataItem(pdbFile);
        pdbFile.setBioDataItemId(pdbFile.getId());
        return pdbFileDao.save(pdbFile);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateMetadata(final long pdbFileId, final Map<String, String> metadata) {
        final PdbFile pdbFile = getPdbFile(pdbFileId);
        pdbFile.setMetadata(metadata);
        pdbFileDao.update(pdbFile);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(final long pdbFileId) {
        final PdbFile pdbFile = getPdbFile(pdbFileId);
        pdbFileDao.delete(pdbFileId);
        biologicalDataItemManager.deleteBiologicalDataItem(pdbFile.getBioDataItemId());
    }

    public PdbFile load(final long pdbFileId) {
        return pdbFileDao.load(pdbFileId);
    }

    public List<PdbFile> load() {
        return pdbFileDao.load();
    }

    public List<PdbFile> load(final List<String> geneIds) {
        final List<SortInfo> sortInfos = Collections.singletonList(SortInfo.builder()
                        .field(NAME)
                        .ascending(true)
                        .build());
        return pdbFileDao.load(getGeneIdsClause(geneIds), sortInfos);
    }

    public Page<PdbFile> load(final PdbFileQueryParams params) {
        final String clause = getFilterClause(params);
        final long totalCount = pdbFileDao.getTotalCount(clause);
        final List<SortInfo> sortInfos = CollectionUtils.isNotEmpty(params.getSortInfos()) ?
                params.getSortInfos() :
                Collections.singletonList(SortInfo.builder()
                        .field(NAME)
                        .ascending(true)
                        .build());
        final List<PdbFile> pdbFiles = pdbFileDao.load(clause, params.getPagingInfo(), sortInfos);
        return Page.<PdbFile>builder()
                .totalCount(totalCount)
                .items(pdbFiles)
                .build();
    }

    public byte[] loadContent(final long pdbFileId) {
        final PdbFile pdbFile = getPdbFile(pdbFileId);
        return readFileContent(pdbFile.getPath());
    }

    private PdbFile getPdbFile(final long pdbFileId) {
        final PdbFile pdbFile = load(pdbFileId);
        Assert.notNull(pdbFile, getMessage(MessagesConstants.ERROR_FILE_NOT_FOUND, pdbFileId));
        return pdbFile;
    }

    private byte[] readFileContent(final String path) {
        if (path != null) {
            final File file = getFile(path);
            try {
                return  FileUtils.readFileToByteArray(file);
            } catch (IOException e) {
                log.debug(e.getMessage(), e);
            }
        }
        return null;
    }

    private void setData(final PdbFile pdbFile) throws IOException {
        final Map<String, String> metadata = parseMetadata(pdbFile.getPath());
        if (MapUtils.isNotEmpty(metadata)) {
            pdbFile.setMetadata(metadata);
        }
        if (StringUtils.isBlank(pdbFile.getPrettyName())) {
            pdbFile.setPrettyName(metadata.get(TITLE));
        }
        pdbFile.setName(getBioDataItemName(pdbFile.getName(), pdbFile.getPath()));
        pdbFile.setType(BiologicalDataItemResourceType.FILE);
        pdbFile.setFormat(BiologicalDataItemFormat.PDB_FILE);
        pdbFile.setCreatedDate(new Date());
        pdbFile.setSource(pdbFile.getPath());
    }

    private Map<String, String> parseMetadata(final String path) throws IOException {
        String line;
        String remarkKey;
        String remarkLine;
        List<String> remarkLines;
        final Map<String, List<String>> remarksMetadata = new HashMap<>();
        final Map<String, String> metadata = new HashMap<>();
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            while ((line = bufferedReader.readLine()) != null) {
                String recordName = line.substring(0, 6).trim();
                if (recordName.equals(TITLE)) {
                    metadata.put(TITLE, line.substring(LINE_VALUE_START_INDEX).trim());
                } else if (recordName.equals(REMARK)) {
                    remarkKey = line.substring(0, LINE_KEY_END_INDEX);
                    remarkLine = line.substring(LINE_VALUE_START_INDEX).trim();
                    if (StringUtils.isNotBlank(remarkLine)) {
                        if (remarksMetadata.containsKey(remarkKey)) {
                            remarksMetadata.get(remarkKey).add(remarkLine);
                        } else {
                            remarkLines = new ArrayList<>();
                            remarkLines.add(remarkLine);
                            remarksMetadata.put(remarkKey, remarkLines);
                        }
                    }
                }
            }
        }
        remarksMetadata.forEach((k, v) -> metadata.put(k, join(v, Utils.NEW_LINE)));
        return metadata;
    }

    private static String getFilterClause(final PdbFileQueryParams params) {
        final List<String> clauses = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(params.getGeneIds())) {
            clauses.add(getGeneIdsClause(params.getGeneIds()));
        }
        if (StringUtils.isNotBlank(params.getOwner())) {
            clauses.add(String.format(Utils.EQUAL_CLAUSE, OWNER, params.getOwner()));
        }
        if (StringUtils.isNotBlank(params.getName())) {
            clauses.add(String.format(Utils.EQUAL_CLAUSE, NAME, params.getName()));
        }
        if (StringUtils.isNotBlank(params.getPrettyName())) {
            clauses.add(String.format(Utils.LIKE_CLAUSE, PRETTY_NAME, params.getPrettyName()));
        }
        return join(clauses, Condition.AND.getValue());
    }
}
