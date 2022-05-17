/*
 * MIT License
 *
 * Copyright (c) 2016-2022 EPAM Systems
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

import static com.epam.catgenome.component.MessageHelper.getMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.manager.project.ProjectManager;
import com.epam.catgenome.util.Utils;
import com.epam.catgenome.util.db.PagingInfo;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Source:      BiologicalDataItemManager
 * Created:     21.03.16, 19:19
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A service class to operate BiologicalDataItem entities
 * </p>
 */
@Service
public class BiologicalDataItemManager {

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private AuthManager authManager;

    private static final String URL_PATTERN = "/#/${REFERENCE_NAME}${CHROMOSOME_NAME}${INDEXES}?tracks=${TRACKS}";
    private static final String INDEXES_PATTERN = "/${START_INDEX}/${END_INDEX}";

    /**
     * Persists a BiologicalDataItem entity to the database
     * @param item a BiologicalDataItem to persist
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void createBiologicalDataItem(BiologicalDataItem item) {
        item.setOwner(authManager.getAuthorizedUser());
        biologicalDataItemDao.createBiologicalDataItem(item);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateBiologicalDataItems(final List<BiologicalDataItem> items) {
        biologicalDataItemDao.updateBiologicalDataItems(items);
    }

    /**
     * Deletes a BiologicalDataItem entity from the database
     * @param id a BiologicalDataItem ID to delete
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteBiologicalDataItem(long id) {
        biologicalDataItemDao.deleteBiologicalDataItem(id);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<BiologicalDataItem> loadAllItems() {
        return biologicalDataItemDao.loadBiologicalDataItems();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<BiologicalDataItem> loadAllItems(final PagingInfo paging) {
        return biologicalDataItemDao.loadBiologicalDataItems(paging);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public Integer countItems() {
        return biologicalDataItemDao.countBiologicalDataItems();
    }

    /**
     * Generates a URL parameters that will open required files on required position, specified by chromosome name,
     * start and end indexes
     *
     *
     * @param dataset
     * @param ids bio logical
     * @param chromosomeName
     * @param startIndex
     * @param endIndex
     * @return
     * @throws JsonProcessingException
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String generateUrl(String dataset, List<String> ids, String chromosomeName,
            Integer startIndex, Integer endIndex)
        throws JsonProcessingException {
        Project project;
        if (NumberUtils.isDigits(dataset)) {
            project = projectManager.load(Long.parseLong(dataset));
        } else {
            project = projectManager.load(dataset);
        }

        Assert.notNull(project, getMessage(MessagesConstants.ERROR_PROJECT_ID_NOT_FOUND, dataset));
        List<String> bioItemNames = new ArrayList<>();
        List<Long> bioItemIds = new ArrayList<>();
        for (String id : ids) {
            if (NumberUtils.isDigits(id)) {
                bioItemIds.add(Long.parseLong(id));
            } else {
                bioItemNames.add(id);
            }
        }

        List<BiologicalDataItem> itemsByNames = biologicalDataItemDao.loadFilesByNamesStrict(bioItemNames);
        if (itemsByNames.size() != bioItemNames.size()) {
            throw new IllegalArgumentException(getMessage(
                MessagesConstants.ERROR_BIO_NAME_NOT_FOUND,
                bioItemNames.stream()
                    .filter(n -> itemsByNames.stream().noneMatch(i -> i.getName().equals(n)))
                    .collect(Collectors.joining(", ")))
            );
        }

        List<BiologicalDataItem> itemsByIds = biologicalDataItemDao.loadBiologicalDataItemsByIds(bioItemIds);
        if (itemsByIds.size() != bioItemIds.size()) {
            throw new IllegalArgumentException(getMessage(
                MessagesConstants.ERROR_BIO_ID_NOT_FOUND,
                bioItemIds.stream()
                    .filter(id -> itemsByIds.stream().noneMatch(i -> i.getId().equals(id)))
                    .map(Object::toString)
                    .collect(Collectors.joining(", ")))
            );
        }

        List<BiologicalDataItem> items = new ArrayList<>(itemsByNames.size() + itemsByIds.size());
        items.addAll(itemsByNames);
        items.addAll(itemsByIds);

        List<Long> references = project.getItems().stream()
                .filter(item -> item.getBioDataItem().getFormat() == BiologicalDataItemFormat.REFERENCE)
                .map(item -> item.getBioDataItem().getId()).collect(Collectors.toList());
        Assert.notNull(references);
        Assert.isTrue(!references.isEmpty());
        Long referenceId = references.get(0);
        Reference reference = referenceGenomeManager.load(referenceId);
        items.add(reference);

        for (BiologicalDataItem item : items) {
            if (FeatureFile.class.isAssignableFrom(item.getClass())) {
                FeatureFile file = (FeatureFile) item;
                Assert.isTrue(project.getItems().stream()
                                .anyMatch(i -> i.getBioDataItem().getId().equals(item.getId())),
                        getMessage(MessagesConstants.ERROR_PROJECT_FILE_NOT_FOUND, item.getName(),
                                project.getName()));
                Assert.isTrue(referenceId.equals(file.getReferenceId()),
                                  "Specified files have different references");
            }
        }

        return makeUrl(items, project, reference, chromosomeName, startIndex, endIndex);
    }

    private String makeUrl(List<BiologicalDataItem> items, Project project, Reference reference, String chromosomeName,
                           Integer startIndex, Integer endIndex) throws JsonProcessingException {
        String indexes;
        if (startIndex != null && endIndex != null && chromosomeName != null) {
            Map<String, Object> params = new HashMap<>();
            params.put("START_INDEX", startIndex);
            params.put("END_INDEX", endIndex);

            indexes = new StrSubstitutor(params).replace(INDEXES_PATTERN);
        } else {
            indexes = "";
        }

        Map<String, String> params = new HashMap<>();
        params.put("REFERENCE_NAME", reference.getName());
        params.put("INDEXES", indexes);

        if (chromosomeName != null) {
            Optional<Chromosome> chromosomeOpt = reference.getChromosomes().stream()
                .filter(c -> c.getName().equalsIgnoreCase(chromosomeName) ||
                        c.getName().equals(Utils.changeChromosomeName(chromosomeName))).findFirst();
            Assert.isTrue(chromosomeOpt.isPresent(), getMessage(
                MessagesConstants.ERROR_CHROMOSOME_NAME_NOT_FOUND, chromosomeName));

            chromosomeOpt.ifPresent(chromosome -> params.put("CHROMOSOME_NAME", "/" + chromosome.getName()));
        } else {
            params.put("CHROMOSOME_NAME", "");
        }

        List<TrackVO> vos;
        if (items.isEmpty()) {
            vos = new ArrayList<>();
            TrackVO vo =new TrackVO();
            vo.setP(project.getName());
            vos.add(vo);
        } else {
            vos = items.stream()
                    .map(item -> new TrackVO(item.getName(), project.getName()))
                    .collect(Collectors.toList());
        }

        JsonMapper mapper = new JsonMapper();
        params.put("TRACKS", mapper.writeValueAsString(vos));

        return new StrSubstitutor(params).replace(URL_PATTERN);
    }

    static class TrackVO {
        private String b;
        private String p;

        TrackVO() {
            // no-op
        }

        TrackVO(String b, String p) {
            this.b = b;
            this.p = p;
        }

        public String getB() {
            return b;
        }

        public void setB(String b) {
            this.b = b;
        }

        public String getP() {
            return p;
        }

        public void setP(String p) {
            this.p = p;
        }
    }
}
