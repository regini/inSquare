package com.nsqre.insquare.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import com.nsqre.insquare.R;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";

    private LinearLayout linearLayout;
    private FloatingActionButton mapFab;
    private float startTouchY;
    private Animation animationUp, animationDown;

    // Variabili per l'inizializzazione della Chat
    public static final String SQUARE_ID_TAG = "SQUARE_URL";
    public static final String SQUARE_NAME_TAG = "SQUARE_NAME";

    private String mSquareId;
    private String mSquareName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mapFab = (FloatingActionButton) findViewById(R.id.map_fab);
        mapFab.setVisibility(View.GONE);

        mapFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapActivity.this, ChatActivity.class);

                intent.putExtra(SQUARE_ID_TAG, mSquareId);
                intent.putExtra(SQUARE_NAME_TAG, mSquareName);
                startActivity(intent);
            }
        });

        linearLayout = (LinearLayout) findViewById(R.id.slider_ll);
        linearLayout.setVisibility(View.GONE);
        animationUp = AnimationUtils.loadAnimation(this, R.anim.anim_up);
        animationDown = AnimationUtils.loadAnimation(this, R.anim.anim_down);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                startTouchY = event.getY();
            case MotionEvent.ACTION_UP:
                float endTouchY = event.getY();

                if(endTouchY < startTouchY)
                {
                    Log.d(TAG, "Moving Up");
//                    linearLayout.setVisibility(View.VISIBLE);
//                    linearLayout.startAnimation(animationUp);
                }else {
                    Log.d(TAG, "Moving Down!");
//                    linearLayout.setVisibility(View.GONE);
//                    linearLayout.startAnimation(animationDown);
                }
        }

        return super.onTouchEvent(event);
    }

    public void setSquareName(String squareName) {
        this.mSquareName = squareName;
    }

    public void setSquareId(String mSquareId) {
        this.mSquareId = mSquareId;
    }

}
