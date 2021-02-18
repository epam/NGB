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

package com.epam.catgenome.controller;

import com.wordnik.swagger.annotations.ApiModelProperty;
import lombok.NoArgsConstructor;

/**
 * Source:      Result.java
 * Created:     10/2/15, 3:24 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code Result} represents a wrapper value object used to transfer result of any performed
 * call to the client, including status of performed operation, info/error message and attached
 * payload.
 *
 * @author Denis Medvedev
 */

@NoArgsConstructor
public final class Result<T> {

    private T payload;

    private String message;

    private ResultStatus status;

    private Result(final ResultStatus status, final T payload) {
        this(status, null, payload);
    }

    private Result(final ResultStatus status, final String message, final T payload) {
        this.status = status;
        this.message = message;
        this.payload = payload;
    }

    /**
     * @return Payload of a service response
     */
    public T getPayload() {
        return payload;
    }

    /**
     * @return Message of a service response
     */
    public String getMessage() {
        return message;
    }

    @ApiModelProperty(value = "it defines the status with which an operation may result in",
        allowableValues = "OK, INFO, WARN, ERROR", required = true)
    public ResultStatus getStatus() {
        return status;
    }

    /**
     * Create a Result object with status {@code ResultStatus.OK} and desired payload
     * @param payload the payload of a Result object
     * @param <T> the class of the payload
     * @return a new Result object with status OK
     */
    public static <T> Result<T> success(final T payload) {
        return new Result<>(ResultStatus.OK, payload);
    }

    /**
     * Create a Result object with status {@code ResultStatus.OK}, desired payload and message
     * @param payload payload of a Result object
     * @param message message of a Result object
     * @param <T> the class of the payload
     * @return a new Result object with status OK
     */
    public static <T> Result<T> success(final T payload, final String message) {
        return new Result<>(ResultStatus.OK, message, payload);
    }

    /**
     * Create a Result object with status {@code ResultStatus.ERROR} and a desired message
     * @param message message of a Result object
     * @param <T> class of the payload
     * @return  a new Result object with status ERROR
     */
    public static <T> Result<T> error(final String message) {
        return result(ResultStatus.ERROR, message, null);
    }

    /**
     * Create a Result object with status {@code ResultStatus.INFO}, desired payload and message
     * @param payload payload of a Result object
     * @param message message of a Result object
     * @param <T> class of the payload
     * @return  a new Result object with status INFO
     */
    public static <T> Result<T> info(final String message, final T payload) {
        return result(ResultStatus.INFO, message, payload);
    }

    /**
     * Create a Result object with status {@code ResultStatus.WARN}, desired payload and message
     * @param payload payload of a Result object
     * @param message message of a Result object
     * @param <T> class of the payload
     * @return  a new Result object with status WARN
     */
    public static <T> Result<T> warn(final String message, final T payload) {
        return result(ResultStatus.WARN, message, payload);
    }

    /**
     * Create a Result object with status {@code ResultStatus.ERROR}, desired payload and message
     * @param payload payload of a Result object
     * @param message message of a Result object
     * @param <T> class of the payload
     * @return  a new Result object with status ERROR
     */
    public static <T> Result<T> error(final String message, final T payload) {
        return result(ResultStatus.ERROR, message, payload);
    }

    /**
     * Create a Result object with desired status, payload and message
     * @param status status of a Result object
     * @param payload payload of a Result object
     * @param message message of a Result object
     * @param <T> class of the payload
     * @return  a new Result object with status INFO
     */
    public static <T> Result<T> result(final ResultStatus status, final String message, final T payload) {
        return new Result<>(status, message, payload);
    }
}
