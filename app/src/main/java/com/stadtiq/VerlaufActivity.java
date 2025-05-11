package com.stadtiq;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;


public class VerlaufActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // TAG for logging, specific to this Activity
    private static final String TAG = "VerlaufActivity";
    // TAG for logging within the inner SimpleGraphView class
    private static final String GRAPH_VIEW_TAG = "SimpleGraphView";

    // UI elements
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Spinner spinnerCity;
    private Spinner spinnerDistrict;
    private LinearLayout timeOptionsContainer; // Holds TextViews for 1, 3, 6, 9, 12 months
    private FrameLayout graphContainer; // Container for the graph view or placeholder message
    private TextView txtGraphPlaceholderMessage; // Message shown when no graph can be displayed
    private SimpleGraphView simpleGraphView; // Custom view for drawing the graph

    // UI for selecting which values (CO2, PM2.5 etc.) to plot on the graph
    private HorizontalScrollView valueOptionsScrollView;
    private LinearLayout valueOptionsContainer; // Holds TextViews for each plottable value
    private List<TextView> valueOptionTextViews = new ArrayList<>(); // References to the value TextViews
    private List<String> selectedGraphDataKeys = new ArrayList<>(); // Stores DATA KEYS of values selected for graphing

    // Mappings for graph appearance and data generation
    private Map<String, Integer> valueColors = new LinkedHashMap<>(); // Maps data keys to colors for graph lines
    private Map<String, float[]> baseGraphDataPatterns = new HashMap<>(); // Base patterns for generating graph data

    // Data for spinners and time period selection
    private List<String> cities;
    private Map<String, List<String>> districts;
    private List<String> timePeriods; // Represents months: "1", "3", "6", "9", "12"
    private TextView selectedTimeOption = null; // Currently selected time period TextView

    // Key for storing and retrieving language preference
    private static final String PREF_LANG_CODE = "pref_language_code";
    // Constants for graph data generation
    private static final int APPROX_DAYS_IN_MONTH = 30; // Used for calculating data points
    private static final int MAX_DAYS_FOR_FULL_YEAR_GRAPH = 365; // Max data points for a "year"
    // Tracks the language code to detect changes on resume/restart
    public String lastKnownLangCode = null;

    // Permission request code for storage
    private static final int REQUEST_STORAGE_PERMISSION = 101;

    // To store export options temporarily if permission needs to be requested
    private Bitmap.CompressFormat pendingExportFormat = Bitmap.CompressFormat.PNG;
    private int pendingExportQuality = 90;
    private boolean pendingExportIncludeLegend = true;


    // --- CONSTANT DATA KEYS (Match MainActivity for consistency) ---
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
        Log.d(TAG, "onCreate: Initial lastKnownLangCode set to '" + lastKnownLangCode + "'.");

        setContentView(R.layout.activity_verlauf);
        Log.d(TAG, "onCreate: ContentView (R.layout.activity_verlauf) set.");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Log.d(TAG, "onCreate: Toolbar (R.id.toolbar) setup as ActionBar.");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.title_activity_verlauf);
            Log.d(TAG, "onCreate: ActionBar title set to R.string.title_activity_verlauf.");
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
            simpleGraphView.setVisibility(View.VISIBLE); // Show empty graph structure initially
            Log.d(TAG, "onCreate: SimpleGraphView added to graphContainer. Initial visibility: VISIBLE.");
        } else {
            Log.e(TAG, "onCreate: graphContainer is NULL! SimpleGraphView cannot be added.");
        }

        initializeValueColors();
        initializeCityDistrictData();
        initializeBaseGraphDataPatterns();
        initializeTimePeriods();
        setupValueOptionTextViews();
        Log.d(TAG, "onCreate: Data and ValueOption TextViews initialized.");

        setupCitySpinner();
        setupDistrictSpinner();
        setupTimeOptionClicks();

        handleSelectionChange(); // Initial call to set up graph (possibly empty)
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
            return;
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
        // Make export option visible only in VerlaufActivity
        MenuItem exportItem = menu.findItem(R.id.action_export_graph);
        if (exportItem != null) {
            exportItem.setVisible(true);
        }
        Log.d(TAG, "onCreateOptionsMenu: Menu inflated. Export graph item set to visible.");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: Item ID " + item.getItemId() + " (" + getResources().getResourceEntryName(item.getItemId()) + ") selected.");
        int itemId = item.getItemId();
        if (itemId == R.id.action_language) {
            Log.d(TAG, "onOptionsItemSelected: Language action selected.");
            showLanguageSelectionDialog();
            return true;
        } else if (itemId == R.id.action_export_graph) {
            Log.d(TAG, "onOptionsItemSelected: Export graph action selected.");
            exportGraphWithOptions(); // Call the method that shows the options dialog
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
            intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        } else if (id == R.id.nav_verlauf) {
            Log.d(TAG, "onNavigationItemSelected: Verlauf selected (current activity).");
            navigationView.setCheckedItem(R.id.nav_verlauf);
        } else if (id == R.id.nav_impressum) {
            Log.d(TAG, "onNavigationItemSelected: Navigating to ImpressumActivity.");
            intent = new Intent(this, ImpressumActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
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
        Log.d(TAG, "showLanguageSelectionDialog: Initiating language selection dialog.");
        LayoutInflater inflater = LayoutInflater.from(this);
        View customDialogView = inflater.inflate(R.layout.dialog_language_selection, null);
        final RadioGroup radioGroupLanguage = customDialogView.findViewById(R.id.radio_group_language);
        RadioButton radioButtonEnglish = customDialogView.findViewById(R.id.radio_button_english);
        RadioButton radioButtonGerman = customDialogView.findViewById(R.id.radio_button_german);
        String currentLangCode = getLanguageCode(this);
        Log.d(TAG, "showLanguageSelectionDialog: Current language code is '" + currentLangCode + "'.");
        if ("de".equals(currentLangCode)) {
            radioButtonGerman.setChecked(true);
        } else {
            radioButtonEnglish.setChecked(true);
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.dialog_select_language_title);
        builder.setView(customDialogView);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            int selectedRadioButtonId = radioGroupLanguage.getCheckedRadioButtonId();
            String selectedLangCode = "en";
            if (selectedRadioButtonId == R.id.radio_button_german) selectedLangCode = "de";
            Log.i(TAG, "showLanguageSelectionDialog: Positive button. Setting locale to '" + selectedLangCode + "'.");
            setLocale(selectedLangCode);
            dialog.dismiss();
        });
        builder.setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> {
            Log.d(TAG, "showLanguageSelectionDialog: Language selection dialog cancelled.");
            dialog.dismiss();
        });
        builder.create().show();
        Log.d(TAG, "showLanguageSelectionDialog: Dialog shown.");
    }

    private void setLocale(String langCode) {
        Log.i(TAG, "setLocale: Attempting to set language to '" + langCode + "'.");
        String currentLangCode = getLanguageCode(this);
        if (!currentLangCode.equals(langCode)) {
            Log.d(TAG, "setLocale: Language changing from '" + currentLangCode + "' to '" + langCode + "'.");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putString(PREF_LANG_CODE, langCode).apply();
            this.lastKnownLangCode = langCode;
            Log.i(TAG, "setLocale: Recreating activity.");
            recreate();
        } else {
            Log.d(TAG, "setLocale: Language is already '" + currentLangCode + "'. No action.");
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
            Log.e(TAG, "initializeBaseGraphDataPatterns: 'Braunschweig' districts not found!");
            allDistrictsForPatterns = new ArrayList<>(Arrays.asList(getString(R.string.hint_select_district), "default_district_fallback"));
        }
        for (String dataKey : ALL_VALUE_DATA_KEYS_FOR_GRAPH) {
            for (String districtName : allDistrictsForPatterns) {
                if (districtName.equals(getString(R.string.hint_select_district))) continue;
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
        Log.d(TAG, "initializeBaseGraphDataPatterns: Generated " + baseGraphDataPatterns.size() + " patterns.");
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
                Log.i(TAG, "City Spinner: Item selected - '" + selectedCity + "'.");
                updateDistrictSpinner(selectedCity);
                handleSelectionChange();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
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
                handleSelectionChange();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        Log.d(TAG, "setupDistrictSpinner: Finished.");
    }

    private void updateDistrictSpinner(String selectedCity) {
        Log.d(TAG, "updateDistrictSpinner: Updating for city '" + selectedCity + "'.");
        List<String> districtList = districts.get(selectedCity);
        if (districtList == null) {
            districtList = new ArrayList<>(Arrays.asList(getString(R.string.hint_select_district)));
        }
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districtList);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistrict.setAdapter(districtAdapter);
        spinnerDistrict.setSelection(0);
        Log.d(TAG, "updateDistrictSpinner: Adapter set. List size: " + districtList.size());
    }

    private void setupTimeOptionClicks() {
        Log.d(TAG, "setupTimeOptionClicks: Setting up listeners.");
        if(timeOptionsContainer == null) {
            Log.e(TAG, "setupTimeOptionClicks: timeOptionsContainer is NULL.");
            return;
        }
        for (int i = 0; i < timeOptionsContainer.getChildCount(); i++) {
            View child = timeOptionsContainer.getChildAt(i);
            if (child instanceof TextView) {
                TextView timeOptionTV = (TextView) child;
                timeOptionTV.setBackgroundResource(R.drawable.value_option_selector);
                timeOptionTV.setTextColor(ContextCompat.getColorStateList(this, R.color.default_text_color_selector_verlauf));
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
            Log.d(TAG, "selectTimeOption: Deselecting previous: '" + selectedTimeOption.getText().toString() + "'.");
            selectedTimeOption.setSelected(false);
            selectedTimeOption.setBackgroundResource(R.drawable.value_option_selector);
            selectedTimeOption.setTextColor(ContextCompat.getColorStateList(this, R.color.default_text_color_selector_verlauf));
        }
        selectedTextView.setSelected(true);

        TypedValue backgroundTypedValue = new TypedValue();
        VerlaufActivity.this.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, backgroundTypedValue, true);
        int selectedBackgroundColor = backgroundTypedValue.resourceId != 0 ?
                ContextCompat.getColor(this, backgroundTypedValue.resourceId) : backgroundTypedValue.data;

        int selectedTextColor = Color.BLACK; // Force black text for selected time options

        selectedTextView.setBackgroundColor(selectedBackgroundColor);
        selectedTextView.setTextColor(selectedTextColor);

        selectedTimeOption = selectedTextView;
        Log.d(TAG, "selectTimeOption: New selected: '" + selectedTimeOption.getText().toString() +
                "'. BG: 0x" + Integer.toHexString(selectedBackgroundColor) +
                ", Text: 0x" + Integer.toHexString(selectedTextColor));
        handleSelectionChange();
    }

    private void setupValueOptionTextViews() {
        Log.d(TAG, "setupValueOptionTextViews: Setting up TextViews.");
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
        Log.d(TAG, "setupValueOptionTextViews: Finished creating " + valueOptionTextViews.size() + " TextViews.");
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
            Integer graphLineColor = valueColors.get(dataKey);
            if (graphLineColor == null) {
                TypedValue fallbackBg = new TypedValue();
                VerlaufActivity.this.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, fallbackBg, true);
                graphLineColor = fallbackBg.resourceId != 0 ? ContextCompat.getColor(this, fallbackBg.resourceId) : fallbackBg.data;
            }
            valueTV.setBackgroundColor(graphLineColor);
            valueTV.setTextColor(Color.BLACK); // Force black text for selected value options
            Log.d(TAG, "updateValueTextViewAppearance: '" + dataKey + "' (Selected). BG: 0x" + Integer.toHexString(graphLineColor) + ", Text: BLACK");
        } else {
            valueTV.setBackgroundResource(R.drawable.value_option_selector);
            valueTV.setTextColor(ContextCompat.getColorStateList(this, R.color.default_text_color_selector_verlauf));
            Log.d(TAG, "updateValueTextViewAppearance: '" + dataKey + "' (Not Selected). Using default.");
        }
    }

    private void handleSelectionChange() {
        Log.i(TAG, "handleSelectionChange: Evaluating selections.");
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

        boolean isCityProperlySelectedForData = !getString(R.string.hint_select_city).equals(selectedCity);
        boolean isTimeSelectedForData = selectedTimeOption != null;
        boolean atLeastOneValueSelectedForData = !selectedGraphDataKeys.isEmpty();

        Log.d(TAG, "handleSelectionChange: CityProper=" + isCityProperlySelectedForData +
                ", TimeSelected=" + (isTimeSelectedForData ? selectedTimeOption.getText().toString() : "null") +
                ", ValuesSelectedCount=" + selectedGraphDataKeys.size() +
                ", DistrictForDisplay='" + selectedDistrictForDisplay + "', DistrictForDataPattern='" + districtKeyForDataPattern + "'");

        if (simpleGraphView == null || txtGraphPlaceholderMessage == null) return;

        txtGraphPlaceholderMessage.setVisibility(View.GONE);
        if (graphContainer != null) graphContainer.setVisibility(View.VISIBLE);
        simpleGraphView.setVisibility(View.VISIBLE);

        if (isCityProperlySelectedForData && isTimeSelectedForData && atLeastOneValueSelectedForData) {
            Log.d(TAG, "handleSelectionChange: Conditions met. Displaying graph with data.");
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
            Log.d(TAG, "handleSelectionChange: Conditions not met. Displaying empty graph structure.");
            String timePeriodForEmptyGraph = isTimeSelectedForData ? selectedTimeOption.getText().toString() : null;
            String cityForEmptyGraph = (selectedCity != null && !getString(R.string.hint_select_city).equals(selectedCity)) ? selectedCity : null;
            String districtForEmptyGraph = (selectedDistrictForDisplay != null && !getString(R.string.hint_select_district).equals(selectedDistrictForDisplay)) ?
                    selectedDistrictForDisplay : null;
            simpleGraphView.updateData(cityForEmptyGraph, districtForEmptyGraph, new ArrayList<>(), timePeriodForEmptyGraph);
        }
    }

    private void exportGraphWithOptions() {
        if (simpleGraphView == null || simpleGraphView.getWidth() == 0 || simpleGraphView.getHeight() == 0) {
            Toast.makeText(this, "Graph is not available for export.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "exportGraphWithOptions: SimpleGraphView is null or not measured.");
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_export_options, null);

        RadioGroup rgFormat = dialogView.findViewById(R.id.rg_export_format);
        RadioButton rbPng = dialogView.findViewById(R.id.rb_format_png);
        LinearLayout layoutJpegQuality = dialogView.findViewById(R.id.layout_jpeg_quality);
        SeekBar seekBarQuality = dialogView.findViewById(R.id.seekbar_jpeg_quality);
        TextView tvQualityValue = dialogView.findViewById(R.id.tv_jpeg_quality_value);
        CheckBox cbIncludeLegend = dialogView.findViewById(R.id.cb_include_legend);

        layoutJpegQuality.setVisibility(View.GONE); // PNG is default
        tvQualityValue.setText(String.valueOf(seekBarQuality.getProgress() + 1));

        rgFormat.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_format_jpeg) {
                layoutJpegQuality.setVisibility(View.VISIBLE);
            } else {
                layoutJpegQuality.setVisibility(View.GONE);
            }
        });

        seekBarQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvQualityValue.setText(String.valueOf(progress + 1));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.dialog_title_export_options))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.export_button), (dialog, which) -> {
                    pendingExportFormat = rbPng.isChecked() ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
                    pendingExportQuality = seekBarQuality.getProgress() + 1;
                    pendingExportIncludeLegend = cbIncludeLegend.isChecked();

                    Log.d(TAG, "Export options: Format=" + pendingExportFormat + ", Quality=" + pendingExportQuality + ", IncludeLegend=" + pendingExportIncludeLegend);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        saveGraphToMediaStore(pendingExportFormat, pendingExportQuality, pendingExportIncludeLegend);
                    } else {
                        if (ContextCompat.checkSelfPermission(VerlaufActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            saveGraphToLegacyStorage(pendingExportFormat, pendingExportQuality, pendingExportIncludeLegend);
                        } else {
                            ActivityCompat.requestPermissions(VerlaufActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }


    private Bitmap getGraphBitmap(boolean includeLegend) {
        if (simpleGraphView == null || simpleGraphView.getWidth() == 0 || simpleGraphView.getHeight() == 0) {
            Log.e(TAG, "getGraphBitmap: GraphView is not ready or has no dimensions.");
            return null;
        }

        int legendHeight = 0;
        TextPaint legendTextPaint = null;
        float legendItemHeight = 0;
        float legendPadding = simpleGraphView.dpToPx(10f);
        float legendColorBoxSize = simpleGraphView.dpToPx(15f);
        float legendTextOffset = simpleGraphView.dpToPx(5f);
        float legendTextMarginBottom = simpleGraphView.dpToPx(4f);


        if (includeLegend && simpleGraphView.datasetsToDraw != null && !simpleGraphView.datasetsToDraw.isEmpty()) {
            legendTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            legendTextPaint.setTextSize(simpleGraphView.dpToPx(12f));
            TypedValue legendTextColorVal = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.textColorPrimary, legendTextColorVal, true);
            legendTextPaint.setColor(legendTextColorVal.resourceId != 0 ? ContextCompat.getColor(this, legendTextColorVal.resourceId) : legendTextColorVal.data);

            Paint.FontMetrics fm = legendTextPaint.getFontMetrics();
            legendItemHeight = (fm.descent - fm.ascent) + legendTextMarginBottom;
            legendHeight = (int) (simpleGraphView.datasetsToDraw.size() * legendItemHeight + 2 * legendPadding - legendTextMarginBottom);
            if (legendHeight < 0) legendHeight = 0;
        }

        int totalBitmapHeight = simpleGraphView.getHeight() + legendHeight;
        Bitmap bitmap = Bitmap.createBitmap(simpleGraphView.getWidth(), totalBitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        TypedValue bgValue = new TypedValue();
        this.getTheme().resolveAttribute(android.R.attr.colorBackground, bgValue, true);
        canvas.drawColor(bgValue.resourceId != 0 ? ContextCompat.getColor(this, bgValue.resourceId) : bgValue.data);

        simpleGraphView.draw(canvas);

        if (includeLegend && legendHeight > 0 && legendTextPaint != null) {
            Log.d(TAG, "Drawing legend. Calculated Height: " + legendHeight);
            float currentY = simpleGraphView.getHeight() + legendPadding;
            float startX = legendPadding;

            for (SimpleGraphView.DatasetForGraph dataset : simpleGraphView.datasetsToDraw) {
                if (dataset.label != null && dataset.linePaint != null) {
                    Paint boxPaint = new Paint(dataset.linePaint);
                    boxPaint.setStyle(Paint.Style.FILL);
                    canvas.drawRect(startX, currentY, startX + legendColorBoxSize, currentY + legendColorBoxSize, boxPaint);

                    float textBaselineY = currentY + (legendColorBoxSize / 2f) - (legendTextPaint.descent() + legendTextPaint.ascent()) / 2f;
                    canvas.drawText(dataset.label, startX + legendColorBoxSize + legendTextOffset, textBaselineY, legendTextPaint);
                    currentY += legendItemHeight;
                }
            }
        }
        Log.d(TAG, "getGraphBitmap: Bitmap created. Include legend: " + includeLegend);
        return bitmap;
    }

    private void saveGraphToMediaStore(Bitmap.CompressFormat format, int quality, boolean includeLegend) {
        Bitmap bitmap = getGraphBitmap(includeLegend);
        if (bitmap == null) {
            Toast.makeText(this, getString(R.string.graph_export_failed) + " (Bitmap error)", Toast.LENGTH_LONG).show();
            return;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileExtension = (format == Bitmap.CompressFormat.PNG) ? ".png" : ".jpg";
        String mimeType = (format == Bitmap.CompressFormat.PNG) ? "image/png" : "image/jpeg";
        String fileName = "StadtIQ_Graph_" + timeStamp + fileExtension;
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "StadtIQ");
        Uri uri = null;
        try {
            uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                    if (outputStream != null) {
                        bitmap.compress(format, quality, outputStream);
                        Toast.makeText(this, getString(R.string.graph_export_success), Toast.LENGTH_LONG).show();
                        Log.i(TAG, "saveGraphToMediaStore: Graph saved as " + format + " to " + uri.toString());
                    } else {
                        throw new IOException("Failed to get output stream for MediaStore URI.");
                    }
                }
            } else {
                throw new IOException("Failed to create new MediaStore record.");
            }
        } catch (IOException e) {
            Log.e(TAG, "saveGraphToMediaStore: Error saving graph", e);
            Toast.makeText(this, getString(R.string.graph_export_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (uri != null) {
                getContentResolver().delete(uri, null, null);
            }
        } finally {
            bitmap.recycle();
        }
    }

    private void saveGraphToLegacyStorage(Bitmap.CompressFormat format, int quality, boolean includeLegend) {
        Bitmap bitmap = getGraphBitmap(includeLegend);
        if (bitmap == null) {
            Toast.makeText(this, getString(R.string.graph_export_failed) + " (Bitmap error)", Toast.LENGTH_LONG).show();
            return;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileExtension = (format == Bitmap.CompressFormat.PNG) ? ".png" : ".jpg";
        String fileName = "StadtIQ_Graph_" + timeStamp + fileExtension;
        File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File stadtIQDir = new File(picturesDir, "StadtIQ");
        if (!stadtIQDir.exists() && !stadtIQDir.mkdirs()) {
            Log.e(TAG, "saveGraphToLegacyStorage: Failed to create directory: " + stadtIQDir.getAbsolutePath());
            Toast.makeText(this, getString(R.string.graph_export_failed) + " (Dir error)", Toast.LENGTH_LONG).show();
            bitmap.recycle();
            return;
        }
        File imageFile = new File(stadtIQDir, fileName);
        try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
            bitmap.compress(format, quality, outputStream);
            outputStream.flush();
            Toast.makeText(this, getString(R.string.graph_export_success) + ": " + imageFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            Log.i(TAG, "saveGraphToLegacyStorage: Graph saved as " + format + " to " + imageFile.getAbsolutePath());
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(imageFile);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
        } catch (IOException e) {
            Log.e(TAG, "saveGraphToLegacyStorage: Error saving graph", e);
            Toast.makeText(this, getString(R.string.graph_export_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            bitmap.recycle();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: WRITE_EXTERNAL_STORAGE permission granted.");
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    saveGraphToLegacyStorage(pendingExportFormat, pendingExportQuality, pendingExportIncludeLegend);
                } else {
                    // This case should ideally not be hit if permission is only asked for legacy.
                    // However, if it is, MediaStore doesn't need the explicit permission.
                    saveGraphToMediaStore(pendingExportFormat, pendingExportQuality, pendingExportIncludeLegend);
                }
            } else {
                Log.w(TAG, "onRequestPermissionsResult: WRITE_EXTERNAL_STORAGE permission denied.");
                Toast.makeText(this, getString(R.string.permission_needed_for_storage), Toast.LENGTH_LONG).show();
            }
        }
    }


    // Inner class SimpleGraphView
    public class SimpleGraphView extends View {
        private Paint axisPaint, textPaint, gridPaint, dataPointPaintDefault, titleTextPaint;
        private TextPaint emptyMessagePaint;
        private List<DatasetForGraph> datasetsToDraw;
        private String city, districtForTitle, viewTimePeriod;
        private String emptyGraphMessage = "Select City, Time, and Value(s)";

        public class DatasetForGraph {
            float[] points; Paint linePaint; Paint pointPaint; String label;
            public DatasetForGraph(float[] points, Paint linePaint, String label) {
                this.points = points; this.linePaint = linePaint; this.label = label;
                this.pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                this.pointPaint.setStyle(Paint.Style.FILL);
                this.pointPaint.setColor(this.linePaint != null ? this.linePaint.getColor() : ContextCompat.getColor(getContext(), R.color.colorPrimaryVariant));
            }
        }

        public SimpleGraphView(Context context) { super(context); init(); }
        public SimpleGraphView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

        private int getColorFromAttr(int attrResId, int defaultColorResId) {
            TypedValue typedValue = new TypedValue();
            getContext().getTheme().resolveAttribute(attrResId, typedValue, true);
            if (typedValue.resourceId != 0) {
                return ContextCompat.getColor(getContext(), typedValue.resourceId);
            } else if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return typedValue.data;
            }
            Log.w(GRAPH_VIEW_TAG, "Failed to resolve attribute. Using default: " + getResources().getResourceEntryName(defaultColorResId));
            return ContextCompat.getColor(getContext(), defaultColorResId);
        }

        private void init() {
            Log.d(GRAPH_VIEW_TAG, "init: Initializing (" + this.hashCode() + ").");
            datasetsToDraw = new ArrayList<>();
            int axisColor = getColorFromAttr(R.attr.graphAxisColor, R.color.graph_axis);
            int gridColor = getColorFromAttr(R.attr.graphGridColor, R.color.graph_grid);
            int labelTextColor = getColorFromAttr(R.attr.graphTextColor, R.color.graph_text);
            int titleTextColor = getColorFromAttr(android.R.attr.textColorPrimary, R.color.textColorPrimary);
            int defaultPointColor = getColorFromAttr(com.google.android.material.R.attr.colorPrimaryVariant, R.color.colorPrimaryVariant);

            axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG); axisPaint.setColor(axisColor); axisPaint.setStrokeWidth(dpToPx(1.5f));
            gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG); gridPaint.setColor(gridColor); gridPaint.setStrokeWidth(dpToPx(0.5f)); gridPaint.setStyle(Paint.Style.STROKE);
            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG); textPaint.setColor(labelTextColor); textPaint.setTextSize(dpToPx(10f)); textPaint.setTextAlign(Paint.Align.CENTER);
            titleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG); titleTextPaint.setColor(titleTextColor); titleTextPaint.setTextSize(dpToPx(12f)); titleTextPaint.setTextAlign(Paint.Align.CENTER); titleTextPaint.setFakeBoldText(true);
            dataPointPaintDefault = new Paint(Paint.ANTI_ALIAS_FLAG); dataPointPaintDefault.setStyle(Paint.Style.FILL); dataPointPaintDefault.setColor(defaultPointColor);

            emptyMessagePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            emptyMessagePaint.setColor(getColorFromAttr(android.R.attr.textColorSecondary, R.color.textColorSecondary));
            emptyMessagePaint.setTextSize(dpToPx(14f));
            emptyMessagePaint.setTextAlign(Paint.Align.CENTER);
        }

        public float dpToPx(float dp) {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
        }

        public float[] getDeterministicDataPoints(String dataKey, String districtKeyForPattern, String timePeriodText, int daysInMonthConstant) {
            Log.d(GRAPH_VIEW_TAG, "getDeterministicDataPoints: For " + dataKey + ", " + districtKeyForPattern + ", " + timePeriodText);
            String patternKey = dataKey + "_" + (districtKeyForPattern != null ? districtKeyForPattern : "default_district");
            float[] basePattern = baseGraphDataPatterns.get(patternKey);
            if (basePattern == null) {
                Log.w(GRAPH_VIEW_TAG, "No pattern for '" + patternKey + "'. Using default for " + dataKey);
                basePattern = baseGraphDataPatterns.get(dataKey + "_default_district");
                if(basePattern == null) return new float[0];
            }
            int numPointsToExtract;
            if (timePeriodText == null || timePeriodText.isEmpty()) numPointsToExtract = daysInMonthConstant;
            else if (timePeriodText.equals(getContext().getString(R.string.time_option_all))) numPointsToExtract = MAX_DAYS_FOR_FULL_YEAR_GRAPH;
            else {
                try {
                    numPointsToExtract = Integer.parseInt(timePeriodText) * daysInMonthConstant;
                } catch (NumberFormatException e) {
                    numPointsToExtract = daysInMonthConstant;
                }
            }
            numPointsToExtract = Math.min(numPointsToExtract, basePattern.length);
            if (numPointsToExtract <= 0) numPointsToExtract = Math.min(2, basePattern.length > 0 ? basePattern.length : (basePattern.length == 1 ? 1 : 2) );
            if (basePattern.length == 1 && numPointsToExtract > 0) numPointsToExtract = 1;
            if (numPointsToExtract > basePattern.length || numPointsToExtract < 0) return new float[0];
            return Arrays.copyOfRange(basePattern, 0, numPointsToExtract);
        }

        public void updateData(String city, String districtForTitleDisplay, List<DatasetForGraph> datasets, String overallTimePeriod) {
            this.city = city; this.districtForTitle = districtForTitleDisplay;
            this.datasetsToDraw = new ArrayList<>(datasets); this.viewTimePeriod = overallTimePeriod;
            if (datasetsToDraw.isEmpty()) {
                if (city == null) emptyGraphMessage = getString(R.string.placeholder_select_city_verlauf);
                else if (overallTimePeriod == null || overallTimePeriod.isEmpty()) emptyGraphMessage = getString(R.string.placeholder_select_time);
                else emptyGraphMessage = getString(R.string.placeholder_select_at_least_one_value);
            }
            Log.i(GRAPH_VIEW_TAG, "updateData: Received " + datasetsToDraw.size() + " datasets. City=" + city);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth(); int height = getHeight();
            if (width == 0 || height == 0) return;

            TypedValue bgValue = new TypedValue();
            getContext().getTheme().resolveAttribute(android.R.attr.colorBackground, bgValue, true);
            canvas.drawColor(bgValue.resourceId != 0 ? ContextCompat.getColor(getContext(), bgValue.resourceId) : bgValue.data);


            float paddingLeft = dpToPx(50f); float paddingRight = dpToPx(20f);
            float paddingTop = dpToPx(50f); float paddingBottom = dpToPx(50f);
            float graphWidth = width - paddingLeft - paddingRight;
            float graphHeight = height - paddingTop - paddingBottom;

            String title = "";
            if (datasetsToDraw != null && !datasetsToDraw.isEmpty()) {
                if (datasetsToDraw.size() == 1 && datasetsToDraw.get(0).label != null) title = datasetsToDraw.get(0).label + " Trend";
                else {
                    StringBuilder labels = new StringBuilder();
                    for (int i = 0; i < datasetsToDraw.size(); i++) {
                        labels.append(datasetsToDraw.get(i).label);
                        if (i < datasetsToDraw.size() - 1) labels.append(" & ");
                    }
                    title = labels.toString() + " Trends";
                }
            } else title = "Historical Data Overview";
            String contextSuffix = ""; boolean cityShown = false;
            if (city != null && !city.equals(getString(R.string.hint_select_city))) {
                contextSuffix += " in " + city;
                cityShown = true;
            }
            if (districtForTitle != null && !districtForTitle.equals(getString(R.string.hint_select_district))) {
                if (cityShown) contextSuffix += "/" + districtForTitle;
                else contextSuffix += " in " + districtForTitle;
            }
            if (viewTimePeriod != null && !viewTimePeriod.isEmpty()) {
                contextSuffix += ((cityShown || (districtForTitle != null && !districtForTitle.equals(getString(R.string.hint_select_district)))) ? " " : "") + "(" + viewTimePeriod + (viewTimePeriod.matches("\\d+") ? " mo" : "") + ")";
            }
            title += contextSuffix;
            canvas.drawText(title, width / 2f, paddingTop / 2f + titleTextPaint.getTextSize() / 3f, titleTextPaint);

            canvas.drawLine(paddingLeft, paddingTop, paddingLeft, height - paddingBottom, axisPaint);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            for (int i = 0; i <= 4; i++) {
                float yPos = paddingTop + (graphHeight * ((float)i / 4f));
                canvas.drawText(String.format(Locale.getDefault(), "%.0f", 100f * (1f - (float)i/4f)), paddingLeft-dpToPx(6f), yPos+textPaint.getTextSize()/3f, textPaint);
                if(i < 4) canvas.drawLine(paddingLeft, yPos, width - paddingRight, yPos, gridPaint);
            }
            textPaint.setTextAlign(Paint.Align.CENTER); canvas.save();
            canvas.rotate(-90, paddingLeft/2f-dpToPx(4f), paddingTop+graphHeight/2f);
            canvas.drawText("Normalized Value (%)", paddingLeft/2f-dpToPx(4f), paddingTop+graphHeight/2f + textPaint.getTextSize()/2f, textPaint);
            canvas.restore();

            canvas.drawLine(paddingLeft, height - paddingBottom, width - paddingRight, height - paddingBottom, axisPaint);
            int xAxisScalingMaxPoints;
            if (datasetsToDraw != null && !datasetsToDraw.isEmpty()) {
                xAxisScalingMaxPoints = 0;
                for (DatasetForGraph ds : datasetsToDraw) if (ds.points != null) xAxisScalingMaxPoints = Math.max(xAxisScalingMaxPoints, ds.points.length);
                if (xAxisScalingMaxPoints == 0) xAxisScalingMaxPoints = 1;
            } else {
                if (viewTimePeriod != null && viewTimePeriod.matches("\\d+")) {
                    try { xAxisScalingMaxPoints = Integer.parseInt(viewTimePeriod) * APPROX_DAYS_IN_MONTH; }
                    catch (NumberFormatException e) { xAxisScalingMaxPoints = APPROX_DAYS_IN_MONTH; }
                } else if (viewTimePeriod != null && viewTimePeriod.equals(getContext().getString(R.string.time_option_all))) {
                    xAxisScalingMaxPoints = MAX_DAYS_FOR_FULL_YEAR_GRAPH;
                } else xAxisScalingMaxPoints = APPROX_DAYS_IN_MONTH;
                if (xAxisScalingMaxPoints == 0) xAxisScalingMaxPoints = 1;
            }
            float xInterval = (xAxisScalingMaxPoints > 1) ? graphWidth / (xAxisScalingMaxPoints - 1) : graphWidth;
            int numXLabels = Math.min(xAxisScalingMaxPoints, xAxisScalingMaxPoints <=5 ? xAxisScalingMaxPoints : 5);
            if (xAxisScalingMaxPoints == 1) numXLabels = 1;
            if (numXLabels == 0 && xAxisScalingMaxPoints > 0) numXLabels = 1;

            for (int i = 0; i < numXLabels; i++) {
                int dataIdx = (numXLabels <= 1 || xAxisScalingMaxPoints <=1 ) ? 0 : Math.round(i * (float)(xAxisScalingMaxPoints - 1) / (numXLabels - 1));
                float xPos = paddingLeft + dataIdx * xInterval;
                if (xAxisScalingMaxPoints == 1) xPos = paddingLeft + graphWidth / 2f;
                String xLabel = "T" + (dataIdx + 1);
                if (this.viewTimePeriod != null && !this.viewTimePeriod.isEmpty()) {
                    if (this.viewTimePeriod.equals(getContext().getString(R.string.time_option_all))) xLabel = "Seg." + (dataIdx + 1);
                    else if (this.viewTimePeriod.matches("\\d+")) {
                        try {
                            int totalMonths = Integer.parseInt(this.viewTimePeriod);
                            int actualPointsToConsiderForLabeling = (datasetsToDraw != null && !datasetsToDraw.isEmpty() && datasetsToDraw.get(0).points != null) ? datasetsToDraw.get(0).points.length : xAxisScalingMaxPoints;
                            if (actualPointsToConsiderForLabeling > totalMonths && actualPointsToConsiderForLabeling <= totalMonths * APPROX_DAYS_IN_MONTH * 1.2) xLabel = "Day " + (dataIdx + 1);
                            else if (actualPointsToConsiderForLabeling == totalMonths || (datasetsToDraw != null && datasetsToDraw.isEmpty() && xAxisScalingMaxPoints == totalMonths)) xLabel = "Mo " + (dataIdx +1);
                        } catch (NumberFormatException e) { /* Keep default */ }
                    }
                }
                canvas.drawText(xLabel, xPos, height - paddingBottom + dpToPx(15f), textPaint);
            }
            String xAxisTitle = getContext().getString(R.string.graph_x_axis_label_time);
            canvas.drawText(xAxisTitle, paddingLeft + graphWidth / 2f, height - paddingBottom + dpToPx(35f), textPaint);

            if (datasetsToDraw != null && !datasetsToDraw.isEmpty()) {
                for (DatasetForGraph dataset : datasetsToDraw) {
                    if (dataset.points == null || dataset.points.length == 0) continue;
                    Path currentPath = new Path();
                    int currentDatasetPointCount = dataset.points.length;
                    float currentXInterval = (currentDatasetPointCount > 1) ? graphWidth / (currentDatasetPointCount - 1) : graphWidth;
                    for (int i = 0; i < currentDatasetPointCount; i++) {
                        float x = paddingLeft + i * currentXInterval;
                        if (currentDatasetPointCount == 1) x = paddingLeft + graphWidth / 2f;
                        float y = paddingTop + (1 - dataset.points[i]) * graphHeight;
                        if (i == 0) currentPath.moveTo(x, y); else currentPath.lineTo(x, y);
                        canvas.drawCircle(x, y, dpToPx(3.5f), dataset.pointPaint != null ? dataset.pointPaint : dataPointPaintDefault);
                    }
                    if (currentDatasetPointCount > 1) canvas.drawPath(currentPath, dataset.linePaint);
                }
            } else {
                emptyMessagePaint.setTextAlign(Paint.Align.CENTER);
                float textMaxWidth = graphWidth - dpToPx(20f);
                if (textMaxWidth <= 0) textMaxWidth = width - dpToPx(40f);

                if (textMaxWidth > 0 && graphHeight > 0) {
                    StaticLayout staticLayout = StaticLayout.Builder.obtain(
                                    emptyGraphMessage, 0, emptyGraphMessage.length(),
                                    emptyMessagePaint, (int) textMaxWidth)
                            .setAlignment(Layout.Alignment.ALIGN_CENTER)
                            .setLineSpacing(0, 1.0f)
                            .setIncludePad(false)
                            .build();
                    float textX = paddingLeft + (graphWidth - staticLayout.getWidth()) / 2f;
                    float textY = paddingTop + (graphHeight - staticLayout.getHeight()) / 2f;
                    canvas.save();
                    canvas.translate(textX, textY);
                    staticLayout.draw(canvas);
                    canvas.restore();
                } else {
                    canvas.drawText(emptyGraphMessage, paddingLeft + graphWidth / 2f, paddingTop + graphHeight / 2f, emptyMessagePaint);
                }
            }
        }
    }

    // Standard lifecycle logging
    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
}