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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.ShoppingListItemAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentPurchaseBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputBarcodeBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheetNew;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.CreateProduct;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.viewmodel.PurchaseViewModel;

public class PurchaseFragment extends BaseFragment {

    private final static String TAG = PurchaseFragment.class.getSimpleName();

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private DateUtil dateUtil;
    private PurchaseFragmentArgs args;
    private FragmentPurchaseBinding binding;
    private PurchaseViewModel viewModel;
    private InfoFullscreenHelper infoFullscreenHelper;

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

        dateUtil = new DateUtil(activity);

        infoFullscreenHelper = new InfoFullscreenHelper(binding.container);

        // INITIALIZE VIEWS

        binding.linearPurchaseShoppingListItem.container.setBackground(
                ContextCompat.getDrawable(activity, R.drawable.bg_list_item_visible_ripple)
        );

        // price

        String currency = sharedPrefs.getString(Constants.PREF.CURRENCY, "");
        if(currency != null && !currency.isEmpty()) {
            binding.textInputPurchasePrice.setHint(getString(R.string.property_price_in, currency));
        } else {
            binding.textInputPurchasePrice.setHint(getString(R.string.property_price));
        }
        binding.editTextPurchasePrice.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                viewModel.getPriceLive().setValue(s != null ? s.toString() : "");
            }
        });
        binding.editTextPurchasePrice.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) IconUtil.start(binding.imagePurchasePrice);
        });
        binding.editTextPurchasePrice.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId != EditorInfo.IME_ACTION_DONE) return false;
                    clearInputFocus();
                    return true;
                }
        );

        // store



        hideDisabledFeatures();

        // show or hide shopping list item section
        if(args.getShoppingListItems() != null) {
            binding.linearPurchaseBatchModeSection.setVisibility(View.VISIBLE);
        } else {
            binding.linearPurchaseBatchModeSection.setVisibility(View.GONE);
        }

        // START

        if(savedInstanceState == null) viewModel.loadFromDatabase(true);

        observeStates();

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
                () -> {
                    if(isFormIncomplete()) {
                        showMessage(getString(R.string.error_missing_information));
                        return;
                    }
                    viewModel.purchaseProduct();
                }
        );
    }

    public void observeStates() {
        viewModel.getInfoFullscreenLive().observe(
                getViewLifecycleOwner(),
                infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
        );
        viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), isDownloading ->
                binding.swipePurchase.setRefreshing(isDownloading)
        );
        viewModel.getBestBeforeDateLive().observe(getViewLifecycleOwner(), date -> {
            if(date == null) {
                binding.textPurchaseBbd.setText(getString(R.string.subtitle_none_selected));
            } else if(date.equals(Constants.DATE.NEVER_EXPIRES)) {
                binding.textPurchaseBbd.setText(getString(R.string.subtitle_never_overdue));
            } else {
                binding.textPurchaseBbd.setText(dateUtil
                        .getLocalizedDate(viewModel.getBestBeforeDate(), DateUtil.FORMAT_MEDIUM)
                );
            }
            if(viewModel.getProductDetails() != null
                    && viewModel.isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)
                    && viewModel.getBestBeforeDateLive().getValue() == null
            ) {
                binding.textPurchaseBbdLabel.setTextColor(getColor(R.color.error));
            } else {
                binding.textPurchaseBbdLabel.setTextColor(
                        getColor(R.color.on_background_secondary)
                );
            }
        });
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
        viewModel.getPriceLive().observe(getViewLifecycleOwner(), price -> {
            if(binding.editTextPurchasePrice.getText() != null
                    && !binding.editTextPurchasePrice.getText().toString().equals(price)
            ) {
                binding.editTextPurchasePrice.setText(price);
            }
            if(viewModel.getProductDetails() != null && price != null && !price.isEmpty()
                    && NumUtil.toDouble(price) < 0
            ) {
                binding.textInputPurchasePrice.setError(
                        activity.getString(R.string.error_bounds_min, NumUtil.trim(0))
                );
            } else {
                binding.textInputPurchasePrice.setErrorEnabled(false);
            }
        });
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
        viewModel.getShoppingListItemPosLive().observe(getViewLifecycleOwner(), itemPos -> {
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
        });

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
                    viewModel.getProductDetailsLive().setValue(null);
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
            }
        });

        getFromThisDestination(Constants.ARGUMENT.BARCODE, barcode -> {
            if(viewModel.getIsDownloading()) {
                viewModel.addQueueEmptyAction(
                        () -> viewModel.loadProductDetailsByBarcode((String) barcode)
                );
            } else {
                viewModel.loadProductDetailsByBarcode((String) barcode);
            }
        });
        getFromThisDestination(Constants.ARGUMENT.PRODUCT_ID, productId -> {
            if(viewModel.getIsDownloading()) {
                viewModel.addQueueEmptyAction(
                        () -> viewModel.loadProductDetails((Integer) productId)
                );
            } else {
                viewModel.loadProductDetails((Integer) productId);
            }
        });
    }

    public void showQuantityUnitsBottomSheet(boolean hasFocus) {
        if(!hasFocus) return;
        HashMap<QuantityUnit, Double> unitsFactors = viewModel.getFormData()
                .getQuantityUnitsFactorsLive().getValue();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(
                Constants.ARGUMENT.QUANTITY_UNITS,
                unitsFactors != null ? new ArrayList<>(unitsFactors.keySet()) : null
        );
        activity.showBottomSheet(new QuantityUnitsBottomSheetNew(), bundle);
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
        binding.textInputShoppingListItemEditAmount.clearFocus();
        binding.quantityUnitContainer.clearFocus();
    }

    public void onItemAutoCompleteClick(AdapterView<?> adapterView, int pos) {
        Product product = (Product) adapterView.getItemAtPosition(pos);
        viewModel.setProduct(product);
        focusNextView();
    }

    public void onProductInputNextClick() {
        /*viewModel.checkProductInput();
        if(isWorkflowEnabled()) {
            focusNextView();
        } else {
            clearInputFocus();
        }*/
    }

    public void focusNextView() {
        if(!isWorkflowEnabled()) {
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

    @Override
    public void onBottomSheetDismissed() {
        focusNextView();
    }

    private void fillWithShoppingListItem(int itemPos) {
        if(args.getShoppingListItems() == null) return;
        ShoppingListItem listItem = args.getShoppingListItems()[itemPos];
        if(viewModel.getQuantityUnitsLive().getValue() == null) return;

        binding.textPurchaseBatch.setText(activity.getString(
                R.string.subtitle_entry_num_of_num,
                itemPos+1,
                args.getShoppingListItems().length
        ));
        ShoppingListItemAdapter.fillShoppingListItem(
                activity,
                listItem,
                binding.linearPurchaseShoppingListItem,
                viewModel.getQuantityUnitsLive().getValue()
        );
        if(listItem.getProductId() != null) {
            viewModel.loadProductDetails(Integer.parseInt(listItem.getProductId()));
        } else {
            viewModel.getProductDetailsLive().setValue(null);
            fillWithProductDetails(null);
        }
        viewModel.setForcedAmount(NumUtil.trim(listItem.getAmount()));
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

    private boolean isFormIncomplete() {
        boolean isIncomplete = false;
        String input = binding.autoCompletePurchaseProduct.getText().toString();
        if(viewModel.getProductNames() != null && !viewModel.getProductNames().isEmpty()
                && !viewModel.getProductNames().contains(input) && !input.isEmpty()
        ) {
            showInputNameBottomSheet(input);
            isIncomplete = true;
        }
        if(requireProductDetails() == null) isIncomplete = true;
        //if(binding.textInputPurchaseAmount.isErrorEnabled()) isIncomplete = true;
        if(binding.textInputPurchasePrice.isErrorEnabled()) isIncomplete = true;
        if(binding.textPurchaseLocationLabel.getCurrentTextColor() == getColor(R.color.error)) {
            isIncomplete = true;
        }
        if(binding.textPurchaseBbdLabel.getCurrentTextColor() == getColor(R.color.error)) {
            isIncomplete = true;
        }
        return isIncomplete;
    }

    private void showInputNameBottomSheet(@NonNull String productName) {
        navigate(PurchaseFragmentDirections
                .actionPurchaseFragmentToInputNameBottomSheetDialogFragment(productName));
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
        MenuItem menuItemBatch, menuItemDetails, menuItemClear, menuItemSkipItem;
        menuItemBatch = activity.getBottomMenu().findItem(R.id.action_batch_mode);
        menuItemDetails = activity.getBottomMenu().findItem(R.id.action_product_overview);
        menuItemClear = activity.getBottomMenu().findItem(R.id.action_clear_form);
        menuItemSkipItem = activity.getBottomMenu().findItem(R.id.action_shopping_list_item_skip);
        if(menuItemBatch == null || menuItemDetails == null || menuItemClear == null
                || menuItemSkipItem == null
        ) return;

        menuItemBatch.setOnMenuItemClickListener(item -> {
            navigate(PurchaseFragmentDirections
                    .actionPurchaseFragmentToScanBatchFragment(Constants.ACTION.PURCHASE));
            return true;
        });
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
            viewModel.getFormData().clearForm();
            return true;
        });
        if(args.getShoppingListItems() != null) {
            menuItemSkipItem.setVisible(true);
            menuItemSkipItem.setOnMenuItemClickListener(item -> {
                IconUtil.start(menuItemSkipItem);
                viewModel.nextShoppingListItemPos();
                return true;
            });
        }
    }

    public void selectStore(int selectedId) {
        viewModel.getStoreIdLive().setValue(selectedId);
    }

    public void selectLocation(int selectedId) {
        viewModel.getLocationIdLive().setValue(selectedId);
    }

    private ProductDetails requireProductDetails() {
        ProductDetails productDetails = viewModel.getProductDetailsLive().getValue();
        if(productDetails == null) {
            binding.textInputPurchaseProduct.setError(getString(R.string.error_select_product));
        }
        return productDetails;
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

    @Override
    public void clearFields() {
        /*binding.textInputPurchaseProduct.setErrorEnabled(false);
        binding.autoCompletePurchaseProduct.setText(null);
        binding.textPurchaseBbdLabel.setTextColor(getColor(R.color.on_background_secondary));
        binding.textInputPurchaseAmount.setErrorEnabled(false);
        binding.editTextPurchaseAmount.setText(null);
        binding.imagePurchaseAmount.setImageResource(R.drawable.ic_round_scatter_plot_anim);
        binding.textInputPurchasePrice.setErrorEnabled(false);
        binding.editTextPurchasePrice.setText(null);
        binding.linearPurchaseTotalPrice.setAlpha(1.0f);
        binding.linearPurchaseTotalPrice.setEnabled(true);
        binding.checkboxPurchaseTotalPrice.setEnabled(true);
        binding.checkboxPurchaseTotalPrice.setChecked(false);
        binding.textPurchaseLocationLabel.setTextColor(getColor(R.color.on_background_secondary));
        viewModel.getBestBeforeDateLive().setValue(null);
        viewModel.getStoreIdLive().setValue(-1);
        viewModel.getLocationIdLive().setValue(-1);
        clearInputFocus();
        for(int i = 0; i < binding.linearPurchaseBarcodeContainer.getChildCount(); i++) {
            ((InputChip) binding.linearPurchaseBarcodeContainer.getChildAt(i)).close();
        }*/
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
