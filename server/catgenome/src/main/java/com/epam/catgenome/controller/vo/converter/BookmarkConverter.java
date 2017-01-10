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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.epam.catgenome.controller.vo.BookmarkItemVO;
import com.epam.catgenome.controller.vo.BookmarkVO;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.reference.Bookmark;

/**
 * Util class for converting Bookmark VO into the Bookmark entity
 */
public final class BookmarkConverter {
    private BookmarkConverter() {
        // no-op
    }

    /**
     * Converts a Bookmark into BookmarkVO
     * @param bookmark bookmark
     * @return BookmarkVO converted instance
     */
    public static BookmarkVO convertTo(Bookmark bookmark) {
        BookmarkVO vo = new BookmarkVO();

        vo.setId(bookmark.getId());
        vo.setName(bookmark.getName());
        vo.setStartIndex(bookmark.getStartIndex());
        vo.setEndIndex(bookmark.getEndIndex());
        vo.setChromosome(bookmark.getChromosome());

        if (bookmark.getOpenedItems() != null) {
            vo.setOpenedItems(convertTo(bookmark.getOpenedItems()));
        }

        return vo;
    }

    /**
     * Converts a {@code List} of Bookmark into a {@code List} BookmarkVO
     * @param bookmarks bookmarks to convert
     * @return BookmarkVO converted instances
     */
    public static List<BookmarkVO> convertTo(List<Bookmark> bookmarks) {
        return bookmarks.stream().map(BookmarkConverter::convertTo).collect(Collectors.toList());
    }

    /**
     * Converts a BookmarkVO into Bookmark
     * @param vo BookmarkVO
     * @return Bookmark
     */
    public static Bookmark convertFrom(BookmarkVO vo) {
        Bookmark bookmark = new Bookmark();

        bookmark.setId(vo.getId());
        bookmark.setName(vo.getName());
        bookmark.setStartIndex(vo.getStartIndex());
        bookmark.setEndIndex(vo.getEndIndex());
        bookmark.setChromosome(vo.getChromosome());

        if (vo.getOpenedItems() != null) {
            bookmark.setOpenedItems(convertFrom(vo.getOpenedItems()));
        }

        return bookmark;
    }

    /**
     * Converts a {@code List} of BookmarkVO into a {@code List} Bookmark
     * @param vos views to convert
     * @return BookmarkVO converted instances
     */
    public static List<Bookmark> convertFrom(List<BookmarkVO> vos) {
        return vos.stream().map(BookmarkConverter::convertFrom).collect(Collectors.toList());
    }

    public static BookmarkItemVO convertTo(final BiologicalDataItem item) {
        BookmarkItemVO vo = new BookmarkItemVO();

        vo.setBioDataItemId(BiologicalDataItem.getBioDataItemId(item));
        vo.setName(item.getName());
        vo.setType(item.getType());
        vo.setFormat(item.getFormat());
        vo.setPath(item.getPath());
        vo.setCreatedBy(item.getCreatedBy());
        vo.setCreatedDate(item.getCreatedDate());

        vo.setId(item.getId());

        if (item instanceof FeatureFile) {
            FeatureFile featureFile = (FeatureFile) item;
            vo.setReferenceId(featureFile.getReferenceId());
            vo.setCompressed(featureFile.getCompressed());
        }

        return vo;
    }

    public static BiologicalDataItem convertFrom(final BookmarkItemVO vo) {
        BiologicalDataItem bioDataItem = new BiologicalDataItem();
        bioDataItem.setId(vo.getBioDataItemId());
        bioDataItem.setName(vo.getName());
        bioDataItem.setPath(vo.getPath());
        bioDataItem.setFormat(vo.getFormat());
        bioDataItem.setType(vo.getType());
        bioDataItem.setCreatedBy(vo.getCreatedBy());
        bioDataItem.setCreatedDate(vo.getCreatedDate());

        return bioDataItem;
    }

    public static List<BiologicalDataItem> convertFrom(Collection<BookmarkItemVO> items) {
        return items.stream().map(BookmarkConverter::convertFrom).collect(Collectors.toList());
    }

    public static List<BookmarkItemVO> convertTo(Collection<BiologicalDataItem> items) {
        return items.stream().map(BookmarkConverter::convertTo).collect(Collectors.toList());
    }
}
