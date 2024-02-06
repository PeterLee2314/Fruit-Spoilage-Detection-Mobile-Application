package com.example.tensorflow_fruit_image_classification_java;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isFirstTime()){
                    Log.d("All Log", ("First time opened"));
                    startActivity(new Intent(SplashActivity.this, GuidelineActivity.class));
                }else{
                    Log.d("All Log", ("Not first time opened"));
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                }
                finish();
            }
        }, 3000);


        TextView slogan1TextView = findViewById(R.id.fresh_fruit);
        TextView slogan2TextView = findViewById(R.id.sickness_go);

        AlphaAnimation animation1 = new AlphaAnimation(0.0f, 1.0f);
        animation1.setDuration(3000);
        animation1.setFillAfter(true);


        slogan1TextView.setAnimation(animation1);
        slogan2TextView.setAnimation(animation1);

    }

    private boolean isFirstTime()
    {
        //Shared Preferences for first launch
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean isFirstLaunch = preferences.getBoolean("isFirstLaunch", false);
        if (!isFirstLaunch) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isFirstLaunch", true);
            editor.commit();
        }
        return !isFirstLaunch;
    }
}
