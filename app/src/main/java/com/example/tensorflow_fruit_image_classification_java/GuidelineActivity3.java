package com.example.tensorflow_fruit_image_classification_java;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GuidelineActivity3 extends AppCompatActivity {

    Button finish, back;
    TextView skip;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guideline3);

        finish = findViewById(R.id.fininshButton);

        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToMainActivity(view);
            }
        });

        back = findViewById(R.id.goPreviousButton);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goPreviousButton(view);
            }
        });


        skip = findViewById(R.id.skip);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                skip(view);
            }
        });
    }
    private void goPreviousButton(View view){
        Intent intent = new Intent(this, GuidelineActivity2.class);
        view.getContext().startActivity(intent);
    }

    private void goToMainActivity(View view){
        Intent intent = new Intent(this, MainActivity.class);
        view.getContext().startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

    }

    private void skip(View view){
        Intent intent = new Intent(this, MainActivity.class);
        view.getContext().startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
    public void finish(){
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}