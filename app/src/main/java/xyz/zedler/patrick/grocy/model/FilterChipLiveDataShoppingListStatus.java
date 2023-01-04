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

import android.app.Application;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;

public class FilterChipLiveDataShoppingListStatus extends FilterChipLiveData {

  public final static int STATUS_ALL = 0;
  public final static int STATUS_BELOW_MIN = 1;
  public final static int STATUS_UNDONE = 2;
  public final static int STATUS_DONE = 3;

  private final Application application;
  private int belowStockCount = 0;
  private int undoneCount = 0;
  private int doneCount = 0;

  public FilterChipLiveDataShoppingListStatus(Application application, Runnable clickListener) {
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

  public FilterChipLiveDataShoppingListStatus setStatus(int status, @Nullable String text) {
    if (status == STATUS_ALL) {
      setActive(false);
      setText(application.getString(R.string.property_status));
    } else {
      setActive(true);
      assert text != null;
      setText(text);
    }
    setItemIdChecked(status);
    return this;
  }

  public FilterChipLiveDataShoppingListStatus setBelowStockCount(int belowStockCount) {
    this.belowStockCount = belowStockCount;
    return this;
  }

  public FilterChipLiveDataShoppingListStatus setUndoneCount(int undoneCount) {
    this.undoneCount = undoneCount;
    return this;
  }

  public FilterChipLiveDataShoppingListStatus setDoneCount(int doneCount) {
    this.doneCount = doneCount;
    return this;
  }

  public void emitCounts() {
    ArrayList<MenuItemData> menuItemDataList = new ArrayList<>();
    menuItemDataList.add(new MenuItemData(
        STATUS_ALL,
        0,
        application.getString(R.string.action_no_filter)
    ));
    menuItemDataList.add(new MenuItemData(
        STATUS_BELOW_MIN,
        0,
        getQuString(R.plurals.msg_missing_products, belowStockCount)
    ));
    menuItemDataList.add(new MenuItemData(
        STATUS_UNDONE,
        0,
        getQuString(R.plurals.msg_undone_items, undoneCount)
    ));
    menuItemDataList.add(new MenuItemData(
        STATUS_DONE,
        0,
        getQuString(R.plurals.msg_done_items, doneCount)
    ));
    setMenuItemDataList(menuItemDataList);
    setMenuItemGroups(new MenuItemGroup(0, true, true));
    for (MenuItemData menuItemData : menuItemDataList) {
      if (getItemIdChecked() != STATUS_ALL && getItemIdChecked() == menuItemData.getItemId()) {
        setText(menuItemData.getText());
      }
    }
    emitValue();
  }

  private String getQuString(@PluralsRes int string, int count) {
    return application.getResources().getQuantityString(string, count, count);
  }
}