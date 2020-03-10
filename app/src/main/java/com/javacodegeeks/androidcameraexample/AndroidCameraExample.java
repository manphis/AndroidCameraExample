package com.javacodegeeks.androidcameraexample;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.javacodegeeks.glcomponent.MyGLSurfaceView;

public class AndroidCameraExample extends Activity implements CameraPreview.CameraFrameCallback, SurfaceHolder.Callback {
	private Camera mCamera;
	private CameraPreview mPreview;
	private PictureCallback mPicture;
	private Button capture, switchCamera;
	private Context myContext;
	private RelativeLayout cameraPreview;
	private boolean cameraFront = false;
	private float screenDensity;

	private SurfaceView mRenderView;
	private RenderThread renderThread = null;
	private MyGLSurfaceView glSurfaceView = null;

	private int cameraId = -1;

	private static final String TAG = "CameraExample";
	private static final int TARGET_WIDTH = 640;
	private static final int TARGET_HEIGHT = 480;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		myContext = this;

		DisplayMetrics metrics = getResources().getDisplayMetrics();
		screenDensity = metrics.density;
		Log.i(TAG, "device dpi = " + screenDensity);

		initialize();
	}

	private int findFrontFacingCamera() {
//		int cameraId = -1;
		// Search for the front facing camera
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
				cameraId = i;
				cameraFront = true;
				break;
			}
		}
		return cameraId;
	}

	private int findBackFacingCamera() {
		int cameraId = -1;
		//Search for the back facing camera
		//get the number of cameras
		int numberOfCameras = Camera.getNumberOfCameras();
		//for every camera check
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
				cameraId = i;
				cameraFront = false;
				break;
			}
		}
		return cameraId;
	}

	public void onResume() {
		Log.i(TAG, "onResume");
		super.onResume();
//		if (!hasCamera(myContext)) {
//			Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
//			toast.show();
//			finish();
//		}
		if (mCamera == null) {
			//if the front facing camera does not exist
			if (findFrontFacingCamera() < 0) {
				Toast.makeText(this, "No front facing camera found.", Toast.LENGTH_LONG).show();
				switchCamera.setVisibility(View.GONE);
			}			
//			mCamera = Camera.open(findBackFacingCamera());
//			mCamera = Camera.open(0);
			cameraId = findFrontFacingCamera();
			mCamera = Camera.open(cameraId);
			mPicture = getPictureCallback();
			mPreview.refreshCamera(mCamera, cameraId);
		}
	}

	public void initialize() {
		Log.i(TAG, "initialize");
		cameraPreview = (RelativeLayout) findViewById(R.id.camera_preview);
		mPreview = new CameraPreview(myContext, mCamera, this, TARGET_WIDTH, TARGET_HEIGHT);
		cameraPreview.addView(mPreview);

		mRenderView = (SurfaceView) findViewById(R.id.render_preview);
		mRenderView.getHolder().addCallback(this);

		capture = (Button) findViewById(R.id.button_capture);
		capture.setOnClickListener(captrureListener);

		switchCamera = (Button) findViewById(R.id.button_ChangeCamera);
		switchCamera.setOnClickListener(switchCameraListener);

		glSurfaceView = (MyGLSurfaceView) findViewById(R.id.gl_preview);
		glSurfaceView.setYuvDataSize(TARGET_WIDTH, TARGET_HEIGHT);
		glSurfaceView.setDisplayOrientation(90);

//		startRenderThread();
	}

    private void startRenderThread(int viewWidth, int viewHeight) {
        if (renderThread == null)
            renderThread = new RenderThread(mRenderView, TARGET_WIDTH, TARGET_HEIGHT, viewWidth, viewHeight);
        if (!renderThread.isRunning())
            renderThread.start();
    }

	OnClickListener switchCameraListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			//get the number of cameras
			int camerasNumber = Camera.getNumberOfCameras();
			if (camerasNumber > 1) {
				//release the old camera instance
				//switch camera, from the front and the back and vice versa
				
				releaseCamera();
				chooseCamera();
			} else {
				Toast toast = Toast.makeText(myContext, "Sorry, your phone has only one camera!", Toast.LENGTH_LONG);
				toast.show();
			}
		}
	};

	public void chooseCamera() {
		//if the camera preview is the front
		if (cameraFront) {
			int cameraId = findBackFacingCamera();
			if (cameraId >= 0) {
				//open the backFacingCamera
				//set a picture callback
				//refresh the preview
				
				mCamera = Camera.open(cameraId);				
				mPicture = getPictureCallback();			
				mPreview.refreshCamera(mCamera, cameraId);
			}
		} else {
			int cameraId = findFrontFacingCamera();
			if (cameraId >= 0) {
				//open the backFacingCamera
				//set a picture callback
				//refresh the preview
				
				mCamera = Camera.open(cameraId);
				mPicture = getPictureCallback();
				mPreview.refreshCamera(mCamera, cameraId);
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		//when on Pause, release camera in order to be used from other applications
		releaseCamera();

		if (null != renderThread) {
		    renderThread.stopRunning();
		    renderThread = null;
        }
	}

	private boolean hasCamera(Context context) {
		//check if the device has camera
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			return true;
		} else {
			return false;
		}
	}

	private PictureCallback getPictureCallback() {
		PictureCallback picture = new PictureCallback() {

			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				//make a new picture file
				File pictureFile = getOutputMediaFile();
				
				if (pictureFile == null) {
					return;
				}
				try {
					//write the file
					FileOutputStream fos = new FileOutputStream(pictureFile);
					fos.write(data);
					fos.close();
					Toast toast = Toast.makeText(myContext, "Picture saved: " + pictureFile.getName(), Toast.LENGTH_LONG);
					toast.show();

				} catch (FileNotFoundException e) {
				} catch (IOException e) {
				}
				
				//refresh camera to continue preview
				mPreview.refreshCamera(mCamera, cameraId);
			}
		};
		return picture;
	}

	OnClickListener captrureListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			mCamera.takePicture(null, null, mPicture);
		}
	};

	//make picture and save to a folder
	private static File getOutputMediaFile() {
		//make a new file directory inside the "sdcard" folder
		File mediaStorageDir = new File("/sdcard/", "JCG Camera");
		
		//if this "JCGCamera folder does not exist
		if (!mediaStorageDir.exists()) {
			//if you cannot make this folder return
			if (!mediaStorageDir.mkdirs()) {
				return null;
			}
		}
		
		//take the current timeStamp
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile;
		//and make a media file:
		mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
		
		return mediaFile;
	}

	private void releaseCamera() {
		// stop and release camera
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;
		}
	}

    @Override
    public void onFrame(byte[] frame) {
        if (null != renderThread && renderThread.isRunning()) {
            renderThread.insertQueue(frame);
        }

		glSurfaceView.feedData(frame, 2);
    }

	@Override
	public void surfaceCreated(SurfaceHolder surfaceHolder) {

	}

	@Override
	public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int width, int height) {
		Log.i(TAG, "surfaceChanged = " + width + " x " + height);
		startRenderThread(width, height);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

	}
}