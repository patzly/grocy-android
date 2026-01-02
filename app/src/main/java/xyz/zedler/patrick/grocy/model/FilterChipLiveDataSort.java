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
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import xyz.zedler.patrick.grocy.R;

public class FilterChipLiveDataSort extends FilterChipLiveData {

  public final static int ID_ASCENDING = 0;
  public final static int ID_START_OPTIONS = 1;

  private final Application application;
  private final SharedPreferences sharedPrefs;
  private final List<SortOption> sortOptions;
  private List<Userfield> userfields;
  private final String prefKey;
  private final String prefKeyAscending;
  private final String defaultOptionKey;
  private String sortMode;
  private boolean sortAscending;
  private final int idStartUserfields;

  public FilterChipLiveDataSort(
      Application application,
      String prefKey,
      String prefKeyAscending,
      Runnable clickListener,
      String defaultOptionKey,
      SortOption... sortOptions
  ) {
    this.application = application;
    this.prefKey = prefKey;
    this.prefKeyAscending = prefKeyAscending;
    this.defaultOptionKey = defaultOptionKey;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    sortMode = sharedPrefs.getString(prefKey, defaultOptionKey);
    sortAscending = sharedPrefs.getBoolean(prefKeyAscending, true);
    this.sortOptions = filterNullElements(sortOptions);
    idStartUserfields = ID_START_OPTIONS + this.sortOptions.size();
    setItemIdChecked(-1);
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

  private static <T> ArrayList<T> filterNullElements(T[] objects) {
    ArrayList<T> arrayList = new ArrayList<>();
    for (T obj : objects) {
      if (obj != null) {
        arrayList.add(obj);
      }
    }
    return arrayList;
  }

  public String getSortMode() {
    return sortMode;
  }

  public boolean isSortAscending() {
    return sortAscending;
  }

  private void setFilterText() {
    String sortBy = null;
    String defaultOptionName = null;
    for (SortOption sortOption : sortOptions) {
      if (sortOption == null) continue;
      if (sortMode.equals(sortOption.key)) {
        sortBy = sortOption.name;
        break;
      }
      if (Objects.equals(sortOption.key, defaultOptionKey)) {
        defaultOptionName = sortOption.name;
      }
    }
    if (sortMode.startsWith(Userfield.NAME_PREFIX) && userfields != null) {
      for (Userfield userfield : userfields) {
        if (userfield == null) continue;
        if (sortMode.equals(Userfield.NAME_PREFIX + userfield.getName())) {
          sortBy = userfield.getCaption();
          break;
        }
      }
    }
    if (sortBy == null) {
      sortBy = defaultOptionName;
    }
    setText(application.getString(R.string.property_sort_mode, sortBy));
  }

  public void setUserfields(List<Userfield> userfields, String... displayedEntities) {
    this.userfields = new ArrayList<>();
    int userfieldId = idStartUserfields;
    for (Userfield userfield : userfields) {
      if (userfield == null) continue;
      if (displayedEntities.length > 0) {
        boolean isDisplayed = false;
        for (String displayedEntity : displayedEntities) {
          if (displayedEntity.equals(userfield.getEntity())
              && userfield.getShowAsColumnInTablesBoolean()) {
            isDisplayed = true;
            break;
          }
        }
        if (!isDisplayed) continue;
      }
      Userfield clonedField = new Userfield(userfield);
      clonedField.setId(userfieldId);
      this.userfields.add(clonedField);
      userfieldId++;
    }
    setFilterText();
    setItems();
  }

  public void setValues(int id) {
    if (id == ID_ASCENDING) {
      sortAscending = !sortAscending;
      sharedPrefs.edit().putBoolean(prefKeyAscending, sortAscending).apply();
    } else if (id < idStartUserfields) {
      SortOption sortOption = sortOptions.get(id-ID_START_OPTIONS);
      if (sortOption == null) return;
      sortMode = sortOption.key;
      setFilterText();
      sharedPrefs.edit().putString(prefKey, sortMode).apply();
    } else {
      sortMode = Userfield.NAME_PREFIX + userfields.get(id-idStartUserfields).getName();
      setFilterText();
      sharedPrefs.edit().putString(prefKey, sortMode).apply();
    }
  }

  private void setItems() {
    ArrayList<MenuItemData> menuItemDataList = new ArrayList<>();
    for (int i=0; i<sortOptions.size(); i++) {
      SortOption sortOption = sortOptions.get(i);
      if (sortOption == null) return;
      menuItemDataList.add(new MenuItemData(
          ID_START_OPTIONS + i,
          0,
          sortOption.name,
          sortMode.equals(sortOption.key)
      ));
    }
    if (userfields != null) {
      for (Userfield userfield : userfields) {
        if (userfield == null) continue;
        menuItemDataList.add(new MenuItemData(
            userfield.getId(),
            1,
            userfield.getCaption(),
            sortMode.equals(Userfield.NAME_PREFIX + userfield.getName())
        ));
      }
    }
    menuItemDataList.add(new MenuItemData(
        ID_ASCENDING,
        2,
        application.getString(R.string.action_ascending),
        sortAscending
    ));
    setMenuItemDataList(menuItemDataList);
    setMenuItemGroups(
        new MenuItemGroup(0, true, true),
        new MenuItemGroup(1, true, true),
        new MenuItemGroup(2, true, false)
    );
    emitValue();
  }

  public static class SortOption {
    public final String key;
    public final String name;

    public SortOption(String key, String name) {
      this.key = key;
      this.name = name;
    }
  }
}