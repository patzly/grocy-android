package xyz.zedler.patrick.grocy.model;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import java.util.HashMap;

public class HorizontalFilterBarSingle {
    public final static String MISSING = "missing";
    public final static String UNDONE = "undone";
    public final static String DUE_NEXT = "due_next";
    public final static String OVERDUE = "overdue";
    public final static String EXPIRED = "expired";
    public final static String IN_STOCK = "in_stock";

    private final HashMap<String, Integer> itemsFilteredCounts;
    private final HashMap<String, Boolean> filterStates;
    private final FilterChangedListener filterChangedListener;

    public HorizontalFilterBarSingle(
            FilterChangedListener filterChangedListener,
            String... filters
    ) {
        itemsFilteredCounts = new HashMap<>();
        filterStates = new HashMap<>();
        for(String filter : filters) {
            itemsFilteredCounts.put(filter, 0);
            filterStates.put(filter, false);
        }
        this.filterChangedListener = filterChangedListener;
    }

    public int getItemsCount(String filter) {
        if(!itemsFilteredCounts.containsKey(filter)) return 0;
        return itemsFilteredCounts.get(filter);
    }

    public void setItemsCount(String filter, int count) {
        itemsFilteredCounts.put(filter, count);
    }

    public void setFilterState(String filter, boolean active) {
        filterStates.put(filter, active);
        onFilterChanged();
    }

    public void setSingleFilterActive(String filter) {
        /* Sets given filter to active and all others to inactive. */
        resetAllFilters();
        filterStates.put(filter, true);
        onFilterChanged();
    }

    public void resetAllFilters() {
        for(String filter : filterStates.keySet()) {
            filterStates.put(filter, false);
        }
        onFilterChanged();
    }

    public boolean isFilterActive(String filter) {
        if(!filterStates.containsKey(filter)) return false;
        return filterStates.get(filter);
    }

    public boolean isNoFilterActive() {
        boolean noFilterIsActive = true;
        for(String filter : filterStates.keySet()) {
            if(filterStates.get(filter)) {
                noFilterIsActive = false;
                break;
            }
        }
        return noFilterIsActive;
    }

    public void onFilterChanged() {
        filterChangedListener.onChanged();
    }

    public interface FilterChangedListener {
        void onChanged();
    }
}