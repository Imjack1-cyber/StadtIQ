package com.stadtiq;

public class ValueItem {
    String displayableName; // Localized name for UI
    String reading;
    String dataKey;         // Non-localized, constant key for data and logic

    public ValueItem(String displayableName, String reading, String dataKey) {
        this.displayableName = displayableName;
        this.reading = reading;
        this.dataKey = dataKey;
    }

    public String getDisplayableName() { return displayableName; }
    public String getReading() { return reading; }
    public String getDataKey() { return dataKey; }
}