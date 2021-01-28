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
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentPurchaseBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputBarcodeBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.CreateProduct;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.scan.ScanInputCaptureManager;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.viewmodel.PurchaseViewModel;

public class PurchaseFragment extends BaseFragment implements ScanInputCaptureManager.BarcodeListener {

    private final static String TAG = PurchaseFragment.class.getSimpleName();

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private PurchaseFragmentArgs args;
    private FragmentPurchaseBinding binding;
    private PurchaseViewModel viewModel;
    private InfoFullscreenHelper infoFullscreenHelper;
    private ScanInputCaptureManager capture;

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

        viewModel = new ViewModelProvider(this).get(PurchaseViewModel.class);
        binding.setActivity(activity);
        binding.setViewModel(viewModel);
        binding.setFragment(this);
        binding.setFormData(viewModel.getFormData());
        binding.setLifecycleOwner(getViewLifecycleOwner());

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        infoFullscreenHelper = new InfoFullscreenHelper(binding.container);

        // INITIALIZE VIEWS

        /*binding.linearPurchaseShoppingListItem.container.setBackground(
                ContextCompat.getDrawable(activity, R.drawable.bg_list_item_visible_ripple)
        );*/

        viewModel.getInfoFullscreenLive().observe(
                getViewLifecycleOwner(),
                infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
        );
        viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), isDownloading ->
                binding.swipePurchase.setRefreshing(isDownloading)
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
                }
            } else if(event.getType() == Event.BARCODE_UNKNOWN) {
                assert event.getBundle() != null;
                activity.showBottomSheet(
                        new InputBarcodeBottomSheet(),
                        event.getBundle()
                );
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

        viewModel.getFormData().getScannerVisibilityLive().observe(getViewLifecycleOwner(), visible -> {
            if(visible) {
                capture.onResume();
                capture.decode();
            } else {
                capture.onPause();
            }
        });

        //hideDisabledFeatures();

        if(savedInstanceState == null) viewModel.loadFromDatabase(true);

        binding.barcodeScan.setTorchOff();
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
        activity.showHideDemoIndicator(this, animated);
        activity.getScrollBehavior().setUpScroll(R.id.scroll_purchase);
        activity.getScrollBehavior().setHideOnScroll(false);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.END,
                R.menu.menu_purchase,
                animated,
                this::setUpBottomMenu
        );
        activity.updateFab(
                R.drawable.ic_round_local_grocery_store,
                R.string.action_purchase,
                Constants.FAB.TAG.PURCHASE,
                animated,
                () -> viewModel.purchaseProduct()
        );
    }

    public void observeStates() {
                /*viewModel.getAmountLive().observe(getViewLifecycleOwner(), amount -> {
            if(binding.editTextPurchaseAmount.getText() != null
                    && !binding.editTextPurchaseAmount.getText().toString().equals(amount)
            ) {
                binding.editTextPurchaseAmount.setText(amount);
            }
            if(viewModel.getProductDetails() != null && NumUtil.isDouble(amount)
                    && NumUtil.toDouble(amount) < viewModel.getMinAmount()
            ) {
                binding.textInputPurchaseAmount.setError(
                        activity.getString(
                                R.string.error_bounds_min,
                                NumUtil.trim(viewModel.getMinAmount())
                        )
                );
            } else if(viewModel.getProductDetails() != null && NumUtil.isDouble(amount)
                    && NumUtil.toDouble(amount) % 1 != 0 // partial amount, has to be allowed in product master
                    && viewModel.getProductDetails().getProduct().getAllowPartialUnitsInStock() == 0
                    || viewModel.getProductDetails() != null && !NumUtil.isDouble(amount)
            ) {
                binding.textInputPurchaseAmount.setError(
                        activity.getString(R.string.error_invalid_amount)
                );
            } else {
                binding.textInputPurchaseAmount.setErrorEnabled(false);
            }
        });*/
        /*viewModel.getLocationIdLive().observe(getViewLifecycleOwner(), locationId -> {
            Location location = viewModel.getLocationFromId(locationId);
            if(location != null) {
                binding.textPurchaseLocation.setText(location.getName());
            } else {
                binding.textPurchaseLocation.setText(
                        getString(R.string.subtitle_none_selected)
                );
            }
            if(viewModel.getProductDetails() != null
                    && !viewModel.isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)
                    && viewModel.getLocationId() < 0
            ) {
                binding.textPurchaseLocationLabel.setTextColor(getColor(R.color.error));
            } else {
                binding.textPurchaseLocationLabel.setTextColor(
                        getColor(R.color.on_background_secondary)
                );
            }
        });*/
        /*viewModel.getShoppingListItemPosLive().observe(getViewLifecycleOwner(), itemPos -> {
            if(args.getShoppingListItems() == null) return;
            if(itemPos == -1) {
                viewModel.getShoppingListItemPosLive().setValue(0);
                return;
            }
            if(itemPos+1 > args.getShoppingListItems().length) {
                activity.navigateUp();
                return;
            }
            if(viewModel.getIsDownloading()) {
                viewModel.addQueueEmptyAction(() -> fillWithShoppingListItem(itemPos));
            } else {
                fillWithShoppingListItem(itemPos);
            }
        });*/
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
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(viewModel.getFormData().isScannerVisible()) {
            return super.onKeyUp(keyCode, event);
        } else if(keyCode == KeyEvent.KEYCODE_TAB) {
            showMessage("TAB");
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onDestroy() {
        capture.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onBarcodeResult(BarcodeResult result) {
        if(result.getText().isEmpty()) resumeScan();
        if(!viewModel.isScanModeEnabled()) viewModel.getFormData().toggleScannerVisibility();
        viewModel.onBarcodeRecognized(result.getText());
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
    public void selectDueDate(String dueDate) {
        viewModel.getFormData().getDueDateLive().setValue(dueDate);
        viewModel.getFormData().isDueDateValid();
    }

    public void clearInputFocus() {
        activity.hideKeyboard();
        binding.autoCompletePurchaseProduct.clearFocus();
        binding.quantityUnitContainer.clearFocus();
        binding.textInputShoppingListItemEditAmount.clearFocus();
        binding.linearPurchaseBbd.clearFocus();
        binding.textInputPurchasePrice.clearFocus();
    }

    public void onItemAutoCompleteClick(AdapterView<?> adapterView, int pos) {
        Product product = (Product) adapterView.getItemAtPosition(pos);
        clearInputFocus();
        viewModel.setProduct(product, null);
    }

    public void onProductInputNextClick() {
        clearInputFocus();
        viewModel.checkProductInput();
    }

    public void focusProductInputIfNecessary() {
        if(!viewModel.isScanModeEnabled()) return;
        ProductDetails productDetails = viewModel.getFormData().getProductDetailsLive().getValue();
        String productNameInput = viewModel.getFormData().getProductNameLive().getValue();
        if(productDetails == null && (productNameInput == null || productNameInput.isEmpty())) {
            binding.autoCompletePurchaseProduct.requestFocus();
            if(viewModel.getExternalScannerEnabled()) {
                activity.hideKeyboard();
            } else {
                activity.showKeyboard(binding.autoCompletePurchaseProduct);
            }
        }
    }

    public void focusNextView() {
        if(!viewModel.isScanModeEnabled()) {
            clearInputFocus();
            return;
        }
        View nextView = FocusFinder.getInstance()
                .findNextFocus(binding.container, activity.getCurrentFocus(), View.FOCUS_DOWN);
        if(nextView == null) {
            clearInputFocus();
            return;
        }
        nextView.requestFocus();
        if(nextView instanceof EditText) {
            activity.showKeyboard((EditText) nextView);
        }
    }

    public void focusNextInvalidView() {
        if(!viewModel.isScanModeEnabled()) {
            clearInputFocus();
            return;
        }
        View nextView = null;
        if(!viewModel.getFormData().isProductNameValid()) {
            nextView = binding.autoCompletePurchaseProduct;
        } else if(!viewModel.getFormData().isAmountValid()) {
            nextView = binding.editTextShoppingListItemEditAmount;
        } else if(!viewModel.getFormData().isDueDateValid()) {
            nextView = binding.linearPurchaseBbd;
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
        viewModel.purchaseProduct();
    }

    @Override
    public void onBottomSheetDismissed() {
        focusNextInvalidView();
    }

    private void fillWithShoppingListItem(int itemPos) {
        if(args.getShoppingListItems() == null) return;
        ShoppingListItem listItem = args.getShoppingListItems()[itemPos];
        //if(viewModel.getQuantityUnitsLive().getValue() == null) return;

        binding.textPurchaseBatch.setText(activity.getString(
                R.string.subtitle_entry_num_of_num,
                itemPos+1,
                args.getShoppingListItems().length
        ));
        /*ShoppingListItemAdapter.fillShoppingListItem(
                activity,
                listItem,
                binding.linearPurchaseShoppingListItem,
                viewModel.getQuantityUnitsLive().getValue()
        );*/
        if(listItem.getProductId() != null) {
            //viewModel.loadProductDetails(Integer.parseInt(listItem.getProductId()));
        } else {
            //viewModel.getProductDetailsLive().setValue(null);
            fillWithProductDetails(null);
        }
        //viewModel.setForcedAmount(NumUtil.trim(listItem.getAmount()));
    }

    /**
     * Fills the form.
     * @param productDetails (ProductDetails): if null, the form won't be filled with these details
     */
    private void fillWithProductDetails(
            @Nullable ProductDetails productDetails
    ) {
        /*clearInputFocus();

        // AMOUNT
        if(viewModel.getForcedAmount() != null) {
            binding.editTextPurchaseAmount.setText(viewModel.getForcedAmount());
            viewModel.setForcedAmount(null);
        } else if(!viewModel.isTareWeightEnabled(productDetails)) {
            String defaultAmount = sharedPrefs.getString(
                    Constants.PREF.STOCK_DEFAULT_PURCHASE_AMOUNT, "1"
            );
            if(defaultAmount != null && !defaultAmount.isEmpty()) {
                defaultAmount = NumUtil.trim(Double.parseDouble(defaultAmount));
            }
            binding.editTextPurchaseAmount.setText(defaultAmount);
        } else { // leave amount empty if tare weight handling enabled
            binding.editTextPurchaseAmount.setText(null);
        }
        if(getAmount().isEmpty()) {
            binding.editTextPurchaseAmount.requestFocus();
            activity.showKeyboard(binding.editTextPurchaseAmount);
        }

        if(productDetails == null) {
            requireProductDetails();
            return;
        }

        binding.textInputPurchaseProduct.setErrorEnabled(false);

        binding.textInputPurchaseAmount.setHint(
                activity.getString(
                        R.string.property_amount_in,
                        productDetails.getQuantityUnitPurchase().getNamePlural()
                )
        );

        // deactivate checkbox if tare weight handling is on
        if(viewModel.isTareWeightEnabled(productDetails)) {
            binding.linearPurchaseTotalPrice.setEnabled(false);
            binding.linearPurchaseTotalPrice.setAlpha(0.5f);
            binding.checkboxPurchaseTotalPrice.setEnabled(false);
            binding.imagePurchaseAmount.setImageResource(R.drawable.ic_round_scale_anim);
        }

        // PRODUCT
        binding.autoCompletePurchaseProduct.setText(productDetails.getProduct().getName());
        binding.autoCompletePurchaseProduct.dismissDropDown(); // necessary for lower Android versions, tested on 5.1

        // mark fields with invalid or missing content as invalid
        isFormIncomplete();*/
    }

    private void hideDisabledFeatures() {
        if(!viewModel.isFeatureEnabled(Constants.PREF.FEATURE_STOCK_PRICE_TRACKING)) {
            binding.linearPurchaseTotalPrice.setVisibility(View.GONE);
            binding.linearPurchasePrice.setVisibility(View.GONE);
        }
        if(!viewModel.isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
            binding.linearPurchaseLocation.setVisibility(View.GONE);
        }
        if(!viewModel.isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
            binding.linearPurchaseBbd.setVisibility(View.GONE);
        }
    }

    private void setUpBottomMenu() {
        MenuItem menuItemDetails, menuItemClear, menuItemSkipItem;
        menuItemDetails = activity.getBottomMenu().findItem(R.id.action_product_overview);
        menuItemClear = activity.getBottomMenu().findItem(R.id.action_clear_form);
        menuItemSkipItem = activity.getBottomMenu().findItem(R.id.action_shopping_list_item_skip);
        if(menuItemDetails == null || menuItemClear == null
                || menuItemSkipItem == null
        ) return;

        menuItemDetails.setOnMenuItemClickListener(item -> {
            IconUtil.start(menuItemDetails);
            if(!viewModel.getFormData().isProductNameValid()) return false;
            navigate(PurchaseFragmentDirections
                    .actionPurchaseFragmentToProductOverviewBottomSheetDialogFragment()
                    .setProductDetails(viewModel.getFormData().getProductDetailsLive().getValue()));
            return true;
        });
        menuItemClear.setOnMenuItemClickListener(item -> {
            IconUtil.start(menuItemClear);
            clearInputFocus();
            viewModel.getFormData().clearForm();
            return true;
        });
        if(args.getShoppingListItems() != null) {
            menuItemSkipItem.setVisible(true);
            menuItemSkipItem.setOnMenuItemClickListener(item -> {
                IconUtil.start(menuItemSkipItem);
                //viewModel.nextShoppingListItemPos();
                return true;
            });
        }
    }

    @Override
    public void addBarcode(String barcode) {
        /*for(int i = 0; i < binding.linearPurchaseBarcodeContainer.getChildCount(); i++) {
            InputChip inputChip = (InputChip) binding.linearPurchaseBarcodeContainer.getChildAt(i);
            if(inputChip.getText().equals(barcode)) {
                showMessage(activity.getString(R.string.msg_barcode_duplicate));
                if(viewModel.getProductDetails() == null) {
                    binding.autoCompletePurchaseProduct.setText(null);
                    activity.showKeyboard(binding.autoCompletePurchaseProduct);
                }
                return;
            }
        }
        InputChip inputChipBarcode = new InputChip(
                activity, barcode, R.drawable.ic_round_barcode, true
        );
        inputChipBarcode.setPadding(0, 0, 0, 8);
        binding.linearPurchaseBarcodeContainer.addView(inputChipBarcode);
        viewModel.getBarcodes().add(barcode);
        if(viewModel.getProductDetails() == null) {
            binding.autoCompletePurchaseProduct.setText(null);
            activity.showKeyboard(binding.autoCompletePurchaseProduct);
        }*/
    }

    @Override
    public void createProductFromBarcode(String barcode) {
        navigate(PurchaseFragmentDirections
                .actionPurchaseFragmentToMasterProductSimpleFragment(Constants.ACTION.CREATE)
                .setCreateProductObject(new CreateProduct(null, barcode,
                        null, null, null)
                ));
    }

    private void showMessage(String text) {
        activity.showSnackbar(
                Snackbar.make(activity.binding.frameMainContainer, text, Snackbar.LENGTH_LONG)
        );
    }

    private int getColor(@ColorRes int color) {
        return ContextCompat.getColor(activity, color);
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
