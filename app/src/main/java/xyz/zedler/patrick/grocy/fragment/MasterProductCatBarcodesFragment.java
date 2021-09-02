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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.adapter.ProductBarcodeAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterProductCatBarcodesBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.FormDataMasterProductCatAmount;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScannerBundle;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.viewmodel.MasterProductCatBarcodesViewModel;

public class MasterProductCatBarcodesFragment extends BaseFragment implements
    ProductBarcodeAdapter.ProductBarcodeAdapterListener, EmbeddedFragmentScanner.BarcodeListener {

  private final static String TAG = MasterProductCatBarcodesFragment.class.getSimpleName();

  private MainActivity activity;
  private ClickUtil clickUtil;
  private FragmentMasterProductCatBarcodesBinding binding;
  private MasterProductCatBarcodesViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private EmbeddedFragmentScanner embeddedFragmentScanner;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentMasterProductCatBarcodesBinding.inflate(
        inflater, container, false
    );
    embeddedFragmentScanner = new EmbeddedFragmentScannerBundle(
            this,
            binding.containerScanner,
            this
    );
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    if (infoFullscreenHelper != null) {
      infoFullscreenHelper.destroyInstance();
      infoFullscreenHelper = null;
    }
    if (binding != null) {
      binding.recycler.animate().cancel();
      binding = null;
    }
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    clickUtil = new ClickUtil();

    MasterProductFragmentArgs args = MasterProductFragmentArgs
        .fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new MasterProductCatBarcodesViewModel
        .MasterProductCatBarcodesViewModelFactory(activity.getApplication(), args)
    ).get(MasterProductCatBarcodesViewModel.class);

    binding.setActivity(activity);
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
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    infoFullscreenHelper = new InfoFullscreenHelper(binding.frameContainer);
    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), isLoading -> {
      if (!isLoading) {
        viewModel.setCurrentQueueLoading(null);
      }
    });

    binding.recycler.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recycler.setItemAnimator(new DefaultItemAnimator());
    binding.recycler.setAdapter(new MasterPlaceholderAdapter());

    viewModel.getProductBarcodesLive().observe(getViewLifecycleOwner(), barcodes -> {
      if (barcodes == null) {
        return;
      }
      if (barcodes.isEmpty()) {
        InfoFullscreen info = new InfoFullscreen(InfoFullscreen.INFO_EMPTY_PRODUCT_BARCODES);
        viewModel.getInfoFullscreenLive().setValue(info);
      } else {
        viewModel.getInfoFullscreenLive().setValue(null);
      }
      if (binding.recycler.getAdapter() instanceof ProductBarcodeAdapter) {
        ((ProductBarcodeAdapter) binding.recycler.getAdapter()).updateData(barcodes);
      } else {
        binding.recycler.setAdapter(new ProductBarcodeAdapter(
            barcodes,
            this,
            viewModel.getQuantityUnits(),
            viewModel.getStores()
        ));
      }
    });

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    embeddedFragmentScanner.setScannerVisibilityLive(
            viewModel.getScannerVisibilityLive()
    );

    embeddedFragmentScanner.startScannerIfVisible();

    updateUI(savedInstanceState == null);
  }

  private void updateUI(boolean animated) {
    activity.getScrollBehavior().setUpScroll(R.id.scroll);
    activity.getScrollBehavior().setHideOnScroll(true);
    activity.updateBottomAppBar(
        Constants.FAB.POSITION.END,
        R.menu.menu_master_product_edit,
        menuItem -> {
          if (menuItem.getItemId() != R.id.action_delete) {
            return false;
          }
          activity.showMessage(R.string.msg_not_implemented_yet);
          //TODO Make the button delete barcodes, instead of entire product.
          /*setForDestination(
              R.id.masterProductFragment,
              Constants.ARGUMENT.ACTION,
              Constants.ACTION.DELETE
          );
          activity.onBackPressed();*/
          return true;
        }
    );
    makeFabAdd(animated);
  }
  private void makeFabAdd(boolean animated){
    activity.updateFab(R.drawable.ic_round_add_anim,
            R.string.action_add,
            Constants.FAB.TAG.ADD,
            animated,
            () -> {
              makeFabSave(animated);
              addBarcode();
            });
  }
  private void makeFabSave(boolean animated){
    activity.updateFab(R.drawable.ic_round_done,
            R.string.action_save,
            Constants.FAB.TAG.SAVE,
            animated,
            this::tryUploadingBarcode);
  }



  @Override
  public void onItemRowClicked(ProductBarcode productBarcode) {
    if (clickUtil.isDisabled()) {
      return;
    }
  }

  public void clearInputFocus() {
    activity.hideKeyboard();
    binding.dummyFocusView.requestFocus();
    binding.editTextProductBarcode.clearFocus();
  }

  @Override
  public boolean onBackPressed() {
    // Override back press to hide barcode addition layouts
    if(viewModel.getAmountInputVisibilityLive().getValue()
            || viewModel.getScannerVisibilityLive().getValue()
            || viewModel.getBarcodeInputVisibilityLive().getValue()){
      viewModel.getScannerVisibilityLive().setValue(false);
      viewModel.getBarcodeInputVisibilityLive().setValue(false);
      viewModel.getAmountInputVisibilityLive().setValue(false);
      makeFabAdd(true);
      clearInputFocus();
      return true;
    }
    setForDestination(
        R.id.masterProductFragment,
        Constants.ARGUMENT.PRODUCT,
        viewModel.getFilledProduct()
    );
    return false;
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

  public void toggleTorch() {
    embeddedFragmentScanner.toggleTorch();
  }
  @Override
  public void onResume() {
    super.onResume();
    embeddedFragmentScanner.onResume();
  }

  @Override
  public void onPause() {
    embeddedFragmentScanner.onPause();
    super.onPause();
  }

  @Override
  public void onDestroy() {
    embeddedFragmentScanner.onDestroy();
    super.onDestroy();
  }

  public boolean isScannerVisible() {
    assert viewModel.getScannerVisibilityLive().getValue() != null;
    return viewModel.getScannerVisibilityLive().getValue();
  }

  public void toggleScannerVisibility() {
    viewModel.getScannerVisibilityLive().setValue(!isScannerVisible());
  }

  public void addBarcode() {
    viewModel.getBarcodeInputVisibilityLive().setValue(true);
    viewModel.getAmountInputVisibilityLive().setValue(true);
  }

  @Override
  public void onBarcodeRecognized(String rawValue) {
    viewModel.barcodeLive.setValue(rawValue);
    viewModel.getScannerVisibilityLive().setValue(false);
  }

  public void showInputNumberBottomSheet() {
    Bundle bundle = new Bundle();
    bundle.putInt(FormDataMasterProductCatAmount.AMOUNT_ARG, 0);
    bundle.putDouble(Constants.ARGUMENT.NUMBER, 1);
    activity.showBottomSheet(new InputBottomSheet(), bundle);
  }

  @Override
  public void saveInput(String input, Bundle argsBundle) {
    String number = NumUtil.isStringDouble(input) ? input : String.valueOf(1);
    if (Double.parseDouble(number) < 1) {
      viewModel.amountLive.setValue(String.valueOf(1));
    } else {
      viewModel.amountLive.setValue(number);
    }
  }

  private void tryUploadingBarcode(){
    // Verify valid barcode input and amount, otherwise show Error
    if (viewModel.amountLive.getValue() == null || viewModel.barcodeLive.getValue() == null){
      //TODO Extract String
      binding.textInputProductBarcode.setError("Please enter/scan the barcode");
      return;
    }else{
      binding.textInputProductBarcode.setError(null);
    }

    // Create new product barcode
    ProductBarcode productBarcode = new ProductBarcode();
    productBarcode.setProductId(viewModel.getFilledProduct().getId());
    productBarcode.setQuId(viewModel.getFilledProduct().getQuIdStock());
    productBarcode.setBarcode(viewModel.barcodeLive.getValue());
    productBarcode.setAmount(viewModel.amountLive.getValue());
    //Update list and adapter
    ArrayList<ProductBarcode> barcodes = viewModel.getProductBarcodesLive().getValue();
    if (barcodes == null)
      barcodes = new ArrayList<>();
    barcodes.add(productBarcode);
    viewModel.getProductBarcodesLive().setValue(barcodes);
    // Upload new barcode to server
    viewModel.uploadBarcode(productBarcode);

    // Reset UI
    viewModel.getScannerVisibilityLive().setValue(false);
    viewModel.getBarcodeInputVisibilityLive().setValue(false);
    viewModel.getAmountInputVisibilityLive().setValue(false);
    viewModel.barcodeLive.setValue(null);
    viewModel.amountLive.setValue(String.valueOf(1));
    makeFabAdd(true);
  }
}
