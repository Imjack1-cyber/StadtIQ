package com.stadtiq;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.util.Locale;

public class ImpressumActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // TAG for logging, specific to this Activity
    private static final String TAG = "ImpressumActivity";

    // UI elements for Navigation Drawer
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    // Key for storing and retrieving language preference in SharedPreferences
    private static final String PREF_LANG_CODE = "pref_language_code";

    /**
     * Called before onCreate(), allows modification of the context this Activity runs in.
     * This is where we apply the user-selected locale.
     * @param base The base context provided by the system.
     */
    @Override
    protected void attachBaseContext(Context base) {
        // Log entry into the method
        Log.d(TAG, "attachBaseContext called.");
        // Retrieve the stored language code (e.g., "en", "de")
        String langCode = getLanguageCode(base);
        // Create a new context with the specified locale
        Context context = contextWithLocale(base, langCode);
        // Call the superclass method with the new locale-aware context
        super.attachBaseContext(context);
        // Log completion of the method
        Log.d(TAG, "attachBaseContext finished.");
    }

    /**
     * Retrieves the saved language code from SharedPreferences.
     * Defaults to "en" (English) if no preference is found.
     * @param context The context used to access SharedPreferences.
     * @return The language code string (e.g., "en", "de").
     */
    private String getLanguageCode(Context context) {
        // Get the default SharedPreferences for the application
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // Retrieve the language code, defaulting to "en" if not found
        String lang = prefs.getString(PREF_LANG_CODE, "en");
        // Log the retrieved language code
        Log.d(TAG, "getLanguageCode: Current language code is '" + lang + "'.");
        return lang;
    }

    /**
     * Creates a new Context with the specified language code applied.
     * This method handles locale changes differently based on Android API level.
     * @param context The original context.
     * @param langCode The language code to apply (e.g., "en", "de").
     * @return A new Context instance with the locale set.
     */
    private Context contextWithLocale(Context context, String langCode) {
        // Log the attempt to apply a new locale
        Log.d(TAG, "contextWithLocale: Applying language code '" + langCode + "'.");
        // Create a Locale object from the language code
        Locale locale = new Locale(langCode);
        // Set this locale as the default for the application
        Locale.setDefault(locale);

        // Get the current configuration from the context's resources
        Configuration config = new Configuration(context.getResources().getConfiguration());

        // Handle locale setting based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // For Android N (API 24) and above, use setLocale and createConfigurationContext
            config.setLocale(locale);
            Log.d(TAG, "contextWithLocale: Using createConfigurationContext for API >= N.");
            return context.createConfigurationContext(config);
        } else {
            // For versions older than Android N, directly set config.locale and update resources
            config.locale = locale;
            Log.d(TAG, "contextWithLocale: Using updateConfiguration for API < N.");
            // Update the resources with the new configuration
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

    /**
     * Displays a dialog for the user to select the application language.
     * Uses a custom layout (dialog_language_selection.xml) with RadioButtons.
     */
    private void showLanguageSelectionDialog() {
        // Log the initiation of the language selection dialog
        Log.d(TAG, "showLanguageSelectionDialog: Initiating language selection dialog.");

        // Inflate the custom layout for the dialog
        LayoutInflater inflater = LayoutInflater.from(this);
        View customDialogView = inflater.inflate(R.layout.dialog_language_selection, null);

        // Get references to the RadioGroup and RadioButtons from the custom layout
        final RadioGroup radioGroupLanguage = customDialogView.findViewById(R.id.radio_group_language);
        RadioButton radioButtonEnglish = customDialogView.findViewById(R.id.radio_button_english);
        RadioButton radioButtonGerman = customDialogView.findViewById(R.id.radio_button_german);

        // Determine the currently selected language to pre-select the correct radio button
        String currentLangCode = getLanguageCode(this);
        Log.d(TAG, "showLanguageSelectionDialog: Current language code is '" + currentLangCode + "'.");

        // Pre-select the radio button corresponding to the current language
        if ("de".equals(currentLangCode)) {
            radioButtonGerman.setChecked(true);
            Log.d(TAG, "showLanguageSelectionDialog: German radio button pre-selected.");
        } else {
            radioButtonEnglish.setChecked(true); // Default to English
            Log.d(TAG, "showLanguageSelectionDialog: English radio button pre-selected.");
        }

        // Use MaterialAlertDialogBuilder for a Material Design styled dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.dialog_select_language_title); // Set the title
        builder.setView(customDialogView); // Set the custom inflated view

        // Set the "OK" or "Confirm" button action
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            int selectedRadioButtonId = radioGroupLanguage.getCheckedRadioButtonId();
            String selectedLangCode = "en"; // Default to English

            if (selectedRadioButtonId == R.id.radio_button_german) {
                selectedLangCode = "de";
                Log.d(TAG, "showLanguageSelectionDialog: German selected via RadioButton.");
            } else if (selectedRadioButtonId == R.id.radio_button_english) {
                selectedLangCode = "en";
                Log.d(TAG, "showLanguageSelectionDialog: English selected via RadioButton.");
            } else {
                Log.w(TAG, "showLanguageSelectionDialog: No radio button selected, defaulting to 'en'. This should not happen if one is pre-checked.");
            }

            // Apply the selected language
            Log.i(TAG, "showLanguageSelectionDialog: Positive button clicked. Attempting to set locale to '" + selectedLangCode + "'.");
            setLocale(selectedLangCode);
            dialog.dismiss(); // Close the dialog
        });

        // Set the "Cancel" button action
        builder.setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> {
            Log.d(TAG, "showLanguageSelectionDialog: Language selection dialog cancelled by user.");
            dialog.dismiss(); // Close the dialog
        });

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
        Log.d(TAG, "showLanguageSelectionDialog: Language selection dialog shown.");
    }

    /**
     * Sets the application's locale to the given language code.
     * If the language changes, it persists the new preference and recreates the Activity.
     * @param langCode The language code to set (e.g., "en", "de").
     */
    private void setLocale(String langCode) {
        // Log the attempt to set a new locale
        Log.i(TAG, "setLocale: Attempting to set language to '" + langCode + "'.");

        // Retrieve the current language code to compare
        String currentLangCode = getLanguageCode(this);
        Log.d(TAG, "setLocale: Current language code is '" + currentLangCode + "'.");

        // Check if the selected language is different from the current language
        if (!currentLangCode.equals(langCode)) {
            // If different, proceed with changing the language
            Log.d(TAG, "setLocale: Language is changing from '" + currentLangCode + "' to '" + langCode + "'.");

            // Get SharedPreferences to persist the new language choice
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            // Edit SharedPreferences: put the new language code and apply the changes
            prefs.edit().putString(PREF_LANG_CODE, langCode).apply();
            Log.d(TAG, "setLocale: New language code '" + langCode + "' saved to SharedPreferences.");

            // Recreate the activity to apply the new language resources
            Log.i(TAG, "setLocale: Recreating activity to apply language change.");
            recreate();
        } else {
            // If the selected language is the same as the current one, no action is needed
            Log.d(TAG, "setLocale: Language is already set to '" + currentLangCode + "'. No action needed.");
        }
    }

    // Standard lifecycle logging methods
    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
}