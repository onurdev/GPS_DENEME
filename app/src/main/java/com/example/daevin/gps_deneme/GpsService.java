package com.example.daevin.gps_deneme;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public abstract class GpsService extends Service {

    protected static final int IMAGE_REQUEST_CODE = 2;
    protected static final int RESULT_OK = -1;
    protected ArrayList<Park> parks;
    protected Location currLocation;

    protected LocationManager mLocationManager;

    protected static final int TEN_SECONDS = 10000;
    protected static final int ONE_METER = 1;
    protected static final int TWO_MINUTES = 1000 * 60 * 2;

    protected final LocationListener listener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            // A new location update is received.  Do something useful with it.  Update the UI with
            // the location update.

           updateLocation(location); //TODO
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
    public void onCreate() {
        super.onCreate();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Park park= new Park();
        DBHelper dbHelper=new DBHelper(this);
        park.setId(dbHelper.getLargestID() + 1);
        park.setLat(currLocation.getLatitude());
        park.setLng(currLocation.getLongitude());
        Geocoder geocoder=new Geocoder(this, Locale.getDefault());
        List<Address> addresses=null;
        try {
            addresses =geocoder.getFromLocation(currLocation.getLatitude(),currLocation.getLongitude(),1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            // Format the first line of address (if available), city, and country name.
            String addressText = String.format("%s, %s, %s",
                    address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "",
                    address.getLocality(),
                    address.getCountryName());

            park.setAddress(addressText);
        }

        if(requestCode==IMAGE_REQUEST_CODE) {
            if(resultCode==RESULT_OK) {
                Bitmap image = data.getExtras().getParcelable("image");
                park.setPhoto(image);
                Log.e("camera","image taken");
            }

        }

        dbHelper.addPark(park);
        Toast.makeText(getApplicationContext(), "saved to database: "+park.getAddress(), Toast.LENGTH_LONG).show();

        ArrayList<Park> tmpParks = dbHelper.getParks();
        parks.clear();
        for(Park p:tmpParks){
            parks.add(p);
            Log.e("save",p.toString());
        }

        //adapter.notifyDataSetChanged(); // local broadcast
    }

    // Set up location providers

    protected void setup() {
        Location gpsLocation = null;
        Location networkLocation = null;
        mLocationManager.removeUpdates(listener);
        // Request updates from both fine (gps) and coarse (network) providers.
        gpsLocation = requestUpdatesFromProvider(LocationManager.GPS_PROVIDER, R.string.not_support_gps);
        networkLocation = requestUpdatesFromProvider(LocationManager.NETWORK_PROVIDER, R.string.not_support_network);

        // If both providers return last known locations, compare the two and use the better
        // one to update the UI.  If only one provider returns a location, use it.
        if (gpsLocation != null && networkLocation != null) {
            updateLocation(getBetterLocation(gpsLocation, networkLocation));
        } else if (gpsLocation != null) {
            updateLocation(gpsLocation);
        } else if (networkLocation != null) {
            updateLocation(networkLocation);
        }
    }

    protected void updateLocation(Location location) {
        currLocation=location;
    }

    protected Location requestUpdatesFromProvider(final String provider, final int errorResId) {
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

    /** Checks whether two providers are the same */
    protected boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
