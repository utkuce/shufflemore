package com.example.utku.shufflemore;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Vector;

import javax.net.ssl.HttpsURLConnection;

import cz.msebera.android.httpclient.Header;


public class AuthenticationActivity extends Activity {

    private final String REDIRECT_URI = "shufflemore://callback";
    private static String CLIENT_ID;
    private static String CLIENT_SECRET;
    private final int REQUEST_CODE = 1234;

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CLIENT_ID = getString(R.string.CLIENT_ID);
        CLIENT_SECRET = getString(R.string.CLIENT_SECRET);

        if (AppData.getRefreshToken(this) == null)
            connectAccount();

        new AsyncTask<Void , Void, String>()
        {
            @Override
            protected String doInBackground (Void... v)
            {
                return displayName();
            }

            @Override
            protected void onPostExecute(String name){
                setContentView(R.layout.activity_main);
                String connected = "Connected as " + "<b>" + name + "</b>";
                ((TextView)findViewById(R.id.textView)).setText(Html.fromHtml(connected));
            }
        }.execute();

    }

    public void connectAccount()
    {
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.CODE, REDIRECT_URI);
        builder.setScopes(new String[]{"user-library-read"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    private String displayName()
    {
        HttpsURLConnection conn;
        StringBuilder response = new StringBuilder();

        try {

            URL url = new URL("https://api.spotify.com/v1/me/");
            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + AppData.getAccessToken(this));

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    response.append(line).append("\n");
                }
            }

            conn.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }

        String name = "null";
        try{
            JSONObject jsonObject = new JSONObject(response.toString());
            name = jsonObject.get("id").toString();
        }catch (JSONException e){
            Toast.makeText(this, "JSON error", Toast.LENGTH_SHORT).show();
        }

        return name;
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
                System.out.println("Response code: " + response.getCode());
                new AsyncTask<Void,Void,Void>(){

                    @Override
                    protected Void doInBackground(Void... params) {
                        setRefreshToken(response.getCode());
                        return null;
                    }

                }.execute();
            }
        }
    }

    private void setRefreshToken(String code)
    {
        SyncHttpClient client = new SyncHttpClient();

        RequestParams params = new RequestParams();

        params.put("client_id", CLIENT_ID);
        params.put("client_secret", CLIENT_SECRET);

        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("scope", "user-library-read");
        params.put("redirect_uri", REDIRECT_URI);

        client.post("https://accounts.spotify.com/api/token", params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {

                try {

                    JSONObject jsonObject = new JSONObject(new String(bytes));

                    AppData.refreshToken = jsonObject.get("refresh_token").toString();

                    File file = new File(getApplication().getFilesDir(), "refresh_token.dat");
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(AppData.refreshToken.getBytes());
                    fileOutputStream.close();

                    AppData.setHistory(new Vector<String>(), getApplication());

                    AppData.accessToken = jsonObject.get("access_token").toString();
                    AppData.expirationTime = System.currentTimeMillis() +
                            Integer.parseInt(jsonObject.get("expires_in").toString()) * 1000;

                    //finish();
                    //startActivity(new Intent(context, MainActivity.class));

                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }

                //System.out.println("Access token: " + AppData.accessToken);
                //System.out.println("Refresh token: " + AppData.refreshToken);
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

                new AlertDialog.Builder(getApplication())
                        .setTitle("True Shuffle")
                        .setMessage("Connection problem")
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();

                //throwable.printStackTrace();
            }
        });
    }

    public static void useRefreshToken( final Context context)
    {
        SyncHttpClient client = new SyncHttpClient();

        RequestParams params = new RequestParams();

        params.put("client_id", CLIENT_ID);
        params.put("client_secret", CLIENT_SECRET);

        params.put("grant_type", "refresh_token");
        params.put("refresh_token", AppData.getRefreshToken(context));

        client.post("https://accounts.spotify.com/api/token", params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {

                try {

                    JSONObject jsonObject = new JSONObject(new String(bytes));

                    AppData.accessToken = jsonObject.get("access_token").toString();
                    AppData.expirationTime = System.currentTimeMillis() +
                            Integer.parseInt(jsonObject.get("expires_in").toString()) * 1000;

                    System.out.println("Access token refreshed");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

                System.out.println("Http request failed: " + i);
                if (bytes != null)
                    System.out.println(new String(bytes));

                //((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                  //      .notify(0, MainActivity.getNotification(context).setContentText("Connection problem").build());
                System.out.println("Connection problem");

//                throwable.printStackTrace();
            }
        });
    }
}

