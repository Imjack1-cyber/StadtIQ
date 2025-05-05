package com.stadtiq; // <<< CHANGE THIS >>>

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ValueDetailActivity extends AppCompatActivity {

    private static final String PREF_LANG_CODE = "pref_language_code";

    @Override
    protected void attachBaseContext(Context base) {
        String langCode = getLanguageCode(base);
        Context context = contextWithLocale(base, langCode);
        super.attachBaseContext(context);
    }

    private String getLanguageCode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_LANG_CODE, "en");
    }

    private Context contextWithLocale(Context context, String langCode) {
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
        setContentView(R.layout.activity_value_detail);

        TextView detailTitle = findViewById(R.id.text_detail_title);
        TextView detailContent = findViewById(R.id.text_detail_content);

        String valueName = "Unknown Value";
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("VALUE_NAME")) {
            valueName = extras.getString("VALUE_NAME");
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.detail_title_format, valueName));
        }

        detailTitle.setText(getString(R.string.detail_title_format, valueName));

        Map<String, String> detailedInfo = new HashMap<>();

        String currentLang = getResources().getConfiguration().locale.getLanguage();

        if ("de".equals(currentLang)) {
            detailedInfo.put("CO2", "Kohlendioxid (CO2) ist ein farbloses, geruchloses Gas, das für das Leben auf der Erde unerlässlich ist, aber hohe Konzentrationen tragen zum Klimawandel bei...");
            detailedInfo.put("PM2,5", "Feinstaub 2,5 (PM2,5) bezieht sich auf winzige Partikel oder Tröpfchen in der Luft mit einer Breite von 2,5 Mikrometern oder weniger...");
            detailedInfo.put("O2", "Sauerstoff (O2) ist ein farbloses, geruchloses Gas, das für das Leben der meisten Organismen notwendig ist..."); // Example
            detailedInfo.put("SO2", "Schwefeldioxid (SO2) ist ein giftiges Gas mit stechendem Geruch, das hauptsächlich durch die Verbrennung fossiler Brennstoffe entsteht..."); // Example
            detailedInfo.put("CH", "Methan (CH4) ist das Hauptbestandteil von Erdgas und ein starkes Treibhausgas..."); // Example
            detailedInfo.put("p", "Luftdruck (p) ist der Druck, den die Atmosphäre an einem bestimmten Ort auf eine Oberfläche ausübt..."); // Example
            detailedInfo.put("lp", "Schallpegel (lp) ist ein logarithmische Maß für die Schallintensität..."); // Example
            detailedInfo.put("lx", "Beleuchtungsstärke (lx) ist die Lichtmenge, die auf eine Oberfläche fällt..."); // Example
            detailedInfo.put("Td", "Der Taupunkt (Td) ist die Temperatur, auf die Luft abgekühlt werden muss, damit Wasserdampf kondensiert..."); // Example
            detailedInfo.put(getString(R.string.value_absolute_humidity), "Absolute Luftfeuchtigkeit ist die Masse des Wasserdampfs pro Volumeneinheit Luft...");
        } else { // Default to English
            detailedInfo.put("CO2", "Carbon Dioxide (CO2) is a colorless, odorless gas essential to life on Earth, but high concentrations contribute to climate change...");
            detailedInfo.put("PM2,5", "Particulate Matter 2.5 (PM2.5) refers to tiny particles or droplets in the air that are 2.5 micrometers or less in width...");
            detailedInfo.put("O2", "Oxygen (O2) is a colorless, odorless gas necessary for the respiration of most organisms..."); // Example
            detailedInfo.put("SO2", "Sulfur Dioxide (SO2) is a toxic gas with a pungent odor, primarily produced from the burning of fossil fuels..."); // Example
            detailedInfo.put("CH", "Methane (CH4) is the main component of natural gas and a potent greenhouse gas..."); // Example
            detailedInfo.put("p", "Atmospheric pressure (p) is the pressure exerted by the weight of the atmosphere on a surface at a given location..."); // Example
            detailedInfo.put("lp", "Sound pressure level (lp) is a logarithmic measure of the effective pressure of a sound relative to a reference value..."); // Example
            detailedInfo.put("lx", "Illuminance (lx) is a measure of how much luminous flux is spread over a given area..."); // Example
            detailedInfo.put("Td", "The dew point (Td) is the temperature to which air must be cooled to become saturated with water vapor..."); // Example
            detailedInfo.put(getString(R.string.value_absolute_humidity), "Absolute humidity is the mass of water vapor per unit volume of air...");
        }


        String content = detailedInfo.get(valueName);
        if (content == null) {
            content = getString(R.string.detail_content_not_available, valueName);
        }

        detailContent.setText(content);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}