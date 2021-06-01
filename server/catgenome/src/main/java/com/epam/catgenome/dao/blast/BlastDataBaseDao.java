package com.epam.catgenome.dao.blast;

import com.epam.catgenome.dao.DaoHelper;
import com.epam.catgenome.entity.blast.BlastDataBase;
import com.epam.catgenome.entity.blast.BlastDataBaseType;
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

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BlastDataBaseDao extends NamedParameterJdbcDaoSupport {

    @Autowired
    private DaoHelper daoHelper;
    private String dataBaseSequenceName;
    private String insertDataBaseQuery;
    private String updateDataBaseQuery;
    private String deleteDataBaseQuery;
    private String loadDataBasesQuery;
    private String loadDataBaseQuery;

    /**
     * Persists a new or updates existing Blast Data Base record.
     * @param dataBase {@code BlastDataBase} a Blast Data Base to persist.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveDataBase(final BlastDataBase dataBase) {
        if (dataBase.getId() == null) {
            dataBase.setId(daoHelper.createId(dataBaseSequenceName));
            getNamedParameterJdbcTemplate().update(insertDataBaseQuery, DataBaseParameters.getParameters(dataBase));
        } else {
            getNamedParameterJdbcTemplate().update(updateDataBaseQuery, DataBaseParameters.getParameters(dataBase));
        }
    }

    /**
     * Deletes a Blast Data Base entity, specified by ID, from the database
     * @param id ID of a record to delete
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteDataBase(final long id) {
       getJdbcTemplate().update(deleteDataBaseQuery, id);
    }

    /**
     * Loads {@code Blast Data Bases} from a database by type.
     * @param type {@code BlastDataBaseType} a type of Data Bases
     * @return a {@code List<BlastDataBase>} from the database
     */
    public List<BlastDataBase> loadDataBases(final BlastDataBaseType type) {
        String query = type == null ? loadDataBasesQuery : loadDataBasesQuery
                + " WHERE type = " + type.getTypeId();
        return getJdbcTemplate().query(query, DataBaseParameters.getRowMapper());
    }

    /**
     * Loads a {@code Blast Data Base} instance from a database by it's ID.
     * @param id {@code long} an ID of a Data Base
     * @return a {@code BlastDataBase} instance from the database
     */
    public BlastDataBase loadDataBase(final long id) {
        List<BlastDataBase> blastDataBases = getJdbcTemplate().query(loadDataBaseQuery, DataBaseParameters.getRowMapper(), id);
        return blastDataBases.isEmpty() ? null : blastDataBases.get(0);
    }

    enum DataBaseParameters {
        DATABASE_ID,
        NAME,
        PATH,
        TYPE;

        static MapSqlParameterSource getParameters(final BlastDataBase dataBase) {
            MapSqlParameterSource params = new MapSqlParameterSource();

            params.addValue(DATABASE_ID.name(), dataBase.getId());
            params.addValue(NAME.name(), dataBase.getName());
            params.addValue(PATH.name(), dataBase.getPath());
            params.addValue(TYPE.name(), dataBase.getType().getTypeId());

            return params;
        }

        static RowMapper<BlastDataBase> getRowMapper() {
            return (rs, rowNum) -> {
                BlastDataBase dataBase = new BlastDataBase();

                dataBase.setId(rs.getLong(DATABASE_ID.name()));
                dataBase.setName(rs.getString(NAME.name()));
                dataBase.setPath(rs.getString(PATH.name()));
                long longVal = rs.getLong(TYPE.name());
                if (!rs.wasNull()) {
                    dataBase.setType(BlastDataBaseType.getTypeById(longVal));
                }
                return dataBase;
            };
        }
    }
}
