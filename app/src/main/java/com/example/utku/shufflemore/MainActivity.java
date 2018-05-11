package com.example.utku.shufflemore;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    RandomSongProvider randomSongProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        randomSongProvider = new RandomSongProvider();
        setContentView(R.layout.activity_main);
    }

    public void playButton(View v) {
        RandomSongProvider.Song s;
        if ((s = RandomSongProvider.chosenSong) != null) {
            playSong(s.url);
        }
    }

    public void refreshButton(View v){
        setNextSong(this);
    }

    public void playSong(String url) {

        startActivity(new Intent(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData(Uri.parse(url)));

        randomSongProvider.addToHistory(this, url);
        setNextSong(this);
    }

    public void setNextSong(final Context context) {

        final ProgressBar pb = findViewById(R.id.progressBar4);
        final LinearLayout ly = findViewById(R.id.card_inner);
        pb.setVisibility(View.VISIBLE);
        ly.setVisibility(View.INVISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final RandomSongProvider.Song newSong = randomSongProvider.getNewSong(context);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        pb.setVisibility(View.GONE);
                        ly.setVisibility(View.VISIBLE);

                        if (newSong != null) {
                            ((TextView)findViewById(R.id.song_name)).setText(
                                    String.format("%s\n%s", newSong.name, newSong.artist));

                            ImageView coverArt = findViewById(R.id.cover_art);
                            coverArt.setMaxWidth(coverArt.getMeasuredHeight());
                            coverArt.setImageBitmap(newSong.cover);
                        }
                    }
                });
            }
        }).start();
    }
}
