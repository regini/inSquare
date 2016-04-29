package com.nsqre.insquare.Activities;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Fade;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.nsqre.insquare.Fragments.BlankFragment;
import com.nsqre.insquare.Fragments.MainContent.FavSquaresFragment;
import com.nsqre.insquare.Fragments.MainContent.MapFragment;
import com.nsqre.insquare.Fragments.MainContent.ProfileFragment;
import com.nsqre.insquare.Fragments.MainContent.RecentSquaresFragment;
import com.nsqre.insquare.Fragments.MainContent.SettingsFragment;
import com.nsqre.insquare.Message.Message;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Services.ChatService;
import com.nsqre.insquare.Services.LocationService;
import com.nsqre.insquare.Square.RecyclerSquareAdapter;
import com.nsqre.insquare.Square.Square;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.BottomSheetMenu.BottomSheetItem;
import com.nsqre.insquare.Utilities.BottomSheetMenu.BottomSheetItemAdapter;
import com.nsqre.insquare.Utilities.DialogHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the main Activity for the app. It lets the user navigate through the several fragments of inSquare.
 */
public class BottomNavActivity extends AppCompatActivity implements BottomSheetItemAdapter.BottomSheetItemListener {

    private enum TABS
    {
        MAP, RECENT, PROFILE, OTHER
    }

    private static final String TAG = "BottomNavActivity";

    public static final String INITIALS_TAG = "INITIALS";
    public static final String INITIALS_COLOR_TAG = "INITIALS_COLOR";

    public static int[] backgroundColors = new int[]{
            R.color.md_amber_A100,
            R.color.md_orange_A100,
            R.color.colorAccent,
            R.color.md_purple_A100,
            R.color.md_deep_purple_A200,
            R.color.md_blue_100,
            R.color.md_teal_A400
    };

    public AHBottomNavigation bottomNavigation;
    public FrameLayout mainContentFrame;
    public CoordinatorLayout coordinatorLayout;

    private BottomSheetDialog bottomSheetDialog;
    private AHBottomNavigation.OnTabSelectedListener mTabSelectedListener;

    private InSquareProfile mProfile;

