package com.example.utku.shufflemore;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.client.ResponseHandler;

public class MainActivity extends AppCompatActivity {

    RandomSongProvider randomSongProvider;
    AppData appData;

    Playlist spotifyPlaylist;

    private BroadcastReceiver receiver;
    TrackRowAdapter trackRowAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appData = new AppData(this);
        randomSongProvider = new RandomSongProvider(appData);

        RecyclerView songList = findViewById(R.id.song_list);
        trackRowAdapter = new TrackRowAdapter(RandomSongProvider.chosenSongs);
        songList.setAdapter(trackRowAdapter);
        songList.setLayoutManager(new LinearLayoutManager(this));

        receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("shufflemore.changenext")) {
                    System.out.println("changenext received");
                    changeNextSong();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("shufflemore.changenext");
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        unregisterReceiver(receiver);
        stopService(new Intent(this, PlayBackReceiverService.class));
    }

    public void playButton(View v) {

        spotifyPlaylist.startPlayback(this, appData);
    }

    public void addButton(View v){

        final RandomSongProvider.Song newSong = randomSongProvider.getNewSong(this);
        RandomSongProvider.chosenSongs.add(newSong);
        trackRowAdapter.notifyItemInserted(RandomSongProvider.chosenSongs.size()-1);

        final Context context = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                spotifyPlaylist.addTrack(context, appData, newSong.uri);
            }
        }).start();

    }

    public void changeNextSong() {

        final Context context = this;
        new Thread(new Runnable() {
            @Override
            public void run() {

                final RandomSongProvider.Song newSong = randomSongProvider.getNewSong(context);
                if (RandomSongProvider.chosenSongs.isEmpty())
                    RandomSongProvider.chosenSongs.add(newSong);
                else
                    RandomSongProvider.chosenSongs.set(0, newSong);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        trackRowAdapter.notifyDataSetChanged();

                        if (newSong != null) {

                            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                                    .notify(0, PlayBackReceiverService.getNotification(context)
                                            .setContentTitle(newSong.name)
                                            .setContentText(newSong.artist)
                                            .setLargeIcon(newSong.cover)
                                            .build());
                        }
                    }
                });
            }
        }).start();
    }
}
