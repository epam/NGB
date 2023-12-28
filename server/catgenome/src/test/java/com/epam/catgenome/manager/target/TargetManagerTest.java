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
import com.epam.catgenome.entity.target.TargetGene;
import com.epam.catgenome.entity.target.TargetGenePriority;
import com.epam.catgenome.entity.target.TargetQueryParams;
import com.epam.catgenome.exception.TargetUpdateException;
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

import java.util.Arrays;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
public class TargetManagerTest extends TestCase {

    private static final List<String> PRODUCTS = Arrays.asList("product1", "product2", "product3");
    private static final List<String> DISEASES = Arrays.asList("disease1", "disease2", "disease3");
    private static final long HOMO_SAP_TAX_ID = 9606L;
    private static final long FELIS_CATUS_TAX_ID = 9685L;
    private static final String TARGET = "Target";
    private static final String TARGET_1 = "Target1";
    private static final String TARGET_2 = "Target2";
    @Autowired
    private TargetManager targetManager;

    @Test
    public void createTargetTest(){
        final Target target = createTarget(TARGET);
        final Target createdTarget = targetManager.load(target.getTargetId());
        assertNotNull(createdTarget);
        assertNotNull(createdTarget.getTargetGenes());
    }

    @Test
    public void updateTargetTest() throws TargetUpdateException {
        final Target target = createTarget(TARGET);
        target.setTargetName("New Target");
        final Target updatedTarget = targetManager.update(target);
        assertEquals(updatedTarget.getTargetName(), "New Target");
    }

    @Test
    public void loadTargetsTest(){
        createTarget(TARGET);
        createTarget(TARGET_1);
        createTarget(TARGET_2);
        final TargetQueryParams targetQueryParams = TargetQueryParams.builder()
                .pagingInfo(PagingInfo.builder().pageSize(2).pageNum(1).build())
                .build();
        final Page<Target> targets = targetManager.load(targetQueryParams);
        assertEquals(2, targets.getItems().size());
        assertEquals(3, targets.getTotalCount());
    }

    @Test
    public void filterTargetsByNameTest(){
        createTarget(TARGET);
        createTarget(TARGET_1);
        final TargetQueryParams targetQueryParams = TargetQueryParams.builder()
                .targetName("target1")
                .build();
        final Page<Target> targets = targetManager.load(targetQueryParams);
        assertEquals(1, targets.getItems().size());
        assertEquals(1, targets.getTotalCount());
    }

    @Test
    public void filterTargetsByProductTest(){
        createTarget(TARGET, Arrays.asList("product1", "product2"), DISEASES);
        createTarget(TARGET_1, Arrays.asList("product3", "product4"), DISEASES);
        createTarget(TARGET_2, Arrays.asList("product5", "product6"), DISEASES);
        final TargetQueryParams targetQueryParams = TargetQueryParams.builder()
                .products(Arrays.asList("product1", "product3"))
                .build();
        final Page<Target> targets = targetManager.load(targetQueryParams);
        assertEquals(2, targets.getItems().size());
        assertEquals(2, targets.getTotalCount());
    }

    @Test
    public void filterTargetsByOwnerTest(){
        createTarget(TARGET, PRODUCTS, DISEASES);
        final TargetQueryParams targetQueryParams = TargetQueryParams.builder()
                .owner("owner")
                .build();
        final Page<Target> targets = targetManager.load(targetQueryParams);
        assertEquals(1, targets.getTotalCount());
        final TargetQueryParams targetQueryParams1 = TargetQueryParams.builder()
                .owner("owner1")
                .build();
        final Page<Target> targets1 = targetManager.load(targetQueryParams1);
        assertEquals(0, targets1.getTotalCount());
    }

    @Test
    public void loadFiledValuesTest(){
        createTarget(TARGET);
        final List<String> diseases = targetManager.loadFieldValues(TargetField.DISEASES);
        assertEquals(3, diseases.size());
        final List<String> products = targetManager.loadFieldValues(TargetField.PRODUCTS);
        assertEquals(3, products.size());
        final List<String> speciesNames = targetManager.loadFieldValues(TargetField.SPECIES_NAME);
        assertEquals(2, speciesNames.size());
    }

    @Test
    public void deleteTargetTest() {
        final Target target = createTarget(TARGET);
        final Target createdTarget = targetManager.load(target.getTargetId());
        assertNotNull(createdTarget);
        targetManager.delete(target.getTargetId());
        assertNull(targetManager.load(target.getTargetId()));
    }

    private Target createTarget(final String name, final List<String> products, final List<String> diseases) {
        final TargetGene targetGene = TargetGene.builder()
                .geneId("ENSG00000133703")
                .geneName("KRAS")
                .taxId(HOMO_SAP_TAX_ID)
                .speciesName("Homo Sapiens")
                .priority(TargetGenePriority.LOW)
                .build();
        final TargetGene targetGene1 = TargetGene.builder()
                .geneId("ENSFCAG00000011704")
                .geneName("PGLYRP4")
                .taxId(FELIS_CATUS_TAX_ID)
                .speciesName("Felis catus")
                .priority(TargetGenePriority.HIGH)
                .build();
        final Target target = Target.builder()
                .targetName(name)
                .owner("OWNER")
                .products(products)
                .diseases(diseases)
                .targetGenes(Arrays.asList(targetGene, targetGene1))
                .build();
        return targetManager.create(target);
    }

    private Target createTarget(final String name) {
        return createTarget(name, PRODUCTS, DISEASES);
    }
}
