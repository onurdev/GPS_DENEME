package com.example.daevin.gps_deneme;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {

    private static final int IMAGE_REQUEST_CODE = 1;
    public static ArrayList<Park> parks;
    public static ArrayAdapter<Park> adapter;

    private Location currLocation;

    private LocationManager mLocationManager;
    private Button saveLocationButton;

    private static final int TEN_SECONDS = 10000;
    private static final int ONE_METER = 1;
    private static final int TWO_MINUTES = 1000 * 60 * 2;

    ListView listView;
    private final LocationListener listener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            // A new location update is received.  Do something useful with it.  Update the UI with
            // the location update.

            updateUILocation(location); //TODO
            Log.d("Location", location.toString());
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        saveLocationButton = (Button) findViewById(R.id.saveLocationButton);

        DBHelper dbHelper=new DBHelper(this);

        parks = dbHelper.getParks();
        adapter = new ParkAdapter(this, 0, parks);

        listView = (ListView) findViewById(R.id.locationsListView);
        listView.setAdapter(adapter);
        listView.setClickable(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapt, View v, int position,long a) {

                Park item =(Park) adapter.getItem(position);

                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("lat", item.getLat());  //TODO make Park parcelable and send that to maps activity
                intent.putExtra("lng", item.getLng());
                startActivity(intent);
            }
        });

        adapter.notifyDataSetChanged();

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        setup();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if the GPS setting is currently enabled on the device.
        // This verification should be done during onStart() because the system calls this method
        // when the user returns to the activity, which ensures the desired location provider is
        // enabled each time the activity resumes from the stopped state.

        final boolean gpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gpsEnabled) {
            // Build an alert dialog here that requests that the user enable
            // the location services, then when the user clicks the "OK" button,
            // call enableLocationSettings()
            new EnableGpsDialogFragment().show(getSupportFragmentManager(), "enableGpsDialog");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        ToggleButton tgb=(ToggleButton)(findViewById(R.id.toggleButton));
        tgb.setChecked(isServiceRunning());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public void saveLocation(View v) {
        final boolean gpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(gpsEnabled) {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivityForResult(intent, IMAGE_REQUEST_CODE);
        }else{
            new EnableGpsDialogFragment().show(getSupportFragmentManager(), "enableGpsDialog");
        }


    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Park park= new Park();
        DBHelper dbHelper=new DBHelper(this);
        park.setId(dbHelper.getLargestID() + 1);
        park.setLat(currLocation.getLatitude());
        park.setLng(currLocation.getLongitude());

        GeocoderHelper geocoderHelper = new GeocoderHelper();
        String addressText = geocoderHelper.getAddress(this, currLocation);
        park.setAddress(addressText);


        if(requestCode==IMAGE_REQUEST_CODE) {
            if(resultCode==RESULT_OK) {
                Bitmap image = data.getExtras().getParcelable("image");
                park.setPhoto(image);
                Log.e("camera","image taken");
            }

        }
        dbHelper.addPark(park);
        Toast.makeText(getApplicationContext(), "saved to database: "+park.getAddress(), Toast.LENGTH_LONG).show();

        notifyAdapter();
    }

    private void notifyAdapter() {
        ArrayList<Park> tmpParks = new DBHelper(this).getParks();
        parks.clear();
        for(Park p:tmpParks){
            parks.add(p);
           // Log.e("save",p.toString());
        }

        adapter.notifyDataSetChanged();
    }

    // Set up location providers
    private void setup() {
        Location gpsLocation = null;
        Location networkLocation = null;
        mLocationManager.removeUpdates(listener);
        // Request updates from both fine (gps) and coarse (network) providers.
        gpsLocation = requestUpdatesFromProvider(LocationManager.GPS_PROVIDER, R.string.not_support_gps);
        networkLocation = requestUpdatesFromProvider(LocationManager.NETWORK_PROVIDER, R.string.not_support_network);

        // If both providers return last known locations, compare the two and use the better
        // one to update the UI.  If only one provider returns a location, use it.
        if (gpsLocation != null && networkLocation != null) {
            updateUILocation(getBetterLocation(gpsLocation, networkLocation));
        } else if (gpsLocation != null) {
            updateUILocation(gpsLocation);
        } else if (networkLocation != null) {
            updateUILocation(networkLocation);
        }

    }

    private void updateUILocation(Location location) {
        currLocation=location;
    }

    private Location requestUpdatesFromProvider(final String provider, final int errorResId) {
        Location location = null;
        if (mLocationManager.isProviderEnabled(provider)) {
            mLocationManager.requestLocationUpdates(provider, TEN_SECONDS, ONE_METER, listener);
            location = mLocationManager.getLastKnownLocation(provider);
        } else {
            Toast.makeText(this, errorResId, Toast.LENGTH_LONG).show();
        }
        return location;
    }

    /** Determines whether one Location reading is better than the current Location fix.
     * Code taken from
     * http://developer.android.com/guide/topics/location/obtaining-user-location.html
     *
     * @param newLocation  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new
     *        one
     * @return The better Location object based on recency and accuracy.
     */
    protected Location getBetterLocation(Location newLocation, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return newLocation;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = newLocation.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved.
        if (isSignificantlyNewer) {
            return newLocation;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return currentBestLocation;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (newLocation.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(newLocation.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return newLocation;
        } else if (isNewer && !isLessAccurate) {
            return newLocation;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return newLocation;
        }
        return currentBestLocation;
    }

    // Method to launch Settings
    private void enableLocationSettings() {
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(settingsIntent);
    }
    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }


    /**
     * Dialog to prompt users to enable GPS on the device.
     */
    @SuppressLint("ValidFragment")
    private class EnableGpsDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog alert = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.enable_gps)
                    .setMessage(R.string.enable_gps_dialog)
                    .setPositiveButton(R.string.enable_gps, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            enableLocationSettings();

                        }
                    })
                    .create();
            //Button button = new Button(getApplicationContext());
            return alert;
        }

    }
    public void imageOnclick(View v){

        ImageView iv =(ImageView)v;


        int id = (int) v.getTag();
        Log.e("imageView","im clicked: "+id);
        Bitmap bmp= new DBHelper(this).getPhotoOf(id);

        Intent intent = new Intent(this, ImageActivity.class);
        intent.putExtra("image",bmp);
        startActivity(intent);


    }

    public void deleteOnClick(View v) {
        Log.e("deleteonclick","delete is clicked");
        int id = (int)v.getTag();

        DBHelper dbHelper = new DBHelper(this);
        dbHelper.deleteRowByID(id);
        Log.e("rowId", "" + id);
        notifyAdapter();
    }

    public void toggleButtonOnClick(View v) {
        ToggleButton toggleButton = (ToggleButton) v;
        if (toggleButton.isChecked()) {
            Log.d("toggle","button is checked");
            Intent intent = new Intent(this, TrackingService.class);
            startService(intent);
        }
        else {
            Log.d("toggle","button is not checked");
            Intent intent = new Intent(this, TrackingService.class);
            stopService(intent);
        }
    }
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (TrackingService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        boolean serviceRunning = intent.getBooleanExtra("isServiceRunning", true);

        if (serviceRunning) {
            ToggleButton tgb = (ToggleButton) (findViewById(R.id.toggleButton));
            tgb.setChecked(false);
        }
        //onStart();
    }
}
