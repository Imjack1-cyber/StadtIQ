package com.stadtiq;

import android.app.AlertDialog; // Needed for language dialog
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu; // Needed for options menu
import android.view.MenuItem; // Needed for options menu
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;


import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ValueDetailActivity extends AppCompatActivity {

    private static final String TAG = "ValueDetailActivity";
    private static final String PREF_LANG_CODE = "pref_language_code";
    private String lastKnownLangCode = null; // For checking language change onResume/onRestart


    // --- CONSTANT DATA KEYS (Match MainActivity) ---
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
        lastKnownLangCode = getLanguageCode(this); // Store initial language

        setContentView(R.layout.activity_value_detail);
        Log.d(TAG, "onCreate: ContentView set.");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.black));
            Log.d(TAG, "onCreate: Status bar color set to black.");
        }

        TextView detailContent = findViewById(R.id.text_detail_content);
        Log.d(TAG, "onCreate: Content TextView found.");

        String dataKey = "UNKNOWN_KEY";
        String displayTitle = "Unknown Value";

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("VALUE_DATA_KEY")) {
            dataKey = extras.getString("VALUE_DATA_KEY");
            displayTitle = getDisplayableNameForKey(dataKey);
            Log.d(TAG, "onCreate: Received VALUE_DATA_KEY: '" + dataKey + "', Display Title: '" + displayTitle + "'.");
        } else {
            Log.w(TAG, "onCreate: VALUE_DATA_KEY not found in intent extras.");
        }

        Toolbar toolbar = findViewById(R.id.toolbar_value_detail);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.detail_title_format, displayTitle));
            Log.d(TAG, "onCreate: ActionBar setup with title and home-as-up enabled.");
        } else {
            Log.w(TAG, "onCreate: getSupportActionBar() is null. Cannot set title or home-as-up.");
        }

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

        String targetResourceName = resourceNameMap.get(dataKey);
        Log.d(TAG, "onCreate: Looking up resource name for dataKey='" + dataKey + "'. Found: '" + targetResourceName + "'.");

        String explanation;
        if (targetResourceName != null) {
            int resId = getResources().getIdentifier(targetResourceName, "string", getPackageName());
            if (resId != 0) {
                explanation = getString(resId);
                Log.d(TAG, "onCreate: Successfully loaded long explanation for '" + dataKey + "' using resource ID " + resId + " ('" + targetResourceName + "').");
            } else {
                Log.w(TAG, "onCreate: Long explanation resource ID NOT FOUND for string name: '" + targetResourceName + "' (for dataKey '" + dataKey + "').");
                explanation = getString(R.string.detail_content_not_available, displayTitle);
            }
        } else {
            Log.w(TAG, "onCreate: No resource name mapping found in resourceNameMap for dataKey: '" + dataKey + "'.");
            explanation = getString(R.string.detail_content_not_available, displayTitle);
        }

        detailContent.setText(explanation);
        Log.d(TAG, "onCreate: Detail content TextView set. Length: " + (explanation != null ? explanation.length() : 0));
        Log.i(TAG, "onCreate: Finished.");
    }

    private String getDisplayableNameForKey(String dataKey) {
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
            case KEY_ABS_HUMIDITY: return getString(R.string.value_absolute_humidity);
            default: return dataKey;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: Activity Restarted.");
        String currentLangCode = getLanguageCode(this);
        if (lastKnownLangCode != null && !lastKnownLangCode.equals(currentLangCode)) {
            Log.i(TAG, "onRestart: Language has changed from " + lastKnownLangCode + " to " + currentLangCode + ". Recreating ValueDetailActivity.");
            lastKnownLangCode = currentLangCode; // Update before recreating
            recreate();
        } else if (lastKnownLangCode == null) { // Should be set in onCreate
            lastKnownLangCode = currentLangCode;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity Resumed.");
        // Check if language changed while this activity was paused, if not handled by onRestart
        String currentLangCode = getLanguageCode(this);
        if (lastKnownLangCode != null && !lastKnownLangCode.equals(currentLangCode)) {
            Log.i(TAG, "onResume: Language changed from " + lastKnownLangCode + " to " + currentLangCode + " while paused. Recreating ValueDetailActivity.");
            lastKnownLangCode = currentLangCode;
            recreate();
            // return; // Potentially return to avoid further processing if recreating
        } else if (lastKnownLangCode == null) {
            lastKnownLangCode = currentLangCode;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options_menu, menu);
        Log.d(TAG, "onCreateOptionsMenu: Menu inflated for ValueDetailActivity.");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: Item ID " + item.getItemId() + " (" + getResources().getResourceEntryName(item.getItemId()) + ") selected.");
        if (item.getItemId() == R.id.action_language) {
            Log.d(TAG, "onOptionsItemSelected: Language action selected in ValueDetailActivity.");
            showLanguageSelectionDialog();
            return true;
        }
        // Handle the Up/Home button
        if (item.getItemId() == android.R.id.home) {
            onSupportNavigateUp(); // or onBackPressed()
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void showLanguageSelectionDialog() {
        Log.d(TAG, "showLanguageSelectionDialog: Called in ValueDetailActivity.");
        String[] languages = {getString(R.string.language_english), getString(R.string.language_german)};
        String currentLangCode = getLanguageCode(this);
        int checkedItem = "de".equals(currentLangCode) ? 1 : 0;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_select_language_title);
        builder.setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
            String selectedLangCode = (which == 0) ? "en" : "de";
            setLocale(selectedLangCode);
            dialog.dismiss();
        });
        builder.setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> dialog.dismiss());
        builder.create().show();
        Log.d(TAG, "showLanguageSelectionDialog: Dialog shown.");
    }

    private void setLocale(String langCode) {
        Log.i(TAG, "setLocale: Attempting to set language to '" + langCode + "' in ValueDetailActivity.");
        String currentLangCode = getLanguageCode(this);
        if (!currentLangCode.equals(langCode)) {
            Log.d(TAG, "setLocale: Language changing from '" + currentLangCode + "' to '" + langCode + "'. Saving and recreating activity.");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putString(PREF_LANG_CODE, langCode).apply();
            lastKnownLangCode = langCode; // Update before recreating
            recreate();
        } else {
            Log.d(TAG, "setLocale: Language is already '" + currentLangCode + "'. No action needed.");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        Log.d(TAG, "onSupportNavigateUp: Called (back button in ActionBar).");
        onBackPressed(); // Standard behavior: finish the current activity
        return true;
    }

    // Lifecycle Logging
    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
}