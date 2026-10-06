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

package xyz.zedler.patrick.grocy.form;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Values for a new chore. Scheduling remains the responsibility of the server. */
public class FormDataChore {

  public static final List<String> PERIOD_TYPES = Collections.unmodifiableList(Arrays.asList(
      "manually", "daily", "weekly", "monthly", "yearly"
  ));
  public static final List<String> WEEKDAYS = Collections.unmodifiableList(Arrays.asList(
      "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"
  ));

  public static Map<String, Object> create(
      String name, String description, String periodType, int interval,
      int monthDay, List<String> weekdays, LocalDate startDate
  ) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("name");
    }
    if (!PERIOD_TYPES.contains(periodType)) {
      throw new IllegalArgumentException("period_type");
    }
    if (!periodType.equals("manually") && interval < 1) {
      throw new IllegalArgumentException("period_interval");
    }
    if (periodType.equals("monthly") && (monthDay < 1 || monthDay > 31)) {
      throw new IllegalArgumentException("period_days");
    }
    if (periodType.equals("weekly") && (weekdays == null || weekdays.isEmpty()
        || !WEEKDAYS.containsAll(weekdays))) {
      throw new IllegalArgumentException("period_config");
    }
    if (startDate == null) {
      throw new IllegalArgumentException("start_date");
    }
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("name", name.trim());
    values.put("description", description == null ? "" : description.trim());
    values.put("period_type", periodType);
    values.put("period_interval", periodType.equals("manually") ? 1 : interval);
    values.put("period_days", periodType.equals("monthly") ? monthDay : 1);
    values.put("period_config", periodType.equals("weekly") ? String.join(",", weekdays) : "");
    values.put("start_date", startDate + " 00:00:00");
    values.put("track_date_only", 1);
    values.put("active", 1);
    values.put("rollover", 0);
    values.put("assignment_type", "no-assignment");
    values.put("assignment_config", "");
    values.put("consume_product_on_execution", 0);
    return values;
  }
}
