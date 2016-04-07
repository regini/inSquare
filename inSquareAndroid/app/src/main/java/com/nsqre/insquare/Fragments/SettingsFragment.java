package com.nsqre.insquare.Fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nsqre.insquare.R;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.DownloadImageTask;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * This is the fragment that show the user's Profile. In it you can find information about the user:
 * his name, his photo and the lists of squares created and favoured
 */
public class SettingsFragment extends Fragment implements
        InSquareProfile.InSquareProfileListener
{

    private static SettingsFragment instance;

    private static final String TAG = "SettingsFragment";

    private CircleImageView userAvatar;
    private TextView username;

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance() {
        if(instance == null){
            instance = new SettingsFragment();
        }
        return instance;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    /**
     * Initialized the view of this fragment setting the lists of favourite and owned squares
     * and the profile image(downloading it if not saved in the local storage)
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return The view created
     * @see DownloadImageTask
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        setupProfile(v);

        username = (TextView) v.findViewById(R.id.settings_top_username);
        username.setText(InSquareProfile.getUsername());

        return v;
    }

    private void setupProfile(View layout) {
        userAvatar = (CircleImageView) layout.findViewById(R.id.settings_top_user_avatar);

        Bitmap bitmap = InSquareProfile.loadProfileImageFromStorage(getContext());
        if (bitmap == null) {
            if (!InSquareProfile.getPictureUrl().equals(""))
                new DownloadImageTask(userAvatar, getContext()).execute(InSquareProfile.getPictureUrl());
        } else {
            userAvatar.setImageBitmap(bitmap);
        }
    }

    /**
     * TODO ??
     */
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        InSquareProfile.removeListener(this);
    }

    @Override
    public void onOwnedChanged() {
        Log.d(TAG, "onOwnedChanged!");
    }

    @Override
    public void onFavChanged() {
    }

    @Override
    public void onRecentChanged() {
        Log.d(TAG, "onRecentChanged!");
    }
}
