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
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.ShoppingListItemAdapter;
import xyz.zedler.patrick.grocy.adapter.ShoppingPlaceholderAdapter;
import xyz.zedler.patrick.grocy.animator.ItemAnimator;
import xyz.zedler.patrick.grocy.databinding.FragmentShoppingModeBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListItemBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.TextEditBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.helper.ShoppingListHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.viewmodel.ShoppingModeViewModel;

public class ShoppingModeFragment extends BaseFragment implements
        ShoppingListItemAdapter.ShoppingListItemAdapterListener {

    private final static String TAG = ShoppingModeFragment.class.getSimpleName();

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private ShoppingModeViewModel viewModel;
    private ClickUtil clickUtil;
    private FragmentShoppingModeBinding binding;
    private InfoFullscreenHelper infoFullscreenHelper;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentShoppingModeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(infoFullscreenHelper != null) {
            infoFullscreenHelper.destroyInstance();
            infoFullscreenHelper = null;
        }
        if(binding != null) {
            binding.recycler.animate().cancel();
            binding.buttonShoppingListLists.animate().cancel();
            binding.textShoppingListTitle.animate().cancel();
            binding.recycler.setAdapter(null);
            binding = null;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) requireActivity();
        viewModel = new ViewModelProvider(this).get(ShoppingModeViewModel.class);
        viewModel.setOfflineLive(!activity.isOnline());
        if(savedInstanceState == null) viewModel.resetSearch();
        binding.setViewModel(viewModel);
        binding.setActivity(activity);
        binding.setFragment(this);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        infoFullscreenHelper = new InfoFullscreenHelper(binding.frame);
        clickUtil = new ClickUtil();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        if(savedInstanceState == null) binding.recycler.scrollTo(0, 0);

        binding.recycler.setLayoutManager(
                new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        );
        binding.recycler.setItemAnimator(new ItemAnimator());
        binding.recycler.setAdapter(new ShoppingPlaceholderAdapter());

        Object forcedSelectedId = getFromThisFragmentNow(Constants.ARGUMENT.SELECTED_ID);
        if(forcedSelectedId != null) {
            viewModel.selectShoppingList((Integer) forcedSelectedId);
            removeForThisFragment(Constants.ARGUMENT.SELECTED_ID);
        }

        viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), state -> {
            if(!state) viewModel.setCurrentQueueLoading(null);
        });

        viewModel.getInfoFullscreenLive().observe(
                getViewLifecycleOwner(),
                infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
        );

        viewModel.getSelectedShoppingListIdLive().observe(
                getViewLifecycleOwner(), this::changeAppBarTitle
        );

        viewModel.getFilteredGroupedListItemsLive().observe(getViewLifecycleOwner(), items -> {
            if(items == null) return;
            if(items.isEmpty()) {
                InfoFullscreen info;
                if(viewModel.isSearchActive()) {
                    info = new InfoFullscreen(InfoFullscreen.INFO_NO_SEARCH_RESULTS);
                } else if(viewModel.getFilterState() != -1) {
                    info = new InfoFullscreen(InfoFullscreen.INFO_NO_FILTER_RESULTS);
                } else {
                    info = new InfoFullscreen(InfoFullscreen.INFO_EMPTY_SHOPPING_LIST);
                }
                viewModel.getInfoFullscreenLive().setValue(info);
            } else {
                viewModel.getInfoFullscreenLive().setValue(null);
            }
            if(binding.recycler.getAdapter() instanceof ShoppingListItemAdapter) {
                ((ShoppingListItemAdapter) binding.recycler.getAdapter()).updateData(
                        items,
                        viewModel.getQuantityUnits(),
                        viewModel.getItemsMissingCount(),
                        viewModel.getItemsUndoneCount()
                );
            } else {
                binding.recycler.setAdapter(
                        new ShoppingListItemAdapter(
                                requireContext(),
                                items,
                                viewModel.getQuantityUnits(),
                                this,
                                viewModel.getFilterState(),
                                state -> viewModel.onFilterChanged(state),
                                viewModel.getItemsMissingCount(),
                                viewModel.getItemsUndoneCount()
                        )
                );
            }
        });

        viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
            if(event.getType() == Event.SNACKBAR_MESSAGE) {
                activity.showSnackbar(((SnackbarMessage) event).getSnackbar(
                        activity,
                        activity.binding.frameMainContainer
                ));
            }
        });

        hideDisabledFeatures();

        if(savedInstanceState == null) viewModel.loadFromDatabase(true);

        updateUI(ShoppingModeFragmentArgs.fromBundle(requireArguments()).getAnimateStart()
                && savedInstanceState == null);
    }

    private void updateUI(boolean animated) {
        activity.showHideDemoIndicator(this, animated);
        activity.getScrollBehavior().setUpScroll(binding.recycler);
        activity.getScrollBehavior().setHideOnScroll(true);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.GONE,
                R.menu.menu_shopping_list,
                animated,
                () -> {}
        );
    }

    @Override
    public void selectShoppingList(int shoppingListId) {
        viewModel.selectShoppingList(shoppingListId);
    }

    private void changeAppBarTitle(int selectedShoppingListId) {
        ShoppingList shoppingList = viewModel.getShoppingListFromId(selectedShoppingListId);
        if(shoppingList == null) return;
        ShoppingListHelper.changeAppBarTitle(
                binding.textShoppingListTitle,
                binding.buttonShoppingListLists,
                shoppingList
        );
    }

    public void toggleDoneStatus(int movedPosition) {
        viewModel.toggleDoneStatus(movedPosition);
    }

    public void editItem(int movedPosition) {
        ShoppingListItem shoppingListItem = viewModel.getShoppingListItemAtPos(movedPosition);
        navigate(ShoppingListFragmentDirections
                .actionShoppingListFragmentToShoppingListItemEditFragment(Constants.ACTION.EDIT)
                .setShoppingListItem(shoppingListItem));
    }

    public void saveNotes(Spanned notes) {
        viewModel.saveNotes(notes);
    }

    public void purchaseItem(int movedPosition) {
        ShoppingListItem shoppingListItem = viewModel.getShoppingListItemAtPos(movedPosition);
        if(viewModel.isOffline() || shoppingListItem == null) return;
        navigate(ShoppingListFragmentDirections.actionShoppingListFragmentToPurchaseFragment()
                .setShoppingListItems(new ShoppingListItem[]{shoppingListItem})
                .setCloseWhenFinished(true));
    }

    public void deleteItem(int movedPosition) {
        viewModel.deleteItem(movedPosition);
    }

    public void addItem() {
        navigate(ShoppingListFragmentDirections
                .actionShoppingListFragmentToShoppingListItemEditFragment(Constants.ACTION.CREATE)
                .setSelectedShoppingListId(viewModel.getSelectedShoppingListId()));
    }

    private void showNotesEditor() {
        Bundle bundle = new Bundle();
        bundle.putString(
                Constants.ARGUMENT.TITLE,
                activity.getString(R.string.action_edit_notes)
        );
        bundle.putString(
                Constants.ARGUMENT.HINT,
                activity.getString(R.string.property_notes)
        );
        ShoppingList shoppingList = viewModel.getSelectedShoppingList();
        if(shoppingList == null) return;
        bundle.putString(Constants.ARGUMENT.HTML, shoppingList.getNotes());
        activity.showBottomSheet(new TextEditBottomSheet(), bundle);
    }

    public void showShoppingListsBottomSheet() {
        activity.showBottomSheet(new ShoppingListsBottomSheet());
    }

    public void clearAllItems(ShoppingList shoppingList, Runnable onResponse) {
        viewModel.clearAllItems(shoppingList, onResponse);
    }

    public void clearDoneItems(ShoppingList shoppingList) {
        viewModel.clearDoneItems(shoppingList);
    }

    @Override
    public void deleteShoppingList(ShoppingList shoppingList) {
        viewModel.safeDeleteShoppingList(shoppingList);
    }

    @Override
    public MutableLiveData<Integer> getSelectedShoppingListIdLive() {
        return viewModel.getSelectedShoppingListIdLive();
    }

    @Override
    public void onItemRowClicked(int position) {
        if(clickUtil.isDisabled()) return;
        int movedPosition = position - 1;
        ArrayList<GroupedListItem> groupedListItems = viewModel.getFilteredGroupedListItemsLive()
                .getValue();
        if(groupedListItems == null) return;
        GroupedListItem groupedListItem = groupedListItems.get(movedPosition);
        if(groupedListItem.getType() == GroupedListItem.TYPE_ENTRY) {
            showItemBottomSheet(groupedListItem, movedPosition);
        } else if(!viewModel.isOffline()) {  // Click on bottom notes
            showNotesEditor();
        }
    }

    @Override
    public void updateConnectivity(boolean isOnline) {
        if(!isOnline == viewModel.isOffline()) return;
        viewModel.setOfflineLive(!isOnline);
        if(isOnline) viewModel.downloadData();
    }

    private void hideDisabledFeatures() {
        if(isFeatureMultipleListsDisabled()) {
            binding.buttonShoppingListLists.setVisibility(View.GONE);
            binding.textShoppingListTitle.setOnClickListener(null);
        }
    }

    private void showItemBottomSheet(GroupedListItem groupedListItem, int position) {
        if(groupedListItem != null) {
            ShoppingListItem shoppingListItem = (ShoppingListItem) groupedListItem;
            Product product = shoppingListItem.getProduct();

            Bundle bundle = new Bundle();
            if(product != null) {
                bundle.putString(
                        Constants.ARGUMENT.PRODUCT_NAME,
                        shoppingListItem.getProduct().getName()
                );
                QuantityUnit quantityUnit = viewModel.getQuantityUnitFromId(
                        shoppingListItem.getProduct().getQuIdPurchase()
                );
                if(quantityUnit != null && shoppingListItem.getAmount() == 1) {
                    bundle.putString(Constants.ARGUMENT.QUANTITY_UNIT, quantityUnit.getName());
                } else if(quantityUnit != null) {
                    bundle.putString(
                            Constants.ARGUMENT.QUANTITY_UNIT,
                            quantityUnit.getNamePlural()
                    );
                }
            }
            bundle.putParcelable(Constants.ARGUMENT.SHOPPING_LIST_ITEM, shoppingListItem);
            bundle.putInt(Constants.ARGUMENT.POSITION, position);
            bundle.putBoolean(Constants.ARGUMENT.SHOW_OFFLINE, viewModel.isOffline());
            activity.showBottomSheet(new ShoppingListItemBottomSheet(), bundle);
        }
    }

    private void showMessage(String msg) {
        activity.showSnackbar(
                Snackbar.make(activity.binding.frameMainContainer, msg, Snackbar.LENGTH_SHORT)
        );
    }

    private boolean isFeatureMultipleListsDisabled() {
        return !sharedPrefs.getBoolean(Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS, true);
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}