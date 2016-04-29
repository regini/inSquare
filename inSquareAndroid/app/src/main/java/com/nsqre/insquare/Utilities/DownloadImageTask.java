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
 * Downloads and locally stores an image
 */
public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    ImageView bmImage;
    Context context;

    public DownloadImageTask(ImageView bmImage, Context c) {
        this.bmImage = bmImage;
        this.context = c;
    }

    /**
     * Downloads an image from a link
     * @param urls the link of the image
     * @return a bitmap
     * @see #onPostExecute(Bitmap)
     */
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

    /**
     * Resizes the image into a circular one with 100px radius and stores it locally
     * @param result the image downloaded
     */
    protected void onPostExecute(Bitmap result) {
        Bitmap circularBitmap = ImageConverter.getRoundedCornerBitmap(result, 100);
        bmImage.setImageBitmap(circularBitmap);
        InSquareProfile.saveToInternalStorage(context, circularBitmap);
    }
}