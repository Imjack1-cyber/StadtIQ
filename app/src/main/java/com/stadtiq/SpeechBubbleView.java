package com.stadtiq;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.FrameLayout;

import com.google.android.material.color.MaterialColors;

public class SpeechBubbleView extends FrameLayout {
    private static final String TAG = "SpeechBubbleView";

    private Paint bubblePaint;
    private Paint strokePaint;
    private Path bubblePath;

    private float cornerRadiusDp = 12f;
    private float tailWidthDp = 24f;
    private float tailHeightDp = 16f;
    private float tailHorizontalOffsetPercent = 0.5f;
    private TailDirection tailDirection = TailDirection.DOWN;

    private int bubbleColor;
    private int strokeColor;
    private float strokeWidthDp = 1.5f;

    private int shadowColor = Color.argb(70, 0, 0, 0); // Slightly more pronounced shadow
    private float shadowRadiusDp = 4f;
    private float shadowDxDp = 0f;
    private float shadowDyDp = 2f;

    private float cornerRadius;
    private float tailWidth;
    private float tailHeight;
    private float strokeWidth;
    private float shadowRadius;
    private float shadowDx;
    private float shadowDy;

    // *** CRITICAL CHANGE FOR CLIPPING TEST ***
    private float contentPaddingFromBubbleEdgeDp = 10f; // VERY SMALL - FOR TESTING
    private float contentPaddingFromBubbleEdge;


    public enum TailDirection { UP, DOWN, NONE }

