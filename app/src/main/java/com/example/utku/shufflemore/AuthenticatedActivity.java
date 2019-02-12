package com.example.utku.shufflemore;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import cz.msebera.android.httpclient.Header;


public class AuthenticatedActivity extends MainActivity {

    static final String REDIRECT_URI = "shufflemore://callback";
    private final int REQUEST_CODE = 1234;

    final String[] spotifyApiPermissions = new String[]{

            "user-library-read",
            "playlist-modify-private",
            "playlist-read-private",
            "user-modify-playback-state",
            "user-read-private",

            "app-remote-control"
    };

    public static class MyBroadcastReceiver extends BroadcastReceiver {

        static AuthenticatedActivity activity;

        @Override
        public void onReceive(Context context, Intent intent) {

            if (activity == null) {
                Log.e("sm_AUTHACT", "Activity for broadcast receiver not set");
                return;
            }

            Log.v("sm_AUTHACT", "Broadcast onReceive called");
            String action = intent.getAction();

            if (action == null) {
                Log.e("sm_AUTHACT", "Received empty action");
                return;
            }

            Log.v("sm_AUTHACT", "intent received: " + action);

            switch (action) {

                case "shufflemore.updateUI":

                    activity.runOnUiThread(() -> {

                        activity.updateUI(RandomSongProvider.chosenSongs.get(0),
                                RandomSongProvider.chosenSongs.get(1));
                    });

                    break;

                case "shufflemore.changenext":

                    activity.changeNextSong();
                    break;

                case "shufflemore.playnext":
                    activity.spotifyPlaylist.mSpotifyAppRemote.getPlayerApi().skipNext();
                    break;

                case "shufflemore.gotoalbum":

                    break;

                case "shufflemore.gotoartist":

                    break;

                case "shufflemore.stopservice":
                    activity.stopService(activity.spotifyRemoteService);
                    break;

                default:

            }

        }
    };

    private Intent spotifyRemoteService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Log.v("sm_AUTHACT", "Checking authentication");
        ((TextView)findViewById(R.id.splash_text)).setText("Checking authentication...");

