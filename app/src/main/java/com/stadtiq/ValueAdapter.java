package com.stadtiq;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ValueAdapter extends RecyclerView.Adapter<ValueViewHolder> {

    private List<ValueItem> valueList;
    private ValueViewHolder.OnItemClickListener listener;

    public ValueAdapter(List<ValueItem> valueList) {
        this.valueList = valueList;
    }

    public void setOnItemClickListener(ValueViewHolder.OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ValueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_value, parent, false);
        return new ValueViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ValueViewHolder holder, int position) {
        ValueItem currentItem = valueList.get(position);
        // Corrected lines:
        if (currentItem != null) {
            holder.valueName.setText(currentItem.getDisplayableName()); // Use getter
            holder.valueReading.setText(currentItem.getReading());     // Use getter
        }
    }

    @Override
    public int getItemCount() {
        return valueList != null ? valueList.size() : 0; // Added null check for safety
    }

    public void updateData(List<ValueItem> newList) {
        this.valueList = newList;
        notifyDataSetChanged(); // Consider using DiffUtil for better performance in complex lists
    }

    public ValueItem getValueItem(int position) {
        if (valueList != null && position >= 0 && position < valueList.size()) {
            return valueList.get(position);
        }
        return null;
    }
}