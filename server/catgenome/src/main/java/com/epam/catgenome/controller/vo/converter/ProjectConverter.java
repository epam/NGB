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

package com.epam.catgenome.controller.vo.converter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.epam.catgenome.controller.vo.ProjectItemVO;
import com.epam.catgenome.controller.vo.ProjectVO;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.project.ProjectItem;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.vcf.VcfFile;


/**
 * Source:      ProjectItemConverter
 * Created:     22.01.16, 15:37
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * Converts Project entity objects into Project VO objects and vice versa
 * </p>
 */
public final class ProjectConverter {
    private ProjectConverter() {
        // no-op
    }

    /**
     * Converts {@code Project} entity into {@code ProjectVO}
     * @param project {@code Project}
     * @return {@code ProjectVO}
     */
    public static ProjectVO convertTo(final Project project) {
        ProjectVO vo = new ProjectVO();

        vo.setId(project.getId());
        vo.setName(project.getName());
        vo.setCreatedBy(project.getCreatedBy());
        vo.setCreatedDate(project.getCreatedDate());
        vo.setLastOpenedDate(project.getLastOpenedDate());
        vo.setItemsCount(project.getItemsCount());
        vo.setItemsCountPerFormat(project.getItemsCountPerFormat());
        vo.setParentId(project.getParentId());
        vo.setPrettyName(project.getPrettyName());

        if (project.getNestedProjects() != null) {
            vo.setNestedProjects(convertTo(project.getNestedProjects()));
        }

        if (project.getItems() != null) {
            vo.setItems(project.getItems().stream().map(ProjectConverter::convertTo).collect(Collectors.toList()));
        }

        return vo;
    }

    /**
     * Converts a {@code List} of {@code Project} entities into {@code List} of {@code ProjectVO}
     * @param projects a {@code List} of {@code Project} entities
     * @return {@code List} of {@code ProjectVO}
     */
    public static List<ProjectVO> convertTo(List<Project> projects) {
        if (CollectionUtils.isEmpty(projects)) {
            return Collections.emptyList();
        }

        return projects.stream().map(ProjectConverter::convertTo).collect(Collectors.toList());
    }

    /**
     * Converts {@code ProjectItem} entity into {@code ProjectItemVO}
     * @param item {@code ProjectItem}
     * @return {@code ProjectItemVO}
     */
    public static ProjectItemVO convertTo(final ProjectItem item) {
        ProjectItemVO vo = new ProjectItemVO();

        vo.setHidden(item.getHidden());
        vo.setOrdinalNumber(item.getOrdinalNumber());

        if (item.getBioDataItem() != null) {
            vo.setBioDataItemId(getBioDataItemId(item.getBioDataItem()));
            vo.setName(item.getBioDataItem().getName());
            vo.setType(item.getBioDataItem().getType());
            vo.setFormat(item.getBioDataItem().getFormat());
            vo.setPath(item.getBioDataItem().getPath());
            vo.setCreatedBy(item.getBioDataItem().getCreatedBy());
            vo.setCreatedDate(item.getBioDataItem().getCreatedDate());
            vo.setPrettyName(item.getBioDataItem().getPrettyName());

            vo.setId(item.getBioDataItem().getId());

            if (item.getBioDataItem() instanceof FeatureFile) {
                FeatureFile featureFile = (FeatureFile) item.getBioDataItem();
                vo.setReferenceId(featureFile.getReferenceId());
                vo.setCompressed(featureFile.getCompressed());
            }

            if (item.getBioDataItem() instanceof VcfFile) {
                vo.setSamples(((VcfFile) item.getBioDataItem()).getSamples());
            }
        }

        return vo;
    }

    /**
     * Converts {@code ProjectVO} object into {@code Project} entity
     * @param vo {@code ProjectVO}
     * @return {@code Project}
     */
    public static Project convertFrom(final ProjectVO vo) {
        Project project = new Project();

        project.setId(vo.getId());
        project.setName(vo.getName());
        project.setCreatedBy(vo.getCreatedBy());
        project.setCreatedDate(vo.getCreatedDate());
        project.setItemsCount(vo.getItemsCount());
        project.setItemsCountPerFormat(vo.getItemsCountPerFormat());
        project.setPrettyName(vo.getPrettyName());

        if (vo.getItems() != null) {
            project.setItems(vo.getItems().stream().map(ProjectConverter::convertFrom).collect(Collectors.toList()));
        }

        return project;
    }

    /**
     * Converts {@code ProjectItemVO} object into {@code ProjectItem} entity
     * @param vo {@code ProjectItemVO}
     * @return {@code ProjectItem}
     */
    public static ProjectItem convertFrom(final ProjectItemVO vo) {
        ProjectItem item = new ProjectItem();

        item.setHidden(vo.getHidden());
        item.setOrdinalNumber(vo.getOrdinalNumber());

        BiologicalDataItem bioDataItem = new BiologicalDataItem();
        bioDataItem.setId(vo.getBioDataItemId());
        bioDataItem.setName(vo.getName());
        bioDataItem.setPath(vo.getPath());
        bioDataItem.setFormat(vo.getFormat());
        bioDataItem.setType(vo.getType());
        bioDataItem.setCreatedBy(vo.getCreatedBy());
        bioDataItem.setCreatedDate(vo.getCreatedDate());
        bioDataItem.setPrettyName(vo.getPrettyName());

        item.setBioDataItem(bioDataItem);

        return item;
    }

    private static Long getBioDataItemId(BiologicalDataItem item) {
        if (item instanceof FeatureFile) {
            return ((FeatureFile) item).getBioDataItemId();
        } else {
            if (item instanceof Reference) {
                return ((Reference) item).getBioDataItemId();
            } else {
                return item.getId();
            }
        }
    }
}
