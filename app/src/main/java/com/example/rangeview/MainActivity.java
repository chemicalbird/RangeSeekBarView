package com.example.rangeview;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.android.rangeview.RangeSeekBarView;

public class MainActivity extends AppCompatActivity {

    RangeSeekBarView.TimeLineChangeListener listener = new RangeSeekBarView.TimeLineChangeListener() {
        @Override
        public void onRangeChanged(long start, long end) {
            infoView.setText(String.format("%d : %d", start, end));
        }

        @Override
        public void onRangeMove(long start, long end) {
            infoView.setText(String.format("%d : %d", start, end));
        }
    };
    private TextView infoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        infoView = findViewById(R.id.info);

        RangeSeekBarView seekBarView1 = findViewById(R.id.seekbar1);
        seekBarView1.setDuration(1000);
        seekBarView1.addIndicatorChangeListener(listener);

        RangeSeekBarView seekBarView2 = findViewById(R.id.seekbar2);
        seekBarView2.setDuration(1000);
        seekBarView2.addIndicatorChangeListener(listener);

        RangeSeekBarView seekBarView3 = findViewById(R.id.seekbar3);
        seekBarView3.setDuration(1000);
        seekBarView3.addIndicatorChangeListener(listener);

        RangeSeekBarView seekBarView4 = findViewById(R.id.seekbar4);
        seekBarView4.setDuration(1000);
        seekBarView4.addIndicatorChangeListener(listener);

        RangeSeekBarView seekBarView5 = findViewById(R.id.seekbar5);
        seekBarView5.setDuration(1000);
        seekBarView5.addIndicatorChangeListener(listener);

        RangeSeekBarView seekBarView6 = findViewById(R.id.seekbar6);
        seekBarView6.setDuration(1000);
        seekBarView6.addIndicatorChangeListener(listener);

        findViewById(R.id.reset).setOnClickListener(v -> {
            seekBarView1.resetState(0, 1000, true);
            seekBarView2.resetState(0, 1000, true);
            seekBarView3.resetState(0, 1000, true);
            seekBarView4.resetState(0, 1000, true);
            seekBarView5.resetState(100, 400, true);
            seekBarView6.resetState(0, 1000, true);
        });
    }
}
