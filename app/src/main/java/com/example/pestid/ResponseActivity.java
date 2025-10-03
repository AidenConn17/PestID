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
    String[] values;
    Intent launchedIntent;

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
        name2 = findViewById(R.id.name2);
        confidence1 = findViewById(R.id.confidence1);
        confidence2 = findViewById(R.id.confidence2);
        danger1 = findViewById(R.id.danger1);
        danger2 = findViewById(R.id.danger2);
        role1 = findViewById(R.id.role1);
        role2 = findViewById(R.id.role2);

        launchedIntent = getIntent();

        values = launchedIntent.getStringArrayExtra("VALUES");

        assert values != null;
        if(values[0].equals("No confident identifications")){
            id1.setText("");
            id2.setText("");
            name1.setText("");
            name2.setText("");
            confidence1.setText("");
            confidence2.setText("");
            danger1.setText("");
            danger2.setText("");
            role1.setText("");
            role2.setText("");
        } else {
            id1.setText(R.string.id_1);
            if(values[4] != null)
                id2.setText(R.string.id_2);
            else
                id2.setText("");
            name1.setText(values[0]);
            confidence1.setText(values[1]);
            danger1.setText(values[2]);
            role1.setText(values[3]);

            name2.setText(values[4]);
            confidence2.setText(values[5]);
            danger2.setText(values[6]);
            role2.setText(values[7]);
        }

        backBtn.setOnClickListener(v ->{
            Intent intent = new Intent(ResponseActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }
}