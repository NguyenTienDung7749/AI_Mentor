package com.example.aimentor;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class TestLifeCycleActivity extends AppCompatActivity {
    private final String LogActivity = "LOG_ACTIVITY";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ham se tu dong chay khi co 1 activity dc kich hoat
        // thong thuong dung de load giao dien app(hien giao dien app) va xu ly cac logic cua app
        setContentView(R.layout.activity_life_cycle_first);
        Log.i(LogActivity, "*** onCreate is running ***");
        Button btnClickMe = findViewById(R.id.btnClickMe); // tim phan tu ngoai view
        // bat su kien
        btnClickMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // di chuyen sang Activity khac
                Intent intent = new Intent(TestLifeCycleActivity.this, TestSecondLifeCycleActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Toast.makeText(TestLifeCycleActivity.this, "Good bye, See you again !", Toast.LENGTH_SHORT).show();

        // ham nay se chay khi Activity bi huy
        Log.i(LogActivity, "Application Exited");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // ham nay se chay khi activity cu bi an di duoc kich hoat quay tro lai
        Log.i(LogActivity, "*** onRestart is running ***");
        // keo theo onStart, onResume se chay lai
    }

    @Override
    protected void onPause() {
        super.onPause();
        // ham nay se chay khi chuan bi co 1 Activity sap dc xuat hien
        Log.i(LogActivity, "*** onPause is running ***");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // ham nay se chay khi Activty hien thoi bi an di de nhuong cho cho Actvity moi xuat hien
        Log.i(LogActivity, "*** onStop is running ***");
    }

    @Override
    protected void onStart() {
        super.onStart();
        // ham nay se tu dong chay truoc khi Activity xuat hien len man hinh thiet bi
        Log.i(LogActivity, "*** onStart is running ***");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ham nay se chay ngay truoc khi nguoi co the tuong tac voi Activity
        Log.i(LogActivity, "*** onResume is running ***");
    }
}
