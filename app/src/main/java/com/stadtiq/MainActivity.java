package com.stadtiq; // <<< CHANGE THIS >>>

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


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
    private Button btnToggleMap;
    private Button btnToggleList;
    private FrameLayout displayAreaContainer;
    private TextView txtPlaceholderMessage;
    private View mapPlaceholderView;
    private LinearLayout listViewContainer;
    private RecyclerView recyclerViewValues;
    private ImageButton btnListLayoutToggle;

    private TextView labelSelectValue;
    private HorizontalScrollView valueOptionsScrollView;
    private LinearLayout valueOptionsContainer;


    private List<String> cities;
    private Map<String, List<String>> districts;

    private ValueAdapter valueAdapter;

    private TextView selectedValueOption = null;

    private static final String PREF_LANG_CODE = "pref_language_code";

    private int currentListLayoutColumns = 1; // Default to 1 column

    @Override
    protected void attachBaseContext(Context base) {
        Log.d(TAG, "attachBaseContext");
        String langCode = getLanguageCode(base);
        Context context = contextWithLocale(base, langCode);
        super.attachBaseContext(context);
    }

    private String getLanguageCode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lang = prefs.getString(PREF_LANG_CODE, "en");
        Log.d(TAG, "getLanguageCode: " + lang);
        return lang;
    }

    private Context contextWithLocale(Context context, String langCode) {
        Log.d(TAG, "contextWithLocale: " + langCode);
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
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // --- Nav Drawer Highlighting ---
        navigationView.setCheckedItem(R.id.nav_home);


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

        spinnerCity = findViewById(R.id.spinner_city);
        spinnerDistrict = findViewById(R.id.spinner_district);
        btnToggleMap = findViewById(R.id.btn_toggle_map);
        btnToggleList = findViewById(R.id.btn_toggle_list);
        displayAreaContainer = findViewById(R.id.display_area_container);
        txtPlaceholderMessage = findViewById(R.id.txt_placeholder_message);
        mapPlaceholderView = findViewById(R.id.map_placeholder_view);
        listViewContainer = findViewById(R.id.list_view_container);
        recyclerViewValues = listViewContainer.findViewById(R.id.recycler_view_values);
        btnListLayoutToggle = listViewContainer.findViewById(R.id.btn_list_layout_toggle);

        labelSelectValue = findViewById(R.id.label_select_value_main);
        valueOptionsScrollView = findViewById(R.id.value_options_scroll_view_main);
        valueOptionsContainer = findViewById(R.id.value_options_container);


        initializeCityDistrictData();
        initializeRecyclerView();

        setupCitySpinner();
        setupDistrictSpinner();

        setupToggleButtons();
        setupListLayoutToggleButton();

        setupValueOptionClicks();

        showPlaceholderMessage();

        String initialCity = null;
        int cityPos = spinnerCity.getSelectedItemPosition();
        if (cityPos >= 0 && cityPos < cities.size()) {
            initialCity = cities.get(cityPos);
        }
        Log.d(TAG, "onCreate: Initial city position: " + cityPos + ", city: " + initialCity);
        handleCitySelected(initialCity);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }


    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
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
        Log.d(TAG, "onNavigationItemSelected: id=" + getResources().getResourceEntryName(id));

        Intent intent = null;

        // --- UPDATED: Check the Activity you are currently in to prevent restarting ---
        if (id == R.id.nav_home) {
            // Already on Home, do nothing or just close drawer
            Log.d(TAG, "onNavigationItemSelected: Navigating to Home (already here)");
            navigationView.setCheckedItem(R.id.nav_home); // Keep checked
        } else if (id == R.id.nav_verlauf) {
            Log.d(TAG, "onNavigationItemSelected: Navigating to VerlaufActivity");
            // Check if already on VerlaufActivity to avoid restarting it
            if (!(this instanceof VerlaufActivity)) {
                intent = new Intent(this, VerlaufActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            } else {
                Log.d(TAG, "onNavigationItemSelected: Already on VerlaufActivity");
                navigationView.setCheckedItem(R.id.nav_verlauf); // Keep checked
            }
        } else if (id == R.id.nav_impressum) {
            Log.d(TAG, "onNavigationItemSelected: Navigating to ImpressumActivity");
            // Check if already on ImpressumActivity to avoid restarting it
            if (!(this instanceof ImpressumActivity)) {
                intent = new Intent(this, ImpressumActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            } else {
                Log.d(TAG, "onNavigationItemSelected: Already on ImpressumActivity");
                navigationView.setCheckedItem(R.id.nav_impressum); // Keep checked
            }
        } else if (id == R.id.nav_language) {
            Log.d(TAG, "onNavigationItemSelected: Showing language dialog");
            showLanguageSelectionDialog();
            // Do NOT check the language item visually
        }

        if (intent != null) {
            startActivity(intent);
            Log.d(TAG, "onNavigationItemSelected: Started new activity");
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        Log.d(TAG, "onNavigationItemSelected: Closing drawer");
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

            recreate(); // Recreate the activity for changes to take effect
        } else {
            Log.d(TAG, "setLocale: Language is the same (" + currentLangCode + "), no action needed");
        }
        Log.d(TAG, "setLocale: Completed");
    }

    private void initializeCityDistrictData() {
        Log.d(TAG, "initializeCityDistrictData");
        cities = new ArrayList<>();
        cities.add(getString(R.string.hint_select_city));
        cities.add("Braunschweig");

        districts = new HashMap<>();
        districts.put(getString(R.string.hint_select_city), new ArrayList<>());

        districts.put("Braunschweig", Arrays.asList(
                getString(R.string.hint_select_district),
                "Innere Stadt", "Westliches Ringgebiet", "Östliches Ringgebiet", "Südliches Ringgebiet",
                "Weststadt", "Heidberg-Melverode", "Südstadt", "Bebelhof",
                "Nördliches Ringgebiet", "Braunschweig-Nord"
        ));
        Log.d(TAG, "initializeCityDistrictData: Cities and districts data initialized");
    }

    private void initializeRecyclerView() {
        Log.d(TAG, "initializeRecyclerView");
        recyclerViewValues.setLayoutManager(new LinearLayoutManager(this));
        valueAdapter = new ValueAdapter(new ArrayList<>());
        recyclerViewValues.setAdapter(valueAdapter);

        valueAdapter.setOnItemClickListener(new ValueViewHolder.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Log.d(TAG, "RecyclerView item clicked: position=" + position);
                ValueItem clickedItem = valueAdapter.getValueItem(position);
                if (clickedItem != null) {
                    Log.d(TAG, "RecyclerView item clicked: ValueItem name=" + clickedItem.name);
                    showValueExplanationPopup(clickedItem);
                } else {
                    Log.w(TAG, "RecyclerView item clicked but ValueItem is null for position: " + position);
                }
            }
        });
        Log.d(TAG, "initializeRecyclerView: RecyclerView initialized");
    }

    private void setupCitySpinner() {
        Log.d(TAG, "setupCitySpinner");
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, cities);
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(cityAdapter);
        Log.d(TAG, "setupCitySpinner: Adapter set");


        spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCity = null;
                if (position >= 0 && position < cities.size()) {
                    selectedCity = (String) parent.getItemAtPosition(position);
                }
                Log.d(TAG, "City Spinner item selected: position=" + position + ", city=" + selectedCity);
                updateDistrictSpinner(selectedCity);
                handleCitySelected(selectedCity);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "City Spinner: nothing selected");
            }
        });
        Log.d(TAG, "setupCitySpinner: Listener set");
    }

    private void setupDistrictSpinner() {
        Log.d(TAG, "setupDistrictSpinner");
        String initialCity = null;
        int cityPos = spinnerCity.getSelectedItemPosition();
        if (cityPos >= 0 && cityPos < cities.size()) {
            initialCity = cities.get(cityPos);
        }
        Log.d(TAG, "setupDistrictSpinner: Initial city for district spinner=" + initialCity);
        updateDistrictSpinner(initialCity);


        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCity = null;
                int cityPos = spinnerCity.getSelectedItemPosition();
                if (cityPos >= 0 && cityPos < cities.size()) {
                    selectedCity = cities.get(cityPos);
                }

                String selectedDistrict = null;
                if (selectedCity != null && districts.get(selectedCity) != null && position >= 0 && position < districts.get(selectedCity).size()) {
                    selectedDistrict = districts.get(selectedCity).get(position);
                }

                Log.d(TAG, "District Spinner item selected: position=" + position + ", district=" + selectedDistrict);
                if (btnToggleList.isSelected()) {
                    Log.d(TAG, "District selected and List view is active, updating list data.");
                    updateListViewWithCalculatedData(selectedCity, selectedDistrict);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "District Spinner: nothing selected");
            }
        });
        Log.d(TAG, "setupDistrictSpinner: Listener set");
    }

    private void updateDistrictSpinner(String selectedCity) {
        Log.d(TAG, "updateDistrictSpinner: selectedCity=" + selectedCity);
        List<String> districtList = districts.get(selectedCity);
        if (districtList == null) {
            Log.w(TAG, "updateDistrictSpinner: No districts found for city: " + selectedCity + ", creating empty list.");
            districtList = new ArrayList<>();
        }
        if (cities.contains(getString(R.string.hint_select_city)) && !districtList.contains(getString(R.string.hint_select_district))) {
            districtList.add(0, getString(R.string.hint_select_district));
        }

        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, districtList);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistrict.setAdapter(districtAdapter);
        Log.d(TAG, "updateDistrictSpinner: Adapter set for district spinner");
    }

    private void setupToggleButtons() {
        Log.d(TAG, "setupToggleButtons");
        btnToggleMap.setText(R.string.button_map);
        btnToggleList.setText(R.string.button_list);

        btnToggleMap.setSelected(true);
        btnToggleList.setSelected(false);
        Log.d(TAG, "setupToggleButtons: Initial state set (Map selected)");

        btnToggleMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Map toggle button clicked");
                toggleView("Map");
            }
        });

        btnToggleList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "List toggle button clicked");
                toggleView("List");
            }
        });
        Log.d(TAG, "setupToggleButtons: Listeners setup complete");
    }

    private void setupListLayoutToggleButton() {
        Log.d(TAG, "setupListLayoutToggleButton");
        btnListLayoutToggle.setSelected(false);
        Log.d(TAG, "setupListLayoutToggleButton: Initial state set (showing 2-column icon)");


        btnListLayoutToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "List layout toggle button clicked");
                toggleListLayout();
            }
        });
        Log.d(TAG, "setupListLayoutToggleButton: Listener setup complete");
    }

    private void toggleListLayout() {
        Log.d(TAG, "toggleListLayout: Current columns=" + currentListLayoutColumns);
        if (currentListLayoutColumns == 1) {
            currentListLayoutColumns = 2;
            recyclerViewValues.setLayoutManager(new GridLayoutManager(this, 2));
            btnListLayoutToggle.setSelected(true);
            Log.d(TAG, "toggleListLayout: Switched to 2 columns");
            // Toast.makeText(this, "Switched to 2 columns", Toast.LENGTH_SHORT).show(); // REMOVED TOAST
        } else {
            currentListLayoutColumns = 1;
            recyclerViewValues.setLayoutManager(new LinearLayoutManager(this));
            btnListLayoutToggle.setSelected(false);
            Log.d(TAG, "toggleListLayout: Switched to 1 column");
            // Toast.makeText(this, "Switched to 1 column", Toast.LENGTH_SHORT).show(); // REMOVED TOAST
        }
        Log.d(TAG, "toggleListLayout: Completed");
    }


    private void toggleView(String viewType) {
        Log.d(TAG, "toggleView: viewType=" + viewType);
        if ("Map".equals(viewType)) {
            btnToggleMap.setSelected(true);
            btnToggleList.setSelected(false);

            listViewContainer.setVisibility(View.GONE);
            recyclerViewValues.setVisibility(View.GONE);
            labelSelectValue.setVisibility(View.VISIBLE);
            valueOptionsScrollView.setVisibility(View.VISIBLE);
            Log.d(TAG, "toggleView: Switched to Map view state");


            String selectedCity = null;
            int selectedCityPosition = spinnerCity.getSelectedItemPosition();
            if (selectedCityPosition >= 0 && selectedCityPosition < cities.size()) {
                selectedCity = cities.get(selectedCityPosition);
            }


            if (selectedCity == null || getString(R.string.hint_select_city).equals(selectedCity)) {
                Log.d(TAG, "toggleView: No city selected for Map view, showing placeholder message");
                showPlaceholderMessage();
            } else {
                Log.d(TAG, "toggleView: City selected for Map view, showing map placeholder");
                showMapPlaceholder();
            }

        } else if ("List".equals(viewType)) {
            btnToggleList.setSelected(true);
            btnToggleMap.setSelected(false);

            txtPlaceholderMessage.setVisibility(View.GONE);
            mapPlaceholderView.setVisibility(View.GONE);
            listViewContainer.setVisibility(View.VISIBLE);
            recyclerViewValues.setVisibility(View.VISIBLE);
            labelSelectValue.setVisibility(View.GONE);
            valueOptionsScrollView.setVisibility(View.GONE);
            Log.d(TAG, "toggleView: Switched to List view state");


            String selectedCity = null;
            int cityPos = spinnerCity.getSelectedItemPosition();
            if (cityPos >= 0 && cityPos < cities.size()) {
                selectedCity = cities.get(cityPos);
            }

            if (selectedCity != null && !getString(R.string.hint_select_city).equals(selectedCity)) {
                String selectedDistrict = null;
                int districtPos = spinnerDistrict.getSelectedItemPosition();
                if (districts.get(selectedCity) != null && districtPos >= 0 && districtPos < districts.get(selectedCity).size()) {
                    selectedDistrict = districts.get(selectedCity).get(districtPos);
                }
                Log.d(TAG, "toggleView: City selected for List view, updating list data for city=" + selectedCity + ", district=" + selectedDistrict);
                updateListViewWithCalculatedData(selectedCity, selectedDistrict);
            } else {
                Log.d(TAG, "toggleView: No city selected for List view, showing empty list");
                valueAdapter.updateData(new ArrayList<>());
            }

            if (currentListLayoutColumns == 1) {
                recyclerViewValues.setLayoutManager(new LinearLayoutManager(this));
                btnListLayoutToggle.setSelected(false);
            } else {
                recyclerViewValues.setLayoutManager(new GridLayoutManager(this, 2));
                btnListLayoutToggle.setSelected(true);
            }
        }
        Log.d(TAG, "toggleView: View state updated");
    }

    private void showPlaceholderMessage() {
        Log.d(TAG, "showPlaceholderMessage");
        txtPlaceholderMessage.setText(R.string.placeholder_select_city);
        txtPlaceholderMessage.setVisibility(View.VISIBLE);
        mapPlaceholderView.setVisibility(View.GONE);
        listViewContainer.setVisibility(View.GONE);
        recyclerViewValues.setVisibility(View.GONE);
        labelSelectValue.setVisibility(View.VISIBLE);
        valueOptionsScrollView.setVisibility(View.VISIBLE);
        Log.d(TAG, "showPlaceholderMessage: Placeholder message shown");
    }

    private void showMapPlaceholder() {
        Log.d(TAG, "showMapPlaceholder");
        txtPlaceholderMessage.setVisibility(View.GONE);
        mapPlaceholderView.setVisibility(View.VISIBLE);
        listViewContainer.setVisibility(View.GONE);
        recyclerViewValues.setVisibility(View.GONE);
        labelSelectValue.setVisibility(View.VISIBLE);
        valueOptionsScrollView.setVisibility(View.VISIBLE);
        Log.d(TAG, "showMapPlaceholder: Map placeholder shown");
    }

    private void handleCitySelected(String selectedCity) {
        Log.d(TAG, "handleCitySelected: selectedCity=" + selectedCity);
        if (getString(R.string.hint_select_city).equals(selectedCity)) {
            Log.d(TAG, "handleCitySelected: Hint city selected, showing placeholder message and clearing selections");
            showPlaceholderMessage();
            clearSelectedValueOption();
            valueAdapter.updateData(new ArrayList<>());
        } else {
            Log.d(TAG, "handleCitySelected: Specific city selected");
            if (btnToggleMap.isSelected()) {
                Log.d(TAG, "handleCitySelected: Map view active, showing map placeholder");
                showMapPlaceholder();
            } else if (btnToggleList.isSelected()) {
                Log.d(TAG, "handleCitySelected: List view active, updating list data");
                String selectedDistrict = null;
                int districtPos = spinnerDistrict.getSelectedItemPosition();
                if (districts.get(selectedCity) != null && districtPos >= 0 && districtPos < districts.get(selectedCity).size()) {
                    selectedDistrict = districts.get(selectedCity).get(districtPos);
                }
                updateListViewWithCalculatedData(selectedCity, selectedDistrict);
            }
        }
        Log.d(TAG, "handleCitySelected: Handled city selection");
    }

    private void setupValueOptionClicks() {
        Log.d(TAG, "setupValueOptionClicks");
        for (int i = 0; i < valueOptionsContainer.getChildCount(); i++) {
            View child = valueOptionsContainer.getChildAt(i);
            if (child instanceof TextView) {
                TextView valueOption = (TextView) child;
                valueOption.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "Value option TextView clicked: text=" + ((TextView) v).getText());
                        selectValueOption((TextView) v);
                    }
                });
            }
        }
        Log.d(TAG, "setupValueOptionClicks: Listeners setup complete");
    }

    private void selectValueOption(TextView selectedTextView) {
        Log.d(TAG, "selectValueOption: text=" + selectedTextView.getText());
        String selectedCity = null;
        int selectedCityPosition = spinnerCity.getSelectedItemPosition();
        if (selectedCityPosition >= 0 && selectedCityPosition < cities.size()) {
            selectedCity = cities.get(selectedCityPosition);
        }

        if (selectedCity == null || getString(R.string.hint_select_city).equals(selectedCity)) {
            Log.d(TAG, "selectValueOption: No city selected, showing toast and returning");
            Toast.makeText(this, getString(R.string.placeholder_select_city), Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedValueOption != null) {
            Log.d(TAG, "selectValueOption: Deselecting previous option: " + selectedValueOption.getText());
            selectedValueOption.setSelected(false);
        }

        Log.d(TAG, "selectValueOption: Selecting new option: " + selectedTextView.getText());
        selectedTextView.setSelected(true);
        selectedValueOption = selectedTextView;

        String selectedValueName = selectedTextView.getText().toString();

        if (btnToggleMap.isSelected()) {
            Log.d(TAG, "selectValueOption: Map view is active, simulating map data update");
            int cityPos = spinnerCity.getSelectedItemPosition();
            int districtPos = spinnerDistrict.getSelectedItemPosition();
            String selectedCityText = (cityPos >= 0 && cityPos < cities.size()) ? cities.get(cityPos) : "Unknown City";
            String selectedDistrictText = (cityPos >= 0 && cities.get(cityPos) != null && districts.get(cities.get(cityPos)) != null && districtPos >= 0 && districtPos < districts.get(cities.get(cityPos)).size()) ? districts.get(cities.get(cities.get(cityPos))).get(districtPos) : "Unknown District";

            // Toast.makeText(this, "Simulating map data overlay for: " + selectedValueName + " in " + selectedCityText + "/" + selectedDistrictText, Toast.LENGTH_SHORT).show(); // REMOVED TOAST

        }
        Log.d(TAG, "selectValueOption: Selection handled");
    }

    private void clearSelectedValueOption() {
        Log.d(TAG, "clearSelectedValueOption");
        if (selectedValueOption != null) {
            Log.d(TAG, "clearSelectedValueOption: Deselecting current option: " + selectedValueOption.getText());
            selectedValueOption.setSelected(false);
            selectedValueOption = null;
        }
        Log.d(TAG, "clearSelectedValueOption: Deselecting all value options TextViews");
        for (int i = 0; i < valueOptionsContainer.getChildCount(); i++) {
            View child = valueOptionsContainer.getChildAt(i);
            if (child instanceof TextView) {
                child.setSelected(false);
            }
        }
        Log.d(TAG, "clearSelectedValueOption: Selection cleared");
    }

    private void updateListViewWithCalculatedData(String city, String district) {
        Log.d(TAG, "updateListViewWithCalculatedData: city=" + city + ", district=" + district);
        List<ValueItem> dataForList = new ArrayList<>();

        List<String> allValueTypes = Arrays.asList(
                "CO₂", "PM2,5", "O₂", "SO₂", "CH₄", "p", "lp", "lx", "Td", getString(R.string.value_absolute_humidity)
        );

        Map<String, Map<String, Map<String, String>>> dummyData = new HashMap<>();

        Map<String, Map<String, String>> bsData = new HashMap<>();
        Map<String, String> innerStadtData = new HashMap<>();
        innerStadtData.put("CO₂", "435 ppm");
        innerStadtData.put("PM2,5", "18 µg/m³");
        innerStadtData.put("O₂", "20.9 %");
        innerStadtData.put("SO₂", "5.2 ppb");
        innerStadtData.put("CH₄", "1.9 ppm");
        innerStadtData.put("p", "1012.5 hPa");
        innerStadtData.put("lp", "65 dB");
        innerStadtData.put("lx", "500 lx");
        innerStadtData.put("Td", "8.1 °C");
        innerStadtData.put(getString(R.string.value_absolute_humidity), "8.8 g/m³");
        bsData.put("Innere Stadt", innerStadtData);

        Map<String, String> weststadtData = new HashMap<>();
        weststadtData.put("CO₂", "428 ppm");
        weststadtData.put("PM2,5", "12 µg/m³");
        weststadtData.put("O₂", "20.9 %");
        weststadtData.put("SO₂", "4.5 ppb");
        weststadtData.put("CH₄", "1.8 ppm");
        weststadtData.put("p", "1012.8 hPa");
        weststadtData.put("lp", "60 dB");
        weststadtData.put("lx", "480 lx");
        weststadtData.put("Td", "8.5 °C");
        weststadtData.put(getString(R.string.value_absolute_humidity), "9.0 g/m³");
        bsData.put("Weststadt", weststadtData);

        Map<String, String> bsDefaultData = new HashMap<>();
        bsDefaultData.put("CO₂", "420 ppm");
        bsDefaultData.put("PM2,5", "10 µg/m³");
        bsDefaultData.put("O₂", "20.9 %");
        bsDefaultData.put("SO₂", "4.0 ppb");
        bsDefaultData.put("CH₄", "1.7 ppm");
        bsDefaultData.put("p", "1013.0 hPa");
        bsDefaultData.put("lp", "55 dB");
        bsDefaultData.put("lx", "400 lx");
        bsDefaultData.put("Td", "8.0 °C");
        bsDefaultData.put(getString(R.string.value_absolute_humidity), "8.5 g/m³");
        bsData.put(getString(R.string.hint_select_district), bsDefaultData);


        dummyData.put("Braunschweig", bsData);


        Map<String, Map<String, String>> cityData = dummyData.get(city);
        Log.d(TAG, "updateListViewWithCalculatedData: Got cityData=" + cityData);

        for (String valueType : allValueTypes) {
            String overallReading = "N/A";

            if (cityData != null) {
                Map<String, String> districtData = cityData.get(district);
                if (districtData == null) {
                    Log.w(TAG, "updateListViewWithCalculatedData: No district data found for district=" + district + ", falling back to default.");
                    districtData = cityData.get(getString(R.string.hint_select_district));
                }

                if (districtData != null) {
                    overallReading = districtData.getOrDefault(valueType, "No data for " + valueType);
                } else {
                    overallReading = "No data for " + city + "/" + district;
                }
            } else {
                overallReading = "No data for " + city;
            }

            dataForList.add(new ValueItem(valueType, overallReading));
            Log.v(TAG, "updateListViewWithCalculatedData: Added item=" + valueType + ", reading=" + overallReading);
        }

        valueAdapter.updateData(dataForList);
        Log.d(TAG, "updateListViewWithCalculatedData: List data updated. Item count=" + dataForList.size());
    }

    private void showValueExplanationPopup(ValueItem item) {
        Log.d(TAG, "showValueExplanationPopup: item name=" + item.name);

        // Inflate the custom layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_speech_bubble, null);
        Log.d(TAG, "showValueExplanationPopup: Inflated dialog_speech_bubble.xml");

        // Find views in the custom layout
        TextView explanationTextView = dialogView.findViewById(R.id.text_explanation);
        TextView learnMoreTextView = dialogView.findViewById(R.id.text_learn_more);
        Log.d(TAG, "showValueExplanationPopup: Found text views");

        // --- Get the SHORT explanation text from resources ---
        String explanationText = getShortExplanation(item.name);
        Log.d(TAG, "showValueExplanationPopup: Got short explanation: " + explanationText);

        // Set the text for the views
        explanationTextView.setText(explanationText);
        learnMoreTextView.setText(R.string.learn_more);
        Log.d(TAG, "showValueExplanationPopup: Set text for text views");

        // Set click listener for the "Learn More" link
        learnMoreTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Learn More... clicked in popup for item: " + item.name);
                // Navigate to the detail page
                navigateToValueDetailPage(item.name);
            }
        });
        Log.d(TAG, "showValueExplanationPopup: Set Learn More click listener");

        // Build the AlertDialog using the custom view
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.info_title_format, item.name));
        builder.setView(dialogView);
        Log.d(TAG, "showValueExplanationPopup: Builder created with custom view");

        // Set a positive button to dismiss the dialog (optional but standard)
        builder.setPositiveButton(R.string.dialog_close, (dialog, which) -> {
            Log.d(TAG, "Popup Close button clicked");
            dialog.dismiss();
        });
        Log.d(TAG, "showValueExplanationPopup: Added Close button");


        // Create the dialog
        AlertDialog dialog = builder.create();
        Log.d(TAG, "showValueExplanationPopup: AlertDialog created");

        // --- Make the dialog window transparent ---
        if (dialog.getWindow() != null) {
            Log.d(TAG, "showValueExplanationPopup: Setting transparent window background");
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.speach_bubble_wrap);
        } else {
            Log.w(TAG, "showValueExplanationPopup: Dialog window is null, cannot set background!");
        }

        Log.d(TAG, "showValueExplanationPopup: Showing AlertDialog");
        dialog.show();
        Log.d(TAG, "showValueExplanationPopup: AlertDialog shown");
    }

    private void navigateToValueDetailPage(String valueName) {
        Log.d(TAG, "navigateToValueDetailPage: valueName=" + valueName);
        Intent intent = new Intent(this, ValueDetailActivity.class);
        intent.putExtra("VALUE_NAME", valueName);
        startActivity(intent);
        Log.d(TAG, "navigateToValueDetailPage: Started ValueDetailActivity");
    }

    // Method to get the short explanation text from resources
    private String getShortExplanation(String valueName) {
        Log.d(TAG, "getShortExplanation: valueName=" + valueName);
        Map<String, String> resourceNameMap = new HashMap<>();
        resourceNameMap.put("CO₂", "explanation_co2_short");
        resourceNameMap.put("PM2,5", "explanation_pm25_short");
        resourceNameMap.put("O₂", "explanation_o2_short");
        resourceNameMap.put("SO₂", "explanation_so2_short");
        resourceNameMap.put("CH₄", "explanation_ch4_short");
        resourceNameMap.put("p", "explanation_p_short");
        resourceNameMap.put("lp", "explanation_lp_short");
        resourceNameMap.put("lx", "explanation_lx_short");
        resourceNameMap.put("Td", "explanation_td_short");
        resourceNameMap.put(getString(R.string.value_absolute_humidity), "explanation_absolute_humidity_short");

        String resourceName = resourceNameMap.get(valueName);
        String explanation = "No short explanation mapping for " + valueName;
        if (resourceName != null) {
            int resId = getResources().getIdentifier(resourceName, "string", getPackageName());
            if (resId != 0) {
                explanation = getString(resId);
                Log.d(TAG, "getShortExplanation: Found resource for " + valueName + ": " + resourceName);
            } else {
                Log.w(TAG, "getShortExplanation: Resource ID not found for string name: " + resourceName);
            }
        } else {
            Log.w(TAG, "getShortExplanation: No resource name mapping found for value: " + valueName);
        }
        Log.d(TAG, "getShortExplanation: Returning explanation: " + explanation);
        return explanation;
    }

    // Method to get the long explanation text (used by ValueDetailActivity)
    private String getDetailedExplanation(String valueName) {
        Log.d(TAG, "getDetailedExplanation: valueName=" + valueName);
        Map<String, String> resourceNameMap = new HashMap<>();
        resourceNameMap.put("CO₂", "explanation_co2_long");
        resourceNameMap.put("PM2,5", "explanation_pm25_long");
        resourceNameMap.put("O₂", "explanation_o2_long");
        resourceNameMap.put("SO₂", "explanation_so2_long");
        resourceNameMap.put("CH₄", "explanation_ch4_long");
        resourceNameMap.put("p", "explanation_p_long");
        resourceNameMap.put("lp", "explanation_lp_long");
        resourceNameMap.put("lx", "explanation_lx_long");
        resourceNameMap.put("Td", "explanation_td_long");
        resourceNameMap.put(getString(R.string.value_absolute_humidity), "explanation_absolute_humidity_long");

        String resourceName = resourceNameMap.get(valueName);
        String explanation = getString(R.string.detail_content_not_available, valueName);
        if (resourceName != null) {
            int resId = getResources().getIdentifier(resourceName, "string", getPackageName());
            if (resId != 0) {
                explanation = getString(resId);
                Log.d(TAG, "getDetailedExplanation: Found resource for " + valueName + ": " + resourceName);
            } else {
                Log.w(TAG, "getDetailedExplanation: Resource ID not found for string name: " + resourceName);
            }
        } else {
            Log.w(TAG, "getDetailedExplanation: No resource name mapping found for value: " + valueName);
        }
        Log.d(TAG, "getDetailedExplanation: Returning explanation: " + explanation);
        return explanation;
    }
}