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

import android.app.Application;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class FilterChipLiveDataFields extends FilterChipLiveData {

  public final static String MULTI_SEPARATOR = "%0";
  public final static String VALUE_SEPARATOR = "%=";

  private final String prefKey;
  private final HashMap<String, Boolean> fieldStates;
  private final SharedPreferences sharedPrefs;

  public FilterChipLiveDataFields(
      String prefKey,
      Application application,
      String... defaultFields
  ) {
    this.prefKey = prefKey;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    fieldStates = getFieldStatesFromMulti(
        sharedPrefs.getString(prefKey, null),
        defaultFields
    );
  }

  public List<String> getActiveFields() {
    return fieldStates.entrySet().stream()
        .filter(Entry::getValue)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  public HashMap<String, Boolean> getFieldStatesFromMulti(
      @Nullable String multiFieldStates,
      String... defaultFields
  ) {
    HashMap<String, Boolean> fieldStates = new HashMap<>();
    if (multiFieldStates != null && !multiFieldStates.isBlank()) {
      for (String fieldStatePair : multiFieldStates.split(MULTI_SEPARATOR)) {
        String[] pairArray = fieldStatePair.split(VALUE_SEPARATOR);
        if (pairArray.length != 2) continue;
        fieldStates.put(pairArray[0], Boolean.parseBoolean(pairArray[1]));
      }
    }
    for (String defaultField : defaultFields) {
      if (!fieldStates.containsKey(defaultField)) {
        fieldStates.put(defaultField, true);
      }
    }
    return fieldStates;
  }

  public boolean isFieldActive(String field) {
    if (!fieldStates.containsKey(field)) return false;
    return Boolean.TRUE.equals(fieldStates.get(field));
  }

  public void enableOrDisableField(String field) {
    if (fieldStates == null || field == null) return;
    if (fieldStates.containsKey(field) && Boolean.TRUE.equals(fieldStates.get(field))) {
      fieldStates.put(field, false);
    } else {
      fieldStates.put(field, true);
    }
  }

  public void storeFieldStates() {
    sharedPrefs.edit().putString(prefKey, createMultiFieldStates()).apply();
  }

  public String createMultiFieldStates() {
    if (fieldStates.isEmpty()) return "";
    StringBuilder stringBuilder = new StringBuilder();
    List<String> fields = new ArrayList<>(fieldStates.keySet());
    for (String field : fields) {
      stringBuilder.append(field);
      stringBuilder.append(VALUE_SEPARATOR);
      stringBuilder.append(Boolean.TRUE.equals(fieldStates.get(field)) ? "true" : "false");
      if (!fields.get(fields.size()-1).equals(field)) {
        stringBuilder.append(MULTI_SEPARATOR);
      }
    }
    return stringBuilder.toString();
  }
}