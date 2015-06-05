package com.example.daevin.gps_deneme;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Daevin on 5.6.2015.
 */
public class ParkAdapter extends ArrayAdapter<Park> {

    private LayoutInflater inflater;

    public ParkAdapter(Activity activity, int resource, List<Park> objects) {
        super(activity, R.layout.park_layout, objects);
        inflater = activity.getWindow().getLayoutInflater();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View item = inflater.inflate(R.layout.park_layout, parent, false);
        ImageView iv=(ImageView)item.findViewWithTag("imageView");
        iv.setImageBitmap(getItem(position).getPhoto(getContext()));

        TextView tv=(TextView)item.findViewWithTag("textView");
        tv.setText(getItem(position).getAddress());
        return item;
    }

}
