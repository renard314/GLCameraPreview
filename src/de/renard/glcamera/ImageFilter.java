package de.renard.glcamera;

import android.util.Log;


public class ImageFilter {
	private final static String DEBUG_TAG = ImageFilter.class.getSimpleName();
	static {
		System.loadLibrary("lept");
		System.loadLibrary("Filter");
	}

	private int mOrientation = 0;
	private static ImageFilter mInstance=null;
	
	public static ImageFilter getInstance(int width, int height){
		if (mInstance!=null){
			mInstance.close();
		}
		mInstance = new ImageFilter(width, height);
		return mInstance;
	}
	
	private ImageFilter(int width, int height) {
		Log.i(DEBUG_TAG,"width = "+width +" height = "+height);
		nativeStart(width, height);
	}

	public void processFrame(final byte[] frame) {
		nativeProcessImage(frame);
	}

	private void close() {
		nativeEnd();
	}

	public native void nativeUploadTexture(int textureName);
	
	private native void nativeProcessImage(byte[] frame);
	
	private native void nativeStart(int w, int h);

	private native void nativeEnd();

}
