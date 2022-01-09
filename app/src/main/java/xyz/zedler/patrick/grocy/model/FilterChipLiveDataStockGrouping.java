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
import android.content.SharedPreferences;
import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.Constants.PREF;

public class FilterChipLiveDataStockGrouping extends FilterChipLiveData {

  public final static int ID_GROUPING_NONE = 0;
  public final static int ID_GROUPING_PRODUCT_GROUP = 1;
  public final static int ID_GROUPING_VALUE = 2;
  public final static int ID_GROUPING_DUE_DATE = 3;
  public final static int ID_GROUPING_CALORIES_PER_STOCK = 4;
  public final static int ID_GROUPING_CALORIES = 5;
  public final static int ID_GROUPING_MIN_STOCK_AMOUNT = 6;
  public final static int ID_GROUPING_PARENT_PRODUCT = 7;
  public final static int ID_GROUPING_DEFAULT_LOCATION = 8;

  public final static String GROUPING_NONE = "grouping_none";
  public final static String GROUPING_PRODUCT_GROUP = "grouping_product_group";
  public final static String GROUPING_VALUE = "grouping_value";
  public final static String GROUPING_DUE_DATE = "grouping_due_date";
  public final static String GROUPING_CALORIES_PER_STOCK = "grouping_calories_per_stock";
  public final static String GROUPING_CALORIES = "grouping_calories";
  public final static String GROUPING_MIN_STOCK_AMOUNT = "grouping_min_stock_amount";
  public final static String GROUPING_PARENT_PRODUCT = "grouping_parent_product";
  public final static String GROUPING_DEFAULT_LOCATION = "grouping_default_location";

  private final Application application;
  private final SharedPreferences sharedPrefs;
  private String groupingMode;

  public FilterChipLiveDataStockGrouping(Application application, Runnable clickListener) {
    this.application = application;
    setItemIdChecked(-1);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    groupingMode = sharedPrefs.getString(PREF.STOCK_GROUPING_MODE, GROUPING_NONE);
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
      case GROUPING_PRODUCT_GROUP:
        groupBy = R.string.property_product_group;
        break;
      case GROUPING_VALUE:
        groupBy = R.string.property_value;
        break;
      case GROUPING_DUE_DATE:
        groupBy = R.string.property_due_date_next;
        break;
      case GROUPING_CALORIES_PER_STOCK:
        groupBy = R.string.property_calories_per_unit;
        break;
      case GROUPING_CALORIES:
        groupBy = R.string.property_calories;
        break;
      case GROUPING_MIN_STOCK_AMOUNT:
        groupBy = R.string.property_amount_min_stock;
        break;
      case GROUPING_PARENT_PRODUCT:
        groupBy = R.string.property_parent_product;
        break;
      case GROUPING_DEFAULT_LOCATION:
        groupBy = R.string.property_location_default;
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
    if (id == ID_GROUPING_PRODUCT_GROUP) {
      groupingMode = GROUPING_PRODUCT_GROUP;
    } else if (id == ID_GROUPING_VALUE) {
      groupingMode = GROUPING_VALUE;
    } else if (id == ID_GROUPING_DUE_DATE) {
      groupingMode = GROUPING_DUE_DATE;
    } else if (id == ID_GROUPING_CALORIES_PER_STOCK) {
      groupingMode = GROUPING_CALORIES_PER_STOCK;
    } else if (id == ID_GROUPING_CALORIES) {
      groupingMode = GROUPING_CALORIES;
    } else if (id == ID_GROUPING_MIN_STOCK_AMOUNT) {
      groupingMode = GROUPING_MIN_STOCK_AMOUNT;
    } else if (id == ID_GROUPING_PARENT_PRODUCT) {
      groupingMode = GROUPING_PARENT_PRODUCT;
    } else if (id == ID_GROUPING_DEFAULT_LOCATION) {
      groupingMode = GROUPING_DEFAULT_LOCATION;
    } else {
      groupingMode = GROUPING_NONE;
    }
    setFilterText();
    sharedPrefs.edit().putString(PREF.STOCK_GROUPING_MODE, groupingMode).apply();
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
        ID_GROUPING_PRODUCT_GROUP,
        0,
        application.getString(R.string.property_product_group),
        groupingMode.equals(GROUPING_PRODUCT_GROUP)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_GROUPING_VALUE,
        0,
        application.getString(R.string.property_value),
        groupingMode.equals(GROUPING_VALUE)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_GROUPING_DUE_DATE,
        0,
        application.getString(R.string.property_due_date_next),
        groupingMode.equals(GROUPING_DUE_DATE)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_GROUPING_CALORIES_PER_STOCK,
        0,
        application.getString(R.string.property_calories_per_unit),
        groupingMode.equals(GROUPING_CALORIES_PER_STOCK)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_GROUPING_CALORIES,
        0,
        application.getString(R.string.property_calories),
        groupingMode.equals(GROUPING_CALORIES)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_GROUPING_MIN_STOCK_AMOUNT,
        0,
        application.getString(R.string.property_amount_min_stock),
        groupingMode.equals(GROUPING_MIN_STOCK_AMOUNT)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_GROUPING_PARENT_PRODUCT,
        0,
        application.getString(R.string.property_parent_product),
        groupingMode.equals(GROUPING_PARENT_PRODUCT)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_GROUPING_DEFAULT_LOCATION,
        0,
        application.getString(R.string.property_location_default),
        groupingMode.equals(GROUPING_DEFAULT_LOCATION)
    ));
    setMenuItemDataList(menuItemDataList);
    setMenuItemGroups(new MenuItemGroup(0, true, true));
    emitValue();
  }
}