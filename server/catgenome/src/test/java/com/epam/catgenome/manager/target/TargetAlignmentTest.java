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

import com.epam.catgenome.entity.target.AlignmentStatus;
import com.epam.catgenome.entity.target.Target;
import com.epam.catgenome.entity.target.TargetGene;
import com.epam.catgenome.entity.target.TargetGenePriority;
import junit.framework.TestCase;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Arrays;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
public class TargetAlignmentTest extends TestCase {
    private static final long HOMO_SAP_TAX_ID = 9606L;
    private static final long SUS_SCROFA_TAX_ID = 9823L;
    private static final long FELIS_CATUS_TAX_ID = 9685L;

    @Autowired
    private AlignmentManager alignmentManager;
    @Autowired
    private TargetManager targetManager;


    @Test
    @Ignore
    public void generateAlignmentTest() throws IOException {
        final Target target = createTarget();
        alignmentManager.generateAlignment();
        final Target alignedTarget = targetManager.getTarget(target.getTargetId());
        assertNotNull(alignedTarget);
        assertEquals(AlignmentStatus.ALIGNED, alignedTarget.getAlignmentStatus());
    }

    private Target createTarget() throws IOException {
        final TargetGene targetGene = TargetGene.builder()
                .geneId("ENSG00000133703")
                .geneName("KRAS")
                .taxId(HOMO_SAP_TAX_ID)
                .speciesName("Homo Sapiens")
                .priority(TargetGenePriority.LOW)
                .build();
        final TargetGene targetGene1 = TargetGene.builder()
                .geneId("ENSFCTG00005013354")
                .geneName("KRAS")
                .taxId(FELIS_CATUS_TAX_ID)
                .speciesName("Felis catus")
                .priority(TargetGenePriority.HIGH)
                .build();
        final TargetGene targetGene2 = TargetGene.builder()
                .geneId("ENSPTRG00000004775")
                .geneName("KRAS")
                .taxId(SUS_SCROFA_TAX_ID)
                .speciesName("Sus scrofa")
                .priority(TargetGenePriority.HIGH)
                .build();
        final Target target = Target.builder()
                .targetName("Target")
                .owner("OWNER")
                .products(Arrays.asList("product1", "product2"))
                .diseases(Arrays.asList("disease1", "disease2"))
                .targetGenes(Arrays.asList(targetGene, targetGene1, targetGene2))
                .build();
        return targetManager.create(target);
    }
}
