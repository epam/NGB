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

package com.epam.catgenome.manager.parallel;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.epam.catgenome.manager.bam.BamTrackEmitter;
import htsjdk.samtools.util.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Created: 2/29/2016
 * Project: CATGenome Browser
 *
 * <p>
 * Represents class, that contains fixed thread pool to execute different parallel task through Genome Browser.
 * </p>
 */
@Service
public final class TaskExecutorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutorService.class);

    private static final long DEFAULT_KEEP_ALIVE_TIME = 60L;
    private static final long DEFAULT_MAX_THREADS = 20;

    @Value("#{catgenome['ngb.bam.streaming.thread.keep-alive'] ?: " + DEFAULT_KEEP_ALIVE_TIME + "}")
    private int keepAliveTime;

    @Value("#{catgenome['server.tomcat.max-connections'] ?: " + DEFAULT_MAX_THREADS + "}")
    private int maxThreadCount;

    public enum ExecutionMode {
        SEQUENTIAL, ASYNC
    }

    //for testing sequential execution is forced
    private volatile boolean forceSequential = false;

    private volatile ExecutorService executorService;

    /**
     * Provides executor service to run runnable/callable tasks.
     *
     * @return ExecutorService
     */
    public ExecutorService getExecutorService() {
        ExecutorService instance = executorService;
        if (instance == null) {
            synchronized (this) {
                instance = executorService;
                if (instance == null) {
                    instance = createExecutorService();
                    executorService = instance;
                }
            }
        }
        return instance;
    }

    public synchronized void executeTrackTask(BamTrackEmitter bamTrackEmitter, ExecutionMode mode, BamTrackTask task)
            throws IOException {

        Executor executor = (mode == ExecutionMode.SEQUENTIAL || forceSequential) ?
                (Runnable::run) :
                getExecutorService();

        CompletableFuture.runAsync(
            () -> {
                try {
                    task.run();
                } catch (IOException e) {
                    throw new RuntimeIOException(e);
                }
            }, executor).exceptionally(
                e -> {
                    bamTrackEmitter.finishWithException(e);
                    return null;
                }
        );
    }

    public int getTaskNumberOfThreads() {
        return Runtime.getRuntime().availableProcessors() / 2 <= maxThreadCount ?
                (Runtime.getRuntime().availableProcessors() / 2) :
                maxThreadCount;
    }

    /**
     * Return fixed thread pool with size of available processors in system.
     *
     * @return ExecutorService
     */
    private ExecutorService createExecutorService() {
        ThreadPoolExecutor threadPoolExecutor =
                new ThreadPoolExecutor(0, maxThreadCount, keepAliveTime, TimeUnit.SECONDS, new SynchronousQueue<>());
        LOGGER.info("Create cached thread pool with max capacity {}", maxThreadCount);
        return threadPoolExecutor;
    }

    public synchronized void setForceSequential(boolean force) {
        forceSequential = force;
    }
}
