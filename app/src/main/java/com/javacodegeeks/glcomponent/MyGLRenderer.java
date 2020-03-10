package com.javacodegeeks.glcomponent;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "MyGLRenderer";

    private MyGLProgram mProgram = null;

    private int mScreenWidth = 0;
    private int mScreenHeight = 0;
    private int mVideoWidth = 0;
    private int mVideoHeight = 0;

    private float[] vPMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];

    private ByteBuffer y;
    private ByteBuffer u;
    private ByteBuffer v;
    private ByteBuffer uv;

    private int type = 0;
    private boolean hasVisibility = false;

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        mProgram = new MyGLProgram();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        mScreenWidth = width;
        mScreenHeight = height;
        float ratio = (float)width / (float)height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f);

        // Set the camera position (View matrix)
//        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 1.0f, 0.0f, 0.0f);  // mark by eason

        if (mVideoWidth > 0 && mVideoHeight > 0) {
            createBuffers(mVideoWidth, mVideoHeight);
        }
        hasVisibility = true;
        Log.d(TAG, "onSurfaceChanged = " + width + " x " + height);

    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        synchronized(this) {
            if (y.capacity() > 0) {
                y.position(0);
                if (type == 0) {
                    u.position(0);
                    v.position(0);
                    mProgram.feedTextureWithImageData(y, u, v, mVideoWidth, mVideoHeight);
                } else {
                    uv.position(0);
                    mProgram.feedTextureWithImageData(y, uv, mVideoWidth, mVideoHeight);
                }
                // Redraw background color
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                // Calculate the projection and view transformation
                Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

                try {
                    mProgram.drawTexture(vPMatrix, type);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 设置显示方向
     * @param degrees 显示旋转角度（逆时针），有效值是（0, 90, 180, and 270.）
     */
    public void setDisplayOrientation(int degrees) {
        // Set the camera position (View matrix)
        if (degrees == 0) {
            Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 1.0f, 0.0f, 0.0f);
        } else if (degrees == 90) {
            Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0.0f, 1.0f, 0.0f);
        } else if (degrees == 180) {
            Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, -1.0f, 0.0f, 0.0f);
        } else if (degrees == 270) {
            Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0.0f, -1.0f, 0.0f);
        } else {
            Log.e(TAG, "degrees pram must be in (0, 90, 180, 270) ");
        }
    }

    /**
     * 设置渲染的YUV数据的宽高
     * @param width 宽度
     * @param height 高度
     */
    public void setYuvDataSize(int width, int height) {
        Log.i(TAG, "setYuvDataSize = " + width + " x " + height);
        if (width > 0 && height > 0) {
            // 调整比例
            createBuffers(width, height);

            // 初始化容器
            if (width != mVideoWidth && height != mVideoHeight) {
                this.mVideoWidth = width;
                this.mVideoHeight = height;
                int yarraySize = width * height;
                int uvarraySize = yarraySize / 4;
                synchronized(this) {
                    y = ByteBuffer.allocate(yarraySize);
                    u = ByteBuffer.allocate(uvarraySize);
                    v = ByteBuffer.allocate(uvarraySize);
                    uv = ByteBuffer.allocate(uvarraySize * 2);
                }
            }
        }
    }

    /**
     * 预览YUV格式数据
     * @param yuvdata yuv格式的数据
     * @param type YUV数据的格式 0 -> I420  1 -> NV12  2 -> NV21
     */
    public void feedData(byte[] yuvdata, int type) {
        synchronized(this) {
            if (hasVisibility) {
                this.type = type;
                if (type == 0) {
                    y.clear();
                    u.clear();
                    v.clear();
                    y.put(yuvdata, 0, mVideoWidth * mVideoHeight);
                    u.put(yuvdata, mVideoWidth * mVideoHeight, mVideoWidth * mVideoHeight / 4);
                    v.put(yuvdata, mVideoWidth * mVideoHeight * 5 / 4, mVideoWidth * mVideoHeight / 4);
                } else {
                    y.clear();
                    uv.clear();
                    y.put(yuvdata, 0, mVideoWidth * mVideoHeight);
                    uv.put(yuvdata, mVideoWidth * mVideoHeight, mVideoWidth * mVideoHeight / 2);
                }
            }
        }
    }

    /**
     * 调整渲染纹理的缩放比例
     * @param width YUV数据宽度
     * @param height YUV数据高度
     */
//    private void createBuffers(int width, int height) {
//        Log.i(TAG, "createBuffers = " + width + " x " + height);
//        if (mScreenWidth > 0 && mScreenHeight > 0) {
//            float f1 = (float)mScreenHeight / (float)mScreenWidth;
//            float f2 = (float)height / (float)width;
//            Log.i(TAG, "f1 = " + f1 + " f2 = " + f2);
//            if (f1 == f2) {
//                mProgram.createBuffers(MyGLProgram.squareVertices);
//            } else if (f1 < f2) {
//                float widthScale = f1 / f2;
//                float[] vert = {-widthScale, -1.0f, widthScale, -1.0f, -widthScale, 1.0f, widthScale, 1.0f};
//                mProgram.createBuffers(vert);
//            } else {
//                float heightScale = f2 / f1;
//                float[] vert = {-1.0f, -heightScale, 1.0f, -heightScale, -1.0f, heightScale, 1.0f, heightScale};
//                mProgram.createBuffers(vert);
//            }
//        } else {
//            Log.i(TAG, "not createBuffers = " + mScreenWidth + " x " + mScreenWidth);
//        }
//    }

    // test by eason
    private void createBuffers(int width, int height) {
        Log.i(TAG, "createBuffers = " + width + " x " + height);
        if (mScreenWidth > 0 && mScreenHeight > 0) {
            float f1 = 4f;
            float f2 = 3f;
            Log.i(TAG, "f1 = " + f1 + " f2 = " + f2);
            if (f1 == f2) {
                mProgram.createBuffers(MyGLProgram.squareVertices);
            } else if (f1 < f2) {
                float widthScale = f1 / f2;
                float[] vert = {-widthScale, -1.0f, widthScale, -1.0f, -widthScale, 1.0f, widthScale, 1.0f};
                mProgram.createBuffers(vert);
            } else {
                float heightScale = f2 / f1;
                float[] vert = {-1.0f, -heightScale, 1.0f, -heightScale, -1.0f, heightScale, 1.0f, heightScale};
                mProgram.createBuffers(vert);
            }
        } else {
            Log.i(TAG, "not createBuffers = " + mScreenWidth + " x " + mScreenWidth);
        }
    }
}
