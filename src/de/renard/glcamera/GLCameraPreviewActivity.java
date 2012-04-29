package de.renard.glcamera;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

public class GLCameraPreviewActivity extends Activity implements PreviewCallback, Runnable {

	public final static int IMAGE_W = 640;
	public final static int IMAGE_H = 480;

	private GLSurfaceView mGlSurfaceView;
	private ExecutorService mExecutor = Executors.newSingleThreadExecutor();

	private Map<Integer, CameraInfo> mCameras = new HashMap<Integer, CameraInfo>();
	private byte[] mData = null;
	Camera mCamera;
	private int mCurrentCameraId;
	private CameraPreviewRenderer mRenderer;
	private de.renard.glcamera.ImageFilter mImageProcessing;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mGlSurfaceView = new GLSurfaceView(this);

		mRenderer = new CameraPreviewRenderer(IMAGE_W, IMAGE_H, getApplicationContext());
		mGlSurfaceView.setRenderer(mRenderer);
		mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		mGlSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR);
		setContentView(mGlSurfaceView);

		findCameras();
	}

	public void findCameras() {
		// Find the total number of cameras available
		int numberOfCameras = Camera.getNumberOfCameras();
		// Find the ID of the default camera
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo cameraInfo = new CameraInfo();
			Camera.getCameraInfo(i, cameraInfo);
			mCameras.put(i, cameraInfo);
			if (mCurrentCameraId == -1 && cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
				mCurrentCameraId = i;
			}
		}
		if (mCurrentCameraId == -1 && numberOfCameras > 0) {
			mCurrentCameraId = 0;
		}
	}

	public void switchCamera() {
		if (mCurrentCameraId < mCameras.size() - 1) {
			mCurrentCameraId++;
		} else {
			mCurrentCameraId = 0;
		}
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
		}
		mCamera = Camera.open(mCurrentCameraId);
		startPreview();
	}

	private void startPreview() {
		if (mCamera != null) {
			mCamera.stopPreview();
			try {
				SurfaceView view = new SurfaceView(this);
				mCamera.setPreviewDisplay(view.getHolder());
			} catch (IOException e) {
				e.printStackTrace();
			}
			mCamera.setPreviewCallbackWithBuffer(this);

			Camera.Parameters parameters = mCamera.getParameters();
			// List<Size> previewSizes = parameters.getSupportedPreviewSizes();

			parameters.setPreviewSize(IMAGE_W, IMAGE_H);

			if (Integer.parseInt(Build.VERSION.SDK) >= 8)
				setDisplayOrientation(mCamera, 90);
			else {
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
					parameters.set("orientation", "portrait");
					parameters.set("rotation", 90);
				}
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
					parameters.set("orientation", "landscape");
					parameters.set("rotation", 90);
				}
			}

			mCamera.setParameters(parameters);
			final Size previewSize = mCamera.getParameters().getPreviewSize();
			final int previewFormat = mCamera.getParameters().getPreviewFormat();
			final int bitsPerPixel = ImageFormat.getBitsPerPixel(previewFormat);
			int bufSize = (previewSize.width * previewSize.height * bitsPerPixel) / 8;

			mImageProcessing = ImageFilter.getInstance(IMAGE_W, IMAGE_H);
			mRenderer.setImageFilter(mImageProcessing);

			mCamera.addCallbackBuffer(new byte[bufSize]);
			mCamera.startPreview();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
		mGlSurfaceView.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mCamera = Camera.open();
		startPreview();
		mGlSurfaceView.onResume();
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (mCamera == null) {
			return;
		}
		mData = data;
		mExecutor.execute(this);
	}

	/**
	 * async processing of frames
	 */
	public void run() {
		if (mData == null) {
			return;
		}
		mImageProcessing.processFrame(mData);
		mGlSurfaceView.requestRender();

		if (mCamera != null) {
			mCamera.addCallbackBuffer(mData);
		}
	}

	protected void setDisplayOrientation(Camera camera, int angle) {
		Method downPolymorphic;
		try {
			downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[] { int.class });
			if (downPolymorphic != null)
				downPolymorphic.invoke(camera, new Object[] { angle });
		} catch (Exception e1) {
		}
	}
}