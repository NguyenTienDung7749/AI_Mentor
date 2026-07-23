package com.example.aimentor;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class DemoEventActivity extends AppCompatActivity {
    EditText edtText;
    CheckBox cbAgree;
    Button btnSubmit, btnAnswer;
    TextView tvData;
    RadioGroup radGAddress;
    RadioButton radHN, radBN, radTN, radPT;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_event);
        // find element by ID
        edtText = findViewById(R.id.edtText);
        cbAgree = findViewById(R.id.cbAgree);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvData = findViewById(R.id.tvTextData);
        btnAnswer = findViewById(R.id.btnAnswer);
        radGAddress = findViewById(R.id.radGAddress);
        radHN = findViewById(R.id.radHN);
        radBN = findViewById(R.id.radBN);
        radTN = findViewById(R.id.radTN);
        radPT = findViewById(R.id.radPT);

        // block EditText and Button
        edtText.setEnabled(false);
        btnSubmit.setEnabled(false);
        // bat su kien cho checkbox
        cbAgree.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton compoundButton, boolean isChecked) {
                if (isChecked){
                    // nguoi dung da tich chon
                    edtText.setEnabled(true);
                    btnSubmit.setEnabled(true);
                } else {
                    edtText.setEnabled(false);
                    btnSubmit.setEnabled(false);
                    edtText.setText(""); // xoa du lieu nhap vao EditText
                }
            }
        });
        // bat su kien cho button
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // lay du lieu nguoi dung nhap vao EditText
                String data = edtText.getText().toString().trim();
                Toast.makeText(DemoEventActivity.this, data, Toast.LENGTH_SHORT).show();
            }
        });
        // bat su kien cho EditText
        edtText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                String data = editable.toString().trim(); // du lieu nguoi dung nhap
                tvData.setText(data);
                int count = data.length();
                if (count > 10){
                    edtText.setEnabled(false);
                    cbAgree.setChecked(false);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
        });
        btnAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int selectedId = radGAddress.getCheckedRadioButtonId();
                RadioButton radio = (RadioButton) findViewById(selectedId);
                if (radio != null) {
                    // lay duoc dia chi ma nguoi dung da chon
                    String address = radio.getText().toString().trim();
                    Toast.makeText(DemoEventActivity.this, address, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DemoEventActivity.this, "Choose Address, please !", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
