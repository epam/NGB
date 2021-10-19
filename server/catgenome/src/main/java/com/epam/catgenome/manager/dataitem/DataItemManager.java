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
import static com.epam.catgenome.constant.MessagesConstants.ERROR_FILE_LOCAL_DOWNLOAD;
import static com.epam.catgenome.constant.MessagesConstants.ERROR_UNSUPPORTED_FILE_FORMAT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemDownloadUrl;
import com.epam.catgenome.entity.BiologicalDataItemFile;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.manager.wig.FacadeWigManager;
import com.epam.catgenome.util.aws.S3Client;
import com.epam.catgenome.util.azure.AzureBlobClient;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.manager.bam.BamManager;
import com.epam.catgenome.manager.bed.BedManager;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.maf.MafManager;
import com.epam.catgenome.manager.seg.SegManager;
import com.epam.catgenome.manager.vcf.VcfManager;

/**
 * {@code DataItemManager} represents a service class designed to encapsulate all business
 * logic operations required to manage data common to all types of files registered on the server.
 */
@Service
public class DataItemManager {
    private static final String DOWNLOAD_LOCAL_FILE_URL_FORMAT = "%s/restapi/dataitem/%d/download";

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private BamManager bamManager;

    @Autowired
    private BedManager bedManager;

    @Autowired
    private VcfManager vcfManager;

    @Autowired
    private FacadeWigManager facadeWigManager;

    @Autowired
    private SegManager segManager;

    @Autowired
    private MafManager mafManager;

    @Autowired
    private GffManager geneManager;

    @Autowired
    private AzureBlobClient azureBlobClient;

    @Value("${base.external.url:}")
    private String baseExternalUrl;

    /**
     * Method finds all files registered in the system by an input search query
     * @param name to find
     * @param strict if true a strict, case sensitive search is performed,
     *               otherwise a substring, case insensitive search is performed
     * @return list of found files
     */
    @Transactional(propagation = Propagation.SUPPORTS)
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
        if (item.getId() == null || item.getId() == 0) {
            biologicalDataItemDao.deleteBiologicalDataItem(id);
            return item;
        }
        switch (item.getFormat()) {
            case BAM:
                bamManager.unregisterBamFile(itemId);
                break;
            case BED:
                bedManager.unregisterBedFile(itemId);
                break;
            case GENE:
            case FEATURE_COUNTS:
                geneManager.unregisterGeneFile(itemId);
                break;
            case WIG:
                facadeWigManager.unregisterWigFile(itemId);
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

    public Map<String, BiologicalDataItemFormat> getFormats() {
        return bedManager.getFormats().stream()
                .map(e -> Pair.of(e, BiologicalDataItemFormat.BED))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    public BiologicalDataItemFile loadItemFile(final BiologicalDataItem biologicalDataItem,
                                               final Boolean source) throws IOException {
        final String dataItemPath = source ? biologicalDataItem.getSource() : biologicalDataItem.getPath();
        if (BiologicalDataItemResourceType.FILE.equals(biologicalDataItem.getType())) {
            return loadLocalFileItem(biologicalDataItem, dataItemPath);
        }
        throw new UnsupportedOperationException("Download available for local data only");
    }

    private BiologicalDataItemFile loadLocalFileItem(final BiologicalDataItem biologicalDataItem,
                                                     final String dataItemPath) throws IOException {
        Assert.state(Files.exists(Paths.get(dataItemPath)),
                getMessage(ERROR_BIO_ID_NOT_FOUND, biologicalDataItem.getId()));
        return BiologicalDataItemFile.builder()
                .content(Files.newInputStream(Paths.get(dataItemPath)))
                .fileName(FilenameUtils.getName(dataItemPath))
                .build();
    }

    public BiologicalDataItemDownloadUrl generateDownloadUrl(final Long id,
                                                             final BiologicalDataItem biologicalDataItem) {
        final BiologicalDataItemResourceType type = determineType(biologicalDataItem);
        switch (type) {
            case FILE:
                return generateDownloadUrlForLocalFile(id, biologicalDataItem);
            case S3:
                return S3Client.getInstance().generatePresignedUrl(biologicalDataItem.getSource());
            case AZ:
                return azureBlobClient.generatePresignedUrl(biologicalDataItem.getSource());
            default:
                throw new UnsupportedOperationException(String.format(
                        "Cannot generate download url for data type '%s'", biologicalDataItem.getType()));
        }
    }

    private BiologicalDataItemResourceType determineType(final BiologicalDataItem biologicalDataItem) {
        if (S3Client.isS3Source(biologicalDataItem.getSource())) {
            return BiologicalDataItemResourceType.S3;
        }
        if (AzureBlobClient.isAzSource(biologicalDataItem.getSource())) {
            return BiologicalDataItemResourceType.AZ;
        }
        return biologicalDataItem.getType();
    }

    private BiologicalDataItemDownloadUrl generateDownloadUrlForLocalFile(final Long id,
                                                                          final BiologicalDataItem biologicalDataItem) {
        final Path dataItemPath = Paths.get(biologicalDataItem.getSource());
        try {
            Assert.state(Files.exists(dataItemPath),
                    getMessage(ERROR_BIO_ID_NOT_FOUND, biologicalDataItem.getId()));
            return BiologicalDataItemDownloadUrl.builder()
                    .url(String.format(DOWNLOAD_LOCAL_FILE_URL_FORMAT,
                            StringUtils.removeEnd(baseExternalUrl, "/"), id))
                    .type(BiologicalDataItemResourceType.FILE)
                    .size(Files.size(dataItemPath))
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException(getMessage(ERROR_FILE_LOCAL_DOWNLOAD, id), e);
        }
    }
}
