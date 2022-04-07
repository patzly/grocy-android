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
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory;
import androidx.lifecycle.ViewModelProvider.Factory;
import com.google.android.material.card.MaterialCardView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.common.Barcode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.PurchasePromptBottomSheet;
import xyz.zedler.patrick.grocy.util.Constants.BarcodeFormats;
import xyz.zedler.patrick.grocy.util.Constants.PREF;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.SCANNER;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.util.HapticUtil;
import xyz.zedler.patrick.grocy.util.UnitUtil;
import xyz.zedler.patrick.grocy.util.UnlockUtil;

public class EmbeddedFragmentScannerMLKit extends EmbeddedFragmentScanner {

  private final static String TAG = EmbeddedFragmentScannerMLKit.class.getSimpleName();

  private static final int PERMISSION_REQUESTS = 1;

  @Nullable
  private ProcessCameraProvider cameraProvider;
  @Nullable private BarcodeScannerProcessor imageProcessor;
  private final GraphicOverlay graphicOverlay;
  private final PreviewView previewView;
  private boolean needUpdateGraphicOverlayImageSourceInfo;
  private final BarcodeScannerOptions barcodeScannerOptions;
  private final SharedPreferences sharedPrefs;
  private final LiveData<ProcessCameraProvider> processCameraProvider;
  private boolean isScannerVisible;
  private final int lensFacing;
  private final CameraSelector cameraSelector;
  private Camera camera;
  private final Fragment fragment;
  private final MainActivity activity;
  private final BarcodeListener barcodeListener;
  private final boolean cropImageToPreviewRect;
  private boolean supressNextScanStart = false;
  private boolean qrCodeFormat;

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
    cropImageToPreviewRect = sharedPrefs.getBoolean(
        SCANNER.CROP_CAMERA_STREAM,
        SETTINGS_DEFAULT.SCANNER.CROP_CAMERA_STREAM
    );

    // set container size
    int width;
    int height;
    if (qrCodeFormat && !takeSmallQrCodeFormat) {
      width = UnitUtil.dpToPx(fragment.requireContext(), 250);
      height = UnitUtil.dpToPx(fragment.requireContext(), 250);
    } else if (qrCodeFormat) {
      width = UnitUtil.dpToPx(fragment.requireContext(), 180);
      height = UnitUtil.dpToPx(fragment.requireContext(), 180);
    } else {
      width = UnitUtil.dpToPx(fragment.requireContext(), 350);
      height = UnitUtil.dpToPx(fragment.requireContext(), 160);
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
    int size16dp = UnitUtil.dpToPx(fragment.requireContext(), 16);
    layoutParamsPreview.setMargins(size16dp, size16dp, size16dp, size16dp);
    previewView = new PreviewView(fragment.requireContext());
    previewView.setLayoutParams(layoutParamsPreview);
    containerScanner.addView(previewView);
    graphicOverlay = new GraphicOverlay(fragment.requireContext());
    graphicOverlay.setLayoutParams(layoutParamsPreview);
    containerScanner.addView(graphicOverlay);
    LayoutParams layoutParamsCard = new LayoutParams(matchParent, matchParent);
    int size12dp = UnitUtil.dpToPx(fragment.requireContext(), 12);
    layoutParamsCard.setMargins(size12dp, size12dp, size12dp, size12dp);
    MaterialCardView cardView = new MaterialCardView(fragment.requireContext());
    cardView.setLayoutParams(layoutParamsCard);
    cardView.setCardElevation(0);
    int backgroundColor = ContextCompat.getColor(fragment.requireContext(), R.color.transparent);
    cardView.setCardBackgroundColor(backgroundColor);
    cardView.setStrokeWidth(UnitUtil.dpToPx(fragment.requireContext(), 5));
    int strokeColor = ContextCompat.getColor(fragment.requireContext(), R.color.grey800);
    cardView.setStrokeColor(strokeColor);
    cardView.setRadius(UnitUtil.dpToPx(fragment.requireContext(), 10));
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

    processCameraProvider = new ViewModelProvider(fragment, (Factory) AndroidViewModelFactory
        .getInstance(fragment.requireActivity().getApplication()))
        .get(CameraXViewModel.class)
        .getProcessCameraProvider();

    lensFacing = sharedPrefs.getBoolean(SCANNER.FRONT_CAM, SETTINGS_DEFAULT.SCANNER.FRONT_CAM)
        ? CameraSelector.LENS_FACING_FRONT
        : CameraSelector.LENS_FACING_BACK;

    cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
  }

