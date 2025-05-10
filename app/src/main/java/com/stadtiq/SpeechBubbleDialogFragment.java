package com.stadtiq;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class SpeechBubbleDialogFragment extends DialogFragment {

    private static final String TAG = "SpeechBubbleDlg";

    // Argument keys
    private static final String ARG_VALUE_DISPLAY_NAME = "value_display_name";
    private static final String ARG_EXPLANATION = "explanation_text";
    private static final String ARG_ANCHOR_BOUNDS = "anchor_bounds";
    private static final String ARG_VALUE_DATA_KEY = "value_data_key";

    private SpeechBubbleView speechBubbleView;
    private Bundle retrievedArgs;
    private TextView explanationTextViewInternal; // For logging


    public static SpeechBubbleDialogFragment newInstance(String valueDisplayName, String explanation,
                                                         int anchorX, int anchorY, int anchorWidth, int anchorHeight,
                                                         String valueDataKey) {
        SpeechBubbleDialogFragment fragment = new SpeechBubbleDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_VALUE_DISPLAY_NAME, valueDisplayName);
        args.putString(ARG_EXPLANATION, explanation);
        Rect anchorBounds = new Rect(anchorX, anchorY, anchorX + anchorWidth, anchorY + anchorHeight);
        args.putParcelable(ARG_ANCHOR_BOUNDS, anchorBounds);
        args.putString(ARG_VALUE_DATA_KEY, valueDataKey);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        retrievedArgs = getArguments();
        if (retrievedArgs == null) {
            Log.e(TAG, "onCreate: getArguments() returned null!");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        Window window = dialog.getWindow();
        if (window != null) {
            window.requestFeature(Window.FEATURE_NO_TITLE);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_speech_bubble, container, false);
        if (rootView instanceof SpeechBubbleView) {
            this.speechBubbleView = (SpeechBubbleView) rootView;
        } else {
            Log.e(TAG, "onCreateView: Root view is NOT SpeechBubbleView. It is: " + (rootView != null ? rootView.getClass().getName() : "null"));
        }
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (this.retrievedArgs == null || this.speechBubbleView == null) {
            Log.e(TAG, "onViewCreated: Args or speechBubbleView is null. Dismissing.");
            dismissAllowingStateLoss();
            return;
        }

        final String valueDataKey = this.retrievedArgs.getString(ARG_VALUE_DATA_KEY);
        final Rect anchorBoundsFromArgs = this.retrievedArgs.getParcelable(ARG_ANCHOR_BOUNDS);
        String explanationText = this.retrievedArgs.getString(ARG_EXPLANATION);

        if (anchorBoundsFromArgs == null) { Log.e(TAG, "AnchorBounds from args is null. Dismissing."); dismissAllowingStateLoss(); return; }

        explanationTextViewInternal = this.speechBubbleView.findViewById(R.id.text_explanation);
        TextView learnMoreTextView = this.speechBubbleView.findViewById(R.id.text_learn_more);
        LinearLayout contentArea = this.speechBubbleView.findViewById(R.id.bubble_content_area);


        if (explanationTextViewInternal == null || learnMoreTextView == null || contentArea == null) {
            Log.e(TAG, "Required views (explanation, learnMore, contentArea) not found. Dismissing.");
            dismissAllowingStateLoss(); return;
        }

        explanationTextViewInternal.setText(explanationText);
        learnMoreTextView.setText(R.string.learn_more);
        learnMoreTextView.setOnClickListener(v -> {
            if (getActivity() != null && valueDataKey != null) {
                Intent intent = new Intent(getActivity(), ValueDetailActivity.class);
                intent.putExtra("VALUE_DATA_KEY", valueDataKey);
                startActivity(intent);
                dismissAllowingStateLoss();
            }
        });

        this.speechBubbleView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (speechBubbleView == null || !speechBubbleView.getViewTreeObserver().isAlive() ||
                        !isAdded() || getContext() == null || getDialog() == null || getDialog().getWindow() == null ||
                        anchorBoundsFromArgs == null) {
                    if (speechBubbleView != null && speechBubbleView.getViewTreeObserver().isAlive()) {
                        speechBubbleView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                    Log.w(TAG, "GlobalLayout: Aborting due to null/invalid state.");
                    return;
                }
                speechBubbleView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                Window window = getDialog().getWindow();
                WindowManager.LayoutParams params = window.getAttributes();

                int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
                int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
                int statusBarHeight = getStatusBarHeight();
                // Max bubble width is a percentage of screen, minus margins
                int minScreenMarginPx = dpToPxInt(10f);
                int maxBubbleWidth = screenWidth - (2 * minScreenMarginPx);


                // --- Measurement Phase ---
                // 1. Tentatively measure for above and below to decide tail direction
                speechBubbleView.setTailDirection(SpeechBubbleView.TailDirection.DOWN); // Try placing above anchor
                speechBubbleView.measure(
                        View.MeasureSpec.makeMeasureSpec(maxBubbleWidth, View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST));
                int measuredHeightDown = speechBubbleView.getMeasuredHeight();
                int yPosIfAbove = anchorBoundsFromArgs.top - measuredHeightDown - dpToPxInt(4f); // Small gap

                speechBubbleView.setTailDirection(SpeechBubbleView.TailDirection.UP);   // Try placing below anchor
                speechBubbleView.measure(
                        View.MeasureSpec.makeMeasureSpec(maxBubbleWidth, View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST));
                int measuredHeightUp = speechBubbleView.getMeasuredHeight();
                int yPosIfBelow = anchorBoundsFromArgs.bottom + dpToPxInt(4f); // Small gap

                // 2. Decide final tail direction
                boolean canFitAbove = (yPosIfAbove >= statusBarHeight + minScreenMarginPx);
                boolean canFitBelow = (yPosIfBelow + measuredHeightUp <= screenHeight - minScreenMarginPx);
                SpeechBubbleView.TailDirection finalTailDirection;

                if (canFitAbove) {
                    finalTailDirection = SpeechBubbleView.TailDirection.DOWN;
                } else if (canFitBelow) {
                    finalTailDirection = SpeechBubbleView.TailDirection.UP;
                } else { // Neither fits perfectly, choose side with more space or default to below
                    if ((anchorBoundsFromArgs.top - measuredHeightDown) > (screenHeight - (anchorBoundsFromArgs.bottom + measuredHeightUp))) {
                        finalTailDirection = SpeechBubbleView.TailDirection.DOWN;
                    } else {
                        finalTailDirection = SpeechBubbleView.TailDirection.UP;
                    }
                    Log.w(TAG, "Bubble positioning compromised. Initial choice Tail: " + finalTailDirection);
                }

                // 3. Set final tail direction and RE-MEASURE to get final W/H for that configuration
                speechBubbleView.setTailDirection(finalTailDirection);
                speechBubbleView.measure(
                        View.MeasureSpec.makeMeasureSpec(maxBubbleWidth, View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
                );
                int finalBubbleWidth = speechBubbleView.getMeasuredWidth();
                int finalBubbleHeight = speechBubbleView.getMeasuredHeight();

                // --- Logging for Clipping Debug ---
                int sbvPaddingL = speechBubbleView.getPaddingLeft();
                int sbvPaddingT = speechBubbleView.getPaddingTop();
                int sbvPaddingR = speechBubbleView.getPaddingRight();
                int sbvPaddingB = speechBubbleView.getPaddingBottom();
                int availableContentWidth = finalBubbleWidth - sbvPaddingL - sbvPaddingR;
                int availableContentHeight = finalBubbleHeight - sbvPaddingT - sbvPaddingB;
                int contentAreaMeasuredW = contentArea.getMeasuredWidth();
                int contentAreaMeasuredH = contentArea.getMeasuredHeight();
                int explanationMeasuredW = explanationTextViewInternal.getMeasuredWidth();
                int explanationMeasuredH = explanationTextViewInternal.getMeasuredHeight();
                Layout explanationLayout = explanationTextViewInternal.getLayout();
                int explanationLineCount = (explanationLayout != null) ? explanationLayout.getLineCount() : -1;


                Log.d(TAG, "--- MEASUREMENT DETAILS ---");
                Log.d(TAG, String.format("Screen W:%d H:%d; MaxBubbleW:%d", screenWidth, screenHeight, maxBubbleWidth));
                Log.d(TAG, String.format("SpeechBubbleView Measured W:%d H:%d", finalBubbleWidth, finalBubbleHeight));
                Log.d(TAG, String.format("SBV Padding L:%d T:%d R:%d B:%d", sbvPaddingL, sbvPaddingT, sbvPaddingR, sbvPaddingB));
                Log.d(TAG, String.format("Available for Content W:%d H:%d", availableContentWidth, availableContentHeight));
                Log.d(TAG, String.format("ContentArea (LinearLayout) Measured W:%d H:%d", contentAreaMeasuredW, contentAreaMeasuredH));
                Log.d(TAG, String.format("ExplanationTV Measured W:%d H:%d Lines:%d", explanationMeasuredW, explanationMeasuredH, explanationLineCount));
                Log.d(TAG, "--- END MEASUREMENT DETAILS ---");


                // --- Positioning Phase ---
                // 4. Calculate final Y position
                int finalYPos;
                if (finalTailDirection == SpeechBubbleView.TailDirection.DOWN) { // Bubble is above anchor
                    finalYPos = anchorBoundsFromArgs.top - finalBubbleHeight - dpToPxInt(4f);
                } else { // Bubble is below anchor (UP or NONE)
                    finalYPos = anchorBoundsFromArgs.bottom + dpToPxInt(4f);
                }
                // Clamp Y to screen bounds
                finalYPos = Math.max(statusBarHeight + minScreenMarginPx, finalYPos);
                if (finalYPos + finalBubbleHeight > screenHeight - minScreenMarginPx) {
                    finalYPos = screenHeight - finalBubbleHeight - minScreenMarginPx;
                    if (finalYPos < statusBarHeight + minScreenMarginPx) { // If still doesn't fit, pin to top
                        finalYPos = statusBarHeight + minScreenMarginPx;
                    }
                }

                // 5. Calculate final X position
                int finalXPos = anchorBoundsFromArgs.centerX() - finalBubbleWidth / 2;
                // Clamp X to screen bounds
                if (finalXPos < minScreenMarginPx) {
                    finalXPos = minScreenMarginPx;
                }
                if (finalXPos + finalBubbleWidth > screenWidth - minScreenMarginPx) {
                    finalXPos = screenWidth - finalBubbleWidth - minScreenMarginPx;
                    // Re-clamp if bubble wider than screen (and not full width itself)
                    if (finalXPos < minScreenMarginPx && finalBubbleWidth < screenWidth - 2 * minScreenMarginPx) finalXPos = minScreenMarginPx;
                }

                // 6. Calculate Tail Horizontal Offset
                float tailOffsetPercent = 0.5f;
                if (finalBubbleWidth > 0 && speechBubbleView.getTailWidthPx() > 0 && speechBubbleView.getCornerRadiusPx() >= 0) {
                    float targetTailTipAbsoluteX = (float)anchorBoundsFromArgs.centerX();
                    // Where the anchor's centerX is, relative to the bubble's left edge (finalXPos)
                    float targetTailTipRelativeToBubbleOrigin = targetTailTipAbsoluteX - finalXPos;

                    // The flat part of the bubble body where the tail can attach
                    // This is: bubble_content_width - (2 * corner_radius)
                    // bubble_content_width = finalBubbleWidth - paddingL - paddingR
                    float flatBodyWidth = availableContentWidth - (2 * speechBubbleView.getCornerRadiusPx());

                    if (flatBodyWidth > speechBubbleView.getTailWidthPx()) {
                        // Offset of the target tip from the start of the flat body part
                        float offsetFromFlatBodyStart = targetTailTipRelativeToBubbleOrigin - sbvPaddingL - speechBubbleView.getCornerRadiusPx();
                        tailOffsetPercent = offsetFromFlatBodyStart / flatBodyWidth;
                    }
                }
                tailOffsetPercent = Math.max(0.01f, Math.min(0.99f, tailOffsetPercent)); // Clamp
                speechBubbleView.setTailHorizontalOffsetPercent(tailOffsetPercent);

                // 7. Apply to Window
                params.gravity = Gravity.TOP | Gravity.START;
                params.x = finalXPos;
                params.y = finalYPos;

                if (finalBubbleWidth > 0 && finalBubbleHeight > 0) {
                    window.setLayout(finalBubbleWidth, finalBubbleHeight);
                } else {
                    Log.e(TAG, "Final bubble dimensions zero/invalid (" + finalBubbleWidth + "x" + finalBubbleHeight + "), using WRAP_CONTENT.");
                    window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
                }
                window.setAttributes(params);

                Log.i(TAG, String.format("FINAL Bubble Pos: X=%d, Y=%d, W=%d, H=%d", finalXPos, finalYPos, finalBubbleWidth, finalBubbleHeight));
                Log.i(TAG, String.format("FINAL Tail: %s, Offset%%: %.2f, Anchor: %s", finalTailDirection, tailOffsetPercent, anchorBoundsFromArgs.toShortString()));
                // Inside onGlobalLayout, after other measurement logs:
                int learnMoreMeasuredW = learnMoreTextView.getMeasuredWidth();
                int learnMoreMeasuredH = learnMoreTextView.getMeasuredHeight();
                Log.d(TAG, String.format("LearnMoreTV Measured W:%d H:%d", learnMoreMeasuredW, learnMoreMeasuredH));
            }
        });
    }

    private int getStatusBarHeight() {
        int result = 0;
        if (getContext() == null) return 0;
        Resources resources = getContext().getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private float dpToPx(float dp) {
        if (getContext() == null) return dp * Resources.getSystem().getDisplayMetrics().density;
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }
    private int dpToPxInt(float dp) {
        return (int) dpToPx(dp);
    }
}