REAL_LOCAL_PATH := $(call my-dir)
LOCAL_PATH :=

include $(CLEAR_VARS)

LOCAL_MODULE := Filter


# jni

LOCAL_SRC_FILES:= $(REAL_LOCAL_PATH)/Filter.cpp

LOCAL_C_INCLUDES += \
  $(REAL_LOCAL_PATH) \
  $(LEPTONICA_PATH)/src

LOCAL_LDLIBS += \
  -lGLESv1_CM \
  -ljnigraphics \
  -llog \

# common
LOCAL_PRELINK_MODULE := false
LOCAL_SHARED_LIBRARIES := liblept


include $(BUILD_SHARED_LIBRARY)