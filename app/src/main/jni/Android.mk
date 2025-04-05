LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := preloader
LOCAL_SRC_FILES := preloader.cpp
include $(BUILD_SHARED_LIBRARY)