  @Override
  public void setScannerVisibilityLive(LiveData<Boolean> scannerVisibilityLive) {
    setScannerVisibilityLive(scannerVisibilityLive, false);
  }

  public void setScannerVisibilityLive(
      LiveData<Boolean> scannerVisibilityLive,
      boolean supressNextScanStart
  ) {
    this.supressNextScanStart = supressNextScanStart;
    if (scannerVisibilityLive.hasObservers()) {
      scannerVisibilityLive.removeObservers(fragment.getViewLifecycleOwner());
    }
    scannerVisibilityLive.observe(fragment.getViewLifecycleOwner(), visible -> {
      isScannerVisible = visible;
      if (visible) {
        processCameraProvider.observe(
            fragment.getViewLifecycleOwner(),
            provider -> {
              cameraProvider = provider;
              if (this.supressNextScanStart) {
                this.supressNextScanStart = false;
                return;
              }
              startScannerIfVisible();
            });
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
    if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
      camera.getCameraControl().enableTorch(false);
    }
    lockOrUnlockRotation(false);
  }

  void stopScanner() {
    keepScreenOn(false);
    if (imageProcessor != null) {
      imageProcessor.stop();
    }
    if (cameraProvider != null) {
      cameraProvider.unbindAll();
    }
  }

  @SuppressLint("UnsafeOptInUsageError")
  public void startScannerIfVisible() {
    if (cameraProvider == null || !isScannerVisible) return;
    if (!allPermissionsGranted()) {
      getRuntimePermissions();
      return;
    }
    // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.

    previewView.postDelayed(() -> {
      cameraProvider.unbindAll();

      UseCaseGroup.Builder useCaseGroupBuilder = new UseCaseGroup.Builder();
      bindPreviewUseCase(useCaseGroupBuilder);
      bindAnalysisUseCase(useCaseGroupBuilder);
      camera = cameraProvider.bindToLifecycle(fragment, cameraSelector, useCaseGroupBuilder.build());
    }, 100);  // wait until animation is finished and previewView has height and width != 0

    keepScreenOn(true);
  }

  @SuppressLint("UnsafeOptInUsageError")
  private void bindPreviewUseCase(UseCaseGroup.Builder useCaseGroupBuilder) {
    if (cameraProvider == null) {
      return;
    }

    Preview.Builder builder = new Preview.Builder();
    Size targetResolution = new Size(1280, 720);
    builder.setTargetResolution(targetResolution);
    Preview previewUseCase = builder.build();
    previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());

    useCaseGroupBuilder.setViewPort(previewView.getViewPort());
    useCaseGroupBuilder.addUseCase(previewUseCase);
  }

  @SuppressLint("UnsafeOptInUsageError")
  private void bindAnalysisUseCase(UseCaseGroup.Builder useCaseGroupBuilder) {
    if (cameraProvider == null) {
      return;
    }
    if (imageProcessor != null) {
      imageProcessor.stop();
    }

    try {
      imageProcessor = new BarcodeScannerProcessor(barcodeScannerOptions, cropImageToPreviewRect) {
        @Override
        protected void onSuccess(@NonNull List<Barcode> barcodes,
            @NonNull GraphicOverlay graphicOverlay) {
          if (barcodes.isEmpty() || barcodes.get(0) == null || barcodes.get(0).getRawValue() == null
              || Objects.equals(barcodes.get(0).getRawValue(), "")) {
            return;
          }
          new HapticUtil(fragment.requireContext()).tick();
          //super.onSuccess(barcodes, graphicOverlay);
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
      };
    } catch (Exception e) {
      Log.e(TAG, "Cannot create image processor. ", e);
      Toast.makeText(
          activity.getApplicationContext(),
          "Cannot create image processor: " + e.getLocalizedMessage(),
          Toast.LENGTH_LONG)
          .show();
      return;
    }

    ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
    Size targetResolution = new Size(1280, 720);
    builder.setTargetResolution(targetResolution);
    ImageAnalysis analysisUseCase = builder.build();

    needUpdateGraphicOverlayImageSourceInfo = true;
    analysisUseCase.setAnalyzer(
        // imageProcessor.processImageProxy will use another thread to run the detection underneath,
        // thus we can just runs the analyzer itself on main thread.
        ContextCompat.getMainExecutor(fragment.requireContext()),
        imageProxy -> {
          if (needUpdateGraphicOverlayImageSourceInfo) {
            boolean isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT;
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
            if (rotationDegrees == 0 || rotationDegrees == 180) {
              graphicOverlay.setImageSourceInfo(
                  imageProxy.getWidth(), imageProxy.getHeight(), isImageFlipped);
            } else {
              graphicOverlay.setImageSourceInfo(
                  imageProxy.getHeight(), imageProxy.getWidth(), isImageFlipped);
            }
            needUpdateGraphicOverlayImageSourceInfo = false;
          }
          try {
            imageProcessor.processImageProxy(imageProxy, graphicOverlay);
          } catch (Exception e) {
            Log.e(TAG, "Failed to process image. Error: " + e.getLocalizedMessage());
            Toast.makeText(activity.getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                .show();
          }
        });

    useCaseGroupBuilder.addUseCase(analysisUseCase);
  }

  public void toggleTorch() {
    if (camera == null || !camera.getCameraInfo().hasFlashUnit()) return;
    assert camera.getCameraInfo().getTorchState().getValue() != null;
    int state = camera.getCameraInfo().getTorchState().getValue();
    camera.getCameraControl().enableTorch(state == TorchState.OFF);
  }

  private String[] getRequiredPermissions() {
    try {
      PackageInfo info = activity.getPackageManager()
              .getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
      String[] ps = info.requestedPermissions;
      if (ps != null && ps.length > 0) {
        return ps;
      } else {
        return new String[0];
      }
    } catch (Exception e) {
      return new String[0];
    }
  }

  private boolean allPermissionsGranted() {
    for (String permission : getRequiredPermissions()) {
      if (isPermissionNotGranted(fragment.requireContext(), permission)) {
        return false;
      }
    }
    return true;
  }

  private void getRuntimePermissions() {
    List<String> allNeededPermissions = new ArrayList<>();
    for (String permission : getRequiredPermissions()) {
      if (isPermissionNotGranted(fragment.requireContext(), permission)) {
        allNeededPermissions.add(permission);
      }
    }

    if (!allNeededPermissions.isEmpty()) {
      ActivityCompat.requestPermissions(
          activity, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
    }
  }

  private static boolean isPermissionNotGranted(Context context, String permission) {
    if (ContextCompat.checkSelfPermission(context, permission)
        == PackageManager.PERMISSION_GRANTED) {
      Log.i(TAG, "Permission granted: " + permission);
      return false;
    }
    Log.i(TAG, "Permission NOT granted: " + permission);
    return true;
  }

  private ArrayList<Integer> getEnabledBarcodeFormats() {
    ArrayList<Integer> enabledBarcodeFormats = new ArrayList<>();
    Set<String> enabledBarcodeFormatsSet = sharedPrefs.getStringSet(
        SCANNER.BARCODE_FORMATS,
        SETTINGS_DEFAULT.SCANNER.BARCODE_FORMATS
    );
    if (enabledBarcodeFormatsSet != null && !enabledBarcodeFormatsSet.isEmpty()) {
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

  public static final class CameraXViewModel extends AndroidViewModel {

    private static final String TAG = "CameraXViewModel";
    private MutableLiveData<ProcessCameraProvider> cameraProviderLiveData;

    /**
     * Create an instance which interacts with the camera service via the given application context.
     */
    public CameraXViewModel(@NonNull Application application) {
      super(application);
    }

    public LiveData<ProcessCameraProvider> getProcessCameraProvider() {
      if (cameraProviderLiveData == null) {
        cameraProviderLiveData = new MutableLiveData<>();

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
            ProcessCameraProvider.getInstance(getApplication());
        cameraProviderFuture.addListener(
            () -> {
              try {
                cameraProviderLiveData.setValue(cameraProviderFuture.get());
              } catch (ExecutionException | InterruptedException e) {
                // Handle any errors (including cancellation) here.
                Log.e(TAG, "Unhandled exception", e);
              }
            },
            ContextCompat.getMainExecutor(getApplication()));
      }

      return cameraProviderLiveData;
    }
  }
}
