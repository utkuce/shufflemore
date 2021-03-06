package com.example.utku.shufflemore;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

        findViewById(R.id.gui).setVisibility(View.GONE);
        findViewById(R.id.splash).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    public void playButton(View v) {

        spotifyPlaylist.mSpotifyAppRemote.getPlayerApi()
                .getPlayerState().setResultCallback(playerState -> {

            if (playerState.isPaused) {

                // if chosen song is playing
                if (playerState.playbackPosition > 1000
                        && playerState.track.uri.equals(RandomSongProvider.chosenSongs.get(0).uri)) {

                    spotifyPlaylist.mSpotifyAppRemote.getPlayerApi().resume();
                    ((Button) findViewById(R.id.play_button)).setText("pause");
                    Log.v("sm_MAIN", "Resume button pressed");

                    return;
                }

            } else {

                // if chosen song is playing
                if (playerState.playbackPosition > 1000
                        && playerState.track.uri.equals(RandomSongProvider.chosenSongs.get(0).uri)) {

                    spotifyPlaylist.mSpotifyAppRemote.getPlayerApi().pause();
                    ((Button)findViewById(R.id.play_button)).setText("play");
                    Log.v("sm_MAIN", "Pause button pressed");

                    return;
                }
            }

            spotifyPlaylist.startPlayback();
            ((Button) findViewById(R.id.play_button)).setText("pause");
            Log.v("sm_MAIN", "Start button pressed");

        });
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

        findViewById(R.id.nextUpCard).setVisibility(View.INVISIBLE);
        findViewById(R.id.progressBar2).setVisibility(View.VISIBLE);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(RemoteService.notificationId, RemoteService.getNotification(this)
                        .setContentTitle("Please wait...")
                        .setContentText("Getting new song")
                        .build());

        if (spotifyPlaylist.chosenButSkipped.equals("")) {
            spotifyPlaylist.chosenButSkipped = RandomSongProvider.chosenSongs.get(1).uri;
        }

        final Context context = this;
        new Thread(() -> {

            spotifyPlaylist.removeTrack(RandomSongProvider.chosenSongs.get(1).uri);

            final RandomSongProvider.Song newSong = randomSongProvider.getNewSong(context);
            RandomSongProvider.chosenSongs.set(1, newSong);
            spotifyPlaylist.addTrack(newSong.uri);

            runOnUiThread(() -> {

                setNextSongUI(RandomSongProvider.chosenSongs.get(1));
                updateNotification();

                findViewById(R.id.nextUpCard).setVisibility(View.VISIBLE);
                findViewById(R.id.progressBar2).setVisibility(View.GONE);

            });
        }).start();
    }

    void updateUI(RandomSongProvider.Song current, RandomSongProvider.Song nextUp) {

        updateNotification();
        setCurrentSongUI(current);
        setNextSongUI(nextUp);
    }

    void updateNotification() {

        RandomSongProvider.Song nextUp = RandomSongProvider.chosenSongs.get(1);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(RemoteService.notificationId, RemoteService.getNotification(this)
                        .setContentTitle(nextUp.name)
                        .setContentText(nextUp.artist)
                        .setLargeIcon(nextUp.cover)
                        .build());
    }

    void setCurrentSongUI(RandomSongProvider.Song song) {

        Log.v("sm_MAIN", "Updating current song UI");
        ((ImageView)findViewById(R.id.cover_art_current)).setImageBitmap(song.cover);
        ((TextView)findViewById(R.id.track_info_current)).setText(String.format("%s\n%s", song.name, song.artist));

    }

    void setNextSongUI(RandomSongProvider.Song song) {

        Log.v("sm_MAIN", "Updating next song UI");
        ((ImageView)findViewById(R.id.cover_art_next)).setImageBitmap(song.cover);
        ((TextView)findViewById(R.id.track_info_next)).setText(String.format("%s\n%s", song.name, song.artist));

    }

    public void shuffleButton(View view) {
        changeNextSong();
    }

    public void goToSpotify(View view) {

        Intent launcher = new Intent( Intent.ACTION_VIEW, Uri.parse("spotify:open") );
        startActivity(launcher);
    }

    public void skipButton(View view) {

        spotifyPlaylist.playNext();
    }
}
