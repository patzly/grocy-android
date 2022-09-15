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

package xyz.zedler.patrick.grocy.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputLayout;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.ShoppingListItemAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentPurchaseBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheetArgs;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.PendingProduct;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner.BarcodeListener;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScannerBundle;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.PurchaseViewModel;

public class PurchaseFragment extends BaseFragment implements BarcodeListener {

  private final static String TAG = PurchaseFragment.class.getSimpleName();

  private MainActivity activity;
  private PurchaseFragmentArgs args;
  private FragmentPurchaseBinding binding;
  private PurchaseViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private EmbeddedFragmentScanner embeddedFragmentScanner;
  private PluralUtil pluralUtil;
  private Boolean backFromChooseProductPage;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentPurchaseBinding.inflate(inflater, container, false);
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
    binding = null;
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
      binding.linearBatchItem.containerRow.setBackground(
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
            activity.binding.coordinatorMain
        ));
      } else if (event.getType() == Event.TRANSACTION_SUCCESS) {
        assert getArguments() != null;
        if (viewModel.hasStoredPurchase()) {
          activity.navigateUp();
        } else if (args.getShoppingListItems() != null) {
          clearInputFocus();
          viewModel.getFormData().clearForm();
          boolean nextItemValid = viewModel.batchModeNextItem();
          if (!nextItemValid) activity.navigateUp();
        } else if (PurchaseFragmentArgs.fromBundle(getArguments()).getCloseWhenFinished()) {
          activity.navigateUp();
        } else {
          viewModel.getFormData().clearForm();
          focusProductInputIfNecessary();
          embeddedFragmentScanner.startScannerIfVisible();
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
      } else if (event.getType() == Event.CONTINUE_SCANNING) {
        embeddedFragmentScanner.startScannerIfVisible();
      } else if (event.getType() == Event.CHOOSE_PRODUCT) {
        String barcode = event.getBundle().getString(ARGUMENT.BARCODE);
        navigate(PurchaseFragmentDirections
            .actionPurchaseFragmentToChooseProductFragment(barcode)
            .setPendingProductsActive(viewModel.isQuickModeEnabled()));
      }
    });

    String barcode = (String) getFromThisDestinationNow(ARGUMENT.BARCODE);
    if (barcode != null) {
      removeForThisDestination(Constants.ARGUMENT.BARCODE);
      viewModel.addBarcodeToExistingProduct(barcode);
    }
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
    Integer pendingProductId = (Integer) getFromThisDestinationNow(ARGUMENT.PENDING_PRODUCT_ID);
    if (pendingProductId != null) {
      removeForThisDestination(ARGUMENT.PENDING_PRODUCT_ID);
      viewModel.setQueueEmptyAction(() -> viewModel.setPendingProduct(pendingProductId, null));
    }

    if (viewModel.hasStoredPurchase()) {
      binding.textInputPurchaseProduct.setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);
    } else {
      binding.textInputPurchaseProduct.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
      binding.textInputPurchaseProduct.setEndIconDrawable(R.drawable.ic_round_barcode_scan);
      binding.textInputPurchaseProduct.setEndIconContentDescription(R.string.action_scan);
      binding.textInputPurchaseProduct.setEndIconOnClickListener(v -> toggleScannerVisibility());
    }

    pluralUtil = new PluralUtil(activity);
    viewModel.getFormData().getShoppingListItemLive().observe(getViewLifecycleOwner(), item -> {
      if(args.getShoppingListItems() == null || item == null) return;
      ShoppingListItemAdapter.fillShoppingListItem(
          requireContext(),
          item,
          binding.linearBatchItem,
          viewModel.getProductHashMap(),
          viewModel.getQuantityUnitHashMap(),
          viewModel.getShoppingListItemAmountsHashMap(),
          pluralUtil
      );
    });

    backFromChooseProductPage = (Boolean)
        getFromThisDestinationNow(ARGUMENT.BACK_FROM_CHOOSE_PRODUCT_PAGE);
    if (backFromChooseProductPage != null) {
      removeForThisDestination(ARGUMENT.BACK_FROM_CHOOSE_PRODUCT_PAGE);
    }
    embeddedFragmentScanner.setScannerVisibilityLive(
        viewModel.getFormData().getScannerVisibilityLive(),
        backFromChooseProductPage != null
            && viewModel.getFormData().getProductDetailsLive().getValue() != null
            ? backFromChooseProductPage : false
    );

    // following lines are necessary because no observers are set in Views
    viewModel.getFormData().getPriceStockLive().observe(getViewLifecycleOwner(), i -> {
    });
    viewModel.getFormData().getQuantityUnitStockLive().observe(getViewLifecycleOwner(), i -> {
    });

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    focusProductInputIfNecessary();

    setHasOptionsMenu(true);

    updateUI(args.getAnimateStart() && savedInstanceState == null);
  }

  private void updateUI(boolean animated) {
    activity.getScrollBehavior().setUpScroll(R.id.scroll_purchase);
    activity.getScrollBehavior().setHideOnScroll(false);
    activity.updateBottomAppBar(
        true,
        args.getShoppingListItems() != null
            ? R.menu.menu_purchase_batch
            : R.menu.menu_purchase,
        this::onMenuItemClick
    );
    activity.updateFab(
        viewModel.hasStoredPurchase() ? R.drawable.ic_round_save
            : R.drawable.ic_round_local_grocery_store,
        R.string.action_purchase,
        Constants.FAB.TAG.PURCHASE,
        animated,
        () -> {
          if (viewModel.isQuickModeEnabled()
              && viewModel.getFormData().isCurrentProductFlowNotInterrupted()) {
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
    if (backFromChooseProductPage != null && backFromChooseProductPage
        && viewModel.getFormData().getProductDetailsLive().getValue() != null) {
      backFromChooseProductPage = false;
      return;
    }
    embeddedFragmentScanner.onResume();
  }

  @Override
  public void onPause() {
    embeddedFragmentScanner.onPause();
    super.onPause();
  }

  @Override
  public void onDestroy() {
    if (embeddedFragmentScanner != null) embeddedFragmentScanner.onDestroy();
    super.onDestroy();
  }

  @Override
  public void onBarcodeRecognized(String rawValue) {
    clearInputFocus();
    if (!viewModel.isQuickModeEnabled()) {
      viewModel.getFormData().toggleScannerVisibility();
    }
    viewModel.onBarcodeRecognized(rawValue);
  }

  public void toggleTorch() {
    embeddedFragmentScanner.toggleTorch();
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
    viewModel.getFormData().getStoreLive().setValue(
        store == null || store.getId() == -1 ? null : store
    );
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
    binding.textInputPurchaseNote.clearFocus();
  }

  public void onItemAutoCompleteClick(AdapterView<?> adapterView, int pos) {
    clearInputFocus();
    Object object = adapterView.getItemAtPosition(pos);
    if (object instanceof PendingProduct) {
      viewModel.setPendingProduct(((PendingProduct) object).getId(), null);
    } else if (object instanceof Product) {
      viewModel.setProduct(((Product) object).getId(), null, null);
    }
  }

  public void navigateToPendingProductsPage() {
    navigate(PurchaseFragmentDirections.actionPurchaseFragmentToPendingPurchasesFragment());
  }

  public void clearFocusAndCheckProductInput() {
    clearInputFocus();
    viewModel.checkProductInput();
  }

  public void clearFocusAndCheckProductInputExternal() {
    clearInputFocus();
    String input = viewModel.getFormData().getProductNameLive().getValue();
    if (input == null || input.isEmpty()) return;
    viewModel.onBarcodeRecognized(viewModel.getFormData().getProductNameLive().getValue());
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
    if (viewModel.isQuickModeEnabled()
        && viewModel.getFormData().isCurrentProductFlowNotInterrupted()) {
      focusNextInvalidView();
    } else {
      clearInputFocus();
    }
  }

  @Override
  public void startTransaction() {
    viewModel.purchaseProduct();
  }

  @Override
  public void interruptCurrentProductFlow() {
    viewModel.getFormData().setCurrentProductFlowInterrupted(true);
  }

  @Override
  public void onBottomSheetDismissed() {
    clearInputFocusOrFocusNextInvalidView();
  }

  private boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.action_product_overview) {
      ViewUtil.startIcon(item);
      if (viewModel.getFormData().getPendingProductLive().getValue() != null) {
        viewModel.showMessage(R.string.subtitle_product_not_on_server);
        return false;
      }
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
      clearInputFocus();
      viewModel.getFormData().clearForm();
      embeddedFragmentScanner.startScannerIfVisible();
      return true;
    } else if (item.getItemId() == R.id.action_skip) {
      ViewUtil.startIcon(item);
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
