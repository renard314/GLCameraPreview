GLCameraPreview
===============

Barebones app which renders the camera preview into a OpenGL texture. Conversion is done with native C++.

**Needs leptonica-1.68 and libjpeg***

add the path to those libraries to jni/Android.mk

**only compiles with NDK r6**
makefiles are broken for NDK r7 because r7 uses only relatives paths