package com.nsqre.insquare.Square;

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
import com.nsqre.insquare.Activities.ChatActivity;
import com.nsqre.insquare.Activities.MapActivity;
import com.nsqre.insquare.Fragments.MapFragment;
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

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
                    intent.putExtra(MapFragment.SQUARE_TAG, square);
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
            SharedPreferences messagePreferences = activity.getSharedPreferences(square.getId(), Context.MODE_PRIVATE);
            messagePreferences.edit().clear().apply();
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

        VolleyManager.getInstance().handleFavoriteSquare(method, square.getId(), InSquareProfile.getUserId(),
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        // method e' POST o DELETE
                    }

                    @Override
                    public void responsePOST(Object object) {
                        if(object == null)
                        {
                            //La richiesta e' fallita
                            Log.d(TAG, "responsePOST - non sono riuscito ad inserire il fav " + square.toString());
                        }else
                        {
                            notifyDataSetChanged();
                            InSquareProfile.addFav(square);
                        }
                    }

                    @Override
                    public void responsePATCH(Object object) {
                        // method e' POST o DELETE
                    }

                    @Override
                    public void responseDELETE(Object object) {
                        if(object == null)
                        {
                            //La richiesta e' fallita
                            Log.d(TAG, "responseDELETE - non sono riuscito ad rimuovere il fav " + square.toString());
                        }else
                        {
                            notifyDataSetChanged();
                            InSquareProfile.removeFav(square.getId());
                        }
                    }
                });
    }

    public static class ViewHolder {
        public TextView name;
        public TextView id;
    }
}
