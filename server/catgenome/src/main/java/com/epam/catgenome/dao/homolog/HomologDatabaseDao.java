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
package com.epam.catgenome.dao.homolog;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.externaldb.homolog.HomologDatabase;
import com.epam.catgenome.util.db.QueryParameters;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.epam.catgenome.util.Utils.addParametersToQuery;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HomologDatabaseDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String sequenceName;
    private String insertQuery;
    private String deleteQuery;
    private String loadQuery;

    /**
     * Persists a new Homolog database records.
     * @param database {@code HomologDatabase} a Homolog database to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public HomologDatabase save(final HomologDatabase database) {
        database.setId(daoHelper.createId(sequenceName));
        getNamedParameterJdbcTemplate().update(insertQuery, DatabaseParameters.getParameters(database));
        return database;
    }

    /**
     * Deletes Homolog database record from the database
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void delete(final Long id) {
        getJdbcTemplate().update(deleteQuery, id);
    }

    /**
     * Loads {@code Homolog database} from a database by parameters.
     * @param queryParameters {@code QueryParameters} query parameters
     * @return a {@code List<HomologDatabase>} from the database
     */
    public List<HomologDatabase> load(final QueryParameters queryParameters) {
        String query = addParametersToQuery(loadQuery, queryParameters);
        return getJdbcTemplate().query(query, DatabaseParameters.getRowMapper());
    }

    enum DatabaseParameters {
        ID,
        NAME,
        PATH;

        static MapSqlParameterSource getParameters(final HomologDatabase database) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(ID.name(), database.getId());
            params.addValue(NAME.name(), database.getName());
            params.addValue(PATH.name(), database.getPath());
            return params;
        }

        static RowMapper<HomologDatabase> getRowMapper() {
            return (rs, rowNum) -> parseAlias(rs);
        }

        static HomologDatabase parseAlias(final ResultSet rs) throws SQLException {
            return HomologDatabase.builder()
                    .id(rs.getLong(ID.name()))
                    .name(rs.getString(NAME.name()))
                    .path(rs.getString(PATH.name()))
                    .build();
        }
    }
}
