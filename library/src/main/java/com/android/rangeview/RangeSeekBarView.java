package com.android.rangeview;


import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;

import com.example.library.R;

public class RangeSeekBarView extends View {
    Paint negativePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    PointF arrowPos = new PointF();
    RectF boxRect = new RectF();
    int handleSize;
    int thumbPadding;
    boolean showTrace;
    float x1,x2;
    float top, bottom;
    float right;
    float left;
    float currentX;
    private Handle leftHandle = new Handle();
    private Handle rightHandle = new Handle();
    private Handle moveHandle = new Handle();
    private Handle positionHandle = new Handle();
    private int pointerId;
    private TimeLineChangeListener timeLineChangeListener;

    private long maxValue;

    private Drawable handleDrawable;
    private Drawable handleRightDrawable;
    private Drawable borderDrawable;

    private final int leftGravity;
    private int rightGravity;

    public static int dpToPx(Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    public RangeSeekBarView(Context context) {
        this(context, null);
    }

    public RangeSeekBarView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public RangeSeekBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RangeSeekBarView,
                0, 0);
        handleSize = (int) typedArray.getDimension(R.styleable.RangeSeekBarView_thumb_size, dpToPx(context, 20));
        thumbPadding = typedArray.getDimensionPixelSize(R.styleable.RangeSeekBarView_thumb_padding, 0);
        showTrace = typedArray.getBoolean(R.styleable.RangeSeekBarView_show_trace, true);

        leftGravity = typedArray.getInt(R.styleable.RangeSeekBarView_thumbGravity, Gravity.CENTER);
        rightGravity = ViewHelper.revertGravity(leftGravity);

        int id = typedArray.getResourceId(R.styleable.RangeSeekBarView_thumbSrc, -1);
        if (id != -1) {
            handleDrawable = AppCompatResources.getDrawable(getContext(), id);
        }

        if (handleDrawable != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && handleDrawable.getConstantState() != null) {
                handleRightDrawable = handleDrawable.getConstantState().newDrawable();
                handleRightDrawable.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                handleRightDrawable.setAutoMirrored(true);
            } else {
                handleRightDrawable = handleDrawable;
            }
            if (typedArray.hasValue(R.styleable.RangeSeekBarView_srcTint)) {
                ColorStateList tint = typedArray.getColorStateList(R.styleable.RangeSeekBarView_srcTint);
                handleDrawable = handleDrawable.mutate();
                DrawableCompat.setTintList(handleDrawable, tint);
                handleRightDrawable = handleRightDrawable.mutate();
                DrawableCompat.setTintList(handleRightDrawable, tint);
            }
        }

        id = typedArray.getResourceId(R.styleable.RangeSeekBarView_background, R.drawable.background);
        if (id != -1) {
            borderDrawable = AppCompatResources.getDrawable(getContext(), id);
        }
        if (showTrace) {
            int color = ViewHelper.getDefiningColor(borderDrawable);
            int negativeColor = color != -1 ? ColorUtils.setAlphaComponent(color, 128) : Color.parseColor("#80000000");
            negativePaint.setColor(negativeColor);
        }

        typedArray.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
            int height = handleSize + getPaddingBottom() + getPaddingTop();
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled()) return false;

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                currentX = ev.getX();
                pointerId = ev.getPointerId(0);

                return true;
            case MotionEvent.ACTION_MOVE:
                float newX = ev.getX(0);
                float dx = newX - currentX;
                currentX = newX;
                Log.d("trim", "X " + newX + " dx = " + dx);
