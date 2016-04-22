package com.nsqre.insquare.Fragments.MainContent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nsqre.insquare.R;
import com.nsqre.insquare.Square.RecyclerProfileSquareAdapter;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.DownloadImageTask;
import com.nsqre.insquare.Utilities.ImageConverter;

/**
 * This is the fragment that show the user's Profile. In it you can find information about the user:
 * his name, his photo and the lists of squares created and favoured
 */
public class ProfileFragment extends Fragment implements
        InSquareProfile.InSquareProfileListener
{

    private static ProfileFragment instance;

    private static final String TAG = "ProfileFragment";

    private static final String TAB_OWNED = "Create";
    private static final String TAB_FAVOURITE = "Preferite";

    private RecyclerView squaresRecyclerView;
    private RecyclerProfileSquareAdapter adapterOwned;
    private ImageView profileImage;
    private TextView username, emptyText;
    private TabLayout tabLayout;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance() {
        if(instance == null){
            instance = new ProfileFragment();
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
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        squaresRecyclerView = (RecyclerView) v.findViewById(R.id.recyclerview_squares_owned);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        squaresRecyclerView.setLayoutManager(linearLayoutManager);



        adapterOwned = new RecyclerProfileSquareAdapter(getContext(), InSquareProfile.getOwnedSquaresList());
        squaresRecyclerView.setAdapter(adapterOwned);

        setupSwipeGesture();

        profileImage = (ImageView) v.findViewById(R.id.profile_profile_image);
        emptyText = (TextView) v.findViewById(R.id.profile_text_empty);

        setupProfile();

        return v;
    }

    private void setupProfile() {
        Bitmap icon = BitmapFactory.decodeResource(getContext().getResources(),
                R.drawable.logo_icon_144);
        Bitmap circularBitmap = ImageConverter.getRoundedCornerBitmap(icon, 100);
        profileImage.setImageBitmap(circularBitmap);

        Bitmap bitmap = InSquareProfile.loadProfileImageFromStorage(getContext());
        if (bitmap == null) {
            if (!InSquareProfile.getPictureUrl().equals(""))
                new DownloadImageTask(profileImage, getContext()).execute(InSquareProfile.getPictureUrl());
        } else {
            profileImage.setImageBitmap(bitmap);
        }
    }

    private void setupSwipeGesture()
    {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT)
        {
            Drawable background;
            Drawable trashCan;
            float rightMargin;
            boolean isTouchHelperCreated;

            private void instantiateDrawables()
            {
                int backgroundColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
                background = new ColorDrawable(backgroundColor);
                trashCan = ContextCompat.getDrawable(getContext(), R.drawable.ic_delete_white_48dp);
                rightMargin = getContext().getResources().getDimension(R.dimen.activity_vertical_margin);
                isTouchHelperCreated = true;
            }

            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                // TODO implement wait while pending undo
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            // Drag & Drop Handler
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            // Chiamato al termine del Swipe Gesture
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction)
            {
                int viewPosition = viewHolder.getAdapterPosition();
                adapterOwned.pendingRemoval(viewPosition);
//                Log.d(TAG, "onSwiped: swiped from " + direction);
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState,
                                    boolean isCurrentlyActive) {
                if(viewHolder.getAdapterPosition() < 0)
                {
                    return;
                } else if(!isTouchHelperCreated)
                {
                    instantiateDrawables();
                }
                if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;

                    float width = itemView.getWidth();
                    float delta = 1.0f - Math.abs(dX) / width;
                    itemView.setAlpha(delta * delta);
                    itemView.setTranslationX(dX);

//                    Log.d(TAG, "onChildDraw: delta " + delta + " and width " + dX);

                    if (delta < 1) {
                        // Sfondo rosso
                        background.setBounds(
                                itemView.getRight() + (int) dX,
                                itemView.getTop(),
                                itemView.getRight(),
                                itemView.getBottom()
                        );
                        background.draw(c);

                        // Immagine Cestino
                        int itemHeight = itemView.getBottom() - itemView.getTop();
                        int trashWidth = (trashCan.getIntrinsicWidth()/3)*2;
                        int trashHeight = trashWidth;
                        int trashLeft = (int) (itemView.getRight() - rightMargin - trashWidth);
                        int trashRight = (int) (itemView.getRight() - rightMargin);
                        int trashTop = itemView.getTop() + (itemHeight - trashHeight)/2;
                        int trashBottom = trashTop + trashHeight;

                        trashCan.setBounds(trashLeft, trashTop, trashRight, trashBottom);
                        trashCan.draw(c);

                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }


        };

        ItemTouchHelper swipeTouchHelper = new ItemTouchHelper(swipeCallback);
        swipeTouchHelper.attachToRecyclerView(squaresRecyclerView);
//        squaresRecyclerView.addItemDecoration(new RecyclerProfileSquareDecoration(getContext()));
    }

    /**
     * TODO ??
     */
    @Override
    public void onStart() {
        super.onStart();
        InSquareProfile.addListener(this);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(mMessageReceiver,
                new IntentFilter("update_squares"));
        InSquareProfile.downloadAllSquares();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(this.adapterOwned.getItemCount() == 0)
        {
            squaresRecyclerView.setVisibility(View.INVISIBLE);
            emptyText.setVisibility(View.VISIBLE);

            String message = getString(R.string.profile_empty_owned);
            emptyText.setText(message);
        }else
        {
            squaresRecyclerView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        InSquareProfile.removeListener(this);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(mMessageReceiver);
    }

    /**
     * TODO ???
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String message = intent.getStringExtra("event");
            Log.d(TAG, "Got message: " + message);
            if("deletion".equals(intent.getStringExtra("action"))) {
                String squareId = intent.getExtras().getString("squareId");
                if(InSquareProfile.isOwned(squareId)) {
                    InSquareProfile.removeOwned(squareId);
                }
                if(InSquareProfile.isFav(squareId)) {
                    InSquareProfile.removeFav(squareId);
                }
                if(InSquareProfile.isRecent(squareId)) {
                    InSquareProfile.removeRecent(squareId);
                }
            }
        }
    };

    @Override
    public void onOwnedChanged() {
        Log.d(TAG, "onOwnedChanged!");
        adapterOwned.notifyDataSetChanged();
    }

    @Override
    public void onFavChanged() {
    }

    @Override
    public void onRecentChanged() {
        Log.d(TAG, "onRecentChanged!");
    }
}
