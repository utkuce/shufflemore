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
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

import javax.net.ssl.HttpsURLConnection;

public class RandomSongProvider
{
    private static int totalTracks = -1;
    public static ArrayList<Song> chosenSongs = new ArrayList<>();

    private AppData appData;
    RandomSongProvider(AppData appData) {
        this.appData = appData;
    }

    public void addToHistory(Context context, String uri)
    {
        Vector<String> history = AppData.getHistory(context);
        if (history != null) {
            history.add(uri);
            while (history.size() > totalTracks * 0.5)
                history.removeElementAt(0);
            AppData.setHistory(history,context);
        }
        else
            Toast.makeText(context, "Cant read song history", Toast.LENGTH_LONG).show();
    }

    public static class Song {
        String uri, name, artist;
        Bitmap cover;
        boolean playable;
    }

    public Song getNewSong(Context context)
    {
        Song song = null;

        if ((totalTracks = (totalTracks == -1) ? getTotalTracks() : totalTracks) != -1)
        {
            Vector<String> history = AppData.getHistory(context);

            do {
                int offset = new Random(System.currentTimeMillis()).nextInt(totalTracks + 1);
                JSONObject response = getTrackObject(offset);
                song = getTrackProperties(response);

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

            } while (history.contains(song.uri) && song.uri == null);
        }

        if (song!= null && !song.playable) {

            System.out.println(song.name + " is not playable, getting another song");
            song = getNewSong(context);
        }

        System.out.println("Chosen song: " + song.name);
        return song;
    }

    @SuppressLint("StaticFieldLeak")
    private JSONObject getTrackObject(final int offset)
    {
        StringBuilder response = new StringBuilder();

        try {

            final URL url = new URL("https://api.spotify.com/v1/me/tracks?offset="
                    + Integer.toString(offset) + "&limit=1");

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + appData.getAccessToken());

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    response.append(line).append("\n");
                }
            }

            conn.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONObject track = null;
        try {

            JSONObject jsonObject = new JSONObject(response.toString());
            totalTracks = Integer.parseInt(jsonObject.get("total").toString());
            System.out.println("Total tracks: " + totalTracks);
            JSONArray itemsArray = jsonObject.getJSONArray("items");

            JSONObject saved_track = new JSONObject(itemsArray.get(0).toString());
            track = saved_track.getJSONObject("track");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return track;
    }

    private int getTotalTracks()
    {
        return 900; //TODO actual number
    }

    static Song getTrackProperties(JSONObject track) {

        Song song = new Song();

        try {

            // uri
            song.uri = track.get("uri").toString();

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
            String coverUrl = images.getJSONObject(0).get("url").toString(); //TODO image quality option

            song.cover = drawableFromUrl(coverUrl);

            // playable
            JSONArray availableMarkets = (JSONArray) track.get("available_markets");
            song.playable = false;
            if (availableMarkets != null) {
                for (int i=0;i<availableMarkets.length();i++){
                    if (availableMarkets.getString(i).equals(AppData.userCountry)) {
                        song.playable = true;
                    }
                }
            }

            return song;

        }catch (JSONException e){
            e.printStackTrace();
        }

        return null;
    }

    @SuppressLint("StaticFieldLeak")
    private static Bitmap drawableFromUrl(final String url) {

        try {

            HttpURLConnection connection = (HttpURLConnection)new URL(url) .openConnection();
            connection.setRequestProperty("User-agent","Mozilla/4.0");

            connection.connect();
            InputStream input = connection.getInputStream();

            return BitmapFactory.decodeStream(input);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}

