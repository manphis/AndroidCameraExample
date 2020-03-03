package com.javacodegeeks.androidcameraexample;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;

import java.util.concurrent.LinkedBlockingQueue;

public class RenderThread extends Thread {
    private static final String TAG = "RenderThread";
    private LinkedBlockingQueue<byte[]> renderQueue = new LinkedBlockingQueue<byte[]>(2);
    private byte[] stop_sig = {0,0,0,0};
    private boolean mIsStarted = false;
    private Canvas canvas = null;
    private SurfaceView mCameraPreview = null;

    private boolean running = false;
    private float m_x_scale = 1.0f;
    private float m_y_scale = 1.0f;
    private int mSourceWidth, mSourceHeight, mViewWidth, mViewHeight;

//    private int[] rgbArray = new int[320*240];
    private int[] rgbArray = null;

    public RenderThread(SurfaceView view, int sourceWidth, int sourceHeight, int viewWidth, int viewHeight) {
        mCameraPreview = view;

        mSourceWidth = sourceWidth;
        mSourceHeight = sourceHeight;
        mViewWidth = viewWidth;
        mViewHeight = viewHeight;

        m_x_scale = (float)mViewWidth / (float)mSourceWidth;
        m_y_scale = (float)mViewHeight / (float)mSourceHeight;

        rgbArray = new int[mSourceWidth * mSourceHeight];
    }

    public void run() {
        Log.e(TAG, "start RenderThread");
        running = true;
        while (running && renderQueue != null) {
            try {
                byte[] buf = renderQueue.take();		// implement by LinkedBlockingQueue<>, blocking mode

                if(buf.equals(stop_sig))
                    continue;

                render(buf);
            } catch (InterruptedException ie) {
                Log.e(TAG, "exception");
                ie.printStackTrace();
            }
        }
        Log.d(TAG, "leave RenderThread");
//        running = false;
        renderQueue.clear();
    }

    public boolean isRunning() {
        return running;
    }

    public void stopRunning() {
        try {
            running = false;
            renderQueue.put(stop_sig);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

    }

    public void insertQueue(byte[] crop_data) {
        if (null == crop_data || renderQueue.size() == 2) {
    		Log.d(TAG, "not insert to renderQueue size = " + renderQueue.size());
            return;
        }
        byte[] copy_data = new byte[crop_data.length];
        System.arraycopy(crop_data, 0, copy_data, 0, copy_data.length);

        try {
            renderQueue.put(copy_data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void render(byte[] crop_data) {
        if (null == crop_data)
            return;

//        synchronized (mSurfaceLock) {
            if (mCameraPreview != null && mCameraPreview.getHolder().getSurface() != null) {
                try {
                    Bitmap bmp = null;
                    Bitmap reBmp = null;
                    canvas = mCameraPreview.getHolder().getSurface().lockCanvas(null);
                    if (canvas != null) {
                        decodeN21toRGB565(rgbArray, crop_data, mSourceWidth, mSourceHeight);

                        Matrix matrix = new Matrix();
                        //matrix.postScale(0.75f, 0.75f);
                        matrix.postScale(m_x_scale, m_y_scale);
                        bmp = Bitmap.createBitmap(rgbArray, mSourceWidth, mSourceHeight, Bitmap.Config.RGB_565);
                        reBmp = Bitmap.createBitmap(bmp, 0, 0, mSourceWidth, mSourceHeight, matrix, true);
                        //bmp = Bitmap.createBitmap(rgbArray, 320, 240, Bitmap.Config.RGB_565);
                        canvas.drawBitmap(reBmp, 0, 0, null);
                    }
                    mCameraPreview.getHolder().getSurface().unlockCanvasAndPost(canvas);
                    if (bmp != null) {
                        bmp.recycle();
                        bmp = null;
                    }

                    if (reBmp != null) {
                        reBmp.recycle();
                        reBmp = null;
                    }

//                    cont++;
//                    calcFps();

//                    if(crop_data != null){
//                    	crop_data = null;
//                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (Surface.OutOfResourcesException e) {
                    e.printStackTrace();
                }
            }
//        }
    }

    private void decodeN21toRGB565(int[] rgb, byte[] yuv420sp, int width, int height){
        int frameSize = width * height;
        int rgb_index = 0;
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0; else if (r > 262143) r = 262143;
                if (g < 0) g = 0; else if (g > 262143) g = 262143;
                if (b < 0) b = 0; else if (b > 262143) b = 262143;

                //mirror the image
                rgb_index = (width-1-i) + j*width;
                rgb[rgb_index] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }
}
