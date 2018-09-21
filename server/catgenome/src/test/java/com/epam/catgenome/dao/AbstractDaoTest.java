/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

import static org.junit.Assert.assertNotNull;

import java.util.Date;

import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import com.epam.catgenome.dao.reference.ReferenceGenomeDao;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.helper.EntityHelper;

/**
 * Source:      AbstractDaoTest
 * Created:     05.12.15, 12:46
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * This is a base class for tests, that depends on reference instance, e.g. VcfFileDaoTest
 *
 * @author Mikhail Miroliubov
 */
public abstract class AbstractDaoTest extends AbstractTransactionalJUnit4SpringContextTests {
    @Autowired
    protected DaoHelper daoHelper;

    @Autowired
    protected ReferenceGenomeDao referenceGenomeDao;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    protected Reference reference;

    private static final Long DEFAULT_USER = 42L;

    @Before
    public void setup() throws Exception {
        assertNotNull("DaoHelper isn't provided.", daoHelper);

        // creates a new reference in the system
        reference = EntityHelper.createReference();
        final Long referenceId = referenceGenomeDao.createReferenceGenomeId();
        assertNotNull("Reference ID cannot be generated.", referenceId);
        reference.setId(referenceId);
        reference.setName(reference.getName() + " " + referenceId);
        BiologicalDataItem index = createReferenceIndex();
        reference.setIndex(index);
        biologicalDataItemDao.createBiologicalDataItem(reference);
        referenceGenomeDao.createReferenceGenome(reference, referenceId);
    }

    @NotNull
    public BiologicalDataItem createReferenceIndex() {
        BiologicalDataItem index = new BiologicalDataItem();
        index.setType(BiologicalDataItemResourceType.FILE);
        index.setFormat(BiologicalDataItemFormat.REFERENCE_INDEX);
        index.setName("");
        index.setPath("");
        index.setCreatedDate(new Date());
        index.setCreatedBy(DEFAULT_USER);
        index.setOwner(EntityHelper.TEST_OWNER);

        biologicalDataItemDao.createBiologicalDataItem(index);
        return index;
    }
}
