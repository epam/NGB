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
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;

public class GeneActivityServiceTest {
    private static final String UID = "e8dd9d77-2342-4fa6-ac6f-e110a596d717";
    private static final String FEATURE_TYPE_OLD = "gene";
    private static final String FEATURE_TYPE_NEW = "exon";
    private static final String SOURCE = "source";
    private static final String STRAND_OLD = "POSITIVE";
    private static final String STRAND_NEW = "NEGATIVE";
    private static final Integer FRAME_OLD = 1;
    private static final Integer FRAME_NEW = -1;
    private static final Float SCORE_OLD = -1.0f;
    private static final Float SCORE_NEW = 1.0f;
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_VALUE = "value";
    private static final String OLD = "1";
    private static final String NEW = "2";

    private final ActivityService activityServiceMock = mock(ActivityService.class);
    private final GeneActivityService activityService = new GeneActivityService(activityServiceMock);

    @Test
    public void shouldCreateUpdateActivity() {
        final List<Activity> activities = activityService.saveGeneActivities(newContent(), oldContent());

        assertThat(activities.size(), is(6));
        final Map<String, Activity> activitiesByField = activities.stream()
                .collect(Collectors.toMap(Activity::getField, Function.identity()));
        assertActivityUpdate(activitiesByField, FeatureIndexDao.FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                FEATURE_TYPE_OLD, FEATURE_TYPE_NEW);
        assertActivityUpdate(activitiesByField, FeatureIndexDao.FeatureIndexFields.SOURCE.getFieldName(),
                SOURCE + OLD, SOURCE + NEW);
        assertActivityUpdate(activitiesByField, FeatureIndexDao.FeatureIndexFields.STRAND.getFieldName(),
                STRAND_OLD, STRAND_NEW);
        assertActivityUpdate(activitiesByField, FeatureIndexDao.FeatureIndexFields.FRAME.getFieldName(),
                FRAME_OLD.toString(), FRAME_NEW.toString());
        assertActivityUpdate(activitiesByField, FeatureIndexDao.FeatureIndexFields.SCORE.getFieldName(),
                SCORE_OLD.toString(), SCORE_NEW.toString());
        assertActivityUpdate(activitiesByField, ATTRIBUTE_NAME,
                ATTRIBUTE_VALUE + OLD, ATTRIBUTE_VALUE + NEW);
    }

    @Test
    public void shouldCreateDeleteActivity() {
        final GeneHighLevel newGeneContent = new GeneHighLevel();
        newGeneContent.setStrand(StrandSerializable.NONE);

        final List<Activity> activities = activityService.saveGeneActivities(newGeneContent, oldContent());

        assertThat(activities.size(), is(6));
        final Map<String, Activity> activitiesByField = activities.stream()
                .collect(Collectors.toMap(Activity::getField, Function.identity()));
        assertActivityDelete(activitiesByField, FeatureIndexDao.FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                FEATURE_TYPE_OLD);
        assertActivityDelete(activitiesByField, FeatureIndexDao.FeatureIndexFields.SOURCE.getFieldName(),
                SOURCE + OLD);
        assertActivityUpdate(activitiesByField, FeatureIndexDao.FeatureIndexFields.STRAND.getFieldName(),
                STRAND_OLD, "NONE");
        assertActivityDelete(activitiesByField, FeatureIndexDao.FeatureIndexFields.FRAME.getFieldName(),
                FRAME_OLD.toString());
        assertActivityDelete(activitiesByField, FeatureIndexDao.FeatureIndexFields.SCORE.getFieldName(),
                SCORE_OLD.toString());
        assertActivityDelete(activitiesByField, ATTRIBUTE_NAME, ATTRIBUTE_VALUE + OLD);
    }

