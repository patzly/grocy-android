/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.NOTIFICATIONS;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS_DEFAULT;

public class NotificationService extends Service {

  private static BroadcastReceiver m_ScreenOffReceiver = null;

  @Override
  public void onCreate() {
    super.onCreate();
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
      startMyOwnForeground();
    } else {
      startForeground(1, new Notification());
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private void startMyOwnForeground() {
    String NOTIFICATION_CHANNEL_ID = "xyz.zedler.patrick.grocy.permanence";
    String channelName = "Background Service";
    NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
    chan.setLightColor(Color.BLUE);
    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    assert manager != null;
    manager.createNotificationChannel(chan);
    Bitmap bitmap = getBitmapFromVectorDrawable(getApplicationContext(), R.drawable.ic_round_grocy);
    Bitmap scaledBitmap = bitmap != null ? Bitmap.createScaledBitmap(
        bitmap,
        getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
        getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height),
        true
    ) : null;
    NotificationCompat.Builder notificationBuilder = new NotificationCompat
        .Builder(this, NOTIFICATION_CHANNEL_ID);
    Notification notification = notificationBuilder
        .setOngoing(true)
        .setContentTitle("App is running in background")
        .setCategory(Notification.CATEGORY_SERVICE)
        .setPriority(Notification.PRIORITY_MIN)
        .setSmallIcon(R.drawable.ic_round_grocy_notification)
        .setLargeIcon(scaledBitmap)
        .build();
    /*builder.setContentText("This is the text");
    builder.setSubText("Some sub text");
    builder.setTicker("Fancy Notification");*/
    startForeground(2, notification);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);
    boolean shouldClose = intent.getBooleanExtra("close", false);
    if (shouldClose) {
      stopSelf();
    } else {
      // Continue to action here
    }
//        return START_STICKY;
    return START_REDELIVER_INTENT;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    if (PreferenceManager.getDefaultSharedPreferences(getApplication()).getBoolean(
        NOTIFICATIONS.DUE_SOON_ENABLE,
        SETTINGS_DEFAULT.NOTIFICATIONS.DUE_SOON_ENABLE
    )) {
      Intent broadcastIntent = new Intent();
      broadcastIntent.setAction("restartservice");
      broadcastIntent.setClass(this, RestarterBroadcastReceiver.class);
      this.sendBroadcast(broadcastIntent);
    }
  }

  @Nullable
  public IBinder onBind(Intent intent) {
    return null;
  }

  public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
    Drawable drawable = ContextCompat.getDrawable(context, drawableId);
    if (drawable == null) return null;
    Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
        drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);
    return bitmap;
  }
}
