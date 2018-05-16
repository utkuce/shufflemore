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

public class MainActivity extends AppCompatActivity {

    RandomSongProvider randomSongProvider;
    BroadcastReceiver receiver;
    TrackRowAdapter trackRowAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        randomSongProvider = new RandomSongProvider();
        RecyclerView songList = findViewById(R.id.song_list);
        trackRowAdapter = new TrackRowAdapter(RandomSongProvider.chosenSongs);
        songList.setAdapter(trackRowAdapter);
        songList.setLayoutManager(new LinearLayoutManager(this));

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("shufflemore.playnext")) {
                    System.out.println("playnext received");
                    playChosen();
                } else if (action.equals("shufflemore.changenext")) {
                    System.out.println("changenext received");
                    changeNextSong(context);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("shufflemore.playnext");
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
        playChosen();
    }

    private void playChosen() {
        RandomSongProvider.Song s;
        if ((s = RandomSongProvider.chosenSongs.get(0)) != null) {
            playSong(s.uri);
        }
    }

    public void addButton(View v){
        RandomSongProvider.chosenSongs.add(randomSongProvider.getNewSong(this));
        trackRowAdapter.notifyItemInserted(RandomSongProvider.chosenSongs.size()-1);
    }

    public void playSong(String uri) {

        RandomSongProvider.currentSongUri = uri;
        startActivity(new Intent(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData(Uri.parse(uri)));
        randomSongProvider.addToHistory(this, uri);

        RandomSongProvider.chosenSongs.remove(0);
        RandomSongProvider.chosenSongs.add(randomSongProvider.getNewSong(this));
        trackRowAdapter.notifyDataSetChanged();

        RandomSongProvider.Song newSong = RandomSongProvider.chosenSongs.get(0);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(0, PlayBackReceiverService.getNotification(this)
                        .setContentTitle(newSong.name)
                        .setContentText(newSong.artist)
                        .setLargeIcon(newSong.cover)
                        .build());

    }

    public void changeNextSong(final Context context) {

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
