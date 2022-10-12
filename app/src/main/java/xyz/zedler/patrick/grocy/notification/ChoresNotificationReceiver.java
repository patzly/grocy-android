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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import androidx.preference.PreferenceManager;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.CHORES;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.NOTIFICATIONS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.util.ReminderUtil;

public class ChoresNotificationReceiver extends BroadcastReceiver {

  public void onReceive(Context context, Intent intent) {
    NotificationManager notificationManager = (NotificationManager) context.getSystemService(
        Context.NOTIFICATION_SERVICE
    );
    if (notificationManager == null) {
      return;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      CharSequence name = context.getString(R.string.title_chores);
      String description = context.getString(R.string.setting_notifications_chores_description);
      int importance = NotificationManager.IMPORTANCE_HIGH;
      NotificationChannel channel = new NotificationChannel(
          NOTIFICATIONS.CHORES_CHANNEL, name, importance
      );
      channel.setDescription(description);
      notificationManager.createNotificationChannel(channel);
    }
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    DownloadHelper dlHelper = new DownloadHelper(context, ChoresNotificationReceiver.class.getSimpleName());

    dlHelper.getChoreEntries(choreEntries -> {
      if (choreEntries.size() == 0) return;

      int days = sharedPrefs.getInt(
          CHORES.DUE_SOON_DAYS,
          SETTINGS_DEFAULT.CHORES.DUE_SOON_DAYS
      );
      String titleText = context.getResources().getQuantityString(
          R.plurals.description_overview_chores_due_today,
          choreEntries.size(), choreEntries.size(), days
      );

      Uri uri = Uri.parse(context.getString(R.string.deep_link_choresFragment));
      Intent notificationIntent = new Intent(Intent.ACTION_VIEW, uri);
      notificationIntent.setClass(context, MainActivity.class);
      notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

      notificationManager.notify(NOTIFICATIONS.CHORES_ID, ReminderUtil.getNotification(
          context,
          titleText,
          null,
          NOTIFICATIONS.CHORES_ID,
          NOTIFICATIONS.CHORES_CHANNEL,
          notificationIntent
      ));
      dlHelper.destroy();
    }, error -> dlHelper.destroy());
  }
}
