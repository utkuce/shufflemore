package com.example.utku.shufflemore;

import android.content.Context;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

class Playlist {

    static final String name = ".shufflemore";
    static String id;

    Playlist(final Context context, AppData appData) {

        if (!alreadyExists(appData))
            create(context, appData);
    }

    private void create(final Context context, AppData appData) {

        String url = String.format("https://api.spotify.com/v1/users/%s/playlists", appData.userId);
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
    private boolean alreadyExists(final AppData appData) {

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

    static void addTrack(final Context context, AppData appData, String uri) {

        String url = String.format("https://api.spotify.com/v1/users/%s/playlists/%s/tracks", appData.userId, id);

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

        System.out.print(jsonParams);

        client.post(context, url, data, "application/json", new JsonHttpResponseHandler() {


            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                System.out.println(response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject res) {

                //Toast.makeText(context, statusCode + " Playlist not created", Toast.LENGTH_LONG).show();
                System.out.println(statusCode + " res: " + res);
            }

        });

    }
}