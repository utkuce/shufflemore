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
import android.util.Pair;

public class RemoteService extends Service {

    public static Notification.Builder notificationBuilder;
    public static int notificationId = 123;
    public static volatile boolean connected = false;

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

        Playlist.disconnectAppRemote();
    }

    public static Notification.Builder getNotification(Context context) {

        Log.v("sm_REMOTE", "Building notification");

        Intent notificationIntent = new Intent(context, AuthenticatedActivity.class);
        notificationIntent.addCategory(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent pNotificationIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder =  new Notification.Builder(context)
                .setSubText("Next up")
                .setSmallIcon(R.drawable.round_shuffle_24)
                .setContentIntent(pNotificationIntent)
                .setOngoing(true);

        Pair[] actions = new Pair[]{

                new Pair<>("shufflemore.changenext", R.drawable.round_refresh_24),
                new Pair<>("shufflemore.playnext", R.drawable.round_play_circle_outline_24),
                new Pair<>("shufflemore.gotoalbum", R.drawable.round_album_24),
                new Pair<>("shufflemore.gotoartist", R.drawable.round_people_24),
                new Pair<>("shufflemore.stopservice", R.drawable.round_close_24)
        };

        for (int i=0; i < actions.length; i++) {

            Intent actionIntent = new Intent(context, AuthenticatedActivity.MyBroadcastReceiver.class);
            actionIntent.setAction(actions[i].first.toString());
            PendingIntent pActionIntent = PendingIntent.getBroadcast(context, i+1, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.addAction((int)actions[i].second, actions[i].first.toString(), pActionIntent);
        }

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
