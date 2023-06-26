package com.hmgmbh.spanishtrainer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.provider.SyncStateContract;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;


public class SpeechService extends Service implements TextToSpeech.OnUtteranceCompletedListener {

    public static final String CHANNEL_ID = "myMediaPlayerServiceChannel";
    private static final String TAG = SpeechService.class.getSimpleName();

    private static final String ACTION_START = TAG + ".ACTION_START";
    private static final String ACTION_QUIT  = TAG + ".ACTION_QUIT";

    private static final String ACTION_PLAY = TAG + ".ACTION_PLAY";

    private static final String EXTRA_PLAYLIST = "extraPlaylist";

    private boolean mIsReady     = false;

    private ArrayList<TextItem> mPlaylist;
    private TextItem mCurrent;

    private PowerManager.WakeLock wakeLock = null;
    private TextToSpeech mTTS;
    private Locale locSpanish = new Locale("spa", "SPA");
    private Locale locGerman = new Locale("ger", "GER");
    private Queue<String> speachQueue = new LinkedList<>();
    private Random rand = new Random();

    private static final MutableLiveData<String> mGermanText  = new MutableLiveData<String>("");
    private Integer mCurrentData = 0;
    private int mWaitTime;
    private int mNumRepeats;


    public SpeechService() {
    }

    public static LiveData<String> getGermanText() {
        return mGermanText;
    }

    public static Intent createSinglePlayIntent(Context context, TextItem item)
    {
        ArrayList<TextItem> playlist = new ArrayList<TextItem>(1);
        playlist.add(item);
        return createPlaylistIntent(context, playlist);
    }

    public static Intent createPlaylistIntent(Context context, ArrayList<TextItem> playlist)
    {
        return new Intent(context, SpeechService.class).setAction(ACTION_START)
                .putParcelableArrayListExtra(EXTRA_PLAYLIST, playlist);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_QUIT);
        intentFilter.addAction(ACTION_PLAY);

        mTTS = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
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
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });
        mTTS.setOnUtteranceCompletedListener(this);
        readPrefs();
    }

    private void readPrefs() {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getResources().getString(R.string.prefs_name),
                Context.MODE_PRIVATE);
        mWaitTime = sharedPref.getInt(getResources().getString(R.string.wait_time), 2000);
        mNumRepeats = sharedPref.getInt(getResources().getString(R.string.num_repeats), 2);
    }

    private void startTrainCycle() {

        mGermanText.postValue(mPlaylist.get(mCurrentData).mGerText.split(":")[1]);

        speachQueue.add(mPlaylist.get(mCurrentData).mGerText);
        for (int i = 0; i < mNumRepeats; i++) {
            speachQueue.add(mPlaylist.get(mCurrentData).mSpaText);
        }

        mCurrentData++;
        if (mCurrentData == mPlaylist.size()) {
            mCurrentData = 0;
        }

        //mCurrentData = rand.nextInt(1);
        speak();
    }

    private void speak() {

        if (speachQueue.isEmpty()) {
            startTrainCycle();
            return;
        }
        if (!mTTS.isSpeaking()) {
            String[] s = speachQueue.remove().split(":");
            String text = s[1];
            String lang = s[0];

            int result = 0;
            if (lang.charAt(0) == 'G') {
                result = mTTS.setLanguage(Locale.GERMAN);
            } else {
                result = mTTS.setLanguage(locSpanish);
                mTTS.setSpeechRate(0.75f);
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
    public void onUtteranceCompleted(String uttId) {
        Log.e("TTS", "speak finished");
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                speak();
            }
        }, mWaitTime);
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "Destroying the foreground service");
        super.onDestroy();
        if (wakeLock != null) {
            wakeLock.release();
        }
        onCommandQuit();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        final String action = intent.getAction();

        Log.d(TAG, "onStartCommand: " + intent.getAction());

        if(ACTION_START.equals(action)){
            ArrayList<TextItem> playlist = intent.getParcelableArrayListExtra(EXTRA_PLAYLIST);
            onCommandStart(playlist);
            return START_STICKY;
        }

        stopSelf();
        return 0;
    }

    private void onCommandStart(ArrayList<TextItem> playlist)
    {
        mPlaylist = playlist;
        mCurrent = null;

        createNotificationChannel();

        Intent notificationIntent =  new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Example Service")
                .setContentText("test")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1,notification);

        wakeLock = acquire(this, PowerManager.PARTIAL_WAKE_LOCK, 10000, "SpeechService::lock");

        mIsReady = true;
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startTrainCycle();;
            }
        }, mWaitTime);
    }

    private void createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, "Example Service Channel",NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    private PendingIntent getButtonPendingIntent(String action)
    {
        Intent intent = new Intent(action);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void onCommandQuit()
    {
        mIsReady = false;
        speachQueue.clear();
        mTTS.shutdown();
        stopForeground(true);
        stopSelf();
    }

    private PowerManager.WakeLock acquire(Context context, int lockType, long timeout, String tag) {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(lockType, tag);

            wakeLock.acquire(timeout);
            Log.d(TAG, "Acquired wakelock with tag: " + tag);

            return wakeLock;
        } catch (Exception e) {
            Log.w(TAG, "Failed to acquire wakelock with tag: " + tag, e);
            return null;
        }
    }

    public static class TextItem
            implements Parcelable
    {
        private String mGerText;
        private String mSpaText;

        public TextItem(String gerText, String spaText)
        {
            mGerText = gerText;
            mSpaText = spaText;
        }

        public TextItem(Parcel source)
        {
            mGerText = source.readString();
            mSpaText = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeString(mGerText);
            dest.writeString(mSpaText);
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        public static final Parcelable.Creator<TextItem> CREATOR = new Parcelable.Creator<TextItem>()
        {
            @Override
            public TextItem createFromParcel(Parcel source)
            {
                return new TextItem(source);
            }

            @Override
            public TextItem[] newArray(int size)
            {
                return new TextItem[size];
            }
        };
    }
}