//                if (dx < 5 && dy > dx) return super.onTouchEvent(ev);

                boolean hasUpdate = true;
                if (leftHandle.active) {
                    x1 = Math.max(left, Math.min(x1 + dx, x2 - handleSize));
                    Log.d("trim", "left - " + newX);
                } else if (rightHandle.active) {
                    x2 = Math.max(Math.min(x2 + dx, right), x1 + handleSize);
                } else if (moveHandle.active) {
                    moveScene(dx);
                } else if (Math.abs(newX - leftHandle.pos) <= handleSize) {
                    x1 = x1 + dx;
                    leftHandle.pointerId = pointerId;
                    leftHandle.active = true;
                } else if (Math.abs(newX - rightHandle.pos) <= handleSize) {
                    x2 = x2 + dx;
                    rightHandle.pointerId = pointerId;
                    rightHandle.active = true;
                } else if (newX > x1 && newX < x2) {
                    moveScene(dx);
                    moveHandle.active = true;
                } else {
                    hasUpdate = false;
                }
                if (hasUpdate) {
                    if (timeLineChangeListener != null) {
                        timeLineChangeListener.onRangeMove(convertToStart(), convertToEnd());
                    }
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (timeLineChangeListener != null) {
                    if (moveHandle.active || leftHandle.active || rightHandle.active) {
                        long duration = convertToEnd();
                        timeLineChangeListener.onRangeChanged(convertToStart(), duration);
                    }
                }
                leftHandle.active = false;
                rightHandle.active = false;
                moveHandle.active = false;

                break;

        }
        return super.onTouchEvent(ev);
    }

    private long convertToStart() {
        return (long) (maxValue * (x1 - left) / width());
    }

    private long convertToEnd() {
        return (long) (maxValue * ((x2-handleSize - left) / width()));
    }

    private void moveScene(float dx) {
        float prevDiff = x2 - x1;
        if (x1 + dx < left) {
            x1 = left;
            x2 = left + prevDiff;
        } else if (x2 + dx > right) {
            x2 = right;
            x1 = x2 - prevDiff;
        } else {
            x1 += dx;
            x2 += dx;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        x1 = left = getPaddingLeft();
        x2 = right = w - getPaddingRight();
        top = getPaddingTop();
        bottom = h - getPaddingBottom();
    }

    public void setDuration(long duration) {
        this.maxValue = duration;
    }

    public void resetState(long start, long duration, boolean animate) {
        if (getWidth() == 0 || maxValue == 0) return;
        float left = start == 0 ? this.left : this.left + width() * start / maxValue;
        float right = duration == -1 ? this.right : this.right - (width() - width() * duration / maxValue);

        if (animate) {
            ValueAnimator leftAnim = ValueAnimator.ofFloat(x1, left);
            leftAnim.addUpdateListener(animation -> {
                x1 = (float) animation.getAnimatedValue();
                invalidate();
            });
            ValueAnimator rightAnim = ValueAnimator.ofFloat(x2, right);
            rightAnim.addUpdateListener(animation -> {
                x2 = (float) animation.getAnimatedValue();
                invalidate();
            });

            AnimatorSet set = new AnimatorSet();
            set.play(leftAnim).with(rightAnim);
            set.start();
        } else {
            x1 = left;
            x2 = right;
            invalidate();
        }
    }

    private float width() {
        return right - left - handleSize;
    }

    void drawTrace1(Canvas canvas) {
        int borderHeight = borderDrawable.getIntrinsicHeight() > 0 ?
                borderDrawable.getIntrinsicHeight() : (int) boxRect.height();
        int top = (getHeight() - borderHeight) / 2;
        int bottom = top + borderHeight;
        int extra1 = 0; //x1 == left ? 0 : handleSize / 2;
        int extra2 = 0; //x2 == right ? 0 : handleSize / 2;
        canvas.drawRect(left, top, x1 + extra1, bottom, negativePaint);
        canvas.drawRect(x2 - extra2, top, right, bottom, negativePaint);
    }

    void drawTrace2(Canvas canvas) {
        if (x1 > left || right > x2) {
            borderDrawable.setBounds((int) left, (int) top, (int) right, (int) bottom);
            borderDrawable.setAlpha(90);
            borderDrawable.draw(canvas);
            borderDrawable.setAlpha(255);
        }
    }

    protected void drawTrace(Canvas canvas) {
        drawTrace2(canvas);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getWidth() > 0 && getHeight() > 0) {

            boxRect.set(x1, top, x2, bottom);
            if (borderDrawable != null) {
                if (showTrace) {
                    drawTrace(canvas);
                }

                borderDrawable.setBounds((int) boxRect.left, (int)boxRect.top, (int)boxRect.right, (int)boxRect.bottom);
                borderDrawable.draw(canvas);
            }

            if (handleDrawable != null) {
                handleDrawable.setBounds(bound((int) x1, leftGravity));
                handleDrawable.draw(canvas);
                leftHandle.pos = handleDrawable.getBounds().left + handleSize / 2;
            } else {
                leftHandle.pos = x1;
            }

            if (handleRightDrawable != null) {
                handleRightDrawable.setBounds(bound((int) x2, rightGravity));
                handleRightDrawable.draw(canvas);
                rightHandle.pos = handleRightDrawable.getBounds().left + handleSize / 2;
            } else {
                rightHandle.pos = x2;
            }
        }
    }

    private Rect bound(int anchor, int gravity) {
        Rect rect = new Rect();
        switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.CENTER_VERTICAL:
                rect.top = (getHeight() - handleSize) / 2;
                rect.bottom = (getHeight() + handleSize) / 2;
                break;
            case Gravity.BOTTOM:
                rect.top = getHeight() - handleSize;
                rect.bottom = getHeight();
                break;
            case Gravity.TOP:
            default:
                rect.top = 0;
                rect.bottom = handleSize;
                break;
        }

        switch (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.CENTER_HORIZONTAL:
                rect.left = anchor - handleSize / 2;
                break;
            case Gravity.RIGHT:
                rect.left = anchor + thumbPadding;
                break;
            case Gravity.LEFT:
            default:
                rect.left = anchor - handleSize - thumbPadding;
                break;
        }
        rect.right = rect.left + handleSize;

        return rect;
    }

    public void setRange() {
        float diff = x2 / 3;
        x1 += diff;
        arrowPos.x += diff;
        x2 -= diff;
        invalidate();
    }

    public void addIndicatorChangeListener(TimeLineChangeListener timeLineChangeListener) {
        this.timeLineChangeListener = timeLineChangeListener;
    }

    class Handle {
        int pointerId;
        boolean active;
        float pos;
    }

    public interface TimeLineChangeListener {
        void onRangeChanged(long start, long end);
        void onRangeMove(long start, long end);
    }
}