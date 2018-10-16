/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2018 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.epam.catgenome.util;

import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.dao.bam.BamFileDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.manager.reference.ReferenceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
public class NGBRegistrationUtils {

    private static final String BAI_EXTENSION = ".bai";

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ReferenceManager referenceManager;

    @Autowired
    private BamFileDao bamFileDao;

    @Autowired
    private BiologicalDataItemDao dataItemDao;

    private Resource resource;

    @PostConstruct
    void init() {
        resource = context.getResource("classpath:templates");
    }

    public Reference registerReference(String filename, String name) throws IOException {
        File fastaFile = new File(resource.getFile().getAbsolutePath() + filename);

        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName(name);
        request.setPath(fastaFile.getPath());

        Reference reference = referenceManager.registerGenome(request);
        List<Chromosome> chromosomeList = reference.getChromosomes();
        for (Chromosome chromosome : chromosomeList) {
            String chromosomeName = "X";
            if (chromosome.getName().equals(chromosomeName)) {
                break;
            }
        }
        return reference;
    }

    public String resolveFilePath(String filename) throws IOException {
        context.getResource("classpath:templates");
        return resource.getFile().getAbsolutePath() + filename;
    }

    public BamFile registerBam(Reference reference, String name, String entityName, String owner) throws IOException {
        final String path = resolveFilePath(name);
        BiologicalDataItem index = new BiologicalDataItem();
        index.setPath(path + BAI_EXTENSION);
        index.setType(BiologicalDataItemResourceType.FILE);
        index.setFormat(BiologicalDataItemFormat.BAM_INDEX);
        index.setOwner(owner);
        index.setCreatedDate(new Date());
        dataItemDao.createBiologicalDataItem(index);

        BamFile file = new BamFile();
        file.setPath(path);
        file.setIndex(index);
        file.setName(entityName);
        file.setOwner(owner);
        file.setReferenceId(reference.getId());
        file.setType(BiologicalDataItemResourceType.FILE);
        file.setCreatedDate(new Date());
        dataItemDao.createBiologicalDataItem(file);
        bamFileDao.createBamFile(file);
        return file;
    }
}
