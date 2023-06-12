package com.hmgmbh.spanishtrainer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.hmgmbh.spanishtrainer.ui.main.MainFragment;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    Button switchToTrainActivity;
    private Button mButtonSpeak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow();
        }
        switchToTrainActivity = findViewById(R.id.start);
        switchToTrainActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchActivities();
            }
        });
    }

    private void switchActivities() {
        Intent switchActivityIntent = new Intent(this, TrainActivity.class);
        startActivity(switchActivityIntent);
    }
}