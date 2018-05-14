package com.example.utku.shufflemore;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.widget.Toast;

public class PlayBackReceiverService extends Service {

    public static Notification.Builder notificationBuilder;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.spotify.music.playbackstatechanged");
        filter.addAction("com.spotify.music.metadatachanged");
        registerReceiver(receiver, filter);
        System.out.println("Playback state service started");

        notificationBuilder = getNotification(this);
        NotificationManager nm = ((NotificationManager)getSystemService(NOTIFICATION_SERVICE));
        if (nm != null)
            nm.notify(0, notificationBuilder.build());

        return startId;
    }

    @Override
    public void onDestroy()
    {
        System.out.println("Playback state service stopped");

        NotificationManager nm;
        if (((nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE))) != null)
            nm.cancel(0);
        unregisterReceiver(receiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        boolean[] playing = new boolean[2];

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (action != null) {
                if (action.equals("com.spotify.music.playbackstatechanged"))
                {
                    playing[0] = playing[1];
                    playing[1] = intent.getBooleanExtra("playing", false);
                    int positionInMs = intent.getIntExtra("playbackPosition", 0);

                    System.out.println("Playback state changed. Pos: " + positionInMs
                            + " playing: " + playing[1] + " previously: " + playing[0]);

                    if (positionInMs == 0 && !playing[1] && playing[0]){
                        System.out.println("Song finished, starting next");
                        sendBroadcast(new Intent("shufflemore.playnext"));
                    }
                } else if (action.equals("com.spotify.music.metadatachanged")) {
                    String uri;
                    if ((uri = RandomSongProvider.currentSongUri) != null)
                        if (!intent.getStringExtra("id").equals(uri)) {
                            System.out.println("Song finished, starting next");
                            sendBroadcast(new Intent("shufflemore.playnext"));
                        }
                }
            } else {
                Toast.makeText(context, "Intent action is empty", Toast.LENGTH_LONG).show();
            }
        }
    };

    public static Notification.Builder getNotification(Context context) {

        Intent notification = new Intent(context, RandomSongProvider.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, notification,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent changeButton = new Intent("shufflemore.changenext");
        PendingIntent changeNext = PendingIntent.getBroadcast(context, 1, changeButton,
                PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Builder(context)
                .setSubText("Next up on ShuffleMore")
                .addAction(R.mipmap.baseline_shuffle_black_24, "Change", changeNext)
                .setSmallIcon(R.mipmap.ic_queue_music_black_24dp)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

    }
}