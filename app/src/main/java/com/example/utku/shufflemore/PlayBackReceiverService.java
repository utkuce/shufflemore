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

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (action != null && action.equals("com.spotify.music.metadatachanged")) {

                //TODO save playlist position to return back later
                System.out.println("Metadata changed");

                if (intent.getStringExtra("id").equals(RandomSongProvider.chosenSongs.get(1).uri)) {

                    System.out.println("Random song finished, adjusting playlist");
                    sendBroadcast(new Intent("shufflemore.playnext"));
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

        Notification.Builder builder =  new Notification.Builder(context)
                .setSubText("Next up on ShuffleMore")
                .addAction(R.drawable.baseline_shuffle_24, "Change", changeNext)
                .setSmallIcon(R.drawable.baseline_shuffle_24)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        if (!RandomSongProvider.chosenSongs.isEmpty()) {
            RandomSongProvider.Song firstSong = RandomSongProvider.chosenSongs.get(0);
            builder.setLargeIcon(firstSong.cover)
                    .setContentTitle(firstSong.name)
                    .setContentText(firstSong.artist);
        }

        return builder;
    }
}