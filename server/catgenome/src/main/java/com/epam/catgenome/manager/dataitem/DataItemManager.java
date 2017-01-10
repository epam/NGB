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

package com.epam.catgenome.manager.dataitem;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.constant.MessagesConstants.ERROR_BIO_ID_NOT_FOUND;
import static com.epam.catgenome.constant.MessagesConstants.ERROR_UNSUPPORTED_FILE_FORMAT;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.manager.bam.BamManager;
import com.epam.catgenome.manager.bed.BedManager;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.maf.MafManager;
import com.epam.catgenome.manager.seg.SegManager;
import com.epam.catgenome.manager.vcf.VcfManager;
import com.epam.catgenome.manager.wig.WigManager;

/**
 * {@code DataItemManager} represents a service class designed to encapsulate all business
 * logic operations required to manage data common to all types of files registered on the server.
 */
@Service
public class DataItemManager {

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private BamManager bamManager;

    @Autowired
    private BedManager bedManager;

    @Autowired
    private VcfManager vcfManager;

    @Autowired
    private WigManager wigManager;

    @Autowired
    private SegManager segManager;

    @Autowired
    private MafManager mafManager;

    @Autowired
    private GffManager geneManager;

    /**
     * Method finds all files registered in the system by an input search query
     * @param name to find
     * @param strict if true a strict, case sensitive search is performed,
     *               otherwise a substring, case insensitive search is performed
     * @return list of found files
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public List<BiologicalDataItem> findFilesByName(final String name, boolean strict) {
        if (strict) {
            return biologicalDataItemDao.loadFilesByNameStrict(name);
        } else {
            return biologicalDataItemDao.loadFilesByName(name);
        }
    }

    /**
     * Method fins a file registered in the system by BiologicalDataItem ID
     * @param id to find
     * @return loaded entity
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public BiologicalDataItem findFileByBioItemId(Long id) {
        List<BiologicalDataItem> items = biologicalDataItemDao.loadBiologicalDataItemsByIds(
                Collections.singletonList(id));
        Assert.notNull(items, getMessage(ERROR_BIO_ID_NOT_FOUND, id));
        Assert.isTrue(!items.isEmpty(), getMessage(ERROR_BIO_ID_NOT_FOUND, id));
        return items.get(0);
    }

    /**
     * Method defines the format of a file, specified by biological data item ID, and uses the
     * corresponding to the format manager to delete it. Reference file must be deleted using
     * corresponding API and are not supported in this method.
     * @param id biological data item ID
     * @return deleted file
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public BiologicalDataItem deleteFileByBioItemId(Long id) throws IOException {
        List<BiologicalDataItem> items = biologicalDataItemDao.loadBiologicalDataItemsByIds(
                Collections.singletonList(id));
        Assert.notNull(items, getMessage(ERROR_BIO_ID_NOT_FOUND, id));
        Assert.isTrue(!items.isEmpty(), getMessage(ERROR_BIO_ID_NOT_FOUND, id));
        final BiologicalDataItem item = items.get(0);
        final Long itemId = item.getId();
        switch (item.getFormat()) {
            case BAM:
                bamManager.unregisterBamFile(itemId);
                break;
            case BED:
                bedManager.unregisterBedFile(itemId);
                break;
            case GENE:
                geneManager.unregisterGeneFile(itemId);
                break;
            case WIG:
                wigManager.unregisterWigFile(itemId);
                break;
            case SEG:
                segManager.unregisterSegFile(itemId);
                break;
            case MAF:
                mafManager.unregisterMafFile(itemId);
                break;
            case VCF:
                vcfManager.unregisterVcfFile(itemId);
                break;
            default:
                throw new IllegalArgumentException(getMessage(ERROR_UNSUPPORTED_FILE_FORMAT,
                        item.getFormat()));
        }
        return item;

    }
}
