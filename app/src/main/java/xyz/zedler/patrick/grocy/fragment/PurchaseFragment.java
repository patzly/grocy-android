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

package xyz.zedler.patrick.grocy.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.TorchState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.zxing.Result;
import com.journeyapps.barcodescanner.BarcodeResult;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.ShoppingListItemAdapter;
import xyz.zedler.patrick.grocy.barcode.BarcodeScannerProcessor;
import xyz.zedler.patrick.grocy.barcode.CameraXViewModel;
import xyz.zedler.patrick.grocy.barcode.GraphicOverlay;
import xyz.zedler.patrick.grocy.barcode.PreferenceUtils;
import xyz.zedler.patrick.grocy.barcode.VisionImageProcessor;
import xyz.zedler.patrick.grocy.databinding.FragmentPurchaseBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheetArgs;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.scan.ScanInputCaptureManager;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.viewmodel.PurchaseViewModel;

public class PurchaseFragment extends BaseFragment implements
    ScanInputCaptureManager.BarcodeListener, OnRequestPermissionsResultCallback {

  private final static String TAG = PurchaseFragment.class.getSimpleName();

  private MainActivity activity;
  private PurchaseFragmentArgs args;
  private FragmentPurchaseBinding binding;
  private PurchaseViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private PluralUtil pluralUtil;

  private static final int PERMISSION_REQUESTS = 1;

  private GraphicOverlay graphicOverlay;

  private LiveData<ProcessCameraProvider> processCameraProvider;

  @Nullable private ProcessCameraProvider cameraProvider;
  @Nullable private ImageAnalysis analysisUseCase;
  @Nullable private VisionImageProcessor imageProcessor;
  private boolean needUpdateGraphicOverlayImageSourceInfo;

  private int lensFacing = CameraSelector.LENS_FACING_BACK;
  private CameraSelector cameraSelector;
  private Camera camera;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentPurchaseBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (infoFullscreenHelper != null) {
      infoFullscreenHelper.destroyInstance();
      infoFullscreenHelper = null;
    }
    if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
      camera.getCameraControl().enableTorch(false);
    }
    lockOrUnlockRotation(false);
    binding = null;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    registerForActivityResult(
        new ActivityResultContracts.RequestPermission(),
        result -> {
          if(result && viewModel != null && viewModel.getFormData().isScannerVisible()
              && allPermissionsGranted()) {
            startCamera();
          }
        });
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();

    args = PurchaseFragmentArgs.fromBundle(requireArguments());

    viewModel = new ViewModelProvider(this, new PurchaseViewModel
        .PurchaseViewModelFactory(activity.getApplication(), args)
    ).get(PurchaseViewModel.class);
    binding.setActivity(activity);
    binding.setViewModel(viewModel);
    binding.setFragment(this);
    binding.setFormData(viewModel.getFormData());
    binding.setLifecycleOwner(getViewLifecycleOwner());

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);

    // INITIALIZE VIEWS

    if (args.getShoppingListItems() != null) {
      binding.containerBatchMode.setVisibility(View.VISIBLE);
      binding.linearPurchaseShoppingListItem.containerRow.setBackground(
          ContextCompat.getDrawable(activity, R.drawable.bg_list_item_visible_ripple)
      );
    }

    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );
    viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), isDownloading ->
        binding.swipePurchase.setRefreshing(isDownloading)
    );
    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(((SnackbarMessage) event).getSnackbar(
            activity,
            activity.binding.frameMainContainer
        ));
      } else if (event.getType() == Event.TRANSACTION_SUCCESS) {
        assert getArguments() != null;
        if (args.getShoppingListItems() != null) {
          clearInputFocus();
          viewModel.getFormData().clearForm();
          boolean nextItemValid = viewModel.batchModeNextItem();
          if (!nextItemValid) activity.navigateUp();
        } else if (PurchaseFragmentArgs.fromBundle(getArguments()).getCloseWhenFinished()) {
          activity.navigateUp();
        } else {
          viewModel.getFormData().clearForm();
          focusProductInputIfNecessary();
          if (viewModel.getFormData().isScannerVisible()) {
            startCamera();
          }
        }
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      } else if (event.getType() == Event.FOCUS_INVALID_VIEWS) {
        focusNextInvalidView();
      } else if (event.getType() == Event.QUICK_MODE_ENABLED) {
        focusProductInputIfNecessary();
      } else if (event.getType() == Event.QUICK_MODE_DISABLED) {
        clearInputFocus();
      }
    });

    Integer productIdSavedSate = (Integer) getFromThisDestinationNow(Constants.ARGUMENT.PRODUCT_ID);
    if (productIdSavedSate != null) {
      removeForThisDestination(Constants.ARGUMENT.PRODUCT_ID);
      viewModel.setQueueEmptyAction(() -> viewModel.setProduct(
          productIdSavedSate, null, null
      ));
    } else if (NumUtil.isStringInt(args.getProductId())) {
      int productId = Integer.parseInt(args.getProductId());
      setArguments(new PurchaseFragmentArgs.Builder(args)
          .setProductId(null).build().toBundle());
      viewModel.setQueueEmptyAction(() -> viewModel.setProduct(
          productId, null, null
      ));
    }

    pluralUtil = new PluralUtil(activity);
    viewModel.getFormData().getShoppingListItemLive().observe(getViewLifecycleOwner(), item -> {
      if(args.getShoppingListItems() == null || item == null) return;
      ShoppingListItemAdapter.fillShoppingListItem(
          requireContext(),
          item,
          binding.linearPurchaseShoppingListItem,
          viewModel.getProductHashMap(),
          viewModel.getQuantityUnitHashMap(),
          viewModel.getShoppingListItemAmountsHashMap(),
          pluralUtil
      );
    });

    processCameraProvider = new ViewModelProvider(this, AndroidViewModelFactory.getInstance(activity.getApplication()))
        .get(CameraXViewModel.class)
        .getProcessCameraProvider();

    viewModel.getFormData().getScannerVisibilityLive().observe(getViewLifecycleOwner(), visible -> {
      if (visible) {
        processCameraProvider.observe(
            getViewLifecycleOwner(),
            provider -> {
              cameraProvider = provider;
              startCamera();
            });
      } else {
        stopCamera();
      }
      lockOrUnlockRotation(visible);
    });
    // following lines are necessary because no observers are set in Views
    viewModel.getFormData().getPriceStockLive().observe(getViewLifecycleOwner(), i -> {
    });
    viewModel.getFormData().getQuantityUnitStockLive().observe(getViewLifecycleOwner(), i -> {
    });

    //hideDisabledFeatures();

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }


    /*binding.barcodeScan.setTorchOff();
    binding.barcodeScan.setTorchListener(new DecoratedBarcodeView.TorchListener() {
      @Override
      public void onTorchOn() {
        viewModel.getFormData().setTorchOn(true);
      }

      @Override
      public void onTorchOff() {
        viewModel.getFormData().setTorchOn(false);
      }
    });
    CameraSettings cameraSettings = new CameraSettings();
    cameraSettings.setRequestedCameraId(viewModel.getUseFrontCam() ? 1 : 0);
    binding.barcodeScan.getBarcodeView().setCameraSettings(cameraSettings);
    capture = new ScanInputCaptureManager(activity, binding.barcodeScan, this);*/

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
    String prefKey = requireContext().getString(R.string.pref_key_camera_live_viewport);
    sharedPreferences.edit().putBoolean(prefKey, false).apply();

    cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
    graphicOverlay = binding.graphicOverlay;




    focusProductInputIfNecessary();

    setHasOptionsMenu(true);

    updateUI(args.getAnimateStart() && savedInstanceState == null);
  }

  private void updateUI(boolean animated) {
    activity.getScrollBehavior().setUpScroll(R.id.scroll_purchase);
    activity.getScrollBehavior().setHideOnScroll(false);
    activity.updateBottomAppBar(
        Constants.FAB.POSITION.END,
        args.getShoppingListItems() != null
            ? R.menu.menu_purchase_shopping_list
            : R.menu.menu_purchase,
        this::onMenuItemClick
    );
    activity.updateFab(
        R.drawable.ic_round_local_grocery_store,
        R.string.action_purchase,
        Constants.FAB.TAG.PURCHASE,
        animated,
        () -> {
          if (viewModel.isQuickModeEnabled()) {
            focusNextInvalidView();
          } else if (!viewModel.getFormData().isProductNameValid()) {
            clearFocusAndCheckProductInput();
          } else {
            viewModel.purchaseProduct();
          }
        }
    );
  }

  @Override
  public void onResume() {
    super.onResume();
    if (viewModel.getFormData().isScannerVisible()) {
      //capture.onResume();
      startCamera();
    }
  }

  @Override
  public void onPause() {
    if (viewModel.getFormData().isScannerVisible()) {
      //capture.onPause();
    }
    stopCamera();
    super.onPause();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (viewModel.getFormData().isScannerVisible()) {
      //return binding.barcodeScan.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public void onDestroy() {
    /*if (capture != null) {
      capture.onDestroy();
    }*/
    stopCamera();
    super.onDestroy();
  }





  private void stopCamera() {
    if (imageProcessor != null) {
      imageProcessor.stop();
    }
    if (cameraProvider != null) {
      cameraProvider.unbindAll();
    }
  }

  private void startCamera() {
    if (cameraProvider == null || !viewModel.getFormData().isScannerVisible()) return;
    if (!allPermissionsGranted()) {
      getRuntimePermissions();
      return;
    }
    // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
    cameraProvider.unbindAll();
    bindAnalysisUseCase();
  }

  private void bindAnalysisUseCase() {
    if (cameraProvider == null) {
      return;
    }
    if (analysisUseCase != null) {
      cameraProvider.unbind(analysisUseCase);
    }
    if (imageProcessor != null) {
      imageProcessor.stop();
    }

    try {
      imageProcessor = new BarcodeScannerProcessor(requireContext()) {
        @Override
        protected void onSuccess(@NonNull List<Barcode> barcodes,
            @NonNull GraphicOverlay graphicOverlay) {
          if (barcodes.isEmpty()) {
            return;
          }
          stopCamera();
          Result result = new Result(barcodes.get(0).getRawValue(), null, null, null, 1);
          BarcodeResult barcodeResult = new BarcodeResult(result, null);
          onBarcodeResult(barcodeResult);
        }
      };
    } catch (Exception e) {
      Log.e(TAG, "Can not create image processor. ", e);
      Toast.makeText(
          activity.getApplicationContext(),
          "Can not create image processor: " + e.getLocalizedMessage(),
          Toast.LENGTH_LONG)
          .show();
      return;
    }

    ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
    Size targetResolution = PreferenceUtils.getCameraXTargetResolution(requireContext(), lensFacing);
    if (targetResolution != null) {
      builder.setTargetResolution(targetResolution);
    }
    analysisUseCase = builder.build();

    needUpdateGraphicOverlayImageSourceInfo = true;
    analysisUseCase.setAnalyzer(
        // imageProcessor.processImageProxy will use another thread to run the detection underneath,
        // thus we can just runs the analyzer itself on main thread.
        ContextCompat.getMainExecutor(requireContext()),
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
          } catch (MlKitException e) {
            Log.e(TAG, "Failed to process image. Error: " + e.getLocalizedMessage());
            Toast.makeText(activity.getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                .show();
          }
        });

    camera = cameraProvider.bindToLifecycle(this, cameraSelector, analysisUseCase);
  }

  private String[] getRequiredPermissions() {
    try {
      PackageInfo info =
          requireActivity().getPackageManager()
              .getPackageInfo(requireActivity().getPackageName(), PackageManager.GET_PERMISSIONS);
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
      if (isPermissionNotGranted(requireContext(), permission)) {
        return false;
      }
    }
    return true;
  }

  private void getRuntimePermissions() {
    List<String> allNeededPermissions = new ArrayList<>();
    for (String permission : getRequiredPermissions()) {
      if (isPermissionNotGranted(requireContext(), permission)) {
        allNeededPermissions.add(permission);
      }
    }

    if (!allNeededPermissions.isEmpty()) {
      ActivityCompat.requestPermissions(
          requireActivity(), allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
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







  @Override
  public void onBarcodeResult(BarcodeResult result) {
    if (result.getText().isEmpty()) {
      resumeScan();
      return;
    }
    clearInputFocus();
    if (!viewModel.isQuickModeEnabled()) {
      viewModel.getFormData().toggleScannerVisibility();
    }
    viewModel.onBarcodeRecognized(result.getText());
  }

  public void toggleTorch() {
    if (viewModel.getFormData().isTorchOn()) {
      //binding.barcodeScan.setTorchOff();
    } else {
      //binding.barcodeScan.setTorchOn();
    }
    if (camera == null || !camera.getCameraInfo().hasFlashUnit()) return;
    assert camera.getCameraInfo().getTorchState().getValue() != null;
    int state = camera.getCameraInfo().getTorchState().getValue();
    camera.getCameraControl().enableTorch(state == TorchState.OFF);
  }

  @Override
  public int getSelectedQuantityUnitId() {
    QuantityUnit selectedId = viewModel.getFormData().getQuantityUnitLive().getValue();
    if (selectedId == null) {
      return -1;
    }
    return selectedId.getId();
  }

  @Override
  public void selectQuantityUnit(QuantityUnit quantityUnit) {
    viewModel.getFormData().getQuantityUnitLive().setValue(quantityUnit);
  }

  @Override
  public void selectPurchasedDate(String purchasedDate) {
    viewModel.getFormData().getPurchasedDateLive().setValue(purchasedDate);
  }

  @Override
  public void selectDueDate(String dueDate) {
    viewModel.getFormData().getDueDateLive().setValue(dueDate);
    viewModel.getFormData().isDueDateValid();
  }

  @Override
  public void selectStore(Store store) {
    viewModel.getFormData().getStoreLive().setValue(store.getId() != -1 ? store : null);
  }

  @Override
  public void selectLocation(Location location) {
    viewModel.getFormData().getLocationLive().setValue(location);
  }

  @Override
  public void addBarcodeToExistingProduct(String barcode) {
    viewModel.addBarcodeToExistingProduct(barcode);
    binding.autoCompletePurchaseProduct.requestFocus();
    activity.showKeyboard(binding.autoCompletePurchaseProduct);
  }

  @Override
  public void addBarcodeToNewProduct(String barcode) {
    viewModel.addBarcodeToExistingProduct(barcode);
  }

  public void toggleScannerVisibility() {
    viewModel.getFormData().toggleScannerVisibility();
    if (viewModel.getFormData().isScannerVisible()) {
      clearInputFocus();
    }
  }

  public void clearAmountFieldAndFocusIt() {
    binding.editTextAmount.setText("");
    activity.showKeyboard(binding.editTextAmount);
  }

  public void clearInputFocus() {
    activity.hideKeyboard();
    binding.dummyFocusView.requestFocus();
    binding.autoCompletePurchaseProduct.clearFocus();
    binding.quantityUnitContainer.clearFocus();
    binding.textInputAmount.clearFocus();
    binding.linearDueDate.clearFocus();
    binding.textInputPurchasePrice.clearFocus();
  }

  public void onItemAutoCompleteClick(AdapterView<?> adapterView, int pos) {
    Product product = (Product) adapterView.getItemAtPosition(pos);
    clearInputFocus();
    if (product == null) {
      return;
    }
    viewModel.setProduct(product.getId(), null, null);
  }

  public void clearFocusAndCheckProductInput() {
    clearInputFocus();
    viewModel.checkProductInput();
  }

  public void focusProductInputIfNecessary() {
    if (!viewModel.isQuickModeEnabled() || viewModel.getFormData().isScannerVisible()) {
      return;
    }
    ProductDetails productDetails = viewModel.getFormData().getProductDetailsLive().getValue();
    String productNameInput = viewModel.getFormData().getProductNameLive().getValue();
    if (productDetails == null && (productNameInput == null || productNameInput.isEmpty())) {
      binding.autoCompletePurchaseProduct.requestFocus();
      if (viewModel.getFormData().getExternalScannerEnabled()) {
        activity.hideKeyboard();
      } else {
        activity.showKeyboard(binding.autoCompletePurchaseProduct);
      }
    }
  }

  public void focusNextInvalidView() {
    View nextView = null;
    if (!viewModel.getFormData().isProductNameValid()) {
      nextView = binding.autoCompletePurchaseProduct;
    } else if (!viewModel.getFormData().isAmountValid()) {
      nextView = binding.editTextAmount;
    } else if (!viewModel.getFormData().isDueDateValid()) {
      nextView = binding.linearDueDate;
    }
    if (nextView == null) {
      clearInputFocus();
      viewModel.showConfirmationBottomSheet();
      return;
    }
    nextView.requestFocus();
    if (nextView instanceof EditText) {
      activity.showKeyboard((EditText) nextView);
    }
  }

  public void clearInputFocusOrFocusNextInvalidView() {
    if (viewModel.isQuickModeEnabled()) {
      focusNextInvalidView();
    } else {
      clearInputFocus();
    }
  }

  private void lockOrUnlockRotation(boolean scannerIsVisible) {
    if (scannerIsVisible) {
      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    } else {
      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
    }
  }

  @Override
  public void startTransaction() {
    viewModel.purchaseProduct();
  }

  @Override
  public void onBottomSheetDismissed() {
    clearInputFocusOrFocusNextInvalidView();
  }

  private void hideDisabledFeatures() {
    if (!viewModel.isFeatureEnabled(Constants.PREF.FEATURE_STOCK_PRICE_TRACKING)) {
      binding.linearPurchaseTotalPrice.setVisibility(View.GONE);
      binding.linearPurchasePrice.setVisibility(View.GONE);
    }
    if (!viewModel.isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
      binding.linearPurchaseLocation.setVisibility(View.GONE);
    }
    if (!viewModel.isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
      binding.linearDueDate.setVisibility(View.GONE);
    }
  }

  private boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.action_product_overview) {
      IconUtil.start(item);
      if (!viewModel.getFormData().isProductNameValid()) {
        return false;
      }
      activity.showBottomSheet(
          new ProductOverviewBottomSheet(),
          new ProductOverviewBottomSheetArgs.Builder()
              .setProductDetails(viewModel.getFormData().getProductDetailsLive().getValue()).build()
              .toBundle()
      );
      return true;
    } else if (item.getItemId() == R.id.action_clear_form) {
      IconUtil.start(item);
      clearInputFocus();
      viewModel.getFormData().clearForm();
      if (viewModel.getFormData().isScannerVisible()) {
        /*capture.onResume();
        capture.decode();*/
      }
      return true;
    } else if (item.getItemId() == R.id.action_skip) {
      IconUtil.start(item);
      clearInputFocus();
      viewModel.getFormData().clearForm();
      boolean nextItemValid = viewModel.batchModeNextItem();
      if (!nextItemValid) activity.navigateUp();
      return true;
    }
    return false;
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