    public SpeechBubbleView(Context context) { super(context); init(); }
    public SpeechBubbleView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public SpeechBubbleView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        setWillNotDraw(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_SOFTWARE, null); // Required for shadow on custom path
        }

        cornerRadius = dpToPx(cornerRadiusDp);
        tailWidth = dpToPx(tailWidthDp);
        tailHeight = dpToPx(tailHeightDp);
        strokeWidth = dpToPx(strokeWidthDp);
        shadowRadius = dpToPx(shadowRadiusDp);
        shadowDx = dpToPx(shadowDxDp);
        shadowDy = dpToPx(shadowDyDp);
        contentPaddingFromBubbleEdge = dpToPx(contentPaddingFromBubbleEdgeDp);

        bubbleColor = MaterialColors.getColor(getContext(), com.google.android.material.R.attr.colorSurface, Color.WHITE);
        strokeColor = MaterialColors.getColor(getContext(), com.google.android.material.R.attr.colorOutline,
                MaterialColors.layer(bubbleColor, Color.BLACK, 0.3f)); // Slightly more contrast for stroke

        bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bubblePaint.setStyle(Paint.Style.FILL);
        bubblePaint.setColor(bubbleColor);
        if (shadowRadius > 0) {
            // Important: Shadows are drawn by this paint.
            // The setLayerType(LAYER_TYPE_SOFTWARE, null) is crucial for this to work on paths.
            bubblePaint.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
        }

        if (strokeWidth > 0) {
            strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor(strokeColor);
            strokePaint.setStrokeWidth(strokeWidth);
            strokePaint.setStrokeCap(Paint.Cap.ROUND);
            strokePaint.setStrokeJoin(Paint.Join.ROUND);
        } else {
            strokePaint = null;
        }

        bubblePath = new Path();
        updateFrameLayoutPaddingAndConfigurePath();
    }

    public void setTailDirection(TailDirection direction) {
        if (this.tailDirection != direction) {
            this.tailDirection = direction;
            updateFrameLayoutPaddingAndConfigurePath(); // This will requestLayout
        }
    }

    public void setTailHorizontalOffsetPercent(float percent) {
        float clampedPercent = Math.max(0.0f, Math.min(1.0f, percent));
        if (Math.abs(this.tailHorizontalOffsetPercent - clampedPercent) > 0.001f) {
            this.tailHorizontalOffsetPercent = clampedPercent;
            if (getWidth() > 0 && getHeight() > 0) {
                configureBubblePath();
                invalidate();
            }
        }
    }

    public float getTailWidthPx() { return this.tailWidth; }
    public float getTailHeightPx() { return this.tailHeight; }
    public float getCornerRadiusPx() { return this.cornerRadius; }

    private void updateFrameLayoutPaddingAndConfigurePath() {
        // Calculate necessary padding for FrameLayout to make space for shadow, stroke, and tail.
        // The content will be placed *inside* this padding by FrameLayout.
        int pShadowL = (int) Math.ceil(shadowRadius - Math.min(0, shadowDx));
        int pShadowT = (int) Math.ceil(shadowRadius - Math.min(0, shadowDy));
        int pShadowR = (int) Math.ceil(shadowRadius + Math.max(0, shadowDx));
        int pShadowB = (int) Math.ceil(shadowRadius + Math.max(0, shadowDy));

        pShadowL = Math.max(0, pShadowL);
        pShadowT = Math.max(0, pShadowT);
        pShadowR = Math.max(0, pShadowR);
        pShadowB = Math.max(0, pShadowB);

        int effectiveStrokePadding = (int) Math.ceil(strokeWidth); // Space for the entire stroke
        int contentP = (int) Math.ceil(contentPaddingFromBubbleEdge); // Internal margin for content

        int pL = pShadowL + effectiveStrokePadding + contentP;
        int pT = pShadowT + effectiveStrokePadding + contentP;
        int pR = pShadowR + effectiveStrokePadding + contentP;
        int pB = pShadowB + effectiveStrokePadding + contentP;

        if (tailDirection == TailDirection.UP) {
            pT += (int) Math.ceil(tailHeight);
        } else if (tailDirection == TailDirection.DOWN) {
            pB += (int) Math.ceil(tailHeight);
        }

        if (getPaddingLeft() != pL || getPaddingTop() != pT || getPaddingRight() != pR || getPaddingBottom() != pB) {
            Log.d(TAG, "Updating FrameLayout Padding: L=" + pL + " T=" + pT + " R=" + pR + " B=" + pB +
                    " (contentPadDp: " + contentPaddingFromBubbleEdgeDp + "px: " + contentPaddingFromBubbleEdge +
                    ", strokeW_px: " + strokeWidth + ", shadowR_px: " + shadowRadius + ")");
            setPadding(pL, pT, pR, pB);
        } else if (getWidth() > 0 && getHeight() > 0 && bubblePath.isEmpty()){
            configureBubblePath();
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "onSizeChanged: " + w + "x" + h + ". Path will be reconfigured.");
        if (w > 0 && h > 0) {
            configureBubblePath();
        }
    }

    private void configureBubblePath() {
        bubblePath.reset();
        final int viewWidth = getWidth();
        final int viewHeight = getHeight();

        if (viewWidth <= 0 || viewHeight <= 0 || strokeWidth < 0) {
            Log.w(TAG, "configureBubblePath: Skip due to zero/negative dimensions or stroke. W:" + viewWidth + " H:" + viewHeight);
            return;
        }

        float halfStroke = strokeWidth / 2f;

        // Define the drawing area for the bubble's path (center of stroke line)
        // This area is inset from view bounds to allow for shadows and half the stroke.
        float pathAreaL = Math.max(halfStroke, shadowRadius - shadowDx) + halfStroke;
        float pathAreaT = Math.max(halfStroke, shadowRadius - shadowDy) + halfStroke;
        float pathAreaR = viewWidth - (Math.max(halfStroke, shadowRadius + shadowDx) + halfStroke);
        float pathAreaB = viewHeight - (Math.max(halfStroke, shadowRadius + shadowDy) + halfStroke);

        if (pathAreaL >= pathAreaR || pathAreaT >= pathAreaB) {
            Log.e(TAG, "configureBubblePath: Illogical path drawing area after insets. L:"+pathAreaL+" R:"+pathAreaR+" T:"+pathAreaT+" B:"+pathAreaB +
                    ". View W:" + viewWidth + " H:" + viewHeight + ". Padding LTRB: "+getPaddingLeft()+","+getPaddingTop()+","+getPaddingRight()+","+getPaddingBottom());
            // Fallback: draw a simple rectangle filling the view minus minimal allowance, if path calculation is bad
            bubblePath.addRect(strokeWidth, strokeWidth, viewWidth - strokeWidth, viewHeight - strokeWidth, Path.Direction.CW);
            return;
        }

        // This is the main rectangle for the bubble body (excluding tail height for now)
        RectF bodyRect = new RectF(pathAreaL, pathAreaT, pathAreaR, pathAreaB);

        // Adjust this bodyRect based on tail direction
        if (tailDirection == TailDirection.UP) {
            bodyRect.top += tailHeight; // Body starts lower if tail is up
        } else if (tailDirection == TailDirection.DOWN) {
            bodyRect.bottom -= tailHeight; // Body ends higher if tail is down
        }

        // Validate bodyRect after tail adjustment
        if (bodyRect.width() <= (strokeWidth*2) || bodyRect.height() <= (strokeWidth*2) || // Must be at least 2x stroke for min visibility
                bodyRect.top >= bodyRect.bottom || bodyRect.left >= bodyRect.right) {
            Log.w(TAG, "Body rect invalid after tail adjustment. W:" + bodyRect.width() + " H:" + bodyRect.height() +
                    " T:" + bodyRect.top + " B:" + bodyRect.bottom + ". Drawing plain rect for path area.");
            bubblePath.addRect(pathAreaL, pathAreaT, pathAreaR, pathAreaB, Path.Direction.CW); // Use original pathArea
            return;
        }

        // Effective corner radius: cannot be larger than half the width/height of the bodyRect
        float R = Math.min(this.cornerRadius, Math.min(bodyRect.width() / 2f, bodyRect.height() / 2f));
        R = Math.max(0, R); // Ensure R is not negative

        // Check if bodyRect is too small even for simple rounded corners or stroke
        if (bodyRect.width() < (2 * R) || bodyRect.height() < (2 * R) ) {
            Log.w(TAG, "Body rect too small for effective corner radius R=" + R +
                    ". W:" + bodyRect.width() + " H:" + bodyRect.height() + ". Drawing simple rect for body.");
            bubblePath.addRect(bodyRect, Path.Direction.CW); // Draw the (potentially tailless-adjusted) bodyRect
        } else {
            bubblePath.addRoundRect(bodyRect, R, R, Path.Direction.CW);
        }

        Log.d(TAG, "Bubble Path Body: " + bodyRect.toShortString() + ", R_eff=" + R +
                ", BodyCalc W=" + bodyRect.width() + ", H=" + bodyRect.height());

        // Add tail path if applicable
        if (tailDirection != TailDirection.NONE && tailHeight > 0 && tailWidth > 0) {
            float flatWidthForTailBase = Math.max(0, bodyRect.width() - 2 * R); // Flat part of body where tail can attach
            float currentEffectiveTailWidth = Math.min(this.tailWidth, flatWidthForTailBase);

            if (currentEffectiveTailWidth > strokeWidth) { // Tail must be wider than stroke to be meaningful
                float halfEffectiveTailWidth = currentEffectiveTailWidth / 2f;
                float tailBaseY, tailTipY;

                if (tailDirection == TailDirection.DOWN) {
                    tailBaseY = bodyRect.bottom;
                    tailTipY = tailBaseY + tailHeight;
                } else { // TailDirection.UP
                    tailBaseY = bodyRect.top;
                    tailTipY = tailBaseY - tailHeight;
                }

                // Calculate tail's horizontal position
                float availableShiftWidthOnBodyFlatPart = flatWidthForTailBase - currentEffectiveTailWidth;
                float tailShift = 0;
                if (availableShiftWidthOnBodyFlatPart > 0) {
                    tailShift = availableShiftWidthOnBodyFlatPart * (tailHorizontalOffsetPercent - 0.5f);
                }
                // Center of the tail's target position on the flat part of the body
                float tailCenterTargetX = bodyRect.left + R + (flatWidthForTailBase / 2f) + tailShift;

                // Clamp tail attachment points to be within the flat part of the body
                float tailAttachPoint1X = Math.max(bodyRect.left + R, tailCenterTargetX - halfEffectiveTailWidth);
                float tailAttachPoint2X = Math.min(bodyRect.right - R, tailCenterTargetX + halfEffectiveTailWidth);

                float finalTailTipX = (tailAttachPoint1X + tailAttachPoint2X) / 2f; // Tip is centered on actual base

                if (tailAttachPoint2X > tailAttachPoint1X + strokeWidth) { // Ensure base has some width
                    Path tailPathPart = new Path();
                    tailPathPart.moveTo(tailAttachPoint1X, tailBaseY);
                    tailPathPart.lineTo(finalTailTipX, tailTipY);
                    tailPathPart.lineTo(tailAttachPoint2X, tailBaseY);
                    bubblePath.addPath(tailPathPart); // Add to existing path
                } else {
                    Log.w(TAG, "Tail could not be formed (actual base width too small). Attach1X: " + tailAttachPoint1X + " Attach2X: " + tailAttachPoint2X);
                }
            } else {
                Log.w(TAG, "Effective tail width ("+currentEffectiveTailWidth+") is too small for stroke or flat area, tail not drawn. FlatWidth: "+flatWidthForTailBase + " R_eff=" + R);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // The shadow is drawn by bubblePaint if setShadowLayer was called.
        // LAYER_TYPE_SOFTWARE is needed for shadows on custom paths.
        if (bubblePath.isEmpty() && (getWidth() > 0 && getHeight() > 0)) {
            Log.w(TAG, "onDraw: Path was empty, reconfiguring.");
            configureBubblePath();
        }

        if (!bubblePath.isEmpty()) {
            canvas.drawPath(bubblePath, bubblePaint); // Draws fill (and its shadow)
            if (strokePaint != null) {
                canvas.drawPath(bubblePath, strokePaint); // Draws stroke over fill
            }
        } else {
            Log.e(TAG, "Bubble path is STILL empty in onDraw. Cannot draw bubble. View W:" + getWidth() + " H:" +getHeight());
        }
        // Children (content TextViews etc.) are drawn by FrameLayout's super.dispatchDraw() after this.
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}