package com.example.aimentor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null){
            String data = bundle.getString("MY_DATA", "");
            int id = bundle.getInt("MY_ID", 0);
            Toast.makeText(MainActivity.this, data + " - " + id, Toast.LENGTH_SHORT).show();
        }
    }
}