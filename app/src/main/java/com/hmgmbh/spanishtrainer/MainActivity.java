package com.hmgmbh.spanishtrainer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;

import com.hmgmbh.spanishtrainer.ui.main.MainFragment;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button switchToTrainActivity;
    private NumberPicker mNPNumRepeat;
    private EditText mRepeatTime;
    private EditText mRepeatAfter;
    private SharedPreferences mSharedPref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow();
        }

        mRepeatTime = findViewById(R.id.editWaitTime);
        mRepeatAfter = findViewById(R.id.editRepeatAfter);
        mNPNumRepeat = findViewById(R.id.npNumRepeats);
        switchToTrainActivity = findViewById(R.id.start);
        mSharedPref = getApplicationContext().getSharedPreferences(getResources().getString(R.string.prefs_name),
                Context.MODE_PRIVATE);
        loadPrefs();
        switchToTrainActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePrefs();

                switchActivities();
            }
        });
    }

    private void loadPrefs() {
        int waitTime = mSharedPref.getInt(getResources().getString(R.string.wait_time), 2000);
        int numRepeats = mSharedPref.getInt(getResources().getString(R.string.num_repeats), 2);
        int repeatAfter = mSharedPref.getInt(getResources().getString(R.string.repeat_after), 10);

        mRepeatTime.setText(String.format("%s", waitTime));
        mRepeatAfter.setText(String.format("%s", repeatAfter));
        mNPNumRepeat.setMinValue(1);
        mNPNumRepeat.setMaxValue(4);
        mNPNumRepeat.setValue(numRepeats);
    }

    private void savePrefs() {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putInt(getResources().getString(R.string.wait_time), Integer.parseInt(String.valueOf(mRepeatTime.getText())));
        editor.putInt(getResources().getString(R.string.repeat_after), Integer.parseInt(String.valueOf(mRepeatAfter.getText())));
        editor.putInt(getResources().getString(R.string.num_repeats), mNPNumRepeat.getValue());
        editor.apply();
    }

    private void switchActivities() {
        Intent switchActivityIntent = new Intent(this, TrainActivity.class);
        startActivity(switchActivityIntent);
    }
}