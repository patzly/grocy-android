package xyz.zedler.patrick.grocy.fragment;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.content.SharedPreferences;
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
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentConsumeNewBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheetArgs;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.scan.ScanInputCaptureManager;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.viewmodel.ConsumeViewModel;

public class ConsumeFragmentNew extends BaseFragment implements ScanInputCaptureManager.BarcodeListener {

    private final static String TAG = ConsumeFragmentNew.class.getSimpleName();

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private PurchaseFragmentArgs args;
    private FragmentConsumeNewBinding binding;
    private ConsumeViewModel viewModel;
    private InfoFullscreenHelper infoFullscreenHelper;
    private ScanInputCaptureManager capture;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentConsumeNewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(infoFullscreenHelper != null) {
            infoFullscreenHelper.destroyInstance();
            infoFullscreenHelper = null;
        }
        binding.barcodeScan.setTorchOff();
        binding = null;
    }

    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) requireActivity();

        args = PurchaseFragmentArgs.fromBundle(requireArguments());

        viewModel = new ViewModelProvider(this).get(ConsumeViewModel.class);
        binding.setActivity(activity);
        binding.setViewModel(viewModel);
        binding.setFragment(this);
        binding.setFormData(viewModel.getFormData());
        binding.setLifecycleOwner(getViewLifecycleOwner());

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        infoFullscreenHelper = new InfoFullscreenHelper(binding.container);

        viewModel.getInfoFullscreenLive().observe(
                getViewLifecycleOwner(),
                infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
        );
        viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), isDownloading ->
                binding.swipe.setRefreshing(isDownloading)
        );
        viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
            if(event.getType() == Event.SNACKBAR_MESSAGE) {
                activity.showSnackbar(((SnackbarMessage) event).getSnackbar(
                        activity,
                        activity.binding.frameMainContainer
                ));
            } else if(event.getType() == Event.PURCHASE_SUCCESS) {
                assert getArguments() != null;
                if(PurchaseFragmentArgs.fromBundle(getArguments()).getCloseWhenFinished()) {
                    activity.navigateUp();
                } else {
                    viewModel.getFormData().clearForm();
                    focusProductInputIfNecessary();
                    if(viewModel.getFormData().isScannerVisible()) {
                        capture.onResume();
                        capture.decode();
                    }
                }
            } else if(event.getType() == Event.BOTTOM_SHEET) {
                BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
                activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
            } else if(event.getType() == Event.FOCUS_INVALID_VIEWS) {
                focusNextInvalidView();
            } else if(event.getType() == Event.SCAN_MODE_ENABLED) {
                focusProductInputIfNecessary();
            } else if(event.getType() == Event.SCAN_MODE_DISABLED) {
                clearInputFocus();
            }
        });

        Integer productId = (Integer) getFromThisDestinationNow(Constants.ARGUMENT.PRODUCT_ID);
        if(productId != null) {
            removeForThisDestination(Constants.ARGUMENT.PRODUCT_ID);
            viewModel.setQueueEmptyAction(() -> viewModel.setProduct(productId, null));
        }

        viewModel.getFormData().getScannerVisibilityLive().observe(getViewLifecycleOwner(), visible -> {
            if(visible) {
                capture.onResume();
                capture.decode();
            } else {
                capture.onPause();
            }
        });
        // following lines are necessary because no observers are set in Views
        viewModel.getFormData().getQuantityUnitStockLive().observe(getViewLifecycleOwner(), i -> {});
        viewModel.getFormData().getProductDetailsLive().observe(
                getViewLifecycleOwner(),
                productDetails -> {
                    MenuItem menuItem = activity.getBottomMenu().findItem(R.id.action_open);
                    if(productDetails != null && productDetails.getProduct()
                            .getEnableTareWeightHandlingBoolean() || menuItem == null) return;
                    menuItem.setVisible(productDetails != null);
                }
        );

        //hideDisabledFeatures();

        if(savedInstanceState == null) viewModel.loadFromDatabase(true);

        binding.barcodeScan.setTorchOff();
        binding.barcodeScan.setTorchListener(new DecoratedBarcodeView.TorchListener() {
            @Override public void onTorchOn() {
                viewModel.getFormData().setTorchOn(true);
            }
            @Override public void onTorchOff() {
                viewModel.getFormData().setTorchOn(false);
            }
        });
        CameraSettings cameraSettings = new CameraSettings();
        cameraSettings.setRequestedCameraId(
                sharedPrefs.getBoolean(Constants.PREF.USE_FRONT_CAM, false) ? 1 : 0
        );
        binding.barcodeScan.getBarcodeView().setCameraSettings(cameraSettings);
        capture = new ScanInputCaptureManager(activity, binding.barcodeScan, this);

        focusProductInputIfNecessary();

        updateUI(args.getAnimateStart() && savedInstanceState == null);
    }

    private void updateUI(boolean animated) {
        activity.getScrollBehavior().setUpScroll(R.id.scroll_purchase);
        activity.getScrollBehavior().setHideOnScroll(false);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.END,
                R.menu.menu_consume,
                animated,
                this::setUpBottomMenu
        );
        activity.updateFab(
                R.drawable.ic_round_local_grocery_store,
                R.string.action_purchase,
                Constants.FAB.TAG.PURCHASE,
                animated,
                () -> {
                    if(viewModel.isScanModeEnabled()) {
                        focusNextInvalidView();
                    } else {
                        viewModel.consumeProduct(false);
                    }
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        if(viewModel.getFormData().isScannerVisible()) capture.onResume();
    }

    @Override
    public void onPause() {
        if(viewModel.getFormData().isScannerVisible()) capture.onPause();
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(viewModel.getFormData().isScannerVisible()) {
            return binding.barcodeScan.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
        } return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDestroy() {
        if(capture != null) capture.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onBarcodeResult(BarcodeResult result) {
        if(result.getText().isEmpty()) resumeScan();
        if(!viewModel.isScanModeEnabled()) viewModel.getFormData().toggleScannerVisibility();
        viewModel.onBarcodeRecognized(result.getText());
    }

    public void toggleTorch() {
        if(viewModel.getFormData().isTorchOn()) {
            binding.barcodeScan.setTorchOff();
        } else {
            binding.barcodeScan.setTorchOn();
        }
    }

    @Override
    public int getSelectedQuantityUnitId() {
        QuantityUnit selectedId = viewModel.getFormData().getQuantityUnitLive().getValue();
        if(selectedId == null) return -1;
        return selectedId.getId();
    }

    @Override
    public void selectQuantityUnit(QuantityUnit quantityUnit) {
        viewModel.getFormData().getQuantityUnitLive().setValue(quantityUnit);
    }

    @Override
    public void selectStockLocation(StockLocation stockLocation) {
        MutableLiveData<StockLocation> stockLocationLive = viewModel.getFormData()
                .getStockLocationLive();
        boolean locationHasChanged = stockLocation != stockLocationLive.getValue();
        stockLocationLive.setValue(stockLocation);
        if(!locationHasChanged) return;
        viewModel.getFormData().getUseSpecificLive().setValue(false);
        viewModel.getFormData().getSpecificStockEntryLive().setValue(null);
        viewModel.getFormData().isAmountValid();
    }

    @Override
    public void selectStockEntry(StockEntry stockEntry) {
        viewModel.getFormData().getSpecificStockEntryLive().setValue(stockEntry);
        viewModel.getFormData().isAmountValid();
    }

    @Override
    public void addBarcodeToExistingProduct(String barcode) {
        viewModel.addBarcodeToExistingProduct(barcode);
        binding.autoCompleteConsumeProduct.requestFocus();
        activity.showKeyboard(binding.autoCompleteConsumeProduct);
    }

    @Override
    public void addBarcodeToNewProduct(String barcode) {
        viewModel.addBarcodeToExistingProduct(barcode);
    }

    public void toggleScannerVisibility() {
        viewModel.getFormData().toggleScannerVisibility();
        if(viewModel.getFormData().isScannerVisible()) clearInputFocus();
    }

    public void clearInputFocus() {
        activity.hideKeyboard();
        binding.autoCompleteConsumeProduct.clearFocus();
        binding.quantityUnitContainer.clearFocus();
        binding.textInputShoppingListItemEditAmount.clearFocus();
    }

    public void onItemAutoCompleteClick(AdapterView<?> adapterView, int pos) {
        Product product = (Product) adapterView.getItemAtPosition(pos);
        clearInputFocus();
        if(product == null) return;
        viewModel.setProduct(product.getId(), null);
    }

    public void clearFocusAndCheckProductInput() {
        clearInputFocus();
        viewModel.checkProductInput();
    }

    public void focusProductInputIfNecessary() {
        if(!viewModel.isScanModeEnabled() || viewModel.getFormData().isScannerVisible()) return;
        ProductDetails productDetails = viewModel.getFormData().getProductDetailsLive().getValue();
        String productNameInput = viewModel.getFormData().getProductNameLive().getValue();
        if(productDetails == null && (productNameInput == null || productNameInput.isEmpty())) {
            binding.autoCompleteConsumeProduct.requestFocus();
            if(viewModel.getExternalScannerEnabled()) {
                activity.hideKeyboard();
            } else {
                activity.showKeyboard(binding.autoCompleteConsumeProduct);
            }
        }
    }

    public void focusNextInvalidView() {
        if(!viewModel.isScanModeEnabled()) {
            clearInputFocus();
            return;
        }
        View nextView = null;
        if(!viewModel.getFormData().isProductNameValid()) {
            nextView = binding.autoCompleteConsumeProduct;
        } else if(!viewModel.getFormData().isAmountValid()) {
            nextView = binding.editTextShoppingListItemEditAmount;
        }
        if(nextView == null) {
            clearInputFocus();
            viewModel.showConfirmationBottomSheet();
            return;
        }
        nextView.requestFocus();
        if(nextView instanceof EditText) {
            activity.showKeyboard((EditText) nextView);
        }
    }

    @Override
    public void startTransaction() {
        viewModel.consumeProduct(false);
    }

    @Override
    public void onBottomSheetDismissed() {
        focusNextInvalidView();
    }

    private void hideDisabledFeatures() {
        /*if(!viewModel.isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
            binding.linearPurchaseLocation.setVisibility(View.GONE);
        }*/
    }

    private void setUpBottomMenu() {
        MenuItem menuItemDetails, menuItemClear, menuItemOpen;
        menuItemDetails = activity.getBottomMenu().findItem(R.id.action_product_overview);
        menuItemClear = activity.getBottomMenu().findItem(R.id.action_clear_form);
        menuItemOpen = activity.getBottomMenu().findItem(R.id.action_open);
        if(menuItemDetails == null || menuItemClear == null || menuItemOpen == null) return;

        menuItemDetails.setOnMenuItemClickListener(item -> {
            IconUtil.start(menuItemDetails);
            if(!viewModel.getFormData().isProductNameValid()) return false;
            activity.showBottomSheet(
                    new ProductOverviewBottomSheet(),
                    new ProductOverviewBottomSheetArgs.Builder().setProductDetails(
                            viewModel.getFormData().getProductDetailsLive().getValue()
                    ).build().toBundle()
            );
            return true;
        });
        menuItemClear.setOnMenuItemClickListener(item -> {
            IconUtil.start(menuItemClear);
            clearInputFocus();
            viewModel.getFormData().clearForm();
            if(viewModel.getFormData().isScannerVisible()) {
                capture.onResume();
                capture.decode();
            }
            return true;
        });
        menuItemOpen.setOnMenuItemClickListener(item -> {
            if(viewModel.isScanModeEnabled()) {
                focusNextInvalidView();
            } else {
                viewModel.consumeProduct(true);
            }
            return true;
        });
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
