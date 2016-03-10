package com.nsqre.insquare.Utilities;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.nsqre.insquare.Activities.MapActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by emanu on 27/02/2016.
 */
public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    ImageView bmImage;
    MapActivity activity;

    public DownloadImageTask(ImageView bmImage, MapActivity mapActivity) {
        this.bmImage = bmImage;
        this.activity = mapActivity;
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
        activity.saveToInternalStorage(circularBitmap);
    }
}