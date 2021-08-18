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

import java.util.HashMap;

public class HorizontalFilterBarMulti {

  public final static String PRODUCT_GROUP = "product_group";
  public final static String LOCATION = "location";

  private final HashMap<String, Filter> filtersActive;
  private final FilterChangedListener filterChangedListener;

  public HorizontalFilterBarMulti(FilterChangedListener filterChangedListener) {
    filtersActive = new HashMap<>();
    this.filterChangedListener = filterChangedListener;
  }

  public void addFilter(String filterType, Filter filter) {
    filtersActive.put(filterType, filter);
    onFilterChanged();
  }

  public void removeFilter(String filter) {
    filtersActive.remove(filter);
    onFilterChanged();
  }

  public Filter getFilter(String filter) {
    if (!filtersActive.containsKey(filter)) {
      return null;
    }
    return filtersActive.get(filter);
  }

  public boolean areFiltersActive() {
    return !filtersActive.isEmpty();
  }

  public void onFilterChanged() {
    if (filterChangedListener == null) {
      return;
    }
    filterChangedListener.onChanged();
  }

  public interface FilterChangedListener {

    void onChanged();
  }

  public static class Filter {

    private final String objectName;
    private final int objectId;

    public Filter(String objectName, int objectId) {
      this.objectName = objectName;
      this.objectId = objectId;
    }

    public String getObjectName() {
      return objectName;
    }

    public int getObjectId() {
      return objectId;
    }
  }
}