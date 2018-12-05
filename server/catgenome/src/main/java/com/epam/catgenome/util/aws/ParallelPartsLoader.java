package com.epam.catgenome.util.aws;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.amazonaws.services.s3.AmazonS3URI;
import htsjdk.samtools.util.Log;

/**
 * A class for parallel parts downloading. It produces a task for each file part,
 * submits them to a queue, collects the results in a correct order and gives them on request.
 */
class ParallelPartsLoader implements Runnable {

    private static final Log LOG = Log.getInstance(ParallelPartsLoader.class);
    public static final int CAPACITY_BUFFER_COEFFICIENT = 3;
    public static final byte[] EOF = new byte[0];

    private final AtomicBoolean canceledFlag = new AtomicBoolean(false);

    private final BlockingQueue<Future<Optional<byte[]>>> tasksQueue;
    private final ExecutorService threadPool;
    private final AmazonS3URI uri;
    private final long from;
    private final long to;

    ParallelPartsLoader(AmazonS3URI uri, long from, long to) {
        this(uri, from, to, new ArrayBlockingQueue<>(1));
    }

    ParallelPartsLoader(AmazonS3URI uri, long from, long to,
                        BlockingQueue<Future<Optional<byte[]>>> tasksQueue) {
        this.threadPool = Executors.newFixedThreadPool(2);
        this.from = from;
        this.to = to;
        this.uri = uri;
        this.tasksQueue = tasksQueue;
        threadPool.execute(this);
    }

    @Override public void run() {
        Thread.currentThread().setName("Parallel Parts Loader");
        try {
            produceTasks();
            putEndTasksSignal();
        } catch (InterruptedException e) {
            LOG.error(e, "Thread was interrupt during the producing of tasks for ", uri.toString());
            Thread.currentThread().interrupt();
            emergencyCancelLoading();
        }

        LOG.debug("Exit, all tasks were completed for ", uri.toString());
    }

    /**
     * This method returns next part.
     *
     * @return byte[], part of loaded file.
     */
    byte[] fetchNextPart() {
        try {
            LOG.debug("New task was get from queue.");
            return tasksQueue.take().get().orElse(EOF);
        } catch (ExecutionException | InterruptedException e) {
            LOG.error(e, "Unable to restore data stream");
            return EOF;
        }
    }

    private void produceTasks() throws InterruptedException {
        int downlPartSize = Configuration.getMinDownloadPartSize();
        for (long curPosition = from; curPosition < to; ) {
            if (canceledFlag.get()) {
                LOG.debug("Canceled ", uri.toString());
                break;
            }

            long destPosition = Math.min(to, curPosition + downlPartSize);
            tasksQueue.put(submitTask(curPosition, destPosition));
            LOG.debug("Submit task with position:" + " " + "[" + curPosition + " - " + destPosition
                    + "] for ", uri.toString());
            curPosition = destPosition;
        }
    }

    private void putEndTasksSignal() throws InterruptedException {
        if (!canceledFlag.get()) {
            //poisoned task, to show that no more tasks shell be presented
            tasksQueue.put(threadPool.submit(() -> {
                LOG.debug("future poison");
                return Optional.empty();
            }));
        }
    }

    private Future<Optional<byte[]>> submitTask(long currentPosition, long destPosition) {
        PartReader task = new PartReader(uri, currentPosition, destPosition, canceledFlag);
        return threadPool.submit(task);
    }

    /**
     * This method terminates work with the current resource.
     * Sets canceled flag true, clears queue of tasks and shutdowns the executor.
     */
    void cancelLoading() {
        canceledFlag.set(true);
        tasksQueue.clear();
        threadPool.shutdown();
        LOG.debug("Thread pool was shut down for ", uri.toString());
    }

    private void emergencyCancelLoading() {
        threadPool.shutdownNow();
    }
}
