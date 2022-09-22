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

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.TorchState;
import androidx.camera.mlkit.vision.MlKitAnalyzer;
import androidx.camera.view.CameraController;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.camera.view.PreviewView.ImplementationMode;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import com.google.android.material.card.MaterialCardView;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.PurchasePromptBottomSheet;
import xyz.zedler.patrick.grocy.util.Constants.BarcodeFormats;
import xyz.zedler.patrick.grocy.util.Constants.PREF;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.SCANNER;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.util.HapticUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.UnlockUtil;

public class EmbeddedFragmentScannerMLKit extends EmbeddedFragmentScanner {

  private final static String TAG = EmbeddedFragmentScannerMLKit.class.getSimpleName();

  private static final int PERMISSION_REQUESTS = 1;

  private final LifecycleCameraController cameraController;
  private final BarcodeScannerOptions barcodeScannerOptions;
  private final SharedPreferences sharedPrefs;
  private boolean isScannerVisible;
  private BarcodeScanner barcodeScanner;
  private final PreviewView previewView;
  private final Fragment fragment;
  private final MainActivity activity;
  private final BarcodeListener barcodeListener;
  private boolean suppressNextScanStart = false;
  private final boolean qrCodeFormat;
  private final int strokeWidth;

  public EmbeddedFragmentScannerMLKit(
      Fragment fragment,
      CoordinatorLayout containerScanner,
      BarcodeListener barcodeListener,
      boolean qrCodeFormat,
      boolean takeSmallQrCodeFormat
  ) {
    super(fragment.requireActivity());
    this.fragment = fragment;
    this.activity = (MainActivity) fragment.requireActivity();
    this.barcodeListener = barcodeListener;
    this.qrCodeFormat = qrCodeFormat;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(fragment.requireContext());

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
    int matchParent = ViewGroup.LayoutParams.MATCH_PARENT;
    LayoutParams layoutParamsPreview = new LayoutParams(matchParent, matchParent);
    int size16dp = UiUtil.dpToPx(fragment.requireContext(), 16);
    layoutParamsPreview.setMargins(size16dp, size16dp, size16dp, size16dp);
    previewView = new PreviewView(fragment.requireContext());
    previewView.setLayoutParams(layoutParamsPreview);
    previewView.setImplementationMode(ImplementationMode.COMPATIBLE);
    previewView.setOnClickListener(v -> toggleTorch());
    containerScanner.addView(previewView);
    LayoutParams layoutParamsCard = new LayoutParams(matchParent, matchParent);
    int size12dp = UiUtil.dpToPx(fragment.requireContext(), 12);
    layoutParamsCard.setMargins(size12dp, size12dp, size12dp, size12dp);
    MaterialCardView cardView = new MaterialCardView(fragment.requireContext());
    cardView.setLayoutParams(layoutParamsCard);
    cardView.setCardElevation(0);
    int backgroundColor = ContextCompat.getColor(fragment.requireContext(), R.color.transparent);
    cardView.setCardBackgroundColor(backgroundColor);
    strokeWidth = UiUtil.dpToPx(fragment.requireContext(), 5);
    cardView.setStrokeWidth(strokeWidth);
    int strokeColor = ContextCompat.getColor(fragment.requireContext(), R.color.grey800);
    cardView.setStrokeColor(strokeColor);
    cardView.setRadius(UiUtil.dpToPx(fragment.requireContext(), 10));
    containerScanner.addView(cardView);

    ArrayList<Integer> enabledBarcodeFormats = getEnabledBarcodeFormats();
    BarcodeScannerOptions.Builder optionsBuilder = new BarcodeScannerOptions.Builder();
    if (enabledBarcodeFormats.size() == 1) {
      optionsBuilder.setBarcodeFormats(enabledBarcodeFormats.get(0));
    } else if (enabledBarcodeFormats.size() > 1) {
      List<Integer> afterFirst = enabledBarcodeFormats.subList(1, enabledBarcodeFormats.size());
      optionsBuilder.setBarcodeFormats(enabledBarcodeFormats.get(0), convertIntegers(afterFirst));
    }
    barcodeScannerOptions = optionsBuilder.build();

    fragment.registerForActivityResult(
        new ActivityResultContracts.RequestPermission(),
        result -> {
          if(result) {
            startScannerIfVisible();
          }
        });

    cameraController = new LifecycleCameraController(fragment.requireContext());
    CameraSelector cameraSelector = sharedPrefs
        .getBoolean(SCANNER.FRONT_CAM, SETTINGS_DEFAULT.SCANNER.FRONT_CAM)
        ? CameraSelector.DEFAULT_FRONT_CAMERA
        : CameraSelector.DEFAULT_BACK_CAMERA;
    previewView.setController(cameraController);

    cameraController.getInitializationFuture().addListener(
        () -> {
          if (cameraController.hasCamera(cameraSelector)) {
            cameraController.setCameraSelector(cameraSelector);
          }
        },
        ContextCompat.getMainExecutor(fragment.requireContext())
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
    startScannerIfVisible();
  }

  public void onPause() {
    stopScanner();
  }

  public void onDestroy() {
    stopScanner();
    if (cameraController != null && cameraController.getCameraInfo() != null
        && cameraController.getCameraInfo().hasFlashUnit()) {
      cameraController.enableTorch(false);
    }
    lockOrUnlockRotation(false);
  }

  void stopScanner() {
    keepScreenOn(false);
    cameraController.unbind();
    if (barcodeScanner != null) {
      barcodeScanner.close();
    }
  }

  public void startScannerIfVisible() {
    if (!isScannerVisible) return;
    if (ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_DENIED) {
      ActivityCompat.requestPermissions(
          activity, new String[] {Manifest.permission.CAMERA}, PERMISSION_REQUESTS
      );
      return;
    }

    cameraController.bindToLifecycle(fragment.getViewLifecycleOwner());

    if (barcodeScanner != null) {
      barcodeScanner.close();
    }
    barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions);

    cameraController.clearImageAnalysisAnalyzer();
    cameraController.setImageAnalysisAnalyzer(
        ContextCompat.getMainExecutor(fragment.requireContext()),
        new MlKitAnalyzer(
            List.of(barcodeScanner),
            CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
            ContextCompat.getMainExecutor(fragment.requireContext()),
            result -> onScanSuccess(result.getValue(barcodeScanner))
        )
    );

    keepScreenOn(true);
  }

