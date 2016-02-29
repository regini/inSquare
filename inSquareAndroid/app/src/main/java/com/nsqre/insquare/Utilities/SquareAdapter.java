package com.nsqre.insquare.Utilities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by emanu on 25/02/2016.
 */
public class SquareAdapter extends BaseAdapter {

    private MapActivity activity;
    private ArrayList data;
    private static LayoutInflater inflater = null;
    int i = 0;
    private InSquareProfile userProfile;

    public SquareAdapter(Activity a, ArrayList d) {

        activity = (MapActivity)a;
        data = d;

        inflater = (LayoutInflater) activity.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        userProfile.getInstance(a.getApplicationContext());
    }

    public int getCount() {

        if (data.size() <= 0)
            return 1;
        return data.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    /****** Depends upon data size called for each row , Create each ListView row *****/
    public View getView(int position, View convertView, ViewGroup parent) {

        View vi = convertView;
        ViewHolder holder;

        final Square square = (Square) data.get(position);

        if(convertView==null){

            /****** Inflate tabitem.xml file for each row ( Defined below ) *******/
            vi = inflater.inflate(R.layout.square_item, null);
            //click sulla riga
            vi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(activity, ChatActivity.class);
                    intent.putExtra(MapFragment.SQUARE_ID_TAG, square.getId());
                    intent.putExtra(MapFragment.SQUARE_NAME_TAG, square.getName());
                    activity.startActivity(intent);
                }
            });

            final ImageView star = (ImageView) vi.findViewById(R.id.square_star_icon);
            //icona gialla se è preferita
            if (activity.getFavouriteSquaresList().contains(square)) {
                star.setImageResource(R.drawable.star_icon_yellow);
            }
            //click sulla stella
            star.setOnClickListener(new View.OnClickListener() {
                //sul click rimuove o aggiunge ai preferiti
                @Override
                public void onClick(View v) {
                    if (activity.getFavouriteSquaresList().contains(square)) {
                        favouriteSquare(Request.Method.DELETE, square);

                    } else {
                        favouriteSquare(Request.Method.POST, square);
                    }
                }
            });

            holder = new ViewHolder();
            holder.name = (TextView) vi.findViewById(R.id.square_item_name);
            holder.id = (TextView) vi.findViewById(R.id.square_item_id);

            /************  Set holder with LayoutInflater ************/
            vi.setTag(holder);
        }
        else
            holder = (ViewHolder) vi.getTag();

        if(data.size() <= 0 ) {
            holder.name.setText("No Data");
        }
        else {
            /***** Get each Square object from Arraylist ********/
            Square s = (Square)data.get(position);
            /************  Set Model values in Holder elements ***********/
            holder.name.setText( s.getName() );
            holder.id.setText( s.getOwnerId());
        }
        return vi;
    }

    public void favouriteSquare(final int method, final Square square) {
        RequestQueue queue = Volley.newRequestQueue(activity);
        final String squareId = square.getId();
        final String userId = userProfile.getUserId();
        String url = "http://recapp-insquare.rhcloud.com/favouritesquares";
        StringRequest postRequest = new StringRequest(method, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        updateView(method, square);
                        Log.d("FAVOURITE", "response => " + response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("FAVOURITE","error => "+error.toString());
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("squareId", squareId);
                params.put("userId", userId);
                return params;
            }
        };
        queue.add(postRequest);
    }

    public void updateView (int method, Square square) {
        if (method == Request.Method.DELETE) {
            //star.setImageResource(R.drawable.star_icon_empty);
            activity.getFavouriteSquaresList().remove(square);
            activity.getProfileFragment().resetAdapters();
        } else {
            //star.setImageResource(R.drawable.star_icon_yellow);
            activity.getFavouriteSquaresList().add(square);
            activity.getProfileFragment().resetAdapters();
        }
    }

    public static class ViewHolder {
        public TextView name;
        public TextView id;
    }
}
