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

import android.view.MenuItem.OnMenuItemClickListener;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;

public class FilterChipLiveData extends MutableLiveData<FilterChipLiveData> {

  private String text;
  @ColorRes private int colorBackground = R.color.grey100;
  @ColorRes private int colorStroke = R.color.grey400;
  @DrawableRes private int drawable = -1;
  private int itemIdChecked = -1;
  private ArrayList<MenuItemData> menuItemDataList;
  private OnMenuItemClickListener menuItemClickListener;

  public void emitValue() {
    setValue(this);  // update view (because this sends new value to observer)
  }

  public FilterChipLiveData setText(String text) {
    this.text = text;
    return this;
  }

  public String getText() {
    return text;
  }

  @ColorRes public int getColorBackground() {
    return colorBackground;
  }

  public void setColorBackground(@ColorRes int colorBackground) {
    this.colorBackground = colorBackground;
  }

  public int getColorStroke() {
    return colorStroke;
  }

  public void setColorStroke(int colorStroke) {
    this.colorStroke = colorStroke;
  }

  public void setColorStrokeToBackground() {
    colorStroke = colorBackground;
  }

  @DrawableRes public int getDrawable() {
    return drawable;
  }

  public void setDrawable(@DrawableRes int drawable) {
    this.drawable = drawable;
  }

  public boolean hasPopupMenu() {
    return menuItemDataList != null;
  }

  public ArrayList<MenuItemData> getMenuItemDataList() {
    return menuItemDataList;
  }

  public void setMenuItemDataList(
      ArrayList<MenuItemData> menuItemDataList
  ) {
    this.menuItemDataList = menuItemDataList;
  }

  public OnMenuItemClickListener getMenuItemClickListener() {
    return menuItemClickListener;
  }

  public void setMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
    this.menuItemClickListener = menuItemClickListener;
  }

  public int getItemIdChecked() {
    return itemIdChecked;
  }

  public void setItemIdChecked(int itemIdChecked) {
    this.itemIdChecked = itemIdChecked;
  }

  public static class MenuItemData {
    private final int itemId;
    private final String text;

    public MenuItemData(int itemId, String text) {
      this.itemId = itemId;
      this.text = text;
    }

    public int getItemId() {
      return itemId;
    }

    public String getText() {
      return text;
    }
  }
}