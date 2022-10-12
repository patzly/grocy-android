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

public class FilterChipLiveDataStockExtraField extends FilterChipLiveData {

  public final static int ID_EXTRA_FIELD_NONE = 0;
  public final static int ID_EXTRA_FIELD_VALUE = 1;
  public final static int ID_EXTRA_FIELD_CALORIES_UNIT = 2;
  public final static int ID_EXTRA_FIELD_CALORIES_TOTAL = 3;
  public final static int ID_EXTRA_FIELD_AVERAGE_PRICE = 4;
  public final static int ID_EXTRA_FIELD_LAST_PRICE = 5;

  public final static String EXTRA_FIELD_NONE = "extra_field_none";
  public final static String EXTRA_FIELD_VALUE = "extra_field_value";
  public final static String EXTRA_FIELD_CALORIES_UNIT = "extra_field_calories_unit";
  public final static String EXTRA_FIELD_CALORIES_TOTAL = "extra_field_calories_total";
  public final static String EXTRA_FIELD_AVERAGE_PRICE = "extra_field_average_price";
  public final static String EXTRA_FIELD_LAST_PRICE = "extra_field_last_price";

  private final Application application;
  private final SharedPreferences sharedPrefs;
  private String extraField;

  public FilterChipLiveDataStockExtraField(Application application, Runnable clickListener) {
    this.application = application;
    setItemIdChecked(-1);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    extraField = sharedPrefs.getString(PREF.STOCK_EXTRA_FIELD, EXTRA_FIELD_NONE);
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

  public String getExtraField() {
    return extraField;
  }

  private void setFilterText() {
    @StringRes int groupBy;
    switch (extraField) {
      case EXTRA_FIELD_VALUE:
        groupBy = R.string.property_value;
        break;
      case EXTRA_FIELD_CALORIES_UNIT:
        groupBy = R.string.property_calories_unit;
        break;
      case EXTRA_FIELD_CALORIES_TOTAL:
        groupBy = R.string.property_calories_total;
        break;
      case EXTRA_FIELD_AVERAGE_PRICE:
        groupBy = R.string.property_price_average;
        break;
      case EXTRA_FIELD_LAST_PRICE:
        groupBy = R.string.property_last_price;
        break;
      default:
        groupBy = R.string.subtitle_none;
        break;
    }
    setText(application.getString(
        R.string.property_extra_field,
        application.getString(groupBy)
    ));
  }

  public void setValues(int id) {
    if (id == ID_EXTRA_FIELD_VALUE) {
      extraField = EXTRA_FIELD_VALUE;
    } else if (id == ID_EXTRA_FIELD_CALORIES_UNIT) {
      extraField = EXTRA_FIELD_CALORIES_UNIT;
    } else if (id == ID_EXTRA_FIELD_CALORIES_TOTAL) {
      extraField = EXTRA_FIELD_CALORIES_TOTAL;
    } else if (id == ID_EXTRA_FIELD_AVERAGE_PRICE) {
      extraField = EXTRA_FIELD_AVERAGE_PRICE;
    } else if (id == ID_EXTRA_FIELD_LAST_PRICE) {
      extraField = EXTRA_FIELD_LAST_PRICE;
    } else {
      extraField = EXTRA_FIELD_NONE;
    }
    setFilterText();
    sharedPrefs.edit().putString(PREF.STOCK_EXTRA_FIELD, extraField).apply();
  }

  private void setItems() {
    ArrayList<MenuItemData> menuItemDataList = new ArrayList<>();
    menuItemDataList.add(new MenuItemData(
        ID_EXTRA_FIELD_NONE,
        0,
        application.getString(R.string.subtitle_none),
        extraField.equals(EXTRA_FIELD_NONE)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_EXTRA_FIELD_VALUE,
        0,
        application.getString(R.string.property_value),
        extraField.equals(EXTRA_FIELD_VALUE)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_EXTRA_FIELD_CALORIES_UNIT,
            0,
            application.getString(R.string.property_calories_unit),
            extraField.equals(EXTRA_FIELD_CALORIES_UNIT)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_EXTRA_FIELD_CALORIES_TOTAL,
            0,
            application.getString(R.string.property_calories_total),
            extraField.equals(EXTRA_FIELD_CALORIES_TOTAL)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_EXTRA_FIELD_AVERAGE_PRICE,
        0,
        application.getString(R.string.property_price_average),
        extraField.equals(EXTRA_FIELD_AVERAGE_PRICE)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_EXTRA_FIELD_LAST_PRICE,
        0,
        application.getString(R.string.property_last_price),
        extraField.equals(EXTRA_FIELD_LAST_PRICE)
    ));
    setMenuItemDataList(menuItemDataList);
    setMenuItemGroups(new MenuItemGroup(0, true, true));
    emitValue();
  }
}