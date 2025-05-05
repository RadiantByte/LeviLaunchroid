package org.levimc.launcher.util;

public class Logger implements AutoCloseable {
    private long nativeLoggerPtr;

    private static class Holder {
        private static final Logger INSTANCE = new Logger("LeviMC");
    }

    public static Logger get() {
        return Holder.INSTANCE;
    }

    private Logger(String loggerName) {
        nativeLoggerPtr = nativeCreateLogger(loggerName);
    }

    public static void release() {
        Holder.INSTANCE.close();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    private native long nativeCreateLogger(String name);

    private native void nativeDestroyLogger(long nativeLoggerPtr);

    private native void nativeInfo(long nativeLoggerPtr, String msg);

    private native void nativeError(long nativeLoggerPtr, String msg);

    private native void nativeWarn(long nativeLoggerPtr, String msg);

    private native void nativeDebug(long nativeLoggerPtr, String msg);

    public void info(String msg, Object... args) {
        if (nativeLoggerPtr != 0)
            nativeInfo(nativeLoggerPtr, format(msg, args));
    }

    public void error(String msg, Object... args) {
        if (nativeLoggerPtr != 0)
            nativeError(nativeLoggerPtr, format(msg, args));
    }

    public void warn(String msg, Object... args) {
        if (nativeLoggerPtr != 0)
            nativeWarn(nativeLoggerPtr, format(msg, args));
    }

    public void debug(String msg, Object... args) {
        if (nativeLoggerPtr != 0)
            nativeDebug(nativeLoggerPtr, format(msg, args));
    }

    @Override
    public void close() {
        if (nativeLoggerPtr != 0) {
            nativeDestroyLogger(nativeLoggerPtr);
            nativeLoggerPtr = 0;
        }
    }

    private String format(String msg, Object... args) {
        try {
            return args == null || args.length == 0 ? msg : String.format(msg, args);
        } catch (Exception e) {
            return msg;
        }
    }
}