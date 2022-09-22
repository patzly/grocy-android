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

import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import androidx.annotation.ColorRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import com.google.zxing.integration.android.IntentIntegrator;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DecoratedBarcodeView.TorchListener;
import com.journeyapps.barcodescanner.camera.CameraSettings;
import com.journeyapps.barcodescanner.camera.CameraSettings.FocusMode;
import java.util.ArrayList;
import java.util.Set;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.scanner.ZXingScanCaptureManager.BarcodeListener;
import xyz.zedler.patrick.grocy.util.Constants.BarcodeFormats;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.SCANNER;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class EmbeddedFragmentScannerZXing extends EmbeddedFragmentScanner implements
    BarcodeListener {

  private final static String TAG = EmbeddedFragmentScannerZXing.class.getSimpleName();

  private boolean isScannerVisible;
  private boolean isTorchOn;
  private final Fragment fragment;
  private final BarcodeListener barcodeListener;
  private final DecoratedBarcodeView barcodeView;
  private final ZXingScanCaptureManager capture;
  private boolean suppressNextScanStart = false;
  private final boolean qrCodeFormat;

  public EmbeddedFragmentScannerZXing(
      Fragment fragment,
      CoordinatorLayout containerScanner,
      BarcodeListener barcodeListener,
      @ColorRes int viewfinderMaskColor,
      boolean qrCodeFormat,
      boolean takeSmallQrCodeFormat
  ) {
    super(fragment.requireActivity());
    this.fragment = fragment;
    this.barcodeListener = barcodeListener;
    this.qrCodeFormat = qrCodeFormat;

    // set container size
    int width;
    int height;
    if (qrCodeFormat && !takeSmallQrCodeFormat) {
      width = UiUtil.dpToPx(fragment.requireContext(), 250);
      height = UiUtil.dpToPx(fragment.requireContext(), 250);
    } else if (qrCodeFormat) {
      width = UiUtil.dpToPx(fragment.requireContext(), 180);
      height = UiUtil.dpToPx(fragment.requireContext(), 180);
    } else {
      width = UiUtil.dpToPx(fragment.requireContext(), 350);
      height = UiUtil.dpToPx(fragment.requireContext(), 160);
    }
    if (containerScanner.getParent() instanceof LinearLayout) {
      LinearLayout.LayoutParams layoutParamsContainer = new LinearLayout.LayoutParams(width, height);
      layoutParamsContainer.gravity = Gravity.CENTER;
      containerScanner.setLayoutParams(layoutParamsContainer);
    } else if (containerScanner.getParent() instanceof RelativeLayout) {
      RelativeLayout.LayoutParams layoutParamsContainer = new RelativeLayout.LayoutParams(width, height);
      layoutParamsContainer.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
      containerScanner.setLayoutParams(layoutParamsContainer);
      ((RelativeLayout) containerScanner.getParent()).setGravity(Gravity.CENTER_HORIZONTAL);
    }

    // fill container with necessary views
    ViewStub viewStub = new ViewStub(fragment.requireContext());
    int matchParent = ViewGroup.LayoutParams.MATCH_PARENT;
    LayoutParams layoutParamsScanner = new LayoutParams(matchParent, matchParent);
    viewStub.setLayoutParams(layoutParamsScanner);
    viewStub.setInflatedId(R.id.decorated_barcode_view);
    if (qrCodeFormat && !takeSmallQrCodeFormat) {
      viewStub.setLayoutResource(R.layout.partial_scanner_zxing_2d_decorated);
    } else if (qrCodeFormat) {
      viewStub.setLayoutResource(R.layout.partial_scanner_zxing_2d_small_decorated);
    } else {
      viewStub.setLayoutResource(R.layout.partial_scanner_zxing_1d_decorated);
    }
    containerScanner.addView(viewStub);
    barcodeView = (DecoratedBarcodeView) viewStub.inflate();
    barcodeView.getViewFinder().setMaskColor(
        ContextCompat.getColor(fragment.requireContext(), viewfinderMaskColor)
    );

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
    cameraSettings.setFocusMode(FocusMode.CONTINUOUS);

    IntentIntegrator integrator = new IntentIntegrator(fragment.requireActivity());
    integrator.setDesiredBarcodeFormats(getEnabledBarcodeFormats());
    barcodeView.initializeFromIntent(integrator.createScanIntent());

    barcodeView.getBarcodeView().setCameraSettings(cameraSettings);
    capture = new ZXingScanCaptureManager(
        fragment.requireActivity(),
        barcodeView,
        this
    );
  }

  @Override
  public void setScannerVisibilityLive(LiveData<Boolean> scannerVisibilityLive) {
    setScannerVisibilityLive(scannerVisibilityLive, false);
  }

  public void setScannerVisibilityLive(
      LiveData<Boolean> scannerVisibilityLive,
      boolean suppressNextScanStart
  ) {
    this.suppressNextScanStart = suppressNextScanStart;
    if (scannerVisibilityLive.hasObservers()) {
      scannerVisibilityLive.removeObservers(fragment.getViewLifecycleOwner());
    }
    scannerVisibilityLive.observe(fragment.getViewLifecycleOwner(), visible -> {
      isScannerVisible = visible;
      if (visible) {
        if (this.suppressNextScanStart) {
          this.suppressNextScanStart = false;
          return;
        }
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

  public void startScannerIfVisible() {
    if (!isScannerVisible) return;
    capture.onResume();
    new Handler().postDelayed(capture::decode, 500);
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

  private String[] getEnabledBarcodeFormats() {
    ArrayList<String> enabledBarcodeFormats = new ArrayList<>();
    SharedPreferences sharedPrefs = PreferenceManager
        .getDefaultSharedPreferences(fragment.requireContext());
    Set<String> enabledBarcodeFormatsSet = sharedPrefs.getStringSet(
        SCANNER.BARCODE_FORMATS,
        SETTINGS_DEFAULT.SCANNER.BARCODE_FORMATS
    );
    if (!enabledBarcodeFormatsSet.isEmpty()) {
      for (String barcodeFormat : enabledBarcodeFormatsSet) {
        switch (barcodeFormat) {
          case BarcodeFormats.BARCODE_FORMAT_CODE128:
            enabledBarcodeFormats.add(IntentIntegrator.CODE_128);
            break;
          case BarcodeFormats.BARCODE_FORMAT_CODE39:
            enabledBarcodeFormats.add(IntentIntegrator.CODE_39);
            break;
          case BarcodeFormats.BARCODE_FORMAT_CODE93:
            enabledBarcodeFormats.add(IntentIntegrator.CODE_93);
            break;
          case BarcodeFormats.BARCODE_FORMAT_EAN13:
            enabledBarcodeFormats.add(IntentIntegrator.EAN_13);
            break;
          case BarcodeFormats.BARCODE_FORMAT_EAN8:
            enabledBarcodeFormats.add(IntentIntegrator.EAN_8);
            break;
          case BarcodeFormats.BARCODE_FORMAT_ITF:
            enabledBarcodeFormats.add(IntentIntegrator.ITF);
            break;
          case BarcodeFormats.BARCODE_FORMAT_UPCA:
            enabledBarcodeFormats.add(IntentIntegrator.UPC_A);
            break;
          case BarcodeFormats.BARCODE_FORMAT_UPCE:
            enabledBarcodeFormats.add(IntentIntegrator.UPC_E);
            break;
          case BarcodeFormats.BARCODE_FORMAT_QR:
            enabledBarcodeFormats.add(IntentIntegrator.QR_CODE);
            break;
          case BarcodeFormats.BARCODE_FORMAT_PDF417:
            enabledBarcodeFormats.add(IntentIntegrator.PDF_417);
            break;
          case BarcodeFormats.BARCODE_FORMAT_MATRIX:
            enabledBarcodeFormats.add(IntentIntegrator.DATA_MATRIX);
            break;
          case BarcodeFormats.BARCODE_FORMAT_RSS14:
            enabledBarcodeFormats.add(IntentIntegrator.RSS_14);
            break;
          case BarcodeFormats.BARCODE_FORMAT_RSSE:
            enabledBarcodeFormats.add(IntentIntegrator.RSS_EXPANDED);
            break;
        }
      }
    }
    if (qrCodeFormat && !enabledBarcodeFormats.contains(IntentIntegrator.QR_CODE)) {
      enabledBarcodeFormats.add(IntentIntegrator.QR_CODE);
    }
    String[] mStringArray = new String[enabledBarcodeFormats.size()];
    return enabledBarcodeFormats.toArray(mStringArray);
  }
}
