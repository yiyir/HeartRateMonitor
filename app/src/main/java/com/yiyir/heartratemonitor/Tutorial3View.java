package com.yiyir.heartratemonitor;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

public class Tutorial3View extends JavaCameraView {

    public Tutorial3View(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void setFlashOn() {
        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(params);
        mCamera.setDisplayOrientation(90);
    }

    public void setFlashOff() {
        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(params);
        mCamera.setDisplayOrientation(90);
    }
}
