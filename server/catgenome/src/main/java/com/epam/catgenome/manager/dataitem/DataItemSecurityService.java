/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2018-2022 EPAM Systems
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
import com.epam.catgenome.entity.BiologicalDataItemDownloadUrl;
import com.epam.catgenome.entity.BiologicalDataItemFile;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.security.acl.aspect.AclMaskList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

import static com.epam.catgenome.security.acl.SecurityExpressions.OR;
import static com.epam.catgenome.security.acl.SecurityExpressions.READ_ON_FILTER_OBJECT;
import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_ADMIN;
import static com.epam.catgenome.security.acl.SecurityExpressions.ROLE_USER;

@Service
public class DataItemSecurityService {

    private static final String FILTER_OBJECT_IS_REFERENCE = "filterObject.format.id == 1";
    private static final String RETURN_OBJECT_IS_REFERENCE = "returnObject.format.id == 1";


    @Autowired
    private DataItemManager dataItemManager;

    @AclMaskList
    @PostFilter(ROLE_ADMIN + OR + READ_ON_FILTER_OBJECT + OR + FILTER_OBJECT_IS_REFERENCE)
    public List<BiologicalDataItem> findFilesByName(String name, boolean strict) {
        return dataItemManager.findFilesByName(name, strict);
    }

    @PreAuthorize("isAllowed(#id, 'WRITE')")
    public BiologicalDataItem deleteFileByBioItemId(Long id) throws IOException {
        return dataItemManager.deleteFileByBioItemId(id);
    }

    @PostAuthorize("isAllowed(returnObject, 'WRITE')")
    public void renameFile(final String name, final String newName, final String newPrettyName) {
        dataItemManager.renameFile(name, newName, newPrettyName);
    }

    @PostAuthorize("isAllowed(returnObject, 'READ')" + OR + RETURN_OBJECT_IS_REFERENCE)
    public BiologicalDataItem findFileByBioItemId(Long id) {
        return dataItemManager.findFileByBioItemId(id);
    }

    @PreAuthorize(ROLE_USER)
    public Map<String, BiologicalDataItemFormat> getFormats() {
        return dataItemManager.getFormats();
    }

    @PreAuthorize("isDownloadAllowedByBioItemId(#id)")
    public BiologicalDataItemFile loadItemFile(final Long id, final Boolean source) throws IOException {
        final BiologicalDataItem biologicalDataItem = dataItemManager.findFileByBioItemId(id);
        return dataItemManager.loadItemFile(biologicalDataItem, source);
    }

    @PreAuthorize("isDownloadAllowedByBioItemId(#id)")
    public BiologicalDataItemDownloadUrl generateDownloadUrl(final Long id) throws AccessDeniedException {
        final BiologicalDataItem biologicalDataItem = dataItemManager.findFileByBioItemId(id);
        return dataItemManager.generateDownloadUrl(id, biologicalDataItem);
    }
}
