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
package com.epam.catgenome.manager.target;

import com.epam.catgenome.entity.target.Target;
import com.epam.catgenome.entity.target.TargetIdentification;
import com.epam.catgenome.entity.target.IdentificationQueryParams;
import com.epam.catgenome.util.db.Page;
import com.epam.catgenome.util.db.PagingInfo;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
public class TargetIdentificationManagerTest extends TestCase {

    private static final String IDENTIFICATION = "Identification";
    private static final String IDENTIFICATION1 = "Identification1";
    private static final String IDENTIFICATION2 = "Identification2";
    @Autowired
    private TargetManager targetManager;
    @Autowired
    private TargetIdentificationManager manager;

    @Test
    public void createIdentificationTest(){
        final TargetIdentification identification = createIdentification(IDENTIFICATION);
        final TargetIdentification identification1 = manager.load(identification.getId());
        assertNotNull(identification1);
    }

    @Test
    public void updateIdentificationTest(){
        final TargetIdentification identification = createIdentification(IDENTIFICATION);
        identification.setName("New Identification");
        identification.setOwner("Owner");
        final TargetIdentification identification1 = manager.update(identification);
        assertEquals(identification1.getName(), "New Identification");
        assertEquals(identification1.getOwner(), "Owner");
    }

    @Test
    public void loadIdentificationsTest(){
        createIdentification(IDENTIFICATION);
        createIdentification(IDENTIFICATION1);
        createIdentification(IDENTIFICATION2);
        final IdentificationQueryParams params = IdentificationQueryParams.builder()
                .pagingInfo(PagingInfo.builder().pageSize(2).pageNum(1).build())
                .name(IDENTIFICATION)
                .build();
        final Page<TargetIdentification> identifications = manager.load(params);
        assertEquals(2, identifications.getItems().size());
        assertEquals(3, identifications.getTotalCount());
    }

    @Test
    public void loadAllIdentificationsTest(){
        createIdentification(IDENTIFICATION);
        createIdentification(IDENTIFICATION1);
        createIdentification(IDENTIFICATION2);
        final List<TargetIdentification> identifications = manager.load();
        assertEquals(3, identifications.size());
    }

    @Test
    public void deleteIdentificationTest() {
        final TargetIdentification identification = createIdentification(IDENTIFICATION);
        final TargetIdentification identification1 = manager.load(identification.getId());
        assertNotNull(identification1);
        manager.delete(identification.getId());
        assertNull(manager.load(identification.getId()));
    }

    private TargetIdentification createIdentification(final String name) {
        final Target target = Target.builder()
                .targetName("Target")
                .products(Collections.emptyList())
                .diseases(Collections.emptyList())
                .targetGenes(Collections.emptyList())
                .build();
        final Target target1 = targetManager.create(target);
        final TargetIdentification identification = TargetIdentification.builder()
                .targetId(target1.getTargetId())
                .genesOfInterest(Collections.singletonList("ENSG00000133703"))
                .translationalGenes(Collections.singletonList("ENSFCAG00000011704"))
                .build();
        identification.setName(name);
        return manager.create(identification);
    }
}
