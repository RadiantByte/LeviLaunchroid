package org.levimc.launcher.gui;

public class NativeMethods {
    static {
        System.loadLibrary("leviutils");
    }

    public static native void onDrawFrame();
    public static native void onSurfaceCreated();
    public static native void onSurfaceChanged(int width,int height);
    public static native void onDetachedFromWindow();
    public static native void SetWindowSize(int w,int h);
    public static native void MotionEventClick(boolean down,float PosX,float PosY);
    public static native String GetWindowRect();
    public static native void AddLog(String str);
    public static native void SetOpen(boolean isopen);

}