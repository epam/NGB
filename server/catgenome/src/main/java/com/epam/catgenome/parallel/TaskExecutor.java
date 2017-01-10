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

package com.epam.catgenome.parallel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created: 2/29/2016
 * Project: CATGenome Browser
 *
 * <p>
 * Represents class, that contains fixed thread pool to execute different parallel task through Genome Browser.
 * </p>
 */
public final class TaskExecutor {

    private static volatile ExecutorService executorService;
    private static int numberOfThreads;
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutor.class);

    /**
     * Provides executor service to run runnable/callable tasks.
     *
     * @return ExecutorService
     */
    public static ExecutorService getExecutorService() {
        ExecutorService instance = executorService;
        if (instance == null) {
            synchronized (TaskExecutor.class) {
                instance = executorService;
                if (instance == null) {
                    instance = createExecutorService();
                    executorService = instance;
                }
            }
        }
        return instance;
    }

    public static int getNumberOfThreads() {
        getExecutorService(); // Executor service must be initialized
        return numberOfThreads;
    }

    /**
     * Return fixed thread pool with size of available processors in system.
     *
     * @return ExecutorService
     */
    private static ExecutorService createExecutorService() {
        numberOfThreads = Runtime.getRuntime().availableProcessors();
        LOGGER.info("Create FixedThreadPool with capacity {}", numberOfThreads);
        return Executors.newFixedThreadPool(numberOfThreads);
    }

    private TaskExecutor() {
        // no-op
    }
}
