package com.steps4news.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;

import com.google.gson.stream.JsonReader;

import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.steps4news.MainActivity;
import com.steps4news.R;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class PlayNewsService extends Service {
    public static boolean stop = false;
    private boolean firstTime = true;
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    String searchTerm; // the term that the news is relevent to
    public TextToSpeech t;
    private String NEWS_API_KEY = "477014080f12442eba72dccf28a4f4a8";

    public PlayNewsService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        stop = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        searchTerm = intent.getStringExtra("searchTerm");

        t = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t.setLanguage(Locale.US);
                }
            }
        });
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Steps 4 News")
                .setContentText("Playing news for " + searchTerm)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);


        Log.e("PlayNewsService", "" + t.isSpeaking());

        fetchNewsAndTalk();

        // fetch news and talk whenever we're done speaking
        t.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
            @Override
            public void onUtteranceCompleted(String utteranceId) {
                Log.e("PlayNewsService", "UTTERANCE FINISHED!!!");
                fetchNewsAndTalk();
            }
        });

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        return null;
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        stop = true;
        t.stop();

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }



    void fetchNewsAndTalk() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.e("PlayNewsService", "API CALLED!!! with search term"+searchTerm);

                    URL url = new URL("https://newsapi.org/v2/everything?q=" + searchTerm + " &sortBy=publishedAt&language=en&apiKey=" + NEWS_API_KEY);
                    HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                    JsonReader jr = new JsonReader(new InputStreamReader((InputStream) urlConnection.getContent()));
                    jr.setLenient(true);

                    JsonParser parser = new JsonParser();
                    JsonObject obj = parser.parse(jr).getAsJsonObject();
                    JsonArray articleArrayObj = obj.get("articles").getAsJsonArray();

                    for (JsonElement element : articleArrayObj) {
                        Log.e("PlayNewsService", element.getAsJsonObject().get("title").toString());
                        Log.e("PlayNewsService", element.getAsJsonObject().get("description").toString());
                        String articleTitle = element.getAsJsonObject().get("title").getAsString();
                        String articleDescription = element.getAsJsonObject().get("description").getAsString();

                        String sendToTextToSpeech = articleTitle + "." + articleDescription;

                        t.speak(sendToTextToSpeech, TextToSpeech.QUEUE_ADD, null);
                    }
                } catch (IOException iox) {
                    Log.e("PlayNewsService", iox.getMessage());
                }
            }
        }).start();
    }
}
