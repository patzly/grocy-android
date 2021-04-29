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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.os.SystemClock;
import android.view.View;

public class ClickUtil {

  private long idle = 500;
  private long lastClick;

  public ClickUtil() {
    lastClick = 0;
  }

  public ClickUtil(long idle) {
    lastClick = 0;
    this.idle = idle;
  }

  public void update() {
    lastClick = SystemClock.elapsedRealtime();
  }

  public boolean isDisabled() {
    if (SystemClock.elapsedRealtime() - lastClick < idle) {
      return true;
    }
    update();
    return false;
  }

  public static void setOnClickListeners(View.OnClickListener listener, View... views) {
    for (View view : views) {
      view.setOnClickListener(listener);
    }
  }
}
