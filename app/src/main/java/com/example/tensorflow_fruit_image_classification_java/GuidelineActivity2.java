package com.example.tensorflow_fruit_image_classification_java;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class GuidelineActivity2 extends AppCompatActivity {

    Button next, back;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guideline2);

        next = findViewById(R.id.button1);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToGuideline3(view);
            }
        });

        back = findViewById(R.id.goPreviousButton);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goPreviousButton(view);
            }
        });

    }
    private void goPreviousButton(View view){
        Intent intent = new Intent(this, GuidelineActivity.class);
        view.getContext().startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
    private void goToGuideline3(View view){
        Intent intent = new Intent(this, GuidelineActivity3.class);
        view.getContext().startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public void finish(){
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}