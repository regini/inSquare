package com.nsqre.insquare.Utilities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.nsqre.insquare.Fragments.MainMapFragment;
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.R;

import java.util.ArrayList;

/**
 * Created by emanu on 25/02/2016.
 */
public class SquareAdapter extends BaseAdapter {

    private static final String TAG = "SquareAdapter";
    private MapActivity activity;
    private ArrayList data;
    private static LayoutInflater inflater = null;
    int i = 0;

    public SquareAdapter(Activity a, ArrayList d) {

        activity = (MapActivity)a;
        data = d;

        inflater = (LayoutInflater) activity.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
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
                    intent.putExtra(MainMapFragment.SQUARE_TAG, square);
                    SharedPreferences sharedPreferences = activity.getSharedPreferences("NOTIFICATION_MAP", Context.MODE_PRIVATE);
                    if(sharedPreferences.contains(square.getId())) {
                        sharedPreferences.edit().remove(square.getId()).apply();
                        sharedPreferences.edit().putInt("squareCount", sharedPreferences.getInt("squareCount",0) - 1).apply();
                        activity.checkNotifications();
                    }
                    activity.startActivity(intent);
                }
            });

            final ImageView star = (ImageView) vi.findViewById(R.id.square_fav_icon);
            //icona gialla se è preferita
//            if (InSquareProfile.favouriteSquaresList.contains(square)) {
            if(InSquareProfile.isFav(square.getId()))
            {
                star.setImageResource(R.drawable.heart_black);
            }
            //click sulla stella
            star.setOnClickListener(new View.OnClickListener() {
                //sul click rimuove o aggiunge ai preferiti
                @Override
                public void onClick(View v) {
                    if (InSquareProfile.isFav(square.getId())) {
                        favouriteSquare(Request.Method.DELETE, square);
                        star.setImageResource(R.drawable.heart_border_black);
                    } else {
                        star.setImageResource(R.drawable.heart_black);
                        favouriteSquare(Request.Method.POST, square);
                    }
                }
            });

            holder = new ViewHolder();
            holder.name = (TextView) vi.findViewById(R.id.square_item_name);
            holder.id = (TextView) vi.findViewById(R.id.square_item_id);

            //contatore di nuovi messaggi
            TextView txtCount = (TextView) vi.findViewById(R.id.counter);
            SharedPreferences sharedPreferences = activity.getSharedPreferences("NOTIFICATION_MAP", Context.MODE_PRIVATE);
            //se non trova la chiave ritorna 0
            int squaresNewMessages = sharedPreferences.getInt(square.getId(), 0);
            if (squaresNewMessages == 0) {
                txtCount.setVisibility(View.INVISIBLE);
            } else {
                txtCount.setText(String.valueOf(squaresNewMessages));
            }


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
            holder.id.setText( s.formatTime() );
        }
        return vi;
    }

    public void favouriteSquare(final int method, final Square square) {
        RequestQueue queue = Volley.newRequestQueue(activity);
        final String squareId = square.getId();
        final String userId = InSquareProfile.getUserId();
        String url = "http://recapp-insquare.rhcloud.com/favouritesquares?";
        url += "squareId=" + squareId;
        url += "&userId=" + userId;
        Log.d(TAG, "favouriteSquare: " + url);
        StringRequest postRequest = new StringRequest(method, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        updateView(method, square);
                        Log.d(TAG, "FAVOURITE response => " + response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "FAVOURITE error => "+error.toString());
                    }
                }
        );
        queue.add(postRequest);
    }

    public void updateView (int method, Square square) {

        if (method == Request.Method.DELETE) {
            InSquareProfile.removeFav(square.getId());
            notifyDataSetChanged();
        } else {
            InSquareProfile.addFav(square);
            notifyDataSetChanged();
        }
    }

    public static class ViewHolder {
        public TextView name;
        public TextView id;
    }
}
