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

import android.view.MenuItem.OnMenuItemClickListener;
import androidx.annotation.DrawableRes;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;

public class FilterChipLiveData extends MutableLiveData<FilterChipLiveData> {

  private boolean active = false;
  private String text;
  @DrawableRes private int drawable = -1;
  private int itemIdChecked = -1;
  private ArrayList<MenuItemData> menuItemDataList;
  private MenuItemGroup[] menuItemGroupArray;
  private OnMenuItemClickListener menuItemClickListener;

  public void emitValue() {
    setValue(this);  // update view (because this sends new value to observer)
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public FilterChipLiveData setText(String text) {
    this.text = text;
    return this;
  }

  public String getText() {
    return text;
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

  public MenuItemGroup[] getMenuItemGroupArray() {
    return menuItemGroupArray;
  }

  public void setMenuItemGroups(MenuItemGroup... menuItemGroupArray) {
    this.menuItemGroupArray = menuItemGroupArray;
  }

  public int getItemIdChecked() {
    return itemIdChecked;
  }

  public void setItemIdChecked(int itemIdChecked) {
    this.itemIdChecked = itemIdChecked;
  }

  public static class MenuItemData {
    private final int itemId;
    private final int groupId;
    private final String text;
    private boolean checked = false;

    public MenuItemData(int itemId, int groupId, String text) {
      this.itemId = itemId;
      this.groupId = groupId;
      this.text = text;
    }

    public MenuItemData(int itemId, int groupId, String text, boolean checked) {
      this(itemId, groupId, text);
      this.checked = checked;
    }

    public int getItemId() {
      return itemId;
    }

    public int getGroupId() {
      return groupId;
    }

    public String getText() {
      return text;
    }

    public boolean isChecked() {
      return checked;
    }
  }

  public static class MenuItemGroup {
    private final int groupId;
    private final boolean checkable;
    private final boolean exclusive;

    public MenuItemGroup(int groupId, boolean checkable, boolean exclusive) {
      this.groupId = groupId;
      this.checkable = checkable;
      this.exclusive = exclusive;
    }

    public int getGroupId() {
      return groupId;
    }

    public boolean isCheckable() {
      return checkable;
    }

    public boolean isExclusive() {
      return exclusive;
    }
  }

  public interface Listener {
    FilterChipLiveData getData();
  }
}