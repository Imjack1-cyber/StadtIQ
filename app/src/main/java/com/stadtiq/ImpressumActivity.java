package com.stadtiq;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;

import java.util.Locale;

public class ImpressumActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "ImpressumActivity";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private static final String PREF_LANG_CODE = "pref_language_code";

    @Override
    protected void attachBaseContext(Context base) {
        Log.d(TAG, "attachBaseContext called.");
        String langCode = getLanguageCode(base);
        Context context = contextWithLocale(base, langCode);
        super.attachBaseContext(context);
        Log.d(TAG, "attachBaseContext finished.");
    }

    private String getLanguageCode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lang = prefs.getString(PREF_LANG_CODE, "en");
        Log.d(TAG, "getLanguageCode: Current language code is '" + lang + "'.");
        return lang;
    }

    private Context contextWithLocale(Context context, String langCode) {
        Log.d(TAG, "contextWithLocale: Applying language code '" + langCode + "'.");
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            Log.d(TAG, "contextWithLocale: Using createConfigurationContext for API >= N.");
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            Log.d(TAG, "contextWithLocale: Using updateConfiguration for API < N.");
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
            return context;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: Activity starting.");
        setContentView(R.layout.activity_impressum);
        Log.d(TAG, "onCreate: ContentView set.");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Log.d(TAG, "onCreate: Toolbar setup.");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.title_activity_impressum);
            Log.d(TAG, "onCreate: ActionBar title set.");
        }

        drawerLayout = findViewById(R.id.drawer_layout_impressum);
        navigationView = findViewById(R.id.nav_view_impressum);
        Log.d(TAG, "onCreate: DrawerLayout and NavigationView found.");

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        if (drawerLayout != null) drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        if (navigationView != null) navigationView.setNavigationItemSelectedListener(this);
        Log.d(TAG, "onCreate: ActionBarDrawerToggle and NavigationView listener setup.");
        Log.i(TAG, "onCreate: Finished.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity Resumed.");
        if (navigationView != null) {
            MenuItem impressumItem = navigationView.getMenu().findItem(R.id.nav_impressum);
            if (impressumItem != null) {
                Log.d(TAG, "onResume: Setting 'Impressum' as checked. Current status before: " + impressumItem.isChecked());
                navigationView.setCheckedItem(R.id.nav_impressum);
                Log.d(TAG, "onResume: Nav drawer item 'Impressum' isChecked after set: " + impressumItem.isChecked());
            } else {
                Log.w(TAG, "onResume: nav_impressum menu item is null.");
            }
        } else {
            Log.w(TAG, "onResume: NavigationView is null, cannot set checked item.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options_menu, menu);
        Log.d(TAG, "onCreateOptionsMenu: Menu inflated.");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: Item ID " + item.getItemId() + " (" + getResources().getResourceEntryName(item.getItemId()) + ") selected.");
        if (item.getItemId() == R.id.action_language) {
            Log.d(TAG, "onOptionsItemSelected: Language action selected.");
            showLanguageSelectionDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: Called.");
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            Log.d(TAG, "onBackPressed: Drawer is open, closing drawer.");
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            Log.d(TAG, "onBackPressed: Drawer is closed, calling super.onBackPressed().");
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        String itemName = getResources().getResourceEntryName(id);
        Log.i(TAG, "onNavigationItemSelected: Item '" + itemName + "' (ID: " + id + ") selected.");
        Intent intent = null;

        if (navigationView == null || drawerLayout == null) {
            Log.e(TAG, "onNavigationItemSelected: NavigationView or DrawerLayout is null, cannot proceed.");
            return false;
        }

        if (id == R.id.nav_home) {
            Log.d(TAG, "onNavigationItemSelected: Navigating to MainActivity.");
            if (!this.getClass().equals(MainActivity.class)) {
                intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            } else {
                Log.d(TAG, "onNavigationItemSelected: Already on MainActivity.");
            }
        } else if (id == R.id.nav_verlauf) {
            Log.d(TAG, "onNavigationItemSelected: Navigating to VerlaufActivity.");
            if (!this.getClass().equals(VerlaufActivity.class)) {
                intent = new Intent(this, VerlaufActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            } else {
                Log.d(TAG, "onNavigationItemSelected: Already on VerlaufActivity.");
            }
        } else if (id == R.id.nav_impressum) {
            Log.d(TAG, "onNavigationItemSelected: Impressum selected (current activity).");
            navigationView.setCheckedItem(R.id.nav_impressum);
        }

        if (intent != null) {
            Log.d(TAG, "onNavigationItemSelected: Starting new activity: " + intent.getComponent().getClassName());
            startActivity(intent);
        } else {
            Log.d(TAG, "onNavigationItemSelected: No new activity started or staying on current page.");
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showLanguageSelectionDialog() {
        Log.d(TAG, "showLanguageSelectionDialog: Called.");
        String[] languages = {getString(R.string.language_english), getString(R.string.language_german)};
        String currentLangCode = getLanguageCode(this);
        int checkedItem = "de".equals(currentLangCode) ? 1 : 0;
        Log.d(TAG, "showLanguageSelectionDialog: Current lang=" + currentLangCode + ", checkedItem=" + checkedItem);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_select_language_title);
        builder.setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
            String selectedLangCode = (which == 0) ? "en" : "de";
            Log.d(TAG, "Language dialog clicked: which=" + which + ", code=" + selectedLangCode);
            setLocale(selectedLangCode);
            dialog.dismiss();
        });
        builder.setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> {
            Log.d(TAG, "Language dialog cancelled");
            dialog.dismiss();
        });
        builder.create().show();
        Log.d(TAG, "showLanguageSelectionDialog: Dialog shown.");
    }

    private void setLocale(String langCode) {
        Log.i(TAG, "setLocale: Attempting to set language to '" + langCode + "'.");
        String currentLangCode = getLanguageCode(this);
        if (!currentLangCode.equals(langCode)) {
            Log.d(TAG, "setLocale: Language changing from '" + currentLangCode + "' to '" + langCode + "'. Saving and recreating activity.");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putString(PREF_LANG_CODE, langCode).apply();
            recreate();
        } else {
            Log.d(TAG, "setLocale: Language is already '" + currentLangCode + "'. No action needed.");
        }
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
}