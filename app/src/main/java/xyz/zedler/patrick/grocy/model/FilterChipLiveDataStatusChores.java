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
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.app.Application;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;

public class FilterChipLiveDataStatusChores extends FilterChipLiveData {

  public final static int STATUS_ALL = 0;
  public final static int STATUS_DUE_TODAY = 1;
  public final static int STATUS_DUE_SOON = 2;
  public final static int STATUS_OVERDUE = 3;
  public final static int STATUS_DUE = 4;

  private final Application application;
  private int dueTodayCount = 0;
  private int dueSoonCount = 0;
  private int overdueCount = 0;
  private int dueCount = 0;

  public FilterChipLiveDataStatusChores(Application application, Runnable clickListener) {
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

  public void setStatus(int status, @Nullable String text) {
    if (status == STATUS_ALL) {
      setActive(false);
      setText(application.getString(R.string.property_status));
    } else if (status == STATUS_DUE) {
      setActive(true);
      setText(getQuString(R.plurals.msg_due_tasks_short, dueCount));
    } else {
      setActive(true);
      assert text != null;
      setText(text);
    }
    setItemIdChecked(status);
  }

  public FilterChipLiveDataStatusChores setDueTodayCount(int dueTodayCount) {
    this.dueTodayCount = dueTodayCount;
    return this;
  }

  public FilterChipLiveDataStatusChores setDueSoonCount(int dueSoonCount) {
    this.dueSoonCount = dueSoonCount;
    return this;
  }

  public FilterChipLiveDataStatusChores setOverdueCount(int overdueCount) {
    this.overdueCount = overdueCount;
    return this;
  }

  public FilterChipLiveDataStatusChores setDueCount(int dueCount) {
    this.dueCount = dueCount;
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
        STATUS_DUE,
        1,
        getQuString(R.plurals.msg_due_tasks_long, dueCount)
    ));
    menuItemDataList.add(new MenuItemData(
        STATUS_OVERDUE,
        1,
        getQuString(R.plurals.msg_overdue_tasks, overdueCount)
    ));
    menuItemDataList.add(new MenuItemData(
        STATUS_DUE_TODAY,
        1,
        getQuString(R.plurals.msg_due_today_tasks, dueTodayCount)
    ));
    menuItemDataList.add(new MenuItemData(
        STATUS_DUE_SOON,
        2,
        getQuString(R.plurals.msg_due_tasks, dueSoonCount)
    ));
    setMenuItemDataList(menuItemDataList);
    setMenuItemGroups(
        new MenuItemGroup(0, true, true),
        new MenuItemGroup(1, true, true),
        new MenuItemGroup(2, true, true)
    );
    setStatus(getStatus(), null);
    emitValue();
  }

  private String getQuString(@PluralsRes int string, int count) {
    return application.getResources().getQuantityString(string, count, count);
  }
}