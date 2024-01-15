/*
 * MIT License
 *
 * Copyright (c) 2023-2024 EPAM Systems
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
import com.epam.catgenome.dao.target.TargetIdentificationDao;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.target.*;
import com.epam.catgenome.exception.TargetUpdateException;
import com.epam.catgenome.manager.AuthManager;
import com.epam.catgenome.util.db.Condition;
import com.epam.catgenome.util.db.Page;
import com.epam.catgenome.util.db.SortInfo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.util.Utils.*;
import static com.epam.catgenome.util.db.DBQueryUtils.*;
import static org.apache.commons.lang3.StringUtils.join;

@Service
@RequiredArgsConstructor
public class TargetManager {

    private static final String TARGET_NAME = "target_name";
    private static final String TARGET_ID = "t.target_id";
    private static final String OWNER = "owner";
    private static final String PRODUCTS = "products";
    private static final String DISEASES = "diseases";
    private static final String GENE_NAME = "gene_name";
    private static final String TAX_ID = "tax_id";
    private static final String SPECIES_NAME = "species_name";
    private final TargetDao targetDao;
    private final TargetGeneDao targetGeneDao;
    private final TargetGeneManager targetGeneManager;
    private final TargetIdentificationDao targetIdentificationDao;
    private final AuthManager authManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public Target create(final Target target) throws IOException {
        if (StringUtils.isEmpty(target.getOwner())) {
            target.setOwner(authManager.getAuthorizedUser());
        }
        target.setAlignmentStatus(AlignmentStatus.NOT_ALIGNED);
        target.setPatentsSearchStatus(PatentsSearchStatus.NOT_STARTED);
        final Target createdTarget = targetDao.saveTarget(target);
        if (isDefault(target)) {
            final List<TargetGene> targetGenes = targetGeneDao.saveTargetGenes(target.getTargetGenes(),
                    target.getTargetId());
            createdTarget.setTargetGenes(targetGenes);
        }
        return createdTarget;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Target update(final Target target) throws TargetUpdateException, IOException {
        if (isDefault(target)) {
            final Target oldTarget = getTarget(target.getTargetId());
            final List<String> genesToDelete = getGenesToDelete(target, oldTarget);
            if (!CollectionUtils.isEmpty(genesToDelete)) {
                final List<TargetIdentification> targetIdentifications = oldTarget.getIdentifications();
                final List<TargetIdentification> identifications = new ArrayList<>();
                targetIdentifications.forEach(i -> {
                    List<String> genesOfInterest = i.getGenesOfInterest().stream()
                            .map(String::toLowerCase)
                            .collect(Collectors.toList());
                    List<String> translationalGenes = i.getTranslationalGenes().stream()
                            .map(String::toLowerCase)
                            .collect(Collectors.toList());
                    if (genesOfInterest.stream().filter(genesToDelete::contains).count() +
                            translationalGenes.stream().filter(genesToDelete::contains).count() > 0) {
                        identifications.add(i);
                    }
                });
                if (!CollectionUtils.isEmpty(identifications)) {
                    final boolean force = Optional.ofNullable(target.getForce()).orElse(false);
                    if (force) {
                        identifications.forEach(i -> targetIdentificationDao.delete(i.getId()));
                    } else {
                        final List<String> identificationNames = identifications.stream()
                                .map(BaseEntity::getName)
                                .collect(Collectors.toList());
                        throw new TargetUpdateException(String.format("Can't delete genes %s because of saved " +
                                        "identifications %s", join(genesToDelete, ","),
                                join(identificationNames, ",")));
                    }
                }
            }
            targetGeneDao.deleteTargetGenes(target.getTargetId());
        }
        final Target updatedTarget = create(target);
        updatedTarget.setIdentifications(
                targetIdentificationDao.loadTargetIdentifications(updatedTarget.getTargetId()));
        return updatedTarget;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateAlignmentStatus(final Target target) {
        targetDao.updateAlignment(target);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updatePatentsSearchStatus(final Target target) {
        targetDao.updatePatentsSearchStatus(target);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(final long targetId) throws ParseException, IOException {
        final Target target = getTarget(targetId);
        targetIdentificationDao.deleteTargetIdentifications(targetId);
        if (isDefault(target)) {
            targetGeneDao.deleteTargetGenes(targetId);
        } else {
            targetGeneManager.delete(targetId, Collections.emptyList());
        }
        targetDao.deleteTarget(targetId);
    }

    public Target getTarget(final long targetId) {
        final Target target = load(targetId);
        Assert.notNull(target, getMessage(MessagesConstants.ERROR_TARGET_NOT_FOUND, targetId));
        return target;
    }

    public Target load(final long targetId) {
        final Target target = targetDao.loadTarget(targetId);
        if (target != null) {
            final List<TargetIdentification> identifications =
                    targetIdentificationDao.loadTargetIdentifications(target.getTargetId());
            target.setIdentifications(identifications);
        }
        return target;
    }

    public Page<Target> load(final TargetQueryParams targetQueryParams) {
        final String clause = getFilterClause(targetQueryParams);
        final long totalCount = targetDao.getTotalCount(clause);
        final SortInfo sortInfo = targetQueryParams.getSortInfo() != null ?
                targetQueryParams.getSortInfo() : SortInfo.builder()
                .field(TARGET_NAME)
                .ascending(true)
                .build();
        final List<Target> targets = targetDao.loadTargets(clause,
                targetQueryParams.getPagingInfo(),
                Collections.singletonList(sortInfo));
        final Set<Long> targetIds = targets.stream().map(Target::getTargetId).collect(Collectors.toSet());
        final List<TargetGene> targetGenes = targetGeneDao.loadTargetGenes(targetIds);
        final List<TargetIdentification> identifications = getIdentifications(targetIds);
        for (Target t: targets) {
            t.setTargetGenes(targetGenes.stream()
                    .filter(g -> g.getTargetId().equals(t.getTargetId()))
                    .collect(Collectors.toList()));
            t.setIdentifications(identifications.stream()
                    .filter(g -> g.getTargetId().equals(t.getTargetId()))
                    .collect(Collectors.toList()));
        }
        return Page.<Target>builder()
                .totalCount(totalCount)
                .items(targets)
                .build();
    }

    public List<Target> load(final String geneName, final Long taxId) throws ParseException, IOException {
        final List<TargetGene> targetGenes = targetGeneManager.search(geneName, taxId);
        final Set<Long> targetIds = targetGenes.stream().map(TargetGene::getTargetId).collect(Collectors.toSet());
        final String clause = getFilterClause(geneName, taxId, targetIds);
        final SortInfo sortInfo = SortInfo.builder()
                .field(TARGET_NAME)
                .ascending(true)
                .build();
        return targetDao.loadTargets(clause, Collections.singletonList(sortInfo));
    }

    public List<Target> getTargetsForAlignment() {
        return targetDao.loadTargetsForAlignment();
    }

    public List<Target> getTargetsForPatentsSearch() {
        return targetDao.getTargetsForPatentsSearch();
    }

    public List<String> loadFieldValues(final TargetField field) {
        final List<String> loaded = getFieldValues(field);
        final Set<String> values = new HashSet<>(loaded.size());

        return loaded.stream().filter(value -> {
            final String upperCase = value.toUpperCase(Locale.ROOT);
            if (!values.contains(upperCase)) {
                values.add(upperCase);
                return true;
            } else {
                return false;
            }
        }).collect(Collectors.toList());
    }

    public List<String> getTargetGeneNames(final List<String> geneIds) {
        final List<TargetGene> targetGenes = getTargetGenes(geneIds);
        return targetGenes.stream().map(TargetGene::getGeneName).distinct().collect(Collectors.toList());
    }

    public List<Long> getTargetGeneSpecies(final List<String> geneIds) {
        final List<TargetGene> targetGenes = getTargetGenes(geneIds);
        return targetGenes.stream().map(TargetGene::getTaxId).distinct().collect(Collectors.toList());
    }

    public List<TargetGene> getTargetGenes(final List<String> geneIds) {
        final List<String> clauses = new ArrayList<>();
        clauses.add(getGeneIdsClause(geneIds));
        return targetGeneDao.loadTargetGenes(join(clauses, Condition.AND.getValue()));
    }

    private static String getFilterClause(final TargetQueryParams targetQueryParams) {
        final List<String> clauses = new ArrayList<>();
        if (StringUtils.isNotBlank(targetQueryParams.getTargetName())) {
            clauses.add(String.format(LIKE_CLAUSE, TARGET_NAME, targetQueryParams.getTargetName()));
        }
        if (StringUtils.isNotBlank(targetQueryParams.getOwner())) {
            clauses.add(String.format(EQUAL_CLAUSE_STRING, OWNER, targetQueryParams.getOwner()));
        }
        if (!CollectionUtils.isEmpty(targetQueryParams.getProducts())) {
            clauses.add(getListFieldFilterClause(PRODUCTS, targetQueryParams.getProducts()));
        }
        if (!CollectionUtils.isEmpty(targetQueryParams.getDiseases())) {
            clauses.add(getListFieldFilterClause(DISEASES, targetQueryParams.getDiseases()));
        }
        if (!CollectionUtils.isEmpty(targetQueryParams.getGeneIds())) {
            clauses.add(getGeneIdsClause(targetQueryParams.getGeneIds()));
        }
        if (!CollectionUtils.isEmpty(targetQueryParams.getGeneNames())) {
            clauses.add(getFilterClause(GENE_NAME, targetQueryParams.getGeneNames(), LIKE_CLAUSE));
        }
        if (!CollectionUtils.isEmpty(targetQueryParams.getSpeciesNames())) {
            clauses.add(getFilterClause(SPECIES_NAME, targetQueryParams.getSpeciesNames(), LIKE_CLAUSE));
        }
        return join(clauses, Condition.AND.getValue());
    }

    private static String getFilterClause(final String geneName, final Long taxId, final Set<Long> targetIds) {
        List<String> clauses = new ArrayList<>();
        clauses.add(String.format(EQUAL_CLAUSE_STRING, GENE_NAME, geneName));
        if (taxId != null) {
            clauses.add(String.format(EQUAL_CLAUSE_NUMBER, TAX_ID, taxId));
        }
        String clause = join(clauses, Condition.AND.getValue());
        if (!CollectionUtils.isEmpty(targetIds)) {
            clauses = new ArrayList<>();
            clauses.add(clause);
            clauses.add(String.format(IN_CLAUSE, TARGET_ID, join(targetIds, ",")));
            clause = join(clauses, Condition.OR.getValue());
        }
        return clause;
    }

    @NotNull
    private static String getFilterClause(final String field, final List<String> values, final String clause) {
        return String.format("(%s)", values.stream()
                .map(v -> String.format(clause, field, v))
                .collect(Collectors.joining(Condition.OR.getValue())));
    }

    private static String getListFieldFilterClause(final String field, final List<String> values) {
        return String.format("(%s)", values.stream()
                .map(v -> String.format("(%s OR %s OR %s OR %s)",
                        String.format("%s = '%s'", field, v),
                        String.format("%s like '%%,%s,%%'", field, v),
                        String.format("%s like '%s,%%'", field, v),
                        String.format("%s like '%%,%s'", field, v)))
                .collect(Collectors.joining(Condition.OR.getValue())));
    }

    private List<TargetIdentification> getIdentifications(final Set<Long> targetIds) {
        return targetIdentificationDao.load(getInClause("target_id", targetIds));
    }

    private List<String> getFieldValues(TargetField field) {
        if (field.equals(TargetField.SPECIES_NAME)) {
            return targetGeneDao.loadSpeciesNames();
        }
        if (field.equals(TargetField.GENE_NAME)) {
            return targetGeneDao.loadGeneNames();
        }
        final List<Target> targets = targetDao.loadAllTargets();
        if (field.equals(TargetField.PRODUCTS)) {
            return targets.stream().map(Target::getProducts)
                    .flatMap(List::stream).distinct().sorted().collect(Collectors.toList());
        }
        if (field.equals(TargetField.DISEASES)) {
            return targets.stream().map(Target::getDiseases)
                    .flatMap(List::stream).distinct().sorted().collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static List<String> getGenesToDelete(final Target target, final Target oldTarget) {
        final List<String> oldGenes = oldTarget.getTargetGenes().stream()
                .map(g -> g.getGeneId().toLowerCase())
                .collect(Collectors.toList());
        final List<String> newGenes = target.getTargetGenes().stream()
                .map(g -> g.getGeneId().toLowerCase())
                .collect(Collectors.toList());
        return oldGenes.stream()
                .filter(g -> !newGenes.contains(g))
                .collect(Collectors.toList());
    }

    private static boolean isDefault(final Target target) {
        return Optional.ofNullable(target.getType()).orElse(TargetType.DEFAULT) == TargetType.DEFAULT;
    }
}
