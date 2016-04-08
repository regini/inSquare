package com.nsqre.insquare.Activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import com.nsqre.insquare.Fragments.FavSquaresFragment;
import com.nsqre.insquare.Fragments.MapFragment;
import com.nsqre.insquare.Fragments.ProfileFragment;
import com.nsqre.insquare.Fragments.RecentSquaresFragment;
import com.nsqre.insquare.Fragments.SettingsFragment;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Square.RecyclerSquareAdapter;
import com.nsqre.insquare.Square.Square;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.BottomSheetMenu.BottomSheetItem;
import com.nsqre.insquare.Utilities.BottomSheetMenu.BottomSheetItemAdapter;
import com.nsqre.insquare.Utilities.DialogHandler;

import java.util.ArrayList;
import java.util.List;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottom_nav);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Fade fade = new Fade();
            fade.setDuration(1000);

            getWindow().setExitTransition(fade);
            getWindow().setEnterTransition(fade);
        }
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.bottom_nav_coordinator_layout);
        bottomNavigation = (AHBottomNavigation) findViewById(R.id.bottom_nav_bar);

        setupBottomNavigation();

        mainContentFrame = (FrameLayout) findViewById(R.id.bottom_nav_content_frame);

        getSupportFragmentManager().beginTransaction().replace(R.id.bottom_nav_content_frame, MapFragment.newInstance()).commit();

    }

    private List<BottomSheetItem> instantiateListMenu(Square square)
    {
        String shareString = getResources().getString(R.string.action_share);
        String muteString = getResources().getString(R.string.action_mute);
        String deleteString = getResources().getString(R.string.action_delete);
        ArrayList<BottomSheetItem> menuList = new ArrayList<BottomSheetItem>();
        if(InSquareProfile.isOwned(square.getId()))
        {
            menuList.add(new BottomSheetItem(R.drawable.ic_delete_black_48dp, deleteString));
        }
        menuList.add(new BottomSheetItem(R.drawable.ic_share_black_48dp, shareString));
        menuList.add(new BottomSheetItem(R.drawable.ic_volume_off_black_48dp, muteString));

        return menuList;
    }


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
    }

    public static String setupInitials(String words) {
        String[] division = words.split("\\s+");

        if(division.length <= 1)
        {
            return words.substring(0,1).toUpperCase();
        }
        else if(division.length == 2)
        {
            return division[0].substring(0,1).toUpperCase() + division[1].substring(0,1).toUpperCase();
        }
        else
        {
            return division[0].substring(0,1).toUpperCase() + division[1].substring(0,1).toUpperCase() + division[2].substring(0, 1).toUpperCase();
        }
    }

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
    }

    /*
        Questo e' il click dell'oggetto nel menu che viene istanziato sul long click di una Square nella lista
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
