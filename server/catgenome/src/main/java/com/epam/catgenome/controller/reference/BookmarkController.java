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

package com.epam.catgenome.controller.reference;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.controller.vo.BookmarkVO;
import com.epam.catgenome.controller.vo.converter.BookmarkConverter;
import com.epam.catgenome.manager.reference.BookmarkManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

/**
 * {@code BookmarkController} represents implementation of MVC controller which handles
 * requests to manage data about bookmarks on the reference genomes.
 * <p>
 * It's designed to communicate with corresponded managers that provide all required
 * calls and manage all operations concerned with bookmarks.
 */
@Controller
@Api(value = "bookmarks", description = "Bookmarks Management")
public class BookmarkController extends AbstractRESTController {
    @Autowired
    private BookmarkManager bookmarkManager;

    @RequestMapping(value = "/bookmarks", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
            value = "Loads all bookmarks for current user",
            notes = "Bookmarks provide without data on tracks, that vas opened when a bookmark was saved",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<BookmarkVO>> loadBookmarks() {
        return Result.success(BookmarkConverter.convertTo(bookmarkManager.loadBookmarksByProject()));
    }

    @RequestMapping(value = "/bookmark/{bookmarkId}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
            value = "Returns bookmark, specified by id",
            notes = "Bookmarks provide data on tracks, that vas opened when a bookmark was saved",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<BookmarkVO> loadBookmark(@PathVariable(value = "bookmarkId") final Long bookmarkId) {
        return Result.success(BookmarkConverter.convertTo(bookmarkManager.loadBookmark(bookmarkId)));
    }


    @RequestMapping(value = "/bookmark/save", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Saves bookmark for a specific chromosome for a given project",
            notes = "Bookmarks provide data on tracks, that vas opened when a bookmark was saved",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<BookmarkVO> saveBookmark(@RequestBody final BookmarkVO bookmarkVO) throws IOException {
        return Result.success(BookmarkConverter.convertTo(bookmarkManager.saveBookmark(BookmarkConverter.convertFrom(
                bookmarkVO))));
    }

    @RequestMapping(value = "/bookmark/{bookmarkId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Result<Boolean> deleteBookmark(@PathVariable(value = "bookmarkId") final Long bookmarkId) {
        bookmarkManager.deleteBookmark(bookmarkId);
        return Result.success(true);
    }
}
