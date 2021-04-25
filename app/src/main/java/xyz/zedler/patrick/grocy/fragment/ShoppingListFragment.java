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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler & Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.ShoppingListItemAdapter;
import xyz.zedler.patrick.grocy.adapter.ShoppingPlaceholderAdapter;
import xyz.zedler.patrick.grocy.behavior.AppBarBehaviorNew;
import xyz.zedler.patrick.grocy.behavior.SwipeBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentShoppingListBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListClearBottomSheet;
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
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.viewmodel.ShoppingListViewModel;

public class ShoppingListFragment extends BaseFragment implements
        ShoppingListItemAdapter.ShoppingListItemAdapterListener {

    private final static String TAG = ShoppingListFragment.class.getSimpleName();

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private ShoppingListViewModel viewModel;
    private AppBarBehaviorNew appBarBehavior;
    private ClickUtil clickUtil;
    private SwipeBehavior swipeBehavior;
    private FragmentShoppingListBinding binding;
    private InfoFullscreenHelper infoFullscreenHelper;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentShoppingListBinding.inflate(inflater, container, false);
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
        viewModel = new ViewModelProvider(this).get(ShoppingListViewModel.class);
        viewModel.setOfflineLive(!activity.isOnline());
        binding.setViewModel(viewModel);
        binding.setActivity(activity);
        binding.setFragment(this);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        infoFullscreenHelper = new InfoFullscreenHelper(binding.frame);
        clickUtil = new ClickUtil();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        // APP BAR BEHAVIOR

        appBarBehavior = new AppBarBehaviorNew(
                activity,
                binding.appBarDefault,
                binding.appBarSearch,
                savedInstanceState
        );

        binding.recycler.setLayoutManager(
                new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        );
        binding.recycler.setAdapter(new ShoppingPlaceholderAdapter());

        if(savedInstanceState == null) {
            binding.recycler.scrollToPosition(0);
            viewModel.resetSearch();
        }

        Object forcedSelectedId = getFromThisDestinationNow(Constants.ARGUMENT.SELECTED_ID);
        if(forcedSelectedId != null) {
            viewModel.selectShoppingList((Integer) forcedSelectedId);
            removeForThisDestination(Constants.ARGUMENT.SELECTED_ID);
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
                } else if(!viewModel.getHorizontalFilterBarSingle().isNoFilterActive()) {
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
                        viewModel.getProductHashMap(),
                        viewModel.getQuantityUnitHashMap(),
                        viewModel.getMissingProductIds(),
                        viewModel.getItemsMissingCount(),
                        viewModel.getItemsUndoneCount()
                );
            } else {
                binding.recycler.setAdapter(
                        new ShoppingListItemAdapter(
                                requireContext(),
                                items,
                                viewModel.getProductHashMap(),
                                viewModel.getQuantityUnitHashMap(),
                                viewModel.getMissingProductIds(),
                                this,
                                viewModel.getHorizontalFilterBarSingle()
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

        if(swipeBehavior == null) {
            swipeBehavior = new SwipeBehavior(
                    activity,
                    swipeStarted -> binding.swipeShoppingList.setEnabled(!swipeStarted)
            ) {
                @Override
                public void instantiateUnderlayButton(
                        RecyclerView.ViewHolder viewHolder,
                        List<UnderlayButton> underlayButtons
                ) {
                    int position = viewHolder.getAdapterPosition()-1;
                    ArrayList<GroupedListItem> groupedListItems = viewModel
                            .getFilteredGroupedListItemsLive().getValue();
                    if(groupedListItems == null || position < 0
                            || position >= groupedListItems.size()) return;
                    GroupedListItem item = groupedListItems.get(position);
                    if(!(item instanceof ShoppingListItem)) return;
                    ShoppingListItem shoppingListItem = (ShoppingListItem) item;
                    underlayButtons.add(new SwipeBehavior.UnderlayButton(
                            R.drawable.ic_round_done,
                            pos -> {
                                if(position >= groupedListItems.size()) return;
                                viewModel.toggleDoneStatus(shoppingListItem);
                            }
                    ));
                }
            };
            swipeBehavior.attachToRecyclerView(binding.recycler);
        }

        hideDisabledFeatures();

        if(savedInstanceState == null) viewModel.loadFromDatabase(true);

        updateUI(ShoppingListFragmentArgs.fromBundle(requireArguments()).getAnimateStart()
                && savedInstanceState == null);
    }

    private void updateUI(boolean animated) {
        activity.getScrollBehavior().setUpScroll(binding.recycler);
        activity.getScrollBehavior().setHideOnScroll(true);
        activity.updateBottomAppBar(
                viewModel.isOffline() ? Constants.FAB.POSITION.GONE : Constants.FAB.POSITION.CENTER,
                viewModel.isOffline() ? R.menu.menu_shopping_list_offline : R.menu.menu_shopping_list,
                animated,
                this::setUpBottomMenu
        );
        activity.updateFab(
                R.drawable.ic_round_add_anim,
                R.string.action_add,
                Constants.FAB.TAG.ADD,
                animated,
                this::addItem
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(appBarBehavior != null) appBarBehavior.saveInstanceState(outState);
    }

    @Override
    public void selectShoppingList(ShoppingList shoppingList) {
        viewModel.selectShoppingList(shoppingList);
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

    public void toggleDoneStatus(@NonNull ShoppingListItem shoppingListItem) {
        viewModel.toggleDoneStatus(shoppingListItem);
    }

    public void editItem(@NonNull ShoppingListItem shoppingListItem) {
        if(showOfflineError()) return;
        navigate(ShoppingListFragmentDirections
                .actionShoppingListFragmentToShoppingListItemEditFragment(Constants.ACTION.EDIT)
                .setShoppingListItem(shoppingListItem));
    }

    @Override
    public void saveText(Spanned notes) {
        viewModel.saveNotes(notes);
    }

    public void purchaseItem(@NonNull ShoppingListItem shoppingListItem) {
        if(showOfflineError()) return;
        navigate(R.id.purchaseFragment, new PurchaseFragmentArgs.Builder()
                .setShoppingListItems(new ShoppingListItem[]{shoppingListItem})
                .setCloseWhenFinished(true).build().toBundle());
    }

    public void deleteItem(@NonNull ShoppingListItem shoppingListItem) {
        if(showOfflineError()) return;
        viewModel.deleteItem(shoppingListItem);
    }

    private boolean showOfflineError() {
        if(viewModel.isOffline()) {
            showMessage(getString(R.string.error_offline));
            return true;
        } return false;
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

    public void setUpBottomMenu() {
        if(activity == null) return; // Fixes crash on theme change
        MenuItem search = activity.getBottomMenu().findItem(R.id.action_search);
        if(search != null) {
            search.setOnMenuItemClickListener(item -> {
                IconUtil.start(item);
                setUpSearch();
                return true;
            });
        }

        MenuItem shoppingMode = activity.getBottomMenu().findItem(R.id.action_shopping_mode);
        if(shoppingMode != null) {
            shoppingMode.setOnMenuItemClickListener(item -> {
                navigate(ShoppingListFragmentDirections
                        .actionShoppingListFragmentToShoppingModeFragment());
                return true;
            });
        }

        MenuItem addMissing = activity.getBottomMenu().findItem(R.id.action_add_missing);
        if(addMissing != null) {
            addMissing.setOnMenuItemClickListener(item -> {
                IconUtil.start(item);
                viewModel.addMissingItems();
                return true;
            });
        }

        MenuItem purchaseItems = activity.getBottomMenu().findItem(R.id.action_purchase_all_items);
        if(purchaseItems != null) {
            purchaseItems.setOnMenuItemClickListener(item -> {
                if(true) {
                    showMessage(getString(R.string.msg_not_implemented_yet));
                    return true;
                }
                ArrayList<ShoppingListItem> shoppingListItemsSelected
                        = viewModel.getFilteredShoppingListItems();
                if(shoppingListItemsSelected == null) {
                    showMessage(activity.getString(R.string.error_undefined));
                    return true;
                }
                if(shoppingListItemsSelected.isEmpty()) {
                    showMessage(activity.getString(R.string.error_empty_shopping_list));
                    return true;
                }
                ArrayList<ShoppingListItem> listItems = new ArrayList<>(shoppingListItemsSelected);
                HashMap<Integer, String> productNamesHashMap = viewModel.getProductNamesHashMap();
                if(productNamesHashMap == null) {
                    showMessage(activity.getString(R.string.error_undefined));
                    return true;
                }
                SortUtil.sortShoppingListItemsByName(listItems, productNamesHashMap, true);
                ShoppingListItem[] array = new ShoppingListItem[listItems.size()];
                for(int i=0; i<array.length; i++) array[i] = listItems.get(i);
                navigate(R.id.purchaseFragment,
                        new PurchaseFragmentArgs.Builder()
                                .setShoppingListItems(array)
                                .setCloseWhenFinished(true).build().toBundle());
                return true;
            });
        }

        MenuItem editNotes = activity.getBottomMenu().findItem(R.id.action_edit_notes);
        if(editNotes != null) {
            editNotes.setOnMenuItemClickListener(item -> {
                showNotesEditor();
                return true;
            });
        }

        MenuItem clear = activity.getBottomMenu().findItem(R.id.action_clear);
        if(clear != null) {
            clear.setOnMenuItemClickListener(item -> {
                IconUtil.start(item);
                ShoppingList shoppingList = viewModel.getSelectedShoppingList();
                if(shoppingList == null) {
                    showMessage(activity.getString(R.string.error_undefined));
                    return true;
                }
                Bundle bundle = new Bundle();
                bundle.putParcelable(Constants.ARGUMENT.SHOPPING_LIST, shoppingList);
                activity.showBottomSheet(new ShoppingListClearBottomSheet(), bundle);
                return true;
            });
        }
    }

    @Override
    public void clearShoppingList(ShoppingList shoppingList, boolean onlyDoneItems) {
        if(onlyDoneItems) {
            viewModel.clearDoneItems(shoppingList);
        } else {
            viewModel.clearAllItems(shoppingList, null);
        }
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
    public void onItemRowClicked(GroupedListItem groupedListItem) {
        if(clickUtil.isDisabled()) return;
        if(groupedListItem == null) return;
        if(swipeBehavior != null) swipeBehavior.recoverLatestSwipedItem();
        if(groupedListItem.getType() == GroupedListItem.TYPE_ENTRY) {
            showItemBottomSheet((ShoppingListItem) groupedListItem);
        } else if(!viewModel.isOffline()) {  // Click on bottom notes
            showNotesEditor();
        }
    }

    @Override
    public void updateConnectivity(boolean isOnline) {
        if(!isOnline == viewModel.isOffline()) return;
        viewModel.setOfflineLive(!isOnline);
        if(isOnline) viewModel.downloadData();
        if(isOnline) activity.updateBottomAppBar(
                Constants.FAB.POSITION.CENTER,
                R.menu.menu_shopping_list,
                true,
                this::setUpBottomMenu
        );
    }

    private void hideDisabledFeatures() {
        if(isFeatureMultipleListsDisabled()) {
            binding.buttonShoppingListLists.setVisibility(View.GONE);
            binding.textShoppingListTitle.setOnClickListener(null);
        }
    }

    private void showItemBottomSheet(ShoppingListItem item) {
        if(item == null) return;

        Bundle bundle = new Bundle();
        if(item.hasProduct()) {
            Product product = viewModel.getProductHashMap().get(item.getProductIdInt());
            bundle.putString(Constants.ARGUMENT.PRODUCT_NAME, product.getName());
            QuantityUnit quantityUnit = viewModel.getQuantityUnitFromId(product.getQuIdPurchase());
            if(quantityUnit != null && item.getAmountDouble() == 1) {
                bundle.putString(Constants.ARGUMENT.QUANTITY_UNIT, quantityUnit.getName());
            } else if(quantityUnit != null) {
                bundle.putString(Constants.ARGUMENT.QUANTITY_UNIT, quantityUnit.getNamePlural());
            }
        }
        bundle.putParcelable(Constants.ARGUMENT.SHOPPING_LIST_ITEM, item);
        bundle.putBoolean(Constants.ARGUMENT.SHOW_OFFLINE, viewModel.isOffline());
        activity.showBottomSheet(new ShoppingListItemBottomSheet(), bundle);
    }

    private void setUpSearch() {
        if(!viewModel.isSearchVisible()) {
            appBarBehavior.switchToSecondary();
            binding.editTextShoppingListSearch.setText("");
        }
        binding.textInputShoppingListSearch.requestFocus();
        activity.showKeyboard(binding.editTextShoppingListSearch);

        viewModel.setIsSearchVisible(true);
    }

    @Override
    public boolean isSearchVisible() {
        return viewModel.isSearchVisible();
    }

    @Override
    public void dismissSearch() {
        appBarBehavior.switchToPrimary();
        activity.hideKeyboard();
        binding.editTextShoppingListSearch.setText("");
        viewModel.setIsSearchVisible(false);
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