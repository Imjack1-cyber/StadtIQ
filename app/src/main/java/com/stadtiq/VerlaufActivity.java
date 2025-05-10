package com.stadtiq;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources; // Added for Resources.Theme
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

// **IMPORTANT**: Ensure there is NO `import android.R;` statement in this file.

public class VerlaufActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "VerlaufActivity";
    private static final String GRAPH_VIEW_TAG = "SimpleGraphView";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Spinner spinnerCity;
    private Spinner spinnerDistrict;
    private LinearLayout timeOptionsContainer;
    private FrameLayout graphContainer;
    private TextView txtGraphPlaceholderMessage;
    private SimpleGraphView simpleGraphView;

    private HorizontalScrollView valueOptionsScrollView;
    private LinearLayout valueOptionsContainer;
    private List<TextView> valueOptionTextViews = new ArrayList<>();
    private List<String> selectedGraphDataKeys = new ArrayList<>(); // Stores DATA KEYS

    private Map<String, Integer> valueColors = new LinkedHashMap<>();
    private Map<String, float[]> baseGraphDataPatterns = new HashMap<>();


    private List<String> cities;
    private Map<String, List<String>> districts;
    private List<String> timePeriods;
    private TextView selectedTimeOption = null;

    private static final String PREF_LANG_CODE = "pref_language_code";
    private static final int APPROX_DAYS_IN_MONTH = 30;
    private static final int MAX_DAYS_FOR_FULL_YEAR_GRAPH = 365;
    private String lastKnownLangCode = null;


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

    private final List<String> ALL_VALUE_DATA_KEYS_FOR_GRAPH = Arrays.asList(
            KEY_CO2, KEY_PM25, KEY_O2, KEY_SO2, KEY_CH4, KEY_P, KEY_LP, KEY_LX, KEY_TD, KEY_ABS_HUMIDITY
    );
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
        lastKnownLangCode = getLanguageCode(this);

        setContentView(R.layout.activity_verlauf);
        Log.d(TAG, "onCreate: ContentView set.");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Log.d(TAG, "onCreate: Toolbar setup.");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.title_activity_verlauf);
            Log.d(TAG, "onCreate: ActionBar title set.");
        }

        drawerLayout = findViewById(R.id.drawer_layout_verlauf);
        navigationView = findViewById(R.id.nav_view_verlauf);
        Log.d(TAG, "onCreate: DrawerLayout and NavigationView found.");

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        if (drawerLayout != null) drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        if (navigationView != null) navigationView.setNavigationItemSelectedListener(this);
        Log.d(TAG, "onCreate: ActionBarDrawerToggle and NavigationView listener setup.");

        spinnerCity = findViewById(R.id.spinner_city_verlauf);
        spinnerDistrict = findViewById(R.id.spinner_district_verlauf);
        timeOptionsContainer = findViewById(R.id.time_options_container);
        graphContainer = findViewById(R.id.graph_placeholder_container);
        txtGraphPlaceholderMessage = findViewById(R.id.txt_graph_placeholder_message_verlauf);
        Log.d(TAG, "onCreate: Basic UI elements found. graphContainer: " + (graphContainer != null));

        valueOptionsScrollView = findViewById(R.id.value_options_scroll_view_verlauf);
        valueOptionsContainer = findViewById(R.id.value_options_container_verlauf);
        Log.d(TAG, "onCreate: Value selection UI found: " + (valueOptionsContainer != null));


        if (graphContainer != null) {
            simpleGraphView = new SimpleGraphView(this);
            FrameLayout.LayoutParams graphLayoutParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            simpleGraphView.setLayoutParams(graphLayoutParams);
            graphContainer.addView(simpleGraphView);
            simpleGraphView.setVisibility(View.GONE);
            Log.d(TAG, "onCreate: SimpleGraphView added to graphContainer.");
        } else {
            Log.e(TAG, "onCreate: graphContainer is NULL! SimpleGraphView cannot be added.");
        }

        initializeValueColors();
        initializeCityDistrictData(); // Must be before initializeBaseGraphDataPatterns
        initializeBaseGraphDataPatterns();
        initializeTimePeriods();
        setupValueOptionTextViews(); // Uses localized strings
        Log.d(TAG, "onCreate: Data and ValueOption TextViews initialized.");

        setupCitySpinner();
        setupDistrictSpinner();
        setupTimeOptionClicks();

        showPlaceholderMessage();
        handleSelectionChange();
        Log.i(TAG, "onCreate: Finished.");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: Activity Restarted.");
        String currentLangCode = getLanguageCode(this);
        if (lastKnownLangCode != null && !lastKnownLangCode.equals(currentLangCode)) {
            Log.i(TAG, "onRestart: Language has changed from " + lastKnownLangCode + " to " + currentLangCode + ". Recreating VerlaufActivity.");
            lastKnownLangCode = currentLangCode;
            recreate();
        } else if (lastKnownLangCode == null) {
            lastKnownLangCode = currentLangCode;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity Resumed.");
        String currentLangCode = getLanguageCode(this);
        if (lastKnownLangCode != null && !lastKnownLangCode.equals(currentLangCode)) {
            Log.i(TAG, "onResume: Language changed from " + lastKnownLangCode + " to " + currentLangCode + " while paused. Recreating VerlaufActivity.");
            lastKnownLangCode = currentLangCode;
            recreate();
            return; // Avoid further processing as activity will restart
        } else if (lastKnownLangCode == null) {
            lastKnownLangCode = currentLangCode;
        }

        if (navigationView != null) {
            MenuItem verlaufItem = navigationView.getMenu().findItem(R.id.nav_verlauf);
            if (verlaufItem != null) {
                Log.d(TAG, "onResume: Setting 'Verlauf' as checked. Current status before: " + verlaufItem.isChecked());
                navigationView.setCheckedItem(R.id.nav_verlauf);
                Log.d(TAG, "onResume: Nav drawer item 'Verlauf' isChecked after set: " + verlaufItem.isChecked());
            } else {
                Log.w(TAG, "onResume: nav_verlauf menu item is null.");
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
            Log.d(TAG, "onNavigationItemSelected: Verlauf selected (current activity).");
            navigationView.setCheckedItem(R.id.nav_verlauf);
        } else if (id == R.id.nav_impressum) {
            Log.d(TAG, "onNavigationItemSelected: Navigating to ImpressumActivity.");
            if (!this.getClass().equals(ImpressumActivity.class)) {
                intent = new Intent(this, ImpressumActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            } else {
                Log.d(TAG, "onNavigationItemSelected: Already on ImpressumActivity.");
            }
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_select_language_title);
        builder.setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
            String selectedLangCode = (which == 0) ? "en" : "de";
            setLocale(selectedLangCode);
            dialog.dismiss();
        });
        builder.setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void setLocale(String langCode) {
        Log.i(TAG, "setLocale: Attempting to set language to '" + langCode + "'.");
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

    private void initializeCityDistrictData() {
        Log.d(TAG, "initializeCityDistrictData: Starting.");
        cities = new ArrayList<>();
        cities.add(getString(R.string.hint_select_city));
        cities.add("Braunschweig");
        districts = new HashMap<>();
        districts.put(getString(R.string.hint_select_city), Arrays.asList(getString(R.string.hint_select_district)));
        districts.put("Braunschweig", Arrays.asList(
                getString(R.string.hint_select_district),
                "Innere Stadt", "Westliches Ringgebiet", "Östliches Ringgebiet", "Südliches Ringgebiet",
                "Weststadt", "Heidberg-Melverode", "Südstadt", "Bebelhof",
                "Nördliches Ringgebiet", "Braunschweig-Nord"
        ));
        Log.d(TAG, "initializeCityDistrictData: Finished.");
    }

    private void initializeTimePeriods() {
        Log.d(TAG, "initializeTimePeriods: Starting.");
        timePeriods = new ArrayList<>(Arrays.asList("1","3", "6", "9", "12"));
        Log.d(TAG, "initializeTimePeriods: Time periods initialized: " + timePeriods.toString());
    }

    private void initializeValueColors() {
        Log.d(TAG, "initializeValueColors: Setting up colors for graph lines.");
        valueColors.put(KEY_CO2, ContextCompat.getColor(this, R.color.graph_co2));
        valueColors.put(KEY_PM25, ContextCompat.getColor(this, R.color.graph_pm25));
        valueColors.put(KEY_O2, ContextCompat.getColor(this, R.color.graph_o2));
        valueColors.put(KEY_SO2, ContextCompat.getColor(this, R.color.graph_so2));
        valueColors.put(KEY_CH4, ContextCompat.getColor(this, R.color.graph_ch4));
        valueColors.put(KEY_P, ContextCompat.getColor(this, R.color.graph_p));
        valueColors.put(KEY_LP, ContextCompat.getColor(this, R.color.graph_lp));
        valueColors.put(KEY_LX, ContextCompat.getColor(this, R.color.graph_lx));
        valueColors.put(KEY_TD, ContextCompat.getColor(this, R.color.graph_td));
        valueColors.put(KEY_ABS_HUMIDITY, ContextCompat.getColor(this, R.color.graph_abs_humidity));
        Log.d(TAG, "initializeValueColors: Finished setting up " + valueColors.size() + " value colors.");
    }

    private void initializeBaseGraphDataPatterns() {
        Log.d(TAG, "initializeBaseGraphDataPatterns: Creating base patterns for graph data.");
        baseGraphDataPatterns.clear();
        Random localRandom = new Random();

        List<String> allDistrictsForPatterns = districts.get("Braunschweig");
        if (allDistrictsForPatterns == null) {
            Log.e(TAG, "initializeBaseGraphDataPatterns: 'Braunschweig' districts not found! This should not happen if initializeCityDistrictData ran first.");
            allDistrictsForPatterns = new ArrayList<>(Arrays.asList(getString(R.string.hint_select_district), "default_district_fallback"));
        }

        for (String dataKey : ALL_VALUE_DATA_KEYS_FOR_GRAPH) {
            for (String districtName : allDistrictsForPatterns) {
                if (districtName.equals(getString(R.string.hint_select_district))) {
                    continue;
                }

                String patternKey = dataKey + "_" + districtName;
                localRandom.setSeed(dataKey.hashCode() ^ districtName.hashCode());

                float[] pattern = new float[MAX_DAYS_FOR_FULL_YEAR_GRAPH];
                float trendFactor = (localRandom.nextFloat() - 0.5f) * 0.002f;
                float baseValue = 0.3f + localRandom.nextFloat() * 0.4f;

                for (int i = 0; i < pattern.length; i++) {
                    float noise = (localRandom.nextFloat() - 0.5f) * 0.2f;
                    pattern[i] = Math.max(0.05f, Math.min(0.95f, baseValue + trendFactor * i + noise));
                }
                baseGraphDataPatterns.put(patternKey, pattern);
            }

            String defaultPatternKey = dataKey + "_default_district";
            if (!baseGraphDataPatterns.containsKey(defaultPatternKey)) {
                localRandom.setSeed(dataKey.hashCode() ^ "default_district".hashCode());
                float[] defaultPattern = new float[MAX_DAYS_FOR_FULL_YEAR_GRAPH];
                float trendFactorDef = (localRandom.nextFloat() - 0.5f) * 0.002f;
                float baseValueDef = 0.3f + localRandom.nextFloat() * 0.4f;
                for (int i = 0; i < defaultPattern.length; i++) {
                    float noiseDef = (localRandom.nextFloat() - 0.5f) * 0.2f;
                    defaultPattern[i] = Math.max(0.05f, Math.min(0.95f, baseValueDef + trendFactorDef * i + noiseDef));
                }
                baseGraphDataPatterns.put(defaultPatternKey, defaultPattern);
            }
        }
        Log.d(TAG, "initializeBaseGraphDataPatterns: Generated base patterns for " + baseGraphDataPatterns.size() + " value-district combinations.");
    }


    private void setupCitySpinner() {
        Log.d(TAG, "setupCitySpinner: Starting.");
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cities);
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(cityAdapter);
        spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCity = (String) parent.getItemAtPosition(position);
                Log.i(TAG, "City Spinner: Item selected - '" + selectedCity + "' at position " + position);
                updateDistrictSpinner(selectedCity);
                handleSelectionChange();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "City Spinner: Nothing selected.");
            }
        });
        Log.d(TAG, "setupCitySpinner: Finished.");
    }

    private void setupDistrictSpinner() {
        Log.d(TAG, "setupDistrictSpinner: Starting.");
        String initialCity = cities.get(spinnerCity.getSelectedItemPosition());
        updateDistrictSpinner(initialCity);
        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCity = cities.get(spinnerCity.getSelectedItemPosition());
                String selectedDistrict = (String) parent.getItemAtPosition(position);
                Log.i(TAG, "District Spinner: Item selected - '" + selectedDistrict + "' for city '" + selectedCity + "'.");
                handleSelectionChange();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "District Spinner: Nothing selected.");
            }
        });
        Log.d(TAG, "setupDistrictSpinner: Finished.");
    }

    private void updateDistrictSpinner(String selectedCity) {
        Log.d(TAG, "updateDistrictSpinner: Updating for city '" + selectedCity + "'.");
        List<String> districtList = districts.get(selectedCity);
        if (districtList == null) {
            Log.w(TAG, "updateDistrictSpinner: No districts found for city '" + selectedCity + "', using default hint list.");
            districtList = new ArrayList<>(Arrays.asList(getString(R.string.hint_select_district)));
        }
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districtList);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistrict.setAdapter(districtAdapter);
        spinnerDistrict.setSelection(0);
        Log.d(TAG, "updateDistrictSpinner: Adapter set for district spinner. List size: " + districtList.size());
    }

    private void setupTimeOptionClicks() {
        Log.d(TAG, "setupTimeOptionClicks: Setting up listeners for time options.");
        if(timeOptionsContainer == null) {
            Log.e(TAG, "setupTimeOptionClicks: timeOptionsContainer is NULL.");
            return;
        }
        for (int i = 0; i < timeOptionsContainer.getChildCount(); i++) {
            View child = timeOptionsContainer.getChildAt(i);
            if (child instanceof TextView) {
                TextView timeOptionTV = (TextView) child;
                timeOptionTV.setBackgroundResource(R.drawable.value_option_selector);
                timeOptionTV.setTextColor(ContextCompat.getColor(this, R.color.default_text_color_selector_verlauf));
                timeOptionTV.setOnClickListener(v -> selectTimeOption(timeOptionTV));
            }
        }
        if (timeOptionsContainer.getChildCount() > 0 && timeOptionsContainer.getChildAt(0) instanceof TextView) {
            selectTimeOption((TextView) timeOptionsContainer.getChildAt(0));
        } else {
            Log.w(TAG, "setupTimeOptionClicks: No time options found to set a default.");
        }
        Log.d(TAG, "setupTimeOptionClicks: Finished.");
    }

    private void selectTimeOption(TextView selectedTextView) {
        String timeText = selectedTextView.getText().toString();
        Log.i(TAG, "selectTimeOption: TextView clicked, time: '" + timeText + "'.");
        if (selectedTimeOption != null) {
            Log.d(TAG, "selectTimeOption: Deselecting previous time option: '" + selectedTimeOption.getText().toString() + "'.");
            selectedTimeOption.setSelected(false);
            selectedTimeOption.setBackgroundResource(R.drawable.value_option_selector);
            selectedTimeOption.setTextColor(ContextCompat.getColor(this, R.color.default_text_color_selector_verlauf));
        }
        selectedTextView.setSelected(true);
        selectedTextView.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_solid_selected_option));
        selectedTextView.setTextColor(ContextCompat.getColor(this, R.color.selected_text_color_selector_verlauf));
        selectedTimeOption = selectedTextView;
        Log.d(TAG, "selectTimeOption: New selected time option: '" + selectedTimeOption.getText().toString() + "'.");
        handleSelectionChange();
    }

    private void setupValueOptionTextViews() {
        Log.d(TAG, "setupValueOptionTextViews: Setting up TextViews for value selection.");
        if (valueOptionsContainer == null) {
            Log.e(TAG, "setupValueOptionTextViews: valueOptionsContainer is NULL.");
            return;
        }
        valueOptionsContainer.removeAllViews();
        valueOptionTextViews.clear();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        params.setMarginEnd(margin * 2);

        for (String dataKey : ALL_VALUE_DATA_KEYS_FOR_GRAPH) {
            TextView valueTV = new TextView(this);
            valueTV.setText(getDisplayableNameForKey_GraphValueOptions(dataKey));
            valueTV.setTag(dataKey);
            valueTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            valueTV.setPadding(padding, padding, padding, padding);
            valueTV.setGravity(Gravity.CENTER);
            valueTV.setMinWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, getResources().getDisplayMetrics()));
            valueTV.setMinHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics()));
            valueTV.setLayoutParams(params);
            valueTV.setClickable(true);
            valueTV.setFocusable(true);

            boolean isSelected = selectedGraphDataKeys.contains(dataKey);
            valueTV.setSelected(isSelected);
            updateValueTextViewAppearance(valueTV, isSelected);

            valueTV.setOnClickListener(v -> {
                TextView clickedTV = (TextView) v;
                String clickedDataKey = (String) clickedTV.getTag();

                boolean wasSelected = selectedGraphDataKeys.contains(clickedDataKey);
                if (wasSelected) {
                    selectedGraphDataKeys.remove(clickedDataKey);
                    clickedTV.setSelected(false);
                } else {
                    selectedGraphDataKeys.add(clickedDataKey);
                    clickedTV.setSelected(true);
                }
                updateValueTextViewAppearance(clickedTV, !wasSelected);
                handleSelectionChange();
            });
            valueOptionTextViews.add(valueTV);
            valueOptionsContainer.addView(valueTV);
        }
        Log.d(TAG, "setupValueOptionTextViews: Finished creating " + valueOptionTextViews.size() + " value TextViews.");
    }
    private String getDisplayableNameForKey_GraphValueOptions(String dataKey) {
        switch (dataKey) {
            case KEY_ABS_HUMIDITY: return getString(R.string.value_absolute_humidity);
            default: return dataKey;
        }
    }


    private void updateValueTextViewAppearance(TextView valueTV, boolean isSelected) {
        String dataKey = (String) valueTV.getTag();
        if (isSelected) {
            Integer color = valueColors.get(dataKey);
            if (color != null) {
                valueTV.setBackgroundColor(color);
                int r = Color.red(color); int g = Color.green(color); int b = Color.blue(color);
                valueTV.setTextColor((0.299 * r + 0.587 * g + 0.114 * b) / 255 > 0.5 ? Color.BLACK : Color.WHITE);
            } else {
                valueTV.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_solid_selected_option));
                valueTV.setTextColor(Color.WHITE);
            }
        } else {
            valueTV.setBackgroundResource(R.drawable.value_option_selector);
            valueTV.setTextColor(ContextCompat.getColor(this, R.color.default_text_color_selector_verlauf));
        }
    }


    private void handleSelectionChange() {
        Log.i(TAG, "handleSelectionChange: Evaluating current selections.");
        String selectedCity = cities.get(spinnerCity.getSelectedItemPosition());
        String selectedDistrictForDisplay = null;
        String districtKeyForDataPattern = "default_district";

        List<String> currentDistrictsList = districts.get(selectedCity);
        if (currentDistrictsList != null && spinnerDistrict.getSelectedItemPosition() >= 0 &&
                spinnerDistrict.getSelectedItemPosition() < currentDistrictsList.size()){
            selectedDistrictForDisplay = currentDistrictsList.get(spinnerDistrict.getSelectedItemPosition());
            if (!selectedDistrictForDisplay.equals(getString(R.string.hint_select_district))) {
                districtKeyForDataPattern = selectedDistrictForDisplay;
            }
        } else if (currentDistrictsList != null && !currentDistrictsList.isEmpty()){
            selectedDistrictForDisplay = currentDistrictsList.get(0);
        }

        boolean isCityProperlySelected = !getString(R.string.hint_select_city).equals(selectedCity);
        boolean isTimeSelected = selectedTimeOption != null;
        boolean atLeastOneValueSelected = !selectedGraphDataKeys.isEmpty();

        Log.d(TAG, "handleSelectionChange: CityProper=" + isCityProperlySelected +
                ", TimeSelected=" + (isTimeSelected ? selectedTimeOption.getText() : "null") +
                ", ValuesSelectedCount=" + selectedGraphDataKeys.size() + " -> " + selectedGraphDataKeys.toString() +
                ", DistrictForDisplay='" + selectedDistrictForDisplay + "', DistrictForDataPattern='" + districtKeyForDataPattern + "'");


        if (simpleGraphView == null || txtGraphPlaceholderMessage == null) return;

        if (isCityProperlySelected && isTimeSelected && atLeastOneValueSelected) {
            txtGraphPlaceholderMessage.setVisibility(View.GONE);
            if (graphContainer != null) graphContainer.setVisibility(View.VISIBLE);
            simpleGraphView.setVisibility(View.VISIBLE);

            String timePeriodText = selectedTimeOption.getText().toString();
            List<SimpleGraphView.DatasetForGraph> datasetsToDisplay = new ArrayList<>();

            for (String dataKey : selectedGraphDataKeys) {
                float[] dataPoints = simpleGraphView.getDeterministicDataPoints(
                        dataKey, districtKeyForDataPattern, timePeriodText, APPROX_DAYS_IN_MONTH);

                Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                linePaint.setStrokeWidth(simpleGraphView.dpToPx(2f));
                linePaint.setStyle(Paint.Style.STROKE);
                linePaint.setColor(valueColors.getOrDefault(dataKey, Color.GRAY));

                datasetsToDisplay.add(simpleGraphView.new DatasetForGraph(dataPoints, linePaint, getDisplayableNameForKey_GraphValueOptions(dataKey)));
            }
            simpleGraphView.updateData(selectedCity, selectedDistrictForDisplay, datasetsToDisplay, timePeriodText);
        } else {
            simpleGraphView.setVisibility(View.GONE);
            if (graphContainer != null) graphContainer.setVisibility(View.GONE);
            txtGraphPlaceholderMessage.setVisibility(View.VISIBLE);
            if (!isCityProperlySelected) txtGraphPlaceholderMessage.setText(R.string.placeholder_select_city_verlauf);
            else if (!isTimeSelected && !atLeastOneValueSelected) txtGraphPlaceholderMessage.setText(R.string.placeholder_select_time_and_value);
            else if (!isTimeSelected) txtGraphPlaceholderMessage.setText(R.string.placeholder_select_time);
            else txtGraphPlaceholderMessage.setText(R.string.placeholder_select_at_least_one_value);
        }
    }

    private void showPlaceholderMessage() {
        if (txtGraphPlaceholderMessage != null) {
            txtGraphPlaceholderMessage.setText(getString(R.string.txt_graph_placeholder_message_verlauf_default));
            txtGraphPlaceholderMessage.setVisibility(View.VISIBLE);
        }
        if (simpleGraphView != null) simpleGraphView.setVisibility(View.GONE);
        if (graphContainer != null) graphContainer.setVisibility(View.GONE);
    }

    // Inner class SimpleGraphView
    public class SimpleGraphView extends View {
        private Paint axisPaint, textPaint, gridPaint, dataPointPaintDefault;
        private List<DatasetForGraph> datasetsToDraw;
        private String city, districtForTitle, viewTimePeriod;

        public class DatasetForGraph {
            float[] points; Paint linePaint; Paint pointPaint; String label;
            public DatasetForGraph(float[] points, Paint linePaint, String label) {
                this.points = points; this.linePaint = linePaint; this.label = label;
                this.pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                this.pointPaint.setStyle(Paint.Style.FILL);
                this.pointPaint.setColor(this.linePaint != null ? this.linePaint.getColor() : ContextCompat.getColor(getContext(), R.color.colorPrimaryVariant)); // Fallback color
            }
        }

        public SimpleGraphView(Context context) { super(context); init(); }
        public SimpleGraphView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

        private int getColorFromAttr(int attrResId, int defaultColor) {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getContext().getTheme();
            if (theme.resolveAttribute(attrResId, typedValue, true)) {
                return typedValue.resourceId != 0 ? ContextCompat.getColor(getContext(), typedValue.resourceId) : typedValue.data;
            }
            Log.w(GRAPH_VIEW_TAG, "Failed to resolve attribute " + getResources().getResourceEntryName(attrResId) + ". Using default color.");
            return ContextCompat.getColor(getContext(), defaultColor); // Use ContextCompat for default too
        }


        private void init() {
            Log.d(GRAPH_VIEW_TAG, "init: Initializing SimpleGraphView (" + this.hashCode() + ").");
            datasetsToDraw = new ArrayList<>();

            int axisColor = getColorFromAttr(R.attr.graphAxisColor, R.color.graph_axis); // Fallback to direct color
            int gridColor = getColorFromAttr(R.attr.graphGridColor, R.color.graph_grid);
            int textColor = getColorFromAttr(R.attr.graphTextColor, R.color.graph_text);
            int defaultPointColor = getColorFromAttr(com.google.android.material.R.attr.colorPrimaryVariant, R.color.colorPrimaryVariant);


            axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            axisPaint.setColor(axisColor);
            axisPaint.setStrokeWidth(dpToPx(1.5f));

            gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            gridPaint.setColor(gridColor);
            gridPaint.setStrokeWidth(dpToPx(0.5f));
            gridPaint.setStyle(Paint.Style.STROKE);

            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(textColor);
            textPaint.setTextSize(dpToPx(10));
            textPaint.setTextAlign(Paint.Align.CENTER);

            dataPointPaintDefault = new Paint(Paint.ANTI_ALIAS_FLAG);
            dataPointPaintDefault.setStyle(Paint.Style.FILL);
            dataPointPaintDefault.setColor(defaultPointColor);
        }

        public float dpToPx(float dp) {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
        }

        public float[] getDeterministicDataPoints(String dataKey, String districtKeyForPattern, String timePeriodText, int daysInMonthConstant) {
            Log.d(GRAPH_VIEW_TAG, "getDeterministicDataPoints (" + this.hashCode() + "): For Value='" + dataKey + "', DistrictKeyForPattern='" + districtKeyForPattern + "', TimePeriod='" + timePeriodText + "'");

            String patternKey = dataKey + "_" + (districtKeyForPattern != null ? districtKeyForPattern : "default_district");
            float[] basePattern = baseGraphDataPatterns.get(patternKey);

            if (basePattern == null) {
                Log.w(GRAPH_VIEW_TAG, "No pattern for key '" + patternKey + "'. Using default for value: " + dataKey);
                basePattern = baseGraphDataPatterns.get(dataKey + "_default_district");
                if(basePattern == null){
                    Log.e(GRAPH_VIEW_TAG, "CRITICAL: Default base pattern also missing for " + dataKey + ". Returning empty array.");
                    return new float[0];
                }
            }

            int numPointsToExtract;
            if (timePeriodText == null || timePeriodText.isEmpty()) {
                numPointsToExtract = daysInMonthConstant;
            } else if (timePeriodText.equals(getString(R.string.time_option_all))) {
                numPointsToExtract = MAX_DAYS_FOR_FULL_YEAR_GRAPH;
            } else {
                try {
                    int months = Integer.parseInt(timePeriodText);
                    numPointsToExtract = months * daysInMonthConstant;
                } catch (NumberFormatException e) {
                    Log.w(GRAPH_VIEW_TAG, "getDeterministicDataPoints: Could not parse timePeriodText '" + timePeriodText + "'. Defaulting to 1 month equivalent.");
                    numPointsToExtract = daysInMonthConstant;
                }
            }
            numPointsToExtract = Math.min(numPointsToExtract, basePattern.length);
            if (numPointsToExtract <= 0) {
                numPointsToExtract = Math.min(2, basePattern.length > 0 ? basePattern.length : (basePattern.length == 1 ? 1 : 2) );
            }
            if (basePattern.length == 1 && numPointsToExtract > 0) {
                numPointsToExtract = 1;
            }


            Log.d(GRAPH_VIEW_TAG, "getDeterministicDataPoints: Extracting " + numPointsToExtract + " points for " + dataKey + " (patternKey: " + patternKey + ")");
            if (numPointsToExtract > basePattern.length || numPointsToExtract < 0) {
                Log.e(GRAPH_VIEW_TAG, "Error in point calculation: numPointsToExtract=" + numPointsToExtract + ", basePattern.length=" + basePattern.length);
                return new float[0];
            }
            return Arrays.copyOfRange(basePattern, 0, numPointsToExtract);
        }


        public void updateData(String city, String districtForTitleDisplay, List<DatasetForGraph> datasets, String overallTimePeriod) {
            this.city = city; this.districtForTitle = districtForTitleDisplay;
            this.datasetsToDraw = new ArrayList<>(datasets); this.viewTimePeriod = overallTimePeriod;
            Log.i(GRAPH_VIEW_TAG, "updateData (" + this.hashCode() + "): Received " + (this.datasetsToDraw != null ? this.datasetsToDraw.size() : 0) +
                    " datasets. City='" + city + "', DisplayDistrict='" + districtForTitle + "', OverallTimePeriod='" + overallTimePeriod + "'.");
            invalidate();
            Log.d(GRAPH_VIEW_TAG, "updateData (" + this.hashCode() + "): Invalidation requested.");
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth(); int height = getHeight();
            Log.d(GRAPH_VIEW_TAG, "onDraw (" + this.hashCode() + "): Called. Width=" + width + ", Height=" + height + ". Datasets: " + (datasetsToDraw != null ? datasetsToDraw.size() : "null"));

            if (width == 0 || height == 0 || datasetsToDraw == null || datasetsToDraw.isEmpty()) {
                Log.w(GRAPH_VIEW_TAG, "onDraw (" + this.hashCode() + "): Cannot draw - invalid dimensions or no datasets.");
                if (width > 0 && height > 0) {
                    canvas.drawColor(Color.WHITE);
                    textPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("Select City, Time, and Value(s)", width / 2f, height / 2f, textPaint);
                }
                return;
            }

            // Use themed background for the graph view itself if desired, or keep white
            // Example: canvas.drawColor(getColorFromAttr(android.R.attr.colorBackground, R.color.white));
            canvas.drawColor(Color.WHITE); // Keeping it white for now for simplicity of graph lines

            float originalTextSize = textPaint.getTextSize();
            float paddingLeft = dpToPx(50); float paddingRight = dpToPx(20);
            float paddingTop = dpToPx(40); float paddingBottom = dpToPx(50);
            float graphWidth = width - paddingLeft - paddingRight;
            float graphHeight = height - paddingTop - paddingBottom;

            textPaint.setTextSize(dpToPx(12)); textPaint.setTextAlign(Paint.Align.CENTER);
            String title = "";
            if (datasetsToDraw.size() == 1 && datasetsToDraw.get(0).label != null) title = datasetsToDraw.get(0).label + " Trend";
            else if (datasetsToDraw.size() > 1) {
                StringBuilder labels = new StringBuilder();
                for(int i=0; i<datasetsToDraw.size(); i++) {
                    labels.append(datasetsToDraw.get(i).label);
                    if (i < datasetsToDraw.size() -1) labels.append(" & ");
                }
                title = labels.toString() + " Trends";
            } else title = "Data Trend";

            if(city != null && !getString(R.string.hint_select_city).equals(city)) {
                title += " in " + city;
                if(districtForTitle != null && !getString(R.string.hint_select_district).equals(districtForTitle) && !districtForTitle.isEmpty()){
                    title += "/" + districtForTitle;
                }
            }
            if (viewTimePeriod != null && !viewTimePeriod.isEmpty()) {
                title += " (" + viewTimePeriod + (viewTimePeriod.matches("\\d+") ? " mo" : "") + ")";
            }
            canvas.drawText(title, width / 2f, paddingTop / 2f + textPaint.getTextSize() / 3, textPaint);
            textPaint.setTextSize(originalTextSize);

            canvas.drawLine(paddingLeft, paddingTop, paddingLeft, height - paddingBottom, axisPaint);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            for (int i = 0; i <= 4; i++) {
                float yPos = paddingTop + (graphHeight * ((float)i / 4));
                canvas.drawText(String.format(Locale.getDefault(), "%.0f", 100f * (1f - (float)i/4)), paddingLeft-dpToPx(6), yPos+textPaint.getTextSize()/3, textPaint);
                if(i < 4) canvas.drawLine(paddingLeft, yPos, width - paddingRight, yPos, gridPaint);
            }
            textPaint.setTextAlign(Paint.Align.CENTER); canvas.save();
            canvas.rotate(-90, paddingLeft/2f-dpToPx(4), paddingTop+graphHeight/2f);
            canvas.drawText("Normalized Value (%)", paddingLeft/2f-dpToPx(4), paddingTop+graphHeight/2f + textPaint.getTextSize()/2, textPaint);
            canvas.restore();

            canvas.drawLine(paddingLeft, height - paddingBottom, width - paddingRight, height - paddingBottom, axisPaint);
            int maxDataPoints = 0;
            for(DatasetForGraph ds : datasetsToDraw) if (ds.points != null) maxDataPoints = Math.max(maxDataPoints, ds.points.length);
            if (maxDataPoints == 0) maxDataPoints = 1;
            float xInterval = (maxDataPoints > 1) ? graphWidth / (maxDataPoints - 1) : graphWidth;
            int numXLabels = Math.min(maxDataPoints, maxDataPoints <=5 ? maxDataPoints : 5);
            if (maxDataPoints == 1) numXLabels = 1;

            for (int i = 0; i < numXLabels; i++) {
                int dataIdx = (numXLabels <= 1) ? 0 : Math.round(i * (float)(maxDataPoints - 1) / (numXLabels - 1));
                float xPos = paddingLeft + dataIdx * xInterval;
                if (maxDataPoints == 1) xPos = paddingLeft + graphWidth / 2;

                String xLabel = "T" + (dataIdx + 1);
                if (this.viewTimePeriod != null && !this.viewTimePeriod.isEmpty()) {
                    if (this.viewTimePeriod.equals(getString(R.string.time_option_all))) {
                        xLabel = "Seg." + (dataIdx + 1);
                    } else if (this.viewTimePeriod.matches("\\d+")) {
                        try {
                            int totalMonths = Integer.parseInt(this.viewTimePeriod);
                            int expectedDailyPoints = totalMonths * APPROX_DAYS_IN_MONTH;
                            if (maxDataPoints > totalMonths && Math.abs(maxDataPoints - expectedDailyPoints) < APPROX_DAYS_IN_MONTH ) {
                                xLabel = "Day " + (dataIdx + 1);
                            } else if (maxDataPoints == totalMonths) {
                                xLabel = "Mo " + (dataIdx +1);
                            }
                        } catch (NumberFormatException e) { /* Keep default T label */ }
                    }
                }
                canvas.drawText(xLabel, xPos, height - paddingBottom + dpToPx(15), textPaint);
            }
            canvas.drawText("Time", paddingLeft + graphWidth / 2f, height - paddingBottom + dpToPx(35), textPaint);

            Log.d(GRAPH_VIEW_TAG, "onDraw (" + this.hashCode() + "): Drawing " + datasetsToDraw.size() + " datasets.");
            for (DatasetForGraph dataset : datasetsToDraw) {
                if (dataset.points == null || dataset.points.length == 0) continue;
                Path currentPath = new Path();
                for (int i = 0; i < dataset.points.length; i++) {
                    if (i >= maxDataPoints && maxDataPoints > 0) break;
                    float x = paddingLeft + i * xInterval;
                    if (maxDataPoints == 1 && dataset.points.length == 1) x = paddingLeft + graphWidth / 2;
                    float y = paddingTop + (1 - dataset.points[i]) * graphHeight;
                    if (i == 0) currentPath.moveTo(x, y); else currentPath.lineTo(x, y);
                    canvas.drawCircle(x, y, dpToPx(3.5f), dataset.pointPaint != null ? dataset.pointPaint : dataPointPaintDefault);
                }
                if (dataset.points.length > 1) canvas.drawPath(currentPath, dataset.linePaint);
                Log.v(GRAPH_VIEW_TAG, "onDraw (" + this.hashCode() + "): Drew dataset '" + dataset.label + "' with " + dataset.points.length + " points.");
            }
            Log.d(GRAPH_VIEW_TAG, "onDraw (" + this.hashCode() + "): Finished drawing all datasets.");
        }
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
}