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

package com.epam.catgenome.dao;

import com.epam.catgenome.dao.reference.SpeciesDao;
import com.epam.catgenome.entity.reference.Species;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class SpeciesDaoTest {

    @Autowired
    private SpeciesDao speciesDao;

    private static final String TEST_NAME = "TEST_NAME";
    private static final String TEST_VERSION = "TEST_VERSION";

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveAndLoad() throws Exception {
        Species testSpecies = new Species();
        testSpecies.setName(TEST_NAME);
        testSpecies.setVersion(TEST_VERSION);

        speciesDao.saveSpecies(testSpecies);

        Species species = speciesDao.loadSpeciesByVersion(TEST_VERSION);
        Assert.assertNotNull(species);
        Assert.assertEquals(species.getName(), TEST_NAME);
        Assert.assertEquals(species.getVersion(), TEST_VERSION);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadAllSpecies() throws Exception {
        for (int i = 0; i < 3; i++) {
            Species species = new Species();
            species.setName(TEST_NAME);
            species.setVersion(TEST_VERSION + i);
            speciesDao.saveSpecies(species);
        }
        List<Species> speciesList = speciesDao.loadAllSpecies();
        Assert.assertEquals(speciesList.size(), 3);
    }

}