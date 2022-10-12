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
import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.Constants.PREF;

public class FilterChipLiveDataStockEntriesGrouping extends FilterChipLiveData {

  public final static int ID_GROUPING_NONE = 0;
  public final static int ID_GROUPING_PRODUCT = 1;
  public final static int ID_GROUPING_DUE_DATE = 2;
  public final static int ID_GROUPING_PURCHASED_DATE = 3;
  public final static int ID_GROUPING_LOCATION = 4;
  public final static int ID_GROUPING_STORE = 5;

  public final static String GROUPING_NONE = "grouping_none";
  public final static String GROUPING_PRODUCT = "grouping_product";
  public final static String GROUPING_DUE_DATE = "grouping_due_date";
  public final static String GROUPING_PURCHASED_DATE = "grouping_purchased_date";
  public final static String GROUPING_LOCATION = "grouping_location";
  public final static String GROUPING_STORE = "grouping_store";

  private final Application application;
  private final SharedPreferences sharedPrefs;
  private String groupingMode;

  public FilterChipLiveDataStockEntriesGrouping(Application application, Runnable clickListener) {
    this.application = application;
    setItemIdChecked(-1);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    groupingMode = sharedPrefs.getString(PREF.STOCK_ENTRIES_GROUPING_MODE, GROUPING_NONE);
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

  public String getGroupingMode() {
    return groupingMode;
  }

  private void setFilterText() {
    @StringRes int groupBy;
    switch (groupingMode) {
      case GROUPING_PRODUCT:
        groupBy = R.string.property_product;
        break;
      case GROUPING_DUE_DATE:
        groupBy = R.string.property_due_date;
        break;
      case GROUPING_PURCHASED_DATE:
        groupBy = R.string.property_purchased_date;
        break;
      case GROUPING_LOCATION:
        groupBy = R.string.property_location;
        break;
      case GROUPING_STORE:
        groupBy = R.string.property_store;
        break;
      default:
        groupBy = R.string.subtitle_none;
        break;
    }
    setText(application.getString(
        R.string.property_group_by,
        application.getString(groupBy)
    ));
  }

  public void setValues(int id) {
    if (id == ID_GROUPING_PRODUCT) {
      groupingMode = GROUPING_PRODUCT;
    } else if (id == ID_GROUPING_DUE_DATE) {
      groupingMode = GROUPING_DUE_DATE;
    } else if (id == ID_GROUPING_PURCHASED_DATE) {
      groupingMode = GROUPING_PURCHASED_DATE;
    } else if (id == ID_GROUPING_LOCATION) {
      groupingMode = GROUPING_LOCATION;
    } else if (id == ID_GROUPING_STORE) {
      groupingMode = GROUPING_STORE;
    } else {
      groupingMode = GROUPING_NONE;
    }
    setFilterText();
    sharedPrefs.edit().putString(PREF.STOCK_ENTRIES_GROUPING_MODE, groupingMode).apply();
  }

  private void setItems() {
    ArrayList<MenuItemData> menuItemDataList = new ArrayList<>();
    menuItemDataList.add(new MenuItemData(
        ID_GROUPING_NONE,
        0,
        application.getString(R.string.subtitle_none),
        groupingMode.equals(GROUPING_NONE)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_GROUPING_PRODUCT,
        0,
        application.getString(R.string.property_product),
        groupingMode.equals(GROUPING_PRODUCT)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_GROUPING_DUE_DATE,
        0,
        application.getString(R.string.property_due_date),
        groupingMode.equals(GROUPING_DUE_DATE)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_GROUPING_PURCHASED_DATE,
        0,
        application.getString(R.string.property_purchased_date),
        groupingMode.equals(GROUPING_PURCHASED_DATE)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_GROUPING_LOCATION,
        0,
        application.getString(R.string.property_location),
        groupingMode.equals(GROUPING_LOCATION)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_GROUPING_STORE,
        0,
        application.getString(R.string.property_store),
        groupingMode.equals(GROUPING_STORE)
    ));
    setMenuItemDataList(menuItemDataList);
    setMenuItemGroups(new MenuItemGroup(0, true, true));
    emitValue();
  }
}