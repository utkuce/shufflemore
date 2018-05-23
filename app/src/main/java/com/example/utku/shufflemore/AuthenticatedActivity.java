package com.example.utku.shufflemore;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
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

    private final String REDIRECT_URI = "shufflemore://callback";
    private final int REQUEST_CODE = 1234;

    private ProgressDialog authDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (appData.getRefreshToken() == null)
            authenticateUser();
        else
            userIsAuthenticated();
    }

    @SuppressLint("StaticFieldLeak")
    private void userIsAuthenticated() {

        final Context context = this;
        new AsyncTask<Void , Void, Void>() {

            @Override
            protected Void doInBackground (Void... v) {

                if (AppData.userId == null)
                    setUserInfo();
                return null;
            }

            @Override
            protected void onPostExecute(Void v){

                postAuthentication(context);
            }

        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void postAuthentication(final Context context) {

        String connected_message = "Connected as " + "<b>" + AppData.userId + "</b>";
        ((TextView)findViewById(R.id.display_name)).setText(Html.fromHtml(connected_message));

        if (RandomSongProvider.chosenSongs.isEmpty())
            new AsyncTask<Void , Void, Void>()
            {
                @Override
                protected Void doInBackground (Void... v)  {

                    spotifyPlaylist = new Playlist(context, appData);
                    RandomSongProvider.chosenSongs.addAll(spotifyPlaylist.getTracks(appData));
                    return null;
                }

                @Override
                protected void onPostExecute(Void v){

                    trackRowAdapter.notifyDataSetChanged();
                    startService(new Intent(context, PlayBackReceiverService.class));
                }

            }.execute();
    }

    public void authenticateUser() {

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(appData.CLIENT_ID,
                AuthenticationResponse.Type.CODE, REDIRECT_URI);
        builder.setScopes(
                new String[]{

                        "user-library-read",
                        "playlist-modify-private",
                        "playlist-read-private",
                        "user-modify-playback-state",
                        "user-read-private"});

        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    private void setUserInfo() {

        final Context context = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                authDialog = ProgressDialog.show(context, "","Retrieving user id...",true);
            }
        });

        SyncHttpClient client = new SyncHttpClient();
        RequestParams params = new RequestParams();

        client.addHeader("Authorization", "Bearer " + appData.getAccessToken());
        params.put("Accept", "application/json");

        client.get("https://api.spotify.com/v1/me/", params,

                new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                        try {

                            AppData.userId = response.get("id").toString();
                            System.out.println("Got user id: " + AppData.userId);

                            AppData.userCountry = response.get("country").toString();
                            System.out.println("Got user country: " + AppData.userCountry);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject response) {

                        System.out.println(response);
                        Toast.makeText(getApplicationContext(), "Couldn't set user id", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFinish() {
                        authDialog.cancel();
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
                //System.out.println("Response code: " + response.getCode());
                new AsyncTask<Void,Void,Void>(){

                    @Override
                    protected Void doInBackground(Void... params) {
                        retrieveRefreshToken(response.getCode());
                        return null;
                    }

                }.execute();
            } else if (response.getType() == AuthenticationResponse.Type.ERROR) {
                new AlertDialog.Builder(getApplicationContext())
                        .setMessage(response.getError())
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).create().show();
                finish();
            }
        }
    }

    private void retrieveRefreshToken(String code) {

        final Context context = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                authDialog = ProgressDialog.show(context, "","Retrieving refresh token...",true);
            }
        });

        SyncHttpClient client = new SyncHttpClient();
        RequestParams params = new RequestParams();

        params.put("client_id", appData.CLIENT_ID);
        params.put("client_secret", appData.CLIENT_SECRET);

        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("scope", "user-library-read playlist-modify-private playlist-read-private user-modify-playback-state user-read-private");
        params.put("redirect_uri", REDIRECT_URI);

        client.post("https://accounts.spotify.com/api/token", params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, JSONObject response) {

                try {

                    appData.setRefreshToken(response.get("refresh_token").toString());

                    File file = new File(getApplication().getFilesDir(), "refresh_token.dat");
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(appData.getRefreshToken().getBytes());
                    fileOutputStream.close();

                    AppData.setHistory(new Vector<String>(), getApplication());

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

                System.out.println(res);
                new AlertDialog.Builder(getApplication())
                        .setTitle("ShuffleMore")
                        .setMessage("Setting refresh token failed")
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            }

            @Override
            public void onFinish() {
                authDialog.cancel();
            }
        });
    }
}