  private void onScanSuccess(List<Barcode> barcodes) {
    if (barcodes == null || barcodes.size() != 1 || barcodes.get(0) == null
        || barcodes.get(0).getRawValue() == null
        || Objects.equals(barcodes.get(0).getRawValue(), "")) {
      return;
    }

    Point[] cornerPoints = barcodes.get(0).getCornerPoints();
    if (cornerPoints == null) return;

    int pointsOutsidePreview = 0;
    for (Point point : cornerPoints) {
      if (point.x < strokeWidth*2 || point.y < strokeWidth*2
          || point.x > previewView.getWidth()-strokeWidth*2
          || point.y > previewView.getHeight()-strokeWidth*2
      ) {
        pointsOutsidePreview++;
        if (pointsOutsidePreview > 2) return;
      }
    }

    new HapticUtil(fragment.requireContext()).tick();
    stopScanner();

    int promptCount = sharedPrefs.getInt(PREF.PURCHASE_PROMPT, 1);
    if (UnlockUtil.isKeyInstalled(activity) && UnlockUtil.isPlayStoreInstalled(activity)) {
      if (!sharedPrefs.getBoolean(PREF.PURCHASED, false) && promptCount > 0) {
        if (promptCount < 50) {
          sharedPrefs.edit().putInt(PREF.PURCHASE_PROMPT, promptCount + 1).apply();
        } else {
          activity.showBottomSheet(new PurchasePromptBottomSheet());
        }
      } else if (promptCount > 1) {
        sharedPrefs.edit().putInt(PREF.PURCHASE_PROMPT, 1).apply();
      }
    } else if (promptCount > 1) {
      sharedPrefs.edit().putInt(PREF.PURCHASE_PROMPT, 1).apply();
    }

    if (barcodeListener != null) {
      barcodeListener.onBarcodeRecognized(barcodes.get(0).getRawValue());
    }
  }

