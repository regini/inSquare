package com.nsqre.insquare;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.nsqre.insquare.Activities.ChatActivity;
import com.nsqre.insquare.Activities.MapActivity;
import com.nsqre.insquare.Fragments.MapFragment;
import com.nsqre.insquare.Square.Square;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Regini on 20/03/2016.
 */
public class SearchAdapter extends CursorAdapter {

    private List<Square> items;
    private MapFragment mapFragment;
    private TextView text;

    public SearchAdapter(Context context, Cursor cursor, List<Square> items, MapFragment mapFragment) {
        super(context, cursor, false);
        this.items = items;
        this.mapFragment = mapFragment;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if(items!=null && text!=null) {
            text.setText(items.get(cursor.getPosition()).getName());
        }
    }

    @Override
    public View newView(Context context, final Cursor cursor, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.search_item, parent, false);

        text = (TextView) view.findViewById(R.id.square_search_name);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Square s = items.get(cursor.getPosition());
                mapFragment.setMapInPosition(s.getLat(), s.getLon());
            }
        });
        return view;

    }

}