    @Test
    public void shouldCreateCreateActivity() {
        final GeneIndexEntry oldContent = new GeneIndexEntry();
        oldContent.setUuid(UUID.fromString(UID));
        oldContent.setFeatureFileId(1L);

        final List<Activity> activities = activityService.saveGeneActivities(newContent(), oldContent);

        assertThat(activities.size(), is(6));
        final Map<String, Activity> activitiesByField = activities.stream()
                .collect(Collectors.toMap(Activity::getField, Function.identity()));
        assertActivityCreate(activitiesByField, FeatureIndexDao.FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                FEATURE_TYPE_NEW);
        assertActivityCreate(activitiesByField, FeatureIndexDao.FeatureIndexFields.SOURCE.getFieldName(),
                SOURCE + NEW);
        assertActivityCreate(activitiesByField, FeatureIndexDao.FeatureIndexFields.STRAND.getFieldName(),
                STRAND_NEW);
        assertActivityCreate(activitiesByField, FeatureIndexDao.FeatureIndexFields.FRAME.getFieldName(),
                FRAME_NEW.toString());
        assertActivityCreate(activitiesByField, FeatureIndexDao.FeatureIndexFields.SCORE.getFieldName(),
                SCORE_NEW.toString());
        assertActivityCreate(activitiesByField, ATTRIBUTE_NAME, ATTRIBUTE_VALUE + NEW);
    }

    private void assertActivityUpdate(final Map<String, Activity> activitiesByField, final String field,
                                      final String oldValue, final String newValue) {
        assertActivity(activitiesByField, field, oldValue, newValue, ActivityType.UPDATE);
    }

    private void assertActivityDelete(final Map<String, Activity> activitiesByField, final String field,
                                      final String oldValue) {
        assertActivity(activitiesByField, field, oldValue, null, ActivityType.DELETE);
    }

    private void assertActivityCreate(final Map<String, Activity> activitiesByField, final String field,
                                      final String newValue) {
        assertActivity(activitiesByField, field, null, newValue, ActivityType.CREATE);
    }

    private void assertActivity(final Map<String, Activity> activitiesByField, final String field,
                                final String oldValue, final String newValue, final ActivityType activityType) {
        assertThat(activitiesByField.containsKey(field), is(true));
        activitiesByField.computeIfPresent(field, (key, value) -> {
            assertCommon(value, activityType);
            assertThat(value.getOldValue(), is(oldValue));
            assertThat(value.getNewValue(), is(newValue));
            return value;
        });
    }

    private void assertCommon(final Activity activity, final ActivityType activityType) {
        assertThat(activity.getUid(), is(UID));
        assertThat(activity.getItemId(), is(1L));
        assertThat(activity.getItemType(), is(BiologicalDataItemFormat.GENE));
        assertThat(activity.getActionType(), is(activityType));
    }

    private GeneIndexEntry oldContent() {
        final GeneIndexEntry geneIndexEntry = new GeneIndexEntry();
        geneIndexEntry.setUuid(UUID.fromString(UID));
        geneIndexEntry.setFeatureFileId(1L);
        geneIndexEntry.setFeature(FEATURE_TYPE_OLD);
        geneIndexEntry.setSource(SOURCE + OLD);
        geneIndexEntry.setStrand("+");
        geneIndexEntry.setFrame(FRAME_OLD);
        geneIndexEntry.setScore(SCORE_OLD);
        geneIndexEntry.setAttributes(Collections.singletonMap(ATTRIBUTE_NAME, ATTRIBUTE_VALUE + OLD));
        return geneIndexEntry;
    }

    private GeneHighLevel newContent() {
        final GeneHighLevel geneHighLevel = new GeneHighLevel();
        geneHighLevel.setFeature(FEATURE_TYPE_NEW);
        geneHighLevel.setSource(SOURCE + NEW);
        geneHighLevel.setStrand(StrandSerializable.NEGATIVE);
        geneHighLevel.setFrame(FRAME_NEW);
        geneHighLevel.setScore(SCORE_NEW);
        geneHighLevel.setAttributes(Collections.singletonMap(ATTRIBUTE_NAME, ATTRIBUTE_VALUE + NEW));
        return geneHighLevel;
    }
}
