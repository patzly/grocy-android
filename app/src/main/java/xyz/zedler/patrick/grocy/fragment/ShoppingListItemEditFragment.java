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

import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.text.InputType;
import android.view.FocusFinder;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentShoppingListItemEditBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheetNew;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListsBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.viewmodel.ShoppingListItemEditViewModel;

public class ShoppingListItemEditFragment extends BaseFragment {

    private final static String TAG = ShoppingListItemEditFragment.class.getSimpleName();

    private MainActivity activity;
    private FragmentShoppingListItemEditBinding binding;
    private ShoppingListItemEditViewModel viewModel;
    private InfoFullscreenHelper infoFullscreenHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup group, Bundle state) {
        binding = FragmentShoppingListItemEditBinding.inflate(inflater, group, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) requireActivity();
        ShoppingListItemEditFragmentArgs args = ShoppingListItemEditFragmentArgs
                .fromBundle(requireArguments());
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
                SnackbarMessage message = (SnackbarMessage) event;
                Snackbar snack = message.getSnackbar(activity, activity.binding.frameMainContainer);
                activity.showSnackbar(snack);
            } else if(event.getType() == Event.NAVIGATE_UP) {
                activity.navigateUp();
            } else if(event.getType() == Event.SET_SHOPPING_LIST_ID) {
                int id = event.getBundle().getInt(Constants.ARGUMENT.SELECTED_ID);
                setForDestination(R.id.shoppingListFragment, Constants.ARGUMENT.SELECTED_ID, id);
            } else if(event.getType() == Event.BOTTOM_SHEET) {
                BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
                activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
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
            InfoFullscreen infoFullscreen = offline ? new InfoFullscreen(
                    InfoFullscreen.ERROR_OFFLINE,
                    () -> updateConnectivity(true)
            ) : null;
            viewModel.getInfoFullscreenLive().setValue(infoFullscreen);
        });

        viewModel.getScanModeEnabled().observe(getViewLifecycleOwner(), isEnabled -> {
            if(isEnabled) {
                binding.editTextShoppingListItemEditNote.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                binding.editTextShoppingListItemEditNote.setImeOptions(EditorInfo.IME_ACTION_DONE);
            } else {
                binding.editTextShoppingListItemEditNote.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                binding.editTextShoppingListItemEditNote.setImeOptions(EditorInfo.IME_ACTION_NONE);
            }
            clearInputFocus();
        });

        // necessary because else getValue() doesn't give current value (?)
        viewModel.getFormData().getQuantityUnitsLive().observe(getViewLifecycleOwner(), qUs -> {});

        String barcode = (String) getFromThisDestinationNow(Constants.ARGUMENT.BARCODE);
        if(barcode != null) {
            removeForThisDestination(Constants.ARGUMENT.BARCODE);
            viewModel.onBarcodeRecognized(barcode); // TODO: Request focus in product field if barcode was written in chip
        }

        if(savedInstanceState == null) viewModel.loadFromDatabase(true);

        updateUI(args.getAnimateStart() && savedInstanceState == null);
    }

    private void updateUI(boolean animated) {
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
                () -> viewModel.saveItem()
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
        viewModel.setProduct(product);
        focusNextView();
    }

    public void onProductInputNextClick() {
        viewModel.checkProductInput();
        if(viewModel.isScanModeEnabled()) {
            focusNextView();
        } else {
            clearInputFocus();
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

    @Override
    public void onBottomSheetDismissed() {
        focusNextView();
    }

    public void showShoppingListsBottomSheet() {
        activity.showBottomSheet(new ShoppingListsBottomSheet());
    }

    public void showQuantityUnitsBottomSheet(boolean hasFocus) {
        if(!hasFocus) return;
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(
                Constants.ARGUMENT.QUANTITY_UNITS,
                viewModel.getFormData().getQuantityUnitsLive().getValue()
        );
        activity.showBottomSheet(new QuantityUnitsBottomSheetNew(), bundle);
    }

    @Override
    public int getSelectedQuantityUnitId() {
        QuantityUnit selectedId = viewModel.getFormData().getQuantityUnitLive().getValue();
        if(selectedId == null) return -1;
        return selectedId.getId();
    }

    @Nullable
    @Override
    public MutableLiveData<Integer> getSelectedShoppingListIdLive() {
        return viewModel.getFormData().getShoppingListIdLive();
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
        menuItemDetails = activity.getBottomMenu().findItem(R.id.action_product_overview);
        menuItemClear = activity.getBottomMenu().findItem(R.id.action_clear_form);
        if(menuItemDelete != null) {
            menuItemDelete.setVisible(viewModel.isActionEdit());
            menuItemDelete.setOnMenuItemClickListener(item -> {
                ((Animatable) menuItemDelete.getIcon()).start();
                viewModel.deleteItem();
                return true;
            });
        }
        if(menuItemDetails != null) {
            menuItemDetails.setOnMenuItemClickListener(item -> {
                IconUtil.start(menuItemDetails);
                viewModel.showProductDetailsBottomSheet();
                return true;
            });
        }
        if(menuItemClear != null) {
            menuItemClear.setOnMenuItemClickListener(item -> {
                clearInputFocus();
                viewModel.getFormData().clearForm();
                return true;
            });
        }
    }

    @Override
    public void updateConnectivity(boolean isOnline) {
        if(!isOnline == viewModel.isOffline()) return;
        viewModel.setOfflineLive(!isOnline);
        if(isOnline) viewModel.downloadData();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
