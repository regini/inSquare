package com.nsqre.insquare.Utilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.nsqre.insquare.User.InSquareProfile;

import java.io.InputStream;

/**
 * Created by emanu on 27/02/2016.
 */
public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    ImageView bmImage;
    Context context;

    public DownloadImageTask(ImageView bmImage, Context c) {
        this.bmImage = bmImage;
        this.context = c;
    }

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
        return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {
        Bitmap circularBitmap = ImageConverter.getRoundedCornerBitmap(result, 100);
        bmImage.setImageBitmap(circularBitmap);
        InSquareProfile.saveToInternalStorage(context, circularBitmap);
    }
}