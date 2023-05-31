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

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.android.volley.toolbox.RequestFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.NETWORK;
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

public class DueSoonCheckWorker extends Worker {

  private final static String TAG = DueSoonCheckWorker.class.getSimpleName();

  public DueSoonCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
    super(context, params);
  }

  @NonNull
  @Override
  public Result doWork() {

    DownloadHelper dlHelper = new DownloadHelper(getApplicationContext(), TAG);
    int timeout = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        .getInt(NETWORK.LOADING_TIMEOUT, SETTINGS_DEFAULT.NETWORK.LOADING_TIMEOUT);

    RequestFuture<String> future = RequestFuture.newFuture();
    VolatileItem.getVolatile(dlHelper, future, future);

    try {
      String response = future.get(timeout, TimeUnit.SECONDS); // this will block

      Type typeStockItem = new TypeToken<List<StockItem>>() {
      }.getType();
      JSONObject jsonObject = new JSONObject(response);
      ArrayList<StockItem> dueItems = (new Gson()).fromJson(
          jsonObject.getJSONArray("due_products").toString(), typeStockItem
      );

      if (dueItems.size() == 0) return Result.success();

      Bitmap bitmap = getBitmapFromVectorDrawable(getApplicationContext(), R.drawable.ic_round_grocy);

      Uri uri = NavUtil.getUriWithArgs(
          getApplicationContext().getString(R.string.deep_link_stockOverviewFragment),
          new StockOverviewFragmentArgs.Builder()
              .setStatusFilterId(String.valueOf(FilterChipLiveDataStockStatus.STATUS_DUE_SOON))
              .build().toBundle()
      );
      Intent intent = new Intent(Intent.ACTION_VIEW, uri);
      intent.setClass(getApplicationContext(), MainActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pendingIntent = PendingIntent
          .getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

      String days = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(
          STOCK.DUE_SOON_DAYS,
          SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS
      );
      int daysInt;
      if (NumUtil.isStringInt(days)) {
        daysInt = Integer.parseInt(days);
      } else {
        daysInt = Integer.parseInt(SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS);
      }
      String titleText = getApplicationContext().getResources().getQuantityString(
          R.plurals.description_overview_stock_due_soon,
          dueItems.size(), dueItems.size(), daysInt
      );

      NotificationCompat.Builder builder = new NotificationCompat
          .Builder(getApplicationContext(), "xyz.zedler.patrick.grocy.due_soon")
          .setSmallIcon(R.drawable.ic_round_grocy_notification)
          .setContentTitle(titleText)
          .setContentText(getApplicationContext().getString(R.string.notification_due_soon_content))
          .setLargeIcon(bitmap)
          .setContentIntent(pendingIntent)
          .setAutoCancel(true)
          .setPriority(NotificationCompat.PRIORITY_HIGH);

      NotificationManagerCompat notificationManager = NotificationManagerCompat
          .from(getApplicationContext());

      int lastId = 1;
      notificationManager.notify(lastId+1, builder.build());

      dlHelper.destroy();
      return Result.success();
    } catch (InterruptedException | TimeoutException | ExecutionException | JSONException e) {
      e.printStackTrace();
      // exception handling
      dlHelper.destroy();
      return Result.failure();
    }
  }

  public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
    Drawable drawable = ContextCompat.getDrawable(context, drawableId);
    if (drawable == null) return null;
    Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
        drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);

    return Bitmap.createScaledBitmap(
            bitmap,
            context.getResources()
                .getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
            context.getResources()
                .getDimensionPixelSize(android.R.dimen.notification_large_icon_height),
            true
        );
  }
}
