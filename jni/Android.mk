LEPTONICA_PATH := /Users/renard/Desktop/devel/leptonica-1.68
LIBJPEG_PATH := /Users/renard/Desktop/devel/libjpeg

ifeq "$(LIBJPEG_PATH)" ""
  $(error You must set the LIBJPEG_PATH variable to the Android JPEG \
          source directory. See README and jni/Android.mk for details)
endif

ifeq "$(LEPTONICA_PATH)" ""
  $(error You must set the LEPTONICA_PATH variable to the leptonica \
          source directory. See README and jni/Android.mk for details)
endif

# Just build the Android.mk files in the subdirs
include $(call all-subdir-makefiles) $(LIBJPEG_PATH)/Android.mk
