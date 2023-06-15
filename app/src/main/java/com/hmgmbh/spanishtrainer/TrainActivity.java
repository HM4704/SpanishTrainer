package com.hmgmbh.spanishtrainer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.hmgmbh.spanishtrainer.ui.train.TrainData;
import com.hmgmbh.spanishtrainer.ui.train.TrainFragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Random;

public class TrainActivity extends AppCompatActivity {

    private ArrayList<TrainData> trainDataList = new ArrayList<TrainData>();
    Button switchToMainActivity;

    private static final ArrayList<SpeechService.TextItem> sSamplePlaylist = new ArrayList<SpeechService.TextItem>(4);
    static{
        sSamplePlaylist.add(new SpeechService.TextItem("G:Wie sagt man '??' auf spanisch?", "S:Como se dice '???' en espanol?"));
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.train_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, TrainFragment.newInstance())
                    .commitNow();
        }
        switchToMainActivity = findViewById(R.id.cancel);
        switchToMainActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchActivities();
            }
        });
        readData();
        fillAudioList();
        startService(SpeechService.createPlaylistIntent(this, sSamplePlaylist));
    }

    private void switchActivities() {
        Intent serviceIntent = new Intent(this, SpeechService.class);
        if (stopService(serviceIntent) == false) {
            Log.e("TTS", "stopService failed");
        }
        Intent switchActivityIntent = new Intent(this, MainActivity.class);
        startActivity(switchActivityIntent);
    }

    private void readData() {
        InputStream is = getResources().openRawResource(R.raw.data);

        BufferedReader reader;

        try {
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line = reader.readLine();

            int lCount = 0;
            while (line != null) {
                // read next line
                String[] texts = line.split(":");
                lCount++;
                if (texts.length == 2) {
                    TrainData d = new TrainData();
                    d.spanischText = texts[1];
                    d.germanText = texts[0];
                    String gerName = "raw/a_" + lCount + "_d";
                    String espName = "raw/a_" + lCount + "_e";
                    trainDataList.add(d);
                }
                line = reader.readLine();
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void fillAudioList() {
        Random rand = new Random();

        sSamplePlaylist.clear();
        LinkedList<Integer> shuffleList = new LinkedList<Integer>();
        for (int i = 0; i < trainDataList.size(); i++) {
            shuffleList.add(i);
        }

        for (int i = 0; i < trainDataList.size(); i++) {
            int num = rand.nextInt(shuffleList.size());
            //Log.e("TTS", "shuffle: num= " + num + "  size= " + shuffleList.size());
            int next = shuffleList.get(num);
            sSamplePlaylist.add(new SpeechService.TextItem("G:" + trainDataList.get(next).germanText,
                    "S:"+ trainDataList.get(next).spanischText));
            shuffleList.remove(num);
        }
//        for (TrainData td: trainDataList
//             ) {
//            sSamplePlaylist.add(new SpeechService.TextItem("G:" + td.germanText, "S:"+ td.spanischText));
//        }
    }
}