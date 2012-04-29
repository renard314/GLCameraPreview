#include <string.h>
#include <jni.h>
#include <allheaders.h>
#include "de_renard_glcamera_ImageFilter.h"
#include "common.h"
#include <GLES/gl.h>

#ifdef __cplusplus
extern "C" {
#endif

int width, height;
Pix* mPreviewPix32 = NULL;
Pix* mPix8Black = NULL;

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
	JNIEnv *env;

	if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
		LOGE("Failed to get the environment using GetEnv()");
		return -1;
	}

	return JNI_VERSION_1_6;
}

static void checkGlError(const char* op) {
	for (GLint error = glGetError(); error; error = glGetError()) {
		LOGE("after %s() glError (0x%x)\n", op, error);
	}
}
static inline void yuvToPixFast(unsigned char* pY) {
	int i, j;
	int nR, nG, nB;
	int nY, nU, nV;
	l_uint32* data = pixGetData(mPreviewPix32);

	unsigned char* pUV = pY + width * height;

	for (i = 0; i < height; i++) {
		nU = 0;
		nV = 0;
		unsigned char* uvp = pUV + (i >> 1) * width;

		for (j = 0; j < width; j++) {

			if ((j & 1) == 0) {
				nV = (0xff & *uvp++) - 128;
				nU = (0xff & *uvp++) - 128;
			}
			// Yuv Convert
			nY = *(pY++);
			nY -= -16;

			if (nY < 0) {
				nY = 0;
			}
			int y1192 = nY * 1192;

			nB = y1192 + 2066 * nU;
			nG = y1192 - 833 * nV - 400 * nU;
			nR = y1192 + 1634 * nV;

			if (nR < 0) {
				nR = 0;
			} else if (nR > 262143) {
				nR = 262143;
			}
			if (nG < 0) {
				nG = 0;
			} else if (nG > 262143) {
				nG = 262143;
			}
			if (nB < 0) {
				nB = 0;
			} else if (nB > 262143) {
				nB = 262143;
			}
			*data++ = ((nR << 14) & 0xff000000) | ((nG << 6) & 0xff0000) | ((nB >> 2) & 0xff00) | (0xff);
		}
	}
}

JNIEXPORT void JNICALL Java_de_renard_glcamera_ImageFilter_nativeUploadTexture(JNIEnv *, jobject javathis, jint textureName) {
	GLuint texture = (GLuint) textureName;
	glBindTexture(GL_TEXTURE_2D, textureName);
	l_uint32* data= pixGetData(mPreviewPix32);

	glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, (GLvoid*)data);
	checkGlError("checkGlError");

}

JNIEXPORT void JNICALL Java_de_renard_glcamera_ImageFilter_nativeProcessImage(JNIEnv *env, jobject javathis, jbyteArray frame) {
	//LOGV(__FUNCTION__);

	jbyte *data_buffer = env->GetByteArrayElements(frame, NULL);
	l_uint8 *byte_buffer = (l_uint8 *) data_buffer;
	yuvToPixFast(byte_buffer);

	pixSetRGBComponent(mPreviewPix32,mPix8Black,L_ALPHA_CHANNEL);
	pixEndianByteSwap(mPreviewPix32);

	env->ReleaseByteArrayElements(frame, data_buffer, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_de_renard_glcamera_ImageFilter_nativeStart(JNIEnv *env, jobject javathis, jint w, jint h) {
	LOGV(__FUNCTION__);
	width = w;
	height = h;

	if (mPreviewPix32!=NULL){
		pixDestroy(&mPreviewPix32);
	}
	if (mPix8Black!=NULL){
		pixDestroy(&mPix8Black);
	}
	mPix8Black = pixCreate(w,h,8);
	mPreviewPix32 = pixCreate(w, h, 32);
	pixSetAllArbitrary(mPix8Black,255);

}

JNIEXPORT void JNICALL Java_de_renard_glcamera_ImageFilter_nativeEnd(JNIEnv *env, jobject javathis) {
	LOGV(__FUNCTION__);
	if (mPreviewPix32!=NULL){
		pixDestroy(&mPreviewPix32);
	}
}


#ifdef __cplusplus
}
#endif
