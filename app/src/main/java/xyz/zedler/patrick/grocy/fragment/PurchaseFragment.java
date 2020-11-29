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

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MatchArrayAdapter;
import xyz.zedler.patrick.grocy.adapter.ShoppingListItemAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentPurchaseBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.BBDateBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputBarcodeBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StoresBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.CreateProduct;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.view.InputChip;
import xyz.zedler.patrick.grocy.viewmodel.EventHandler;
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

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        dateUtil = new DateUtil(activity);

        infoFullscreenHelper = new InfoFullscreenHelper(binding.frame);

        // INITIALIZE VIEWS

        binding.framePurchaseBack.setOnClickListener(v -> activity.navigateUp());

        binding.linearPurchaseShoppingListItem.container.setBackground(
                ContextCompat.getDrawable(activity, R.drawable.bg_list_item_visible_ripple)
        );

        // swipe refresh

        binding.swipePurchase.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipePurchase.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        binding.swipePurchase.setOnRefreshListener(() -> {
            binding.swipePurchase.setRefreshing(false);
            viewModel.refresh(args);
        });

        // product

        binding.textInputPurchaseProduct.setErrorIconDrawable(null);
        binding.textInputPurchaseProduct.setEndIconOnClickListener(v -> NavHostFragment
                .findNavController(this).navigate(
                        PurchaseFragmentDirections.actionPurchaseFragmentToScanInputFragment()
                )
        );
        binding.autoCompletePurchaseProduct.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(!hasFocus) return;
            IconUtil.start(binding.imagePurchaseProduct);
            if(viewModel.getProductNamesLive().getValue() == null
                    || viewModel.getProductNamesLive().getValue().isEmpty()
            ) viewModel.updateProducts();
        });
        binding.autoCompletePurchaseProduct.setOnItemClickListener((parent, v, position, id) -> {
            String productName = (String) parent.getItemAtPosition(position);
            Product product = viewModel.getProductFromName(productName);
            if(product != null) viewModel.loadProductDetails(product.getId());
        });
        binding.autoCompletePurchaseProduct.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        clearInputFocus();
                        String input = binding.autoCompletePurchaseProduct.getText().toString();
                        if(viewModel.getProductNames() != null
                                && !viewModel.getProductNames().isEmpty()
                                && !viewModel.getProductNames().contains(input)
                                && !input.isEmpty()
                        ) {
                            showInputNameBottomSheet(input);
                        }
                        return true;
                    } return false;
        });

        // best before date

        binding.linearPurchaseBbd.setOnClickListener(v -> {
            ProductDetails productDetails = requireProductDetails();
            if(productDetails == null) return;
            Bundle bundle = new Bundle();
            bundle.putString(
                    Constants.ARGUMENT.DEFAULT_BEST_BEFORE_DAYS,
                    String.valueOf(productDetails.getProduct().getDefaultBestBeforeDays())
            );
            bundle.putString(Constants.ARGUMENT.SELECTED_DATE, viewModel.getBestBeforeDate());
            activity.showBottomSheet(new BBDateBottomSheet(), bundle);
        });

        // amount

        binding.editTextPurchaseAmount.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                viewModel.getAmountLive().setValue(s != null ? s.toString() : "");
            }
        });
        binding.editTextPurchaseAmount.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) IconUtil.start(binding.imagePurchaseAmount);
        });
        binding.editTextPurchaseAmount.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId != EditorInfo.IME_ACTION_DONE) return false;
                    clearInputFocus();
                    return true;
                }
        );
        binding.buttonPurchaseAmountMore.setOnClickListener(v -> {
            IconUtil.start(activity, R.id.image_purchase_amount);
            viewModel.changeAmountMore();
        });
        binding.buttonPurchaseAmountLess.setOnClickListener(v -> {
            IconUtil.start(activity, R.id.image_purchase_amount);
            viewModel.changeAmountLess();
        });

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
        binding.buttonPurchasePriceMore.setOnClickListener(v -> {
            IconUtil.start(activity, R.id.image_purchase_price);
            viewModel.changePriceMore();
        });
        binding.buttonPurchasePriceLess.setOnClickListener(v -> {
            IconUtil.start(activity, R.id.image_purchase_price);
            viewModel.changePriceLess();
        });

        binding.linearPurchaseTotalPrice.setOnClickListener(
                v -> binding.checkboxPurchaseTotalPrice.setChecked(
                        !binding.checkboxPurchaseTotalPrice.isChecked()
                )
        );
        binding.checkboxPurchaseTotalPrice.setOnCheckedChangeListener(
                (v, isChecked) -> viewModel.getTotalPriceCheckedLive().setValue(isChecked)
        );

        // store

        binding.linearPurchaseStore.setOnClickListener(v -> {
            if(requireProductDetails() == null) return;
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(Constants.ARGUMENT.STORES, viewModel.getStores());
            bundle.putInt(Constants.ARGUMENT.SELECTED_ID, viewModel.getStoreId());
            activity.showBottomSheet(new StoresBottomSheet(), bundle);
        });

        // location

        binding.linearPurchaseLocation.setOnClickListener(v -> {
            if(requireProductDetails() == null) return;
            IconUtil.start(activity, R.id.image_purchase_location);
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(Constants.ARGUMENT.LOCATIONS, viewModel.getLocations());
            bundle.putInt(Constants.ARGUMENT.SELECTED_ID, viewModel.getLocationId());
            activity.showBottomSheet(new LocationsBottomSheet(), bundle);
        });

        hideDisabledFeatures();

        // show or hide shopping list item section
        if(args.getShoppingListItems() != null) {
            binding.linearPurchaseBatchModeSection.setVisibility(View.VISIBLE);
        } else {
            binding.linearPurchaseBatchModeSection.setVisibility(View.GONE);
        }

        // START

        // load or refresh data if there wasn't a configuration change
        if(savedInstanceState == null) viewModel.refresh(args);

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
                binding.progressbarPurchase.setVisibility(isDownloading ? View.VISIBLE : View.GONE)
        );
        viewModel.getBestBeforeDateLive().observe(getViewLifecycleOwner(), date -> {
            if(date == null) {
                binding.textPurchaseBbd.setText(getString(R.string.subtitle_none_selected));
            } else if(date.equals(Constants.DATE.NEVER_EXPIRES)) {
                binding.textPurchaseBbd.setText(getString(R.string.subtitle_never_expires));
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
        viewModel.getAmountLive().observe(getViewLifecycleOwner(), amount -> {
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
        });
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
        viewModel.getTotalPriceCheckedLive().observe(
                getViewLifecycleOwner(),
                isChecked -> binding.checkboxPurchaseTotalPrice.setChecked(isChecked)
        );
        viewModel.getStoreIdLive().observe(
                getViewLifecycleOwner(),
                storeId -> {
                    Store store = viewModel.getStoreFromId(storeId);
                    if(store != null) {
                        binding.textPurchaseStore.setText(store.getName());
                    } else {
                        binding.textPurchaseStore.setText(
                                getString(R.string.subtitle_none_selected)
                        );
                    }
                }
        );
        viewModel.getLocationIdLive().observe(getViewLifecycleOwner(), locationId -> {
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
        });
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


        viewModel.getProductNamesLive().observe(getViewLifecycleOwner(), productNames -> {
            MatchArrayAdapter adapterProducts = new MatchArrayAdapter(
                    activity,
                    new ArrayList<>(productNames)
            );
            binding.autoCompletePurchaseProduct.setAdapter(adapterProducts);
        });
        viewModel.getProductDetailsLive().observe(getViewLifecycleOwner(), productDetails -> {
            if(productDetails != null) {
                setForThisFragment( // for loading selected product after process death
                        Constants.ARGUMENT.PRODUCT_ID,
                        productDetails.getProduct().getId()
                );
                fillWithProductDetails(productDetails);
                viewModel.writeDefaultValues();
            } else {
                removeForThisFragment(Constants.ARGUMENT.PRODUCT_ID);
                clearFields();
            }
        });
        viewModel.getEventHandler().observe(getViewLifecycleOwner(),
                (EventHandler.EventObserver) event -> {
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
            }
        });
        if(viewModel.getProductDetails() != null
                && (binding.autoCompletePurchaseProduct.getText() == null
                || !binding.autoCompletePurchaseProduct.getText().toString()
                .equals(viewModel.getProductDetails().getProduct().getName()))
        ) {
            new Handler().postDelayed(() -> binding.autoCompletePurchaseProduct.setText(
                    viewModel.getProductDetails().getProduct().getName()
            ), 100);
        }

        // display barcodes which are already in viewModel
        for(String barcode : viewModel.getBarcodes()) {
            InputChip inputChipBarcode = new InputChip(
                    activity, barcode, R.drawable.ic_round_barcode, false
            );
            inputChipBarcode.setPadding(0, 0, 0, 8);
            binding.linearPurchaseBarcodeContainer.addView(inputChipBarcode);
        }

        getFromLastFragment(Constants.ARGUMENT.BARCODE, barcode -> {
            if(viewModel.getIsDownloading()) {
                viewModel.addQueueEmptyAction(
                        () -> viewModel.loadProductDetailsByBarcode((String) barcode)
                );
            } else {
                viewModel.loadProductDetailsByBarcode((String) barcode);
            }
        });
        getFromLastFragment(Constants.ARGUMENT.PRODUCT_ID, productId -> {
            if(viewModel.getIsDownloading()) {
                viewModel.addQueueEmptyAction(
                        () -> viewModel.loadProductDetails((Integer) productId)
                );
            } else {
                viewModel.loadProductDetails((Integer) productId);
            }
        });
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
        clearInputFocus();

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
        isFormIncomplete();
    }

    private void clearInputFocus() {
        activity.hideKeyboard();
        binding.textInputPurchaseProduct.clearFocus();
        binding.textInputPurchaseAmount.clearFocus();
        binding.textInputPurchasePrice.clearFocus();
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
        if(binding.textInputPurchaseAmount.isErrorEnabled()) isIncomplete = true;
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
            if(requireProductDetails() == null) return false;
            navigate(PurchaseFragmentDirections
                    .actionPurchaseFragmentToProductOverviewBottomSheetDialogFragment()
                    .setProductDetails(viewModel.getProductDetailsLive().getValue()));
            return true;
        });
        menuItemClear.setOnMenuItemClickListener(item -> {
            IconUtil.start(menuItemClear);
            viewModel.getProductDetailsLive().setValue(null);
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

    public void selectBestBeforeDate(String selectedBestBeforeDate) {
        viewModel.getBestBeforeDateLive().setValue(selectedBestBeforeDate);
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
        for(int i = 0; i < binding.linearPurchaseBarcodeContainer.getChildCount(); i++) {
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
        }
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
        binding.textInputPurchaseProduct.setErrorEnabled(false);
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
        }
    }

    private void showMessage(String text) {
        activity.showSnackbar(
                Snackbar.make(activity.binding.frameMainContainer, text, Snackbar.LENGTH_LONG)
        );
    }

    private int getColor(@ColorRes int color) {
        return ContextCompat.getColor(activity, color);
    }

    private String getAmount() {
        Editable amount = binding.editTextPurchaseAmount.getText();
        if(amount == null) return "";
        return amount.toString();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
