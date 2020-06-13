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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
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

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.ShoppingActivity;
import xyz.zedler.patrick.grocy.adapter.ShoppingListItemAdapter;
import xyz.zedler.patrick.grocy.adapter.StockPlaceholderAdapter;
import xyz.zedler.patrick.grocy.animator.ItemAnimator;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.behavior.SwipeBehavior;
import xyz.zedler.patrick.grocy.dao.ShoppingListItemDao;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.databinding.FragmentShoppingListBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListItemBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.TextEditBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.EmptyStateHelper;
import xyz.zedler.patrick.grocy.helper.ShoppingListHelper;
import xyz.zedler.patrick.grocy.helper.LoadOfflineDataShoppingListHelper;
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
import xyz.zedler.patrick.grocy.view.FilterChip;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class ShoppingListFragment extends Fragment
        implements ShoppingListItemAdapter.ShoppingListItemAdapterListener,
            LoadOfflineDataShoppingListHelper.AsyncResponse,
            StoreOfflineDataShoppingListHelper.AsyncResponse {

    private final static String TAG = Constants.UI.SHOPPING_LIST;
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private DownloadHelper downloadHelper;
    private AppDatabase database;
    private GrocyApi grocyApi;
    private AppBarBehavior appBarBehavior;
    private WebRequest request;
    private ShoppingListItemAdapter shoppingListItemAdapter;
    private ClickUtil clickUtil = new ClickUtil();
    private AnimUtil animUtil = new AnimUtil();
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
    private boolean isFragmentFromBackground;

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
        binding.recyclerShoppingList.animate().cancel();
        binding.buttonShoppingListLists.animate().cancel();
        binding.textShoppingListTitle.animate().cancel();
        emptyStateHelper.destroyInstance();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if(isHidden()) return;

        activity = (MainActivity) getActivity();
        assert activity != null;

        // GET PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        // WEB REQUESTS

        downloadHelper = new DownloadHelper(
                activity,
                TAG,
                this::onDownloadError,
                this::onQueueEmpty
        );

        database = AppDatabase.getAppDatabase(activity.getApplicationContext());

        request = new WebRequest(activity.getRequestQueue());
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
        selectedShoppingListId = sharedPrefs.getInt(Constants.PREF.SHOPPING_LIST_LAST_ID, 1);
        search = "";
        showOffline = false;
        errorState = Constants.STATE.NONE;
        isRestoredInstance = false;

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

        if(!isFragmentFromBackground) {
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

        activity.updateUI(
                showOffline
                        ? Constants.UI.SHOPPING_LIST_OFFLINE
                        : Constants.UI.SHOPPING_LIST_DEFAULT,
                (getArguments() == null || getArguments().getBoolean(
                        Constants.ARGUMENT.ANIMATED, true)
                ), TAG
        );
        setArguments(null);
        isFragmentFromBackground = false;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(!isHidden()) {
            outState.putParcelableArrayList("shoppingLists", shoppingLists);
            outState.putParcelableArrayList("shoppingListItems", shoppingListItems);
            outState.putParcelableArrayList("shoppingListItemsSelected", shoppingListItemsSelected);
            outState.putParcelableArrayList("missingItems", missingItems);
            outState.putParcelableArrayList("missingShoppingListItems", missingShoppingListItems);
            outState.putParcelableArrayList("undoneShoppingListItems", undoneShoppingListItems);
            outState.putParcelableArrayList("filteredItems", filteredItems);
            outState.putParcelableArrayList("quantityUnits", quantityUnits);
            outState.putParcelableArrayList("products", products);
            outState.putParcelableArrayList("productGroups", productGroups);

            outState.putString("itemsToDisplay", itemsToDisplay);
            outState.putString("errorState", errorState);
            outState.putString("search", search);
            outState.putBoolean("isDataStored", isDataStored);
            outState.putBoolean("showOffline", showOffline);
            outState.putString("startupShoppingListName", startupShoppingListName);

            appBarBehavior.saveInstanceState(outState);
        }
        super.onSaveInstanceState(outState);
    }

    private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
        if(isHidden()) return;

        errorState = savedInstanceState.getString("errorState", Constants.STATE.NONE);
        setError(errorState, false);
        if(errorState.equals(Constants.STATE.OFFLINE)
                || errorState.equals(Constants.STATE.ERROR)
        ) return;

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

        appBarBehavior.restoreInstanceState(savedInstanceState);
        activity.setUI(
                appBarBehavior.isPrimaryLayout()
                        ? Constants.UI.SHOPPING_LIST_DEFAULT
                        : Constants.UI.SHOPPING_LIST_SEARCH
        );

        binding.swipeShoppingList.setRefreshing(false);

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
        isFragmentFromBackground = true;
        onViewCreated(requireView(), null);
    }

    private void updateUI() {
        activity.updateUI(
                showOffline
                        ? Constants.UI.SHOPPING_LIST_OFFLINE
                        : Constants.UI.SHOPPING_LIST_DEFAULT,
                getArguments() == null || getArguments().getBoolean(
                        Constants.ARGUMENT.ANIMATED, true
                ),
                TAG
        );
    }

    private void load() {
        if(activity.isOnline()) {
            download();
        } else {
            showOffline = true;
            new LoadOfflineDataShoppingListHelper(
                    AppDatabase.getAppDatabase(activity.getApplicationContext()),
                    this
            ).execute();
        }
    }

    public void refresh() {
        if(activity.isOnline()) {
            download();
        } else {
            binding.swipeShoppingList.setRefreshing(false);
            if(!showOffline) {
                showOffline = true;
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

        View viewIn = binding.linearError.linearError;
        View viewOut = binding.scrollShoppingList;

        switch (state) {
            case Constants.STATE.OFFLINE:
                binding.linearError.imageError.setImageResource(R.drawable.illustration_broccoli);
                binding.linearError.textErrorTitle.setText(R.string.error_offline);
                binding.linearError.textErrorSubtitle.setText(R.string.error_offline_subtitle);
                emptyStateHelper.clearState();
                break;
            case Constants.STATE.ERROR:
                binding.linearError.imageError.setImageResource(R.drawable.illustration_popsicle);
                binding.linearError.textErrorTitle.setText(R.string.error_unknown);
                binding.linearError.textErrorSubtitle.setText(R.string.error_unknown_subtitle);
                emptyStateHelper.clearState();
                break;
            case Constants.STATE.NONE:
                viewIn = binding.scrollShoppingList;
                viewOut = binding.linearError.linearError;
                break;
        }

        animUtil.replaceViews(viewIn, viewOut, animated);
    }

    private void download() {
        binding.swipeShoppingList.setRefreshing(true);
        downloadHelper.downloadQuantityUnits(quantityUnits -> this.quantityUnits = quantityUnits);
        downloadHelper.downloadProducts(products -> this.products = products);
        downloadHelper.downloadProductGroups(productGroups -> this.productGroups = productGroups);
        downloadHelper.downloadShoppingListItems(listItems -> this.shoppingListItems = listItems);
        downloadHelper.downloadShoppingLists(shoppingLists -> {
            this.shoppingLists = shoppingLists;
            // set shopping list if chosen with name on fragment start
            if(startupShoppingListName != null) {
                for(ShoppingList shoppingList : shoppingLists) {
                    if(shoppingList.getName().equals(startupShoppingListName)) {
                        selectedShoppingListId = shoppingList.getId();
                    }
                }
                startupShoppingListName = null;
            }
            changeAppBarTitle();
        });
        downloadHelper.downloadVolatile((expiring, expired, missing) -> missingItems = missing);
    }

    private void onQueueEmpty() {
        if(showOffline) {
            showOffline = false;
            updateUI();
        }

        if(!isDataStored) {
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
                    allUsedProductIds
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

    private void onDownloadError(VolleyError error) {
        binding.swipeShoppingList.setRefreshing(false);
        if(!showOffline) {
            showOffline = true;
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
    ) {                                                // for offline mode
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
        if(DEBUG) Log.i(
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
        if(DEBUG) Log.i(TAG, "filterItems: filteredItems = " + filteredItems);
        // SEARCH
        if(!search.isEmpty()) { // active search
            searchItems(search);
        } else if(displayedItems != filteredItems) { // only update items in recycler view
            displayedItems = filteredItems;          // if they have changed
            groupItems();
        }
    }

    private void searchItems(String search) {
        search = search.toLowerCase();
        if(DEBUG) Log.i(TAG, "searchItems: search = " + search);
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
            if(displayedItems != searchedItems) {
                displayedItems = searchedItems;
                groupItems();
            }
        }
    }

    private void groupItems() {
        groupedListItems = ShoppingListHelper.groupItems(
                displayedItems,
                productGroups,
                shoppingLists,
                selectedShoppingListId,
                activity
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
        binding.recyclerShoppingList.animate().alpha(0).setDuration(150).withEndAction(() -> {
            binding.recyclerShoppingList.setAdapter(adapter);
            binding.recyclerShoppingList.animate().alpha(1).setDuration(150).start();
        }).start();
    }

    private void showShoppingListsBottomSheet() {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(Constants.ARGUMENT.SHOPPING_LISTS, shoppingLists);
        bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedShoppingListId);
        bundle.putBoolean(Constants.ARGUMENT.SHOW_OFFLINE, showOffline);
        activity.showBottomSheet(new ShoppingListsBottomSheetDialogFragment(), bundle);
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
            Log.e(TAG, "toggleDoneStatus: " + e);
        }
        downloadHelper.editShoppingListItem(
                shoppingListItem.getId(),
                body,
                response -> updateDoneStatus(shoppingListItem, position),
                error -> {
                    showMessage(activity.getString(R.string.msg_error));
                    if(DEBUG) Log.i(TAG, "toggleDoneStatus: " + error);
                },
                false
        );
    }

    private void updateDoneStatus(ShoppingListItem shoppingListItem, int position) {
        new Thread(() -> database.shoppingListItemDao().update(shoppingListItem)).start();
        if(shoppingListItem.getDone() == 1) {
            undoneShoppingListItems.remove(shoppingListItem);
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
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.EDIT);
        bundle.putParcelable(
                Constants.ARGUMENT.SHOPPING_LIST_ITEM,
                (ShoppingListItem) groupedListItems.get(position)
        );
        activity.replaceFragment(
                Constants.UI.SHOPPING_LIST_ITEM_EDIT,
                bundle,
                true
        );
    }

    public void saveNotes(Spanned notes) {
        JSONObject body = new JSONObject();

        String notesHtml = notes != null ? Html.toHtml(notes) : "";
        try {
            body.put("description", notesHtml);
        } catch (JSONException e) {
            Log.e(TAG, "saveNotes: " + e);
        }
        request.put(
                grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LISTS, selectedShoppingListId),
                body,
                response -> {
                    ShoppingList shoppingList = getShoppingList(selectedShoppingListId);
                    if(shoppingList == null) return;
                    shoppingList.setNotes(notesHtml);
                    onQueueEmpty();
                },
                error -> {
                    showMessage(activity.getString(R.string.msg_error));
                    if(DEBUG) Log.i(TAG, "saveNotes: " + error);
                }
        );
    }

    public void purchaseItem(int position) {
        if(showOffline) return;
        ShoppingListItem shoppingListItem = (ShoppingListItem) groupedListItems.get(position);
        if(shoppingListItem.getProduct() == null) return;
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.PURCHASE_THEN_SHOPPING_LIST);
        bundle.putString(Constants.ARGUMENT.PRODUCT_NAME, shoppingListItem.getProduct().getName());
        bundle.putString(Constants.ARGUMENT.AMOUNT, String.valueOf(shoppingListItem.getAmount()));
        bundle.putParcelable(Constants.ARGUMENT.SHOPPING_LIST_ITEM, shoppingListItem);
        activity.replaceFragment(Constants.UI.PURCHASE, bundle, true);
    }

    public void deleteItem(int position) {
        ShoppingListItem shoppingListItem = (ShoppingListItem) groupedListItems.get(position);
        request.delete(
                grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, shoppingListItem.getId()),
                response -> removeItemFromList(position),
                error -> {
                    showMessage(activity.getString(R.string.msg_error));
                    if(DEBUG) Log.i(TAG, "deleteItem: " + error);
                }
        );
    }

    private void removeItemFromList(int position) {
        ShoppingListHelper.removeItemFromList(shoppingListItemAdapter, groupedListItems, position);
    }

    public void addItem() {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.CREATE);
        bundle.putInt(Constants.ARGUMENT.SHOPPING_LIST_ID, selectedShoppingListId);
        activity.replaceFragment(Constants.UI.SHOPPING_LIST_ITEM_EDIT, bundle, true);
    }

    private void showNotesEditor() {
        // TODO: Block clicks if no connection
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
        activity.showBottomSheet(new TextEditBottomSheetDialogFragment(), bundle);
    }

    public void setUpBottomMenu() {
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
                startActivityForResult(
                        new Intent(activity, ShoppingActivity.class),
                        Constants.REQUEST.SHOPPING_MODE
                );
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
                        Log.e(TAG, "setUpBottomMenu: add missing: " + e);
                    }
                    request.post(
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
                                showMessage(activity.getString(R.string.msg_error));
                                Log.e(
                                        TAG, "setUpBottomMenu: add missing "
                                                + shoppingList.getName()
                                                + ": " + error
                                );
                            }
                    );
                } else {
                    showMessage(activity.getString(R.string.msg_error));
                }
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
                    showMessage(activity.getString(R.string.msg_error));
                    return true;
                }
                clearShoppingList(
                        shoppingList,
                        response -> {
                            showMessage(
                                    activity.getString(
                                            R.string.msg_shopping_list_cleared,
                                            shoppingList.getName()
                                    )
                            );
                            // reload now empty list
                            refresh();
                        });
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
                    showMessage(activity.getString(R.string.msg_error));
                    return true;
                }
                Bundle bundle = new Bundle();
                bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.EDIT);
                bundle.putParcelable(Constants.ARGUMENT.SHOPPING_LIST, shoppingList);
                activity.replaceFragment(Constants.UI.SHOPPING_LIST_EDIT, bundle, true);
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
                    showMessage(activity.getString(R.string.msg_error));
                    return true;
                }
                clearShoppingList(
                        shoppingList,
                        response -> {
                            deleteShoppingList(shoppingList);
                            tidyUpItems();
                        }
                );
                return true;
            });
        }
    }

    private void clearShoppingList(
            ShoppingList shoppingList,
            OnResponseListener responseListener
    ) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("list_id", selectedShoppingListId);
        } catch (JSONException e) {
            Log.e(TAG, "clearShoppingList: " + e);
        }
        request.post(
                grocyApi.clearShoppingList(),
                jsonObject,
                responseListener::onResponse,
                error -> {
                    showMessage(activity.getString(R.string.msg_error));
                    Log.e(
                            TAG, "clearShoppingList: "
                                    + shoppingList.getName()
                                    + ": " + error
                    );
                }
        );
    }

    private void deleteShoppingList(ShoppingList shoppingList) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("list_id", selectedShoppingListId);
        } catch (JSONException e) {
            Log.e(TAG, "deleteShoppingList: delete list: " + e);
        }

        request.delete(
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
                    showMessage(activity.getString(R.string.msg_error));
                    Log.e(
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
        if(isFeatureEnabled()) {
            for(ShoppingList shoppingList : shoppingLists) listIds.add(shoppingList.getId());
            if(listIds.isEmpty()) return;  // possible if download error happened
        } else {
            listIds.add(1);  // id of first and single shopping list
        }

        ShoppingListItemDao itemDao = database.shoppingListItemDao();
        for(ShoppingListItem listItem : shoppingListItems) {
            if(!listIds.contains(listItem.getShoppingListId())) {
                Log.i(TAG, "tidyUpItems: " + listItem);
                request.delete(
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
            HashMap<Integer, ShoppingListItem> serverItemHashMap
    ) {
        for(ShoppingListItem itemToSync : itemsToSync) {
            JSONObject body = new JSONObject();
            try {
                body.put("done", itemToSync.getDone());
            } catch (JSONException e) {
                Log.e(TAG, "syncItems: " + e);
            }
            request.put(
                    grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, itemToSync.getId()),
                    TAG,
                    body,
                    response -> {
                        ShoppingListItem serverItem = serverItemHashMap.get(itemToSync.getId());
                        if(serverItem != null) serverItem.setDone(itemToSync.getDone());
                    },
                    error -> {
                        request.cancelAll(TAG);
                        showMessage("Failed to sync items"); // TODO
                    },
                    () -> {
                        showMessage("Entries synced successfully");
                        new StoreOfflineDataShoppingListHelper(
                                AppDatabase.getAppDatabase(activity.getApplicationContext()),
                                this,
                                false,
                                shoppingLists,
                                shoppingListItems,
                                productGroups,
                                quantityUnits,
                                products,
                                usedProductIds
                        ).execute();
                    }
            );
        }
    }

    @Override
    public void storedDataSuccessfully(ArrayList<ShoppingListItem> shoppingListItems) {
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
            showNotesEditor();
        }
    }

    public void updateConnectivity(boolean online) {
        showOffline = online;
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
        if(!isFeatureEnabled()) {
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
            activity.showBottomSheet(new ShoppingListItemBottomSheetDialogFragment(), bundle);
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

        activity.setUI(Constants.UI.SHOPPING_LIST_SEARCH);
    }

    public void dismissSearch() {
        appBarBehavior.switchToPrimary();
        activity.hideKeyboard();
        search = "";
        filterItems(itemsToDisplay);

        activity.setUI(Constants.UI.SHOPPING_LIST_DEFAULT);
    }

    private void showMessage(String msg) {
        activity.showMessage(
                Snackbar.make(activity.binding.frameMainContainer, msg, Snackbar.LENGTH_SHORT)
        );
    }

    private boolean isFeatureEnabled() {
        return sharedPrefs.getBoolean(Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS, true);
    }

    public interface OnResponseListener {
        void onResponse(JSONObject response);
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}