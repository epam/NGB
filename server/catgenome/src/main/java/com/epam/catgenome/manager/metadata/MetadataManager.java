/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

package com.epam.catgenome.manager.metadata;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.metadata.MetadataDao;
import com.epam.catgenome.entity.metadata.EntityVO;
import com.epam.catgenome.entity.metadata.MetadataVO;
import com.epam.catgenome.entity.security.AbstractSecuredEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataManager {

    private final MetadataDao metadataDao;

    @Transactional(propagation = Propagation.REQUIRED)
    public MetadataVO upsert(final MetadataVO metadataVO) {
        final Long entityId = metadataVO.getId();
        Assert.notNull(entityId, MessageHelper.getMessage(MessagesConstants.ERROR_METADATA_ENTITY_ID_NOT_SPECIFIED));
        Assert.notNull(metadataVO.getAclClass(), MessageHelper.getMessage(
                MessagesConstants.ERROR_METADATA_ENTITY_CLASS_NOT_SPECIFIED));
        final String entityClass = metadataVO.getAclClass().name();

        prepareMetadata(metadataVO);

        final MetadataVO loadedMetadata = metadataDao.get(entityId, entityClass);
        if (Objects.isNull(loadedMetadata)) {
            metadataDao.save(metadataVO);
            return metadataDao.get(entityId, entityClass);
        }

        if (MapUtils.isEmpty(metadataVO.getMetadata())) {
            metadataDao.delete(entityId, entityClass);
            return metadataVO;
        }

        metadataDao.update(metadataVO);
        return metadataDao.get(entityId, entityClass);
    }

    public MetadataVO get(final Long entityId, final String entityClass) {
        return metadataDao.get(entityId, entityClass);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public MetadataVO delete(final AbstractSecuredEntity entity) {
        return delete(entity.getId(), entity.getAclClass().name());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public MetadataVO delete(final Long entityId, final String entityClass) {
        final MetadataVO metadataVO = metadataDao.get(entityId, entityClass);
        if (Objects.isNull(metadataVO)) {
            log.debug("Requested metadata for entity ID '{}' and class '{}' does not exist", entityId, entityClass);
            return null;
        }
        metadataDao.delete(entityId, entityClass);
        return metadataVO;
    }

    public Map<EntityVO, MetadataVO> getItems(final List<EntityVO> items) {
        return metadataDao.getItems(items).stream()
                .collect(Collectors.toMap(metadataVO -> EntityVO.builder()
                        .entityId(metadataVO.getId())
                        .entityClass(metadataVO.getAclClass())
                        .build(), Function.identity()));
    }

    private void prepareMetadata(final MetadataVO metadataVO) {
        final Map<String, String> preparedData = MapUtils.emptyIfNull(metadataVO.getMetadata()).entrySet().stream()
                .peek(entry -> Assert.isTrue(StringUtils.isNotBlank(entry.getKey()),
                        MessageHelper.getMessage(MessagesConstants.ERROR_METADATA_EMPTY_KEY)))
                .peek(entry -> Assert.isTrue(StringUtils.isNotBlank(entry.getValue()),
                        MessageHelper.getMessage(MessagesConstants.ERROR_METADATA_EMPTY_VALUE)))
                .collect(Collectors.toMap(entry -> StringUtils.upperCase(entry.getKey()), entry ->
                        StringUtils.upperCase(entry.getValue())));
        metadataVO.setMetadata(preparedData);
    }
}
