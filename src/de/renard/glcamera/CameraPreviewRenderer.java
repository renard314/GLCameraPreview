package de.renard.glcamera;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import android.content.Context;
import android.graphics.Point;
import android.graphics.RectF;
import android.opengl.GLSurfaceView.Renderer;

public class CameraPreviewRenderer implements Renderer {

	private int[] mTextureNameWorkspace = new int[1];
	private ImageFilter mImageFilter;
	private RectF mPreviewRect = new RectF();
	private final Point mPreviewSize = new Point();

	GLSprite mCameraPreviewSprite = new GLSprite();

	public CameraPreviewRenderer(final int width, final int height, final Context c) {
		int w = width;
		int h = height;
		if (!isPowerOf2(w)) {
			w = nextPowerOf2(w);
		}
		if (!isPowerOf2(h)) {
			h = nextPowerOf2(h);
		}

		mCameraPreviewSprite.textureHeight = h;
		mCameraPreviewSprite.textureWidth = w;
		mPreviewSize.set(width, height);

		final Grid backgroundGrid = new Grid(2, 2, false);
		mCameraPreviewSprite.setGrid(backgroundGrid);
	}

	public void setImageFilter(ImageFilter filter) {
		mImageFilter = filter;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

		gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
		gl.glShadeModel(GL10.GL_FLAT);
		gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glDisable(GL10.GL_DITHER);
		gl.glDisable(GL10.GL_LIGHTING);

		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glGenTextures(1, mTextureNameWorkspace, 0);

		int textureName = mTextureNameWorkspace[0];
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textureName);

		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

		gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);

		final int internalFormat = GL10.GL_RGBA;
		final int type = GL10.GL_UNSIGNED_BYTE;
		gl.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, mCameraPreviewSprite.textureWidth, mCameraPreviewSprite.textureHeight, 0, internalFormat, type, (ByteBuffer) null);

		mCameraPreviewSprite.setTextureName(textureName);
	}

	private void findBestFittingRect(final int width, final int height) {
		final float previewAspect = (float) mPreviewSize.x / mPreviewSize.y;
		final float screenAspect = (float) width / height;
		final float w;
		final float h;
		final float x;
		final float y;
		if (previewAspect > screenAspect) {
			w = width;
			h = width / previewAspect;
		} else {
			h = height;
			w = height * previewAspect;
		}
		x = Math.abs(w - width) / 2;
		y = Math.abs(h - height) / 2;
		mPreviewRect.set(x, y, x + w, y + h);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		findBestFittingRect(width, height);
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthof(0.0f, width, 0.0f, height, 0.0f, 1.0f);

		gl.glShadeModel(GL10.GL_FLAT);
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glColor4x(0x10000, 0x10000, 0x10000, 0x10000);
		gl.glEnable(GL10.GL_TEXTURE_2D);

		final Grid backgroundGrid = mCameraPreviewSprite.getGrid();

		backgroundGrid.invalidateHardwareBuffers();
		mCameraPreviewSprite.width = mPreviewRect.width();
		mCameraPreviewSprite.height = mPreviewRect.height();

		final float v = ((float) mPreviewSize.y) / mCameraPreviewSprite.textureHeight;
		final float u = ((float) mPreviewSize.x) / mCameraPreviewSprite.textureWidth;
		backgroundGrid.set(0, 0, 0.0f, 0.0f, 0.0f, 0.0f, v, null);
		backgroundGrid.set(1, 0, mCameraPreviewSprite.width, 0.0f, 0.0f, u, v, null);
		backgroundGrid.set(0, 1, 0.0f, mCameraPreviewSprite.height, 0.0f, 0.0f, 0.0f, null);
		backgroundGrid.set(1, 1, mCameraPreviewSprite.width, mCameraPreviewSprite.height, 0.0f, u, 0.0f, null);

		backgroundGrid.generateHardwareBuffers(gl);

		mCameraPreviewSprite.y = mPreviewRect.top;
		mCameraPreviewSprite.x = mPreviewRect.left;
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		mImageFilter.nativeUploadTexture(mCameraPreviewSprite.getTextureName());

		Grid.beginDrawing(gl, true, false);
		mCameraPreviewSprite.draw(gl);
		Grid.endDrawing(gl);
	}

	/**
	 * Called when the rendering thread shuts down. This is a good place to
	 * release OpenGL ES resources.
	 * 
	 * @param gl
	 */
	public void shutdown(GL10 gl) {
		int[] textureToDelete = new int[1];
		textureToDelete[0] = mCameraPreviewSprite.getTextureName();
		gl.glDeleteTextures(1, textureToDelete, 0);
		mCameraPreviewSprite.setTextureName(0);
		mCameraPreviewSprite.getGrid().releaseHardwareBuffers(gl);
	}

	public static int nextPowerOf2(int n) {
		n -= 1;
		n |= n >>> 16;
		n |= n >>> 8;
		n |= n >>> 4;
		n |= n >>> 2;
		n |= n >>> 1;
		return n + 1;
	}

	public static boolean isPowerOf2(int n) {
		return (n & -n) == n;
	}

}
