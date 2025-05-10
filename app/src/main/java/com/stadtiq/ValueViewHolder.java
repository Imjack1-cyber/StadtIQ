package com.stadtiq;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ValueViewHolder extends RecyclerView.ViewHolder {

    TextView valueName;
    TextView valueReading;

    public interface OnItemClickListener {
        void onItemClick(View itemView, int position); // Modified to pass itemView
    }

    public ValueViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
        super(itemView);
        valueName = itemView.findViewById(R.id.text_value_name);
        valueReading = itemView.findViewById(R.id.text_value_reading);

        itemView.setOnClickListener(v -> {
            if (listener != null) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(itemView, position); // Pass itemView
                }
            }
        });
    }
}