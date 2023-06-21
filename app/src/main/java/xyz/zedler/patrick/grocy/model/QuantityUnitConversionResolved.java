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

package xyz.zedler.patrick.grocy.model;

import android.annotation.SuppressLint;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import com.google.gson.reflect.TypeToken;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.util.QuantityUnitConversionUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

@Entity(tableName = "quantity_unit_conversion_resolved_table")
public class QuantityUnitConversionResolved extends QuantityUnitConversion {

  public static QuantityUnitConversionResolved findConversion(
      List<QuantityUnitConversionResolved> conversionsResolved,
      int productId,
      int fromQuId,
      int toQuId
  ) {
    String productIdStr = String.valueOf(productId);
    for (QuantityUnitConversionResolved tmpConversion : conversionsResolved) {
      if (productIdStr.equals(tmpConversion.getProductId())
          && tmpConversion.getFromQuId() == fromQuId
          && tmpConversion.getToQuId() == toQuId) {
        return tmpConversion;
      }
    }
    return null;
  }

  @SuppressLint("CheckResult")
  public static QueueItem updateQuantityUnitConversions(
      DownloadHelper dlHelper,
      String dbChangedTime,
      boolean forceUpdate,
      List<Product> products,
      OnObjectsResponseListener<QuantityUnitConversionResolved> onResponseListener
  ) {
    String lastTime = !forceUpdate ? dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS_RESOLVED, null
    ) : null;
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnMultiTypeErrorListener errorListener,
            @Nullable String uuid
        ) {
          boolean isServerVersion4 = VersionUtil.isGrocyServerMin400(dlHelper.sharedPrefs);
          dlHelper.get(
              dlHelper.grocyApi.getObjects(isServerVersion4
                  ? ENTITY.QUANTITY_UNIT_CONVERSIONS_RESOLVED : ENTITY.QUANTITY_UNIT_CONVERSIONS),
              uuid,
              response -> {
                Type type = new TypeToken<List<QuantityUnitConversionResolved>>() {
                }.getType();
                List<QuantityUnitConversionResolved> conversionsResolved;
                if (isServerVersion4) {
                  List<QuantityUnitConversionResolved> conversionsResolvedNotForDb = dlHelper.gson
                      .fromJson(response, type);
                  if (dlHelper.debug) {
                    Log.i(dlHelper.tag, "download QuantityUnitConversionsResolved: "
                        + conversionsResolvedNotForDb);
                  }
                  conversionsResolved = new ArrayList<>();
                  int id = 0;
                  for (QuantityUnitConversionResolved conversion : conversionsResolvedNotForDb) {
                    conversion.setId(id);
                    conversionsResolved.add(conversion);
                    id++;
                  }
                } else {
                  List<QuantityUnitConversion> conversions
                      = dlHelper.gson.fromJson(response, type);
                  if (dlHelper.debug) {
                    Log.i(dlHelper.tag, "download QuantityUnitConversions: "
                        + conversions);
                  }
                  conversionsResolved = QuantityUnitConversionUtil
                      .calculateConversions(conversions, products);
                  if (dlHelper.debug) {
                    Log.i(dlHelper.tag, "resolved QuantityUnitConversions: "
                        + conversionsResolved);
                  }
                }
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.quantityUnitConversionResolvedDao()
                      .deleteConversionsResolved().blockingSubscribe();
                  dlHelper.appDatabase.quantityUnitConversionResolvedDao()
                      .insertConversionsResolved(conversionsResolved).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS_RESOLVED, dbChangedTime).apply();
                  return true;
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(conversionsResolved);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe(ignored -> {}, throwable -> {
                      if (errorListener != null) {
                        errorListener.onError(throwable);
                      }
                    });
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (dlHelper.debug) {
        Log.i(dlHelper.tag, "downloadData: skipped QuantityUnitConversions download");
      }
      return null;
    }
  }
}
