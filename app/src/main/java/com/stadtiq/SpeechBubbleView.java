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

    // Configurable Dp values
    private float cornerRadiusDp = 12f;
    private float tailWidthDp = 24f;
    private float tailHeightDp = 16f;
    private float tailHorizontalOffsetPercent = 0.5f;
    private TailDirection tailDirection = TailDirection.DOWN;

    private int bubbleColor;
    private int strokeColor;
    private float strokeWidthDp = 1.5f;

    private int shadowColor = Color.argb(60, 0, 0, 0);
    private float shadowRadiusDp = 4f;
    private float shadowDxDp = 0f;
    private float shadowDyDp = 2f;

    // Pixel values, converted from Dp
    private float cornerRadius;
    private float tailWidth;
    private float tailHeight;
    private float strokeWidth;
    private float shadowRadius;
    private float shadowDx;
    private float shadowDy;

    private float contentPaddingFromBubbleEdgeDp = 12f;
    private float contentPaddingFromBubbleEdge;


    public enum TailDirection { UP, DOWN, NONE }

    public SpeechBubbleView(Context context) { super(context); init(); }
    public SpeechBubbleView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public SpeechBubbleView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        setWillNotDraw(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
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
                MaterialColors.layer(bubbleColor, Color.BLACK, 0.3f));

        bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bubblePaint.setStyle(Paint.Style.FILL);
        bubblePaint.setColor(bubbleColor);
        if (shadowRadius > 0) {
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
            updateFrameLayoutPaddingAndConfigurePath();
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
        int pShadowL = (int) Math.ceil(shadowRadius - Math.min(0, shadowDx));
        int pShadowT = (int) Math.ceil(shadowRadius - Math.min(0, shadowDy));
        int pShadowR = (int) Math.ceil(shadowRadius + Math.max(0, shadowDx));
        int pShadowB = (int) Math.ceil(shadowRadius + Math.max(0, shadowDy));

        pShadowL = Math.max(0, pShadowL);
        pShadowT = Math.max(0, pShadowT);
        pShadowR = Math.max(0, pShadowR);
        pShadowB = Math.max(0, pShadowB);

        int effectiveStrokePadding = (int) Math.ceil(strokeWidth);
        int contentP = (int) Math.ceil(contentPaddingFromBubbleEdge);

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
            Log.d(TAG, "Updating FrameLayout Padding: L=" + pL + " T=" + pT + " R=" + pR + " B=" + pB);
            setPadding(pL, pT, pR, pB);
        } else if (getWidth() > 0 && getHeight() > 0 && bubblePath.isEmpty()){
            // If padding didn't change but path is not configured yet (e.g., initial call)
            configureBubblePath();
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "onSizeChanged: " + w + "x" + h);
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

        float pathAreaL = Math.max(halfStroke, shadowRadius - shadowDx) + halfStroke;
        float pathAreaT = Math.max(halfStroke, shadowRadius - shadowDy) + halfStroke;
        float pathAreaR = viewWidth - (Math.max(halfStroke, shadowRadius + shadowDx) + halfStroke);
        float pathAreaB = viewHeight - (Math.max(halfStroke, shadowRadius + shadowDy) + halfStroke);

        if (pathAreaL >= pathAreaR || pathAreaT >= pathAreaB) {
            Log.e(TAG, "configureBubblePath: Illogical path drawing area. L:"+pathAreaL+" R:"+pathAreaR+" T:"+pathAreaT+" B:"+pathAreaB);
            // Fallback: draw a simple rectangle filling the view minus minimal allowance
            bubblePath.addRect(strokeWidth, strokeWidth, viewWidth - strokeWidth, viewHeight - strokeWidth, Path.Direction.CW);
            return;
        }

        RectF bodyRect = new RectF(pathAreaL, pathAreaT, pathAreaR, pathAreaB);

        if (tailDirection == TailDirection.UP) {
            bodyRect.top += tailHeight;
        } else if (tailDirection == TailDirection.DOWN) {
            bodyRect.bottom -= tailHeight;
        }

        if (bodyRect.width() <= (strokeWidth*2) || bodyRect.height() <= (strokeWidth*2) ||
                bodyRect.top >= bodyRect.bottom || bodyRect.left >= bodyRect.right) {
            Log.w(TAG, "Body rect invalid after tail adjustment. W:" + bodyRect.width() + " H:" + bodyRect.height());
            bubblePath.addRect(pathAreaL, pathAreaT, pathAreaR, pathAreaB, Path.Direction.CW); // Use original pathArea
            return;
        }

        float R = Math.min(this.cornerRadius, Math.min(bodyRect.width() / 2f, bodyRect.height() / 2f));
        R = Math.max(0, R);

        if (bodyRect.width() < (2 * R) || bodyRect.height() < (2 * R) ) {
            Log.w(TAG, "Body rect too small for effective corner radius R=" + R + ". Drawing simple rect.");
            bubblePath.addRect(bodyRect, Path.Direction.CW);
        } else {
            bubblePath.addRoundRect(bodyRect, R, R, Path.Direction.CW);
        }

        if (tailDirection != TailDirection.NONE && tailHeight > 0 && tailWidth > 0) {
            float flatWidthForTailBase = Math.max(0, bodyRect.width() - 2 * R);
            float currentEffectiveTailWidth = Math.min(this.tailWidth, flatWidthForTailBase);

            if (currentEffectiveTailWidth > strokeWidth) {
                float halfEffectiveTailWidth = currentEffectiveTailWidth / 2f;
                float tailBaseY, tailTipY;

                if (tailDirection == TailDirection.DOWN) {
                    tailBaseY = bodyRect.bottom;
                    tailTipY = tailBaseY + tailHeight;
                } else {
                    tailBaseY = bodyRect.top;
                    tailTipY = tailBaseY - tailHeight;
                }

                float availableShiftWidthOnBodyFlatPart = flatWidthForTailBase - currentEffectiveTailWidth;
                float tailShift = 0;
                if (availableShiftWidthOnBodyFlatPart > 0) {
                    tailShift = availableShiftWidthOnBodyFlatPart * (tailHorizontalOffsetPercent - 0.5f);
                }
                float tailCenterTargetX = bodyRect.left + R + (flatWidthForTailBase / 2f) + tailShift;

                float tailAttachPoint1X = Math.max(bodyRect.left + R, tailCenterTargetX - halfEffectiveTailWidth);
                float tailAttachPoint2X = Math.min(bodyRect.right - R, tailCenterTargetX + halfEffectiveTailWidth);

                float finalTailTipX = (tailAttachPoint1X + tailAttachPoint2X) / 2f;

                if (tailAttachPoint2X > tailAttachPoint1X + strokeWidth) {
                    Path tailPathPart = new Path();
                    tailPathPart.moveTo(tailAttachPoint1X, tailBaseY);
                    tailPathPart.lineTo(finalTailTipX, tailTipY);
                    tailPathPart.lineTo(tailAttachPoint2X, tailBaseY);
                    bubblePath.addPath(tailPathPart);
                } else {
                    Log.w(TAG, "Tail could not be formed (actual base width too small).");
                }
            } else {
                Log.w(TAG, "Effective tail width ("+currentEffectiveTailWidth+") is too small, tail not drawn.");
            }
        }
        Log.d(TAG, "Bubble path configured. BodyRect: " + bodyRect.toShortString() + ", R_eff=" + R);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bubblePath.isEmpty() && (getWidth() > 0 && getHeight() > 0)) {
            Log.w(TAG, "onDraw: Path was empty, reconfiguring.");
            configureBubblePath();
        }

        if (!bubblePath.isEmpty()) {
            canvas.drawPath(bubblePath, bubblePaint);
            if (strokePaint != null) {
                canvas.drawPath(bubblePath, strokePaint);
            }
        } else {
            Log.e(TAG, "Bubble path is empty in onDraw. Cannot draw bubble shape.");
        }
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}