        if (appData.getRefreshToken() == null)
            authenticateUser();
        else
            userIsAuthenticated();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //unregisterReceiver(broadcastReceiver);
        stopService(spotifyRemoteService);
    }

    @SuppressLint("StaticFieldLeak")
    private void userIsAuthenticated() {

        Log.v("sm_AUTHACT", "User is authenticated");

        new AsyncTask<Void , Void, Void>() {

            @Override
            protected Void doInBackground (Void... v) {

                if (AppData.userId == null)
                    setUserInfo();

                return null;
            }

            @Override
            protected void onPostExecute(Void v){

                postAuthentication();
            }

        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void postAuthentication() {

        Log.v("sm_AUTHACT", "Authentication complete, user: " + AppData.userId);

        String connected_message = "Connected as " + "<b>" + AppData.userId + "</b>";
        ((TextView)findViewById(R.id.display_name)).setText(Html.fromHtml(connected_message));

        startJob();
    }

    public void authenticateUser() {

        Log.v("sm_AUTHACT", "Authenticating user");
        ((TextView)findViewById(R.id.splash_text)).setText("Authenticating user...");

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(appData.CLIENT_ID,
                AuthenticationResponse.Type.CODE, REDIRECT_URI);
        builder.setScopes(spotifyApiPermissions);
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    private void setUserInfo() {

        Log.v("sm_AUTHACT", "Setting user info");
        runOnUiThread(() -> { ((TextView)findViewById(R.id.splash_text)).setText("Setting user info..."); });


        SyncHttpClient client = new SyncHttpClient();
        RequestParams params = new RequestParams();

        client.addHeader("Authorization", "Bearer " + appData.getAccessToken());
        params.put("Accept", "application/json");

        runOnUiThread(() -> { ((TextView)findViewById(R.id.splash_text)).setText("Retrieving user id..."); });

        client.get("https://api.spotify.com/v1/me/", params,

                new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                        try {

                            AppData.userId = response.get("id").toString();
                            Log.v("sm_AUTHACT", "Got user id: " + AppData.userId);

                            AppData.userCountry = response.get("country").toString();
                            Log.v("sm_AUTHACT", "Got user country: " + AppData.userCountry);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject response) {

                        Log.e("sm_AUTHACT", "Could not get user info, response: " + String.valueOf(response));
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Couldn't set user id", Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onFinish() {
                        //authDialog.cancel();
                    }
                });
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_CODE)
        {
            final AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.CODE)
            {
                Log.v("sm_AUTHACT", "Authentication response code: " + response.getCode());
                new AsyncTask<Void,Void,Void>(){

                    @Override
                    protected Void doInBackground(Void... params) {
                        retrieveRefreshToken(response.getCode());
                        return null;
                    }

                }.execute();
            } else if (response.getType().equals(AuthenticationResponse.Type.ERROR)) {

                Log.e("sm_AUTHACT", "Spotify Authentication Error: " + response.getError());
                new AlertDialog.Builder(AuthenticatedActivity.this)
                        .setTitle("Spotify Authentication Error")
                        .setMessage(response.getError())
                        .setOnDismissListener(dialogInterface -> finish()).create().show();
            }
        }
    }

    private void retrieveRefreshToken(String code) {

        Log.v("sm_AUTHACT", "Retrieving refresh token");

        final Context context = this;
        //runOnUiThread(() -> authDialog = ProgressDialog.show(context, "","Retrieving refresh token...",true));
        ((TextView)findViewById(R.id.splash_text)).setText("Retrieving refresh token...");

        SyncHttpClient client = new SyncHttpClient();
        RequestParams params = new RequestParams();

        params.put("client_id", appData.CLIENT_ID);
        params.put("client_secret", appData.CLIENT_SECRET);

        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("scope", TextUtils.join(" ", spotifyApiPermissions));
        params.put("redirect_uri", REDIRECT_URI);

        client.post("https://accounts.spotify.com/api/token", params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, JSONObject response) {

                Log.v("sm_AUTHACT", "Refresh token response (success): " + response);
                try {

                    appData.setRefreshToken(response.get("refresh_token").toString());

                    File file = new File(getApplication().getFilesDir(), "refresh_token.dat");
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(appData.getRefreshToken().getBytes());
                    fileOutputStream.close();

                    appData.setHistory(new Vector<>(), getApplication());

                    appData.setAccessToken(response.get("access_token").toString());
                    AppData.expirationTime = System.currentTimeMillis() +
                            Integer.parseInt(response.get("expires_in").toString()) * 1000;

                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }

                userIsAuthenticated();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {

                Log.e("sm_AUTHACT", "Refresh token response (failed): " + res);
                new AlertDialog.Builder(getApplication())
                        .setTitle("ShuffleMore")
                        .setMessage("Setting refresh token failed")
                        .setNeutralButton("OK", (dialog, which) -> {

                        })
                        .show();
            }

            @Override
            public void onFinish() {
                //authDialog.cancel();
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    protected void startJob() {

        Log.v("sm_AUTHACT", "Starting job");

        ((TextView)findViewById(R.id.splash_text)).setText("Creating playlist object...");
        spotifyPlaylist = new Playlist(this, appData);
        randomSongProvider = new RandomSongProvider(appData);
        MyBroadcastReceiver.activity = this;

        IntentFilter filter = new IntentFilter();
        filter.addAction("shufflemore.updateUI");

        //Log.v("sm_AUTHACT", "Registering broadcast receiver");
        //registerReceiver(broadcastReceiver, filter);

        final Context context = this;

        Log.v("sm_AUTHACT", "Chosen songs list is empty");

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... v) {

                if (!spotifyPlaylist.alreadyExists())
                    spotifyPlaylist.create();

                RandomSongProvider.chosenSongs.clear();
                RandomSongProvider.chosenSongs.addAll(spotifyPlaylist.getTracks());
                while (RandomSongProvider.chosenSongs.size() < 2) {

                    RandomSongProvider.Song newSong = randomSongProvider.getNewSong(context);
                    RandomSongProvider.chosenSongs.add(newSong);
                    spotifyPlaylist.addTrack(newSong.uri);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void v) {

                Log.v("sm_AUTHACT", "Creating remote service");
                ((TextView)findViewById(R.id.splash_text)).setText("Creating service...");
                spotifyRemoteService = new Intent(context, RemoteService.class);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(spotifyRemoteService);
                } else {
                    startService(spotifyRemoteService);
                }

                updateUI(RandomSongProvider.chosenSongs.get(0),
                        RandomSongProvider.chosenSongs.get(1));

                findViewById(R.id.gui).setVisibility(View.VISIBLE);
                findViewById(R.id.splash).setVisibility(View.GONE);
            }

        }.execute();
    }

}

