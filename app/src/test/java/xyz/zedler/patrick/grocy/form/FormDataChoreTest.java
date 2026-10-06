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

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;

public class FormDataChoreTest {

  private final LocalDate start = LocalDate.of(2026, 10, 5);

  @Test
  public void dailyUsesIntervalAndDateOnlyDefaults() {
    Map<String, Object> values = FormDataChore.create(" Clean ", " Kitchen ", "daily", 3,
        31, Collections.singletonList("monday"), start);
    assertEquals("Clean", values.get("name"));
    assertEquals("Kitchen", values.get("description"));
    assertEquals(3, values.get("period_interval"));
    assertEquals(1, values.get("period_days"));
    assertEquals("", values.get("period_config"));
    assertEquals("2026-10-05 00:00:00", values.get("start_date"));
    assertEquals(1, values.get("active"));
    assertEquals(1, values.get("track_date_only"));
    assertEquals("no-assignment", values.get("assignment_type"));
    assertEquals("", values.get("assignment_config"));
    assertEquals(0, values.get("rollover"));
    assertEquals(0, values.get("consume_product_on_execution"));
    assertFalse(values.containsKey("id"));
  }

  @Test
  public void yearlyPreservesIntervalAndLeapDayStart() {
    Map<String, Object> values = FormDataChore.create("Service", "", "yearly", 2, 31,
        Collections.singletonList("monday"), LocalDate.of(2028, 2, 29));
    assertEquals("yearly", values.get("period_type"));
    assertEquals(2, values.get("period_interval"));
    assertEquals("2028-02-29 00:00:00", values.get("start_date"));
    assertEquals(1, values.get("period_days"));
    assertEquals("", values.get("period_config"));
  }

  @Test
  public void monthlyAcceptsBothDayBoundaries() {
    for (int day : new int[]{1, 31}) {
      Map<String, Object> values = FormDataChore.create("Filter", "", "monthly", 1, day,
          Collections.emptyList(), start);
      assertEquals(day, values.get("period_days"));
      assertEquals("", values.get("period_config"));
    }
  }

  @Test
  public void weeklyUsesServerWeekdayNames() {
    Map<String, Object> values = FormDataChore.create("Bins", "", "weekly", 2, 1,
        Arrays.asList("monday", "friday"), start);
    assertEquals("monday,friday", values.get("period_config"));
    assertEquals(2, values.get("period_interval"));
  }

  @Test
  public void monthlyAndManualNormalizeHiddenFields() {
    Map<String, Object> monthly = FormDataChore.create("Filter", "", "monthly", 2, 31,
        Collections.emptyList(), start);
    assertEquals(31, monthly.get("period_days"));
    Map<String, Object> manual = FormDataChore.create("Other", null, "manually", 0, 0,
        Collections.singletonList("monday"), start);
    assertEquals(1, manual.get("period_interval"));
    assertEquals(1, manual.get("period_days"));
    assertEquals("", manual.get("period_config"));
    assertEquals("", manual.get("description"));
  }

  @Test
  public void rejectsInvalidSchedulesBeforeSending() {
    assertThrows(IllegalArgumentException.class, () -> FormDataChore.create(" ", "", "daily", 1, 1, Collections.emptyList(), start));
    assertThrows(IllegalArgumentException.class, () -> FormDataChore.create(null, "", "daily", 1, 1, Collections.emptyList(), start));
    assertThrows(IllegalArgumentException.class, () -> FormDataChore.create("x", "", null, 1, 1, Collections.emptyList(), start));
    assertThrows(IllegalArgumentException.class, () -> FormDataChore.create("x", "", "unknown", 1, 1, Collections.emptyList(), start));
    assertThrows(IllegalArgumentException.class, () -> FormDataChore.create("x", "", "daily", 0, 1, Collections.emptyList(), start));
    assertThrows(IllegalArgumentException.class, () -> FormDataChore.create("x", "", "weekly", 1, 1, Collections.emptyList(), start));
    assertThrows(IllegalArgumentException.class, () -> FormDataChore.create("x", "", "weekly", 1, 1, null, start));
    assertThrows(IllegalArgumentException.class, () -> FormDataChore.create("x", "", "weekly", 1, 1, Collections.singletonList("Monday"), start));
    assertThrows(IllegalArgumentException.class, () -> FormDataChore.create("x", "", "monthly", 1, 32, Collections.emptyList(), start));
    assertThrows(IllegalArgumentException.class, () -> FormDataChore.create("x", "", "monthly", 1, 0, Collections.emptyList(), start));
    assertThrows(IllegalArgumentException.class, () -> FormDataChore.create("x", "", "yearly", 1, 1, Collections.emptyList(), null));
  }
}
