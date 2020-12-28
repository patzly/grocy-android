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

import android.animation.LayoutTransition;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import xyz.zedler.patrick.grocy.behavior.AppBarBehaviorNew;
import xyz.zedler.patrick.grocy.databinding.FragmentShoppingListBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListClearBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListItemBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.TextEditBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
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
    private DownloadHelper dlHelper;
    private ShoppingListViewModel viewModel;
    private AppBarBehaviorNew appBarBehavior;
    private ClickUtil clickUtil;
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
        dlHelper.destroy();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) requireActivity();

        viewModel = new ViewModelProvider(this).get(ShoppingListViewModel.class);
        viewModel.setOfflineLive(!activity.isOnline());
        if(savedInstanceState == null) viewModel.resetSearch();

        infoFullscreenHelper = new InfoFullscreenHelper(binding.frame);
        clickUtil = new ClickUtil();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        dlHelper = new DownloadHelper(activity, TAG);

        // INITIALIZE VIEWS

        binding.textShoppingListTitle.setOnClickListener(v -> showShoppingListsBottomSheet());
        binding.buttonShoppingListLists.setOnClickListener(v -> showShoppingListsBottomSheet());

        binding.frameShoppingListBack.setOnClickListener(v -> activity.onBackPressed());
        binding.frameShoppingListSearchClose.setOnClickListener(v -> dismissSearch());
        binding.editTextShoppingListSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                viewModel.updateSearchInput(s.toString());
            }
        });
        binding.editTextShoppingListSearch.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        activity.hideKeyboard();
                        return true;
                    } return false;
                });

        // APP BAR BEHAVIOR

        appBarBehavior = new AppBarBehaviorNew(
                activity,
                binding.appBarDefault,
                binding.appBarSearch,
                savedInstanceState
        );

        // SWIPE REFRESH

        binding.swipeShoppingList.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipeShoppingList.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        binding.swipeShoppingList.setOnRefreshListener(() -> viewModel.downloadData());

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
            binding.swipeShoppingList.setRefreshing(state);
            if(!state) viewModel.setCurrentQueueLoading(null);
        });

        viewModel.getInfoFullscreenLive().observe(
                getViewLifecycleOwner(),
                infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
        );

        viewModel.getOfflineLive().observe(getViewLifecycleOwner(), offline -> {
            appBarOfflineInfo(offline);
            /*activity.updateBottomAppBar( TODO
                    Constants.FAB.POSITION.CENTER,
                    offline ? R.menu.menu_shopping_list_offline : R.menu.menu_shopping_list,
                    getArguments() == null || getArguments()
                            .getBoolean(Constants.ARGUMENT.ANIMATED, true),
                    this::setUpBottomMenu
            );*/
        });

        viewModel.getSelectedShoppingListIdLive().observe(getViewLifecycleOwner(), id -> {
            setUpBottomMenu(); // to hide delete action if necessary
            changeAppBarTitle(id);
        });

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

        binding.swipeShoppingList.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        if(savedInstanceState == null) viewModel.loadFromDatabase(true);

        // UPDATE UI
        updateUI(ShoppingListFragmentArgs.fromBundle(requireArguments()).getAnimateStart());
    }

    private void updateUI(boolean animated) {
        activity.showHideDemoIndicator(this, animated);
        activity.getScrollBehavior().setUpScroll(binding.recycler);
        activity.getScrollBehavior().setHideOnScroll(true);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.CENTER,
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

    private void showShoppingListsBottomSheet() {
        ArrayList<ShoppingList> shoppingLists = viewModel.getShoppingLists();
        if(shoppingLists == null) return;
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(Constants.ARGUMENT.SHOPPING_LISTS, shoppingLists);
        bundle.putInt(Constants.ARGUMENT.SELECTED_ID, viewModel.getSelectedShoppingListId());
        bundle.putBoolean(
                Constants.ARGUMENT.SHOW_OFFLINE,
                viewModel.isOffline() || isFeatureMultipleListsDisabled()
        );
        activity.showBottomSheet(new ShoppingListsBottomSheet(), bundle);
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
                // TODO: Navigate to ShoppingModeFragment
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
                ArrayList<ShoppingListItem> shoppingListItemsSelected
                        = viewModel.getFilteredShoppingListItems();
                if(shoppingListItemsSelected.isEmpty()) {
                    showMessage(activity.getString(R.string.error_empty_shopping_list));
                    return true;
                }
                ArrayList<ShoppingListItem> listItems = new ArrayList<>(shoppingListItemsSelected);
                SortUtil.sortShoppingListItemsByName(listItems, true);
                ShoppingListItem[] array = new ShoppingListItem[listItems.size()];
                for(int i=0; i<array.length; i++) array[i] = listItems.get(i);
                navigate(ShoppingListFragmentDirections
                        .actionShoppingListFragmentToPurchaseFragment()
                        .setShoppingListItems(array)
                        .setCloseWhenFinished(true));
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
        GroupedListItem groupedListItem = viewModel.getFilteredGroupedListItemsLive()
                .getValue().get(movedPosition);
        if(groupedListItem.getType() == GroupedListItem.TYPE_ENTRY) {
            showItemBottomSheet(groupedListItem, movedPosition);
        } else if(!viewModel.isOffline()) {  // Click on bottom notes
            showNotesEditor();
        }
    }

    @Override
    public void updateConnectivity(boolean online) {
        if(!online == viewModel.isOffline()) return;
        viewModel.setOfflineLive(!online);
        viewModel.downloadData();
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

    private void setUpSearch() {
        if(!isSearchVisible()) {
            appBarBehavior.switchToSecondary();
            binding.editTextShoppingListSearch.setText("");
        }
        binding.textInputShoppingListSearch.requestFocus();
        activity.showKeyboard(binding.editTextShoppingListSearch);

        setIsSearchVisible(true);
    }

    @Override
    public void dismissSearch() {
        appBarBehavior.switchToPrimary();
        activity.hideKeyboard();
        binding.editTextShoppingListSearch.setText("");
        setIsSearchVisible(false);
    }

    private void appBarOfflineInfo(boolean visible) {
        boolean currentState = binding.linearOfflineError.getVisibility() == View.VISIBLE;
        if(visible == currentState) return;
        binding.linearOfflineError.setVisibility(visible ? View.VISIBLE : View.GONE);
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