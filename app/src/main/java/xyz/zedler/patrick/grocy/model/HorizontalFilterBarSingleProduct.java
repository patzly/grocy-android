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

public class HorizontalFilterBarSingleProduct extends HorizontalFilterBarSingle{

  public final static String MISSING = "missing";
  public final static String UNDONE = "undone";
  public final static String DUE_NEXT = "due_next";
  public final static String OVERDUE = "overdue";
  public final static String EXPIRED = "expired";
  public final static String IN_STOCK = "in_stock";

  public HorizontalFilterBarSingleProduct(FilterChangedListener filterChangedListener, String... filters) {
    super(filterChangedListener, filters);
  }
}