package com.example.daevin.gps_deneme;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;

/**
 * Takes a single photo on service start.
 */
public class PhotoTakingService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    ActionUserPresentReceiver mActionUserPresentReceiver;

    LocationRequest mLocationRequest;

    protected GoogleApiClient mGoogleApiClient;
    Camera mCamera = null;

    protected Location currLocation;
    private Bitmap currPhoto = null;
    private static Park currPark = null;
    public long time = 0;


    boolean gotFastFlag = false;
    boolean gotSlowFlag = false;


    @Override
    public void onLocationChanged(Location location) {
        Log.e("location", "received: " + location);
        currLocation = location;

        if (gotFastFlag) {
            currPark=null;
            if (gotSlowFlag) {
                gotFastFlag = false;
                gotSlowFlag = false;
                takePhoto(getApplicationContext());

            } else {
                gotSlowFlag = checkGotSlow(location);
            }

        } else {
            gotFastFlag = checkGotFast(location);

        }
    }

    private boolean checkGotSlow(Location location) {
        if (location.hasSpeed()) {
            return location.getSpeed() < (5 / 3.6);
        }
        return false;
    }

    public boolean checkGotFast(Location location) {
        if (location.hasSpeed()) {
            return location.getSpeed() > (50.0 / 3.6);
        }
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mActionUserPresentReceiver = new ActionUserPresentReceiver();
        IntentFilter intentFilter= new IntentFilter();
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mActionUserPresentReceiver,intentFilter);
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


            }
        };
        new Thread(r).start();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
        unregisterReceiver(mActionUserPresentReceiver);
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
                            currPhoto = scaleDownBitmap(bmp, 100, getApplicationContext());
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
                Log.d("surfaceDestroyed", "surfaceDestroyed");
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
       // dbHelper.addPark(currPark);
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

        int h = (int) (newHeight * densityMultiplier);
        int w = (int) (h * photo.getWidth() / ((double) photo.getHeight()));

        photo = Bitmap.createScaledBitmap(photo, w, h, true);

        return photo;
    }

   public class ActionUserPresentReceiver extends BroadcastReceiver {
       public ActionUserPresentReceiver() {
           super();
       }

       @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                Log.e("broadcast receiver","broadcast received");

                Intent intent1 = new Intent(getApplicationContext(), MainActivity.class);
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if(currPark!=null){
                    new DBHelper(context).addPark(currPark);
                    currPark=null;
                    intent1.putExtra("isServiceRunning", false);
                    startActivity(intent1);
                    stopSelf();
                }

            }
        }
    }
}
