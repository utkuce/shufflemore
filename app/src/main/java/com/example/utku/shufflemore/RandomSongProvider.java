package com.example.utku.shufflemore;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.Html;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

public class RandomSongProvider
{
    private static int totalTracks = -1;

    public void addToHistory(Context context, String url)
    {
        Vector<String> history = AppData.getHistory(context);
        if (history != null) {
            history.add(url);
            while (history.size() > totalTracks * 0.5)
                history.removeElementAt(0);
            AppData.setHistory(history,context);
        }
        else
            Toast.makeText(context, "Cant read song history", Toast.LENGTH_LONG).show();
    }

    public class Song {
        String url, name, artist, album;
        Bitmap cover;
    }

    public Song getNewSong(Context context)
    {
        Song song = null;

        if ((totalTracks = (totalTracks == -1) ? getTotalTracks() : totalTracks) != -1)
        {
            Vector<String> history = AppData.getHistory(context);

            do {
                int offset = new Random(System.currentTimeMillis()).nextInt(totalTracks + 1);
                String response = spotifyRequest(offset);
                song = getSongProperties(response);

                if (song == null) {

                    Toast.makeText(context,
                            Html.fromHtml("<b>Error:</b> Can't get song properties"),
                            Toast.LENGTH_LONG).show();
                    return null;
                }

                if (history == null) {
                    Toast.makeText(context,
                            Html.fromHtml("<b>Warning:</b> Cant read song history"),
                            Toast.LENGTH_LONG).show();
                    break;
                }

            } while (history.contains(song.url) && song.url == null);
        }

        return song;
    }

    @SuppressLint("StaticFieldLeak")
    private String spotifyRequest(final int offset)
    {

        try {
            return new AsyncTask<Void , Void, String>()
            {
                @Override
                protected String doInBackground (Void... v)
                {
                    StringBuilder response = new StringBuilder();

                    try {
                        final URL url = new URL("https://api.spotify.com/v1/me/tracks?offset="
                                + Integer.toString(offset) + "&limit=1");

                        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                        conn.setRequestProperty("Accept", "application/json");
                        conn.setRequestProperty("Authorization", "Bearer " + AppData.accessToken);

                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                            for (String line; (line = reader.readLine()) != null; ) {
                                response.append(line).append("\n");
                            }
                        }

                        conn.disconnect();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return response.toString();
                }

            }.execute().get();

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return null;
    }

    private int getTotalTracks()
    {
        String response = spotifyRequest(0);

        try {
            JSONObject jsonObject = new JSONObject(response);
            return Integer.parseInt(jsonObject.get("total").toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return -1;
    }

    private Song getSongProperties(String response) {

        Song song = new Song();

        try {

            JSONObject jsonObject = new JSONObject(response);
            JSONArray itemsArray = jsonObject.getJSONArray("items");

            JSONObject saved_track = new JSONObject(itemsArray.get(0).toString());
            JSONObject track = saved_track.getJSONObject("track");

            // url
            JSONObject external_urls = track.getJSONObject("external_urls");
            song.url = external_urls.get("spotify").toString();

            // artist
            JSONArray artists = track.getJSONArray("artists");
            StringBuilder artistsList = new StringBuilder();
            for (int i = 0; i < artists.length(); i++) {
                artistsList.append(artists.getJSONObject(i).get("name").toString()).append(", ");
            }
            song.artist = artistsList.toString().substring(0, artistsList.length()-2);

            // name
            song.name = track.get("name").toString();

            // cover
            JSONObject album = track.getJSONObject("album");
            JSONArray images = album.getJSONArray("images");
            String coverUrl = images.getJSONObject(0).get("url").toString();

            song.cover = drawableFromUrl(coverUrl);

            return song;

        }catch (JSONException e){
            e.printStackTrace();
        }

        return null;
    }

    @SuppressLint("StaticFieldLeak")
    private Bitmap drawableFromUrl(final String url) {

        try {
            return new AsyncTask<Void,Void,Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void...v) {
                    try {

                        HttpURLConnection connection = (HttpURLConnection)new URL(url) .openConnection();
                        connection.setRequestProperty("User-agent","Mozilla/4.0");

                        connection.connect();
                        InputStream input = connection.getInputStream();

                        return BitmapFactory.decodeStream(input);

                    }catch (IOException e) {
                        e.printStackTrace();
                    }

                    return null;
                }

            }.execute().get();

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return null;
    }
}

