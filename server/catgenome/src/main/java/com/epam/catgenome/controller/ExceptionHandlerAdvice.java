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

import java.io.FileNotFoundException;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedClientException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.epam.catgenome.component.MessageHelper;

/**
 * Source:      ExceptionHandlerAdvice.java
 * Created:     10/2/15, 4:16 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code ExceptionHandlerAdvice} is the advice for all application controllers required to handle any
 * exceptions, which were thrown during the execution of any methods of any application controller.
 * <p>
 * Here any {@code Throwable}, which could be encountered any time when workflow fails due to
 * incorrect data, forbidden or/and ambiguous situations will be processed too.
 */
@ControllerAdvice
public class ExceptionHandlerAdvice extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandlerAdvice.class);

    @ResponseBody
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(Throwable.class)
    public final ResponseEntity<Result<String>> handleUncaughtException(final Throwable exception, final WebRequest
            request) {
        // adds information about encountered error to application log
        LOG.error(MessageHelper.getMessage("logger.error", request.getDescription(true)), exception);
        HttpStatus code = HttpStatus.OK;

        String message;
        if (exception instanceof FileNotFoundException) {
            // any details about real path of a resource should be normally prevented to send to the client
            message = MessageHelper.getMessage("error.io.not.found");
        } else if (exception instanceof DataAccessException) {
            // any details about data access error should be normally prevented to send to the client,
            // as its message can contain information about failed SQL query or/and database schema
            if (exception instanceof BadSqlGrammarException) {
                // for convenience we need to provide detailed information about occurred BadSqlGrammarException,
                // but it can be retrieved
                SQLException root = ((BadSqlGrammarException) exception).getSQLException();
                if (root.getNextException() != null) {
                    LOG.error(MessageHelper.getMessage("logger.error.root.cause", request.getDescription(true)),
                        root.getNextException());
                }
                message = MessageHelper.getMessage("error.sql.bad.grammar");
            } else {
                message = MessageHelper.getMessage("error.sql");
            }
        } else if (exception instanceof UnauthorizedClientException) {
            message = exception.getMessage();
            code = HttpStatus.UNAUTHORIZED;
        } else {
            message = exception.getMessage();
        }

        return new ResponseEntity<>(Result.error(StringUtils.defaultString(StringUtils.trimToNull(message),
                                       MessageHelper.getMessage("error" + ".default"))), code);
    }
}
