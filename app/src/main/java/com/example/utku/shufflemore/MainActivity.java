package com.example.utku.shufflemore;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    RandomSongProvider randomSongProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        randomSongProvider = new RandomSongProvider();
        setContentView(R.layout.activity_main);
    }

    public void playSong(String url) {

        startActivity(new Intent(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData(Uri.parse(url)));

        randomSongProvider.addToHistory(this, url);
    }
}
