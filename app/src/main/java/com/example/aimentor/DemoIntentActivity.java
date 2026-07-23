package com.example.aimentor;

import static android.Manifest.permission.CALL_PHONE;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class DemoIntentActivity extends AppCompatActivity {
    EditText edtData, edtPhone, edtUrl;
    Button btnActivity, btnCallPhone, btnUrl;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_intent);
        edtData = findViewById(R.id.edtData);
        btnActivity = findViewById(R.id.btnOtherActivity);
        edtPhone = findViewById(R.id.edtPhone);
        btnCallPhone = findViewById(R.id.btnCallPhone);
        edtUrl = findViewById(R.id.edtUrl);
        btnUrl = findViewById(R.id.btnURL);
        // bat su kien
        btnActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String data = edtData.getText().toString().trim();
                if (TextUtils.isEmpty(data)){
                    edtData.setError("Data can not blank");
                    return;
                }
                // gui du lieu sang mot man hinh khac
                Intent intent = new Intent(DemoIntentActivity.this, MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("MY_DATA", data);
                bundle.putInt("MY_ID", 101);
                intent.putExtras(bundle); // gui du lieu sang
                startActivity(intent); // chuyen sang man hinh khac
            }
        });
        btnCallPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String phone = edtPhone.getText().toString().trim();
                if (TextUtils.isEmpty(phone)){
                    edtPhone.setError("Phone not empty");
                    return;
                }
                // goi den so dien thoai
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:"+phone));
                // kiem tra xem co duoc quyen thuc thi viec call
                if(ContextCompat.checkSelfPermission(getApplicationContext(), CALL_PHONE) == PackageManager.PERMISSION_GRANTED){
                    startActivity(intent);
                } else {
                    requestPermissions(new String[] { CALL_PHONE }, 1);
                }
            }
        });
        btnUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = edtUrl.getText().toString().trim();
                if (TextUtils.isEmpty(url)){
                    edtUrl.setError("URL not empty");
                    return;
                }
                // load website
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
        });

    }
}
