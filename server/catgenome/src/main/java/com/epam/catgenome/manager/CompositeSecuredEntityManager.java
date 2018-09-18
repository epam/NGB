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

package com.epam.catgenome.manager;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.security.AbstractSecuredEntity;
import com.epam.catgenome.entity.security.AclClass;

@Service
public class CompositeSecuredEntityManager {
    private Map<AclClass, SecuredEntityManager> managers;

    @Autowired(required = false) // TODO: fix
    public void setManagers(List<SecuredEntityManager> managers) {
        if (CollectionUtils.isEmpty(managers)) {
            this.managers = new EnumMap<>(AclClass.class);
        } else {
            this.managers = managers
                .stream()
                .collect(Collectors.toMap(
                    SecuredEntityManager::getSupportedClass, Function.identity()));
        }
    }

    public SecuredEntityManager getEntityManager(AclClass aclClass) {
        if (!managers.containsKey(aclClass)) {
            throw new IllegalArgumentException(MessageHelper.getMessage(MessagesConstants.ERROR_ACL_CLASS_NOT_SUPPORTED, aclClass));
        }
        return managers.get(aclClass);
    }

    public SecuredEntityManager getEntityManager(AbstractSecuredEntity entity) {
        return getEntityManager(entity.getAclClass());
    }

    public AbstractSecuredEntity load(AclClass aclClass, Long id) {
        return getEntityManager(aclClass).load(id);
    }

    public AbstractSecuredEntity loadByNameOrId(AclClass aclClass, String identifier) {
        return getEntityManager(aclClass).loadByNameOrId(identifier);
    }

    public AbstractSecuredEntity changeOwner(AclClass aclClass, Long id, String owner) {
        return getEntityManager(aclClass).changeOwner(id, owner);
    }

    public Integer loadTotalCount(AclClass aclClass) {
        return getEntityManager(aclClass).loadTotalCount();
    }

    public Collection<? extends AbstractSecuredEntity> loadAllWithParents(
        AclClass aclClass, Integer page, Integer pageSize) {
        return getEntityManager(aclClass).loadAllWithParents(page, pageSize);
    }
}
