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

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentInventoryBinding;
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
import xyz.zedler.patrick.grocy.viewmodel.InventoryViewModel;
import xyz.zedler.patrick.grocy.viewmodel.InventoryViewModel.InventoryViewModelFactory;

public class InventoryFragment extends BaseFragment implements
    ScanInputCaptureManager.BarcodeListener {

  private final static String TAG = InventoryFragment.class.getSimpleName();

  private MainActivity activity;
  private InventoryFragmentArgs args;
  private FragmentInventoryBinding binding;
  private InventoryViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private ScanInputCaptureManager capture;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentInventoryBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (infoFullscreenHelper != null) {
      infoFullscreenHelper.destroyInstance();
      infoFullscreenHelper = null;
    }
    binding.barcodeScan.setTorchOff();
    lockOrUnlockRotation(false);
    binding = null;
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();

    args = InventoryFragmentArgs.fromBundle(requireArguments());

    viewModel = new ViewModelProvider(this,
        new InventoryViewModelFactory(activity.getApplication(), args)
    ).get(InventoryViewModel.class);
    binding.setActivity(activity);
    binding.setViewModel(viewModel);
    binding.setFragment(this);
    binding.setFormData(viewModel.getFormData());
    binding.setLifecycleOwner(getViewLifecycleOwner());

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);

    // INITIALIZE VIEWS

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
        if (PurchaseFragmentArgs.fromBundle(getArguments()).getCloseWhenFinished()) {
          activity.navigateUp();
        } else {
          viewModel.getFormData().clearForm();
          focusProductInputIfNecessary();
          if (viewModel.getFormData().isScannerVisible()) {
            capture.onResume();
            capture.decode();
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
      viewModel.setQueueEmptyAction(() -> viewModel.setProduct(productIdSavedSate));
    } else if (NumUtil.isStringInt(args.getProductId())) {
      int productId = Integer.parseInt(args.getProductId());
      setArguments(new InventoryFragmentArgs.Builder(args)
          .setProductId(null).build().toBundle());
      viewModel.setProduct(productId);
    }

    viewModel.getFormData().getScannerVisibilityLive().observe(getViewLifecycleOwner(), visible -> {
      if (visible) {
        capture.onResume();
        capture.decode();
      } else {
        capture.onPause();
      }
      lockOrUnlockRotation(visible);
    });
    // following lines are necessary because no observers are set in Views
    viewModel.getFormData().getQuantityUnitStockLive().observe(getViewLifecycleOwner(), i -> {
    });

    //hideDisabledFeatures();

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    binding.barcodeScan.setTorchOff();
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
    capture = new ScanInputCaptureManager(activity, binding.barcodeScan, this);

    focusProductInputIfNecessary();

    setHasOptionsMenu(true);

    updateUI(args.getAnimateStart() && savedInstanceState == null);
  }

  private void updateUI(boolean animated) {
    activity.getScrollBehavior().setUpScroll(R.id.scroll_inventory);
    activity.getScrollBehavior().setHideOnScroll(false);
    activity.updateBottomAppBar(
        Constants.FAB.POSITION.END,
        R.menu.menu_purchase,
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
            viewModel.inventoryProduct();
          }
        }
    );
  }

  @Override
  public void onResume() {
    super.onResume();
    if (viewModel.getFormData().isScannerVisible()) {
      capture.onResume();
    }
  }

  @Override
  public void onPause() {
    if (viewModel.getFormData().isScannerVisible()) {
      capture.onPause();
    }
    super.onPause();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (viewModel.getFormData().isScannerVisible()) {
      return binding.barcodeScan.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public void onDestroy() {
    if (capture != null) {
      capture.onDestroy();
    }
    super.onDestroy();
  }

  @Override
  public void onBarcodeResult(BarcodeResult result) {
    if (result.getText().isEmpty()) {
      resumeScan();
      return;
    }
    if (!viewModel.isQuickModeEnabled()) {
      viewModel.getFormData().toggleScannerVisibility();
    }
    viewModel.onBarcodeRecognized(result.getText());
  }

  public void toggleTorch() {
    if (viewModel.getFormData().isTorchOn()) {
      binding.barcodeScan.setTorchOff();
    } else {
      binding.barcodeScan.setTorchOn();
    }
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
    viewModel.setProduct(product.getId());
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

  private void lockOrUnlockRotation(boolean scannerIsVisible) {
    if (scannerIsVisible) {
      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    } else {
      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
    }
  }

  @Override
  public void startTransaction() {
    viewModel.inventoryProduct();
  }

  @Override
  public void onBottomSheetDismissed() {
    if (!viewModel.isQuickModeEnabled()) {
      clearInputFocus();
    } else {
      focusNextInvalidView();
    }
  }

  private void hideDisabledFeatures() {
    if (!viewModel.isFeatureEnabled(Constants.PREF.FEATURE_STOCK_PRICE_TRACKING)) {
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
        capture.onResume();
        capture.decode();
      }
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
