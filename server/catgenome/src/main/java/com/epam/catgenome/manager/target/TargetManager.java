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

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.target.TargetDao;
import com.epam.catgenome.dao.target.TargetGeneDao;
import com.epam.catgenome.entity.target.Target;
import com.epam.catgenome.entity.target.TargetGene;
import com.epam.catgenome.entity.target.TargetQueryParams;
import com.epam.catgenome.util.Condition;
import com.epam.catgenome.util.db.Page;
import com.epam.catgenome.util.db.SortInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static org.apache.commons.lang3.StringUtils.join;

@Service
@RequiredArgsConstructor
@Slf4j
public class TargetManager {

    private static final String LIKE_CLAUSE = "%s like '%%%s%%'";
    private static final String NAME = "name";
    private static final String PRODUCTS = "products";
    private static final String DISEASES = "diseases";
    private static final String SPECIES_NAME = "species_name";
    private final TargetDao targetDao;
    private final TargetGeneDao targetGeneDao;

    @Transactional(propagation = Propagation.REQUIRED)
    public Target createTarget(final Target target) {
        final Target createdTarget = targetDao.saveTarget(target);
        final List<TargetGene> targetGenes = targetGeneDao.saveTargetGenes(target.getTargetGenes(),
                target.getTargetId());
        createdTarget.setTargetGenes(targetGenes);
        return createdTarget;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Target updateTarget(Target target) {
        getTarget(target.getTargetId());
        targetGeneDao.deleteTargetGenes(target.getTargetId());
        return createTarget(target);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteTarget(final long targetId) {
        getTarget(targetId);
        targetGeneDao.deleteTargetGenes(targetId);
        targetDao.deleteTarget(targetId);
    }

    public Target loadTarget(final long targetId) {
        return targetDao.loadTarget(targetId);
    }

    public Page<Target> loadTargets(final TargetQueryParams targetQueryParams) {
        final String clause = getFilterClause(targetQueryParams);
        final long totalCount = targetDao.getTotalCount(clause);
        final SortInfo sortInfo = SortInfo.builder()
                .field(NAME)
                .ascending(true)
                .build();
        final List<Target> targets = targetDao.loadTargets(clause,
                targetQueryParams.getPagingInfo(),
                Collections.singletonList(sortInfo));
        final Set<Long> targetIds = targets.stream().map(Target::getTargetId).collect(Collectors.toSet());
        final List<TargetGene> targetGenes = targetGeneDao.loadTargetGenes(targetIds);
        for (Target t: targets) {
            t.setTargetGenes(targetGenes.stream()
                    .filter(g -> g.getTargetId().equals(t.getTargetId()))
                    .collect(Collectors.toList()));
        }
        return Page.<Target>builder()
                .totalCount(totalCount)
                .items(targets)
                .build();
    }

    public List<String> loadFieldValues(final TargetField field) {
        if (field.equals(TargetField.SPECIES_NAME)) {
            return targetGeneDao.loadSpeciesNames();
        }
        final List<Target> targets = targetDao.loadAllTargets();
        if (field.equals(TargetField.PRODUCTS)) {
            return targets.stream().map(Target::getProducts)
                    .flatMap(List::stream).distinct().collect(Collectors.toList());
        }
        if (field.equals(TargetField.DISEASES)) {
            return targets.stream().map(Target::getDiseases)
                    .flatMap(List::stream).distinct().collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static String getFilterClause(final TargetQueryParams targetQueryParams) {
        final List<String> clauses = new ArrayList<>();
        if (StringUtils.isNotBlank(targetQueryParams.getName())) {
            clauses.add(String.format(LIKE_CLAUSE, NAME, targetQueryParams.getName()));
        }
        if (!CollectionUtils.isEmpty(targetQueryParams.getProducts())) {
            clauses.add(getFilterClause(PRODUCTS, targetQueryParams.getProducts()));
        }
        if (!CollectionUtils.isEmpty(targetQueryParams.getDiseases())) {
            clauses.add(getFilterClause(DISEASES, targetQueryParams.getDiseases()));
        }
        if (!CollectionUtils.isEmpty(targetQueryParams.getSpeciesNames())) {
            clauses.add(getFilterClause(SPECIES_NAME, targetQueryParams.getSpeciesNames()));
        }
        return join(clauses, Condition.AND.getValue());
    }

    @NotNull
    private static String getFilterClause(final String field, final List<String> values) {
        return String.format("(%s)", values.stream()
                .map(v -> String.format(LIKE_CLAUSE, field, v))
                .collect(Collectors.joining(Condition.OR.getValue())));
    }

    private void getTarget(final long targetId) {
        final Target target = loadTarget(targetId);
        Assert.notNull(target, getMessage(MessagesConstants.ERROR_TARGET_NOT_FOUND, targetId));
    }
}
