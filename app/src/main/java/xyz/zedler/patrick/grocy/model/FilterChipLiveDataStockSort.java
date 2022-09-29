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

package xyz.zedler.patrick.grocy.model;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;

public class FilterChipLiveDataStockSort extends FilterChipLiveData {

  public final static int ID_SORT_NAME = 0;
  public final static int ID_SORT_DUE_DATE = 1;
  public final static int ID_ASCENDING = 2;

  public final static String SORT_NAME = "sort_name";
  public final static String SORT_DUE_DATE = "sort_due_date";

  private final Application application;
  private final SharedPreferences sharedPrefs;
  private String sortMode;
  private boolean sortAscending;

  public FilterChipLiveDataStockSort(Application application, Runnable clickListener) {
    this.application = application;
    setItemIdChecked(-1);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    sortMode = sharedPrefs.getString(Constants.PREF.STOCK_SORT_MODE, SORT_NAME);
    sortAscending = sharedPrefs.getBoolean(Constants.PREF.STOCK_SORT_ASCENDING, true);
    setFilterText();
    setItems();
    if (clickListener != null) {
      setMenuItemClickListener(item -> {
        setValues(item.getItemId());
        setItems();
        emitValue();
        clickListener.run();
        return true;
      });
    }
  }

  public String getSortMode() {
    return sortMode;
  }

  public boolean isSortAscending() {
    return sortAscending;
  }

  private void setFilterText() {
    setText(application.getString(
        R.string.property_sort_mode,
        sortMode.equals(SORT_NAME)
            ? application.getString(R.string.property_name)
            : application.getString(R.string.property_due_date_next)
    ));
  }

  public void setValues(int id) {
    if (id == ID_SORT_NAME) {
      sortMode = SORT_NAME;
      setFilterText();
      sharedPrefs.edit().putString(Constants.PREF.STOCK_SORT_MODE, sortMode).apply();
    } else if (id == ID_SORT_DUE_DATE) {
      sortMode = SORT_DUE_DATE;
      setFilterText();
      sharedPrefs.edit().putString(Constants.PREF.STOCK_SORT_MODE, sortMode).apply();
    } else if (id == ID_ASCENDING) {
      sortAscending = !sortAscending;
      sharedPrefs.edit().putBoolean(Constants.PREF.STOCK_SORT_ASCENDING, sortAscending).apply();
    }
  }

  private void setItems() {
    ArrayList<MenuItemData> menuItemDataList = new ArrayList<>();
    menuItemDataList.add(new MenuItemData(
        ID_SORT_NAME,
        0,
        application.getString(R.string.property_name),
        sortMode.equals(SORT_NAME)
    ));
    if (sharedPrefs.getBoolean(PREF.FEATURE_STOCK_BBD_TRACKING, true)) {
      menuItemDataList.add(new MenuItemData(
          ID_SORT_DUE_DATE,
          0,
          application.getString(R.string.property_due_date_next),
          sortMode.equals(SORT_DUE_DATE)
      ));
    }
    menuItemDataList.add(new MenuItemData(
        ID_ASCENDING,
        1,
        application.getString(R.string.action_ascending),
        sortAscending
    ));
    setMenuItemDataList(menuItemDataList);
    setMenuItemGroups(
        new MenuItemGroup(0, true, true),
        new MenuItemGroup(1, true, false)
    );
    emitValue();
  }
}