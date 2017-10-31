/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

package com.epam.catgenome.dao.reference;

import com.epam.catgenome.entity.reference.Species;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * {@code SpeciesDao} is a DAO component, that handles database interaction with Species data.
 */
public class SpeciesDao extends NamedParameterJdbcDaoSupport {

    private String saveSpeciesQuery;
    private String loadSpeciesByVersionQuery;
    private String loadAllSpeciesQuery;

    /**
     * Saves or updates a {@code Species} instance in the database
     * @param species to save or update
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Species saveSpecies(Species species) {
        Assert.isTrue(StringUtils.isNotBlank(species.getName()));
        Assert.isTrue(StringUtils.isNotBlank(species.getVersion()));
        getNamedParameterJdbcTemplate().update(saveSpeciesQuery, SpeciesParameters.getParameters(species));
        return species;
    }

    /**
     * Loads a {@code Species} instance from the database specified by it's version
     * @param version of a species
     * @return a loaded {@code Species} instance or
     *          {@code null} if user with a given version
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public Species loadSpeciesByVersion(String version) {
        Assert.isTrue(StringUtils.isNotBlank(version));
        List<Species> list = getNamedParameterJdbcTemplate().query(loadSpeciesByVersionQuery,
            new MapSqlParameterSource(SpeciesParameters.SPECIES_VERSION.name(), version),
            SpeciesParameters.getRowMapper()
        );
        return CollectionUtils.isNotEmpty(list) ? list.get(0) : null;
    }

    /**
     * Loads all persisted {@code Species} entities from the database
     * @return all {@code Species} instances, saved in the database
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Species> loadAllSpecies() {
        return getNamedParameterJdbcTemplate().query(loadAllSpeciesQuery, SpeciesParameters.getRowMapper());
    }

    @Required
    public void setLoadSpeciesByVersionQuery(String loadSpeciesByVersionQuery) {
        this.loadSpeciesByVersionQuery = loadSpeciesByVersionQuery;
    }

    @Required
    public void setSaveSpeciesQuery(String saveSpeciesQuery) {
        this.saveSpeciesQuery = saveSpeciesQuery;
    }

    @Required
    public void setLoadAllSpeciesQuery(String loadAllSpeciesQuery) {
        this.loadAllSpeciesQuery = loadAllSpeciesQuery;
    }

    enum SpeciesParameters {
        SPECIES_NAME,
        SPECIES_VERSION;

        static MapSqlParameterSource getParameters(Species species) {
            MapSqlParameterSource params = new MapSqlParameterSource();

            params.addValue(SPECIES_NAME.name(), species.getName());
            params.addValue(SPECIES_VERSION.name(), species.getVersion());

            return params;
        }

        static RowMapper<Species> getRowMapper() {
            return (rs, rowNum) -> {
                Species species = new Species();

                species.setName(rs.getString(SPECIES_NAME.name()));
                species.setVersion(rs.getString(SPECIES_VERSION.name()));

                return species;
            };
        }
    }
}
