package com.stadtiq; // <<< CHANGE THIS >>>

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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
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

public class VerlaufActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "VerlaufActivity";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Spinner spinnerCity;
    private Spinner spinnerDistrict;
    private LinearLayout timeOptionsContainer;
    private FrameLayout graphPlaceholderContainer;
    private TextView txtGraphPlaceholderMessage;
    private View graphWhitePlaceholderView;
    private LinearLayout valueOptionsContainer;

    private List<String> cities;
    private Map<String, List<String>> districts;
    private List<String> timePeriods;

    private TextView selectedTimeOption = null;
    private TextView selectedValueOption = null;

    private static final String PREF_LANG_CODE = "pref_language_code";

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
        setContentView(R.layout.activity_verlauf);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.title_activity_verlauf);
        }

        drawerLayout = findViewById(R.id.drawer_layout_verlauf);
        navigationView = findViewById(R.id.nav_view_verlauf);

        navigationView.setCheckedItem(R.id.nav_verlauf);


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

        spinnerCity = findViewById(R.id.spinner_city_verlauf);
        spinnerDistrict = findViewById(R.id.spinner_district_verlauf);
        timeOptionsContainer = findViewById(R.id.time_options_container);
        graphPlaceholderContainer = findViewById(R.id.graph_placeholder_container);
        txtGraphPlaceholderMessage = findViewById(R.id.txt_graph_placeholder_message);
        graphWhitePlaceholderView = findViewById(R.id.graph_white_placeholder_view);
        valueOptionsContainer = findViewById(R.id.value_options_container_verlauf);


        initializeCityDistrictData();
        initializeTimePeriods();

        setupCitySpinner();
        setupDistrictSpinner();

        setupTimeOptionClicks();
        setupValueOptionClicks();

        showPlaceholderMessage();

        if (spinnerCity.getSelectedItemPosition() >= 0) {
            Log.d(TAG, "onCreate: Spinner has item selected, calling handleSelectionChange");
            handleSelectionChange();
        } else {
            Log.d(TAG, "onCreate: Spinner has no item selected yet, relying on listener");
        }
        Log.d(TAG, "onCreate: Completed setup");
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

        if (id == R.id.nav_home) {
            Log.d(TAG, "onNavigationItemSelected: Navigating to MainActivity");
            intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        } else if (id == R.id.nav_verlauf) {
            Log.d(TAG, "onNavigationItemSelected: Navigating to VerlaufActivity (already here)");
            navigationView.setCheckedItem(R.id.nav_verlauf);
        } else if (id == R.id.nav_impressum) {
            Log.d(TAG, "onNavigationItemSelected: Navigating to ImpressumActivity");
            intent = new Intent(this, ImpressumActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        } else if (id == R.id.nav_language) {
            Log.d(TAG, "onNavigationItemSelected: Showing language dialog");
            showLanguageSelectionDialog();
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

            recreate();
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

    private void initializeTimePeriods() {
        Log.d(TAG, "initializeTimePeriods");
        timePeriods = new ArrayList<>();
        timePeriods.add("3");
        timePeriods.add("6");
        timePeriods.add("9");
        timePeriods.add("12");
        timePeriods.add("15");
        timePeriods.add("18");
        timePeriods.add("21");
        timePeriods.add("24");
        timePeriods.add(getString(R.string.time_option_all));
        Log.d(TAG, "initializeTimePeriods: Time periods data initialized");
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

    private void setupTimeOptionClicks() {
        Log.d(TAG, "setupTimeOptionClicks");
        for (int i = 0; i < timeOptionsContainer.getChildCount(); i++) {
            View child = timeOptionsContainer.getChildAt(i);
            if (child instanceof TextView) {
                TextView timeOption = (TextView) child;
                timeOption.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "Time option TextView clicked: text=" + ((TextView) v).getText());
                        selectTimeOption((TextView) v);
                    }
                });
            }
        }
        Log.d(TAG, "setupTimeOptionClicks: Listeners setup complete");
    }

    private void selectTimeOption(TextView selectedTextView) {
        Log.d(TAG, "selectTimeOption: text=" + selectedTextView.getText());
        String selectedCity = null;
        int cityPos = spinnerCity.getSelectedItemPosition();
        if (cityPos >= 0 && cityPos < cities.size()) {
            selectedCity = cities.get(cityPos);
        }

        if (selectedCity == null || getString(R.string.hint_select_city).equals(selectedCity)) {
            Log.d(TAG, "selectTimeOption: No city selected, showing toast and returning");
            Toast.makeText(this, getString(R.string.placeholder_select_city_verlauf), Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTimeOption != null) {
            Log.d(TAG, "selectTimeOption: Deselecting previous option: " + selectedTimeOption.getText());
            selectedTimeOption.setSelected(false);
        }

        Log.d(TAG, "selectTimeOption: Selecting new option: " + selectedTextView.getText());
        selectedTextView.setSelected(true);
        selectedTimeOption = selectedTextView;

        String selectedTime = selectedTextView.getText().toString();
        Toast.makeText(this, "Selected Time: " + selectedTime, Toast.LENGTH_SHORT).show();

        handleSelectionChange();
        Log.d(TAG, "selectTimeOption: Selection handled");
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
        int cityPos = spinnerCity.getSelectedItemPosition();
        if (cityPos >= 0 && cityPos < cities.size()) {
            selectedCity = cities.get(cityPos);
        }

        if (selectedCity == null || getString(R.string.hint_select_city).equals(selectedCity)) {
            Log.d(TAG, "selectValueOption: No city selected, showing toast and returning");
            Toast.makeText(this, getString(R.string.placeholder_select_city_verlauf), Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "Selected Value: " + selectedValueName, Toast.LENGTH_SHORT).show();

        handleSelectionChange();
        Log.d(TAG, "selectValueOption: Selection handled");
    }

    private void handleCitySelected(String selectedCity) {
        Log.d(TAG, "handleCitySelected: selectedCity=" + selectedCity);
        if (getString(R.string.hint_select_city).equals(selectedCity)) {
            Log.d(TAG, "handleCitySelected: Hint city selected, showing placeholder message and clearing selections");
            showPlaceholderMessage();
            clearSelectedTimeOption();
            clearSelectedValueOption();
        } else {
            Log.d(TAG, "handleCitySelected: Specific city selected");
            handleSelectionChange(); // Re-evaluate state based on new city selection
        }
        Log.d(TAG, "handleCitySelected: Handled city selection");
    }

    private void handleSelectionChange() {
        Log.d(TAG, "handleSelectionChange");
        String selectedCity = null;
        int cityPos = spinnerCity.getSelectedItemPosition();
        if (cityPos >= 0 && cityPos < cities.size()) {
            selectedCity = cities.get(cityPos);
        }
        boolean citySelected = (selectedCity != null && !getString(R.string.hint_select_city).equals(selectedCity));


        String selectedDistrict = null;
        int districtPos = spinnerDistrict.getSelectedItemPosition();
        if (citySelected && districts.get(selectedCity) != null && districtPos >= 0 && districtPos < districts.get(selectedCity).size()) {
            selectedDistrict = districts.get(selectedCity).get(districtPos);
        }
        boolean districtSelected = (selectedDistrict != null && !getString(R.string.hint_select_district).equals(selectedDistrict));


        boolean timeSelected = selectedTimeOption != null;
        String timePeriod = timeSelected ? selectedTimeOption.getText().toString() : "N/A";

        boolean valueSelected = selectedValueOption != null;
        String valueName = valueSelected ? selectedValueOption.getText().toString() : "N/A";

        Log.d(TAG, "handleSelectionChange: citySelected=" + citySelected + ", districtSelected=" + districtSelected + ", timeSelected=" + timeSelected + ", valueSelected=" + valueSelected); // Log state


        if (citySelected && timeSelected && valueSelected) {
            Log.d(TAG, "handleSelectionChange: All required selections made, showing white graph placeholder");
            showWhiteGraphPlaceholder();

            Toast.makeText(this, "Simulating graph data for: " + valueName + " over " + timePeriod + " in " + selectedCity + "/" + selectedDistrict, Toast.LENGTH_LONG).show();

        } else if (citySelected) {
            Log.d(TAG, "handleSelectionChange: City selected, but missing time or value, showing prompt");
            txtGraphPlaceholderMessage.setText(R.string.placeholder_select_time_value);
            txtGraphPlaceholderMessage.setVisibility(View.VISIBLE);
            graphWhitePlaceholderView.setVisibility(View.GONE);

        } else {
            Log.d(TAG, "handleSelectionChange: No city selected, showing initial placeholder");
            showPlaceholderMessage();
        }
        Log.d(TAG, "handleSelectionChange: Completed"); // Log completion
    }

    private void showPlaceholderMessage() {
        Log.d(TAG, "showPlaceholderMessage");
        txtGraphPlaceholderMessage.setText(R.string.placeholder_select_city_verlauf);
        txtGraphPlaceholderMessage.setVisibility(View.VISIBLE);
        graphWhitePlaceholderView.setVisibility(View.GONE);
    }

    private void showWhiteGraphPlaceholder() {
        Log.d(TAG, "showWhiteGraphPlaceholder");
        txtGraphPlaceholderMessage.setVisibility(View.GONE);
        graphWhitePlaceholderView.setVisibility(View.VISIBLE);
    }

    private void clearSelectedTimeOption() {
        Log.d(TAG, "clearSelectedTimeOption");
        if (selectedTimeOption != null) {
            Log.d(TAG, "clearSelectedTimeOption: Deselecting current option: " + selectedTimeOption.getText());
            selectedTimeOption.setSelected(false);
            selectedTimeOption = null;
        }
        for (int i = 0; i < timeOptionsContainer.getChildCount(); i++) {
            View child = timeOptionsContainer.getChildAt(i);
            if (child instanceof TextView) {
                child.setSelected(false);
            }
        }
        Log.d(TAG, "clearSelectedTimeOption: Selection cleared");
    }

    private void clearSelectedValueOption() {
        Log.d(TAG, "clearSelectedValueOption");
        if (selectedValueOption != null) {
            Log.d(TAG, "clearSelectedValueOption: Deselecting current option: " + selectedValueOption.getText());
            selectedValueOption.setSelected(false);
            selectedValueOption = null;
        }
        for (int i = 0; i < valueOptionsContainer.getChildCount(); i++) {
            View child = valueOptionsContainer.getChildAt(i);
            if (child instanceof TextView) {
                child.setSelected(false);
            }
        }
        Log.d(TAG, "clearSelectedValueOption: Selection cleared");
    }

    private void navigateToValueDetailPage(String valueName) {
        Log.d(TAG, "navigateToValueDetailPage: valueName=" + valueName);
        Intent intent = new Intent(this, ValueDetailActivity.class);
        intent.putExtra("VALUE_NAME", valueName);
        startActivity(intent);
        Log.d(TAG, "navigateToValueDetailPage: Started ValueDetailActivity");
    }
}
