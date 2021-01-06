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
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.FragmentShoppingListItemEditBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputNameBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheetArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheetNew;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListsBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.view.InputChip;
import xyz.zedler.patrick.grocy.viewmodel.ShoppingListItemEditViewModel;

public class ShoppingListItemEditFragment extends BaseFragment {

    private final static String TAG = ShoppingListItemEditFragment.class.getSimpleName();

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private Gson gson;
    private GrocyApi grocyApi;
    private DownloadHelper dlHelper;
    private ShoppingListItemEditFragmentArgs args;
    private FragmentShoppingListItemEditBinding binding;
    private ShoppingListItemEditViewModel viewModel;
    private InfoFullscreenHelper infoFullscreenHelper;

    private ArrayList<Product> products;
    private ArrayList<ShoppingList> shoppingLists;
    private ArrayList<String> productNames;

    private ProductDetails productDetails;

    private double amount;
    private boolean nameAutoFilled;
    private int selectedShoppingListId;
    private boolean debug;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentShoppingListItemEditBinding.inflate(
                inflater,
                container,
                false
        );
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        dlHelper.destroy();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) requireActivity();
        args = ShoppingListItemEditFragmentArgs.fromBundle(requireArguments());
        viewModel = new ViewModelProvider(this, new ShoppingListItemEditViewModel
                .ShoppingListItemEditViewModelFactory(activity.getApplication(), args)
        ).get(ShoppingListItemEditViewModel.class);
        binding.setActivity(activity);
        binding.setViewModel(viewModel);
        binding.setFormData(viewModel.getFormData());
        binding.setFragment(this);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
            if(event.getType() == Event.SNACKBAR_MESSAGE) {
                activity.showSnackbar(((SnackbarMessage) event).getSnackbar(
                        activity,
                        activity.binding.frameMainContainer
                ));
            } else if(event.getType() == Event.NAVIGATE_UP) {
                activity.navigateUp();
            } else if(event.getType() == Event.SET_SHOPPING_LIST_ID) {
                int id = event.getBundle().getInt(Constants.ARGUMENT.SELECTED_ID);
                setForDestination(R.id.shoppingListFragment, Constants.ARGUMENT.SELECTED_ID, id);
            }
        });

        infoFullscreenHelper = new InfoFullscreenHelper(binding.container);
        viewModel.getInfoFullscreenLive().observe(
                getViewLifecycleOwner(),
                infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
        );

        viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), isLoading -> {
            if(!isLoading) viewModel.setCurrentQueueLoading(null);
        });

        viewModel.getOfflineLive().observe(getViewLifecycleOwner(), offline -> {
            InfoFullscreen infoFullscreen = offline
                    ? new InfoFullscreen(InfoFullscreen.ERROR_OFFLINE)
                    : null;
            viewModel.getInfoFullscreenLive().setValue(infoFullscreen);
        });

        getWorkflowEnabled().observe(getViewLifecycleOwner(), isEnabled -> {
            if(isEnabled) {
                binding.editTextShoppingListItemEditNote.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                binding.editTextShoppingListItemEditNote.setImeOptions(EditorInfo.IME_ACTION_DONE);
            } else {
                binding.editTextShoppingListItemEditNote.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                binding.editTextShoppingListItemEditNote.setImeOptions(EditorInfo.IME_ACTION_NONE);
            }
            clearInputFocus();
        });

        // PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        // WEB

        grocyApi = activity.getGrocy();
        gson = new Gson();
        dlHelper = new DownloadHelper(activity, TAG);

        // VARIABLES

        products = new ArrayList<>();
        shoppingLists = new ArrayList<>();
        productNames = new ArrayList<>();

        productDetails = null;

        amount = 0;
        nameAutoFilled = false;
        selectedShoppingListId = -1;

        // VIEWS

        // product
        binding.autoCompleteShoppingListItemEditProduct.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        /*clearInputFocus();
                        String input = binding.autoCompleteShoppingListItemEditProduct
                                .getText()
                                .toString()
                                .trim();
                        if(!productNames.contains(input)
                                && !input.isEmpty()
                                && !nameAutoFilled
                        ) {
                            Bundle bundle = new Bundle();
                            bundle.putString(
                                    Constants.ARGUMENT.TYPE,
                                    Constants.ACTION.CREATE_THEN_SHOPPING_LIST_ITEM_EDIT
                            );
                            bundle.putString(Constants.ARGUMENT.PRODUCT_NAME, input);
                            activity.showBottomSheet(
                                    new InputNameBottomSheet(), bundle
                            );
                        }
                        return true;*/
                    } return false;
                });
        nameAutoFilled = false;

        viewModel.getFormData().getQuantityUnitsLive().observe(getViewLifecycleOwner(), qUs -> {});


        if(savedInstanceState == null) viewModel.loadFromDatabase(true);

        updateUI(args.getAnimateStart() && savedInstanceState == null);

        getFromThisDestination(Constants.ARGUMENT.PRODUCT_NAME, productName -> {
            // TODO: Prioritize productName over args.getProductName(), no idea yet
        });
    }

    private void updateUI(boolean animated) {
        activity.showHideDemoIndicator(this, animated);
        activity.getScrollBehavior().setUpScroll(R.id.scroll_shopping_list_item_edit);
        activity.getScrollBehavior().setHideOnScroll(true);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.END,
                R.menu.menu_shopping_list_item_edit,
                animated,
                this::setUpBottomMenu
        );
        activity.updateFab(
                R.drawable.ic_round_backup,
                R.string.action_save,
                Constants.FAB.TAG.SAVE,
                animated,
                () -> {}
        );
    }

    private void onQueueEmpty() {
        binding.swipe.setRefreshing(false);
        if(true) return;

        /*switch (args.getAction()) {
            case Constants.ACTION.EDIT: {
                ShoppingListItem shoppingListItem = args.getShoppingListItem();
                if (shoppingListItem == null) return;
                String productName = args.getProductName();
                if (productName != null) {
                    // is given after new product was created from this fragment
                    // with method (setProductName)
                    binding.autoCompleteShoppingListItemEditProduct.setText(productName);
                } else if (shoppingListItem.getProduct() != null) {
                    binding.autoCompleteShoppingListItemEditProduct.setText(
                            shoppingListItem.getProduct().getName()
                    );
                }
                binding.editTextShoppingListItemEditAmount.setText(
                        NumUtil.trim(shoppingListItem.getAmount())
                );
                selectShoppingList(shoppingListItem.getShoppingListId());
                binding.editTextShoppingListItemEditNote.setText(
                        TextUtil.trimCharSequence(shoppingListItem.getNote())
                );
                break;
            }
            case Constants.ACTION.CREATE: {
                String productName = args.getProductName();
                if (productName != null) {
                    // is given after new product was created from this fragment
                    // with method (setProductName)
                    binding.autoCompleteShoppingListItemEditProduct.setText(productName);
                }
                if (shoppingLists.size() >= 1 && args.getSelectedShoppingListId() != -1) {
                    selectShoppingList(args.getSelectedShoppingListId());
                } else if(shoppingLists.size() >= 1) {
                    selectShoppingList(shoppingLists.get(0).getId());
                } else {
                    selectShoppingList(-1);
                }
                break;
            }
        }*/
    }

    private void fillWithProductDetails() {
        nameAutoFilled = true;

        clearInputFocus();

        // PRODUCT
        binding.autoCompleteShoppingListItemEditProduct.setText(
                productDetails.getProduct().getName()
        );
        binding.textInputShoppingListItemEditProduct.setErrorEnabled(false);

        // AMOUNT
        binding.textInputShoppingListItemEditAmount.setHint(
                activity.getString(
                        R.string.property_amount_in,
                        productDetails.getQuantityUnitPurchase().getNamePlural()
                )
        );
    }

    public void clearInputFocus() {
        activity.hideKeyboard();
        binding.textInputShoppingListItemEditProduct.clearFocus();
        binding.textInputShoppingListItemEditAmount.clearFocus();
        binding.textInputShoppingListItemEditNote.clearFocus();
        binding.shoppingListContainer.clearFocus();
        binding.quantityUnitContainer.clearFocus();
    }

    public void onItemAutoCompleteClick(AdapterView<?> adapterView, int pos) {
        Product product = (Product) adapterView.getItemAtPosition(pos);

        viewModel.getFormData().getProductLive().setValue(product);
        viewModel.getFormData().isFormValid();
        viewModel.setProductQuantityUnitsAndFactors(product);
        focusNextView();
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

    private boolean isFormIncomplete() {
        String input = binding.autoCompleteShoppingListItemEditProduct.getText().toString().trim();
        if(binding.barcodeContainer.getChildCount() > 0 && input.isEmpty()
        ) {
            binding.textInputShoppingListItemEditProduct.setError(
                    activity.getString(R.string.error_empty)
            );
            return true;
        } else {
            binding.textInputShoppingListItemEditProduct.setErrorEnabled(false);
        }
        if(!productNames.contains(input)
                && !input.isEmpty()
                && !nameAutoFilled
        ) {
            Bundle bundle = new Bundle();
            bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.CREATE_THEN_PURCHASE);
            bundle.putString(Constants.ARGUMENT.PRODUCT_NAME, input);
            activity.showBottomSheet(new InputNameBottomSheet(), bundle);
            return true;
        } else return !isAmountValid();
    }

    private Product getProductFromName(String name) {
        if(name != null && !name.isEmpty()) {
            for(Product product : products) {
                if(product.getName().equals(name)) {
                    return product;
                }
            }
        }
        return null;
    }

    @Override
    public void onBottomSheetDismissed() {
        focusNextView();
    }

    public void showShoppingListsBottomSheet() {
        activity.showBottomSheet(new ShoppingListsBottomSheet());
    }

    public void showQuantityUnitsBottomSheet() {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(Constants.ARGUMENT.QUANTITY_UNITS, viewModel.getFormData().getQuantityUnitsLive().getValue());
        activity.showBottomSheet(new QuantityUnitsBottomSheetNew(), bundle);
    }

    public void showQuantityUnitsBottomSheet(boolean hasFocus) {
        if(!hasFocus) return;
        showQuantityUnitsBottomSheet();
    }

    @Nullable
    @Override
    public MutableLiveData<Integer> getSelectedShoppingListIdLive() {
        return viewModel.getFormData().getShoppingListIdLive();
    }

    @Override
    public int getSelectedQuantityUnitId() {
        QuantityUnit selectedId = viewModel.getFormData().getQuantityUnitLive().getValue();
        if(selectedId == null) return -1;
        return selectedId.getId();
    }

    @Override
    public void selectShoppingList(ShoppingList shoppingList) {
        viewModel.getFormData().getShoppingListLive().setValue(shoppingList);
    }

    @Override
    public void selectQuantityUnit(QuantityUnit quantityUnit) {
        viewModel.getFormData().getQuantityUnitLive().setValue(quantityUnit);
    }

    public void setUpBottomMenu() {
        MenuItem menuItemDelete, menuItemDetails, menuItemClear;
        menuItemDelete = activity.getBottomMenu().findItem(R.id.action_delete);
        if(menuItemDelete != null) {
            menuItemDelete.setVisible(viewModel.isActionEdit());
            if(menuItemDelete.isVisible()) {
                menuItemDelete.setOnMenuItemClickListener(item -> {
                    ((Animatable) menuItemDelete.getIcon()).start();
                    ShoppingListItem shoppingListItem = args.getShoppingListItem();
                    assert shoppingListItem != null;
                    dlHelper.delete(
                            grocyApi.getObject(
                                    GrocyApi.ENTITY.SHOPPING_LIST,
                                    shoppingListItem.getId()
                            ),
                            response -> activity.dismissFragment(),
                            error -> {
                                showErrorMessage();
                                if(debug) Log.i(
                                        TAG,
                                        "setUpBottomMenu: deleteItem: " + error
                                );
                            }
                    );
                    return true;
                });
            }
        }

        menuItemDetails = activity.getBottomMenu().findItem(R.id.action_product_overview);
        if(menuItemDetails != null) {
            menuItemDetails.setOnMenuItemClickListener(item -> {
                IconUtil.start(menuItemDetails);
                String input = binding.autoCompleteShoppingListItemEditProduct.getText()
                        .toString()
                        .trim();
                if(productDetails != null && input.equals(productDetails.getProduct().getName())) {
                    navigate(R.id.productOverviewBottomSheetDialogFragment,
                            new ProductOverviewBottomSheetArgs.Builder()
                                    .setProductDetails(productDetails).build().toBundle());
                } else {
                    Product product = getProductFromName(input);
                    if(product != null) {
                        dlHelper.get(
                                grocyApi.getStockProductDetails(product.getId()),
                                response -> {
                                    productDetails = gson.fromJson(
                                            response,
                                            new TypeToken<ProductDetails>() {
                                            }.getType()
                                    );
                                    navigate(R.id.productOverviewBottomSheetDialogFragment,
                                            new ProductOverviewBottomSheetArgs.Builder()
                                                    .setProductDetails(productDetails)
                                                    .build().toBundle());
                                }, error -> {
                                }
                        );
                    } else if(!productNames.isEmpty()) {
                        showMessage(activity.getString(R.string.error_invalid_product));
                    } else {
                        showErrorMessage();
                    }
                }
                return true;
            });
        }

        menuItemClear = activity.getBottomMenu().findItem(R.id.action_clear_form);
        if(menuItemClear != null) {
            menuItemClear.setOnMenuItemClickListener(item -> {
                viewModel.getFormData().clearForm();
                return true;
            });
        }
    }

    private boolean isAmountValid() {
        if(amount >= 1) {
            binding.textInputShoppingListItemEditAmount.setErrorEnabled(false);
            return true;
        } else {
            binding.textInputShoppingListItemEditAmount.setError(
                    activity.getString(
                            R.string.error_bounds_min,
                            NumUtil.trim(1)
                    )
            );
            return false;
        }
    }

    public void addInputAsBarcode() {
        String input = binding.autoCompleteShoppingListItemEditProduct.getText().toString().trim();
        if(input.isEmpty()) return;
        for(
                int i = 0;
                i < binding.barcodeContainer.getChildCount();
                i++
        ) {
            InputChip inputChip = (InputChip) binding.barcodeContainer
                    .getChildAt(i);
            if(inputChip.getText().equals(input)) {
                showMessage(activity.getString(R.string.msg_barcode_duplicate));
                binding.autoCompleteShoppingListItemEditProduct.setText(null);
                binding.autoCompleteShoppingListItemEditProduct.requestFocus();
                return;
            }
        }
        InputChip inputChipBarcode = new InputChip(
                activity, input, R.drawable.ic_round_barcode, true
        );
        inputChipBarcode.setPadding(0, 0, 0, 8);
        binding.barcodeContainer.addView(inputChipBarcode);
        binding.autoCompleteShoppingListItemEditProduct.setText(null);
        binding.autoCompleteShoppingListItemEditProduct.requestFocus();
    }

    public void clearAll() {
        binding.textInputShoppingListItemEditProduct.setErrorEnabled(false);
        binding.autoCompleteShoppingListItemEditProduct.setText(null);
        binding.textInputShoppingListItemEditAmount.setErrorEnabled(false);
        binding.editTextShoppingListItemEditAmount.setText(NumUtil.trim(1));
        binding.imageAmount.setImageResource(
                R.drawable.ic_round_scatter_plot_anim
        );
        clearInputFocus();
        for(
                int i = 0;
                i < binding.barcodeContainer.getChildCount();
                i++
        ) {
            ((InputChip) binding.barcodeContainer.getChildAt(i)).close();
        }
        productDetails = null;
        nameAutoFilled = false;
    }

    private void showMessage(String msg) {
        activity.showSnackbar(
                Snackbar.make(activity.binding.frameMainContainer, msg, Snackbar.LENGTH_SHORT)
        );
    }

    private void showErrorMessage() {
        showMessage(activity.getString(R.string.error_undefined));
    }

    @Override
    public void updateConnectivity(boolean isOnline) {
        if(viewModel.isOffline() == !isOnline) return;
        if(isOnline && viewModel.isOffline()) viewModel.downloadData();
        viewModel.setOfflineLive(!isOnline);
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
