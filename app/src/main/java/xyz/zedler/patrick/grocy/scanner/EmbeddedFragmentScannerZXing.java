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

package xyz.zedler.patrick.grocy.scanner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.ViewGroup;
import android.view.ViewStub;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DecoratedBarcodeView.TorchListener;
import com.journeyapps.barcodescanner.camera.CameraSettings;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.scan.ScanInputCaptureManager;
import xyz.zedler.patrick.grocy.scan.ScanInputCaptureManager.BarcodeListener;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.SCANNER;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS_DEFAULT;

public class EmbeddedFragmentScannerZXing extends EmbeddedFragmentScanner implements
    BarcodeListener {

  private final static String TAG = EmbeddedFragmentScannerZXing.class.getSimpleName();

  private boolean isScannerVisible;
  private boolean isTorchOn;
  private final Fragment fragment;
  private final Activity activity;
  private final BarcodeListener barcodeListener;
  private final DecoratedBarcodeView barcodeView;
  private final ScanInputCaptureManager capture;

  public EmbeddedFragmentScannerZXing(
      Fragment fragment,
      CoordinatorLayout containerScanner,
      BarcodeListener barcodeListener
  ) {
    super(fragment.requireActivity());
    this.fragment = fragment;
    this.activity = fragment.requireActivity();
    this.barcodeListener = barcodeListener;

    // fill container with necessary views
    ViewStub viewStub = new ViewStub(fragment.requireContext());
    int matchParent = ViewGroup.LayoutParams.MATCH_PARENT;
    LayoutParams layoutParamsScanner = new LayoutParams(matchParent, matchParent);
    viewStub.setLayoutParams(layoutParamsScanner);
    viewStub.setInflatedId(R.id.decorated_barcode_view);
    viewStub.setLayoutResource(R.layout.partial_scanner_barcode_view);
    containerScanner.addView(viewStub);
    barcodeView = (DecoratedBarcodeView) viewStub.inflate();

    barcodeView.setTorchListener(new TorchListener() {
      @Override
      public void onTorchOn() {
        isTorchOn = true;
      }
      @Override
      public void onTorchOff() {
        isTorchOn = false;
      }
    });
    barcodeView.setTorchOff();
    SharedPreferences sharedPreferences = PreferenceManager
        .getDefaultSharedPreferences(fragment.requireContext());
    boolean useFrontCam = sharedPreferences
        .getBoolean(SCANNER.FRONT_CAM, SETTINGS_DEFAULT.SCANNER.FRONT_CAM);
    CameraSettings cameraSettings = new CameraSettings();
    cameraSettings.setRequestedCameraId(useFrontCam ? 1 : 0);
    barcodeView.getBarcodeView().setCameraSettings(cameraSettings);
    capture = new ScanInputCaptureManager(activity, barcodeView, this);
  }

  public void setScannerVisibilityLive(LiveData<Boolean> scannerVisibilityLive) {
    if (scannerVisibilityLive.hasObservers()) {
      scannerVisibilityLive.removeObservers(fragment.getViewLifecycleOwner());
    }
    scannerVisibilityLive.observe(fragment.getViewLifecycleOwner(), visible -> {
      isScannerVisible = visible;
      if (visible) {
        startScannerIfVisible();
      } else {
        stopScanner();
      }
      lockOrUnlockRotation(visible);
    });
  }

  public void onResume() {
    if (isScannerVisible) {
      capture.onResume();
    }
  }

  public void onPause() {
    capture.onPause();
  }

  public void onDestroy() {
    stopScanner();
    barcodeView.setTorchOff();
    lockOrUnlockRotation(false);
  }

  void stopScanner() {
    capture.onPause();
    capture.onDestroy();
  }

  @SuppressLint("UnsafeOptInUsageError")
  public void startScannerIfVisible() {
    if (!isScannerVisible) return;

    capture.onResume();
    capture.decode();
  }

  public void toggleTorch() {
    if (isTorchOn) {
      barcodeView.setTorchOff();
    } else {
      barcodeView.setTorchOn();
    }
  }

  @Override
  public void onBarcodeResult(BarcodeResult result) {
    if (result.getText().isEmpty()) {
      startScannerIfVisible();
      return;
    }
    barcodeListener.onBarcodeRecognized(result.getText());
  }
}
