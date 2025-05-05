package com.stadtiq; // <<< CHANGE THIS >>>

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log; // NEW: Import Log
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.google.android.material.navigation.NavigationView;

import java.util.Locale;
import android.app.AlertDialog;


public class ImpressumActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "ImpressumActivity"; // NEW: Define a TAG

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView impressumPlaceholderImage;

    private static final String PREF_LANG_CODE = "pref_language_code";

    @Override
    protected void attachBaseContext(Context base) {
        Log.d(TAG, "attachBaseContext"); // Log lifecycle
        String langCode = getLanguageCode(base);
        Context context = contextWithLocale(base, langCode);
        super.attachBaseContext(context);
    }

    private String getLanguageCode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lang = prefs.getString(PREF_LANG_CODE, "en");
        Log.d(TAG, "getLanguageCode: " + lang); // Log value
        return lang;
    }

    private Context contextWithLocale(Context context, String langCode) {
        Log.d(TAG, "contextWithLocale: " + langCode); // Log value
        Locale locale = new Locale(langCode);
        Configuration config = context.getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
            return context;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate"); // Log lifecycle
        setContentView(R.layout.activity_impressum);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.title_activity_impressum);
        }

        drawerLayout = findViewById(R.id.drawer_layout_impressum);
        navigationView = findViewById(R.id.nav_view_impressum);

        // --- Nav Drawer Highlighting ---
        navigationView.setCheckedItem(R.id.nav_impressum);


        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        navigationView.setNavigationItemSelectedListener(this);

        impressumPlaceholderImage = findViewById(R.id.logo_stadtiq_mit); // Use the actual ID of the ImageView

        Log.d(TAG, "onCreate: Completed setup"); // Log completion
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart"); // Log lifecycle
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume"); // Log lifecycle
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause"); // Log lifecycle
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop"); // Log lifecycle
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy"); // Log lifecycle
    }


    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed"); // Log event
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            Log.d(TAG, "onBackPressed: Drawer open, closing it");
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            Log.d(TAG, "onBackPressed: Drawer closed, calling super");
            super.onBackPressed();
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Log.d(TAG, "onNavigationItemSelected: id=" + getResources().getResourceEntryName(id)); // Log selected item

        Intent intent = null;

        // --- UPDATED: Check the Activity you are currently in to prevent restarting ---
        if (id == R.id.nav_home) {
            Log.d(TAG, "onNavigationItemSelected: Navigating to MainActivity"); // Log action
            // Check if already on MainActivity to avoid restarting it
            if (!(this instanceof com.stadtiq.MainActivity)) {
                intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            } else {
                Log.d(TAG, "onNavigationItemSelected: Already on MainActivity");
                navigationView.setCheckedItem(R.id.nav_home); // Keep checked
            }
        } else if (id == R.id.nav_verlauf) {
            Log.d(TAG, "onNavigationItemSelected: Navigating to VerlaufActivity"); // Log action
            // Check if already on VerlaufActivity to avoid restarting it
            if (!(this instanceof com.stadtiq.VerlaufActivity)) {
                intent = new Intent(this, VerlaufActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            } else {
                Log.d(TAG, "onNavigationItemSelected: Already on VerlaufActivity");
                navigationView.setCheckedItem(R.id.nav_verlauf); // Keep checked
            }
        } else if (id == R.id.nav_impressum) {
            Log.d(TAG, "onNavigationItemSelected: Navigating to ImpressumActivity (already here)"); // Log action
            navigationView.setCheckedItem(R.id.nav_impressum); // Keep checked
        } else if (id == R.id.nav_language) {
            Log.d(TAG, "onNavigationItemSelected: Showing language dialog"); // Log action
            showLanguageSelectionDialog();
        }

        if (intent != null) {
            startActivity(intent);
            Log.d(TAG, "onNavigationItemSelected: Started new activity"); // Log action
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        Log.d(TAG, "onNavigationItemSelected: Closing drawer"); // Log action
        return true;
    }

    private void showLanguageSelectionDialog() {
        Log.d(TAG, "showLanguageSelectionDialog");
        String[] languages = {
                getString(R.string.language_english),
                getString(R.string.language_german)
        };

        String currentLangCode = getLanguageCode(this);
        int checkedItem = 0;
        if ("de".equals(currentLangCode)) {
            checkedItem = 1;
        }
        Log.d(TAG, "showLanguageSelectionDialog: Current lang=" + currentLangCode + ", checkedItem=" + checkedItem);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_select_language_title);
        builder.setSingleChoiceItems(languages, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String selectedLangCode;
                if (which == 0) { // English
                    selectedLangCode = "en";
                } else { // German
                    selectedLangCode = "de";
                }
                Log.d(TAG, "Language dialog clicked: which=" + which + ", code=" + selectedLangCode);
                setLocale(selectedLangCode);
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Log.d(TAG, "Language dialog cancelled");
            dialog.dismiss();
        });

        builder.create().show();
        Log.d(TAG, "showLanguageSelectionDialog: Dialog shown");
    }

    private void setLocale(String langCode) {
        Log.d(TAG, "setLocale: langCode=" + langCode);
        String currentLangCode = getLanguageCode(this);

        if (!currentLangCode.equals(langCode)) {
            Log.d(TAG, "setLocale: Language changed from " + currentLangCode + " to " + langCode + ", saving and recreating");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_LANG_CODE, langCode);
            editor.apply();

            recreate();
        } else {
            Log.d(TAG, "setLocale: Language is the same (" + currentLangCode + "), no action needed");
        }
        Log.d(TAG, "setLocale: Completed");
    }
}