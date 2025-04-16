package org.levimc.launcher.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppExecutors {
    private static final ExecutorService diskIO = Executors.newSingleThreadExecutor();
    private static final ExecutorService networkIO = Executors.newFixedThreadPool(3);

    public static ExecutorService diskIO() {
        return diskIO;
    }

    public static ExecutorService networkIO() {
        return networkIO;
    }
}