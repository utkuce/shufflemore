package com.example.utku.shufflemore;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

public class RemoteService extends Service {

    public static Notification.Builder notificationBuilder;
    public static int notificationId = 123;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        notificationBuilder = getNotification(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(notificationId, notificationBuilder.build());
        }
        Log.v("sm_REMOTE", "start foreground called");

        Playlist.connectAppRemote(this);
        Log.v("sm_REMOTE", "Remote service started");

        return startId;
    }

    @Override
    public void onDestroy()
    {
        Log.w("sm_REMOTE","Remote service stopped");

        NotificationManager nm;
        if (((nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE))) != null)
            nm.cancel(notificationId);
    }

    public static Notification.Builder getNotification(Context context) {

        Log.v("sm_REMOTE", "Building notification");

        Intent notificationIntent = new Intent(context, Playlist.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent changeButton = new Intent("shufflemore.changenext");
        PendingIntent changeNext = PendingIntent.getBroadcast(context, 1, changeButton,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder =  new Notification.Builder(context)

                .setSubText("Next up")

                .addAction(R.drawable.round_play_circle_outline_24, "play", null)
                .addAction(R.drawable.round_refresh_24, "shuffle", changeNext)
                .addAction(R.drawable.round_album_24, "album", null)
                .addAction(R.drawable.round_people_24, "artist", null)
                .addAction(R.drawable.round_close_24, "close", null)

                .setSmallIcon(R.drawable.round_shuffle_24)

                .setContentIntent(pendingIntent)
                .setOngoing(true);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel("SMNC", "shufflemore notification channel", importance);

            if (mNotificationManager != null) {

                mNotificationManager.createNotificationChannel(mChannel);
                builder.setChannelId("SMNC");
            }
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            builder.setStyle(new Notification.MediaStyle()
                    .setShowActionsInCompactView(0,1));
        }

        if (!RandomSongProvider.chosenSongs.isEmpty()) {
            RandomSongProvider.Song nextUp = RandomSongProvider.chosenSongs.get(1);
            builder.setLargeIcon(nextUp.cover)
                    .setContentTitle(nextUp.name)
                    .setContentText(nextUp.artist);
        }

        return builder;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
