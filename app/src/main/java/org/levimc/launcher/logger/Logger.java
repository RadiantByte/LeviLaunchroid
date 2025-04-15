package org.levimc.launcher.logger;

public class Logger implements AutoCloseable {

    private long nativeLoggerPtr;

    static {
        System.loadLibrary("leviutils");
    }

    public Logger(String loggerName){
        nativeLoggerPtr = nativeCreateLogger(loggerName);
    }

    public void release(){
        nativeDestroyLogger(nativeLoggerPtr);
        nativeLoggerPtr = 0;
    }

    @Override
    protected void finalize() throws Throwable {
        if(nativeLoggerPtr != 0){
            release();
        }
        super.finalize();
    }

    private native long nativeCreateLogger(String name);

    private native void nativeDestroyLogger(long nativeLoggerPtr);

    private native void nativeInfo(long nativeLoggerPtr, String msg);

    private native void nativeError(long nativeLoggerPtr, String msg);

    private native void nativeWarn(long nativeLoggerPtr, String msg);

    private native void nativeDebug(long nativeLoggerPtr, String msg);


    public void info(String msg, Object...args){
        nativeInfo(nativeLoggerPtr, String.format(msg, args));
    }

    public void error(String msg, Object...args){
        nativeError(nativeLoggerPtr, String.format(msg, args));
    }

    public void warn(String msg, Object...args){
        nativeWarn(nativeLoggerPtr, String.format(msg, args));
    }

    public void debug(String msg, Object...args){
        nativeDebug(nativeLoggerPtr, String.format(msg, args));
    }

    @Override
    public void close() {
        if(nativeLoggerPtr != 0){
            nativeDestroyLogger(nativeLoggerPtr);
            nativeLoggerPtr = 0;
        }
    }

}