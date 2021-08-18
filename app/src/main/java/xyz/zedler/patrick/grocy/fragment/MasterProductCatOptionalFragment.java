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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterProductCatOptionalBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductGroupsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.TextEditBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.scan.ScanInputCaptureManager;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.viewmodel.MasterProductCatOptionalViewModel;

public class MasterProductCatOptionalFragment extends BaseFragment implements
    ScanInputCaptureManager.BarcodeListener {

  private final static String TAG = MasterProductCatOptionalFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentMasterProductCatOptionalBinding binding;
  private MasterProductCatOptionalViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private ScanInputCaptureManager capture;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentMasterProductCatOptionalBinding.inflate(
        inflater, container, false
    );
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    MasterProductFragmentArgs args = MasterProductFragmentArgs
        .fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new MasterProductCatOptionalViewModel
        .MasterProductCatOptionalViewModelFactory(activity.getApplication(), args)
    ).get(MasterProductCatOptionalViewModel.class);
    binding.setActivity(activity);
    binding.setFormData(viewModel.getFormData());
    binding.setViewModel(viewModel);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        SnackbarMessage message = (SnackbarMessage) event;
        Snackbar snack = message.getSnackbar(activity, activity.binding.frameMainContainer);
        activity.showSnackbar(snack);
      } else if (event.getType() == Event.NAVIGATE_UP) {
        activity.navigateUp();
      } else if (event.getType() == Event.SET_SHOPPING_LIST_ID) {
        int id = event.getBundle().getInt(Constants.ARGUMENT.SELECTED_ID);
        setForDestination(R.id.shoppingListFragment, Constants.ARGUMENT.SELECTED_ID, id);
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);
    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getFormData().getScannerVisibilityLive().observe(getViewLifecycleOwner(), visible -> {
      if (visible) {
        capture.onResume();
        capture.decode();
      } else {
        capture.onPause();
      }
      lockOrUnlockRotation(visible);
    });

    viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), isLoading -> {
      if (!isLoading) {
        viewModel.setCurrentQueueLoading(null);
      }
    });

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

    updateUI(savedInstanceState == null);
  }

  private void updateUI(boolean animated) {
    activity.getScrollBehavior().setUpScroll(R.id.scroll);
    activity.getScrollBehavior().setHideOnScroll(true);
    activity.updateBottomAppBar(
        Constants.FAB.POSITION.END,
        viewModel.isActionEdit() ? R.menu.menu_master_product_edit : R.menu.menu_empty,
        menuItem -> {
          if (menuItem.getItemId() != R.id.action_delete) {
            return false;
          }
          setForDestination(
              R.id.masterProductFragment,
              Constants.ARGUMENT.ACTION,
              Constants.ACTION.DELETE
          );
          activity.onBackPressed();
          return true;
        }
    );
    activity.updateFab(
        R.drawable.ic_round_backup,
        R.string.action_save,
        Constants.FAB.TAG.SAVE,
        animated,
        () -> {
          setForDestination(
              R.id.masterProductFragment,
              Constants.ARGUMENT.ACTION,
              Constants.ACTION.SAVE
          );
          activity.onBackPressed();
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
    if (result == null || result.getText().isEmpty()) {
      resumeScan();
      return;
    }
    viewModel.getFormData().toggleScannerVisibility();
    viewModel.onBarcodeRecognized(result.getText());
  }

  public void toggleTorch() {
    if (viewModel.getFormData().isTorchOn()) {
      binding.barcodeScan.setTorchOff();
    } else {
      binding.barcodeScan.setTorchOn();
    }
  }

  public void clearInputFocus() {
    activity.hideKeyboard();
    binding.textInputParentProduct.clearFocus();
    binding.energy.clearFocus();
  }

  public void onItemAutoCompleteClick(AdapterView<?> adapterView, int pos) {
    Product product = (Product) adapterView.getItemAtPosition(pos);
    viewModel.getFormData().getProductLive().setValue(product);
    clearInputFocus();
  }

  public void onParentProductInputFocusChanged(boolean hasFocus) {
    if (hasFocus) {
      return;
    }
    viewModel.getFormData().isParentProductValid();
  }

  public void showDescriptionBottomSheet() {
    Bundle bundle = new Bundle();
    bundle.putString(Constants.ARGUMENT.TITLE, getString(R.string.title_edit_description));
    bundle.putString(Constants.ARGUMENT.HINT, getString(R.string.property_description));
    Spanned description = viewModel.getFormData().getDescriptionLive().getValue();
    bundle.putString(
        Constants.ARGUMENT.HTML,
        description != null ? Html.toHtml(description) : null
    );
    activity.showBottomSheet(new TextEditBottomSheet(), bundle);
  }

  @Override
  public void saveText(Spanned text) {
    viewModel.getFormData().getDescriptionLive().setValue(text);
  }

  public void showProductGroupsBottomSheet() {
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(
        Constants.ARGUMENT.PRODUCT_GROUPS,
        viewModel.getFormData().getProductGroupsLive().getValue()
    );

    ProductGroup productGroup = viewModel.getFormData().getProductGroupLive().getValue();
    int productGroupId = productGroup != null ? productGroup.getId() : -1;
    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, productGroupId);
    activity.showBottomSheet(new ProductGroupsBottomSheet(), bundle);
  }

  @Override
  public void selectProductGroup(ProductGroup productGroup) {
    viewModel.getFormData().getProductGroupLive().setValue(productGroup);
  }

  @Override
  public boolean onBackPressed() {
    setForDestination(
        R.id.masterProductFragment,
        Constants.ARGUMENT.PRODUCT,
        viewModel.getFilledProduct()
    );
    return false;
  }

  private void lockOrUnlockRotation(boolean scannerIsVisible) {
    if (scannerIsVisible) {
      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    } else {
      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
    }
  }

  @Override
  public void updateConnectivity(boolean isOnline) {
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.setOfflineLive(!isOnline);
    if (isOnline) {
      viewModel.downloadData();
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
