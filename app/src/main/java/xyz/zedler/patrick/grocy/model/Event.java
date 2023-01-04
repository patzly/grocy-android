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

import android.os.Bundle;

public abstract class Event {

  public final static int SNACKBAR_MESSAGE = 0;
  public final static int TRANSACTION_SUCCESS = 2;
  public final static int CONTINUE_SCANNING = 4;
  public final static int BOTTOM_SHEET = 6;
  public final static int NAVIGATE_UP = 8;
  public final static int SET_SHOPPING_LIST_ID = 10;
  public final static int FOCUS_INVALID_VIEWS = 12;
  public final static int QUICK_MODE_DISABLED = 14;
  public final static int QUICK_MODE_ENABLED = 16;
  public final static int SET_PRODUCT_ID = 18;
  public final static int LOGIN_SUCCESS = 20;
  public final static int CONSUME_SUCCESS = 22;
  public final static int CHOOSE_PRODUCT = 24;
  public final static int SET_RECIPE_ID = 26;

  abstract public int getType();

  public Bundle getBundle() {
    return null;
  }
}
