package com.example.utku.shufflemore;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

import cz.msebera.android.httpclient.Header;

public class AppData {

    private Context context;

    String CLIENT_ID;
    String CLIENT_SECRET;

    private String refreshToken;
    private String accessToken;
    public static long expirationTime; //TODO write to file

    static String userId;
    static String userCountry;


    AppData(Context context) {

        this.context = context;

        CLIENT_ID = context.getString(R.string.CLIENT_ID);
        CLIENT_SECRET = context.getString(R.string.CLIENT_SECRET);
    }

    void setAccessToken(String token) {
        accessToken = token;
    }

    String getAccessToken() {

        if (accessToken == null)
          retrieveAccessToken();

        return accessToken;

    }

    void setRefreshToken(String token) {
            refreshToken = token;
    }

    String getRefreshToken()
    {
        if (refreshToken == null) {

            File file  = new File(context.getFilesDir(), "refresh_token.dat");

            try {

                FileInputStream fileInputStream = new FileInputStream(file);
                refreshToken = new BufferedReader(new InputStreamReader(fileInputStream)).readLine();

            } catch (FileNotFoundException e) {

                Toast.makeText(context, "Refresh token not found", Toast.LENGTH_LONG).show();
                System.out.println("File not found: " + file.getPath());
                return null;

            } catch (IOException e) {

                Toast.makeText(context, "Error reading refresh token", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }

        //System.out.println("Refresh token: " + refreshToken);
        return refreshToken;
    }

    static Vector<String> getHistory(Context context)
    {
        try
        {
            File file  = new File(context.getFilesDir(), "history.dat");
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            Vector<String> h = (Vector<String>) in.readObject();
            in.close();
            fileIn.close();
            return h;

        }catch(IOException i) {
            i.printStackTrace();
        }catch(ClassNotFoundException c) {
            System.out.println("Class not found");
            c.printStackTrace();
        }

        return null;
    }

    static void setHistory(Vector<String> history, Context context)
    {
        try {
            File file = new File(context.getFilesDir(), "history.dat");
            FileOutputStream fileOutputStream2 = new FileOutputStream(file);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream2);
            objectOutputStream.writeObject(history);
            objectOutputStream.close();
            fileOutputStream2.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean tokenExpired()
    {
        return expirationTime <= System.currentTimeMillis();
    }

    private void retrieveAccessToken()
    {
        System.out.println("Retrieving access token");

        SyncHttpClient client = new SyncHttpClient();
        RequestParams params = new RequestParams();

        params.put("client_id", CLIENT_ID);
        params.put("client_secret", CLIENT_SECRET);

        params.put("grant_type", "refresh_token");
        params.put("refresh_token", getRefreshToken());

        client.post("https://accounts.spotify.com/api/token", params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, JSONObject response) {

                try {

                    setAccessToken(response.get("access_token").toString());
                    expirationTime = System.currentTimeMillis() +
                            Integer.parseInt(response.get("expires_in").toString()) * 1000;

                    System.out.println("Access token refreshed");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String response, Throwable t) {

                System.out.println("Http request failed: " + statusCode);
                System.out.println(response);

                //((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                //      .notify(0, MainActivity.getNotification(context).setContentText("Connection problem").build());

                System.out.println("Failed to retrieve access token");
                new AlertDialog.Builder(context)
                        .setMessage("Failed to retrieve access token")
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).create().show();
            }
        });
    }
}

