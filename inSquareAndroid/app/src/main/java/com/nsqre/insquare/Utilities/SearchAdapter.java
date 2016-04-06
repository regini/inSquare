package com.nsqre.insquare.Utilities;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nsqre.insquare.R;
import com.nsqre.insquare.Square.Square;

import java.util.List;

/**
 * Created by Regini on 20/03/2016.
 */
public class SearchAdapter extends CursorAdapter {

    private List<Square> items;
    private TextView text;

    public SearchAdapter(Context context, Cursor cursor, List<Square> items) {
        super(context, cursor, false);
        this.items = items;
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

        /*
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Square s = items.get(cursor.getPosition());
                mapFragment.startChatActivity(s);
                mapFragment.setMapInPosition(s.getLat(), s.getLon());
            }
        });
        */
        return view;

    }

}