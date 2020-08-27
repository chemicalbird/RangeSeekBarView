package com.example.rangeview;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.widget.TextView;

import com.android.rangeview.RangeSeekBarView;
import com.android.rangeview.SplitRangeView;

public class MainActivity extends AppCompatActivity {
    private long TOTAL = 1000L;

    RangeSeekBarView.TimeLineChangeListener listener = new RangeSeekBarView.TimeLineChangeListener() {
        @Override
        public void onRangeChanged(float start, float end) {
            infoView.setText(String.format("%.2f : %.2f", TOTAL * start, TOTAL * end));
        }

        @Override
        public void onRangeMove(float start, float end) {
            infoView.setText(String.format("%.2f : %.2f", TOTAL * start, TOTAL * end));
        }
    };
    private TextView infoView;
    private Object lastRangeChangeObject;
    private float testStartFraction;
    private float testEndFraction;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        infoView = findViewById(R.id.info);

        RangeSeekBarView seekBarView1 = findViewById(R.id.seekbar1);
        seekBarView1.setMinValueFactor(50F / TOTAL);
        seekBarView1.addIndicatorChangeListener(listener);

        RangeSeekBarView seekBarView2 = findViewById(R.id.seekbar2);
        seekBarView2.setMinValueFactor(150F / TOTAL);
        seekBarView2.addIndicatorChangeListener(listener);

        RangeSeekBarView seekBarView3 = findViewById(R.id.seekbar3);
        seekBarView3.addIndicatorChangeListener(listener);

        RangeSeekBarView seekBarView4 = findViewById(R.id.seekbar4);
        seekBarView4.addIndicatorChangeListener(listener);

        RangeSeekBarView seekBarView5 = findViewById(R.id.seekbar5);
        seekBarView5.addIndicatorChangeListener(listener);

        RangeSeekBarView seekBarView6 = findViewById(R.id.seekbar6);
        seekBarView6.setMinValueFactor(0.1f);
        seekBarView6.addIndicatorChangeListener(listener);

        findViewById(R.id.reset).setOnClickListener(v -> {
            seekBarView1.updateSpanDimensions(0, 1, false);
            seekBarView2.updateSpanDimensions(0, 1, true);
            seekBarView3.updateSpanDimensions(0, 1, true);
            seekBarView4.updateSpanDimensions(0, 1, true);
            seekBarView5.updateSpanDimensions(0.1f, 0.4f, true);
            seekBarView6.updateSpanDimensions(0, 1, true);
        });

        SplitRangeView splitRangeView = findViewById(R.id.split_range);
        splitRangeView.getLayoutParams().width = 60 * 100;
//        splitRangeView.addSpan(0, 100, "", 1);

        splitRangeView.addSpan(200, 399, "", 2);

        splitRangeView.addSpan(800, 790, "", 3);
        splitRangeView.addSpan(2000, 2200, "", 4);

        Paint customSpanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        customSpanPaint.setColor(Color.CYAN);

        Paint customSelectedSpanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        customSelectedSpanPaint.setColor(Color.WHITE);
        customSelectedSpanPaint.setStyle(Paint.Style.STROKE);
        customSelectedSpanPaint.setStrokeWidth(4);

        SplitRangeView.Span span = new SplitRangeView.Span(0, 100, "", 1) {
            @Override
            protected boolean draw(Canvas canvas, RectF bound) {
                canvas.drawRoundRect(bound, 8, 8, customSpanPaint);
                if (isSelected()) {
                    canvas.drawRoundRect(bound, 8, 8, customSelectedSpanPaint);
                }
                return true;
            }
        };
        splitRangeView.addSpan(span);
    }
}
