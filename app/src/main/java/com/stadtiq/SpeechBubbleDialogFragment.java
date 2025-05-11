package com.stadtiq;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.stadtiq.R;


public class SpeechBubbleDialogFragment extends DialogFragment {

    private static final String TAG = "SpeechBubbleDlg";

    private static final String ARG_VALUE_DISPLAY_NAME = "value_display_name";
    private static final String ARG_EXPLANATION = "explanation_text";
    private static final String ARG_ANCHOR_BOUNDS = "anchor_bounds";
    private static final String ARG_VALUE_DATA_KEY = "value_data_key";

    private SpeechBubbleView speechBubbleView;
    private Bundle retrievedArgs;
    private TextView learnMoreTextViewInternal; // To access in post runnable if needed


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
            Log.e(TAG, "onCreateView: Root view is NOT SpeechBubbleView. Is: " + (rootView != null ? rootView.getClass().getName() : "null"));
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

        final TextView explanationTextView = this.speechBubbleView.findViewById(R.id.text_explanation);
        learnMoreTextViewInternal = this.speechBubbleView.findViewById(R.id.text_learn_more);

        if (explanationTextView == null || learnMoreTextViewInternal == null) {
            Log.e(TAG, "Required views (explanation, learnMore) not found. Dismissing.");
            dismissAllowingStateLoss(); return;
        }

        explanationTextView.setText(explanationText);
        learnMoreTextViewInternal.setText(R.string.learn_more);

