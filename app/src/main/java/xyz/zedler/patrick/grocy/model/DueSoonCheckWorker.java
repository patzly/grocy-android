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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
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
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.NETWORK;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS_DEFAULT;

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
    dlHelper.getVolatile(future, future);

    try {
      String response = future.get(timeout, TimeUnit.SECONDS); // this will block

      ArrayList<StockItem> dueItems = new ArrayList<>();
      Type typeStockItem = new TypeToken<List<StockItem>>() {
      }.getType();
      JSONObject jsonObject = new JSONObject(response);
      dueItems = (new Gson()).fromJson(
          jsonObject.getJSONArray("due_products").toString(), typeStockItem
      );

      Log.i(TAG, "doWork: " + dueItems);

      dlHelper.destroy();
      return Result.success();
    } catch (InterruptedException | TimeoutException | ExecutionException | JSONException e) {
      e.printStackTrace();
      // exception handling
      dlHelper.destroy();
      return Result.failure();
    }
  }

}
