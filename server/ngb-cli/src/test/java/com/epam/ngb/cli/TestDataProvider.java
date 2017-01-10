/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

package com.epam.ngb.cli;

import com.epam.ngb.cli.entity.*;
import com.epam.ngb.cli.manager.JsonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public final class TestDataProvider {

    private static final Long DEFAULT_USER = 42L;
    private static final Date DEFAULT_DATE = getDefaultDate();
    private static final JsonMapper MAPPER = JsonMapper.getMapper();
    private static final String FILE_TYPE = "FILE";

    private TestDataProvider() {
        //no operations
    }

    public static String getFilePayloadJson(Long id, Long bioItemId, BiologicalDataItemFormat format,
            String path, String name) {
        return getFilePayloadJson(id, bioItemId, format, path, name, false);
    }

    public static String getFilePayloadJson(Long id, Long bioItemId, BiologicalDataItemFormat format,
            String path, String name, boolean wrapInList) {
        BiologicalDataItem item = getBioItem(id, bioItemId, format, path, name);
        if (wrapInList) {
            return getPayloadJson(Collections.singletonList(item));
        } else {
            return getPayloadJson(item);
        }
    }

    public static BiologicalDataItem getBioItem(Long id, Long bioItemId, BiologicalDataItemFormat format,
            String path, String name) {
        BiologicalDataItem item = new BiologicalDataItem();
        item.setId(id);
        item.setBioDataItemId(bioItemId);
        item.setCreatedBy(DEFAULT_USER);
        item.setCreatedDate(DEFAULT_DATE);
        item.setType(FILE_TYPE);
        item.setFormat(format);
        item.setPath(path);
        item.setName(name);
        return item;
    }

    public static String getProjectPayloadJson(Long id, String name, List<BiologicalDataItem> items) {
        Project project =getProject(id, name, items);
        return getPayloadJson(project);

    }

    public static String getRegistrationJson(Long referenceID, String path, String name,
            String indexPath, List<ProjectItem> items) {
        RegistrationRequest request = new RegistrationRequest();
        request.setReferenceId(referenceID);
        request.setPath(path);
        request.setName(name);
        request.setIndexPath(indexPath);
        request.setItems(items);
        return getJson(request);
    }

    public static String getRegistrationJson(Long referenceID, String path, String name,
                                             String indexPath, boolean doIndex) {
        RegistrationRequest request = new RegistrationRequest();
        request.setReferenceId(referenceID);
        request.setPath(path);
        request.setName(name);
        request.setIndexPath(indexPath);
        request.setDoIndex(doIndex);
        return getJson(request);
    }

    public static String getJson(Object item) {
        try {
            return MAPPER.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("Illegal object to serialize to json.");
    }

    protected static <T> String getPayloadJson(T item){
        ResponseResult<T> result = new ResponseResult<>();
        result.setStatus("OK");
        result.setPayload(item);
        return getJson(result);
    }

    private static Date getDefaultDate() {
        try {
            return new SimpleDateFormat("yyyy-MM-dd")
                    .parse("2016-06-08");
        } catch (ParseException e) {
            return new Date();
        }
    }

    public static Project getProject(Long id, String name,
            List<BiologicalDataItem> items) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setCreatedBy(DEFAULT_USER);
        project.setCreatedDate(DEFAULT_DATE);
        project.setLastOpenedDate(DEFAULT_DATE);
        project.setItems(items);
        return project;
    }
}
