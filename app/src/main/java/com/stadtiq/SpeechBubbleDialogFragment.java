package com.stadtiq; // <<< CHANGE THIS >>>

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window; // Import Window
import android.view.WindowManager; // Import WindowManager
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment; // Use androidx DialogFragment

public class SpeechBubbleDialogFragment extends DialogFragment {

    private static final String ARG_VALUE_NAME = "value_name";
    private static final String ARG_EXPLANATION = "explanation_text";
    // Optional: Pass the clicked view's location/dimensions for positioning
    private static final String ARG_ANCHOR_X = "anchor_x";
    private static final String ARG_ANCHOR_Y = "anchor_y";
    private static final String ARG_ANCHOR_WIDTH = "anchor_width";
    private static final String ARG_ANCHOR_HEIGHT = "anchor_height";


    // Factory method to create a new instance of the DialogFragment
    public static SpeechBubbleDialogFragment newInstance(String valueName, String explanation,
                                                         int anchorX, int anchorY, int anchorWidth, int anchorHeight) {
        SpeechBubbleDialogFragment fragment = new SpeechBubbleDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_VALUE_NAME, valueName);
        args.putString(ARG_EXPLANATION, explanation);
        args.putInt(ARG_ANCHOR_X, anchorX);
        args.putInt(ARG_ANCHOR_Y, anchorY);
        args.putInt(ARG_ANCHOR_WIDTH, anchorWidth);
        args.putInt(ARG_ANCHOR_HEIGHT, anchorHeight);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Remove the default dialog title bar
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            // Make dialog background transparent so the activity shows through
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Use a transparent system drawable
        }

        // Inflate the custom layout for the dialog content
        return inflater.inflate(R.layout.dialog_speech_bubble, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get data from arguments
        String valueName = getArguments().getString(ARG_VALUE_NAME);
        String explanationText = getArguments().getString(ARG_EXPLANATION);
        int anchorX = getArguments().getInt(ARG_ANCHOR_X);
        int anchorY = getArguments().getInt(ARG_ANCHOR_Y);
        int anchorWidth = getArguments().getInt(ARG_ANCHOR_WIDTH);
        int anchorHeight = getArguments().getInt(ARG_ANCHOR_HEIGHT);


        // Find views in the custom layout
        TextView explanationTextView = view.findViewById(R.id.text_explanation);
        TextView learnMoreTextView = view.findViewById(R.id.text_learn_more);
        // You might also want to add a TextView for the title within the bubble layout
        // TextView titleTextView = view.findViewById(R.id.text_title_in_bubble);


        // Set the text
        explanationTextView.setText(explanationText);
        learnMoreTextView.setText(R.string.learn_more); // Use string resource for "Learn More"

        // Set click listener for "Learn More"
        learnMoreTextView.setOnClickListener(v -> {
            // Navigate to the detail page
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), ValueDetailActivity.class);
                intent.putExtra("VALUE_NAME", valueName); // Pass value name
                startActivity(intent);
                // Dismiss the dialog when navigating away
                dismiss();
            }
        });

        // *** Positioning Logic (Attempt) ***
        // This is a basic attempt and might need refinement.
        // It tries to position the dialog's window.
        if (getDialog() != null && getDialog().getWindow() != null) {
            Window window = getDialog().getWindow();
            WindowManager.LayoutParams params = window.getAttributes();

            // Set gravity to top-left to use absolute coordinates
            params.gravity = android.view.Gravity.TOP | android.view.Gravity.LEFT;

            // Calculate desired position relative to the anchor view
            // This is complex in a scrolling list and might need offsets and consideration of popup size
            // Example: Position the top-left of the dialog slightly above the anchor's top-left
            params.x = anchorX; // Align left edge with anchor's left edge
            params.y = anchorY - params.height; // Position dialog immediately above anchor (needs accurate height)

            // Adjust x or y based on where the pointer is drawn on the speech bubble drawable
            // For a pointer on the bottom-center, you might align x to anchor_x + anchor_width / 2
            // For simplicity, let's try to center the dialog horizontally above the anchor
            params.x = anchorX + (anchorWidth / 2) - (view.getMeasuredWidth() / 2);
            params.y = anchorY - view.getMeasuredHeight(); // Position above the anchor (needs accurate height)


            // Set the attributes
            window.setAttributes(params);

            // Need to force layout to measure the view so getMeasuredHeight() works.
            // This often requires posting a runnable or waiting for a layout pass.
            // A simpler approach initially is to set a reasonable size and refine.

            // Consider setting a fixed size or max size if dynamic size causes issues
            // params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            // params.height = WindowManager.LayoutParams.WRAP_CONTENT;

            // Re-apply attributes after the view is measured (more reliable)
            view.post(() -> {
                if (getDialog() != null && getDialog().getWindow() != null) {
                    Window windowPost = getDialog().getWindow();
                    WindowManager.LayoutParams paramsPost = windowPost.getAttributes();

                    // Recalculate y based on actual measured height
                    paramsPost.y = anchorY - view.getMeasuredHeight() - 10; // Add a small offset (10dp) above the item

                    // Recalculate x to try and center above the anchor
                    paramsPost.x = anchorX + (anchorWidth / 2) - (view.getMeasuredWidth() / 2);

                    // Add boundary checks to prevent the popup from going off-screen

                    windowPost.setAttributes(paramsPost);
                }
            });

        }
    }
}