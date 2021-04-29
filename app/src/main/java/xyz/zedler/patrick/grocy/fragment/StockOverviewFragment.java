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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.StockOverviewItemAdapter;
import xyz.zedler.patrick.grocy.adapter.StockPlaceholderAdapter;
import xyz.zedler.patrick.grocy.behavior.AppBarBehaviorNew;
import xyz.zedler.patrick.grocy.behavior.SwipeBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentStockOverviewBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.HorizontalFilterBarMulti;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.scan.ScanInputCaptureManager;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.viewmodel.StockOverviewViewModel;

public class StockOverviewFragment extends BaseFragment implements
        StockOverviewItemAdapter.StockOverviewItemAdapterListener,
        ScanInputCaptureManager.BarcodeListener {

    private final static String TAG = ShoppingListFragment.class.getSimpleName();

    private MainActivity activity;
    private StockOverviewViewModel viewModel;
    private AppBarBehaviorNew appBarBehavior;
    private ClickUtil clickUtil;
    private SwipeBehavior swipeBehavior;
    private FragmentStockOverviewBinding binding;
    private InfoFullscreenHelper infoFullscreenHelper;
    private ScanInputCaptureManager capture;

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
        lockOrUnlockRotation(false);
        if(binding != null) {
            binding.recycler.animate().cancel();
            binding.recycler.setAdapter(null);
            binding.barcodeScan.setTorchOff();
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
                } else if(viewModel.getHorizontalFilterBarMulti().areFiltersActive()) {
                    info = new InfoFullscreen(InfoFullscreen.INFO_NO_FILTER_RESULTS);
                } else {
                    info = new InfoFullscreen(InfoFullscreen.INFO_EMPTY_STOCK);
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
                        viewModel.getProductIdsMissingStockItems(),
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
                                viewModel.getProductIdsMissingStockItems(),
                                this,
                                viewModel.getHorizontalFilterBarSingle(),
                                viewModel.getHorizontalFilterBarMulti(),
                                viewModel.getItemsDueCount(),
                                viewModel.getItemsOverdueCount(),
                                viewModel.getItemsExpiredCount(),
                                viewModel.getItemsMissingCount(),
                                viewModel.getItemsInStockCount(),
                                true,
                                5,
                                viewModel.getSortMode()
                        )
                );
            }
        });

        viewModel.getScannerVisibilityLive().observe(getViewLifecycleOwner(), visible -> {
            if(visible) {
                capture.onResume();
                capture.decode();
            } else {
                capture.onPause();
            }
            lockOrUnlockRotation(visible);
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
                    swipeStarted -> binding.swipe.setEnabled(!swipeStarted)
            ) {
                @Override
                public void instantiateUnderlayButton(
                        RecyclerView.ViewHolder viewHolder,
                        List<UnderlayButton> underlayButtons
                ) {
                    int position = viewHolder.getAdapterPosition()-2;
                    ArrayList<StockItem> displayedItems = viewModel.getFilteredStockItemsLive()
                            .getValue();
                    if(displayedItems == null || position < 0
                            || position >= displayedItems.size()) return;
                    StockItem stockItem = displayedItems.get(position);
                    if(stockItem.getAmountAggregatedDouble() > 0
                            && stockItem.getProduct().getEnableTareWeightHandling() == 0
                    ) {
                        underlayButtons.add(new SwipeBehavior.UnderlayButton(
                                R.drawable.ic_round_consume_product,
                                pos -> {
                                    if(pos-2 >= displayedItems.size()) return;
                                    swipeBehavior.recoverLatestSwipedItem();
                                    viewModel.performAction(
                                            Constants.ACTION.CONSUME,
                                            displayedItems.get(pos-2)
                                    );
                                }
                        ));
                    }
                    if(stockItem.getAmountAggregatedDouble()
                            > stockItem.getAmountOpenedAggregatedDouble()
                            && stockItem.getProduct().getEnableTareWeightHandling() == 0
                            && viewModel.isFeatureEnabled(Constants.PREF.FEATURE_STOCK_OPENED_TRACKING)
                    ) {
                        underlayButtons.add(new SwipeBehavior.UnderlayButton(
                                R.drawable.ic_round_open,
                                pos -> {
                                    if(pos-2 >= displayedItems.size()) return;
                                    swipeBehavior.recoverLatestSwipedItem();
                                    viewModel.performAction(
                                            Constants.ACTION.OPEN,
                                            displayedItems.get(pos-2)
                                    );
                                }
                        ));
                    }
                    if(underlayButtons.isEmpty()) {
                        underlayButtons.add(new SwipeBehavior.UnderlayButton(
                                R.drawable.ic_round_close,
                                pos -> swipeBehavior.recoverLatestSwipedItem()
                        ));
                    }
                }
            };
            swipeBehavior.attachToRecyclerView(binding.recycler);
        }

        hideDisabledFeatures();

        if(savedInstanceState == null) viewModel.loadFromDatabase(true);

        binding.barcodeScan.setTorchOff();
        binding.barcodeScan.setTorchListener(new DecoratedBarcodeView.TorchListener() {
            @Override public void onTorchOn() {
                viewModel.setTorchOn(true);
            }
            @Override public void onTorchOff() {
                viewModel.setTorchOn(false);
            }
        });
        CameraSettings cameraSettings = new CameraSettings();
        cameraSettings.setRequestedCameraId(viewModel.getUseFrontCam() ? 1 : 0);
        binding.barcodeScan.getBarcodeView().setCameraSettings(cameraSettings);
        capture = new ScanInputCaptureManager(activity, binding.barcodeScan, this);

        updateUI(ShoppingListFragmentArgs.fromBundle(requireArguments()).getAnimateStart()
                && savedInstanceState == null);
    }

    private void updateUI(boolean animated) {
        activity.getScrollBehavior().setUpScroll(binding.recycler);
        activity.getScrollBehavior().setHideOnScroll(true);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.GONE,
                R.menu.menu_stock,
                this::onMenuItemClick
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(appBarBehavior != null) appBarBehavior.saveInstanceState(outState);
    }

    public void toggleScannerVisibility() {
        viewModel.toggleScannerVisibility();
        if(viewModel.isScannerVisible()) {
            binding.editTextSearch.clearFocus();
            activity.hideKeyboard();
        } else {
            activity.showKeyboard(binding.editTextSearch);
        }
    }

    public void toggleTorch() {
        if(viewModel.isTorchOn()) {
            binding.barcodeScan.setTorchOff();
        } else {
            binding.barcodeScan.setTorchOn();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(viewModel.isScannerVisible()) capture.onResume();
    }

    @Override
    public void onPause() {
        if(viewModel.isScannerVisible()) capture.onPause();
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(viewModel.isScannerVisible()) {
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
        viewModel.toggleScannerVisibility();
        binding.editTextSearch.setText(result.getText());
    }

    @Override
    public void performAction(String action, StockItem stockItem) {
        viewModel.performAction(action, stockItem);
    }

    private boolean showOfflineError() {
        if(viewModel.isOffline()) {
            showMessage(getString(R.string.error_offline));
            return true;
        } return false;
    }

    private boolean onMenuItemClick(MenuItem item) {
        if(item.getItemId() == R.id.action_search) {
            IconUtil.start(item);
            setUpSearch();
            return true;
        } else if(item.getItemId() == R.id.action_sort) {
            SubMenu menuSort = item.getSubMenu();
            MenuItem sortName = menuSort.findItem(R.id.action_sort_name);
            MenuItem sortBBD = menuSort.findItem(R.id.action_sort_bbd);
            MenuItem sortAscending = menuSort.findItem(R.id.action_sort_ascending);
            switch (viewModel.getSortMode()) {
                case Constants.STOCK.SORT.NAME:
                    sortName.setChecked(true);
                    break;
                case Constants.STOCK.SORT.BBD:
                    sortBBD.setChecked(true);
                    break;
            }
            sortAscending.setChecked(viewModel.isSortAscending());
            return true;
        } else if(item.getItemId() == R.id.action_sort_name) {
            if(!item.isChecked()) {
                item.setChecked(true);
                viewModel.setSortMode(Constants.STOCK.SORT.NAME);
                viewModel.updateFilteredStockItems();
            }
            return true;
        } else if(item.getItemId() == R.id.action_sort_bbd) {
            if(!item.isChecked()) {
                item.setChecked(true);
                viewModel.setSortMode(Constants.STOCK.SORT.BBD);
                viewModel.updateFilteredStockItems();
            }
            return true;
        } else if(item.getItemId() == R.id.action_sort_ascending) {
            item.setChecked(!item.isChecked());
            viewModel.setSortAscending(item.isChecked());
            return true;
        } else if(item.getItemId() == R.id.action_filter_product_group) {
            SubMenu menuProductGroups = item.getSubMenu();
            menuProductGroups.clear();
            ArrayList<ProductGroup> productGroups = viewModel.getProductGroupsLive().getValue();
            if(productGroups == null) return true;
            SortUtil.sortProductGroupsByName(requireContext(), productGroups, true);
            for(ProductGroup pg : productGroups) {
                menuProductGroups.add(pg.getName()).setOnMenuItemClickListener(pgItem -> {
                    if(binding.recycler.getAdapter() == null) return false;
                    viewModel.getHorizontalFilterBarMulti().addFilter(
                            HorizontalFilterBarMulti.PRODUCT_GROUP,
                            new HorizontalFilterBarMulti.Filter(pg.getName(), pg.getId())
                    );
                    binding.recycler.getAdapter().notifyItemChanged(1);
                    return true;
                });
            }
        } else if(item.getItemId() == R.id.action_filter_location) {
            SubMenu menuLocations = item.getSubMenu();
            menuLocations.clear();
            ArrayList<Location> locations = viewModel.getLocationsLive().getValue();
            if(locations == null) return true;
            SortUtil.sortLocationsByName(requireContext(), locations, true);
            for(Location loc : locations) {
                menuLocations.add(loc.getName()).setOnMenuItemClickListener(locItem -> {
                    if(binding.recycler.getAdapter() == null) return false;
                    viewModel.getHorizontalFilterBarMulti().addFilter(
                            HorizontalFilterBarMulti.LOCATION,
                            new HorizontalFilterBarMulti.Filter(loc.getName(), loc.getId())
                    );
                    binding.recycler.getAdapter().notifyItemChanged(1);
                    return true;
                });
            }
        }
        return false;
    }

    @Override
    public void onItemRowClicked(StockItem stockItem) {
        if(clickUtil.isDisabled()) return;
        if(stockItem == null) return;
        if(swipeBehavior != null) swipeBehavior.recoverLatestSwipedItem();
        showProductOverview(stockItem);
    }

    private void showProductOverview(StockItem stockItem) {
        if(stockItem == null) return;
        QuantityUnit quantityUnit = viewModel.getQuantityUnitFromId(stockItem.getProduct().getQuIdStock());
        Location location = viewModel.getLocationFromId(stockItem.getProduct().getLocationIdInt());
        if(quantityUnit == null || location == null) return;
        navigate(StockOverviewFragmentDirections
                .actionStockOverviewFragmentToProductOverviewBottomSheetDialogFragment()
                .setShowActions(true)
                .setStockItem(stockItem)
                .setQuantityUnit(quantityUnit)
                .setLocation(location));
    }

    @Override
    public void updateConnectivity(boolean isOnline) {
        if(!isOnline == viewModel.isOffline()) return;
        viewModel.setOfflineLive(!isOnline);
        if(isOnline) viewModel.downloadData();
    }

    private void hideDisabledFeatures() {
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
        if(viewModel.isScannerVisible()) viewModel.toggleScannerVisibility();
    }

    private void lockOrUnlockRotation(boolean scannerIsVisible) {
        if(scannerIsVisible) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        }
    }

    private void showMessage(String msg) {
        activity.showSnackbar(
                Snackbar.make(activity.binding.frameMainContainer, msg, Snackbar.LENGTH_SHORT)
        );
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        return setStatusBarColor(transit, enter, nextAnim, activity, R.color.primary);
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}