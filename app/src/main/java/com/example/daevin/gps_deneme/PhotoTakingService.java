package com.example.daevin.gps_deneme;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;

/**
 * Takes a single photo on service start.
 */
public class PhotoTakingService extends GpsService {

    private Bitmap currPhoto = null;
    private Park currPark = null;
    public long time = 0;
    protected LocationListener listener;

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("servicee","on start");

        Runnable r = new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                Log.d("runnable","running");
                mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                listener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        Log.d("location","received: "+location);
                        currLocation = getBetterLocation(location, currLocation);

                        if ((System.currentTimeMillis() - time) > 10000) {
                            time = System.currentTimeMillis();
                            takePhoto(getApplicationContext());
                            while (currPhoto == null) ; // wait photo to be taken

                            DBHelper dbHelper = new DBHelper(getApplicationContext());

                            GeocoderHelper geocoderHelper = new GeocoderHelper();
                            String addressText = geocoderHelper.getAddress(getApplicationContext(), currLocation);

                            Park park = new Park();
                            park.setId(dbHelper.getLargestID() + 1);
                            park.setLat(currLocation.getLatitude());
                            park.setLng(currLocation.getLongitude());
                            park.setAddress(addressText);
                            park.setPhoto(currPhoto);
                            currPark = park;
                            //dbHelper.addPark(currPark);
                            //Toast.makeText(getApplicationContext(), "saved to database: " + park.getAddress(), Toast.LENGTH_LONG).show();

                            currPhoto = null;
                        }
                    }

                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {

                    }

                    @Override
                    public void onProviderEnabled(String s) {
                    }

                    @Override
                    public void onProviderDisabled(String s) {

                    }

                };
                mLocationManager.removeUpdates(listener);

                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,0, listener);
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0,0, listener);

                mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
/*
                while (true) {
                    // Check if the GPS setting is currently enabled on the device.
                    // This verification should be done during onStart() because the system calls this method
                    // when the user returns to the activity, which ensures the desired location provider is
                    // enabled each time the activity resumes from the stopped state.

                    final boolean gpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

                    if (!gpsEnabled) {
                        // Service stops itself.
                        stopSelf();
                        // TODO create dialog
                    }

                }
                */
            }
        };
        new Thread(r).start();
        return Service.START_STICKY;
    }
        @Override
        public void onDestroy () {
            mLocationManager.removeUpdates(listener);
            super.onDestroy();
        }

        @SuppressWarnings("deprecation")
        private void takePhoto ( final Context context){
            final SurfaceView preview = new SurfaceView(context);
            SurfaceHolder holder = preview.getHolder();
            // deprecated setting, but required on Android versions prior to 3.0
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

            holder.addCallback(new SurfaceHolder.Callback() {
                @Override
                //The preview must happen at or after this point or takePicture fails
                public void surfaceCreated(SurfaceHolder holder) {
                    showMessage("Surface created");

                    Camera camera = null;

                    try {
                        camera = Camera.open();
                        showMessage("Opened camera");

                        try {
                            camera.setPreviewDisplay(holder);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        camera.startPreview();
                        showMessage("Started preview");

                        camera.takePicture(null, null, new Camera.PictureCallback() {

                            @Override
                            public void onPictureTaken(byte[] data, Camera camera) {
                                showMessage("Took picture");
                                getPhoto(BitmapFactory.decodeByteArray(data, 0, data.length));
                                camera.release();
                                //return bitmap;

                            }
                        });
                    } catch (Exception e) {
                        if (camera != null)
                            camera.release();
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }
            });

            WindowManager wm = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    1, 1, //Must be at least 1x1
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    0,
                    //Don't know if this is a safe default
                    PixelFormat.UNKNOWN);

            //Don't set the preview visibility to GONE or INVISIBLE
            wm.addView(preview, params);
        }

    private static void showMessage(String message) {
        Log.d("Camera", message);
    }

    private void getPhoto(Bitmap bitmap) {
        currPhoto = bitmap;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
