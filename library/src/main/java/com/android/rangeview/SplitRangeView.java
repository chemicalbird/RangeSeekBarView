package com.android.rangeview;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import java.util.ArrayList;
import java.util.List;

public class SplitRangeView extends View {
    private static final String TAG = SplitRangeView.class.getSimpleName();

    private final int prefferedTextHeight;
    RectF boxRect = new RectF();
    int handleSize;
    int thumbPadding;
    float top, bottom;
    float currentX;

    private TimeLineChangeListener timeLineChangeListener;

    private Drawable handleDrawable;
    private Drawable handleRightDrawable;
    private Drawable borderDrawable;

    private final int leftGravity;
    private int rightGravity;

    private List<Span> rangeSpans = new ArrayList<>();
    private Span activeSpan;
    private Paint spanTextPaint;

    private GestureDetector gestureDetector;
    private Rect tempRect = new Rect();

    private int textPad = 40;
    private int minimumSize = 10;

    public void setInfoPadding(int textPad) {
        this.textPad = textPad;
    }

    public void setMinimumSize(int minimumSize) {
        this.minimumSize = minimumSize;
    }

    public static int dpToPx(Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    public SplitRangeView(Context context) {
        this(context, null);
    }

    public SplitRangeView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public SplitRangeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return singleTapConfirmed(e);
            }
        });

        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RangeSeekBarView,
                0, 0);
        handleSize = (int) typedArray.getDimension(R.styleable.RangeSeekBarView_thumb_size, dpToPx(context, 20));
        thumbPadding = typedArray.getDimensionPixelSize(R.styleable.RangeSeekBarView_thumb_padding, 0);

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

        typedArray.recycle();

        spanTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        spanTextPaint.setTextSize(dpToPx(getContext(), 12));
        spanTextPaint.setColor(Color.WHITE);
        spanTextPaint.getTextBounds("A", 0, 1, tempRect);
        prefferedTextHeight = tempRect.height();
    }

    public void setTextColor(int color) {
        spanTextPaint.setColor(color);
    }

    public void setTextSize(float size) {
        spanTextPaint.setTextSize(size);
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
        gestureDetector.onTouchEvent(ev);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                currentX = ev.getX();
                activeSpan = findActiveSpanUnder(currentX);
                return true;
            case MotionEvent.ACTION_MOVE:
                float newX = ev.getX(0);
                float newY = ev.getY(0);
                float dx = newX - currentX;
                currentX = newX;

                int dxInt = (int) dx;
                boolean hasUpdate = false;
                if (activeSpan != null) {
                    if (activeSpan.handlesShowing) {
                        hasUpdate = true;
                        if (activeSpan.leftDragging) {
                            handleLeftMovement(activeSpan, dxInt);
                        } else if (activeSpan.rightDragging) {
                            handleRightMovement(activeSpan, dxInt);
                        } else if (activeSpan.translateDragging) {
                            int amount = computeActualDistance(activeSpan, dxInt);
                            if (amount != 0) {
                                activeSpan.move(amount);
                            } else {
                                hasUpdate = false;
                            }
                        } else {
                            boolean leftThumb = bound(activeSpan.offset, leftGravity).contains((int)newX, (int)newY);
                            boolean rightThumb = bound(activeSpan.end(), rightGravity).contains((int)newX, (int)newY);
                            if (leftThumb) {
                                handleLeftMovement(activeSpan, dxInt);
                                activeSpan.leftDragging = true;
                            } else if (rightThumb) {
                                handleRightMovement(activeSpan, dxInt);
                                activeSpan.rightDragging = true;
                            } else {
                                int amount = computeActualDistance(activeSpan, dxInt);
                                if (amount != 0) {
                                    activeSpan.translateDragging = true;
                                    activeSpan.move(amount);
                                } else {
                                    hasUpdate = false;
                                }
                            }
                        }
                    }
                    if (hasUpdate) {
                        invalidate();
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (activeSpan != null && timeLineChangeListener != null) {
                    timeLineChangeListener.onRangeChanged(activeSpan.tag, activeSpan.offset * 1F / getWidth() , activeSpan.end() * 1F / getWidth());
                }
                if (activeSpan != null) {
                    activeSpan.leftDragging = activeSpan.rightDragging = activeSpan.translateDragging = false;
                    activeSpan = null;
                }

                break;

        }
        return super.onTouchEvent(ev);
    }

    private void handleLeftMovement(Span span, int dx) {
        if (dx < 0) {
            int newDx = computeActualDistance(span, dx);
            if (newDx != 0) {
                span.shrinkLeft(newDx);
            }
        } else {
            span.shrinkLeft(Math.min(dx, span.length - minimumSize));
        }
    }

    private void handleRightMovement(Span span, int dx) {
        if (dx > 0) {
            int newDx = computeActualDistance(span, dx);
            if (newDx != 0) {
                span.length += newDx;
            }
        } else {
            span.length = Math.max(span.length + dx, minimumSize);
        }
    }

    private int computeActualDistance(Span target, int dx) {
        boolean canMove = dx <= 0 && target.offset + dx > 0 || dx > 0 && target.end() + dx < getWidth();
        if (!canMove) {
            return dx <= 0 ? -target.offset : getWidth() - target.end(); // compensate
        }

        for (Span child: rangeSpans) {
            if (child == target) {
                continue;
            }

            if (dx > 0) {
                if (child.offset > target.offset) { // check childs right off
                    canMove = child.offset >= target.end() + dx;
                }
            } else {
                if (child.end() <= target.offset) { // check childs left off
                    canMove = child.end() <= target.offset + dx;
                }
            }

            if (!canMove) {
                // requested dx can be bigger: compensate
                return dx > 0 ? child.offset - target.end() : child.end() - target.offset;
            }
        }

        return dx;
    }

    private int extraBasedOnThumbGravity() {
        int gravityPadding = 0;
        int mask = (leftGravity & Gravity.HORIZONTAL_GRAVITY_MASK);
        if (mask == Gravity.LEFT) {
            gravityPadding = handleSize;
        } else if (mask == Gravity.CENTER_HORIZONTAL) {
            gravityPadding = handleSize / 2;
        }

        return gravityPadding;
    }

    private Span findActiveSpanUnder(float x) {
        int extraPadding = extraBasedOnThumbGravity();
        for (Span range: rangeSpans) {
            if (range.handlesShowing && range.offset - extraPadding < x && x < range.end() +  extraPadding) {
                return range;
            }
        }
        return null;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        top = getPaddingTop();
        bottom = h - getPaddingBottom();
    }

    public void addSpan(int offset, int length, String info, Object obj) {
        Span newSpan = new Span(offset, length, info, obj);
        if (verifyBoundsAreCorrect(newSpan)) {
            rangeSpans.add(newSpan);
        }
    }

    private boolean verifyBoundsAreCorrect(Span item) {
        for (Span range : rangeSpans) {
            if ((overlap(item, range))) {
                Log.d(TAG, "Cannot have overlapping bars");
                return false;
            }
        }
        return true;
    }

    public void addSpan(Span span) {
        if (verifyBoundsAreCorrect(span)) {
            rangeSpans.add(span);
        }
    }

    public void addSpan(int offset, int length, String info) {
        addSpan(offset, length, info, null);
    }

    public void addSpan(int offset, int length) {
        addSpan(offset, length, null, null);
    }

    public void removeSpan(Object tag) {
        int i;
        for (i = 0; i < rangeSpans.size(); i++) {
            if (rangeSpans.get(i).tag == tag) {
                break;
            }
        }
        if (i < rangeSpans.size()) {
            rangeSpans.remove(i);
            invalidate();
        }
    }

    public void updateSpan(Object tag, String newInfo) {
        Span span = findSpanByTag(tag);
        if (span != null) {
            span.info = newInfo;
            invalidate();
        }
    }

    public void updateSpan(Object tag, boolean selected) {
        Span span = findSpanByTag(tag);
        if (span != null) {
            span.handlesShowing = selected;
            invalidate();
        }
    }

    public void updateSpanRange(Object tag, int offset, int length) {
        Span span = findSpanByTag(tag);
        if (span != null) {
            int oldOffset = span.offset;
            int oldLen = span.length;

            span.offset = offset;
            span.length = length;

            for (Span target : rangeSpans) {
                if (target != span && overlap(span, target)) {
                    span.offset = oldOffset;
                    span.length = oldLen;
                    Log.d(TAG, "Property update fail: Overlapping");
                }
            }

            invalidate();
        }
    }

    private Span findSpanByTag(Object tag) {
        for (Span span: rangeSpans) {
            if (span.tag == tag) {
                return span;
            }
        }
        return null;
    }

    private boolean overlap(Span target, Span source) {
        return target.offset < source.end() && source.offset < target.end();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getWidth() > 0 && getHeight() > 0) {

            Span selectedSpan = null;
            for (Span item: rangeSpans) {
                boxRect.set(item.offset, top, item.end(), bottom);

                if (!item.draw(canvas, boxRect)) {

                    if (borderDrawable != null) {
                        borderDrawable.setBounds((int) boxRect.left, (int)boxRect.top, (int)boxRect.right, (int)boxRect.bottom);
                        borderDrawable.setState(item.handlesShowing ? SELECTED_STATE_SET : EMPTY_STATE_SET);
                        borderDrawable.draw(canvas);
                    }

                    String txt = item.info;
                    if (!TextUtils.isEmpty(txt)) {
                        int leftPad = (leftGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.RIGHT ? handleSize + thumbPadding : textPad;
                        drawInfo(canvas, item, leftPad, boxRect);
                    }
                }

                if (item.handlesShowing) {
                    selectedSpan = item;
                }
            }
            if (selectedSpan != null) {
                if (handleDrawable != null) {
                    Rect leftBounds = bound(selectedSpan.offset, leftGravity);
                    Rect rightBounds = bound(selectedSpan.end(), rightGravity);
                    if (leftBounds.right < rightBounds.left) {
                        handleDrawable.setBounds(leftBounds);
                        handleDrawable.draw(canvas);

                        handleRightDrawable.setBounds(rightBounds);
                        handleRightDrawable.draw(canvas);
                    }
                }

            }
        }
    }

    private void drawInfo(Canvas canvas, Span span, int leftPadding, RectF boxRect) {
        String txt = span.info;
        spanTextPaint.getTextBounds(txt, 0, span.info.length(), tempRect);
        if (boxRect.width() - 2*textPad < tempRect.width()) {
            int newEnd = (int) (txt.length() * (boxRect.width() - 2*textPad) / tempRect.width());
            txt = newEnd <= 0 ? "" : newEnd < txt.length() ? txt.substring(0, newEnd) : txt;
        }
        canvas.drawText(txt, boxRect.left + leftPadding, boxRect.bottom - (boxRect.height() - prefferedTextHeight) / 2,
                spanTextPaint);
    }

    private Rect bound(int anchor, int gravity) {
//        int height = (int) (handleSize * handleDrawable.getIntrinsicHeight() * 1F / handleDrawable.getIntrinsicWidth());
        int height = handleSize;
        Rect rect = new Rect();
        switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.CENTER_VERTICAL:
                rect.top = (getHeight() - height) / 2;
                rect.bottom = (getHeight() + height) / 2;
                break;
            case Gravity.BOTTOM:
                rect.top = getHeight() - height;
                rect.bottom = getHeight();
                break;
            case Gravity.TOP:
            default:
                rect.top = 0;
                rect.bottom = height;
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

    public void addIndicatorChangeListener(TimeLineChangeListener timeLineChangeListener) {
        this.timeLineChangeListener = timeLineChangeListener;
    }

    private boolean singleTapConfirmed(MotionEvent e) {
        float x = e.getX();
        Span spanToSelect = null;
        Span spanToDeselect = null;
        int gravityPadding = extraBasedOnThumbGravity();

        for (Span item: rangeSpans) {

            if (item.handlesShowing) {
                if (x < item.offset - gravityPadding || x > item.end() + gravityPadding) { // is out ?
                    item.handlesShowing = false;
                    spanToDeselect = item;
                    Log.d(TAG, "UNSelect " + spanToDeselect.hashCode());
                } else {
                    if (x < item.offset - gravityPadding + handleSize) {
                        Log.d(TAG, "Left click");
                        if (timeLineChangeListener != null) {
                            timeLineChangeListener.onThumbClicked(item.tag, 0);
                        }
                    } else if (x > item.end() + gravityPadding - handleSize) {
                        Log.d(TAG, "Right click");
                        if (timeLineChangeListener != null) {
                            timeLineChangeListener.onThumbClicked(item.tag, 1);
                        }
                    }
                    if (spanToSelect != null) {
                        spanToSelect.handlesShowing = false;
                        spanToSelect = null;
                    }
                    break;
                }
            } else if (item.offset < x && x < item.end()) {
                item.handlesShowing = true;
                spanToSelect = item;
                Log.d("Splity", "Select " + spanToSelect.hashCode());
            }
        }

        if (spanToSelect != null) {
            notifySelectionChange(spanToSelect.tag, true);
        } else if (spanToDeselect != null) {
            notifySelectionChange(spanToDeselect.tag, false);
        }

        invalidate();
        return false;
    }

    private void notifySelectionChange(Object tag, boolean val) {
        if (timeLineChangeListener != null) {
            timeLineChangeListener.onSelectionChange(tag, val);
        }
    }

    public interface TimeLineChangeListener {
        void onRangeChanged(Object tag, float startFraction, float endFraction);
        void onSelectionChange(Object tag, boolean selected);
        void onThumbClicked(Object tag, int thumbId);
    }

    public static class Span {
        int offset;
        int length;
        String info;
        Object tag;
        boolean handlesShowing;
        boolean leftDragging;
        boolean rightDragging;
        boolean translateDragging;

        public Span(int offset, int length, String info, Object tag) {
            this.offset = offset;
            this.length = length;
            this.info = info;
            this.tag = tag;
        }

        protected boolean draw(Canvas canvas, RectF bound) {
            return false;
        }

        protected boolean isSelected() {
            return handlesShowing;
        }

        public int end() {
            return offset + length;
        }

        void shrinkLeft(int dx) {
            int oldOffset = offset;
            offset += dx;
            length = length - (offset - oldOffset);
        }

        public void move(int dx) {
            offset += dx;
        }
    }
}