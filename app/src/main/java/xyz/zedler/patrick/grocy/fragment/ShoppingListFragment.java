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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.ShoppingListItemAdapter;
import xyz.zedler.patrick.grocy.adapter.StockPlaceholderAdapter;
import xyz.zedler.patrick.grocy.animator.ItemAnimator;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.behavior.SwipeBehavior;
import xyz.zedler.patrick.grocy.dao.ShoppingListItemDao;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.databinding.FragmentShoppingListBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListClearBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListItemBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.TextEditBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.EmptyStateHelper;
import xyz.zedler.patrick.grocy.helper.LoadOfflineDataShoppingListHelper;
import xyz.zedler.patrick.grocy.helper.ShoppingListHelper;
import xyz.zedler.patrick.grocy.helper.StoreOfflineDataShoppingListHelper;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.util.AnimUtil;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.view.FilterChip;

public class ShoppingListFragment extends BaseFragment implements
        ShoppingListItemAdapter.ShoppingListItemAdapterListener,
        LoadOfflineDataShoppingListHelper.AsyncResponse,
        StoreOfflineDataShoppingListHelper.AsyncResponse {

    private final static String TAG = ShoppingListFragment.class.getSimpleName();

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private DownloadHelper dlHelper;
    private AppDatabase database;
    private GrocyApi grocyApi;
    private AppBarBehavior appBarBehavior;
    private ShoppingListItemAdapter shoppingListItemAdapter;
    private ClickUtil clickUtil;
    private AnimUtil animUtil;
    private FragmentShoppingListBinding binding;
    private SwipeBehavior swipeBehavior;
    private EmptyStateHelper emptyStateHelper;

    private FilterChip chipUndone;
    private FilterChip chipMissing;

    private ArrayList<ShoppingList> shoppingLists;
    private ArrayList<ShoppingListItem> shoppingListItems;
    private ArrayList<ShoppingListItem> shoppingListItemsSelected;
    private ArrayList<MissingItem> missingItems;
    private ArrayList<ShoppingListItem> missingShoppingListItems;
    private ArrayList<ShoppingListItem> undoneShoppingListItems;
    private ArrayList<ShoppingListItem> filteredItems;
    private ArrayList<ShoppingListItem> displayedItems;
    private ArrayList<QuantityUnit> quantityUnits;
    private ArrayList<Product> products;
    private ArrayList<ProductGroup> productGroups;
    private ArrayList<GroupedListItem> groupedListItems;
    private HashMap<Integer, ShoppingList> shoppingListHashMap;

    private int selectedShoppingListId;
    private String startupShoppingListName;
    private String itemsToDisplay;
    private String search;
    private String errorState;
    private boolean isDataStored;
    private boolean showOffline;
    private boolean isRestoredInstance;
    private boolean debug = false;

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

        if(emptyStateHelper != null) emptyStateHelper.destroyInstance();
        if(binding != null) {
            binding.recyclerShoppingList.animate().cancel();
            binding.buttonShoppingListLists.animate().cancel();
            binding.textShoppingListTitle.animate().cancel();
            binding.recyclerShoppingList.setAdapter(null);
            binding = null;
        }
        dlHelper.destroy();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if(isHidden()) return;

        activity = (MainActivity) requireActivity();

        // PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        // UTILS

        clickUtil = new ClickUtil();
        animUtil = new AnimUtil();

        // WEB

        dlHelper = new DownloadHelper(activity, TAG);
        database = AppDatabase.getAppDatabase(activity.getApplicationContext());
        grocyApi = activity.getGrocy();

        // INITIALIZE VARIABLES

        shoppingLists = new ArrayList<>();
        shoppingListItems = new ArrayList<>();
        shoppingListItemsSelected = new ArrayList<>();
        missingItems = new ArrayList<>();
        missingShoppingListItems = new ArrayList<>();
        undoneShoppingListItems = new ArrayList<>();
        filteredItems = new ArrayList<>();
        displayedItems = new ArrayList<>();
        quantityUnits = new ArrayList<>();
        products = new ArrayList<>();
        productGroups = new ArrayList<>();
        groupedListItems = new ArrayList<>();
        shoppingListHashMap = new HashMap<>();

        itemsToDisplay = Constants.SHOPPING_LIST.FILTER.ALL;
        search = "";
        showOffline = false;
        errorState = Constants.STATE.NONE;
        isRestoredInstance = false;
        int lastId = sharedPrefs.getInt(Constants.PREF.SHOPPING_LIST_LAST_ID, 1);
        if(lastId != 1 && !isFeatureMultipleListsEnabled()) {
            sharedPrefs.edit().putInt(Constants.PREF.SHOPPING_LIST_LAST_ID, 1).apply();
            lastId = 1;
        }
        selectedShoppingListId = lastId;

        // INITIALIZE VIEWS

        // top app bar
        binding.textShoppingListTitle.setOnClickListener(v -> showShoppingListsBottomSheet());
        binding.buttonShoppingListLists.setOnClickListener(v -> showShoppingListsBottomSheet());

        binding.frameShoppingListBack.setOnClickListener(v -> activity.onBackPressed());
        binding.frameShoppingListSearchClose.setOnClickListener(v -> dismissSearch());
        binding.editTextShoppingListSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                search = s.toString();
            }
        });
        binding.editTextShoppingListSearch.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        Editable search = binding.editTextShoppingListSearch.getText();
                        searchItems(search != null ? search.toString() : "");
                        activity.hideKeyboard();
                        return true;
                    } return false;
                });
        emptyStateHelper = new EmptyStateHelper(this, binding.linearEmpty);

        // APP BAR BEHAVIOR

        appBarBehavior = new AppBarBehavior(
                activity,
                R.id.linear_shopping_list_app_bar_default,
                R.id.linear_shopping_list_app_bar_search
        );

        // SWIPE REFRESH

        binding.swipeShoppingList.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipeShoppingList.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        binding.swipeShoppingList.setOnRefreshListener(this::refresh);

        // CHIPS

        chipMissing = new FilterChip(
                activity,
                R.color.retro_blue_bg,
                activity.getString(R.string.msg_missing_products, 0),
                () -> {
                    chipUndone.changeState(false);
                    filterItems(Constants.SHOPPING_LIST.FILTER.MISSING);
                },
                () -> filterItems(Constants.SHOPPING_LIST.FILTER.ALL)
        );
        chipMissing.setId(R.id.chip_shopping_filter_missing);
        chipUndone = new FilterChip(
                activity,
                R.color.retro_yellow_bg,
                activity.getString(R.string.msg_undone_items, 0),
                () -> {
                    chipMissing.changeState(false);
                    filterItems(Constants.SHOPPING_LIST.FILTER.UNDONE);
                },
                () -> filterItems(Constants.SHOPPING_LIST.FILTER.ALL)
        );
        chipUndone.setId(R.id.chip_shopping_filter_undone);

        // clear filter container
        binding.linearShoppingListFilterContainer.removeAllViews();

        binding.linearShoppingListFilterContainer.addView(chipMissing);
        binding.linearShoppingListFilterContainer.addView(chipUndone);

        if(savedInstanceState == null) binding.scrollShoppingList.scrollTo(0, 0);

        binding.recyclerShoppingList.setLayoutManager(
                new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        );
        binding.recyclerShoppingList.setItemAnimator(new ItemAnimator());
        binding.recyclerShoppingList.setAdapter(new StockPlaceholderAdapter());

        if(swipeBehavior == null) {
            swipeBehavior = new SwipeBehavior(activity) {
                @Override
                public void instantiateUnderlayButton(
                        RecyclerView.ViewHolder viewHolder,
                        List<UnderlayButton> underlayButtons
                ) {
                    if(viewHolder.getItemViewType() == GroupedListItem.TYPE_ENTRY) {

                        underlayButtons.add(new UnderlayButton(
                                R.drawable.ic_round_done,
                                position -> toggleDoneStatus(position)
                        ));

                        // check if item has product or is only note
                        if(viewHolder.getAdapterPosition() == -1
                                || ((ShoppingListItem) groupedListItems
                                .get(viewHolder.getAdapterPosition()))
                                .getProduct() == null
                                || showOffline
                        ) return;

                        underlayButtons.add(new UnderlayButton(
                                R.drawable.ic_round_local_grocery_store,
                                position -> purchaseItem(position)
                        ));
                    }
                }
            };
            swipeBehavior.attachToRecyclerView(binding.recyclerShoppingList);
        }

        hideDisabledFeatures();

        if(savedInstanceState == null) {
            load();
        } else {
            restoreSavedInstanceState(savedInstanceState);
        }

        // UPDATE UI
        updateUI((getArguments() == null
                || getArguments().getBoolean(Constants.ARGUMENT.ANIMATED, true))
                && savedInstanceState == null);

        setArguments(null);
    }

    private void updateUI(boolean animated) {
        activity.showHideDemoIndicator(this, animated);
        activity.getScrollBehavior().setUpScroll(R.id.scroll_shopping_list);
        activity.getScrollBehavior().setHideOnScroll(true);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.CENTER,
                showOffline ? R.menu.menu_shopping_list_offline : R.menu.menu_shopping_list,
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

    private void updateUI() {
        updateUI(getArguments() == null || getArguments().getBoolean(
                Constants.ARGUMENT.ANIMATED, true));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(isHidden()) return;

        outState.putParcelableArrayList("shoppingLists", shoppingLists);
        outState.putParcelableArrayList("shoppingListItems", shoppingListItems);
        outState.putParcelableArrayList("shoppingListItemsSelected", shoppingListItemsSelected);
        outState.putParcelableArrayList("missingItems", missingItems);
        outState.putParcelableArrayList("missingShoppingListItems", missingShoppingListItems);
        outState.putParcelableArrayList("undoneShoppingListItems", undoneShoppingListItems);
        outState.putParcelableArrayList("filteredItems", filteredItems);
        outState.putParcelableArrayList("displayedItems", displayedItems);
        outState.putParcelableArrayList("quantityUnits", quantityUnits);
        outState.putParcelableArrayList("products", products);
        outState.putParcelableArrayList("productGroups", productGroups);

        outState.putString("itemsToDisplay", itemsToDisplay);
        outState.putString("errorState", errorState);
        outState.putString("search", search);
        outState.putBoolean("isDataStored", isDataStored);
        outState.putBoolean("showOffline", showOffline);
        outState.putString("startupShoppingListName", startupShoppingListName);
        outState.putInt("selectedShoppingListId", selectedShoppingListId);

        appBarBehavior.saveInstanceState(outState);
    }

    private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
        if(isHidden()) return;

        errorState = savedInstanceState.getString("errorState", Constants.STATE.NONE);
        setError(errorState, false);

        shoppingLists = savedInstanceState.getParcelableArrayList("shoppingLists");
        shoppingListItems = savedInstanceState.getParcelableArrayList("shoppingListItems");
        shoppingListItemsSelected = savedInstanceState.getParcelableArrayList(
                "shoppingListItemsSelected"
        );
        missingItems = savedInstanceState.getParcelableArrayList("missingItems");
        missingShoppingListItems = savedInstanceState.getParcelableArrayList(
                "missingShoppingListItems"
        );
        undoneShoppingListItems = savedInstanceState.getParcelableArrayList(
                "undoneShoppingListItems"
        );
        filteredItems = savedInstanceState.getParcelableArrayList("filteredItems");
        quantityUnits = savedInstanceState.getParcelableArrayList("quantityUnits");
        products = savedInstanceState.getParcelableArrayList("products");
        productGroups = savedInstanceState.getParcelableArrayList("productGroups");

        groupedListItems = new ArrayList<>();
        shoppingListHashMap = new HashMap<>();

        appBarBehavior.restoreInstanceState(savedInstanceState);

        binding.swipeShoppingList.setRefreshing(false);
        showOffline = savedInstanceState.getBoolean("showOffline");

        // SEARCH
        search = savedInstanceState.getString("search", "");
        binding.editTextShoppingListSearch.setText(search);

        // FILTERS
        isRestoredInstance = true;
        filterItems(
                savedInstanceState.getString("itemsToDisplay", Constants.STOCK.FILTER.ALL)
        );

        chipMissing.setText(
                activity.getString(R.string.msg_missing_products, missingItems.size())
        );
        chipUndone.setText(
                activity.getString(R.string.msg_undone_items, undoneShoppingListItems.size())
        );
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if(hidden) return;

        if(getView() != null) onViewCreated(getView(), null);
    }

    private void load() {
        if(activity.isOnline()) {
            download();
        } else {
            showOffline = true;
            appBarOfflineInfo(true);
            new LoadOfflineDataShoppingListHelper(
                    AppDatabase.getAppDatabase(activity.getApplicationContext()),
                    this
            ).execute();
        }
    }

    public void refresh() {
        if(activity.isOnline()) {
            setError(Constants.STATE.NONE, true);
            download();
        } else {
            binding.swipeShoppingList.setRefreshing(false);
            if(!showOffline) {
                showOffline = true;
                appBarOfflineInfo(true);
                updateUI();
                new LoadOfflineDataShoppingListHelper(
                        AppDatabase.getAppDatabase(activity.getApplicationContext()),
                        this
                ).execute();
            }
            showMessage(activity.getString(R.string.msg_no_connection));
        }
    }

    private void setError(String state, boolean animated) {
        errorState = state;

        binding.relativeError.buttonErrorRetry.setOnClickListener(v -> refresh());

        View viewIn = binding.relativeError.relativeError;
        View viewOut = binding.scrollShoppingList;

        switch (state) {
            case Constants.STATE.OFFLINE:
                binding.relativeError.imageError.setImageResource(R.drawable.illustration_broccoli);
                binding.relativeError.textErrorTitle.setText(R.string.error_offline);
                binding.relativeError.textErrorSubtitle.setText(R.string.error_offline_subtitle);
                emptyStateHelper.clearState();
                break;
            case Constants.STATE.ERROR:
                binding.relativeError.imageError.setImageResource(R.drawable.illustration_popsicle);
                binding.relativeError.textErrorTitle.setText(R.string.error_unknown);
                binding.relativeError.textErrorSubtitle.setText(R.string.error_undefined);
                emptyStateHelper.clearState();
                break;
            case Constants.STATE.NONE:
                viewIn = binding.scrollShoppingList;
                viewOut = binding.relativeError.relativeError;
                break;
        }

        animUtil.replaceViews(viewIn, viewOut, animated);
    }

    private void download() {
        binding.swipeShoppingList.setRefreshing(true);
        DownloadHelper.Queue queue = dlHelper.newQueue(
                this::onQueueEmpty,
                this::onDownloadError
        );
        queue.append(
                dlHelper.getShoppingLists(listItems -> this.shoppingLists = listItems),
                dlHelper.getShoppingListItems(listItems -> this.shoppingListItems = listItems),
                dlHelper.getProductGroups(listItems -> this.productGroups = listItems),
                dlHelper.getQuantityUnits(listItems -> this.quantityUnits = listItems),
                dlHelper.getProducts(listItems -> this.products = listItems),
                dlHelper.getVolatile((expiring, expired, missing) -> missingItems = missing)
        );
        queue.start();
    }

    private void onQueueEmpty() {
        if(showOffline) {
            showOffline = false;
            appBarOfflineInfo(false);
            updateUI();
        }

        if(!isDataStored) {
            // set shopping list if chosen with name on fragment start
            if(startupShoppingListName != null) {
                for(ShoppingList shoppingList : shoppingLists) {
                    if(shoppingList.getName().equals(startupShoppingListName)) {
                        selectShoppingList(shoppingList.getId());
                    }
                }
                startupShoppingListName = null;
            }
            changeAppBarTitle();

            ArrayList<String> missingProductIds = new ArrayList<>();
            for(MissingItem missingItem : missingItems) {
                missingProductIds.add(String.valueOf(missingItem.getId()));
            }
            missingShoppingListItems = new ArrayList<>();
            undoneShoppingListItems = new ArrayList<>();
            shoppingListItemsSelected = new ArrayList<>();
            ArrayList<Integer> allUsedProductIds = new ArrayList<>();  // for database preparing
            for(ShoppingListItem shoppingListItem : shoppingListItems) {
                if(shoppingListItem.getProductId() != null) {
                    allUsedProductIds.add(Integer.parseInt(shoppingListItem.getProductId()));
                }
                if(shoppingListItem.getShoppingListId() != selectedShoppingListId) continue;
                shoppingListItemsSelected.add(shoppingListItem);
                if(missingProductIds.contains(shoppingListItem.getProductId())) {
                    shoppingListItem.setIsMissing(true);
                    missingShoppingListItems.add(shoppingListItem);
                }
                if(shoppingListItem.getDone() == 0) {
                    undoneShoppingListItems.add(shoppingListItem);
                }
            }
            chipMissing.setText(
                    activity.getString(R.string.msg_missing_products, missingShoppingListItems.size())
            );
            chipUndone.setText(
                    activity.getString(R.string.msg_undone_items, undoneShoppingListItems.size())
            );

            // sync modified data and store new data
            new StoreOfflineDataShoppingListHelper(
                    AppDatabase.getAppDatabase(activity.getApplicationContext()),
                    this,
                    true,
                    shoppingLists,
                    shoppingListItems,
                    productGroups,
                    quantityUnits,
                    products,
                    allUsedProductIds,
                    false
            ).execute();
        } else {
            isDataStored = false;

            tidyUpItems(); // only for deleting "lost" items if multiple lists feature is disabled

            // set product in shoppingListItem
            HashMap<Integer, Product> productHashMap = new HashMap<>();
            for(Product p : products) productHashMap.put(p.getId(), p);
            for(ShoppingListItem shoppingListItem : shoppingListItemsSelected) {
                if(shoppingListItem.getProductId() == null) continue;
                shoppingListItem.setProduct(
                        productHashMap.get(Integer.parseInt(shoppingListItem.getProductId()))
                );
            }

            binding.swipeShoppingList.setRefreshing(false);
            filterItems(itemsToDisplay);
        }
    }

    private void onDownloadError(VolleyError ignored) {
        if(binding != null) binding.swipeShoppingList.setRefreshing(false);
        if(activity == null) return;
        if(!showOffline) {
            showOffline = true;
            appBarOfflineInfo(true);
            updateUI();
        }
        new LoadOfflineDataShoppingListHelper(
                AppDatabase.getAppDatabase(activity.getApplicationContext()),
                this
        ).execute();
    }

    @Override
    public void prepareOfflineData(
            ArrayList<ShoppingListItem> shoppingListItems,
            ArrayList<ShoppingList> shoppingLists,
            ArrayList<ProductGroup> productGroups,
            ArrayList<QuantityUnit> quantityUnits
    ) { // for offline mode
        this.shoppingListItems = shoppingListItems;
        this.shoppingLists = shoppingLists;
        this.productGroups = productGroups;
        this.quantityUnits = quantityUnits;

        missingShoppingListItems = new ArrayList<>();
        undoneShoppingListItems = new ArrayList<>();
        shoppingListItemsSelected = new ArrayList<>();

        for(ShoppingListItem shoppingListItem : shoppingListItems) {
            if(shoppingListItem.getShoppingListId() != selectedShoppingListId) continue;
            shoppingListItemsSelected.add(shoppingListItem);
            if(shoppingListItem.getDone() == 0) {
                undoneShoppingListItems.add(shoppingListItem);
            }
            if(shoppingListItem.isMissing()) {
                missingShoppingListItems.add(shoppingListItem);
            }
        }

        chipMissing.setText(
                activity.getString(R.string.msg_missing_products, missingShoppingListItems.size())
        );
        chipUndone.setText(
                activity.getString(R.string.msg_undone_items, undoneShoppingListItems.size())
        );
        changeAppBarTitle();

        filterItems(itemsToDisplay);
    }

    private void filterItems(String filter) {
        itemsToDisplay = filter.isEmpty() ? Constants.SHOPPING_LIST.FILTER.ALL : filter;
        if(debug) Log.i(
                TAG, "filterItems: filter = " + filter + ", display = " + itemsToDisplay
        );
        // VOLATILE
        switch (itemsToDisplay) {
            case Constants.SHOPPING_LIST.FILTER.MISSING:
                filteredItems = this.missingShoppingListItems;
                break;
            case Constants.SHOPPING_LIST.FILTER.UNDONE:
                filteredItems = this.undoneShoppingListItems;
                break;
            default:
                filteredItems = this.shoppingListItemsSelected;
                break;
        }
        if(debug) Log.i(TAG, "filterItems: filteredItems = " + filteredItems);
        // SEARCH
        if(!search.isEmpty()) { // active search
            searchItems(search);
        } else {
            // EMPTY STATES
            if(filteredItems.isEmpty() && errorState.equals(Constants.STATE.NONE)) {
                if(itemsToDisplay.equals(Constants.SHOPPING_LIST.FILTER.MISSING)
                        || itemsToDisplay.equals(Constants.SHOPPING_LIST.FILTER.UNDONE)
                ) {
                    emptyStateHelper.setNoFilterResults();
                } else {
                    emptyStateHelper.setEmpty();
                }
            } else {
                emptyStateHelper.clearState();
            }

            // SORTING
            if(displayedItems != filteredItems || isRestoredInstance) {
                displayedItems = filteredItems;
                groupItems();
            }
            isRestoredInstance = false;
        }
    }

    private void searchItems(String search) {
        search = search.toLowerCase();
        if(debug) Log.i(TAG, "searchItems: search = " + search);
        this.search = search;
        if(search.isEmpty()) {
            filterItems(itemsToDisplay);
        } else { // only if search contains something
            ArrayList<ShoppingListItem> searchedItems = new ArrayList<>();
            for(ShoppingListItem shoppingListItem : filteredItems) {
                String name;
                String description = null;
                if(shoppingListItem.getProduct() != null) {
                    name = shoppingListItem.getProduct().getName();
                    description = shoppingListItem.getProduct().getDescription();
                } else {
                    name = shoppingListItem.getNote();
                }
                name = name != null ? name.toLowerCase() : "";
                description = description != null ? description.toLowerCase() : "";
                if(name.contains(search) || description.contains(search)) {
                    searchedItems.add(shoppingListItem);
                }
            }
            if(searchedItems.isEmpty() && errorState.equals(Constants.STATE.NONE)) {
                emptyStateHelper.setNoSearchResults();
            } else {
                emptyStateHelper.clearState();
            }
            if(displayedItems != searchedItems) {
                displayedItems = searchedItems;
                groupItems();
            }
        }
    }

    private void groupItems() {
        groupedListItems = ShoppingListHelper.groupItems(
                activity,
                displayedItems,
                productGroups,
                shoppingLists,
                selectedShoppingListId,
                search.isEmpty() && itemsToDisplay.equals(
                        Constants.SHOPPING_LIST.FILTER.ALL
                )
        );
        refreshAdapter(
                new ShoppingListItemAdapter(
                        activity,
                        groupedListItems,
                        quantityUnits,
                        this
                )
        );
    }

    private void refreshAdapter(ShoppingListItemAdapter adapter) {
        shoppingListItemAdapter = adapter;
        if(isRestoredInstance) {
            binding.recyclerShoppingList.setAdapter(adapter);
            return;
        }
        binding.recyclerShoppingList.animate().alpha(0).setDuration(150).withEndAction(() -> {
            binding.recyclerShoppingList.setAdapter(adapter);
            binding.recyclerShoppingList.animate().alpha(1).setDuration(150).start();
        }).start();
    }

    private void showShoppingListsBottomSheet() {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(Constants.ARGUMENT.SHOPPING_LISTS, shoppingLists);
        bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedShoppingListId);
        bundle.putBoolean(
                Constants.ARGUMENT.SHOW_OFFLINE,
                showOffline || !isFeatureMultipleListsEnabled()
        );
        activity.showBottomSheet(new ShoppingListsBottomSheet(), bundle);
    }

    public void selectShoppingList(int shoppingListId) {
        if(shoppingListId == selectedShoppingListId) return;
        ShoppingList shoppingList = getShoppingList(shoppingListId);
        if(shoppingList == null) return;
        selectedShoppingListId = shoppingListId;
        sharedPrefs.edit().putInt(Constants.PREF.SHOPPING_LIST_LAST_ID, shoppingListId).apply();
        changeAppBarTitle(shoppingList);
        chipMissing.changeState(false);
        chipUndone.changeState(false);
        itemsToDisplay = Constants.SHOPPING_LIST.FILTER.ALL;
        if(showOffline) {
            new LoadOfflineDataShoppingListHelper(
                    AppDatabase.getAppDatabase(activity.getApplicationContext()),
                    this
            ).execute();
        } else {
            onQueueEmpty();
        }
        setUpBottomMenu(); // to hide delete action if necessary
    }

    private void changeAppBarTitle(ShoppingList shoppingList) {
        ShoppingListHelper.changeAppBarTitle(
                binding.textShoppingListTitle,
                binding.buttonShoppingListLists,
                shoppingList
        );
    }

    private void changeAppBarTitle() {
        ShoppingList shoppingList = getShoppingList(selectedShoppingListId);
        changeAppBarTitle(shoppingList);
    }

    public void selectShoppingList(String shoppingListName) {
        startupShoppingListName = shoppingListName;
    }

    private ShoppingList getShoppingList(int shoppingListId) {
        if(shoppingListHashMap.isEmpty()) {
            for(ShoppingList s : shoppingLists) shoppingListHashMap.put(s.getId(), s);
        }
        return shoppingListHashMap.get(shoppingListId);
    }

    public void toggleDoneStatus(int position) {
        ShoppingListItem shoppingListItem = (ShoppingListItem) groupedListItems.get(position);

        if(shoppingListItem.getDoneSynced() == -1) {
            shoppingListItem.setDoneSynced(shoppingListItem.getDone());
        }

        shoppingListItem.setDone(shoppingListItem.getDone() == 0 ? 1 : 0);  // toggle state

        if(showOffline) {
            updateDoneStatus(shoppingListItem, position);
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("done", shoppingListItem.getDone());
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "toggleDoneStatus: " + e);
        }
        dlHelper.editShoppingListItem(
                shoppingListItem.getId(),
                body,
                response -> updateDoneStatus(shoppingListItem, position),
                error -> {
                    showMessage(activity.getString(R.string.error_undefined));
                    if(debug) Log.e(TAG, "toggleDoneStatus: " + error);
                }
        ).perform(dlHelper.getUuid());
    }

    private void updateDoneStatus(ShoppingListItem shoppingListItem, int position) {
        new Thread(() -> database.shoppingListItemDao().update(shoppingListItem)).start();
        if(shoppingListItem.getDone() == 1) {
            undoneShoppingListItems.remove(shoppingListItem);
            if(undoneShoppingListItems.isEmpty()) emptyStateHelper.setNoFilterResults();
        } else {
            undoneShoppingListItems = new ArrayList<>();
            for(ShoppingListItem shoppingListItem1 : shoppingListItems) {
                if(shoppingListItem1.getShoppingListId() != selectedShoppingListId) {
                    continue;
                }
                if(shoppingListItem1.getDone() == 0) {
                    undoneShoppingListItems.add(shoppingListItem1);
                }
            }
        }
        chipUndone.setText(
                activity.getString(
                        R.string.msg_undone_items,
                        undoneShoppingListItems.size()
                )
        );
        if(itemsToDisplay.equals(Constants.SHOPPING_LIST.FILTER.UNDONE)) {
            removeItemFromList(position);
        } else {
            shoppingListItemAdapter.notifyItemChanged(position);
            swipeBehavior.recoverLatestSwipedItem();
        }
    }

    public void editItem(int position) {
        navigate(ShoppingListFragmentDirections
                .actionShoppingListFragmentToShoppingListItemEditFragment(Constants.ACTION.EDIT)
                .setShoppingListItem((ShoppingListItem) groupedListItems.get(position)));
    }

    public void saveNotes(Spanned notes) {
        JSONObject body = new JSONObject();

        String notesHtml = notes != null ? Html.toHtml(notes) : "";
        try {
            body.put("description", notesHtml);
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "saveNotes: " + e);
        }
        dlHelper.put(
                grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LISTS, selectedShoppingListId),
                body,
                response -> {
                    ShoppingList shoppingList = getShoppingList(selectedShoppingListId);
                    if(shoppingList == null) return;
                    shoppingList.setNotes(notesHtml);
                    onQueueEmpty();
                },
                error -> {
                    showMessage(activity.getString(R.string.error_undefined));
                    if(debug) Log.e(TAG, "saveNotes: " + error);
                }
        );
    }

    public void purchaseItem(int position) {
        if(showOffline) return;
        ShoppingListItem shoppingListItem = (ShoppingListItem) groupedListItems.get(position);
        navigate(ShoppingListFragmentDirections.actionShoppingListFragmentToPurchaseFragment()
                .setShoppingListItems(new ShoppingListItem[]{shoppingListItem})
                .setCloseWhenFinished(true));
    }

    public void deleteItem(int position) {
        ShoppingListItem shoppingListItem = (ShoppingListItem) groupedListItems.get(position);
        dlHelper.delete(
                grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, shoppingListItem.getId()),
                response -> removeItemFromList(position),
                error -> {
                    showMessage(activity.getString(R.string.error_undefined));
                    if(debug) Log.e(TAG, "deleteItem: " + error);
                }
        );
    }

    private void removeItemFromList(int position) {
        ShoppingListHelper.removeItemFromList(shoppingListItemAdapter, groupedListItems, position);
    }

    public void addItem() {
        navigate(ShoppingListFragmentDirections
                .actionShoppingListFragmentToShoppingListItemEditFragment(Constants.ACTION.CREATE)
                .setSelectedShoppingListId(selectedShoppingListId));
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
        ShoppingList shoppingList = getShoppingList(selectedShoppingListId);
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
                ShoppingList shoppingList = getShoppingList(selectedShoppingListId);
                if(shoppingList != null) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("list_id", selectedShoppingListId);
                    } catch (JSONException e) {
                        if(debug) Log.e(TAG, "setUpBottomMenu: add missing: " + e);
                    }
                    dlHelper.post(
                            grocyApi.addMissingProducts(),
                            jsonObject,
                            response -> {
                                showMessage(
                                        activity.getString(
                                                R.string.msg_added_missing_products,
                                                shoppingList.getName()
                                        )
                                );
                                refresh();
                            },
                            error -> {
                                showMessage(activity.getString(R.string.error_undefined));
                                if(debug) Log.e(
                                        TAG, "setUpBottomMenu: add missing "
                                                + shoppingList.getName()
                                                + ": " + error
                                );
                            }
                    );
                } else {
                    showMessage(activity.getString(R.string.error_undefined));
                }
                return true;
            });
        }

        MenuItem purchaseItems = activity.getBottomMenu().findItem(R.id.action_purchase_all_items);
        if(purchaseItems != null) {
            purchaseItems.setOnMenuItemClickListener(item -> {
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
                ShoppingList shoppingList = getShoppingList(selectedShoppingListId);
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

        MenuItem editShoppingList = activity.getBottomMenu().findItem(
                R.id.action_edit_shopping_list
        );
        if(editShoppingList != null) {
            editShoppingList.setOnMenuItemClickListener(item -> {
                ShoppingList shoppingList = getShoppingList(selectedShoppingListId);
                if(shoppingList == null) {
                    showMessage(activity.getString(R.string.error_undefined));
                    return true;
                }
                Bundle bundle = new Bundle();
                bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.EDIT);
                bundle.putParcelable(Constants.ARGUMENT.SHOPPING_LIST, shoppingList);
                //activity.replaceFragment(Constants.UI.SHOPPING_LIST_EDIT, bundle, true); //TODO
                return true;
            });
        }

        MenuItem deleteShoppingList = activity.getBottomMenu().findItem(
                R.id.action_delete_shopping_list
        );
        if(deleteShoppingList != null) {
            if(selectedShoppingListId == 1) {
                deleteShoppingList.setVisible(false);
            } else {
                deleteShoppingList.setVisible(true);
            }
            deleteShoppingList.setOnMenuItemClickListener(item -> {
                ShoppingList shoppingList = getShoppingList(selectedShoppingListId);
                if(shoppingList == null) {
                    showMessage(activity.getString(R.string.error_undefined));
                    return true;
                }
                clearAllItems(
                        shoppingList,
                        () -> {
                            deleteShoppingList(shoppingList);
                            tidyUpItems();
                        }
                );
                return true;
            });
        }
    }

    public void clearAllItems(
            ShoppingList shoppingList,
            OnResponseListener responseListener
    ) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("list_id", selectedShoppingListId);
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "clearShoppingList: " + e);
        }
        dlHelper.post(
                grocyApi.clearShoppingList(),
                jsonObject,
                response -> responseListener.onResponse(),
                error -> {
                    showMessage(activity.getString(R.string.error_undefined));
                    if(debug) Log.e(
                            TAG, "clearShoppingList: "
                                    + shoppingList.getName()
                                    + ": " + error
                    );
                }
        );
    }

    public void clearDoneItems(ShoppingList shoppingList) {
        DownloadHelper.Queue queue = dlHelper.newQueue(
                () -> {
                    showMessage(
                            activity.getString(
                                    R.string.msg_shopping_list_cleared,
                                    shoppingList.getName()
                            )
                    );
                    refresh();
                },
                volleyError -> {
                    showMessage(activity.getString(R.string.error_undefined));
                    refresh();
                }
        );
        for(ShoppingListItem shoppingListItem : shoppingListItems) {
            if(shoppingListItem.getShoppingListId() != shoppingList.getId()) continue;
            if(shoppingListItem.getDone() == 0) continue;
            queue.append(dlHelper.deleteShoppingListItem(shoppingListItem.getId()));
        }
        queue.start();
    }

    private void deleteShoppingList(ShoppingList shoppingList) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("list_id", selectedShoppingListId);
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "deleteShoppingList: delete list: " + e);
        }

        dlHelper.delete(
                grocyApi.getObject(
                        GrocyApi.ENTITY.SHOPPING_LISTS,
                        shoppingList.getId()
                ),
                response -> {
                    showMessage(
                            activity.getString(
                                    R.string.msg_shopping_list_deleted,
                                    shoppingList.getName()
                            )
                    );
                    shoppingLists.remove(shoppingList);
                    selectShoppingList(1);
                },
                error -> {
                    showMessage(activity.getString(R.string.error_undefined));
                    if(debug) Log.e(
                            TAG, "deleteShoppingList: delete "
                                    + shoppingList.getName()
                                    + ": " + error
                    );
                }
        );
    }

    private void tidyUpItems() {
        // Tidy up lost shopping list items, which have deleted shopping lists
        // as an id – else they will never show up on any shopping list
        ArrayList<Integer> listIds = new ArrayList<>();
        if(isFeatureMultipleListsEnabled()) {
            for(ShoppingList shoppingList : shoppingLists) listIds.add(shoppingList.getId());
            if(listIds.isEmpty()) return;  // possible if download error happened
        } else {
            listIds.add(1);  // id of first and single shopping list
        }

        ShoppingListItemDao itemDao = database.shoppingListItemDao();
        for(ShoppingListItem listItem : shoppingListItems) {
            if(!listIds.contains(listItem.getShoppingListId())) {
                if(debug) Log.i(TAG, "tidyUpItems: " + listItem);
                dlHelper.delete(
                        grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, listItem.getId()),
                        response -> new Thread(() -> itemDao.delete(listItem) // delete in db too
                        ).start(),
                        error -> {}
                );
            }
        }
    }

    @Override
    public void syncItems(
            ArrayList<ShoppingListItem> itemsToSync,
            ArrayList<ShoppingList> shoppingLists,
            ArrayList<ShoppingListItem> shoppingListItems,
            ArrayList<ProductGroup> productGroups,
            ArrayList<QuantityUnit> quantityUnits,
            ArrayList<Product> products,
            ArrayList<Integer> usedProductIds,
            HashMap<Integer, ShoppingListItem> serverItemHashMap,
            boolean onlyDeltaUpdateAdapter
    ) {
        DownloadHelper.Queue queue = dlHelper.newQueue(
                () -> {
                    showMessage(getString(R.string.msg_synced));
                    new StoreOfflineDataShoppingListHelper(
                            AppDatabase.getAppDatabase(activity.getApplicationContext()),
                            this,
                            false,
                            shoppingLists,
                            shoppingListItems,
                            productGroups,
                            quantityUnits,
                            products,
                            usedProductIds,
                            onlyDeltaUpdateAdapter
                    ).execute();
                },
                error -> showMessage(getString(R.string.msg_failed_to_sync)
                ));
        for(ShoppingListItem itemToSync : itemsToSync) {
            JSONObject body = new JSONObject();
            try {
                body.put("done", itemToSync.getDone());
            } catch (JSONException e) {
                if(debug) Log.e(TAG, "syncItems: " + e);
            }
            queue.append(
                    dlHelper.editShoppingListItem(
                            itemToSync.getId(),
                            body,
                            response -> {
                                ShoppingListItem serverItem = serverItemHashMap.get(
                                        itemToSync.getId()
                                );
                                if(serverItem != null) serverItem.setDone(itemToSync.getDone());
                            }
                    )
            );
        }
        queue.start();
    }

    @Override
    public void storedDataSuccessfully(
            ArrayList<ShoppingListItem> shoppingListItems,
            boolean onlyDeltaUpdateAdapter
    ) {
        isDataStored = true;
        this.shoppingListItems = shoppingListItems;
        onQueueEmpty();
    }

    @Override
    public void onItemRowClicked(int position) {
        if(clickUtil.isDisabled()) return;
        swipeBehavior.recoverLatestSwipedItem();

        GroupedListItem groupedListItem = groupedListItems.get(position);
        if(groupedListItem.getType() == GroupedListItem.TYPE_ENTRY) {
            showItemBottomSheet(groupedListItems.get(position), position);
        } else {  // Click on bottom notes
            if(!showOffline) showNotesEditor();
        }
    }

    public void updateConnectivity(boolean online) {
        if(!online == showOffline) return;
        refresh();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Constants.REQUEST.SHOPPING_MODE) {
            refresh();
        }
    }

    private void hideDisabledFeatures() {
        if(!isFeatureMultipleListsEnabled()) {
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
                QuantityUnit quantityUnit = getQuantityUnit(
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
            bundle.putBoolean(Constants.ARGUMENT.SHOW_OFFLINE, showOffline);
            activity.showBottomSheet(new ShoppingListItemBottomSheet(), bundle);
        }
    }

    private QuantityUnit getQuantityUnit(int id) {
        for(QuantityUnit quantityUnit : quantityUnits) {
            if(quantityUnit.getId() == id) {
                return quantityUnit;
            }
        } return null;
    }

    private void setUpSearch() {
        if(search.isEmpty()) { // only if no search is active
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
        filterItems(itemsToDisplay);

        emptyStateHelper.clearState();

        setIsSearchVisible(false);
    }

    private void appBarOfflineInfo(boolean visible) {
        boolean currentState = binding.linearOfflineError.getVisibility() == View.VISIBLE;
        if(visible == currentState) return;
        if(visible) {
            binding.linearOfflineError.setAlpha(0);
            binding.linearOfflineError.setVisibility(View.VISIBLE);
            binding.linearOfflineError.animate().alpha(1).setDuration(125).withEndAction(
                    () -> updateScrollViewHeight(true)
            ).start();
        } else {
            binding.linearOfflineError.animate().alpha(0).setDuration(125).withEndAction(
                    () -> {
                        binding.linearOfflineError.setVisibility(View.GONE);
                        updateScrollViewHeight(false);
                    }
            ).start();
        }
    }

    private void updateScrollViewHeight(boolean visible) {
        // get offline indicator height
        int offlineIndicatorHeight;
        if(visible) {
            offlineIndicatorHeight = binding.linearOfflineError.getHeight();
        } else {
            offlineIndicatorHeight = 0;
        }
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams)
                binding.scrollShoppingList.getLayoutParams();
        layoutParams.setMargins(0, offlineIndicatorHeight, 0, 0);
        binding.scrollShoppingList.setLayoutParams(layoutParams);
    }

    private void showMessage(String msg) {
        activity.showSnackbar(
                Snackbar.make(activity.binding.frameMainContainer, msg, Snackbar.LENGTH_SHORT)
        );
    }

    private boolean isFeatureMultipleListsEnabled() {
        return sharedPrefs.getBoolean(Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS, true);
    }

    public interface OnResponseListener {
        void onResponse();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}