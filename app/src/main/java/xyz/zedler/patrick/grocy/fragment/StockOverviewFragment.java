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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.StockOverviewItemAdapter;
import xyz.zedler.patrick.grocy.adapter.StockPlaceholderAdapter;
import xyz.zedler.patrick.grocy.behavior.AppBarBehaviorNew;
import xyz.zedler.patrick.grocy.behavior.SwipeBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentStockOverviewBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListItemBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.viewmodel.StockOverviewViewModel;

public class StockOverviewFragment extends BaseFragment implements
        StockOverviewItemAdapter.StockOverviewItemAdapterListener {

    private final static String TAG = ShoppingListFragment.class.getSimpleName();

    private MainActivity activity;
    private StockOverviewViewModel viewModel;
    private AppBarBehaviorNew appBarBehavior;
    private ClickUtil clickUtil;
    private SwipeBehavior swipeBehavior;
    private FragmentStockOverviewBinding binding;
    private InfoFullscreenHelper infoFullscreenHelper;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentStockOverviewBinding.inflate(inflater, container, false);
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
            binding.recycler.setAdapter(null);
            binding = null;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) requireActivity();
        viewModel = new ViewModelProvider(this).get(StockOverviewViewModel.class);
        viewModel.setOfflineLive(!activity.isOnline());
        binding.setViewModel(viewModel);
        binding.setActivity(activity);
        binding.setFragment(this);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        infoFullscreenHelper = new InfoFullscreenHelper(binding.frame);
        clickUtil = new ClickUtil();

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
        binding.recycler.setAdapter(new StockPlaceholderAdapter());

        if(savedInstanceState == null) {
            binding.recycler.scrollToPosition(0);
            viewModel.resetSearch();
        }

        viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), state -> {
            if(!state) viewModel.setCurrentQueueLoading(null);
        });

        viewModel.getInfoFullscreenLive().observe(
                getViewLifecycleOwner(),
                infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
        );

        viewModel.getFilteredStockItemsLive().observe(getViewLifecycleOwner(), items -> {
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
            if(binding.recycler.getAdapter() instanceof StockOverviewItemAdapter) {
                ((StockOverviewItemAdapter) binding.recycler.getAdapter()).updateData(
                        items,
                        viewModel.getShoppingListItemsProductIds(),
                        viewModel.getQuantityUnitHashMap(),
                        viewModel.getProductIdsMissingItems(),
                        viewModel.getItemsDueCount(),
                        viewModel.getItemsOverdueCount(),
                        viewModel.getItemsExpiredCount(),
                        viewModel.getItemsMissingCount(),
                        viewModel.getItemsInStockCount(),
                        viewModel.getSortMode()
                );
            } else {
                binding.recycler.setAdapter(
                        new StockOverviewItemAdapter(
                                requireContext(),
                                items,
                                viewModel.getShoppingListItemsProductIds(),
                                viewModel.getQuantityUnitHashMap(),
                                viewModel.getProductIdsMissingItems(),
                                this,
                                viewModel.getHorizontalFilterBarSingle(),
                                true,
                                5,
                                viewModel.getSortMode()
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
            swipeBehavior = new SwipeBehavior(activity) {
                @Override
                public void instantiateUnderlayButton(
                        RecyclerView.ViewHolder viewHolder,
                        List<UnderlayButton> underlayButtons
                ) {
                    int position = viewHolder.getAdapterPosition()-1;
                    /*ArrayList<GroupedListItem> groupedListItems = viewModel.getFilteredStockItemsLive().getValue();
                    if(groupedListItems == null || position < 0 || position >= groupedListItems.size()) return;
                    GroupedListItem item = groupedListItems.get(position);
                    if(!(item instanceof ShoppingListItem)) return;
                    ShoppingListItem shoppingListItem = (ShoppingListItem) item;
                    underlayButtons.add(new SwipeBehavior.UnderlayButton(
                            R.drawable.ic_round_done,
                            pos -> {
                                if(position >= groupedListItems.size()) return;
                                viewModel.toggleDoneStatus(shoppingListItem);
                            }
                    ));*/
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
                Constants.FAB.POSITION.GONE,
                R.menu.menu_stock,
                animated,
                this::setUpBottomMenu
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(appBarBehavior != null) appBarBehavior.saveInstanceState(outState);
    }

    private boolean showOfflineError() {
        if(viewModel.isOffline()) {
            showMessage(getString(R.string.error_offline));
            return true;
        } return false;
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
        SubMenu menuSort = activity.getBottomMenu().findItem(R.id.action_sort).getSubMenu();
        if(menuSort == null) return;
        MenuItem sortName = menuSort.findItem(R.id.action_sort_name);
        MenuItem sortBBD = menuSort.findItem(R.id.action_sort_bbd);
        switch (viewModel.getSortMode()) {
            case Constants.STOCK.SORT.NAME:
                sortName.setChecked(true);
                break;
            case Constants.STOCK.SORT.BBD:
                sortBBD.setChecked(true);
                break;
        }
        sortName.setOnMenuItemClickListener(item -> {
            if(!item.isChecked()) {
                item.setChecked(true);
                viewModel.setSortMode(Constants.STOCK.SORT.NAME);
                viewModel.updateFilteredStockItems();
            }
            return true;
        });
        sortBBD.setOnMenuItemClickListener(item -> {
            if(!item.isChecked()) {
                item.setChecked(true);
                viewModel.setSortMode(Constants.STOCK.SORT.BBD);
                viewModel.updateFilteredStockItems();
            }
            return true;
        });
        MenuItem sortAscending = menuSort.findItem(R.id.action_sort_ascending);
        if(sortAscending != null) {
            sortAscending.setChecked(viewModel.isSortAscending());
            sortAscending.setOnMenuItemClickListener(item -> {
                item.setChecked(!item.isChecked());
                viewModel.setSortAscending(item.isChecked());
                return true;
            });
        }
    }

    @Override
    public void onItemRowClicked(StockItem stockItem) {
        if(clickUtil.isDisabled()) return;
        if(stockItem == null) return;
        if(swipeBehavior != null) swipeBehavior.recoverLatestSwipedItem();
        //showItemBottomSheet((ShoppingListItem) groupedListItem);
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
            binding.editTextSearch.setText("");
        }
        binding.textInputSearch.requestFocus();
        activity.showKeyboard(binding.editTextSearch);

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
        binding.editTextSearch.setText("");
        viewModel.setIsSearchVisible(false);
    }

    private void showMessage(String msg) {
        activity.showSnackbar(
                Snackbar.make(activity.binding.frameMainContainer, msg, Snackbar.LENGTH_SHORT)
        );
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}