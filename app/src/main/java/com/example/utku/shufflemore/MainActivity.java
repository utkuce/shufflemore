package com.example.utku.shufflemore;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    RandomSongProvider randomSongProvider;
    AppData appData;

    Playlist spotifyPlaylist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appData = new AppData(this);

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        stopService(new Intent(this, PlayBackReceiverService.class));
    }

    public void playButton(View v) {

        spotifyPlaylist.startPlayback();
    }

    @SuppressLint("StaticFieldLeak")
    public void addButton(View v){

        final Context context = this;
        new AsyncTask<Void , Void, RandomSongProvider.Song>() {

            @Override
            protected RandomSongProvider.Song doInBackground (Void... v) {

                RandomSongProvider.Song newSong = randomSongProvider.getNewSong(context);
                spotifyPlaylist.addTrack(newSong.uri);
                return newSong;
            }

            @Override
            protected void onPostExecute(RandomSongProvider.Song newSong){

                RandomSongProvider.chosenSongs.add(newSong);
            }

        }.execute();
    }

    public void changeNextSong() {

        final Context context = this;
        new Thread(new Runnable() {
            @Override
            public void run() {

                spotifyPlaylist.removeTrack(RandomSongProvider.chosenSongs.get(1).uri);

                final RandomSongProvider.Song newSong = randomSongProvider.getNewSong(context);
                RandomSongProvider.chosenSongs.set(1, newSong);
                spotifyPlaylist.addTrack(newSong.uri);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (newSong != null) {

                            setNextSongUI(RandomSongProvider.chosenSongs.get(1));

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

    void setCurrentSongUI(RandomSongProvider.Song song) {

        ((ImageView)findViewById(R.id.cover_art_current)).setImageBitmap(song.cover);
        ((TextView)findViewById(R.id.track_info_current)).setText(String.format("%s\n%s", song.name, song.artist));

    }

    void setNextSongUI(RandomSongProvider.Song song) {

        ((ImageView)findViewById(R.id.cover_art_next)).setImageBitmap(song.cover);
        ((TextView)findViewById(R.id.track_info_next)).setText(String.format("%s\n%s", song.name, song.artist));

    }

    public void shuffleButton(View view) {
        changeNextSong();
    }
}