    /**
     * Sets up the buttons and loads the MapFragment: the first fragment which will be shown to the user.
     * Eventually it will start ChatService to send outgoing messages
     * @see MapFragment
     * @see ChatService
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottom_nav);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Fade fade = new Fade();
            fade.setDuration(1000);

            getWindow().setExitTransition(fade);
            getWindow().setEnterTransition(fade);

            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.logo_icon_144);
            ActivityManager.TaskDescription taskDesc =
                    new ActivityManager.TaskDescription(getString(R.string.app_name),
                            icon, Color.parseColor("#D32F2F"));
            setTaskDescription(taskDesc);
        }

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.bottom_nav_coordinator_layout);
        bottomNavigation = (AHBottomNavigation) findViewById(R.id.bottom_nav_bar);

        setupBottomNavigation();

        mainContentFrame = (FrameLayout) findViewById(R.id.bottom_nav_content_frame);

        getSupportFragmentManager().beginTransaction().replace(R.id.bottom_nav_content_frame, MapFragment.newInstance()).commitAllowingStateLoss();

        Intent intent = new Intent(this, LocationService.class);
        startService(intent);

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiver,
                new IntentFilter("update_squares"));

        //outgoing
        mProfile = InSquareProfile.getInstance(getApplicationContext());
        for (String squareid : mProfile.getOutgoingMessages().keySet()) {
            for (Message mess : mProfile.getOutgoingMessages().get(squareid)) {
                Intent chatIntent = new Intent(this, ChatService.class);
                chatIntent.putExtra("squareid", squareid);
                chatIntent.putExtra("message", mess);
                startService(chatIntent);
                Log.d(TAG, "onCreate: sto tentando l'invio di un messaggio che sta nella coda: {"
                        + squareid + ", " + mess.toString());
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if(intent.getStringExtra("squareId") != null && !"".equals(intent.getStringExtra("squareId"))) {
            MapFragment mapFragment = MapFragment.newInstance();
            if(!mapFragment.isAdded()) {
                mTabSelectedListener.onTabSelected(0, true);
                bottomNavigation.setCurrentItem(0);
            } else {
                mapFragment.checkActivityIntent(intent);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        calculateNotifications();
    }

    /**
     * The broadcast receiver which receives the notification and calls calculateNotification() to elaborate the view
     * @see #calculateNotifications()
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("event");
            Log.d("receiver", "Got message: " + message);
            if("update".equals(intent.getStringExtra("action"))) {
                calculateNotifications();
                InSquareProfile.getInstance(getApplicationContext());
                InSquareProfile.downloadAllSquares();
            }
        }
    };

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SettingsFragment.RC_SIGN_IN || requestCode == SettingsFragment.REQUEST_INVITE) {
            SettingsFragment fragment = SettingsFragment.newInstance();
            fragment.onActivityResult(requestCode, resultCode, data);
        } else if(requestCode == MapFragment.REQUEST_SQUARE && data != null)
        {
            MapFragment.newInstance().handleSquareCreation(resultCode, data);
        }else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Shows in the bottom bar how many notification are unread by the user
     */
    private void calculateNotifications() {
        if(bottomNavigation != null) {
            InSquareProfile.getInstance(getApplicationContext());
            SharedPreferences notificationPreferences = getSharedPreferences("NOTIFICATION_MAP", MODE_PRIVATE);
            int recents = 0;
            int favourites = 0;
            for(String k : notificationPreferences.getAll().keySet()) {
                if(InSquareProfile.isFav(k)) {
                    favourites += notificationPreferences.getInt(k,0);
                }
                if(InSquareProfile.isRecent(k)) {
                    recents += notificationPreferences.getInt(k,0);
                }
            }
            bottomNavigation.setNotification(favourites,2);
            bottomNavigation.setNotification(recents,3);
        }
    }

    /**
     * Creates the menu items for a given square
     * @param square
     * @return The menu list for that square
     */
    private List<BottomSheetItem> instantiateListMenu(Square square)
    {
        String shareString = getResources().getString(R.string.action_share);
        String muteString = getResources().getString(R.string.action_mute);
        String deleteString = getResources().getString(R.string.action_delete);
        ArrayList<BottomSheetItem> menuList = new ArrayList<>();
        if(InSquareProfile.isOwned(square.getId()))
        {
            menuList.add(new BottomSheetItem(R.drawable.ic_delete_black_48dp, deleteString));
        }
        menuList.add(new BottomSheetItem(R.drawable.ic_share_black_48dp, shareString));
        menuList.add(new BottomSheetItem(R.drawable.ic_sms_failed_black_24dp, muteString));

        return menuList;
    }

