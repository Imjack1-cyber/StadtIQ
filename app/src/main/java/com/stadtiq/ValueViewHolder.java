package com.stadtiq; // <<< CHANGE THIS >>>

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ValueViewHolder extends RecyclerView.ViewHolder {

    TextView valueName;
    TextView valueReading;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public ValueViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
        super(itemView);
        valueName = itemView.findViewById(R.id.text_value_name);
        valueReading = itemView.findViewById(R.id.text_value_reading);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            }
        });
    }
}