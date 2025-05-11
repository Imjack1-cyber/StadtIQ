package com.stadtiq;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu; // Needed for options menu
import android.view.MenuItem; // Needed for options menu
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;


import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ValueDetailActivity extends AppCompatActivity {

    // TAG for logging, specific to this Activity
    private static final String TAG = "ValueDetailActivity";
    // Key for storing and retrieving language preference in SharedPreferences
    private static final String PREF_LANG_CODE = "pref_language_code";
    // Tracks the language code to detect changes on resume/restart
    public String lastKnownLangCode = null; // Made public for setLocale, consider alternatives

    // --- CONSTANT DATA KEYS (Match MainActivity for consistency) ---
    // These keys are used to identify the type of environmental value being detailed.
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
        // Store the initial language code to detect changes on resume/restart
        lastKnownLangCode = getLanguageCode(this);
        Log.d(TAG, "onCreate: Initial lastKnownLangCode set to '" + lastKnownLangCode + "'.");

        setContentView(R.layout.activity_value_detail);
        Log.d(TAG, "onCreate: ContentView (R.layout.activity_value_detail) set.");

        // Set status bar color to black for Android Lollipop (API 21) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.black));
            Log.d(TAG, "onCreate: Status bar color set to black for API >= 21.");
        }

        // Find the TextView that will display the detailed content
        TextView detailContent = findViewById(R.id.text_detail_content);
        Log.d(TAG, "onCreate: Content TextView (R.id.text_detail_content) found.");

        // Initialize default values for dataKey and displayTitle
        String dataKey = "UNKNOWN_KEY";
        String displayTitle = "Unknown Value";

        // Retrieve the dataKey passed from the previous activity (e.g., MainActivity via SpeechBubbleDialog)
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("VALUE_DATA_KEY")) {
            dataKey = extras.getString("VALUE_DATA_KEY");
            // Get the displayable name for this dataKey (used for the Toolbar title)
            displayTitle = getDisplayableNameForKey(dataKey);
            Log.d(TAG, "onCreate: Received VALUE_DATA_KEY: '" + dataKey + "', which maps to Display Title: '" + displayTitle + "'.");
        } else {
            Log.w(TAG, "onCreate: VALUE_DATA_KEY not found in intent extras. Using default title and key.");
        }

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_value_detail);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Show "Up" button (back arrow)
            // Set the Toolbar title, formatting it with the displayTitle
            getSupportActionBar().setTitle(getString(R.string.detail_title_format, displayTitle));
            Log.d(TAG, "onCreate: ActionBar setup with title ('" + getString(R.string.detail_title_format, displayTitle) + "') and HomeAsUpEnabled.");
        } else {
            Log.w(TAG, "onCreate: getSupportActionBar() is null. Cannot set title or enable HomeAsUp.");
        }

        // Map data keys to their corresponding long explanation string resource names
        Map<String, String> resourceNameMap = new HashMap<>();
        resourceNameMap.put(KEY_CO2, "explanation_co2_long");
        resourceNameMap.put(KEY_PM25, "explanation_pm25_long");
        resourceNameMap.put(KEY_O2, "explanation_o2_long");
        resourceNameMap.put(KEY_SO2, "explanation_so2_long");
        resourceNameMap.put(KEY_CH4, "explanation_ch4_long");
        resourceNameMap.put(KEY_P, "explanation_p_long");
        resourceNameMap.put(KEY_LP, "explanation_lp_long");
        resourceNameMap.put(KEY_LX, "explanation_lx_long");
        resourceNameMap.put(KEY_TD, "explanation_td_long");
        resourceNameMap.put(KEY_ABS_HUMIDITY, "explanation_absolute_humidity_long");

        // Get the target resource name for the current dataKey
        String targetResourceName = resourceNameMap.get(dataKey);
        Log.d(TAG, "onCreate: Looking up resource name for dataKey='" + dataKey + "'. Mapped to: '" + targetResourceName + "'.");

        String explanation; // Variable to hold the loaded explanation text
        if (targetResourceName != null) {
            // If a resource name is found, try to get its resource ID
            int resId = getResources().getIdentifier(targetResourceName, "string", getPackageName());
            if (resId != 0) {
                // If resource ID is valid, load the string
                explanation = getString(resId);
                Log.d(TAG, "onCreate: Successfully loaded long explanation for '" + dataKey + "' using resource ID " + resId + " ('" + targetResourceName + "').");
            } else {
                // If resource ID is not found (e.g., typo or missing resource)
                Log.w(TAG, "onCreate: Long explanation string resource ID NOT FOUND for name: '" + targetResourceName + "' (for dataKey '" + dataKey + "'). Using fallback message.");
                explanation = getString(R.string.detail_content_not_available, displayTitle);
            }
        } else {
            // If dataKey is not in the map (i.e., no explanation defined for it)
            Log.w(TAG, "onCreate: No resource name mapping found in resourceNameMap for dataKey: '" + dataKey + "'. Using fallback message.");
            explanation = getString(R.string.detail_content_not_available, displayTitle);
        }

        // Set the loaded explanation text to the TextView
        detailContent.setText(explanation);
        Log.d(TAG, "onCreate: Detail content TextView set. Text length: " + (explanation != null ? explanation.length() : "null (or 0 if empty string)"));
        Log.i(TAG, "onCreate: Finished.");
    }

    /**
     * Returns a displayable (potentially localized) name for a given data key.
     * This is used, for example, in the Toolbar title.
     * @param dataKey The internal data key (e.g., KEY_CO2).
     * @return The user-friendly display name (e.g., "CO₂").
     */
    private String getDisplayableNameForKey(String dataKey) {
        // Maps internal data keys to their display representations.
        switch (dataKey) {
            case KEY_CO2: return "CO₂";
            case KEY_PM25: return "PM2,5";
            case KEY_O2: return "O₂";
            case KEY_SO2: return "SO₂";
            case KEY_CH4: return "CH₄";
            case KEY_P: return "p";
            case KEY_LP: return "lp";
            case KEY_LX: return "lx";
            case KEY_TD: return "Td";
            case KEY_ABS_HUMIDITY: return getString(R.string.value_absolute_humidity); // Localized
            default:
                Log.w(TAG, "getDisplayableNameForKey: Unknown dataKey '" + dataKey + "'. Returning key itself.");
                return dataKey; // Fallback
        }
    }

    /**
     * Called when the activity is restarting after being stopped.
     * Checks if the language has changed and recreates the activity if necessary.
     */
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: Activity Restarted.");
        String currentLangCode = getLanguageCode(this);
        // Compare with the language code known when the activity was last fully running
        if (lastKnownLangCode != null && !lastKnownLangCode.equals(currentLangCode)) {
            Log.i(TAG, "onRestart: Language has changed from '" + lastKnownLangCode + "' to '" + currentLangCode + "'. Recreating ValueDetailActivity.");
            lastKnownLangCode = currentLangCode; // Update before recreating
            recreate(); // Recreate to apply the new language
        } else if (lastKnownLangCode == null) {
            // Fallback: if lastKnownLangCode wasn't set (should be by onCreate)
            lastKnownLangCode = currentLangCode;
            Log.w(TAG, "onRestart: lastKnownLangCode was null, now set to '" + currentLangCode + "'.");
        }
    }

    /**
     * Called when the activity will start interacting with the user.
     * Checks for language changes if onRestart didn't catch it.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity Resumed.");
        String currentLangCode = getLanguageCode(this);
        // This check is a safeguard if the activity was paused but not stopped,
        // and language changed externally.
        if (lastKnownLangCode != null && !lastKnownLangCode.equals(currentLangCode)) {
            Log.i(TAG, "onResume: Language changed from '" + lastKnownLangCode + "' to '" + currentLangCode + "' while paused. Recreating ValueDetailActivity.");
            lastKnownLangCode = currentLangCode; // Update before recreating
            recreate(); // Recreate to apply new language
            // Potentially return here to avoid further onResume processing if recreating
            // return;
        } else if (lastKnownLangCode == null) {
            // Fallback if lastKnownLangCode wasn't set
            lastKnownLangCode = currentLangCode;
            Log.w(TAG, "onResume: lastKnownLangCode was null, now set to '" + currentLangCode + "'.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options_menu, menu);
        Log.d(TAG, "onCreateOptionsMenu: Menu (R.menu.main_options_menu) inflated for ValueDetailActivity.");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: Item ID " + item.getItemId() + " (" + getResources().getResourceEntryName(item.getItemId()) + ") selected.");
        if (item.getItemId() == R.id.action_language) {
            Log.d(TAG, "onOptionsItemSelected: Language action (R.id.action_language) selected in ValueDetailActivity.");
            showLanguageSelectionDialog();
            return true; // Event handled
        }
        // Handle the Up/Home button press in the ActionBar
        if (item.getItemId() == android.R.id.home) {
            Log.d(TAG, "onOptionsItemSelected: Home/Up button (android.R.id.home) selected.");
            onSupportNavigateUp(); // Triggers the Up navigation behavior
            return true; // Event handled
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Displays a dialog for the user to select the application language.
     * Uses a custom layout (dialog_language_selection.xml) with RadioButtons.
     */
    private void showLanguageSelectionDialog() {
        // Log the initiation of the language selection dialog
        Log.d(TAG, "showLanguageSelectionDialog: Initiating language selection dialog in ValueDetailActivity.");

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
        Log.i(TAG, "setLocale: Attempting to set language to '" + langCode + "' in ValueDetailActivity.");

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

            // Update lastKnownLangCode before recreating to ensure the new instance
            // doesn't try to recreate again due to stale lastKnownLangCode.
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
     * Handles the Up navigation (back arrow in the ActionBar).
     * Standard behavior is to finish the current activity.
     * @return true if Up navigation was handled successfully.
     */
    @Override
    public boolean onSupportNavigateUp() {
        Log.d(TAG, "onSupportNavigateUp: Called (back button in ActionBar). Navigating back.");
        onBackPressed(); // This will finish the current activity and go to the previous one in the stack
        return true;
    }

    // Standard lifecycle logging methods
    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart: Activity starting."); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause: Activity paused."); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop: Activity stopped."); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy: Activity being destroyed."); }
}