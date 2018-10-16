/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2018 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.epam.catgenome.manager.dataitem;


import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.security.acl.aspect.AclFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
public class DataItemSecurityService {

    @Autowired
    private DataItemManager dataItemManager;


    @AclFilter
    public List<BiologicalDataItem> findFilesByName(String name, boolean strict) {
        return dataItemManager.findFilesByName(name, strict);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @PostAuthorize("isAllowed(returnObject, 'WRITE')")
    public BiologicalDataItem deleteFileByBioItemId(Long id) throws IOException {
        return dataItemManager.deleteFileByBioItemId(id);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @PostAuthorize("isAllowed(returnObject, 'WRITE')")
    public BiologicalDataItem findFileByBioItemId(Long id) {
        return dataItemManager.findFileByBioItemId(id);
    }
}
