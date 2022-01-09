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

public class InfoFullscreen {

  public static final int ERROR_UNSPECIFIED = 0;
  public static final int ERROR_OFFLINE = 2;
  public static final int ERROR_NETWORK = 4;
  public static final int INFO_NO_SEARCH_RESULTS = 6;
  public static final int INFO_NO_FILTER_RESULTS = 8;
  public static final int INFO_EMPTY_STOCK = 10;
  public static final int INFO_EMPTY_SHOPPING_LIST = 12;
  public static final int INFO_EMPTY_PRODUCTS = 14;
  public static final int INFO_EMPTY_QUS = 16;
  public static final int INFO_EMPTY_STORES = 18;
  public static final int INFO_EMPTY_PRODUCT_GROUPS = 20;
  public static final int INFO_EMPTY_LOCATIONS = 22;
  public static final int INFO_EMPTY_PRODUCT_BARCODES = 24;
  public static final int INFO_EMPTY_TASKS = 26;
  public static final int INFO_EMPTY_TASK_CATEGORIES = 28;

  private final int type;
  private final String exact;
  private final OnRetryButtonClickListener clickListener;

  public InfoFullscreen(int type, String exact, OnRetryButtonClickListener clickListener) {
    this.type = type;
    this.exact = exact;
    this.clickListener = clickListener;
  }

  public InfoFullscreen(int type, OnRetryButtonClickListener clickListener) {
    this(type, null, clickListener);
  }

  public InfoFullscreen(int type, String exact) {
    this(type, exact, null);
  }

  public InfoFullscreen(int type) {
    this(type, null, null);
  }

  public int getType() {
    return type;
  }

  public String getExact() {
    return exact;
  }

  public OnRetryButtonClickListener getClickListener() {
    return clickListener;
  }

  public interface OnRetryButtonClickListener {

    void onClicked();
  }
}
