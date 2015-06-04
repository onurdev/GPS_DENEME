package com.example.daevin.gps_deneme;

import android.content.Context;
import android.graphics.Bitmap;

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
        return bArray;
    }

    public byte[] getThumbNailAsByteArray () {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (getThumbNail() == null) { return null;}
        getThumbNail().compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] bArray = bos.toByteArray();
        return bArray;
    }


}