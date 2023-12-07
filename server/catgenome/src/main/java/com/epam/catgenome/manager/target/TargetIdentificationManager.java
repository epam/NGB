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
import com.epam.catgenome.dao.target.TargetIdentificationDao;
import com.epam.catgenome.entity.target.*;
import com.epam.catgenome.manager.AuthManager;
import com.epam.catgenome.util.db.Condition;
import com.epam.catgenome.util.db.Page;
import com.epam.catgenome.util.db.SortInfo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.util.Utils.*;
import static org.apache.commons.lang3.StringUtils.join;

@Service
@RequiredArgsConstructor
public class TargetIdentificationManager {

    private static final String NAME = "name";
    private static final String OWNER = "owner";
    private static final String GENES_OF_INTEREST = "genes_of_interest";
    private static final String TRANSLATIONAL_GENES = "translational_genes";
    private static final String TARGET_ID = "target_id";
    private final TargetIdentificationDao targetIdentificationDao;
    private final AuthManager authManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public TargetIdentification create(final TargetIdentification identification) {
        if (StringUtils.isEmpty(identification.getOwner())) {
            identification.setOwner(authManager.getAuthorizedUser());
        }
        identification.setCreatedDate(new Date());
        return targetIdentificationDao.save(identification);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public TargetIdentification update(TargetIdentification identification) {
        getIdentification(identification.getId());
        return targetIdentificationDao.save(identification);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(final long id) {
        getIdentification(id);
        targetIdentificationDao.delete(id);
    }

    public TargetIdentification getIdentification(final long id) {
        final TargetIdentification identification = load(id);
        Assert.notNull(identification, getMessage(MessagesConstants.ERROR_TARGET_IDENTIFICATION_NOT_FOUND, id));
        return identification;
    }

    public TargetIdentification load(final long id) {
        return targetIdentificationDao.load(id);
    }
    
    public Page<TargetIdentification> load(final IdentificationQueryParams params) {
        final String clause = getFilterClause(params);
        final long totalCount = targetIdentificationDao.getTotalCount(clause);
        final SortInfo sortInfo = params.getSortInfo() != null ?
                params.getSortInfo() : SortInfo.builder()
                .field(NAME)
                .ascending(true)
                .build();
        final List<TargetIdentification> identifications = targetIdentificationDao.load(clause,
                params.getPagingInfo(),
                Collections.singletonList(sortInfo));
        return Page.<TargetIdentification>builder()
                .totalCount(totalCount)
                .items(identifications)
                .build();
    }

    public List<TargetIdentification> load() {
        return targetIdentificationDao.load();
    }

    public List<TargetIdentification> loadTargetIdentifications(final long targetId) {
        return targetIdentificationDao.loadTargetIdentifications(targetId);
    }

    private static String getFilterClause(final IdentificationQueryParams params) {
        final List<String> clauses = new ArrayList<>();
        if (StringUtils.isNotBlank(params.getName())) {
            clauses.add(String.format(LIKE_CLAUSE, NAME, params.getName()));
        }
        if (params.getTargetId() != null) {
            clauses.add(String.format(EQUAL_CLAUSE_NUMBER, TARGET_ID, params.getTargetId()));
        }
        if (StringUtils.isNotBlank(params.getOwner())) {
            clauses.add(String.format(EQUAL_CLAUSE_STRING, OWNER, params.getOwner()));
        }
        if (!CollectionUtils.isEmpty(params.getGeneIds())) {
            clauses.add(getGeneIdsFilterClause(params.getGeneIds()));
        }
        return join(clauses, Condition.AND.getValue());
    }

    private static String getGeneIdsFilterClause(final List<String> geneIds) {
        final List<String> clauses = new ArrayList<>();
        clauses.add(getFilterClause(GENES_OF_INTEREST, geneIds, LIKE_CLAUSE));
        clauses.add(getFilterClause(TRANSLATIONAL_GENES, geneIds, LIKE_CLAUSE));
        return join(clauses, Condition.OR.getValue());
    }

    @NotNull
    private static String getFilterClause(final String field, final List<String> values, final String clause) {
        return String.format("(%s)", values.stream()
                .map(v -> String.format(clause, field, v))
                .collect(Collectors.joining(Condition.OR.getValue())));
    }
}
