package com.nsqre.insquare.Activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialog;
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
import com.nsqre.insquare.Utilities.BottomSheetMenu.BottomSheetItem;
import com.nsqre.insquare.Utilities.BottomSheetMenu.BottomSheetItemAdapter;

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

    AHBottomNavigation bottomNavigation;
    FrameLayout mainContentFrame;

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

        bottomNavigation = (AHBottomNavigation) findViewById(R.id.bottom_nav_bar);

        setupBottomNavigation();

        mainContentFrame = (FrameLayout) findViewById(R.id.bottom_nav_content_frame);

        getSupportFragmentManager().beginTransaction().replace(R.id.bottom_nav_content_frame, MapFragment.newInstance()).commit();

    }

    private List<BottomSheetItem> instantiateListMenu()
    {
        String shareString = getResources().getString(R.string.share_action);
        String editString = getResources().getString(R.string.edit_action);
        String muteString = getResources().getString(R.string.mute_action);
        String deleteString = getResources().getString(R.string.delete_action);
        ArrayList<BottomSheetItem> menuList = new ArrayList<BottomSheetItem>();
//        menuList.add(new BottomSheetItem(R.drawable.ic_mode_edit_black_48dp, "Modifica"));
        menuList.add(new BottomSheetItem(R.drawable.ic_share_black_48dp, shareString));
        menuList.add(new BottomSheetItem(R.drawable.ic_volume_off_black_48dp, "Muto"));
//        menuList.add(new BottomSheetItem(R.drawable.ic_delete_black_48dp, "Elimina"));

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

        bottomNavigation.setBehaviorTranslationEnabled(false);
        bottomNavigation.setColored(false);
        // Set background color
        bottomNavigation.setDefaultBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
        bottomNavigation.setAccentColor(ContextCompat.getColor(getApplicationContext(), R.color.white));

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

    public void showBottomSheetDialog(String name)
    {
        bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_menu, null);
        TextView dialogSquareName = (TextView) view.findViewById(R.id.bottom_sheet_name);
        dialogSquareName.setText(name);
        final RecyclerView list = (RecyclerView) view.findViewById(R.id.bottom_sheet_list);
        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new BottomSheetItemAdapter(instantiateListMenu(), this));

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
    public void onBottomMenuItemClick(BottomSheetItem item) {
        if(bottomSheetDialog != null)
        {
            String shareString = getResources().getString(R.string.share_action);
            if(item.getTitle() == shareString)
            {
                String text = "Vieni anche tu su InSquare! - https://play.google.com/store/apps/details?id=com.nsqre.insquare";
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, text);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "inSquare Sharing");
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Send To"));
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
