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

package xyz.zedler.patrick.grocy.scanner;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.WindowManager;
import androidx.lifecycle.LiveData;

public abstract class EmbeddedFragmentScanner {

  private final Activity activity;

  public EmbeddedFragmentScanner(Activity activity) {
    this.activity = activity;
  }

  public abstract void setScannerVisibilityLive(LiveData<Boolean> scannerVisibilityLive);

  public abstract void onResume();

  public abstract void onPause();

  public abstract void onDestroy();

  abstract void stopScanner();

  public abstract void startScannerIfVisible();

  public abstract void toggleTorch();

  void lockOrUnlockRotation(boolean scannerIsVisible) {
    if (scannerIsVisible) {
      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    } else {
      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
    }
  }

  // for MLKit scanner class
  void keepScreenOn(boolean keepOn) {
    if (keepOn) {
      activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    } else {
      activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
  }

  public interface BarcodeListener {
    void onBarcodeRecognized(String rawValue);
  }
}