    /**
     * Sets up the icons for the bottom navigation bar and manages the tap on those buttons
     */
    private void setupBottomNavigation() {

        final String mapTabName = getString(R.string.bottom_nav_tab_map);
        final String mySquaresTabName = getString(R.string.bottom_nav_tab_created);
        final String favTabName = getString(R.string.bottom_nav_tab_favs);
        final String recentsTabName = getString(R.string.bottom_nav_tab_recent);
        final String otherTabName = getString(R.string.bottom_nav_tab_settings);

        AHBottomNavigationItem mapItem = new AHBottomNavigationItem(mapTabName, R.drawable.bottom_bar_map);
        AHBottomNavigationItem profileItem = new AHBottomNavigationItem(mySquaresTabName, R.drawable.ic_assignment_white_48dp);
        AHBottomNavigationItem favsItem = new AHBottomNavigationItem(favTabName, R.drawable.heart_white);
        AHBottomNavigationItem recentsItem = new AHBottomNavigationItem(recentsTabName, R.drawable.bottom_bar_recent);
        AHBottomNavigationItem othersItem = new AHBottomNavigationItem(otherTabName, R.drawable.ic_settings_white_48dp);

        bottomNavigation.removeAllItems();

        bottomNavigation.addItem(mapItem);
        bottomNavigation.addItem(profileItem);
        bottomNavigation.addItem(favsItem);
        bottomNavigation.addItem(recentsItem);
        bottomNavigation.addItem(othersItem);

        bottomNavigation.setColored(false);
        // Set background color
        bottomNavigation.setDefaultBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        bottomNavigation.setAccentColor(ContextCompat.getColor(this, R.color.white));

        bottomNavigation.setOnTabSelectedListener(
                new AHBottomNavigation.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(int position, boolean wasSelected) {
                        Fragment f;
                        mTabSelectedListener = this;

                        // position dipende dall'ordine di inserimento tramite bottomNavigation#addItem()
                        switch (position) {
                            case 0: // Selezionata Mappa
                                f = MapFragment.newInstance();
                                break;
                            case 1: // Selezionato Profilo
                                f = ProfileFragment.newInstance();
                                break;
                            case 2:
                                f = FavSquaresFragment.newInstance();
                                break;
                            case 3: // Selezionato Recenti
                                f = RecentSquaresFragment.newInstance();
                                break;
                            case 4: // Selezionato Altro
                                f = new SettingsFragment();
                                break;
                            default:
                                f = new BlankFragment();
                        }
                        getSupportFragmentManager().beginTransaction().replace(R.id.bottom_nav_content_frame, f).commit();
                    }
                });
        calculateNotifications();
    }

    /**
     * TODO documentare
     * @param square
     * @param adapter
     * @param viewHolderPosition
     */
    public void showBottomSheetDialog(Square square, RecyclerSquareAdapter adapter, int viewHolderPosition)
    {
        bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_menu, null);
        TextView dialogSquareName = (TextView) view.findViewById(R.id.bottom_sheet_name);
        dialogSquareName.setText(square.getName());
        final RecyclerView list = (RecyclerView) view.findViewById(R.id.bottom_sheet_list);
        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new BottomSheetItemAdapter(instantiateListMenu(square), this, adapter, viewHolderPosition));

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
        bottomSheetDialog.setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        bottomSheetDialog = null;
                        list.setAdapter(null);
                    }
                }
        );

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMessageReceiver);
    }

    /**
     * Manages the long press on a Square from the ProfileFragment, FavSquaresFragment or RecentSquaresFragment giving the user the possibility to share the Square
     * or to mute the notification for that particular square
     * @see ProfileFragment
     * @see FavSquaresFragment
     * @see RecentSquaresFragment
     */
    @Override
    public void onBottomMenuItemClick(BottomSheetItem item,
                                      final RecyclerSquareAdapter fragmentListElementAdapter,
                                      final int listHolderPosition
    )
    {
        if(bottomSheetDialog != null)
        {
            Resources res = getResources();
            final String shareString = res.getString(R.string.action_share);
            final String muteString = res.getString(R.string.action_mute);
            final String deleteString = res.getString(R.string.action_delete);

            if(item.getTitle().equals(shareString))
            {
                String text = "Vieni anche tu su InSquare! - https://play.google.com/store/apps/details?id=com.nsqre.insquare";
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, text);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "inSquare Sharing");
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Send To"));
            }
            if(item.getTitle().equals(muteString))
            {
                String squareId = fragmentListElementAdapter.squaresArrayList.get(listHolderPosition).getId();
                (new DialogHandler()).handleMuteRequest(this, coordinatorLayout, TAG, squareId);
            }
            if(item.getTitle().equals(deleteString))
            {
                fragmentListElementAdapter.removeElement(listHolderPosition);
            }


            Log.d(TAG, "onBottomMenuItemClick: I've just clicked " + item.getTitle());

            // TODO implementare menuitemclick
            bottomSheetDialog.dismiss();
        }
    }
    @Override
    public void onBackPressed() {
        this.finishAffinity();
    }
}
