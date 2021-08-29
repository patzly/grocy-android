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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.app.Application;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;

public class FilterChipLiveDataStockStatus extends FilterChipLiveData {

  public final static int STATUS_ALL = 0;
  public final static int STATUS_DUE_SOON = 1;
  public final static int STATUS_OVERDUE = 2;
  public final static int STATUS_EXPIRED = 3;
  public final static int STATUS_BELOW_MIN = 4;
  public final static int STATUS_IN_STOCK = 5;

  private final Application application;
  private int dueSoonCount = 0;
  private int overdueCount = 0;
  private int expiredCount = 0;
  private int belowStockCount = 0;
  private int inStockCount = 0;

  public FilterChipLiveDataStockStatus(Application application, Runnable clickListener) {
    this.application = application;
    setStatus(STATUS_ALL, null);
    if (clickListener != null) {
      setMenuItemClickListener(item -> {
        setStatus(item.getItemId(), item.getTitle().toString());
        emitValue();
        clickListener.run();
        return true;
      });
    }
  }

  public int getStatus() {
    return getItemIdChecked();
  }

  public FilterChipLiveDataStockStatus setStatus(int status, @Nullable String text) {
    if (status == STATUS_DUE_SOON) {
      setColorBackground(R.color.retro_yellow_bg);
    } else if (status == STATUS_OVERDUE) {
      setColorBackground(R.color.retro_dirt);
    } else if (status == STATUS_EXPIRED) {
      setColorBackground(R.color.retro_red_bg_black);
    } else if (status == STATUS_BELOW_MIN) {
      setColorBackground(R.color.retro_blue_bg);
    } else if (status == STATUS_IN_STOCK) {
      setColorBackground(R.color.retro_green_bg_black);
    } else {
      setColorBackground(R.color.grey100);
    }
    if (status == STATUS_ALL) {
      setColorStroke(R.color.grey400);
      setText(application.getString(R.string.property_status));
    } else {
      setColorStrokeToBackground();
      assert text != null;
      setText(text);
    }
    setItemIdChecked(status);
    return this;
  }

  public FilterChipLiveDataStockStatus setDueSoonCount(int dueSoonCount) {
    this.dueSoonCount = dueSoonCount;
    return this;
  }

  public FilterChipLiveDataStockStatus setOverdueCount(int overdueCount) {
    this.overdueCount = overdueCount;
    return this;
  }

  public FilterChipLiveDataStockStatus setExpiredCount(int expiredCount) {
    this.expiredCount = expiredCount;
    return this;
  }

  public FilterChipLiveDataStockStatus setBelowStockCount(int belowStockCount) {
    this.belowStockCount = belowStockCount;
    return this;
  }

  public FilterChipLiveDataStockStatus setInStockCount(int inStockCount) {
    this.inStockCount = inStockCount;
    return this;
  }

  public void emitCounts() {
    ArrayList<MenuItemData> menuItemDataList = new ArrayList<>();
    menuItemDataList.add(new MenuItemData(
        STATUS_ALL,
        application.getString(R.string.action_no_filter)
    ));
    menuItemDataList.add(new MenuItemData(
        STATUS_DUE_SOON,
        getQuString(R.plurals.msg_due_products, dueSoonCount)
    ));
    menuItemDataList.add(new MenuItemData(
        STATUS_OVERDUE,
        getQuString(R.plurals.msg_overdue_products, overdueCount)
    ));
    menuItemDataList.add(new MenuItemData(
        STATUS_EXPIRED,
        getQuString(R.plurals.msg_expired_products, expiredCount)
    ));
    menuItemDataList.add(new MenuItemData(
        STATUS_BELOW_MIN,
        getQuString(R.plurals.msg_missing_products, belowStockCount)
    ));
    menuItemDataList.add(new MenuItemData(
        STATUS_IN_STOCK,
        getQuString(R.plurals.msg_in_stock_products, inStockCount)
    ));
    setMenuItemDataList(menuItemDataList);
    emitValue();
  }

  private String getQuString(@PluralsRes int string, int count) {
    return application.getResources().getQuantityString(string, count, count);
  }
}