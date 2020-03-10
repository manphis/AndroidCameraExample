package com.javacodegeeks.glcomponent;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import java.nio.ByteBuffer;

public class MyGLSurfaceView extends GLSurfaceView {
    private MyGLRenderer renderer;

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setEGLContextClientVersion(2);
        renderer = new MyGLRenderer();
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setDisplayOrientation(int degree) {
        renderer.setDisplayOrientation(degree);
    }

    public void setYuvDataSize(int width, int height) {
        renderer.setYuvDataSize(width, height);
    }

    /**
     * 填充预览YUV格式数据
     * @param yuvData yuv格式的数据
     * @param type YUV数据的格式 0 -> I420  1 -> NV12  2 -> NV21
     */
    public void feedData(byte[] yuvData, int type) {
        if (null == yuvData)
            return;
        renderer.feedData(yuvData, type);

        requestRender();
    }
}
