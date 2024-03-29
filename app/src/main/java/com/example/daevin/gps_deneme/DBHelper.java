package com.example.daevin.gps_deneme;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Cem Benar on 04/06/2015.
 */
public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "ParkDB.db";
    public static final String PARKS_TABLE_NAME = "parks";
    public static final String PARKS_COLUMN_ID = "id";
    public static final String PARKS_COLUMN_LATITUDE = "latitude";
    public static final String PARKS_COLUMN_LONGITUDE = "longitude";
    public static final String PARKS_COLUMN_ADDRESS = "address";
    public static final String PARKS_COLUMN_PHOTO = "photo";
    public static final String PARKS_COLUMN_THUMBNAIL = "thumbnail";


    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, 2);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + PARKS_TABLE_NAME +
                        " (" + PARKS_COLUMN_ID + " INTEGER PRIMARY KEY, " +
                        PARKS_COLUMN_LATITUDE + " REAL, " +
                        PARKS_COLUMN_LONGITUDE + " REAL, " +
                        PARKS_COLUMN_ADDRESS + " TEXT, " +
                        PARKS_COLUMN_PHOTO + " BLOB, " +
                        PARKS_COLUMN_THUMBNAIL + " BLOB) "
        );
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + PARKS_TABLE_NAME);
        this.onCreate(db);
    }

    public void addPark(Park park) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PARKS_COLUMN_ID, park.getId());
        values.put(PARKS_COLUMN_LATITUDE, park.getLat());
        values.put(PARKS_COLUMN_LONGITUDE, park.getLng());
        values.put(PARKS_COLUMN_ADDRESS, park.getAddress());
        values.put(PARKS_COLUMN_PHOTO, park.getPhotoAsByteArray());
       // values.put(PARKS_COLUMN_THUMBNAIL, park.getThumbNailAsByteArray());


        db.insert(PARKS_TABLE_NAME, null, values);
        db.close();
    }

    public ArrayList<Park> getParks() {
        SQLiteDatabase db = this.getReadableDatabase();
        String [] columns = {PARKS_COLUMN_ID, PARKS_COLUMN_LATITUDE, PARKS_COLUMN_LONGITUDE, PARKS_COLUMN_ADDRESS, PARKS_COLUMN_THUMBNAIL};
        Cursor cursor = db.query(PARKS_TABLE_NAME, columns, null, null, null, null, null);
        ArrayList<Park> parks = new ArrayList<Park>();
        if(cursor.moveToFirst()) {
            do {
                Park park = new Park();
                park.setId(cursor.getInt(0));
                park.setLat(cursor.getDouble(1));
                park.setLng(cursor.getDouble(2));
                park.setAddress(cursor.getString(3));
                byte[] blob = cursor.getBlob(4);
                //park.setThumbNail(BitmapFactory.decodeByteArray(blob, 0, blob.length));

                parks.add(park);
            } while (cursor.moveToNext());
        }
        db.close();
        return parks;
    }

    public Bitmap getPhotoOf(int id) {
        try {
        SQLiteDatabase db = this.getReadableDatabase();
        String [] columns = {PARKS_COLUMN_ID,PARKS_COLUMN_PHOTO};
        String where=" id = "+id;

        Cursor cursor = db.query(PARKS_TABLE_NAME, columns, where, null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }else{
            return null;
        }
        if(cursor.getCount()<1)return null;
        byte[] blob = cursor.getBlob(1);
        if(blob==null){
            Log.e("DB","photo is null");
            return null;
        }
        Bitmap bitmap;

            bitmap = BitmapFactory.decodeByteArray(blob, 0, blob.length);

        db.close();
        return bitmap;
        }catch (Exception e){
            return null;
        }
    }

    public int getLargestID() {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            String[] columns = {PARKS_COLUMN_ID};
            String orderBy = PARKS_COLUMN_ID + " DESC";
            Cursor cursor = db.query(PARKS_TABLE_NAME, columns, null, null, null, null, orderBy);
            cursor.moveToFirst();
            int largestID = cursor.getInt(0);
            cursor.close();
            Log.e("DB count", "" + largestID);
            db.close();
            return largestID;
        }catch (Exception e){

            return 0;
        }
    }

    public void deleteRowByID (int rowID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(DBHelper.PARKS_TABLE_NAME, DBHelper.PARKS_COLUMN_ID + "=" + rowID, null);
    }
}
