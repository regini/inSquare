/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nsqre.insquare.Utilities.PushNotification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.nsqre.insquare.Activities.LoginActivity;
import com.nsqre.insquare.R;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "From: " + from);

        if (from.startsWith("/topics/global")) {
            String event = data.getString("event");
            String userId = data.getString("userId");
            String squareId = data.getString("squareId");
            Log.d(TAG, event);
            updateSquares(userId, squareId, event);
        } else {
            String message = data.getString("message");
            String squareName = data.getString("squareName");
            String squareId = data.getString("squareId");
            sendNotification(message, squareName, squareId);
        }
    }
    // [END receive_message]

    private void updateSquares(String userId, String squareId, String event) {
        Intent intent = new Intent("update_squares");
        intent.putExtra("event", "update_squares");
        intent.putExtra("action", event);
        intent.putExtra("userId", userId);
        intent.putExtra("squareId", squareId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message, String squareName, String squareId) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("profile", 2);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);
        SharedPreferences sharedPreferences = getSharedPreferences("NOTIFICATION_MAP", MODE_PRIVATE);
        int notificationCount = 0;
        int squareCount = sharedPreferences.getInt("squareCount", 0);

        if(squareId.equals(sharedPreferences.getString("actualSquare",""))) {
            return;
        }

        if(!sharedPreferences.contains(squareId)) {
            sharedPreferences.edit().putInt("squareCount", squareCount + 1).apply();
        }
        sharedPreferences.edit().putInt(squareId, sharedPreferences.getInt(squareId, 0) + 1).apply();

        for(String square : sharedPreferences.getAll().keySet()) {
            if(!"squareCount".equals(square) && !"actualSquare".equals(square)) {
                notificationCount += sharedPreferences.getInt(square, 0);
            }
        }

        squareCount = sharedPreferences.getInt("squareCount", 0);

        Log.d(TAG, sharedPreferences.getAll().toString());

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.nsqre_map_pin_empty_inside)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
                .setContentTitle(notificationCount > 1 ? "inSquare" : squareName)
                .setContentText(notificationCount > 1 ? "Hai " + (notificationCount) + " nuovi messaggi in " + squareCount
                        + " piazze" : message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setVibrate(new long[] { 300, 300, 300, 300, 300 })
                .setLights(Color.RED, 1000, 3000)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
        updateSquares("","","update");
    }

    public static class QuickstartPreferences {

        public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";
        public static final String REGISTRATION_COMPLETE = "registrationComplete";

    }
}
