package com.kevin.lib.quickbledemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.kevin.lib.quickble.Ble;
import com.kevin.lib.quickble.BleRequestHandler;

public class MainActivity extends AppCompatActivity {
private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e(TAG, "onCreate: " );
        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BleRequestHandler handler = Ble.handler();
                Log.e(TAG, "onClick: "+handler );
            }
        });
    }

}
