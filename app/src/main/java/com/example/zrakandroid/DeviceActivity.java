package com.example.zrakandroid;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DeviceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        Intent intent = this.getIntent();
        String device_name = intent.getStringExtra("device_name");
        TextView tv = findViewById(R.id.device_name);
        tv.setText(device_name);
    }
}
