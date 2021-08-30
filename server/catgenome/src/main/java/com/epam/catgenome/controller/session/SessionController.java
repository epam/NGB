/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

package com.epam.catgenome.controller.session;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.entity.session.NGBSession;
import com.epam.catgenome.entity.session.NGBSessionFilter;
import com.epam.catgenome.manager.session.NGBSessionSecurityService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;

/**
 * A REST Controller implementation, responsible for handling session related queries
 * </p>
 */
@Controller
@RequiredArgsConstructor
@Api(value = "session", description = "NGB Session Management")
public class SessionController extends AbstractRESTController {

    @Autowired
    private final NGBSessionSecurityService sessionSecurityService;

    @RequestMapping(value = "/session/filter", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
            value = "Returns all sessions matching filters",
            notes = "List all available sessions matching filters",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<List<NGBSession>> filterSessions(@RequestBody(required = false) NGBSessionFilter filter) {
        return Result.success(sessionSecurityService.filter(filter));
    }

    @RequestMapping(value = "/session/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(
        value = "Returns session by id",
        notes = "Returns session by id",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<NGBSession> loadSession(@PathVariable Long id) {
        return Result.success(sessionSecurityService.load(id));
    }

    @RequestMapping(value = "/session/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(
            value = "Deletes a session by given ID",
            notes = "Deletes a session by given ID",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<NGBSession> deleteSession(@PathVariable final Long id) {
        return Result.success(sessionSecurityService.delete(id));
    }

    @RequestMapping(value = "/session", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(
        value = "Creates session",
        notes = "Creates session with given parameters",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
        })
    public Result<NGBSession> createSession(@RequestBody final NGBSession session) {
        return Result.success(sessionSecurityService.create(session));
    }

    @RequestMapping(value = "/session", method = RequestMethod.PUT)
    @ResponseBody
    @ApiOperation(
            value = "Updates session",
            notes = "Updates session with given parameters",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<NGBSession> update(@RequestBody final NGBSession session) {
        return Result.success(sessionSecurityService.update(session));
    }

}
