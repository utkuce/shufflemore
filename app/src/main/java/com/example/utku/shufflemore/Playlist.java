package com.example.utku.shufflemore;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

import com.example.utku.shufflemore.RandomSongProvider.Song;

class Playlist {

    private static final String name = ".shufflemore";
    String id;

    private Context context;
    private AppData appData;

    static SpotifyAppRemote mSpotifyAppRemote;
    private static Connector.ConnectionListener connectionListener;
    private boolean remoteConnected = false;

    private RandomSongProvider randomSongProvider;

    private String lastCallback = "";
    String chosenButSkipped = "";


    Playlist(final Context context, AppData appData) {

        Log.v("sm_PLAYLIST", "Creating playlist object");

        this.context = context;
        this.appData = appData;

        connectionListener = new Connector.ConnectionListener() {

            @SuppressLint("StaticFieldLeak")
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {

                mSpotifyAppRemote = spotifyAppRemote;
                Log.v("sm_PLAYLIST","Spotify App Remote connected");
                Toast.makeText(context, "Spotify App Remote connected", Toast.LENGTH_SHORT).show();
                remoteConnected = true;

                randomSongProvider = new RandomSongProvider(appData);

                mSpotifyAppRemote.getPlayerApi()
                        .subscribeToPlayerState()
                        .setEventCallback(playerState -> {

                            //Log.v("sm_PLAYLIST","Player state event callback received: "
                            //      + playerState.track.name  + ", " + playerState.playbackPosition);

                            String currentSong = playerState.track.uri;
                            String secondInPlaylist = RandomSongProvider.chosenSongs.get(1).uri;


                            if (!lastCallback.equals("") && !lastCallback.equals(currentSong)) {
                                if (currentSong.equals(secondInPlaylist) || currentSong.equals(chosenButSkipped)) {

                                    Log.v("sm_PLAYLIST","Song ended, adjusting next up");

                                    new AsyncTask<Void , Void, Song>()
                                    {
                                        @Override
                                        protected Song doInBackground (Void... v)  {

                                            pausePlayback();
                                            return randomSongProvider.getNewSong(context);
                                        }

                                        @Override
                                        protected void onPostExecute(final Song newSong){

                                            new Thread(() -> {

                                                boolean removed = removeTrack(RandomSongProvider.chosenSongs.get(0).uri);
                                                if (removed)
                                                    RandomSongProvider.chosenSongs.remove(0);

                                                addTrack(newSong.uri); // TODO: add success check
                                                RandomSongProvider.chosenSongs.add(newSong);

                                                chosenButSkipped = "";
                                                startPlayback();
                                                context.sendBroadcast(new Intent("shufflemore.updateUI"));

                                            }).start();
                                        }
                                    }.execute();
                                }
                            }

                            lastCallback = playerState.track.uri;
                        });
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.w("sm_PLAYLIST", "Connection lost to Spotify App Remote, reconnecting");
                connectAppRemote(context);

                /*
                AlertDialog.Builder builder = new AlertDialog.Builder(context);

                builder.setTitle("Connection lost to Spotify App Remote")
                        .setMessage(throwable.getMessage())
                        .setPositiveButton("reconnect", (dialog, which) -> connectAppRemote(context))
                        .show();
                */
            }
        };
    }

    @SuppressLint("StaticFieldLeak")
    static void connectAppRemote(Context context) {

        ConnectionParams connectionParams = new ConnectionParams.Builder(AppData.CLIENT_ID)
                .setRedirectUri(AuthenticatedActivity.REDIRECT_URI)
                .showAuthView(true)
                .build();

        SpotifyAppRemote.connect(context, connectionParams, connectionListener);

    }

    static void disconnetcAppRemote() {

        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
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
                Log.e("sm_PLAYLIST",statusCode + " res: " + res);
            }

        });
    }

    private boolean alreadyExists = false;
    boolean alreadyExists() {

        Log.v("sm_PLAYLIST", "Checking if playlist already exists");

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
                                    Log.v("sm_PLAYLIST","Playlist already exists, playlist id: " + id);
                                }
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject response) {
                        Log.e("sm_PLAYLIST","Status code: " + statusCode + " Playlist exists check failed, response: " + response);
                    }
                });

        return alreadyExists;
    }

    void addTrack(String uri) {

        //if (RandomSongProvider.chosenSongs.size() > 0)
        //    mSpotifyAppRemote.getPlayerApi().queue(uri);

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

                Log.v("sm_PLAYLIST","New track added to playlist");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject res) {

                Toast.makeText(context, statusCode + " Playlist could not be created", Toast.LENGTH_LONG).show();
                Log.e("sm_PLAYLIST",statusCode + " res: " + res);
            }

        });
    }

    private boolean removeSuccess = false;
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
                Log.v("sm_PLAYLIST","Track removed from playlist");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject res) {

                Toast.makeText(context, statusCode + " Track could not be removed", Toast.LENGTH_LONG).show();
                Log.e("sm_PLAYLIST",statusCode + " res: " + res);
            }

        });

        return removeSuccess;
    }

    private ArrayList<Song> songList = new ArrayList<>();
    ArrayList<Song> getTracks() {

        Log.v("sm_PLAYLIST", "Getting tracks list");

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
                            Log.v("sm_PLAYLIST",tracks.length() + " songs currently in playlist:");

                            for (int i=0; i<tracks.length(); i++) {

                                JSONObject track = tracks.getJSONObject(i).getJSONObject("track");
                                Log.v("sm_PLAYLIST","Track name: " + track.get("name").toString());
                                songList.add(RandomSongProvider.getTrackProperties(track));
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject response) {
                        Log.e("sm_PLAYLIST","Status code: " + statusCode + "Get tracks failed, responsea: " + response);
                    }
                });

        return songList;
    }

    void startPlayback() {

        Log.v("sm_PLAYLIST","Starting playback");
        if (remoteConnected)
            mSpotifyAppRemote.getPlayerApi().play(String.format("spotify:user:%s:playlist:%s", AppData.userId, this.id));

        //TODO: else error
    }

    private void pausePlayback() {

        mSpotifyAppRemote.getPlayerApi().pause();
        Log.v("sm_PLAYLIST", "Playback paused");
    }
}