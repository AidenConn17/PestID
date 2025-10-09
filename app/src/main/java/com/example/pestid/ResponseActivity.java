package com.example.pestid;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pestid.databinding.ActivityResponseBinding;

public class ResponseActivity extends AppCompatActivity {

    Button backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityResponseBinding binding = ActivityResponseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        backBtn = findViewById(R.id.back_btn);

        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ResponseActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }
}