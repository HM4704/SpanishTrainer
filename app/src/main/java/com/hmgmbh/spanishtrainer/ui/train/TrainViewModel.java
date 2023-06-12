package com.hmgmbh.spanishtrainer.ui.train;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.hmgmbh.spanishtrainer.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

public class TrainViewModel extends ViewModel implements TextToSpeech.OnUtteranceCompletedListener {
    private ArrayList<TrainData> trainDataList = new ArrayList<TrainData>();
    private Context mContext = null;
    private final MutableLiveData<String> mGermanText  = new MutableLiveData<String>("");
    private Integer mCurrentData = 0;

    private TextToSpeech mTTS;
    private Locale locSpanish = new Locale("spa", "SPA");
    private Locale locGerman = new Locale("ger", "GER");
    private Queue<String> textQueue = new LinkedList<>();

    public void readData(final Context context) {
        mContext = context;
        InputStream is = context.getResources().openRawResource(R.raw.data);

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
                    d.germanAudio = mContext.getResources().getIdentifier(gerName, null, mContext.getPackageName());
                    if (d.germanAudio == 0) {
                        d.germanAudio = mContext.getResources().getIdentifier("no_audio", null, mContext.getPackageName());
                    }
                    d.spanishAudio = mContext.getResources().getIdentifier(espName, null, mContext.getPackageName());
                    if (d.spanishAudio == 0) {
                        d.spanishAudio = mContext.getResources().getIdentifier("no_audio", null, mContext.getPackageName());
                    }
                    trainDataList.add(d);
                }
                line = reader.readLine();
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mTTS = new TextToSpeech(mContext, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    Locale locSpanish = new Locale("spa", "SPA");
                    int result = mTTS.setLanguage(locSpanish);
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    }
                    result = mTTS.setLanguage(Locale.GERMANY);
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startTrainCycle();
                            }
                        }, 1000);
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });
        mTTS.setOnUtteranceCompletedListener(this);
    }

    public LiveData<String> getGermanText() {
        return mGermanText;
    }

    private void startTrainCycle() {
        mGermanText.postValue(trainDataList.get(mCurrentData).germanText);

        textQueue.add("G:"+trainDataList.get(mCurrentData).germanText);
        textQueue.add("S:"+trainDataList.get(mCurrentData).spanischText);
        textQueue.add("S:"+trainDataList.get(mCurrentData).spanischText);
        speak();

        mCurrentData++;
        if (mCurrentData == trainDataList.size()) {
            mCurrentData = 0;
        }
    }

    private void speak() {

        if (textQueue.isEmpty()) {
            startTrainCycle();
            return;
        }
        if (!mTTS.isSpeaking()) {
            String[] s = textQueue.remove().split(":");
            String text = s[1];
            String lang = s[0];

            int result = 0;
            if (lang.charAt(0) == 'G') {
                result = mTTS.setLanguage(Locale.GERMAN);
            } else {
                result = mTTS.setLanguage(locSpanish);
            }
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language not supported");
            }

//        mTTS.setPitch(pitch);
//        mTTS.setSpeechRate(speed);
            mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null, "myId");
        }
    }

    public void stop() {
        textQueue.clear();
        mTTS.shutdown();
    }

    public void onUtteranceCompleted(String uttId) {
        Log.e("TTS", "speak finished");
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                speak();
            }
        }, 2000);
    }
}