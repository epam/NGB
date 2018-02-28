package com.epam.catgenome.util.aws;



import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * This class contains the plugins's executor services.
 */
public class ExecutorsFactory {

    private static final int RESERVED_FOR_TASK_PRODUCER = 1;

    public static ScheduledExecutorService getDaemonScheduledExecutorService(String name) {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName(name);
            t.setDaemon(true);
            return t;
        });
    }

    static ExecutorService getTasksExecutor() {
        return Executors.newFixedThreadPool(
                Configuration.getNumberOfConnections() + RESERVED_FOR_TASK_PRODUCER
        );
    }
}
