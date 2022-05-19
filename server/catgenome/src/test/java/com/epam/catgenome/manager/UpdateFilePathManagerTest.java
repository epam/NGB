/*
 * MIT License
 *
 * Copyright (c) 2022 EPAM Systems
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

package com.epam.catgenome.manager;

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.HeatmapRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.LineageTreeRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.PathwayRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.heatmap.Heatmap;
import com.epam.catgenome.entity.lineage.LineageTree;
import com.epam.catgenome.entity.pathway.NGBPathway;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.dataitem.DataItemManager;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.heatmap.HeatmapManager;
import com.epam.catgenome.manager.lineage.LineageTreeManager;
import com.epam.catgenome.manager.pathway.PathwayManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
@Ignore
public class UpdateFilePathManagerTest extends AbstractManagerTest {

    private static final String HEATMAP_CONTENT = "\tgene1\tgene2\tgene3\tgene4\tgene5\tgene6\n" +
            "gene1\t0\t0.001273579\t0.001184362\t0.001193085\t0.00010578\t9.56E-05\n";

    private static final String LABEL_ANNOTATION_CONTENT = "gene1\ttest1\n";
    private static final String CELL_ANNOTATION_CONTENT = "\tgene1\tgene2\tgene3\tgene4\tgene5\n" +
            "gene1\ta\tb\tc\td\te\n";

    private static final String NODES_CONTENT = "name\tdescription\treferenceId\tdataset\tcreation_date\tattributes\n" +
            "strain-01\tdescription1\t1\tdataset\t2020-11-10\tkey1=value1,key2=value2\n" +
            "strain-02\tdescription2\t2\t999\t2020-11-11\tkey1=value1,key2=value2\n";

    private static final String EDGES_CONTENT = "from\tto\tattributes\ttype_of_interaction\n" +
            "strain-01\tstrain-02\tkey1=value1,key2=value2\tUV\n";

    private static final String GFF_CONTENT = "A1\tFelis_catus_6.2\tchromosome\t1\t239302903\t.\t.\t.\t" +
            "ID=chromosome:A1;Alias=CM001378.1,NC_018723.1\n" +
            "###\n" +
            "A1\tensembl\tgene\t35459\t46532\t.\t+\t.\tID=gene:ENSFCAG00000011704;Name=PGLYRP4;" +
            "biotype=protein_coding;" +
            "description=peptidoglycan recognition protein 4 [Source:HGNC Symbol%3BAcc:HGNC:30015];" +
            "gene_id=ENSFCAG00000011704;logic_name=ensembl;version=2\n";

    private static final String PATHWAY_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<sbgn xmlns=\"http://sbgn.org/libsbgn/0.2\">\n" +
            "    <map language=\"process description\">\n" +
            "        <glyph class=\"process\" " +
            "id=\"http___pathwaycommons_org_pc12_Transport_309295743d5faf180aabd52db5ecb83LEFT_TO_RIGHT\">\n" +
            "            <bbox w=\"15.0\" h=\"15.0\" x=\"243.53595\" y=\"210.26923\"/>\n" +
            "            <port " +
            "id=\"InputPort_http___pathwaycommons_org_pc12_Transport_309295743d5faf180aabd52db5ecb83LEFT_TO_RIGHT\" " +
            "x=\"251.03595\" y=\"227.76923\"/>\n" +
            "            <port " +
            "id=\"OutputPort_http___pathwaycommons_org_pc12_Transport_309295743d5faf180aabd52db5ecb83LEFT_TO_RIGHT\" " +
            "x=\"251.03595\" y=\"207.76923\"/>\n" +
            "        </glyph>\n" +
            "    </map>\n" +
            "</sbgn>\n";

    private static final int TEST_CHROMOSOME_SIZE = 239107476;

    @Autowired
    private UpdateItemPathManager updateItemPathManager;

    @Autowired
    private HeatmapManager heatmapManager;

    @Autowired
    private LineageTreeManager lineageTreeManager;

    @Autowired
    private PathwayManager pathwayManager;

    @Autowired
    private GffManager gffManager;

    @Autowired
    private DataItemManager dataItemManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public TemporaryFolder newTemporaryFolder = new TemporaryFolder();

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testUpdateHeatmap() throws IOException {
        final File heatmapFile = temporaryFolder.newFile("heatmap.tsv");
        FileUtils.writeStringToFile(heatmapFile, HEATMAP_CONTENT);
        final File labelAnnotationFile = temporaryFolder.newFile("label_annotation.tsv");
        FileUtils.writeStringToFile(labelAnnotationFile, LABEL_ANNOTATION_CONTENT);
        final File cellAnnotationFile = temporaryFolder.newFile("cell_annotation.tsv");
        FileUtils.writeStringToFile(cellAnnotationFile, CELL_ANNOTATION_CONTENT);

        final HeatmapRegistrationRequest request = new HeatmapRegistrationRequest();
        request.setPath(heatmapFile.getAbsolutePath());
        request.setLabelAnnotationPath(labelAnnotationFile.getAbsolutePath());
        request.setCellAnnotationPath(cellAnnotationFile.getAbsolutePath());
        final Heatmap heatmap = heatmapManager.createHeatmap(request);

        final File newHeatmapFile = newTemporaryFolder.newFile("heatmap.tsv");
        FileUtils.writeStringToFile(newHeatmapFile, HEATMAP_CONTENT);
        boolean deleted = heatmapFile.delete();
        Assert.assertTrue(deleted);
        final File newLabelAnnotationFile = newTemporaryFolder.newFile("label_annotation.tsv");
        FileUtils.writeStringToFile(newLabelAnnotationFile, LABEL_ANNOTATION_CONTENT);
        deleted = labelAnnotationFile.delete();
        Assert.assertTrue(deleted);
        final File newCellAnnotationFile = newTemporaryFolder.newFile("cell_annotation.tsv");
        FileUtils.writeStringToFile(newCellAnnotationFile, CELL_ANNOTATION_CONTENT);
        deleted = cellAnnotationFile.delete();
        Assert.assertTrue(deleted);

        updateItemPathManager.updateItemPath(temporaryFolder.getRoot().getAbsolutePath(),
                newTemporaryFolder.getRoot().getAbsolutePath(), false);

        final Heatmap updatedHeatmap = heatmapManager.loadHeatmap(heatmap.getHeatmapId());
        Assert.assertEquals(newHeatmapFile.getAbsolutePath(), updatedHeatmap.getPath());
        Assert.assertEquals(newLabelAnnotationFile.getAbsolutePath(), updatedHeatmap.getLabelAnnotationPath());
        Assert.assertEquals(newCellAnnotationFile.getAbsolutePath(), updatedHeatmap.getCellAnnotationPath());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testUpdateLineageTree() throws IOException {
        final File nodesFile = temporaryFolder.newFile("nodes.txt");
        FileUtils.writeStringToFile(nodesFile, NODES_CONTENT);
        final File edgesFile = temporaryFolder.newFile("edges.txt");
        FileUtils.writeStringToFile(edgesFile, EDGES_CONTENT);

        final LineageTreeRegistrationRequest request = new LineageTreeRegistrationRequest();
        request.setEdgesPath(edgesFile.getAbsolutePath());
        request.setNodesPath(nodesFile.getAbsolutePath());
        final LineageTree lineageTree = lineageTreeManager.createLineageTree(request);

        final File newNodesFile = newTemporaryFolder.newFile("nodes.txt");
        FileUtils.writeStringToFile(newNodesFile, NODES_CONTENT);
        boolean deleted = nodesFile.delete();
        Assert.assertTrue(deleted);
        final File newEdgesFile = newTemporaryFolder.newFile("edges.txt");
        FileUtils.writeStringToFile(newEdgesFile, EDGES_CONTENT);
        deleted = edgesFile.delete();
        Assert.assertTrue(deleted);

        updateItemPathManager.updateItemPath(temporaryFolder.getRoot().getAbsolutePath(),
                newTemporaryFolder.getRoot().getAbsolutePath(), false);

        final LineageTree updatedLineageTree = lineageTreeManager.loadLineageTree(lineageTree.getLineageTreeId());
        Assert.assertEquals(newEdgesFile.getAbsolutePath(), updatedLineageTree.getEdgesPath());
        Assert.assertEquals(newNodesFile.getAbsolutePath(), updatedLineageTree.getNodesPath());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testUpdatePathway() throws IOException {
        final File pathwayFile = temporaryFolder.newFile("pathway.sbgn");
        FileUtils.writeStringToFile(pathwayFile, PATHWAY_CONTENT);

        final PathwayRegistrationRequest request = PathwayRegistrationRequest.builder()
                .path(pathwayFile.getAbsolutePath())
                .build();
        final NGBPathway pathway = pathwayManager.registerPathway(request);

        final File newPathwayFile = newTemporaryFolder.newFile("pathway.sbgn");
        FileUtils.writeStringToFile(newPathwayFile, PATHWAY_CONTENT);
        final boolean deleted = pathwayFile.delete();
        Assert.assertTrue(deleted);

        updateItemPathManager.updateItemPath(temporaryFolder.getRoot().getAbsolutePath(),
                newTemporaryFolder.getRoot().getAbsolutePath(), false);

        final NGBPathway updatedPathway = pathwayManager.loadPathway(pathway.getPathwayId());
        Assert.assertEquals(newPathwayFile.getAbsolutePath(), updatedPathway.getPath());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testUpdateGeneFile() throws IOException {
        final File gene = temporaryFolder.newFile("genes.gff");
        FileUtils.writeStringToFile(gene, GFF_CONTENT);

        final Chromosome testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        final Reference testReference = EntityHelper.createNewReference(testChromosome,
                referenceGenomeManager.createReferenceId());
        referenceGenomeManager.create(testReference);
        final Long referenceId = testReference.getId();

        final FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setName("genes");
        request.setPath(gene.getAbsolutePath());

        final GeneFile geneFile = gffManager.registerGeneFile(request);

        final File newGene = newTemporaryFolder.newFile("genes.gff");
        FileUtils.writeStringToFile(newGene, GFF_CONTENT);
        final boolean deleted = gene.delete();
        Assert.assertTrue(deleted);

        updateItemPathManager.updateItemPath(temporaryFolder.getRoot().getAbsolutePath(),
                newTemporaryFolder.getRoot().getAbsolutePath(), false);

        final BiologicalDataItem item = dataItemManager.findFileByBioItemId(geneFile.getBioDataItemId());
        Assert.assertEquals(newGene.getAbsolutePath(), item.getPath());
    }
}