  public void toggleTorch() {
    if (cameraController.getCameraInfo() == null
        || !cameraController.getCameraInfo().hasFlashUnit()) return;
    assert cameraController.getCameraInfo().getTorchState().getValue() != null;
    int state = cameraController.getCameraInfo().getTorchState().getValue();
    cameraController.enableTorch(state == TorchState.OFF);
  }

  private ArrayList<Integer> getEnabledBarcodeFormats() {
    ArrayList<Integer> enabledBarcodeFormats = new ArrayList<>();
    Set<String> enabledBarcodeFormatsSet = sharedPrefs.getStringSet(
        SCANNER.BARCODE_FORMATS,
        SETTINGS_DEFAULT.SCANNER.BARCODE_FORMATS
    );
    if (!enabledBarcodeFormatsSet.isEmpty()) {
      for (String barcodeFormat : enabledBarcodeFormatsSet) {
        switch (barcodeFormat) {
          case BarcodeFormats.BARCODE_FORMAT_CODE128:
            enabledBarcodeFormats.add(Barcode.FORMAT_CODE_128);
            break;
          case BarcodeFormats.BARCODE_FORMAT_CODE39:
            enabledBarcodeFormats.add(Barcode.FORMAT_CODE_39);
            break;
          case BarcodeFormats.BARCODE_FORMAT_CODE93:
            enabledBarcodeFormats.add(Barcode.FORMAT_CODE_93);
            break;
          case BarcodeFormats.BARCODE_FORMAT_EAN13:
            enabledBarcodeFormats.add(Barcode.FORMAT_EAN_13);
            break;
          case BarcodeFormats.BARCODE_FORMAT_EAN8:
            enabledBarcodeFormats.add(Barcode.FORMAT_EAN_8);
            break;
          case BarcodeFormats.BARCODE_FORMAT_ITF:
            enabledBarcodeFormats.add(Barcode.FORMAT_ITF);
            break;
          case BarcodeFormats.BARCODE_FORMAT_UPCA:
            enabledBarcodeFormats.add(Barcode.FORMAT_UPC_A);
            break;
          case BarcodeFormats.BARCODE_FORMAT_UPCE:
            enabledBarcodeFormats.add(Barcode.FORMAT_UPC_E);
            break;
          case BarcodeFormats.BARCODE_FORMAT_QR:
            enabledBarcodeFormats.add(Barcode.FORMAT_QR_CODE);
            break;
          case BarcodeFormats.BARCODE_FORMAT_PDF417:
            enabledBarcodeFormats.add(Barcode.FORMAT_PDF417);
            break;
          case BarcodeFormats.BARCODE_FORMAT_MATRIX:
            enabledBarcodeFormats.add(Barcode.FORMAT_DATA_MATRIX);
            break;
          case BarcodeFormats.BARCODE_FORMAT_CODABAR:
            enabledBarcodeFormats.add(Barcode.FORMAT_CODABAR);
            break;
          case BarcodeFormats.BARCODE_FORMAT_AZTEC:
            enabledBarcodeFormats.add(Barcode.FORMAT_AZTEC);
            break;
        }
      }
    }
    if (qrCodeFormat && !enabledBarcodeFormats.contains(Barcode.FORMAT_QR_CODE)) {
      enabledBarcodeFormats.add(Barcode.FORMAT_QR_CODE);
    }
    return enabledBarcodeFormats;
  }

  public static int[] convertIntegers(List<Integer> integers) {
    int[] ret = new int[integers.size()];
    for (int i=0; i < ret.length; i++) {
      ret[i] = integers.get(i);
    }
    return ret;
  }
}
