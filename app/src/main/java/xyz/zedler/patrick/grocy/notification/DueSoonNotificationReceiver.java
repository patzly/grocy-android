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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.NOTIFICATIONS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.fragment.StockOverviewFragmentArgs;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockStatus;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.VolatileItem;
import xyz.zedler.patrick.grocy.util.NavUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ReminderUtil;

public class DueSoonNotificationReceiver extends BroadcastReceiver {

  public void onReceive(Context context, Intent intent) {
    NotificationManager notificationManager = (NotificationManager) context.getSystemService(
        Context.NOTIFICATION_SERVICE
    );
    if (notificationManager == null) {
      return;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      CharSequence name = context.getString(R.string.setting_notifications_due_soon);
      String description = context.getString(R.string.setting_notifications_due_soon_description);
      int importance = NotificationManager.IMPORTANCE_HIGH;
      NotificationChannel channel = new NotificationChannel(
          NOTIFICATIONS.DUE_SOON_CHANNEL, name, importance
      );
      channel.setDescription(description);
      notificationManager.createNotificationChannel(channel);
    }
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    DownloadHelper dlHelper = new DownloadHelper(context, DueSoonNotificationReceiver.class.getSimpleName());

    VolatileItem.getVolatile(dlHelper, response -> {
      try {
        Type typeStockItem = new TypeToken<List<StockItem>>() {
        }.getType();
        JSONObject jsonObject = new JSONObject(response);
        ArrayList<StockItem> dueItems = (new Gson()).fromJson(
            jsonObject.getJSONArray("due_products").toString(), typeStockItem
        );

        if (dueItems.size() == 0) return;

        String days = sharedPrefs.getString(
            STOCK.DUE_SOON_DAYS,
            SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS
        );
        int daysInt;
        if (NumUtil.isStringInt(days)) {
          daysInt = Integer.parseInt(days);
        } else {
          daysInt = Integer.parseInt(SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS);
        }
        String titleText = context.getResources().getQuantityString(
            R.plurals.description_overview_stock_due_soon,
            dueItems.size(), dueItems.size(), daysInt
        );

        Uri uri = NavUtil.getUriWithArgs(
            context.getString(R.string.deep_link_stockOverviewFragment),
            new StockOverviewFragmentArgs.Builder()
                .setStatusFilterId(String.valueOf(FilterChipLiveDataStockStatus.STATUS_NOT_FRESH))
                .build().toBundle()
        );
        Intent notificationIntent = new Intent(Intent.ACTION_VIEW, uri);
        notificationIntent.setClass(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        notificationManager.notify(NOTIFICATIONS.DUE_SOON_ID, ReminderUtil.getNotification(
            context,
            titleText,
            context.getString(R.string.notification_due_soon_content),
            NOTIFICATIONS.DUE_SOON_ID,
            NOTIFICATIONS.DUE_SOON_CHANNEL,
            notificationIntent
        ));
        dlHelper.destroy();
      } catch (JSONException e) {
        e.printStackTrace();
        // exception handling
        dlHelper.destroy();
      }
    }, error -> dlHelper.destroy());
  }
}
