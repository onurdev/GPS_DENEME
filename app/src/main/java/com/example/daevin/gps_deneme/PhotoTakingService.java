package com.example.daevin.gps_deneme;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;

/**
 * Takes a single photo on service start.
 */
public class PhotoTakingService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    LocationRequest mLocationRequest;
    protected FusedLocationProviderApi mFusedLocationProviderApi;
    protected GoogleApiClient mGoogleApiClient;
    Camera mCamera = null;

    protected Location currLocation;
    private Bitmap currPhoto = null;
    private Park currPark = null;
    public long time = 0;

    @Override
    public void onLocationChanged(Location location) {
        Log.d("location", "received: " + location);
        currLocation = location;


        takePhoto(getApplicationContext());
        Log.d("cameraservice", "photo is being taken");
        //while (currPhoto == null) ; // wait photo to be taken
        if (currPhoto == null)
            Log.d("cameraservice", "photo is not taken");


        Log.d("cameraservice", "continue");


    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    protected synchronized void buildGoogleApiClient() {
        Log.d("googleApiClient", "Building googleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("servicee", "on start");


        Runnable r = new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                Log.d("runnable", "running");

                buildGoogleApiClient();
                mGoogleApiClient.connect();
                Log.d("googleapi", "GoogleApiClient is connected");
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
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @SuppressWarnings("deprecation")
    private void takePhoto(final Context context) {
        final WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        final SurfaceView preview = new SurfaceView(context);
        SurfaceHolder holder = preview.getHolder();
        // deprecated setting, but required on Android versions prior to 3.0
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        holder.addCallback(new SurfaceHolder.Callback() {

            @Override
            //The preview must happen at or after this point or takePicture fails
            public void surfaceCreated(SurfaceHolder holder) {
                showMessage("Surface created");

                mCamera = null;

                try {
                    mCamera = Camera.open();
                    showMessage("Opened camera");

                    try {
                        mCamera.setPreviewDisplay(holder);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    mCamera.startPreview();
                    showMessage("Started preview");

                    mCamera.takePicture(null, null, new Camera.PictureCallback() {

                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            showMessage("Took picture");
                            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                            currPhoto= scaleDownBitmap(bmp,100,getApplicationContext());
                            savePark();

                            wm.removeViewImmediate(preview);
                            camera.release();


                            //return bitmap;

                        }
                    });
                } catch (Exception e) {
                    if (mCamera != null)
                        mCamera.release();
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d("surfaceDestroyed","surfaceDestroyed");
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                wm.removeViewImmediate(preview);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }
        });


        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                1, 1, //Must be at least 1x1
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                0,
                //Don't know if this is a safe default
                PixelFormat.UNKNOWN);

        //Don't set the preview visibility to GONE or INVISIBLE
        wm.addView(preview, params);

    }

    private void savePark() {
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
        dbHelper.addPark(currPark);
        Toast.makeText(getApplicationContext(), "saved to database: " + park.getAddress(), Toast.LENGTH_LONG).show();


    }

    private static void showMessage(String message) {
        Log.d("Camera", message);
    }



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(10000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
    public static Bitmap scaleDownBitmap(Bitmap photo, int newHeight, Context context) {

        final float densityMultiplier = context.getResources().getDisplayMetrics().density;

        int h= (int) (newHeight*densityMultiplier);
        int w= (int) (h * photo.getWidth()/((double) photo.getHeight()));

        photo=Bitmap.createScaledBitmap(photo, w, h, true);

        return photo;
    }
}
