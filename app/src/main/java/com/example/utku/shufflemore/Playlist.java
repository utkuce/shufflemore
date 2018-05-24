package com.example.utku.shufflemore;

import android.content.Context;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

class Playlist {

    static final String name = ".shufflemore";
    static String id;

    private Context context;
    private AppData appData;

    Playlist(final Context context, AppData appData) {

        this.context = context;
        this.appData = appData;

        if (!alreadyExists())
            create();
    }

    void create() {

        String url = String.format("https://api.spotify.com/v1/users/%s/playlists", AppData.userId);
        SyncHttpClient client = new SyncHttpClient();
        client.addHeader("Authorization", "Bearer " + appData.getAccessToken());

        JSONObject jsonParams = new JSONObject();
        StringEntity data = null;

        try {

            jsonParams.put("name", name);
            jsonParams.put("description", "Created by ShuffleMore app");
            jsonParams.put("public", false);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            data = new StringEntity(jsonParams.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        client.post(context, url, data, "application/json", new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                try {
                    id = response.get("id").toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {

                Toast.makeText(context, statusCode + " Playlist not created", Toast.LENGTH_LONG).show();
                System.out.println(statusCode + " res: " + res);
            }

        });
    }

    private boolean alreadyExists = false;
    boolean alreadyExists() {

        SyncHttpClient client = new SyncHttpClient();
        RequestParams params = new RequestParams();

        client.addHeader("Authorization", "Bearer " + appData.getAccessToken());
        params.put("Accept", "application/json");

        client.get("https://api.spotify.com/v1/me/playlists", params,

                new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                        try {

                            JSONArray playlists = response.getJSONArray("items");
                            for (int i=0; i<playlists.length(); i++) {

                                JSONObject playlist = playlists.getJSONObject(i);
                                if (name.equals(playlist.get("name").toString())) {

                                    alreadyExists = true;
                                    id = playlist.get("id").toString();
                                    System.out.println("Playlist id: " + id);
                                }
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject response) {
                        System.out.println(response);
                    }
                });

        return alreadyExists;
    }

    void addTrack(String uri) {

        String url = String.format("https://api.spotify.com/v1/users/%s/playlists/%s/tracks", AppData.userId, id);

        SyncHttpClient client = new SyncHttpClient();
        client.addHeader("Authorization", "Bearer " + appData.getAccessToken());

        JSONObject jsonParams = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(uri);

        StringEntity data = null;

        try {

            jsonParams.put("uris", jsonArray);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            data = new StringEntity(jsonParams.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        client.post(context, url, data, "application/json", new JsonHttpResponseHandler() {


            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                System.out.println("New track added to playlist");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject res) {

                Toast.makeText(context, statusCode + " Playlist not created", Toast.LENGTH_LONG).show();
                System.out.println(statusCode + " res: " + res);
            }

        });

    }

    boolean removeSuccess = false;
    boolean removeTrack(String uri) {

        String url = String.format("https://api.spotify.com/v1/users/%s/playlists/%s/tracks", AppData.userId, id);

        SyncHttpClient client = new SyncHttpClient();
        client.addHeader("Authorization", "Bearer " + appData.getAccessToken());

        JSONObject jsonParams = new JSONObject();
        StringEntity data = null;

        try {

            JSONArray tracks = new JSONArray();
            tracks.put(new JSONObject().put("uri", uri));
            jsonParams.put("tracks", tracks);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            data = new StringEntity(jsonParams.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        client.delete(context, url, data, "application/json", new JsonHttpResponseHandler() {


            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                removeSuccess = true;
                System.out.println("Track removed from playlist");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject res) {

                Toast.makeText(context, statusCode + " Track not removed", Toast.LENGTH_LONG).show();
                System.out.println(statusCode + " res: " + res);
            }

        });

        return removeSuccess;
    }

    private ArrayList<RandomSongProvider.Song> songList = new ArrayList<>();
    ArrayList<RandomSongProvider.Song> getTracks() {

        songList.clear();
        String url = String.format("https://api.spotify.com/v1/users/%s/playlists/%s/tracks", AppData.userId, id);

        SyncHttpClient client = new SyncHttpClient();
        RequestParams params = new RequestParams();

        client.addHeader("Authorization", "Bearer " + appData.getAccessToken());
        params.put("Accept", "application/json");

        client.get(url, params,

                new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                        try {

                            JSONArray tracks = response.getJSONArray("items");
                            System.out.println(tracks.length() + " songs in playlist:");

                            for (int i=0; i<tracks.length(); i++) {

                                JSONObject track = tracks.getJSONObject(i).getJSONObject("track");
                                System.out.println("Track name: " + track.get("name").toString());
                                songList.add(RandomSongProvider.getTrackProperties(track));
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject response) {
                        System.out.println(response);
                    }
                });

        return songList;
    }

    void startPlayback() {

        String url = "https://api.spotify.com/v1/me/player/play";
        AsyncHttpClient client = new AsyncHttpClient();

        client.addHeader("Authorization", "Bearer " + appData.getAccessToken());

        JSONObject jsonParams = new JSONObject();
        StringEntity data = null;

        try {

            jsonParams.put("context_uri", String.format("spotify:user:%s:playlist:%s", AppData.userId, Playlist.id));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            data = new StringEntity(jsonParams.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        client.put(context, url, data, "application/json", new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                System.out.println("Playlist playback started");
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

                Toast.makeText(context, "Playlist playback error: " + i, Toast.LENGTH_SHORT).show();
                System.out.println("Playlist playback error: " + i + new String(bytes));
            }
        });
    }
}