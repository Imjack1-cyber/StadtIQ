package com.stadtiq;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // TAG for logging, specific to this Activity
    private static final String TAG = "MainActivity";

    // UI elements
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Spinner spinnerCity;
    private Spinner spinnerDistrict;
    private FrameLayout displayAreaContainer;
    private TextView txtPlaceholderMessage;
    private LinearLayout listViewContainer;
    private RecyclerView recyclerViewValues;
    private ImageButton btnListLayoutToggle;

    // Data structures for cities, districts, and dummy environmental data
    private List<String> cities;
    private Map<String, List<String>> districts;
    private Map<String, Map<String, Map<String, String>>> dummyData;

    // Adapter for the RecyclerView displaying values
    private ValueAdapter valueAdapter;

    // Key for storing and retrieving language preference
    private static final String PREF_LANG_CODE = "pref_language_code";
    // State for list layout (1 or 2 columns)
    private int currentListLayoutColumns = 1;
    // Tracks the language code to detect changes on resume/restart
    public String lastKnownLangCode = null; // Made public for setLocale, consider alternatives


    // --- CONSTANT DATA KEYS (used for dummyData and identifying values) ---
    private static final String KEY_CO2 = "CO₂";
    private static final String KEY_PM25 = "PM2,5";
    private static final String KEY_O2 = "O₂";
    private static final String KEY_SO2 = "SO₂";
    private static final String KEY_CH4 = "CH₄";
    private static final String KEY_P = "p";
    private static final String KEY_LP = "lp";
    private static final String KEY_LX = "lx";
    private static final String KEY_TD = "Td";
    private static final String KEY_ABS_HUMIDITY = "ABS_HUMIDITY_KEY";

    // List of all data keys used to populate the value list
    private final List<String> ALL_VALUE_DATA_KEYS = Arrays.asList(
            KEY_CO2, KEY_PM25, KEY_O2, KEY_SO2, KEY_CH4, KEY_P, KEY_LP, KEY_LX, KEY_TD, KEY_ABS_HUMIDITY
    );
    // --- END CONSTANT DATA KEYS ---

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
        String lang = prefs.getString(PREF_LANG_CODE, "en"); // Default to "en"
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

        // Store the initial language code. This is used in onRestart/onResume
        // to detect if the language changed while the activity was not in the foreground.
        lastKnownLangCode = getLanguageCode(this);
        Log.d(TAG, "onCreate: Initial lastKnownLangCode set to '" + lastKnownLangCode + "'.");

        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: ContentView (R.layout.activity_main) set.");

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Log.d(TAG, "onCreate: Toolbar (R.id.toolbar) setup as ActionBar.");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name); // Set title from string resource
            Log.d(TAG, "onCreate: ActionBar title set to app_name string resource.");
        }

        // Setup Navigation Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        Log.d(TAG, "onCreate: DrawerLayout (R.id.drawer_layout) and NavigationView (R.id.nav_view) found.");

        // ActionBarDrawerToggle ties together the DrawerLayout and Toolbar
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, // Accessibility string for opening drawer
                R.string.navigation_drawer_close); // Accessibility string for closing drawer
        if (drawerLayout != null) drawerLayout.addDrawerListener(toggle);
        toggle.syncState(); // Synchronize the indicator with the state of the linked DrawerLayout
        if (navigationView != null) navigationView.setNavigationItemSelectedListener(this);
        Log.d(TAG, "onCreate: ActionBarDrawerToggle and NavigationView listener setup.");

        // Initialize other UI elements
        spinnerCity = findViewById(R.id.spinner_city);
        spinnerDistrict = findViewById(R.id.spinner_district);
        displayAreaContainer = findViewById(R.id.display_area_container);
        txtPlaceholderMessage = findViewById(R.id.txt_placeholder_message);
        Log.d(TAG, "onCreate: Basic UI elements (spinners, displayAreaContainer, txtPlaceholderMessage) found. displayAreaContainer: " + (displayAreaContainer != null));

        // Initialize elements within the list view container
        listViewContainer = findViewById(R.id.list_view_container);
        if (listViewContainer != null) {
            recyclerViewValues = listViewContainer.findViewById(R.id.recycler_view_values);
            btnListLayoutToggle = listViewContainer.findViewById(R.id.btn_list_layout_toggle);
            Log.d(TAG, "onCreate: ListViewContainer and its children (RecyclerView, ToggleButton) found.");
        } else {
            Log.e(TAG, "onCreate: listViewContainer (R.id.list_view_container) is NULL! This is critical for list display.");
        }

        // Initialize data (dummy data, city/district lists)
        // These methods might use string resources, so they are sensitive to the current locale
        // and should be called after the locale is set in attachBaseContext.
        initializeDummyData();
        initializeCityDistrictData();
        initializeRecyclerView(); // Setup RecyclerView and its adapter
        Log.d(TAG, "onCreate: Data (dummy, city/district) and RecyclerView initialized.");

        // Setup listeners and adapters for spinners and buttons
        setupCitySpinner();
        setupDistrictSpinner();
        setupListLayoutToggleButton();
        Log.d(TAG, "onCreate: Spinners and list layout toggle button listeners setup.");

        // Perform initial data load based on default spinner selections
        String initialCity = cities.get(spinnerCity.getSelectedItemPosition());
        Log.i(TAG, "onCreate: Initial city from spinner is '" + initialCity + "'. Triggering initial data display.");
        handleCityOrDistrictChange(); // This will update the main view (list)
        Log.i(TAG, "onCreate: Finished.");
    }

    /**
     * Called when the activity is restarting after being stopped.
     * Checks if the language has changed (e.g., via settings in another app or activity)
     * and recreates the activity if necessary to apply the new language.
     */
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: Activity Restarted.");
        // Get the current language code
        String currentLangCode = getLanguageCode(this);
        // Check if lastKnownLangCode was initialized and if it differs from the current one
        if (lastKnownLangCode != null && !lastKnownLangCode.equals(currentLangCode)) {
            Log.i(TAG, "onRestart: Language has changed from '" + lastKnownLangCode + "' to '" + currentLangCode + "'. Recreating MainActivity.");
            lastKnownLangCode = currentLangCode; // Update lastKnownLangCode before recreating
            recreate(); // Recreate the activity to apply the new language
        } else if (lastKnownLangCode == null) {
            // This case should ideally not happen if onCreate sets it, but as a fallback:
            lastKnownLangCode = currentLangCode;
            Log.w(TAG, "onRestart: lastKnownLangCode was null, set to '" + currentLangCode + "'.");
        }
    }


    /**
     * Called when the activity will start interacting with the user.
     * Ensures the correct navigation drawer item is selected ("Home").
     * Also checks for language changes if onRestart didn't catch it (e.g. if activity was paused, not stopped).
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity Resumed.");
        // Check if language changed while this activity was paused, if not handled by onRestart
        String currentLangCode = getLanguageCode(this);
        if (lastKnownLangCode != null && !lastKnownLangCode.equals(currentLangCode)) {
            Log.i(TAG, "onResume: Language changed from '" + lastKnownLangCode + "' to '" + currentLangCode + "' while paused. Recreating MainActivity.");
            lastKnownLangCode = currentLangCode; // Update before recreating
            recreate(); // Recreate to apply new language
            return; // Avoid further onResume processing as activity will restart
        } else if (lastKnownLangCode == null) {
            // Fallback if lastKnownLangCode wasn't set (e.g., if onCreate was skipped in some edge case)
            lastKnownLangCode = currentLangCode;
            Log.w(TAG, "onResume: lastKnownLangCode was null, set to '" + currentLangCode + "'.");
        }

        // Set the "Home" item in the navigation drawer as checked
        if (navigationView != null) {
            MenuItem homeItem = navigationView.getMenu().findItem(R.id.nav_home);
            if (homeItem != null) {
                Log.d(TAG, "onResume: Setting 'Home' (R.id.nav_home) as checked. Current status before: " + homeItem.isChecked());
                navigationView.setCheckedItem(R.id.nav_home); // Mark "Home" as selected
                // Note: setCheckedItem might not immediately reflect in isChecked() if the menu is complex or uses action views.
                // For simple menus, it's usually reliable for visual state.
                Log.d(TAG, "onResume: Nav drawer item 'Home' isChecked after set (may not update immediately for complex items): " + homeItem.isChecked());
            } else {
                Log.w(TAG, "onResume: nav_home menu item (R.id.nav_home) is null. Cannot set checked.");
            }
        } else {
            Log.w(TAG, "onResume: NavigationView is null, cannot set checked item.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options_menu, menu);
        Log.d(TAG, "onCreateOptionsMenu: Options menu (R.menu.main_options_menu) inflated.");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Log which item was selected from the options menu (e.g., Toolbar menu)
        // Using getResourceEntryName to get a human-readable name for the item ID.
        Log.d(TAG, "onOptionsItemSelected: Item ID " + item.getItemId() + " (" + getResources().getResourceEntryName(item.getItemId()) + ") selected.");
        if (item.getItemId() == R.id.action_language) {
            Log.d(TAG, "onOptionsItemSelected: Language action (R.id.action_language) selected.");
            showLanguageSelectionDialog(); // Display the language selection dialog
            return true; // Event was handled
        }
        // If the item is not one we explicitly handle, pass it to the superclass
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: Called.");
        // If the navigation drawer is open, close it
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            Log.d(TAG, "onBackPressed: Drawer is open, closing drawer.");
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Otherwise, perform the default back button behavior (e.g., finish activity)
            Log.d(TAG, "onBackPressed: Drawer is closed or null, calling super.onBackPressed().");
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handles item selections from the navigation drawer
        int id = item.getItemId(); // Get the ID of the selected item
        // Get the resource entry name for logging (e.g., "nav_home")
        String itemName = getResources().getResourceEntryName(id);
        Log.i(TAG, "onNavigationItemSelected: Item '" + itemName + "' (ID: " + id + ") selected from navigation drawer.");

        Intent intent = null; // Intent to start a new activity, if any

        // Ensure NavigationView and DrawerLayout are not null before proceeding
        if (navigationView == null || drawerLayout == null) {
            Log.e(TAG, "onNavigationItemSelected: NavigationView or DrawerLayout is null. Cannot proceed with navigation.");
            return false; // Indicate that the event was not fully handled
        }

        // Determine action based on the selected item ID
        if (id == R.id.nav_home) {
            Log.d(TAG, "onNavigationItemSelected: Home (R.id.nav_home) selected. This is the current activity.");
            navigationView.setCheckedItem(R.id.nav_home); // Ensure "Home" remains checked
        } else if (id == R.id.nav_verlauf) {
            Log.d(TAG, "onNavigationItemSelected: Navigating to VerlaufActivity (R.id.nav_verlauf).");
            intent = new Intent(this, VerlaufActivity.class);
            // FLAG_ACTIVITY_REORDER_TO_FRONT brings an existing instance to the front if available,
            // instead of creating a new one.
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        } else if (id == R.id.nav_impressum) {
            Log.d(TAG, "onNavigationItemSelected: Navigating to ImpressumActivity (R.id.nav_impressum).");
            intent = new Intent(this, ImpressumActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }

        // If an intent was created, start the new activity
        if (intent != null) {
            Log.d(TAG, "onNavigationItemSelected: Starting new activity: " + intent.getComponent().getClassName());
            startActivity(intent);
        } else {
            Log.d(TAG, "onNavigationItemSelected: No new activity started (likely already on the selected page or no intent created).");
        }

        // Close the navigation drawer
        drawerLayout.closeDrawer(GravityCompat.START);
        return true; // Indicate that the navigation event was handled
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

            // Update lastKnownLangCode BEFORE recreating. This ensures that onResume/onRestart
            // in the NEW activity instance correctly sees the change and doesn't try to recreate again.
            this.lastKnownLangCode = langCode;
            Log.d(TAG, "setLocale: Updated lastKnownLangCode to '" + langCode + "' before recreate.");

            // Recreate the activity to apply the new language resources
            Log.i(TAG, "setLocale: Recreating activity to apply language change.");
            recreate();
        } else {
            // If the selected language is the same as the current one, no action is needed
            Log.d(TAG, "setLocale: Language is already set to '" + currentLangCode + "'. No action needed.");
        }
    }

    /**
     * Initializes dummy data for environmental readings.
     * This data is hardcoded and used for demonstration purposes.
     * The data structure is: City -> District -> {ValueKey: ReadingString}
     */
    private void initializeDummyData() {
        Log.d(TAG, "initializeDummyData: Starting with distinct fixed district data for demonstration.");
        dummyData = new HashMap<>(); // Main map holding data for all cities

        // --- Data for Braunschweig ---
        Map<String, Map<String, String>> braunschweigCityData = new HashMap<>(); // Data for districts in Braunschweig

        // Data for "Innere Stadt" district
        Map<String, String> innerStadt = new HashMap<>();
        innerStadt.put(KEY_CO2, "480 ppm"); innerStadt.put(KEY_PM25, "28 µg/m³"); innerStadt.put(KEY_O2, "20.6 %");
        innerStadt.put(KEY_SO2, "7.5 ppb"); innerStadt.put(KEY_CH4, "2.2 ppm"); innerStadt.put(KEY_P, "1010.0 hPa");
        innerStadt.put(KEY_LP, "75 dB");    innerStadt.put(KEY_LX, "700 lx");    innerStadt.put(KEY_TD, "9.5 °C");
        innerStadt.put(KEY_ABS_HUMIDITY, "10.0 g/m³");
        braunschweigCityData.put("Innere Stadt", innerStadt); // Add to Braunschweig's district data

        // Data for "Westliches Ringgebiet" district
        Map<String, String> westRing = new HashMap<>();
        westRing.put(KEY_CO2, "440 ppm"); westRing.put(KEY_PM25, "20 µg/m³"); westRing.put(KEY_O2, "20.8 %");
        westRing.put(KEY_SO2, "5.5 ppb"); westRing.put(KEY_CH4, "1.95 ppm"); westRing.put(KEY_P, "1012.0 hPa");
        westRing.put(KEY_LP, "68 dB");    westRing.put(KEY_LX, "550 lx");     westRing.put(KEY_TD, "8.8 °C");
        westRing.put(KEY_ABS_HUMIDITY, "9.0 g/m³");
        braunschweigCityData.put("Westliches Ringgebiet", westRing);

        // Data for "Östliches Ringgebiet" district
        Map<String, String> ostRing = new HashMap<>();
        ostRing.put(KEY_CO2, "425 ppm"); ostRing.put(KEY_PM25, "16 µg/m³"); ostRing.put(KEY_O2, "20.88 %");
        ostRing.put(KEY_SO2, "4.8 ppb"); ostRing.put(KEY_CH4, "1.88 ppm"); ostRing.put(KEY_P, "1012.9 hPa");
        ostRing.put(KEY_LP, "62 dB");    ostRing.put(KEY_LX, "490 lx");      ostRing.put(KEY_TD, "8.3 °C");
        ostRing.put(KEY_ABS_HUMIDITY, "8.8 g/m³");
        braunschweigCityData.put("Östliches Ringgebiet", ostRing);

        // Data for "Südliches Ringgebiet" district
        Map<String, String> suedRing = new HashMap<>();
        suedRing.put(KEY_CO2, "435 ppm"); suedRing.put(KEY_PM25, "19 µg/m³"); suedRing.put(KEY_O2, "20.82 %");
        suedRing.put(KEY_SO2, "5.1 ppb"); suedRing.put(KEY_CH4, "1.92 ppm"); suedRing.put(KEY_P, "1012.2 hPa");
        suedRing.put(KEY_LP, "66 dB");    suedRing.put(KEY_LX, "530 lx");     suedRing.put(KEY_TD, "8.5 °C");
        suedRing.put(KEY_ABS_HUMIDITY, "8.9 g/m³");
        braunschweigCityData.put("Südliches Ringgebiet", suedRing);

        // Data for "Weststadt" district
        Map<String, String> weststadt = new HashMap<>();
        weststadt.put(KEY_CO2, "412 ppm"); weststadt.put(KEY_PM25, "11 µg/m³"); weststadt.put(KEY_O2, "20.91 %");
        weststadt.put(KEY_SO2, "3.3 ppb"); weststadt.put(KEY_CH4, "1.78 ppm"); weststadt.put(KEY_P, "1013.5 hPa");
        weststadt.put(KEY_LP, "57 dB");    weststadt.put(KEY_LX, "430 lx");    weststadt.put(KEY_TD, "7.8 °C");
        weststadt.put(KEY_ABS_HUMIDITY, "8.3 g/m³");
        braunschweigCityData.put("Weststadt", weststadt);

        // Data for "Heidberg-Melverode" district
        Map<String, String> heidberg = new HashMap<>();
        heidberg.put(KEY_CO2, "402 ppm"); heidberg.put(KEY_PM25, "8 µg/m³"); heidberg.put(KEY_O2, "20.97 %");
        heidberg.put(KEY_SO2, "2.7 ppb"); heidberg.put(KEY_CH4, "1.71 ppm"); heidberg.put(KEY_P, "1014.2 hPa");
        heidberg.put(KEY_LP, "51 dB");    heidberg.put(KEY_LX, "370 lx");    heidberg.put(KEY_TD, "7.3 °C");
        heidberg.put(KEY_ABS_HUMIDITY, "7.8 g/m³");
        braunschweigCityData.put("Heidberg-Melverode", heidberg);

        // Data for "Südstadt" district
        Map<String, String> suedstadt = new HashMap<>();
        suedstadt.put(KEY_CO2, "418 ppm"); suedstadt.put(KEY_PM25, "13 µg/m³"); suedstadt.put(KEY_O2, "20.90 %");
        suedstadt.put(KEY_SO2, "3.9 ppb"); suedstadt.put(KEY_CH4, "1.81 ppm"); suedstadt.put(KEY_P, "1013.1 hPa");
        suedstadt.put(KEY_LP, "59 dB");    suedstadt.put(KEY_LX, "460 lx");     suedstadt.put(KEY_TD, "7.9 °C");
        suedstadt.put(KEY_ABS_HUMIDITY, "8.4 g/m³");
        braunschweigCityData.put("Südstadt", suedstadt);

        // Data for "Bebelhof" district
        Map<String, String> bebelhof = new HashMap<>();
        bebelhof.put(KEY_CO2, "428 ppm"); bebelhof.put(KEY_PM25, "17 µg/m³"); bebelhof.put(KEY_O2, "20.86 %");
        bebelhof.put(KEY_SO2, "4.9 ppb"); bebelhof.put(KEY_CH4, "1.89 ppm"); bebelhof.put(KEY_P, "1012.6 hPa");
        bebelhof.put(KEY_LP, "64 dB");    bebelhof.put(KEY_LX, "510 lx");     bebelhof.put(KEY_TD, "8.4 °C");
        bebelhof.put(KEY_ABS_HUMIDITY, "8.85 g/m³");
        braunschweigCityData.put("Bebelhof", bebelhof);

        // Data for "Nördliches Ringgebiet" district
        Map<String, String> nordRing = new HashMap<>();
        nordRing.put(KEY_CO2, "433 ppm"); nordRing.put(KEY_PM25, "19 µg/m³"); nordRing.put(KEY_O2, "20.83 %");
        nordRing.put(KEY_SO2, "5.3 ppb"); nordRing.put(KEY_CH4, "1.91 ppm"); nordRing.put(KEY_P, "1012.3 hPa");
        nordRing.put(KEY_LP, "67 dB");    nordRing.put(KEY_LX, "540 lx");     nordRing.put(KEY_TD, "8.6 °C");
        nordRing.put(KEY_ABS_HUMIDITY, "8.95 g/m³");
        braunschweigCityData.put("Nördliches Ringgebiet", nordRing);

        // Data for "Braunschweig-Nord" district
        Map<String, String> bsNord = new HashMap<>();
        bsNord.put(KEY_CO2, "408 ppm"); bsNord.put(KEY_PM25, "9 µg/m³"); bsNord.put(KEY_O2, "20.94 %");
        bsNord.put(KEY_SO2, "3.0 ppb"); bsNord.put(KEY_CH4, "1.73 ppm"); bsNord.put(KEY_P, "1013.9 hPa");
        bsNord.put(KEY_LP, "54 dB");    bsNord.put(KEY_LX, "400 lx");    bsNord.put(KEY_TD, "7.6 °C");
        bsNord.put(KEY_ABS_HUMIDITY, "8.0 g/m³");
        braunschweigCityData.put("Braunschweig-Nord", bsNord);

        // Placeholder data for the "Select District" hint option
        Map<String, String> hintDistrictData = new HashMap<>();
        hintDistrictData.put(KEY_CO2, "400 ppm"); hintDistrictData.put(KEY_PM25, "7 µg/m³"); hintDistrictData.put(KEY_O2, "20.98 %");
        hintDistrictData.put(KEY_SO2, "2.5 ppb"); hintDistrictData.put(KEY_CH4, "1.7 ppm"); hintDistrictData.put(KEY_P, "1014.5 hPa");
        hintDistrictData.put(KEY_LP, "50 dB");    hintDistrictData.put(KEY_LX, "300 lx");    hintDistrictData.put(KEY_TD, "7.0 °C");
        hintDistrictData.put(KEY_ABS_HUMIDITY, "7.5 g/m³");
        // Uses getString() for the key to ensure it matches the localized hint in the spinner
        braunschweigCityData.put(getString(R.string.hint_select_district), hintDistrictData);

        // Add Braunschweig's data to the main dummyData map
        dummyData.put("Braunschweig", braunschweigCityData);
        Log.d(TAG, "initializeDummyData: Finished. Braunschweig data includes " + braunschweigCityData.size() + " district entries (including hint).");
    }


    /**
     * Initializes the lists of cities and their corresponding districts.
     * These are used to populate the City and District spinners.
     * Uses string resources for localization.
     */
    private void initializeCityDistrictData() {
        Log.d(TAG, "initializeCityDistrictData: Starting to populate city and district lists.");
        // Initialize the list of cities
        cities = new ArrayList<>();
        cities.add(getString(R.string.hint_select_city)); // "Please select..." hint
        cities.add("Braunschweig"); // Currently, only Braunschweig is supported

        // Initialize the map of districts for each city
        districts = new HashMap<>();
        // Add a default district list for the "select city" hint
        districts.put(getString(R.string.hint_select_city), Arrays.asList(getString(R.string.hint_select_district)));
        // Add districts for Braunschweig
        districts.put("Braunschweig", Arrays.asList(
                getString(R.string.hint_select_district), // "Please select..." hint
                "Innere Stadt", "Westliches Ringgebiet", "Östliches Ringgebiet", "Südliches Ringgebiet",
                "Weststadt", "Heidberg-Melverode", "Südstadt", "Bebelhof",
                "Nördliches Ringgebiet", "Braunschweig-Nord"
        ));
        Log.d(TAG, "initializeCityDistrictData: Finished. Cities: " + cities.size() + ", District map entries: " + districts.size());
    }

    /**
     * Initializes the RecyclerView for displaying environmental values.
     * Sets up its LayoutManager and Adapter.
     * Attaches an item click listener to show detailed explanations.
     */
    private void initializeRecyclerView() {
        Log.d(TAG, "initializeRecyclerView: Starting setup of RecyclerView for value list.");
        if (recyclerViewValues == null) {
            Log.e(TAG, "initializeRecyclerView: recyclerViewValues (R.id.recycler_view_values) is NULL. Cannot setup RecyclerView.");
            return;
        }
        // Set a LinearLayoutManager by default (single column)
        recyclerViewValues.setLayoutManager(new LinearLayoutManager(this));
        // Create and set the adapter with an empty list initially
        valueAdapter = new ValueAdapter(new ArrayList<>());
        recyclerViewValues.setAdapter(valueAdapter);

        // Set a click listener for items in the RecyclerView
        valueAdapter.setOnItemClickListener((itemView, position) -> {
            Log.d(TAG, "RecyclerView item clicked at position: " + position);
            // Retrieve the ValueItem object associated with the clicked position
            ValueItem clickedItem = valueAdapter.getValueItem(position);
            if (clickedItem != null) {
                Log.d(TAG, "Clicked ValueItem: Display Name='" + clickedItem.getDisplayableName() + "', Data Key='" + clickedItem.getDataKey() + "', Reading='" + clickedItem.getReading() + "'.");
                // Get the screen location of the clicked item view to position the popup
                int[] location = new int[2];
                itemView.getLocationOnScreen(location);
                // Show the speech bubble popup with explanation
                showValueExplanationPopup(clickedItem, location[0], location[1], itemView.getWidth(), itemView.getHeight());
            } else {
                Log.w(TAG, "Clicked item at position " + position + " is null. Cannot show explanation popup.");
            }
        });
        Log.d(TAG, "initializeRecyclerView: Finished setup. LayoutManager and Adapter set. ItemClickListener attached.");
    }

    /**
     * Sets up the City spinner with an ArrayAdapter and an item selection listener.
     * When a city is selected, it updates the District spinner and refreshes the data view.
     */
    private void setupCitySpinner() {
        Log.d(TAG, "setupCitySpinner: Starting setup for City spinner (R.id.spinner_city).");
        // Create an ArrayAdapter using the simple spinner layout and the list of cities
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cities);
        // Specify the layout to use when the list of choices appears
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinnerCity.setAdapter(cityAdapter);

        // Set the listener for when an item is selected in the city spinner
        spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCity = (String) parent.getItemAtPosition(position);
                Log.i(TAG, "City Spinner: Item selected - '" + selectedCity + "' at position " + position + ".");
                // Update the district spinner based on the selected city
                updateDistrictSpinner(selectedCity);
                // Handle the change in selection to update the displayed data
                handleCityOrDistrictChange();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "City Spinner: Nothing selected.");
                // Optionally, handle the case where no city is selected, though usually one item is pre-selected.
            }
        });
        Log.d(TAG, "setupCitySpinner: Finished. Adapter and OnItemSelectedListener set.");
    }

    /**
     * Sets up the District spinner.
     * It's initially populated based on the default selected city.
     * An item selection listener triggers a data view refresh.
     */
    private void setupDistrictSpinner() {
        Log.d(TAG, "setupDistrictSpinner: Starting setup for District spinner (R.id.spinner_district).");
        // Get the initially selected city from the city spinner
        String initialCity = cities.get(spinnerCity.getSelectedItemPosition());
        // Populate the district spinner based on this initial city
        updateDistrictSpinner(initialCity);

        // Set the listener for when an item is selected in the district spinner
        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // When a district is selected, handle the change to update the displayed data
                String selectedDistrict = (String) parent.getItemAtPosition(position);
                Log.i(TAG, "District Spinner: Item selected - '" + selectedDistrict + "' at position " + position + ".");
                handleCityOrDistrictChange();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "District Spinner: Nothing selected.");
            }
        });
        Log.d(TAG, "setupDistrictSpinner: Finished. Initial population and OnItemSelectedListener set.");
    }

    /**
     * Updates the District spinner's content based on the selected city.
     * @param selectedCity The city for which to display districts.
     */
    private void updateDistrictSpinner(String selectedCity) {
        Log.d(TAG, "updateDistrictSpinner: Updating district spinner for city '" + selectedCity + "'.");
        // Get the list of districts for the selected city
        List<String> districtList = districts.get(selectedCity);

        // If no districts are found for the selected city (e.g., for the "select city" hint),
        // use a default list containing only the "select district" hint.
        if (districtList == null) {
            Log.w(TAG, "updateDistrictSpinner: No districts found for city '" + selectedCity + "'. Using default hint list.");
            districtList = new ArrayList<>(Arrays.asList(getString(R.string.hint_select_district)));
        }

        // Create an ArrayAdapter for the district spinner
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districtList);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the district spinner
        spinnerDistrict.setAdapter(districtAdapter);
        // Set the selection to the first item (usually the "select district" hint)
        spinnerDistrict.setSelection(0);
        Log.d(TAG, "updateDistrictSpinner: Adapter set for district spinner. List size: " + districtList.size() + ". First item: '" + (districtList.isEmpty() ? "EMPTY" : districtList.get(0)) + "'.");
    }

    /**
     * Sets up the click listener for the list layout toggle button (single/grid view).
     */
    private void setupListLayoutToggleButton() {
        Log.d(TAG, "setupListLayoutToggleButton: Starting setup for list layout toggle button (R.id.btn_list_layout_toggle).");
        if (btnListLayoutToggle == null) {
            Log.e(TAG, "setupListLayoutToggleButton: btnListLayoutToggle is NULL. Cannot set up listener.");
            return;
        }
        // Set the initial selected state of the button based on currentListLayoutColumns
        // setSelected(true) typically means grid/2-column view.
        btnListLayoutToggle.setSelected(currentListLayoutColumns == 2);
        // Set the click listener to toggle the layout
        btnListLayoutToggle.setOnClickListener(v -> {
            Log.d(TAG, "List layout toggle button clicked.");
            toggleListLayout();
        });
        Log.d(TAG, "setupListLayoutToggleButton: Finished. Initial state set to " + (currentListLayoutColumns == 2 ? "grid (2 columns)" : "list (1 column)") + " and OnClickListener attached.");
    }

    /**
     * Toggles the RecyclerView layout between a single-column list and a two-column grid.
     * Updates the `currentListLayoutColumns` state and the button's selected appearance.
     */
    private void toggleListLayout() {
        Log.d(TAG, "toggleListLayout: Current columns = " + currentListLayoutColumns + ". Toggling now.");
        // Ensure RecyclerView and the toggle button are not null
        if (recyclerViewValues == null || btnListLayoutToggle == null) {
            Log.e(TAG, "toggleListLayout: RecyclerView or ToggleButton is null. Cannot change layout.");
            return;
        }

        // Toggle between 1 and 2 columns
        if (currentListLayoutColumns == 1) {
            currentListLayoutColumns = 2; // Switch to 2 columns (grid)
            recyclerViewValues.setLayoutManager(new GridLayoutManager(this, 2));
            btnListLayoutToggle.setSelected(true); // Update button state to "selected" (grid view active)
            Log.d(TAG, "toggleListLayout: Switched to 2 columns (GridLayoutManager). Button selected: true.");
        } else {
            currentListLayoutColumns = 1; // Switch to 1 column (list)
            recyclerViewValues.setLayoutManager(new LinearLayoutManager(this));
            btnListLayoutToggle.setSelected(false); // Update button state to "unselected" (list view active)
            Log.d(TAG, "toggleListLayout: Switched to 1 column (LinearLayoutManager). Button selected: false.");
        }
        // Note: The adapter does not need to be reset, only the LayoutManager.
    }

    /**
     * Updates the main content view (the list of values) based on current selections.
     * If a city is not properly selected, it shows a placeholder message.
     * Otherwise, it configures the RecyclerView layout and updates its data.
     */
    private void updateMainView() {
        Log.i(TAG, "updateMainView: Updating main content area (list view or placeholder).");

        // Get selected city from the spinner
        String selectedCity = cities.get(spinnerCity.getSelectedItemPosition());
        String selectedDistrict = null;

        // Get the list of districts for the currently selected city
        List<String> currentDistrictList = districts.get(selectedCity);

        // Determine the selected district name, handling potential out-of-bounds or null list
        if (currentDistrictList != null && spinnerDistrict.getSelectedItemPosition() >= 0 &&
                spinnerDistrict.getSelectedItemPosition() < currentDistrictList.size()) {
            selectedDistrict = currentDistrictList.get(spinnerDistrict.getSelectedItemPosition());
        } else if (currentDistrictList != null && !currentDistrictList.isEmpty()){
            // Fallback: if position is invalid, default to the first district in the list (often the hint)
            selectedDistrict = currentDistrictList.get(0);
            Log.w(TAG, "updateMainView: District spinner position invalid or list changed. Defaulting to first item: '" + selectedDistrict + "'.");
        } else {
            // This case means there's no district list for the selected city, which is problematic.
            Log.e(TAG, "updateMainView: currentDistrictList is null or empty for city: '" + selectedCity + "'. Showing placeholder.");
            showPlaceholderMessageForList(); // Show placeholder as data cannot be determined
            return;
        }

        Log.d(TAG, "updateMainView: Current selections - City='" + selectedCity + "', District='" + selectedDistrict + "'.");

        // Check if the "select city" hint is currently chosen
        if (getString(R.string.hint_select_city).equals(selectedCity)) {
            Log.d(TAG, "updateMainView: No city properly selected (hint is shown). Displaying placeholder message for the list.");
            showPlaceholderMessageForList(); // Show placeholder message
        } else {
            // A proper city is selected, proceed to show the list view
            Log.d(TAG, "updateMainView: City '" + selectedCity + "' selected. Preparing to show list for district '" + selectedDistrict + "'.");
            // Hide the placeholder message
            if (txtPlaceholderMessage != null) txtPlaceholderMessage.setVisibility(View.GONE);
            // Show the list view container
            if (listViewContainer != null) {
                listViewContainer.setVisibility(View.VISIBLE);
                // Ensure the RecyclerView's LayoutManager is correctly set based on currentListLayoutColumns
                if (recyclerViewValues != null) {
                    if (currentListLayoutColumns == 1) {
                        recyclerViewValues.setLayoutManager(new LinearLayoutManager(this));
                    } else {
                        recyclerViewValues.setLayoutManager(new GridLayoutManager(this, 2));
                    }
                    Log.d(TAG, "updateMainView: RecyclerView layout set to " + currentListLayoutColumns + " column(s).");
                }
            }
            // Update the list view with data for the selected city and district
            updateListViewWithCalculatedData(selectedCity, selectedDistrict);
        }
    }

    /**
     * Shows a placeholder message in the main content area and hides the list view.
     * This is used when a city isn't selected or data isn't available.
     */
    private void showPlaceholderMessageForList() {
        Log.i(TAG, "showPlaceholderMessageForList: Displaying placeholder message for the list area.");
        if (txtPlaceholderMessage != null) {
            // Set the text for the placeholder (e.g., "Please select a city...")
            txtPlaceholderMessage.setText(R.string.placeholder_select_city_for_list);
            txtPlaceholderMessage.setVisibility(View.VISIBLE); // Make the placeholder visible
            Log.d(TAG, "showPlaceholderMessageForList: Placeholder TextView text set and visibility set to VISIBLE.");
        }
        if (listViewContainer != null) {
            listViewContainer.setVisibility(View.GONE); // Hide the list container
            Log.d(TAG, "showPlaceholderMessageForList: ListViewContainer visibility set to GONE.");
        }
        // Clear any existing data from the adapter to ensure the list is empty
        if (valueAdapter != null) {
            valueAdapter.updateData(new ArrayList<>()); // Update adapter with an empty list
            Log.d(TAG, "showPlaceholderMessageForList: ValueAdapter data cleared.");
        }
    }


    /**
     * Handles changes in City or District selection by updating the main view.
     * This method serves as a central point for reacting to spinner changes.
     */
    private void handleCityOrDistrictChange() {
        Log.i(TAG, "handleCityOrDistrictChange: City or District selection has changed. Triggering view update.");
        updateMainView(); // Calls the method to refresh the list or show placeholder
    }

    /**
     * Updates the RecyclerView (list view) with calculated/dummy data for the given city and district.
     * @param city The selected city name.
     * @param district The selected district name.
     */
    private void updateListViewWithCalculatedData(String city, String district) {
        Log.i(TAG, "updateListViewWithCalculatedData: Updating list for City='" + city + "', District='" + district + "'.");
        List<ValueItem> dataForList = new ArrayList<>(); // List to hold ValueItem objects for the adapter

        if (valueAdapter == null) {
            Log.e(TAG, "updateListViewWithCalculatedData: valueAdapter is NULL. Cannot update list data.");
            return;
        }

        // If the "select city" hint is chosen, show an empty list.
        if (getString(R.string.hint_select_city).equals(city)) {
            Log.d(TAG, "updateListViewWithCalculatedData: 'Select city' hint is active. Showing empty list.");
            valueAdapter.updateData(dataForList); // Pass empty list to adapter
            return;
        }

        // Retrieve data for the selected city from the dummyData map
        Map<String, Map<String, String>> cityData = dummyData.get(city);
        Log.d(TAG, "updateListViewWithCalculatedData: Fetched cityData for '" + city + "'. Found: " + (cityData != null ? "yes (contains " + cityData.size() + " district entries)" : "no"));

        Map<String, String> districtSpecificReadings = null;
        if (cityData != null) {
            // Check if the "select district" hint is chosen for the district
            if (district != null && district.equals(getString(R.string.hint_select_district))) {
                // If hint is selected, use the data associated with the hint key in dummyData
                districtSpecificReadings = cityData.get(getString(R.string.hint_select_district));
                Log.d(TAG, "updateListViewWithCalculatedData: 'Select district' hint is active. Using hint-specific data.");
            } else if (district != null && cityData.containsKey(district)) {
                // If a specific district is selected, get its data
                districtSpecificReadings = cityData.get(district);
                Log.d(TAG, "updateListViewWithCalculatedData: Specific district '" + district + "' selected. Using its data.");
            } else {
                // Fallback: if district is null or not found, use data for the hint (or could be an error state)
                districtSpecificReadings = cityData.get(getString(R.string.hint_select_district));
                Log.w(TAG, "updateListViewWithCalculatedData: District '" + district + "' not found or null. Fallback to hint data.");
            }
        }

        // If no readings found for the district (even after fallback), use an empty map to avoid NullPointerExceptions
        if (districtSpecificReadings == null) {
            Log.w(TAG, "updateListViewWithCalculatedData: No data found for City='" + city + "', District='" + district + "'. Using empty readings map.");
            districtSpecificReadings = new HashMap<>(); // Prevent NPE
        }

        // Iterate through all known data keys (CO2, PM2.5, etc.) to create ValueItem objects
        for (String dataKey : ALL_VALUE_DATA_KEYS) {
            // Get the reading for the current dataKey, or "N/A" if not found
            String reading = districtSpecificReadings.getOrDefault(dataKey, "N/A");
            // Get the displayable (potentially localized) name for the dataKey
            String displayName = getDisplayableNameForKey(dataKey);
            // Create a new ValueItem and add it to the list
            dataForList.add(new ValueItem(displayName, reading, dataKey));
            Log.v(TAG, "updateListViewWithCalculatedData: Added to list: Key='" + dataKey + "', DisplayName='" + displayName + "', Reading='" + reading + "'.");
        }

        // Update the RecyclerView's adapter with the new list of data
        valueAdapter.updateData(dataForList);
        Log.d(TAG, "updateListViewWithCalculatedData: List updated with " + dataForList.size() + " items.");
    }

    /**
     * Returns a displayable (potentially localized) name for a given data key.
     * @param dataKey The internal data key (e.g., KEY_CO2).
     * @return The user-friendly display name (e.g., "CO₂").
     */
    private String getDisplayableNameForKey(String dataKey) {
        // This method maps internal data keys to their display representations.
        // It's important for UI consistency and potential localization.
        switch (dataKey) {
            case KEY_CO2: return "CO₂";
            case KEY_PM25: return "PM2,5";
            case KEY_O2: return "O₂";
            case KEY_SO2: return "SO₂";
            case KEY_CH4: return "CH₄";
            case KEY_P: return "p"; // Atmospheric pressure
            case KEY_LP: return "lp"; // Sound pressure level
            case KEY_LX: return "lx"; // Illuminance
            case KEY_TD: return "Td"; // Dew point
            case KEY_ABS_HUMIDITY: return getString(R.string.value_absolute_humidity); // Localized string
            default:
                Log.w(TAG, "getDisplayableNameForKey: Unknown dataKey '" + dataKey + "'. Returning key itself as display name.");
                return dataKey; // Fallback to the key itself if no mapping exists
        }
    }

    /**
     * Shows a speech bubble dialog fragment with a short explanation for the selected value.
     * @param item The ValueItem containing data about the selected value.
     * @param anchorX The X-coordinate of the anchor view (on screen).
     * @param anchorY The Y-coordinate of the anchor view (on screen).
     * @param anchorWidth The width of the anchor view.
     * @param anchorHeight The height of the anchor view.
     */
    private void showValueExplanationPopup(ValueItem item, int anchorX, int anchorY, int anchorWidth, int anchorHeight) {
        Log.i(TAG, "showValueExplanationPopup: Preparing for item - Display Name='" + item.getDisplayableName() + "', Data Key='"+ item.getDataKey() + "'.");
        // Get the short explanation text using the item's dataKey (non-localized identifier)
        String shortExplanation = getShortExplanation(item.getDataKey());
        Log.d(TAG, "showValueExplanationPopup: Short explanation for Data Key '" + item.getDataKey() + "': \"" + shortExplanation + "\"");

        // Create a new instance of SpeechBubbleDialogFragment
        SpeechBubbleDialogFragment dialogFragment = SpeechBubbleDialogFragment.newInstance(
                item.getDisplayableName(), // Pass display name for UI in the bubble
                shortExplanation,          // The short explanation text
                anchorX, anchorY, anchorWidth, anchorHeight, // Anchor view's bounds for positioning
                item.getDataKey()          // Pass dataKey for "Learn More" navigation to ValueDetailActivity
        );
        // Show the dialog fragment
        dialogFragment.show(getSupportFragmentManager(), "SpeechBubbleDialog"); // Tag for the fragment
        Log.d(TAG, "showValueExplanationPopup: SpeechBubbleDialogFragment instance created and show() called.");
    }

    /**
     * Retrieves a short explanation string for a given data key.
     * Explanation strings are stored in string resources.
     * @param dataKey The internal data key (e.g., KEY_CO2).
     * @return The short explanation string, or a fallback message if not found.
     */
    private String getShortExplanation(String dataKey) {
        Log.d(TAG, "getShortExplanation: Requesting short explanation for dataKey '" + dataKey + "'.");
        // Map internal data keys to the names of string resources containing short explanations
        Map<String, String> resourceNameMap = new HashMap<>();
        resourceNameMap.put(KEY_CO2, "explanation_co2_short");
        resourceNameMap.put(KEY_PM25, "explanation_pm25_short");
        resourceNameMap.put(KEY_O2, "explanation_o2_short");
        resourceNameMap.put(KEY_SO2, "explanation_so2_short");
        resourceNameMap.put(KEY_CH4, "explanation_ch4_short");
        resourceNameMap.put(KEY_P, "explanation_p_short");
        resourceNameMap.put(KEY_LP, "explanation_lp_short");
        resourceNameMap.put(KEY_LX, "explanation_lx_short");
        resourceNameMap.put(KEY_TD, "explanation_td_short");
        resourceNameMap.put(KEY_ABS_HUMIDITY, "explanation_absolute_humidity_short");

        // Get the resource name from the map
        String resourceName = resourceNameMap.get(dataKey);
        String explanation = "No short explanation available for " + dataKey; // Default fallback message

        if (resourceName != null) {
            // If a resource name is mapped, try to get its resource ID
            int resId = getResources().getIdentifier(resourceName, "string", getPackageName());
            if (resId != 0) {
                // If resource ID is found, get the string
                explanation = getString(resId);
                Log.d(TAG, "getShortExplanation: Found explanation for '" + dataKey + "' using resource '" + resourceName + "' (ID: " + resId + ").");
            } else {
                // If resource ID is not found (e.g., typo in resourceName or missing string resource)
                Log.w(TAG, "getShortExplanation: String resource ID not found for resource name: '" + resourceName + "' (intended for dataKey '" + dataKey + "'). Using fallback.");
            }
        } else {
            // If dataKey is not in the map
            Log.w(TAG, "getShortExplanation: No resource name mapping found for dataKey: '" + dataKey + "'. Using fallback.");
        }
        return explanation;
    }

    // Standard lifecycle logging methods
    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart: Activity starting."); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause: Activity paused."); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop: Activity stopped."); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy: Activity being destroyed."); }
}