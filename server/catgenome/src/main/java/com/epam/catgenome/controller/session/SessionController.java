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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
@Tag(name = "session", description = "NGB Session Management")
public class SessionController extends AbstractRESTController {

    @Autowired
    private final NGBSessionSecurityService sessionSecurityService;

    @RequestMapping(value = "/session/filter", method = RequestMethod.POST)
    @ResponseBody
    @Operation(
            summary = "Returns all sessions matching filters",
            description = "List all available sessions matching filters")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<List<NGBSession>> filterSessions(@RequestBody(required = false) NGBSessionFilter filter) {
        return Result.success(sessionSecurityService.filter(filter));
    }

    @RequestMapping(value = "/session/{id}", method = RequestMethod.GET)
    @ResponseBody
    @Operation(
        summary = "Returns session by id",
        description = "Returns session by id")
    @ApiResponses(
        value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
        })
    public Result<NGBSession> loadSession(@PathVariable Long id) {
        return Result.success(sessionSecurityService.load(id));
    }

    @RequestMapping(value = "/session/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    @Operation(
            summary = "Deletes a session by given ID",
            description = "Deletes a session by given ID")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<NGBSession> deleteSession(@PathVariable final Long id) {
        return Result.success(sessionSecurityService.delete(id));
    }

    @RequestMapping(value = "/session", method = RequestMethod.POST)
    @ResponseBody
    @Operation(
        summary = "Creates session",
        description = "Creates session with given parameters")
    @ApiResponses(
        value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
        })
    public Result<NGBSession> createSession(@RequestBody final NGBSession session) {
        return Result.success(sessionSecurityService.create(session));
    }

    @RequestMapping(value = "/session", method = RequestMethod.PUT)
    @ResponseBody
    @Operation(
            summary = "Updates session",
            description = "Updates session with given parameters")
    @ApiResponses(
            value = {@ApiResponse(responseCode = HTTP_STATUS_OK, description = API_STATUS_DESCRIPTION)
            })
    public Result<NGBSession> update(@RequestBody final NGBSession session) {
        return Result.success(sessionSecurityService.update(session));
    }

}