        learnMoreTextViewInternal.setOnClickListener(v -> {
            Log.d(TAG, "Learn More CLICKED! (ValueKey: " + valueDataKey + ")");
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
                        anchorBoundsFromArgs == null || learnMoreTextViewInternal == null) {
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
                int minScreenMarginPx = dpToPxInt(10f);
                int maxBubbleWidth = screenWidth - (2 * minScreenMarginPx);

                speechBubbleView.setTailDirection(SpeechBubbleView.TailDirection.DOWN);
                speechBubbleView.measure(
                        View.MeasureSpec.makeMeasureSpec(maxBubbleWidth, View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST));
                int measuredHeightDown = speechBubbleView.getMeasuredHeight();
                int yPosIfAbove = anchorBoundsFromArgs.top - measuredHeightDown - dpToPxInt(4f);

                speechBubbleView.setTailDirection(SpeechBubbleView.TailDirection.UP);
                speechBubbleView.measure(
                        View.MeasureSpec.makeMeasureSpec(maxBubbleWidth, View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST));
                int measuredHeightUp = speechBubbleView.getMeasuredHeight();
                int yPosIfBelow = anchorBoundsFromArgs.bottom + dpToPxInt(4f);

                boolean canFitAbove = (yPosIfAbove >= statusBarHeight + minScreenMarginPx);
                boolean canFitBelow = (yPosIfBelow + measuredHeightUp <= screenHeight - minScreenMarginPx);
                SpeechBubbleView.TailDirection finalTailDirection;

                if (canFitAbove) {
                    finalTailDirection = SpeechBubbleView.TailDirection.DOWN;
                } else if (canFitBelow) {
                    finalTailDirection = SpeechBubbleView.TailDirection.UP;
                } else {
                    finalTailDirection = (anchorBoundsFromArgs.top - measuredHeightDown > screenHeight - (anchorBoundsFromArgs.bottom + measuredHeightUp)) ?
                            SpeechBubbleView.TailDirection.DOWN : SpeechBubbleView.TailDirection.UP;
                }

                speechBubbleView.setTailDirection(finalTailDirection);
                speechBubbleView.measure(
                        View.MeasureSpec.makeMeasureSpec(maxBubbleWidth, View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
                );
                int finalBubbleWidth = speechBubbleView.getMeasuredWidth();
                int finalBubbleHeight = speechBubbleView.getMeasuredHeight();

                Log.i(TAG, String.format("MEASURE_FINAL -> SBV W:%d H:%d | ExplTV Lines:%d | LMTV W:%d H:%d",
                        finalBubbleWidth, finalBubbleHeight,
                        explanationTextView.getLayout() != null ? explanationTextView.getLayout().getLineCount() : -1,
                        learnMoreTextViewInternal.getMeasuredWidth(), learnMoreTextViewInternal.getMeasuredHeight()
                ));

                int finalYPos;
                if (finalTailDirection == SpeechBubbleView.TailDirection.DOWN) {
                    finalYPos = anchorBoundsFromArgs.top - finalBubbleHeight - dpToPxInt(4f);
                } else {
                    finalYPos = anchorBoundsFromArgs.bottom + dpToPxInt(4f);
                }
                finalYPos = Math.max(statusBarHeight + minScreenMarginPx, finalYPos);
                if (finalYPos + finalBubbleHeight > screenHeight - minScreenMarginPx) {
                    finalYPos = screenHeight - finalBubbleHeight - minScreenMarginPx;
                    if (finalYPos < statusBarHeight + minScreenMarginPx) {
                        finalYPos = statusBarHeight + minScreenMarginPx;
                    }
                }

                int finalXPos = anchorBoundsFromArgs.centerX() - finalBubbleWidth / 2;
                if (finalXPos < minScreenMarginPx) {
                    finalXPos = minScreenMarginPx;
                }
                if (finalXPos + finalBubbleWidth > screenWidth - minScreenMarginPx) {
                    finalXPos = screenWidth - finalBubbleWidth - minScreenMarginPx;
                    if (finalXPos < minScreenMarginPx && finalBubbleWidth < screenWidth - 2 * minScreenMarginPx) finalXPos = minScreenMarginPx;
                }

                float tailOffsetPercent = 0.5f;
                if (finalBubbleWidth > 0 && speechBubbleView.getTailWidthPx() > 0 && speechBubbleView.getCornerRadiusPx() >= 0) {
                    float targetTailTipAbsoluteX = (float)anchorBoundsFromArgs.centerX();
                    float targetTailTipRelativeToBubbleOrigin = targetTailTipAbsoluteX - finalXPos;
                    float availableContentWidth = finalBubbleWidth - speechBubbleView.getPaddingLeft() - speechBubbleView.getPaddingRight();
                    float flatBodyWidth = availableContentWidth - (2 * speechBubbleView.getCornerRadiusPx());

                    if (flatBodyWidth > speechBubbleView.getTailWidthPx()) {
                        float offsetFromFlatBodyStart = targetTailTipRelativeToBubbleOrigin - speechBubbleView.getPaddingLeft() - speechBubbleView.getCornerRadiusPx();
                        tailOffsetPercent = offsetFromFlatBodyStart / flatBodyWidth;
                    }
                }
                tailOffsetPercent = Math.max(0.01f, Math.min(0.99f, tailOffsetPercent));
                speechBubbleView.setTailHorizontalOffsetPercent(tailOffsetPercent);

                params.gravity = Gravity.TOP | Gravity.START;
                params.x = finalXPos;
                params.y = finalYPos;

                if (finalBubbleWidth > 0 && finalBubbleHeight > 0) {
                    window.setLayout(finalBubbleWidth, finalBubbleHeight);
                } else {
                    Log.e(TAG, "Final bubble dimensions zero/invalid, using WRAP_CONTENT.");
                    window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
                }
                window.setAttributes(params);

                Log.i(TAG, String.format("FINAL Bubble Pos: X=%d, Y=%d, W=%d, H=%d, Tail:%s",
                        finalXPos, finalYPos, finalBubbleWidth, finalBubbleHeight, finalTailDirection));

                if (learnMoreTextViewInternal != null) {
                    Log.d(TAG, "Attempting final refresh of LearnMoreTV. IsShown: " + learnMoreTextViewInternal.isShown() + " W: " + learnMoreTextViewInternal.getWidth() + " H: " + learnMoreTextViewInternal.getHeight());
                    learnMoreTextViewInternal.post(() -> {
                        if (learnMoreTextViewInternal == null || speechBubbleView == null || !isAdded()) return;

                        learnMoreTextViewInternal.invalidate();
                        speechBubbleView.invalidate();
                        // Log the height *after* the invalidate has been posted, though it might not reflect an immediate change
                        Log.d(TAG, "Posted invalidate for LearnMoreTV and SpeechBubbleView. LMTV Current H: " + learnMoreTextViewInternal.getHeight() + " W: " + learnMoreTextViewInternal.getWidth());
                    });
                }
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

    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        super.show(manager, tag);
    }
}