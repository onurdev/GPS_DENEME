package com.example.daevin.gps_deneme;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;

/**
 * Created by Daevin on 4.6.2015.
 */
public class Park {
    public int id;
    public double lat;
    public double lng;
    public String address;
    public Bitmap photo;
    public Bitmap thumbNail;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Bitmap getPhoto(Context context) {
        if (photo == null) {
            DBHelper dbHelper = new DBHelper(context);
            photo = dbHelper.getPhotoOf(getId());
        }
        return photo;
    }

    public void setPhoto(Bitmap photo) {

        this.photo = photo;

      //  setThumbNail(makeSmaller(photo,100,context));

    }
    public Bitmap makeSmaller(Bitmap image,int newHeight,Context context){
        final float densityMultiplier = context.getResources().getDisplayMetrics().density;

        int h= (int) (newHeight*densityMultiplier);
        int w= (int) (h * photo.getWidth()/((double) photo.getHeight()));

        photo=Bitmap.createScaledBitmap(photo, w, h, true);

        return photo;
    }

    public Bitmap getThumbNail() {
        return thumbNail;
    }

    public void setThumbNail(Bitmap thumbNail) {
        this.thumbNail = thumbNail;
    }

    public byte[] getPhotoAsByteArray () {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (photo == null) { return null;}
        photo.compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] bArray = bos.toByteArray();
        Log.e("Park", "ByteArray: " + bArray.length);
        return bArray;
    }

    public byte[] getThumbNailAsByteArray () {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (getThumbNail() == null) { return null;}
        getThumbNail().compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] bArray = bos.toByteArray();
        return bArray;
    }

    @Override
    public String toString() {
        return "Park{" +
                "id=" + id +
                ", lat=" + lat +
                ", lng=" + lng +
                ", address='" + address + '\'' +
                '}';
    }
}