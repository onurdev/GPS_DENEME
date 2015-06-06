package com.example.daevin.gps_deneme;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by Cem Benar on 06/06/2015.
 */
public class GeocoderHelper {

    String getAddress (Context context, Location location) {
        Geocoder geocoder=new Geocoder(context, Locale.getDefault());
        List<Address> addresses=null;
        try {
            addresses =geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
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

            return addressText;
        }
        return null;
    }
}
