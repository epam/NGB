/*
 * MIT License
 *
 * Copyright (c) 2018 EPAM Systems
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

package com.epam.catgenome.entity.security;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * {@link AbstractHierarchicalEntity} class represents a tree of entities' hierarchy.
 * In contrast to {@link AbstractSecuredEntity}, which is a single entity with a link to higher
 * hierarchy level via {@code getParent()} method, this entity has collections of children (sub-trees)
 * and leaves - references to a lower hierarchy entities.
 * Main goal of this class is to provide filtering and setting permissions mask for the whole hierarchy of entities.
 * {@link AbstractHierarchicalEntity} is filtered and post processed in {@code AclAspect}
 * for methods returning {@link AbstractSecuredEntity} marked with annotation {@code AclMask}.
 * The overall approach to filtering of :
 * -    children and leaves inherit permissions from a higher hierarchy entities
 * -    if permissions are set for a certain entity, they override any inherited permissions
 * -    if user has permission to view child entry in hierarchy, than all higher hierarchy levels should be
 *      also visible to this user.
 *      In this case the visibility of entry's neighbours depends on their own permissions.
 */
public abstract class AbstractHierarchicalEntity extends AbstractSecuredEntity {

    public AbstractHierarchicalEntity() {
        super();
    }

    public AbstractHierarchicalEntity(Long id) {
        super(id);
    }

    public AbstractHierarchicalEntity(Long id, String name) {
        super(id, name);
    }

    /**
     * @return a list of child entities, that are leaves in the hierarchy - they cannot have children itself.
     */
    @JsonIgnore
    public abstract List<? extends AbstractSecuredEntity> getLeaves();

    /**
     * @return a list of child entities, that <b>can</b> have children themselves - {@link AbstractHierarchicalEntity}
     */
    @JsonIgnore
    public abstract List<? extends AbstractHierarchicalEntity> getChildren();

    /**
     * Method filters out child leaf entities that matched {@param idToRemove}. Mainly used to remove
     * entities without permissions
     * @param idToRemove map, specifying which entities should be removed
     */
    public abstract void filterLeaves(Map<AclClass, Set<Long>> idToRemove);

    /**
     * Method filters out child tree entities that matched {@param idToRemove}. Mainly used to remove
     * entities without permissions
     * @param idToRemove map, specifying which entities should be removed
     */
    public abstract void filterChildren(Map<AclClass, Set<Long>> idToRemove);

    /**
     * Since {@link AbstractSecuredEntity} maybe available to user even without any permissions,
     * in case of some present permission in the lower layers of hierarchy, we may want to clear
     * some fields before returning such 'readOnly' entity to the client.
     */
    public void clearForReadOnlyView() {
        // no operations by default
    }
}
