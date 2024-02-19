/*
 * MIT License
 *
 * Copyright (c) 2016-2023 EPAM Systems
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

package com.epam.catgenome.controller.llm;

import com.epam.catgenome.controller.AbstractRESTController;
import com.epam.catgenome.controller.Result;
import com.epam.catgenome.entity.llm.LLMMessage;
import com.epam.catgenome.entity.llm.LLMProvider;
import com.epam.catgenome.manager.llm.LLMSecurityService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Api(value = "LLM", description = "Access to large language models API")
@RequiredArgsConstructor
public class LLMController extends AbstractRESTController {

    private final LLMSecurityService llmSecurityService;

    @PostMapping(value = "/llm/summary")
    @ApiOperation(
            value = "Returns summary over specified PubMed articles",
            notes = "Returns summary over specified PubMed articles",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<String> getArticleSummary(
            @RequestBody final List<String> pubMedIDs,
            @RequestParam(required = false, defaultValue = "OPENAI_GPT_35") final LLMProvider provider,
            @RequestParam(required = false, defaultValue = "500") final int maxSize,
            @RequestParam(required = false, defaultValue = "0.3") final double temperature) {
        return Result.success(llmSecurityService.getArticleSummary(pubMedIDs, provider, maxSize, temperature));
    }

    @GetMapping(value = "/llm/patent")
    @ApiOperation(
            value = "Returns summary over google patents",
            notes = "Returns summary over google patents",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<String> getPatentSummary(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "OPENAI_GPT_35") final LLMProvider provider,
            @RequestParam(required = false, defaultValue = "500") final int maxSize,
            @RequestParam(required = false, defaultValue = "0.3") final double temperature) {
        return Result.success(llmSecurityService.getPatentSummary(query, provider, maxSize, temperature));
    }

    @PostMapping(value = "/llm/chat")
    @ApiOperation(
            value = "Returns LLM model response for chat",
            notes = "Returns LLM model response for chat",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {@ApiResponse(code = HTTP_STATUS_OK, message = API_STATUS_DESCRIPTION)
            })
    public Result<String> getChatResponse(
            @RequestBody final List<LLMMessage> messages,
            @RequestParam(required = false, defaultValue = "OPENAI_GPT_35") final LLMProvider provider,
            @RequestParam(required = false, defaultValue = "500") final int maxSize,
            @RequestParam(required = false, defaultValue = "0.3") final double temperature) {
        return Result.success(llmSecurityService.getChatResponse(messages, provider, maxSize, temperature));
    }
}
