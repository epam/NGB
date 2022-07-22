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

package com.epam.catgenome.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.UrlRequestVO;
import com.epam.catgenome.entity.security.SessionExpirationBehavior;
import com.epam.catgenome.manager.UrlShorterManager;
import com.epam.catgenome.util.IndexUtils;
import com.epam.catgenome.entity.UrlWithAliasItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.epam.catgenome.controller.vo.FilesVO;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.manager.FileManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import static com.epam.catgenome.component.MessageHelper.getMessage;

/**
 * Source:      UtilsController
 * Created:     25.04.16, 16:11
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A REST controller to supply some common util data
 * </p>
 */
@Controller
public class UtilsController extends AbstractRESTController {

    @Autowired
    private FileManager fileManager;

    @Autowired
    private UrlShorterManager urlShorterManager;

    @Autowired
    private BiologicalDataItemManager biologicalDataItemManager;

    @Value("#{catgenome['version']}")
    private String version;

    @Value("${security.acl.enable: false}")
    private boolean aclSecurityEnabled;

    @Value("${session.expiration.behavior: CONFIRM}")
    private SessionExpirationBehavior expirationBehavior;

    @ResponseBody
    @RequestMapping(value = "/version", method = RequestMethod.GET)
    @ApiOperation(
            value = "Returns application's version",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<String> loadVersion() {
        return Result.success(version);
    }

    @ResponseBody
    @RequestMapping(value = "/isRoleModelEnabled", method = RequestMethod.GET)
    @ApiOperation(
            value = "Returns application's version",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> isRoleModelEnabled() {
        return Result.success(aclSecurityEnabled);
    }

    @ResponseBody
    @RequestMapping(value = "/sessionExpirationBehavior", method = RequestMethod.GET)
    @ApiOperation(
            value = "Returns application's version",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<SessionExpirationBehavior> sessionExpirationBehaviour() {
        return Result.success(expirationBehavior);
    }

    @ResponseBody
    @RequestMapping(value = "/files", method = RequestMethod.GET)
    @ApiOperation(
        value = "Returns directory contents",
        notes = "Returns directory contents, specified by path",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<FilesVO> loadDirectoryContents(@RequestParam(required = false) String path)
        throws IOException {
        return Result.success(new FilesVO(fileManager.loadDirectoryContents(path), fileManager.getNgsDataRootPath()));
    }

    @ResponseBody
    @RequestMapping(value = "/files/allowed", method = RequestMethod.GET)
    @ApiOperation(
            value = "Checks is directory browsing is allowed",
            notes = "Returns true if directory browsing is allowed and false if not",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> isFilesBrowsingAllowed()
            throws IOException {
        return Result.success(fileManager.isFilesBrowsingAllowed());
    }

    @ResponseBody
    @RequestMapping(value = "/url", method = RequestMethod.POST)
    @ApiOperation(
        value = "Generates URL postfix",
        notes = "Generates URL that displays specified files, optionally on specified interval",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<String> generateUrl(
                    @RequestBody UrlRequestVO request,
                    @RequestParam(required = false) String chromosomeName,
                    @RequestParam(required = false) Integer startIndex,
                    @RequestParam(required = false) Integer endIndex) throws JsonProcessingException {
        return Result.success(biologicalDataItemManager.generateUrl(request.getDataset(),
                request.getIds() == null ? Collections.emptyList() : request.getIds(), chromosomeName,
                startIndex, endIndex));
    }

    @ResponseBody
    @RequestMapping(value = "/urls/allowed", method = RequestMethod.GET)
    @ApiOperation(
            value = "Checks if url file browsing is allowed",
            notes = "Returns true if url file browsing is allowed and false if not",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Boolean> isUrlsBrowsingAllowed()
            throws IOException {
        return Result.success(fileManager.isUrlsBrowsingAllowed());
    }

    @ResponseBody
    @RequestMapping(value = "/generateShortUrl", method = RequestMethod.POST)
    @ApiOperation(
            value = "Generates short URL postfix",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<String> generateShortUrl(@RequestBody UrlWithAliasItem urlWithAlias) {
        String alias = urlWithAlias.getAlias();
        String url = urlWithAlias.getUrl();

        String payload = urlShorterManager.generateAndSaveShortUrlPostfix(url, alias);

        Result<String> result;
        if (alias != null && !alias.equals(payload)) {
            result = Result.success(payload, getMessage(MessagesConstants.INFO_ALIAS_ALREADY_EXIST_MASSAGE));
        } else {
            result = Result.success(payload);
        }
        return result;
    }

    @ResponseBody
    @RequestMapping(value = "/navigate", method = RequestMethod.GET)
    @ApiOperation(
            value = "redirect on a original URL by short URL postfix, or on the 404 if short url doesn't exist",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public void redirectToOriginalUrlByAlias(@RequestParam String alias, HttpServletResponse resp) throws IOException {
        Optional<String> maybeOriginalUrl = urlShorterManager.getOriginalUrl(alias);
        if (maybeOriginalUrl.isPresent()) {
            String url = maybeOriginalUrl.get();
            resp.addHeader("Location", url);
            resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            resp.sendRedirect(url);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, MessagesConstants.ERROR_URL_WAS_EXPIRED);
        }
    }

    @ResponseBody
    @RequestMapping(value = "/defaultTrackSettings", method = RequestMethod.GET)
    @ApiOperation(
            value = "Return default track settings",
            notes = "Return default track settings, which specified in catgenome.properties file",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<Map<String, Map<String, Object>>> getDefaultTracksSettings() throws IOException {
        return Result.success(fileManager.getDefaultTrackSettings());
    }

    @ResponseBody
    @RequestMapping(value = "/getPathToExistingIndex", method = RequestMethod.GET)
    @ApiOperation(value = "Return path of existing index for file, or null if it don't exist",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)})
    public Result<String> getPathToExistingIndex(@RequestParam String filePath) throws IOException {
        return Result.success(IndexUtils.checkExistingIndex(filePath));
    }
}
