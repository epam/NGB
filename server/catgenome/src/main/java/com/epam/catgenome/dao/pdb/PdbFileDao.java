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
package com.epam.catgenome.dao.pdb;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.pdb.PdbFile;
import com.epam.catgenome.util.db.PagingInfo;
import com.epam.catgenome.util.db.SortInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.epam.catgenome.util.Utils.addClauseToQuery;
import static com.epam.catgenome.util.Utils.addPagingInfoToQuery;
import static com.epam.catgenome.util.Utils.addSortInfoToQuery;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PdbFileDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String pdbFileSequenceName;
    private String insertPdbFileQuery;
    private String deletePdbFileQuery;
    private String loadPdbFileQuery;
    private String loadPdbFilesQuery;
    private String totalCountQuery;
    private String updatePdbFileMetadataQuery;

    /**
     * Persists new Heatmap record.
     * @param pdbFile {@code Heatmap} a Heatmap to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public PdbFile save(final PdbFile pdbFile) {
        pdbFile.setPdbFileId(daoHelper.createId(pdbFileSequenceName));
        final MapSqlParameterSource params = PdbFileParameters.getParameters(pdbFile);
        getNamedParameterJdbcTemplate().update(insertPdbFileQuery, params);
        return pdbFile;
    }

    /**
     * Deletes PDB File from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void delete(final Long pdbFileId) {
        getJdbcTemplate().update(deletePdbFileQuery, pdbFileId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void update(final PdbFile pdbFile) {
        final MapSqlParameterSource params = PdbFileParameters.getParameters(pdbFile);
        getNamedParameterJdbcTemplate().update(updatePdbFileMetadataQuery, params);
    }

    /**
     * Loads {@code PdbFile} from a database by id.
     * @param pdbFileId {@code long} query parameters
     * @return a {@code PdbFile} from the database
     */
    public PdbFile load(final long pdbFileId) {
        List<PdbFile> pdbFiles = getJdbcTemplate().query(loadPdbFileQuery, PdbFileParameters.getRowMapper(), pdbFileId);
        return pdbFiles.isEmpty() ? null : pdbFiles.get(0);
    }

    /**
     * Loads {@code PdbFile} from a database.
     * @return a {@code List<PdbFile>} from the database
     */
    public List<PdbFile> load() {
        return getJdbcTemplate().query(loadPdbFilesQuery, PdbFileParameters.getRowMapper());
    }

    public List<PdbFile> load(final String clause, final PagingInfo pagingInfo, final List<SortInfo> sortInfos) {
        final String query = addPagingInfoToQuery(addSortInfoToQuery(addClauseToQuery(loadPdbFilesQuery, clause),
                sortInfos), pagingInfo);
        return getJdbcTemplate().query(query, PdbFileParameters.getRowMapper());
    }

    public List<PdbFile> load(final String clause, final List<SortInfo> sortInfos) {
        final String query = addSortInfoToQuery(addClauseToQuery(loadPdbFilesQuery, clause), sortInfos);
        return getJdbcTemplate().query(query, PdbFileParameters.getRowMapper());
    }

    public List<PdbFile> load(final String clause) {
        final String query = addClauseToQuery(loadPdbFilesQuery, clause);
        return getJdbcTemplate().query(query, PdbFileParameters.getRowMapper());
    }

    public long getTotalCount(final String clause) {
        final String query = addClauseToQuery(totalCountQuery, clause);
        return getJdbcTemplate().queryForObject(query, Long.class);
    }

    enum PdbFileParameters {
        BIO_DATA_ITEM_ID,
        NAME,
        PRETTY_NAME,
        TYPE,
        PATH,
        SOURCE,
        FORMAT,
        CREATED_DATE,
        OWNER,

        PDB_FILE_ID,
        GENE_ID,
        METADATA;

        static MapSqlParameterSource getParameters(final PdbFile pdbFile) {
            final MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(PDB_FILE_ID.name(), pdbFile.getPdbFileId());
            params.addValue(BIO_DATA_ITEM_ID.name(), pdbFile.getBioDataItemId());
            params.addValue(GENE_ID.name(), pdbFile.getGeneId());
            params.addValue(METADATA.name(), serialize(pdbFile.getMetadata()));
            return params;
        }


        static RowMapper<PdbFile> getRowMapper() {
            return (rs, rowNum) -> parse(rs);
        }

        static PdbFile parse(final ResultSet rs) throws SQLException {
            final PdbFile pdbFile = PdbFile.builder()
                    .pdbFileId(rs.getLong(PDB_FILE_ID.name()))
                    .geneId(rs.getString(GENE_ID.name()))
                    .metadata(deSerialize(rs.getString(METADATA.name())))
                    .build();
            pdbFile.setBioDataItemId(rs.getLong(BIO_DATA_ITEM_ID.name()));
            pdbFile.setName(rs.getString(NAME.name()));
            pdbFile.setPrettyName(rs.getString(PRETTY_NAME.name()));
            pdbFile.setType(BiologicalDataItemResourceType.getById(rs.getLong(TYPE.name())));
            pdbFile.setOwner(rs.getString(OWNER.name()));
            pdbFile.setPath(rs.getString(PATH.name()));
            pdbFile.setSource(rs.getString(SOURCE.name()));
            pdbFile.setFormat(BiologicalDataItemFormat.getById(rs.getLong(FORMAT.name())));
            pdbFile.setCreatedDate(rs.getDate(CREATED_DATE.name()));
            return pdbFile;
        }

    }

    @SneakyThrows
    private static String serialize(final Map<String, String> data) {
        final ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(data);
    }

    @SneakyThrows
    private static Map<String, String> deSerialize(final String data) {
        final ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(data, HashMap.class);
    }
}
