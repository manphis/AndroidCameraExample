package com.javacodegeeks.androidcameraexample;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private int cameraID;
	private Context mContext;

	private int mWidth = 640;
	private int mHeight = 480;
	private int previewWidth = 0;
	private int previewHeight = 0;
	private static final int PIXEL_FORMAT = ImageFormat.NV21;
	private static final int CAMERA_FPS = 30;
	private CameraFrameCallback mCallback = null;

	private static final String TAG = "CameraExamplePreview";

	public CameraPreview(Context context, Camera camera) {
		super(context);
		Log.i(TAG, "CameraPreview");
		mContext = context;
		mCamera = camera;
		mHolder = getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public CameraPreview(Context context, Camera camera, CameraFrameCallback callback, int width, int height) {
		super(context);
		Log.i(TAG, "CameraPreview");
		mContext = context;
		mCamera = camera;
		mHolder = getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mCallback = callback;

		mWidth = width;
		mHeight = height;
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "surfaceCreated");
		try {
			// create the surface and start camera preview
			if (mCamera == null) {
				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();
			}
		} catch (IOException e) {
			Log.d(VIEW_LOG_TAG, "Error setting camera preview: " + e.getMessage());
		}
	}

	public void refreshCamera(Camera camera, int cameraID) {
		Log.i(TAG, "refreshCamera");
		if (mHolder.getSurface() == null) {
			// preview surface does not exist
			Log.i(TAG, "preview surface does not exist");
			return;
		}
		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			// ignore: tried to stop a non-existent preview
		}
		// set preview size and make any resize, rotate or
		// reformatting changes here
		// start preview with new settings
		setCamera(camera, cameraID);
		try {
			mCamera.setPreviewDisplay(mHolder);


			Camera.Parameters params = mCamera.getParameters();

			Camera.Size defaultSize = params.getPreviewSize();
			Log.i(TAG, "default preview size = " + defaultSize.width + " x " + defaultSize.height);

			float mRatio = (float)mHeight/(float)mWidth;
			float diffRatio = 1f;

			List<Camera.Size> sizes = params.getSupportedPreviewSizes();
			for (int i = sizes.size()-1; i >= 0; i--) {
				Camera.Size size = sizes.get(i);
				Log.i(TAG, "supported preview size = " + size.width + " x " + size.height);

//				float ratio = (float)size.height/(float)size.width;
//				float diff = Math.abs(ratio - mRatio);
//				if (size.height >= mWidth && diff < diffRatio) {
//					previewWidth = size.width;
//					previewHeight = size.height;
//					diffRatio = diff;
//				}

				if (size.height >= mWidth) {
					previewWidth = size.width;
					previewHeight = size.height;
					break;
				}
			}
			Log.i(TAG, "selected preview size = " + previewWidth + " x " + previewHeight);
			params.setPreviewFormat(PIXEL_FORMAT);
//			params.setPreviewSize(mWidth, mHeight);
			params.setPreviewSize(previewWidth, previewHeight);
			params.setPreviewFpsRange(CAMERA_FPS*1000, CAMERA_FPS*1000);
			params.setPreviewFrameRate(CAMERA_FPS);

			mCamera.setParameters(params);
			setCameraDisplayOrientation((Activity)mContext, cameraID, mCamera);


			Camera.Size settingSize = mCamera.getParameters().getPreviewSize();
			Log.i(TAG, "set preview size = " + settingSize.width + " x " + settingSize.height);

			mCamera.setPreviewCallback(this);

			mCamera.startPreview();
		} catch (Exception e) {
			Log.d(VIEW_LOG_TAG, "Error starting camera preview: " + e.getMessage());
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		Log.i(TAG, "surfaceChanged");
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.
		refreshCamera(mCamera, cameraID);
	}

	public void setCamera(Camera camera, int id) {
		//method to set a camera instance
		mCamera = camera;
		cameraID = id;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		// mCamera.release();

	}

	public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation) {
			case Surface.ROTATION_0:
				degrees = 0;
				break;
			case Surface.ROTATION_90:
				degrees = 90;
				break;
			case Surface.ROTATION_180:
				degrees = 180;
				break;
			case Surface.ROTATION_270:
				degrees = 270;
				break;
		}

		int result;
		//int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		// do something for phones running an SDK before lollipop
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}

		camera.setDisplayOrientation(result);
	}

	@Override
	public void onPreviewFrame(byte[] bytes, Camera camera) {
		byte[] cropedData = null;
		cropedData = rotateAndCropFrame(bytes);
//		sendToEncode(cropedData);

		if (null != mCallback)
			mCallback.onFrame(cropedData);
	}

	public interface CameraFrameCallback {
		public void onFrame(byte[] frame);
	}


	private int mRotationDegree = 270;
	private byte[] rotateAndCropFrame(final byte[] frame_data) {
//		Log.i(TAG, "data length = " + frame_data.length);
//		if (frame_data.length != mWidth*mHeight*3/2){
//			return null;
//		}
		if (frame_data.length != previewWidth*previewHeight*3/2){
			return null;
		}

		int reallength = mWidth*mHeight*3/2;//320*240*3/2;
		byte[] rotatedCropData = new byte[reallength];
//		int cameraheight = mHeight;
//		int camerawidth  = mWidth;
		int cameraheight = previewHeight;
		int camerawidth  = previewWidth;
		int camerauv     = cameraheight*camerawidth;

		int realheight   = mHeight;//240;
		int realwidth    = mWidth;//320;
		int realuv       = realwidth*realheight;
		int diffWidth    = camerawidth - realheight;
		int diffHeight   = cameraheight - realwidth;

		if (mRotationDegree == 270 /*|| true*/) {
			for (int y = camerawidth*diffHeight/2,j = 0; y < (cameraheight-diffHeight/2)*camerawidth ; y+=camerawidth,j++){
				for (int offsetx= realheight * realwidth,z=diffWidth/2; offsetx > 0 ;offsetx-=realwidth,z++){
					rotatedCropData[offsetx - realwidth + j ] =  frame_data[(z + y)];
				}
			} //Y
			for (int y = (camerawidth*diffHeight/4),j = 0; y < ((cameraheight/2-diffHeight/4)*camerawidth); y+=camerawidth,j+=2){
				for (int offsetx= realheight * realwidth /2,z=diffWidth/2; offsetx >0 ;offsetx-=realwidth,z+=2){
					rotatedCropData[realuv+(offsetx - realwidth + j )] = frame_data[camerauv+(z + y)];
					rotatedCropData[realuv+(offsetx - realwidth + j )+1] = frame_data[camerauv+(z + y)+1];
				}
			}//UV
		} else if (mRotationDegree == 0) {
//			if (camerawidth == 320 && cameraheight == 240) {
			if (camerawidth == mWidth && cameraheight == mHeight) {
				System.arraycopy(frame_data, 0, rotatedCropData, 0, reallength);
			} else {
				diffWidth    = camerawidth - realwidth;
				diffHeight   = cameraheight - realheight;
				for (int y = camerawidth*diffHeight/2,j = 0; y < (cameraheight-diffHeight/2)*camerawidth ; y+=camerawidth,j+=realwidth){
					for (int offsetx= 0,z=diffWidth/2; offsetx < realwidth ;offsetx++,z++){
						rotatedCropData[offsetx + j ] =  frame_data[(z + y)];
					}
				} //Y
				for (int y = camerawidth*diffHeight/4,j = 0; y < (cameraheight/2-diffHeight/4)*camerawidth ; y+=camerawidth,j+=realwidth){
					for (int offsetx= 0,z=diffWidth/2; offsetx < realwidth ;offsetx+=2,z+=2){
						rotatedCropData[realuv+(offsetx + j )] = frame_data[camerauv+(z + y)];
						rotatedCropData[realuv+(offsetx + j )+1] = frame_data[camerauv+(z + y)+1];
					}
				}//UV
			}
		} else if (mRotationDegree == 180) {
			diffWidth    = camerawidth - realwidth;
			diffHeight   = cameraheight - realheight;
			for (int y = camerawidth*diffHeight/2,j = realwidth*realheight-1; y < (cameraheight-diffHeight/2)*camerawidth ; y+=camerawidth,j-=realwidth){
				for (int offsetx= realwidth,z=diffWidth/2; offsetx > 0 ;offsetx--,z++){
					rotatedCropData[offsetx - realwidth + j ] =  frame_data[(z + y)];
				}
			} //Y
			for (int y = camerawidth*diffHeight/4,j = (realwidth*realheight/2)-1; y < (cameraheight/2-diffHeight/4)*camerawidth ; y+=camerawidth,j-=realwidth){
				for (int offsetx= (realwidth),z=diffWidth/2; offsetx > 0 ;offsetx-=2,z+=2){
					rotatedCropData[realuv+(offsetx - realwidth + j ) -1] = frame_data[camerauv+(z + y)];
					rotatedCropData[realuv+(offsetx - realwidth + j )] = frame_data[camerauv+(z + y)+1];
				}
			}//UV
		} else {
			for (int y = camerawidth*diffHeight/2,j = 0; y < (cameraheight-diffHeight/2)*camerawidth ; y+=camerawidth,j++){
				for (int offsetx= 0,z=diffWidth/2; offsetx < realheight * realwidth ;offsetx+=realwidth,z++){
					rotatedCropData[offsetx + realwidth-1 - j ] =  frame_data[(z + y)];
				}
			} //Y
			for (int y = (camerawidth*diffHeight/4),j = 0; y < ((cameraheight/2-diffHeight/4)*camerawidth); y+=camerawidth,j+=2){
				for (int offsetx= 0,z=diffWidth/2; offsetx < realheight * realwidth /2 ;offsetx+=realwidth,z+=2){
					rotatedCropData[realuv+(offsetx + realwidth-1 - j )] = frame_data[camerauv+(z + y)+1];
					rotatedCropData[realuv+(offsetx + realwidth-1 - j )-1] = frame_data[camerauv+(z + y)];
				}
			}//UV

		}

		return rotatedCropData;
	}
}