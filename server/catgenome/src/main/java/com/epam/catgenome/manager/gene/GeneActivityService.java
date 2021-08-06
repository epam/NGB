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

package com.epam.catgenome.manager.gene;

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.activity.Activity;
import com.epam.catgenome.entity.activity.ActivityType;
import com.epam.catgenome.entity.gene.GeneHighLevel;
import com.epam.catgenome.entity.index.GeneIndexEntry;
import com.epam.catgenome.manager.activity.ActivityService;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GeneActivityService {

    private final ActivityService activityService;

    public List<Activity> saveGeneActivities(final GeneHighLevel newGeneContent,
                                             final GeneIndexEntry oldEntry) {
        final List<Activity> activities = new ArrayList<>();

        addActivityIfRequired(oldEntry.getFeature(), newGeneContent.getFeature(), activities,
                FeatureIndexDao.FeatureIndexFields.FEATURE_TYPE.getFieldName());

        addActivityIfRequired(oldEntry.getSource(), newGeneContent.getSource(), activities,
                FeatureIndexDao.FeatureIndexFields.SOURCE.getFieldName());

        final String oldStrand = oldEntry.getStrand();
        addActivityIfRequired(Objects.nonNull(oldStrand) ? StrandSerializable.forValue(oldStrand).name() : null,
                newGeneContent.getStrand().name(), activities,
                FeatureIndexDao.FeatureIndexFields.STRAND.getFieldName());

        addNumberActivityIfRequired(oldEntry.getFrame(), newGeneContent.getFrame(), activities,
                FeatureIndexDao.FeatureIndexFields.FRAME.getFieldName());

        addNumberActivityIfRequired(oldEntry.getScore(), newGeneContent.getScore(), activities,
                FeatureIndexDao.FeatureIndexFields.SCORE.getFieldName());

        addAttributesActivity(MapUtils.emptyIfNull(oldEntry.getAttributes()),
                MapUtils.emptyIfNull(newGeneContent.getAttributes()), activities);

        final String uuid = oldEntry.getUuid().toString();
        final Long featureFileId = oldEntry.getFeatureFileId();
        activities.stream()
                .map(activity -> setCommonFields(activity, uuid, featureFileId))
                .forEach(activityService::save);
        return activities;
    }

    private ActivityType determineActivityTypeFromStringValues(final String oldValue, final String newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return null;
        }
        if (StringUtils.isBlank(oldValue) && StringUtils.isNotBlank(newValue)) {
            return ActivityType.CREATE;
        }
        if (StringUtils.isNotBlank(oldValue) && StringUtils.isBlank(newValue)) {
            return ActivityType.DELETE;
        }
        return ActivityType.UPDATE;
    }

    private void addNumberActivityIfRequired(final Number oldValue, final Number newValue,
                                              final List<Activity> activities, final String field) {
        addActivityIfRequired(Objects.isNull(oldValue) ? null : oldValue.toString(),
                Objects.isNull(newValue) ? null : newValue.toString(), activities, field);
    }

    private void addActivityIfRequired(final String oldValue, final String newValue,
                                       final List<Activity> activities, final String field) {
        final ActivityType activityType = determineActivityTypeFromStringValues(oldValue, newValue);
        if (Objects.nonNull(activityType)) {
            activities.add(buildActivity(oldValue, newValue, activityType, field));
        }
    }

    private Activity buildActivity(final String oldValue, final String newValue, final ActivityType actionType,
                                   final String fieldName) {
        return Activity.builder()
                .actionType(actionType)
                .field(fieldName)
                .oldValue(ActivityType.CREATE.equals(actionType) ? null : oldValue)
                .newValue(ActivityType.DELETE.equals(actionType) ? null : newValue)
                .build();
    }

    private Activity setCommonFields(final Activity activity, final String uid, final Long fileId) {
        activity.setUid(uid);
        activity.setItemId(fileId);
        activity.setItemType(BiologicalDataItemFormat.GENE);
        return activity;
    }

    private void addAttributesActivity(final Map<String, String> oldAttributes, final Map<String, String> newAttributes,
                                       final List<Activity> activities) {
        final Set<String> commonAttributes = Sets.intersection(oldAttributes.keySet(), newAttributes.keySet());
        commonAttributes.stream()
                .map(attributeName -> buildAttributeUpdateActivity(oldAttributes.get(attributeName),
                        newAttributes.get(attributeName), attributeName))
                .filter(Objects::nonNull)
                .forEach(activities::add);
        oldAttributes.entrySet().stream()
                .filter(entry -> !commonAttributes.contains(entry.getKey()))
                .map(entry -> buildAttributeDeleteActivity(entry.getValue(), entry.getKey()))
                .filter(Objects::nonNull)
                .forEach(activities::add);
        newAttributes.entrySet().stream()
                .filter(entry -> !commonAttributes.contains(entry.getKey()))
                .map(entry -> buildAttributeCreateActivity(entry.getValue(), entry.getKey()))
                .filter(Objects::nonNull)
                .forEach(activities::add);
    }

    private Activity buildAttributeUpdateActivity(final String oldValue, final String newValue,
                                                  final String fieldName) {
        return buildAttributeActivity(oldValue, newValue, ActivityType.UPDATE, fieldName);
    }

    private Activity buildAttributeCreateActivity(final String newValue, final String fieldName) {
        return buildAttributeActivity(null, newValue, ActivityType.CREATE, fieldName);
    }

    private Activity buildAttributeDeleteActivity(final String oldValue, final String fieldName) {
        return buildAttributeActivity(oldValue, null, ActivityType.DELETE, fieldName);
    }

    private Activity buildAttributeActivity(final String oldValue, final String newValue,
                                            final ActivityType actionType, final String fieldName) {
        if (Objects.equals(oldValue, newValue)) {
            return null;
        }
        return buildActivity(oldValue, newValue, actionType, fieldName);
    }
}
