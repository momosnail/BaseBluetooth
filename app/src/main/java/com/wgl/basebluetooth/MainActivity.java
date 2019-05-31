package com.wgl.basebluetooth;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.widget.CompoundButton;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private SwitchCompat mSwitchBluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
    }

    private void initData() {
        mSwitchBluetooth.setOnCheckedChangeListener(mOnCheckedChangeListener);
    }

    private void initView() {
        mSwitchBluetooth = findViewById(R.id.switch_bluetooth);
    }

    CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                Log.e(TAG, "onCheckedChanged: "+isChecked );
            } else {
                Log.e(TAG, "onCheckedChanged: "+isChecked );
                Log.e(TAG, "onCheckedChanged: "+isChecked );

            }

        }
    };
}
