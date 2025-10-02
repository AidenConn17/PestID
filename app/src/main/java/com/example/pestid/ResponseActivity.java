package com.example.pestid;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ResponseActivity extends AppCompatActivity {
    Button backBtn;
    TextView id1;
    TextView id2;
    TextView name1;
    TextView name2;
    TextView confidence1;
    TextView confidence2;
    TextView danger1;
    TextView danger2;
    TextView role1;
    TextView role2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_response);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        backBtn = findViewById(R.id.back_button);
        id1 = findViewById(R.id.id1);
        id2 = findViewById(R.id.id2);
        name1 = findViewById(R.id.name1);
        backBtn.setOnClickListener(v ->{
            Intent intent = new Intent(ResponseActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }
}