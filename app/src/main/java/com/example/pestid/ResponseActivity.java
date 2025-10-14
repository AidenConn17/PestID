package com.example.pestid;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.pestid.databinding.ActivityResponseBinding;

public class ResponseActivity extends AppCompatActivity {

    Button backBtn;

    TextView id1;
    TextView name1;
    TextView confidence1;
    TextView danger1;
    TextView role1;
    ImageView image1;

    TextView id2;
    TextView name2;
    TextView confidence2;
    TextView danger2;
    TextView role2;
    ImageView image2;

    TextView noConfidentId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityResponseBinding binding = ActivityResponseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        backBtn = findViewById(R.id.back_btn);

        id1 = findViewById(R.id.id1);
        name1 = findViewById(R.id.name1);
        confidence1 = findViewById(R.id.confidence1);
        danger1 = findViewById(R.id.danger1);
        role1 = findViewById(R.id.role1);
        image1 = findViewById(R.id.image1);

        id2 = findViewById(R.id.id2);
        name2 = findViewById(R.id.name2);
        confidence2 = findViewById(R.id.confidence2);
        danger2 = findViewById(R.id.danger2);
        role2 = findViewById(R.id.role2);
        image2 = findViewById(R.id.image2);

        noConfidentId = findViewById(R.id.noConfidentId);

        Intent sentIntent = getIntent();
        String[] sentValues = sentIntent.getStringArrayExtra("VALUES");
        try {
            assert sentValues != null;
            if (sentValues[0].equals("No confident identifications")) {
                id1.setText("");
                name1.setText("");
                confidence1.setText("");
                danger1.setText("");
                role1.setText("");
                image1.setImageAlpha(0);

                id2.setText("");
                name2.setText("");
                confidence2.setText("");
                danger2.setText("");
                role2.setText("");
                image2.setImageAlpha(0);
            } else if (sentValues[5] != null) {
                noConfidentId.setText("");

                name1.setText(sentValues[0]);
                confidence1.setText(sentValues[1]);
                danger1.setText(sentValues[2]);
                role1.setText(sentValues[3]);
                Glide.with(this).load(sentValues[4]).into(image1);


                name2.setText(sentValues[5]);
                confidence2.setText(sentValues[6]);
                danger2.setText(sentValues[7]);
                role2.setText(sentValues[8]);
                Glide.with(this).load(sentValues[9]).into(image2);
            } else {
                noConfidentId.setText("");

                name1.setText(sentValues[0]);
                confidence1.setText(sentValues[1]);
                danger1.setText(sentValues[2]);
                role1.setText(sentValues[3]);
                Glide.with(this).load(sentValues[4]).into(image1);

                id2.setText("");
                name2.setText("");
                confidence2.setText("");
                danger2.setText("");
                role2.setText("");
                image2.setImageAlpha(0);
            }
        } catch (NullPointerException e){
            Log.e("Response Activity", "Sent Values is null");
        }
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ResponseActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }
}