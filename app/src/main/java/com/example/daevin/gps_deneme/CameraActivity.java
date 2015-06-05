package com.example.daevin.gps_deneme;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;


public class CameraActivity extends ActionBarActivity implements SurfaceHolder.Callback {

    private SurfaceView sv;
    private SurfaceHolder sHolder;
    private Camera mCamera=null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_camera);


        int index = getBackCameraId();
        if (index == -1){
            Toast.makeText(getApplicationContext(), "No back camera", Toast.LENGTH_LONG).show();
        }
        else
        {



            sv = (SurfaceView) findViewById(R.id.surfaceView);
            sHolder = sv.getHolder();
            sHolder.addCallback(this);
            sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        }



    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
    {
        Camera.Parameters parameters = mCamera.getParameters();
        mCamera.setParameters(parameters);
        mCamera.startPreview();

        Camera.PictureCallback mCall = new Camera.PictureCallback()
        {
            @Override
            public void onPictureTaken(byte[] data, Camera camera)
            {

                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                Intent resultIntent=new Intent();
                resultIntent.putExtra("image",bmp);
                finish();///////////////////////////////
            }
        };

        mCamera.takePicture(null, null, mCall);

    }

    int getBackCameraId() {
        Camera.CameraInfo ci = new Camera.CameraInfo();
        for (int i = 0 ; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == Camera.CameraInfo.CAMERA_FACING_BACK) return i;
        }
        return -1; // No back-facing camera found
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        int index = getBackCameraId();
        if (index == -1){
            Toast.makeText(getApplicationContext(), "No front camera", Toast.LENGTH_LONG).show();
        }
        else
        {
            mCamera = Camera.open(index);
            Toast.makeText(getApplicationContext(), "With front camera", Toast.LENGTH_LONG).show();
        }
       // mCamera = Camera.open(index);
        try {
            mCamera.setPreviewDisplay(holder);

        } catch (IOException exception) {
            mCamera.release();
            mCamera = null;
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }
}
