package com.stadtiq;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Spinner spinnerCity;
    private Spinner spinnerDistrict;
    private FrameLayout displayAreaContainer;
    private TextView txtPlaceholderMessage;
    private LinearLayout listViewContainer;
    private RecyclerView recyclerViewValues;
    private ImageButton btnListLayoutToggle;

    private List<String> cities;
    private Map<String, List<String>> districts;
    private Map<String, Map<String, Map<String, String>>> dummyData;

    private ValueAdapter valueAdapter;

    private static final String PREF_LANG_CODE = "pref_language_code";
    private int currentListLayoutColumns = 1;
    private String lastKnownLangCode = null;

    // --- CONSTANT DATA KEYS ---
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

    private final List<String> ALL_VALUE_DATA_KEYS = Arrays.asList(
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
        String lang = prefs.getString(PREF_LANG_CODE, "en"); // Default to "en"
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

        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: ContentView set.");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Log.d(TAG, "onCreate: Toolbar setup.");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            Log.d(TAG, "onCreate: ActionBar title set.");
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        Log.d(TAG, "onCreate: DrawerLayout and NavigationView found.");

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        if (drawerLayout != null) drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        if (navigationView != null) navigationView.setNavigationItemSelectedListener(this);
        Log.d(TAG, "onCreate: ActionBarDrawerToggle and NavigationView listener setup.");

        spinnerCity = findViewById(R.id.spinner_city);
        spinnerDistrict = findViewById(R.id.spinner_district);
        displayAreaContainer = findViewById(R.id.display_area_container);
        txtPlaceholderMessage = findViewById(R.id.txt_placeholder_message);
        Log.d(TAG, "onCreate: Basic UI elements found. displayAreaContainer: " + (displayAreaContainer != null));

        listViewContainer = findViewById(R.id.list_view_container);
        if (listViewContainer != null) {
            recyclerViewValues = listViewContainer.findViewById(R.id.recycler_view_values);
            btnListLayoutToggle = listViewContainer.findViewById(R.id.btn_list_layout_toggle);
            Log.d(TAG, "onCreate: ListViewContainer and its children found.");
        } else {
            Log.e(TAG, "onCreate: listViewContainer is NULL!");
        }

        initializeDummyData(); // Uses string resources, so sensitive to current locale
        initializeCityDistrictData(); // Uses string resources
        initializeRecyclerView();
        Log.d(TAG, "onCreate: Data and RecyclerView initialized.");

        setupCitySpinner();
        setupDistrictSpinner();
        setupListLayoutToggleButton();
        Log.d(TAG, "onCreate: Spinners and button listeners setup.");

        String initialCity = cities.get(spinnerCity.getSelectedItemPosition());
        Log.i(TAG, "onCreate: Initial city from spinner: " + initialCity);
        handleCityOrDistrictChange();
        Log.i(TAG, "onCreate: Finished.");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: Activity Restarted.");
        String currentLangCode = getLanguageCode(this);
        if (lastKnownLangCode != null && !lastKnownLangCode.equals(currentLangCode)) {
            Log.i(TAG, "onRestart: Language has changed from " + lastKnownLangCode + " to " + currentLangCode + ". Recreating MainActivity.");
            lastKnownLangCode = currentLangCode; // Update before recreating
            recreate();
        } else if (lastKnownLangCode == null) { // Should ideally be set in onCreate
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
            Log.i(TAG, "onResume: Language changed from " + lastKnownLangCode + " to " + currentLangCode + " while paused. Recreating MainActivity.");
            lastKnownLangCode = currentLangCode;
            recreate();
            return; // Avoid further onResume processing as activity will restart
        } else if (lastKnownLangCode == null) {
            lastKnownLangCode = currentLangCode;
        }


        if (navigationView != null) {
            MenuItem homeItem = navigationView.getMenu().findItem(R.id.nav_home);
            if (homeItem != null) {
                Log.d(TAG, "onResume: Setting 'Home' as checked. Current status before: " + homeItem.isChecked());
                navigationView.setCheckedItem(R.id.nav_home);
                Log.d(TAG, "onResume: Nav drawer item 'Home' isChecked after set: " + homeItem.isChecked());
            } else {
                Log.w(TAG, "onResume: nav_home menu item is null.");
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
            Log.d(TAG, "onNavigationItemSelected: Home selected (current activity).");
            navigationView.setCheckedItem(R.id.nav_home);
        } else if (id == R.id.nav_verlauf) {
            Log.d(TAG, "onNavigationItemSelected: Navigating to VerlaufActivity.");
            intent = new Intent(this, VerlaufActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        } else if (id == R.id.nav_impressum) {
            Log.d(TAG, "onNavigationItemSelected: Navigating to ImpressumActivity.");
            intent = new Intent(this, ImpressumActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }

        if (intent != null) {
            Log.d(TAG, "onNavigationItemSelected: Starting new activity: " + intent.getComponent().getClassName());
            startActivity(intent);
        } else {
            Log.d(TAG, "onNavigationItemSelected: No new activity started (likely already on the selected page).");
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showLanguageSelectionDialog() {
        Log.d(TAG, "showLanguageSelectionDialog: Called.");
        String[] languages = {
                getString(R.string.language_english),
                getString(R.string.language_german)
        };
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
            lastKnownLangCode = langCode; // Update lastKnownLangCode before recreating
            recreate();
        } else {
            Log.d(TAG, "setLocale: Language is already '" + currentLangCode + "'. No action needed.");
        }
    }

    private void initializeDummyData() {
        Log.d(TAG, "initializeDummyData: Starting with distinct fixed district data.");
        dummyData = new HashMap<>();
        Map<String, Map<String, String>> braunschweigCityData = new HashMap<>();

        Map<String, String> innerStadt = new HashMap<>();
        innerStadt.put(KEY_CO2, "480 ppm"); innerStadt.put(KEY_PM25, "28 µg/m³"); innerStadt.put(KEY_O2, "20.6 %");
        innerStadt.put(KEY_SO2, "7.5 ppb"); innerStadt.put(KEY_CH4, "2.2 ppm"); innerStadt.put(KEY_P, "1010.0 hPa");
        innerStadt.put(KEY_LP, "75 dB");    innerStadt.put(KEY_LX, "700 lx");    innerStadt.put(KEY_TD, "9.5 °C");
        innerStadt.put(KEY_ABS_HUMIDITY, "10.0 g/m³");
        braunschweigCityData.put("Innere Stadt", innerStadt);

        Map<String, String> westRing = new HashMap<>();
        westRing.put(KEY_CO2, "440 ppm"); westRing.put(KEY_PM25, "20 µg/m³"); westRing.put(KEY_O2, "20.8 %");
        westRing.put(KEY_SO2, "5.5 ppb"); westRing.put(KEY_CH4, "1.95 ppm"); westRing.put(KEY_P, "1012.0 hPa");
        westRing.put(KEY_LP, "68 dB");    westRing.put(KEY_LX, "550 lx");     westRing.put(KEY_TD, "8.8 °C");
        westRing.put(KEY_ABS_HUMIDITY, "9.0 g/m³");
        braunschweigCityData.put("Westliches Ringgebiet", westRing);

        Map<String, String> ostRing = new HashMap<>();
        ostRing.put(KEY_CO2, "425 ppm"); ostRing.put(KEY_PM25, "16 µg/m³"); ostRing.put(KEY_O2, "20.88 %");
        ostRing.put(KEY_SO2, "4.8 ppb"); ostRing.put(KEY_CH4, "1.88 ppm"); ostRing.put(KEY_P, "1012.9 hPa");
        ostRing.put(KEY_LP, "62 dB");    ostRing.put(KEY_LX, "490 lx");      ostRing.put(KEY_TD, "8.3 °C");
        ostRing.put(KEY_ABS_HUMIDITY, "8.8 g/m³");
        braunschweigCityData.put("Östliches Ringgebiet", ostRing);

        Map<String, String> suedRing = new HashMap<>();
        suedRing.put(KEY_CO2, "435 ppm"); suedRing.put(KEY_PM25, "19 µg/m³"); suedRing.put(KEY_O2, "20.82 %");
        suedRing.put(KEY_SO2, "5.1 ppb"); suedRing.put(KEY_CH4, "1.92 ppm"); suedRing.put(KEY_P, "1012.2 hPa");
        suedRing.put(KEY_LP, "66 dB");    suedRing.put(KEY_LX, "530 lx");     suedRing.put(KEY_TD, "8.5 °C");
        suedRing.put(KEY_ABS_HUMIDITY, "8.9 g/m³");
        braunschweigCityData.put("Südliches Ringgebiet", suedRing);

        Map<String, String> weststadt = new HashMap<>();
        weststadt.put(KEY_CO2, "412 ppm"); weststadt.put(KEY_PM25, "11 µg/m³"); weststadt.put(KEY_O2, "20.91 %");
        weststadt.put(KEY_SO2, "3.3 ppb"); weststadt.put(KEY_CH4, "1.78 ppm"); weststadt.put(KEY_P, "1013.5 hPa");
        weststadt.put(KEY_LP, "57 dB");    weststadt.put(KEY_LX, "430 lx");    weststadt.put(KEY_TD, "7.8 °C");
        weststadt.put(KEY_ABS_HUMIDITY, "8.3 g/m³");
        braunschweigCityData.put("Weststadt", weststadt);

        Map<String, String> heidberg = new HashMap<>();
        heidberg.put(KEY_CO2, "402 ppm"); heidberg.put(KEY_PM25, "8 µg/m³"); heidberg.put(KEY_O2, "20.97 %");
        heidberg.put(KEY_SO2, "2.7 ppb"); heidberg.put(KEY_CH4, "1.71 ppm"); heidberg.put(KEY_P, "1014.2 hPa");
        heidberg.put(KEY_LP, "51 dB");    heidberg.put(KEY_LX, "370 lx");    heidberg.put(KEY_TD, "7.3 °C");
        heidberg.put(KEY_ABS_HUMIDITY, "7.8 g/m³");
        braunschweigCityData.put("Heidberg-Melverode", heidberg);

        Map<String, String> suedstadt = new HashMap<>();
        suedstadt.put(KEY_CO2, "418 ppm"); suedstadt.put(KEY_PM25, "13 µg/m³"); suedstadt.put(KEY_O2, "20.90 %");
        suedstadt.put(KEY_SO2, "3.9 ppb"); suedstadt.put(KEY_CH4, "1.81 ppm"); suedstadt.put(KEY_P, "1013.1 hPa");
        suedstadt.put(KEY_LP, "59 dB");    suedstadt.put(KEY_LX, "460 lx");     suedstadt.put(KEY_TD, "7.9 °C");
        suedstadt.put(KEY_ABS_HUMIDITY, "8.4 g/m³");
        braunschweigCityData.put("Südstadt", suedstadt);

        Map<String, String> bebelhof = new HashMap<>();
        bebelhof.put(KEY_CO2, "428 ppm"); bebelhof.put(KEY_PM25, "17 µg/m³"); bebelhof.put(KEY_O2, "20.86 %");
        bebelhof.put(KEY_SO2, "4.9 ppb"); bebelhof.put(KEY_CH4, "1.89 ppm"); bebelhof.put(KEY_P, "1012.6 hPa");
        bebelhof.put(KEY_LP, "64 dB");    bebelhof.put(KEY_LX, "510 lx");     bebelhof.put(KEY_TD, "8.4 °C");
        bebelhof.put(KEY_ABS_HUMIDITY, "8.85 g/m³");
        braunschweigCityData.put("Bebelhof", bebelhof);

        Map<String, String> nordRing = new HashMap<>();
        nordRing.put(KEY_CO2, "433 ppm"); nordRing.put(KEY_PM25, "19 µg/m³"); nordRing.put(KEY_O2, "20.83 %");
        nordRing.put(KEY_SO2, "5.3 ppb"); nordRing.put(KEY_CH4, "1.91 ppm"); nordRing.put(KEY_P, "1012.3 hPa");
        nordRing.put(KEY_LP, "67 dB");    nordRing.put(KEY_LX, "540 lx");     nordRing.put(KEY_TD, "8.6 °C");
        nordRing.put(KEY_ABS_HUMIDITY, "8.95 g/m³");
        braunschweigCityData.put("Nördliches Ringgebiet", nordRing);

        Map<String, String> bsNord = new HashMap<>();
        bsNord.put(KEY_CO2, "408 ppm"); bsNord.put(KEY_PM25, "9 µg/m³"); bsNord.put(KEY_O2, "20.94 %");
        bsNord.put(KEY_SO2, "3.0 ppb"); bsNord.put(KEY_CH4, "1.73 ppm"); bsNord.put(KEY_P, "1013.9 hPa");
        bsNord.put(KEY_LP, "54 dB");    bsNord.put(KEY_LX, "400 lx");    bsNord.put(KEY_TD, "7.6 °C");
        bsNord.put(KEY_ABS_HUMIDITY, "8.0 g/m³");
        braunschweigCityData.put("Braunschweig-Nord", bsNord);

        Map<String, String> hintDistrictData = new HashMap<>();
        hintDistrictData.put(KEY_CO2, "400 ppm"); hintDistrictData.put(KEY_PM25, "7 µg/m³"); hintDistrictData.put(KEY_O2, "20.98 %");
        hintDistrictData.put(KEY_SO2, "2.5 ppb"); hintDistrictData.put(KEY_CH4, "1.7 ppm"); hintDistrictData.put(KEY_P, "1014.5 hPa");
        hintDistrictData.put(KEY_LP, "50 dB");    hintDistrictData.put(KEY_LX, "300 lx");    hintDistrictData.put(KEY_TD, "7.0 °C");
        hintDistrictData.put(KEY_ABS_HUMIDITY, "7.5 g/m³");
        braunschweigCityData.put(getString(R.string.hint_select_district), hintDistrictData);

        dummyData.put("Braunschweig", braunschweigCityData);
        Log.d(TAG, "initializeDummyData: Finished with distinct district data. BS Data districts count: " + braunschweigCityData.size());
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

    private void initializeRecyclerView() {
        Log.d(TAG, "initializeRecyclerView: Starting.");
        if (recyclerViewValues == null) {
            Log.e(TAG, "initializeRecyclerView: recyclerViewValues is NULL. Cannot setup RecyclerView.");
            return;
        }
        recyclerViewValues.setLayoutManager(new LinearLayoutManager(this));
        valueAdapter = new ValueAdapter(new ArrayList<>());
        recyclerViewValues.setAdapter(valueAdapter);

        valueAdapter.setOnItemClickListener((itemView, position) -> {
            Log.d(TAG, "RecyclerView item clicked at position: " + position);
            ValueItem clickedItem = valueAdapter.getValueItem(position);
            if (clickedItem != null) {
                Log.d(TAG, "Clicked ValueItem display name: " + clickedItem.getDisplayableName() + ", data key: " + clickedItem.getDataKey());
                int[] location = new int[2];
                itemView.getLocationOnScreen(location);
                showValueExplanationPopup(clickedItem, location[0], location[1], itemView.getWidth(), itemView.getHeight());
            } else {
                Log.w(TAG, "Clicked item at position " + position + " is null.");
            }
        });
        Log.d(TAG, "initializeRecyclerView: Finished.");
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
                handleCityOrDistrictChange();
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
                handleCityOrDistrictChange();
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

    private void setupListLayoutToggleButton() {
        Log.d(TAG, "setupListLayoutToggleButton: Starting.");
        if (btnListLayoutToggle == null) {
            Log.e(TAG, "setupListLayoutToggleButton: btnListLayoutToggle is NULL.");
            return;
        }
        btnListLayoutToggle.setSelected(currentListLayoutColumns == 2); // Set initial state
        btnListLayoutToggle.setOnClickListener(v -> { Log.d(TAG, "List layout toggle button clicked."); toggleListLayout(); });
        Log.d(TAG, "setupListLayoutToggleButton: Finished.");
    }

    private void toggleListLayout() {
        Log.d(TAG, "toggleListLayout: Current columns = " + currentListLayoutColumns);
        if (recyclerViewValues == null || btnListLayoutToggle == null) {
            Log.e(TAG, "toggleListLayout: RecyclerView or ToggleButton is null. Cannot change layout.");
            return;
        }
        if (currentListLayoutColumns == 1) {
            currentListLayoutColumns = 2;
            recyclerViewValues.setLayoutManager(new GridLayoutManager(this, 2));
            btnListLayoutToggle.setSelected(true);
            Log.d(TAG, "toggleListLayout: Switched to 2 columns.");
        } else {
            currentListLayoutColumns = 1;
            recyclerViewValues.setLayoutManager(new LinearLayoutManager(this));
            btnListLayoutToggle.setSelected(false);
            Log.d(TAG, "toggleListLayout: Switched to 1 column.");
        }
    }

    private void updateMainView() {
        Log.i(TAG, "updateMainView: Updating main content area (list view).");
        String selectedCity = cities.get(spinnerCity.getSelectedItemPosition());
        String selectedDistrict = null;
        List<String> currentDistrictList = districts.get(selectedCity);

        if (currentDistrictList != null && spinnerDistrict.getSelectedItemPosition() >= 0 &&
                spinnerDistrict.getSelectedItemPosition() < currentDistrictList.size()) {
            selectedDistrict = currentDistrictList.get(spinnerDistrict.getSelectedItemPosition());
        } else if (currentDistrictList != null && !currentDistrictList.isEmpty()){
            selectedDistrict = currentDistrictList.get(0);
            Log.w(TAG, "updateMainView: District spinner position invalid, defaulting to first item: " + selectedDistrict);
        } else {
            Log.e(TAG, "updateMainView: currentDistrictList is null or empty for city: " + selectedCity);
            showPlaceholderMessageForList();
            return;
        }

        Log.d(TAG, "updateMainView: Current selectedCity='" + selectedCity + "', selectedDistrict='" + selectedDistrict + "'.");

        if (getString(R.string.hint_select_city).equals(selectedCity)) {
            Log.d(TAG, "updateMainView: No city selected (hint shown), showing placeholder for list.");
            showPlaceholderMessageForList();
        } else {
            Log.d(TAG, "updateMainView: City '" + selectedCity + "' selected, showing list for district '" + selectedDistrict + "'.");
            if (txtPlaceholderMessage != null) txtPlaceholderMessage.setVisibility(View.GONE);
            if (listViewContainer != null) {
                listViewContainer.setVisibility(View.VISIBLE);
                if (recyclerViewValues != null) {
                    if (currentListLayoutColumns == 1) {
                        recyclerViewValues.setLayoutManager(new LinearLayoutManager(this));
                    } else {
                        recyclerViewValues.setLayoutManager(new GridLayoutManager(this, 2));
                    }
                }
            }
            updateListViewWithCalculatedData(selectedCity, selectedDistrict);
        }
    }

    private void showPlaceholderMessageForList() {
        Log.i(TAG, "showPlaceholderMessageForList: Displaying placeholder for list.");
        if (txtPlaceholderMessage != null) {
            txtPlaceholderMessage.setText(R.string.placeholder_select_city_for_list);
            txtPlaceholderMessage.setVisibility(View.VISIBLE);
        }
        if (listViewContainer != null) {
            listViewContainer.setVisibility(View.GONE);
            Log.d(TAG, "showPlaceholderMessageForList: ListViewContainer visibility set to GONE.");
        }
        if (valueAdapter != null) valueAdapter.updateData(new ArrayList<>());
    }


    private void handleCityOrDistrictChange() {
        Log.i(TAG, "handleCityOrDistrictChange: City or District selection has changed.");
        updateMainView();
    }

    private void updateListViewWithCalculatedData(String city, String district) {
        Log.i(TAG, "updateListViewWithCalculatedData: Updating for city='" + city + "', district='" + district + "'.");
        List<ValueItem> dataForList = new ArrayList<>();
        if (valueAdapter == null) {
            Log.e(TAG, "updateListViewWithCalculatedData: valueAdapter is NULL.");
            return;
        }

        if (getString(R.string.hint_select_city).equals(city)) {
            Log.d(TAG, "updateListViewWithCalculatedData: Hint city selected, showing empty list.");
            valueAdapter.updateData(dataForList);
            return;
        }

        Map<String, Map<String, String>> cityData = dummyData.get(city);
        Log.d(TAG, "updateListViewWithCalculatedData: Fetched cityData for '" + city + "': " + (cityData != null ? "found (" + cityData.size() + " districts)" : "not found"));

        Map<String, String> districtSpecificReadings = null;
        if (cityData != null) {
            if (district != null && district.equals(getString(R.string.hint_select_district))) {
                districtSpecificReadings = cityData.get(getString(R.string.hint_select_district));
            } else if (district != null && cityData.containsKey(district)) {
                districtSpecificReadings = cityData.get(district);
            } else {
                districtSpecificReadings = cityData.get(getString(R.string.hint_select_district)); // Fallback
            }
        }

        if (districtSpecificReadings == null) {
            Log.w(TAG, "updateListViewWithCalculatedData: No data found for city '" + city + "', district '" + district + "'.");
            districtSpecificReadings = new HashMap<>(); // Empty map to avoid NPE
        }

        for (String dataKey : ALL_VALUE_DATA_KEYS) {
            String reading = districtSpecificReadings.getOrDefault(dataKey, "N/A");
            String displayName = getDisplayableNameForKey(dataKey);
            dataForList.add(new ValueItem(displayName, reading, dataKey));
            Log.v(TAG, "updateListViewWithCalculatedData: Added: Key=" + dataKey + ", Display=" + displayName + ", Reading=" + reading);
        }
        valueAdapter.updateData(dataForList);
        Log.d(TAG, "updateListViewWithCalculatedData: List updated with " + dataForList.size() + " items.");
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
            default: return dataKey; // Fallback
        }
    }

    private void showValueExplanationPopup(ValueItem item, int anchorX, int anchorY, int anchorWidth, int anchorHeight) {
        Log.i(TAG, "showValueExplanationPopup: For item display name '" + item.getDisplayableName() + "', data key '"+ item.getDataKey() + "'");
        String shortExplanation = getShortExplanation(item.getDataKey()); // Use dataKey
        Log.d(TAG, "showValueExplanationPopup: Short explanation for '" + item.getDataKey() + "': \"" + shortExplanation + "\"");

        SpeechBubbleDialogFragment dialogFragment = SpeechBubbleDialogFragment.newInstance(
                item.getDisplayableName(), // Pass display name for UI
                shortExplanation,
                anchorX, anchorY, anchorWidth, anchorHeight,
                item.getDataKey() // Pass dataKey for "Learn More"
        );
        dialogFragment.show(getSupportFragmentManager(), "SpeechBubbleDialog");
        Log.d(TAG, "showValueExplanationPopup: SpeechBubbleDialogFragment shown.");
    }

    private String getShortExplanation(String dataKey) {
        Log.d(TAG, "getShortExplanation: Requesting for dataKey '" + dataKey + "'.");
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

        String resourceName = resourceNameMap.get(dataKey);
        String explanation = "No short explanation for " + dataKey; // Fallback
        if (resourceName != null) {
            int resId = getResources().getIdentifier(resourceName, "string", getPackageName());
            if (resId != 0) {
                explanation = getString(resId);
            } else {
                Log.w(TAG, "getShortExplanation: Resource ID not found for string name: " + resourceName + " (for dataKey '" + dataKey + "')");
            }
        } else {
            Log.w(TAG, "getShortExplanation: No resource name mapping found for dataKey: '" + dataKey + "'.");
        }
        return explanation;
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
}