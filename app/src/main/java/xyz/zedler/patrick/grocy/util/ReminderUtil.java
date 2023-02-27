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
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import com.google.android.material.color.DynamicColors;
import java.util.Calendar;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.SETTINGS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT.NOTIFICATIONS;
import xyz.zedler.patrick.grocy.Constants.THEME;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.notification.BootReceiver;
import xyz.zedler.patrick.grocy.notification.DueSoonNotificationReceiver;

public class ReminderUtil {

  private static final String TAG = ReminderUtil.class.getSimpleName();

  private final Context context;
  private final SharedPreferences sharedPrefs;
  private final AlarmManager alarmManager;
  private PendingIntent pendingIntent;
  private final NotificationManager notificationManager;

  public ReminderUtil(Context context) {
    this.context = context;

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    notificationManager = (NotificationManager) context.getSystemService(
        Context.NOTIFICATION_SERVICE
    );
  }

  @SuppressLint("SimpleDateFormat")
  public void scheduleReminder(@Nullable String time) {
    if (time == null) {
      time = NOTIFICATIONS.DUE_SOON_TIME;
    }

    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(System.currentTimeMillis());

    calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.split(":")[0]));
    calendar.set(Calendar.MINUTE, Integer.parseInt(time.split(":")[1]));
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);

    if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
      calendar.add(Calendar.DATE, 1);
    }

    pendingIntent = PendingIntent.getBroadcast(
        context,
        SETTINGS.NOTIFICATIONS.DUE_SOON_ID,
        new Intent(context, DueSoonNotificationReceiver.class),
        VERSION.SDK_INT >= VERSION_CODES.M
            ? PendingIntent.FLAG_IMMUTABLE
            : PendingIntent.FLAG_UPDATE_CURRENT
    );

    if (notificationManager != null) {
      notificationManager.cancelAll();
    }
    if (alarmManager == null) {
      return;
    }

    alarmManager.cancel(pendingIntent);
    new Handler(Looper.getMainLooper()).postDelayed(
        () -> alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.getTimeInMillis(),
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        ),
        100
    );
  }

  public void scheduleReminderIfEnabled() {
    if (sharedPrefs.getBoolean(
        SETTINGS.NOTIFICATIONS.DUE_SOON_ENABLE,
        SETTINGS_DEFAULT.NOTIFICATIONS.DUE_SOON_ENABLE
    )) {
      scheduleReminder(sharedPrefs.getString(
          SETTINGS.NOTIFICATIONS.DUE_SOON_TIME,
          NOTIFICATIONS.DUE_SOON_TIME
      ));
    }
  }

  public void setReminderEnabled(boolean enabled) {
    sharedPrefs.edit().putBoolean(SETTINGS.NOTIFICATIONS.DUE_SOON_ENABLE, enabled).apply();
    if (enabled) {
      scheduleReminder(sharedPrefs.getString(
          SETTINGS.NOTIFICATIONS.DUE_SOON_TIME,
          NOTIFICATIONS.DUE_SOON_TIME
      ));
    } else {
      if (notificationManager != null) {
        notificationManager.cancelAll();
      }
      if (pendingIntent != null && alarmManager != null) {
        alarmManager.cancel(pendingIntent);
      }
    }
    startOnBootCompleted(enabled);
  }

  public void startOnBootCompleted(boolean enabled) {
    context.getPackageManager().setComponentEnabledSetting(
        new ComponentName(context, BootReceiver.class),
        enabled
            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP
    );
  }

  public static Notification getNotification(
      Context context,
      String title,
      String text,
      int notificationId,
      String channelId,
      Intent intent
  ) {
    int themeResId = -1;
    Context dynamicColorContext = null;
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    String theme = sharedPrefs.getString(
        Constants.SETTINGS.APPEARANCE.THEME, Constants.SETTINGS_DEFAULT.APPEARANCE.THEME
    );
    switch (theme) {
      case THEME.RED:
        themeResId = R.style.Theme_Grocy_Red;
        break;
      case THEME.YELLOW:
        themeResId = R.style.Theme_Grocy_Yellow;
        break;
      case THEME.LIME:
        themeResId = R.style.Theme_Grocy_Lime;
        break;
      case THEME.GREEN:
        themeResId = R.style.Theme_Grocy_Green;
        break;
      case THEME.TURQUOISE:
        themeResId = R.style.Theme_Grocy_Turquoise;
        break;
      case THEME.TEAL:
        themeResId = R.style.Theme_Grocy_Teal;
        break;
      case THEME.BLUE:
        themeResId = R.style.Theme_Grocy_Blue;
        break;
      case THEME.PURPLE:
        themeResId = R.style.Theme_Grocy_Purple;
        break;
      default:
        if (DynamicColors.isDynamicColorAvailable()) {
          dynamicColorContext = DynamicColors.wrapContextIfAvailable(context);
        } else {
          themeResId = R.style.Theme_Grocy_Green;
        }
        break;
    }

    Context colorContext;
    if (themeResId != -1) {
      colorContext = new ContextThemeWrapper(context, themeResId);
    } else {
      colorContext = dynamicColorContext;
    }

    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
    builder
        .setContentTitle(title)
        .setAutoCancel(true)
        .setColor(ResUtil.getColorAttr(colorContext, R.attr.colorPrimary))
        .setSmallIcon(R.drawable.ic_round_grocy_notification)
        .setContentIntent(
            PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                VERSION.SDK_INT >= VERSION_CODES.M
                    ? PendingIntent.FLAG_IMMUTABLE
                    : PendingIntent.FLAG_UPDATE_CURRENT
            )
        ).setPriority(NotificationCompat.PRIORITY_DEFAULT);
    if (text != null) {
      builder.setContentText(text);
      builder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
    }
    return builder.build();
  }
}

