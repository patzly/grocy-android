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

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.LinearLayout;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.adapter.ShoppingListItemAdapter;
import xyz.zedler.patrick.grocy.adapter.StockPlaceholderAdapter;
import xyz.zedler.patrick.grocy.animator.ItemAnimator;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.behavior.SwipeBehavior;
import xyz.zedler.patrick.grocy.dao.ProductGroupDao;
import xyz.zedler.patrick.grocy.dao.QuantityUnitDao;
import xyz.zedler.patrick.grocy.dao.ShoppingListDao;
import xyz.zedler.patrick.grocy.dao.ShoppingListItemDao;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.databinding.FragmentShoppingListBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListItemBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.TextEditBottomSheetDialogFragment;
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
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.view.FilterChip;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class ShoppingListFragment extends Fragment
        implements ShoppingListItemAdapter.ShoppingListItemAdapterListener, AsyncResponse {

    private final static String TAG = Constants.UI.SHOPPING_LIST;
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private AppBarBehavior appBarBehavior;
    private WebRequest request;
    private ShoppingListItemAdapter shoppingListItemAdapter;
    private ClickUtil clickUtil = new ClickUtil();
    private AnimUtil animUtil = new AnimUtil();
    private FragmentShoppingListBinding binding;
    private SwipeBehavior swipeBehavior;

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

    private int selectedShoppingListId;
    private String startupShoppingListName;
    private String itemsToDisplay;
    private String search;
    private String errorState;
    private boolean isDataStored;
    private boolean showOffline;
    private boolean isRestoredInstance;

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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (MainActivity) getActivity();
        assert activity != null;

        showOffline = false;

        // GET PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        // WEB REQUESTS

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

        itemsToDisplay = Constants.SHOPPING_LIST.FILTER.ALL;
        selectedShoppingListId = 1;
        search = "";
        showOffline = false;
        errorState = Constants.STATE.NONE;
        isRestoredInstance = false;

        // INITIALIZE VIEWS

        binding.frameShoppingListBack.setOnClickListener(v -> activity.onBackPressed());

        // top app bar
        binding.textShoppingListTitle.setOnClickListener(v -> showShoppingListsBottomSheet());
        binding.buttonShoppingListLists.setOnClickListener(v -> showShoppingListsBottomSheet());

        binding.linearShoppingListBottomNotesClick.setOnClickListener(v -> showNotesEditor());

        // search
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
                (getArguments() == null
                        || getArguments().getBoolean(Constants.ARGUMENT.ANIMATED, true))
                        && savedInstanceState == null,
                TAG
        );
        setArguments(null);
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
            outState.putParcelableArrayList("displayedItems", displayedItems);
            outState.putParcelableArrayList("quantityUnits", quantityUnits);
            outState.putParcelableArrayList("products", products);
            outState.putParcelableArrayList("productGroups", productGroups);
            //outState.putParcelableArrayList("groupedListItems", groupedListItems);

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
        displayedItems = savedInstanceState.getParcelableArrayList("displayedItems");
        quantityUnits = savedInstanceState.getParcelableArrayList("quantityUnits");
        products = savedInstanceState.getParcelableArrayList("products");
        productGroups = savedInstanceState.getParcelableArrayList("productGroups");
        //groupedListItems = savedInstanceState.getParcelableArrayList("groupedListItems");

        appBarBehavior.restoreInstanceState(savedInstanceState);
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
        if(!hidden) onActivityCreated(null);
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
            new LoadOfflineData(activity, this).execute();
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
                new LoadOfflineData(activity, this).execute();
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
                setEmptyState(Constants.STATE.NONE);
                break;
            case Constants.STATE.ERROR:
                binding.linearError.imageError.setImageResource(R.drawable.illustration_popsicle);
                binding.linearError.textErrorTitle.setText(R.string.error_unknown);
                binding.linearError.textErrorSubtitle.setText(R.string.error_unknown_subtitle);
                setEmptyState(Constants.STATE.NONE);
                break;
            case Constants.STATE.NONE:
                viewIn = binding.scrollShoppingList;
                viewOut = binding.linearError.linearError;
                break;
        }

        animUtil.replaceViews(viewIn, viewOut, animated);
    }

    private void setEmptyState(String state) {
        LinearLayout container = binding.linearEmpty.linearEmpty;
        new Handler().postDelayed(() -> {
            switch (state) {
                case Constants.STATE.EMPTY:
                    binding.linearEmpty.imageEmpty.setImageResource(R.drawable.illustration_toast);
                    binding.linearEmpty.textEmptyTitle.setText(R.string.error_empty_shopping_list);
                    binding.linearEmpty.textEmptySubtitle.setText(
                            R.string.error_empty_shopping_list_sub
                    );
                    break;
                case Constants.STATE.NO_SEARCH_RESULTS:
                    binding.linearEmpty.imageEmpty.setImageResource(R.drawable.illustration_jar);
                    binding.linearEmpty.textEmptyTitle.setText(R.string.error_search);
                    binding.linearEmpty.textEmptySubtitle.setText(R.string.error_search_sub);
                    break;
                case Constants.STATE.NO_FILTER_RESULTS:
                    binding.linearEmpty.imageEmpty.setImageResource(R.drawable.illustration_coffee);
                    binding.linearEmpty.textEmptyTitle.setText(R.string.error_filter);
                    binding.linearEmpty.textEmptySubtitle.setText(R.string.error_filter_sub);
                    break;
                case Constants.STATE.NONE:
                    if(container.getVisibility() == View.GONE) return;
                    break;
            }
        }, 125);
        // show new empty state with delay or hide it if NONE
        if(state.equals(Constants.STATE.NONE)) {
            container.animate().alpha(0).setDuration(125).withEndAction(
                    () -> container.setVisibility(View.GONE)
            ).start();
        } else {
            if(container.getVisibility() == View.VISIBLE) {
                // first hide previous empty state if needed
                container.animate().alpha(0).setDuration(125).start();
            }
            new Handler().postDelayed(() -> {
                container.setAlpha(0);
                container.setVisibility(View.VISIBLE);
                container.animate().alpha(1).setDuration(125).start();
            }, 150);
        }
    }

    private void download() {
        binding.swipeShoppingList.setRefreshing(true);
        binding.linearShoppingListBottomNotes.animate().alpha(0).withEndAction(
                () -> binding.linearShoppingListBottomNotes.setVisibility(View.GONE)
        ).setDuration(150).start();
        downloadQuantityUnits();
        downloadProducts();
        downloadProductGroups();
        downloadShoppingListItems();
        downloadVolatile();
        downloadShoppingLists();
    }

    private void downloadQuantityUnits() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS),
                TAG,
                response -> {
                    quantityUnits = gson.fromJson(
                            response,
                            new TypeToken<List<QuantityUnit>>(){}.getType()
                    );
                    if(DEBUG) Log.i(
                            TAG, "downloadQuantityUnits: quantityUnits = " + quantityUnits
                    );
                },
                this::onDownloadError,
                this::onQueueEmpty
        );
    }

    private void downloadProducts() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
                TAG,
                response -> {
                    products = gson.fromJson(
                            response,
                            new TypeToken<List<Product>>(){}.getType()
                    );
                    if(DEBUG) Log.i(TAG, "downloadProducts: products = " + products);
                },
                this::onDownloadError,
                this::onQueueEmpty
        );
    }

    private void downloadProductGroups() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_GROUPS),
                TAG,
                response -> {
                    productGroups = gson.fromJson(
                            response,
                            new TypeToken<List<ProductGroup>>(){}.getType()
                    );
                    if(DEBUG) Log.i(
                            TAG, "downloadProductGroups: productGroups = " + productGroups
                    );
                },
                this::onDownloadError,
                this::onQueueEmpty
        );
    }

    private void downloadShoppingListItems() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LIST),
                TAG,
                response -> {
                    shoppingListItems = gson.fromJson(
                            response,
                            new TypeToken<List<ShoppingListItem>>(){}.getType()
                    );
                    if(DEBUG) Log.i(
                            TAG,
                            "downloadShoppingList: shoppingListItems = " + shoppingListItems
                    );
                },
                this::onDownloadError,
                this::onQueueEmpty
        );
    }

    private void downloadVolatile() {
        request.get(
                grocyApi.getStockVolatile(),
                TAG,
                response -> {
                    if(DEBUG) Log.i(TAG, "downloadVolatile: success");
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        // Parse third part of volatile array: missing products
                        missingItems = gson.fromJson(
                                jsonObject.getJSONArray("missing_products").toString(),
                                new TypeToken<List<MissingItem>>(){}.getType()
                        );
                        if(DEBUG) Log.i(TAG, "downloadVolatile: missing = " + missingItems);

                    } catch (JSONException e) {
                        Log.e(TAG, "downloadVolatile: " + e);
                    }
                },
                this::onDownloadError,
                this::onQueueEmpty
        );
    }

    private void downloadShoppingLists() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LISTS),
                TAG,
                response -> {
                    shoppingLists = gson.fromJson(
                            response,
                            new TypeToken<List<ShoppingList>>(){}.getType()
                    );
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
                    if(DEBUG) Log.i(
                            TAG,
                            "downloadShoppingLists: shoppingLists = " + shoppingLists
                    );
                },
                this::onDownloadError,
                this::onQueueEmpty
        );
    }

    private void onQueueEmpty() {
        if(showOffline) {
            showOffline = false;
            updateUI();
        }
        ArrayList<String> missingProductIds = new ArrayList<>();
        for(MissingItem missingItem : missingItems) {
            missingProductIds.add(String.valueOf(missingItem.getId()));
        }
        missingShoppingListItems = new ArrayList<>();
        undoneShoppingListItems = new ArrayList<>();
        shoppingListItemsSelected = new ArrayList<>();
        ArrayList<Integer> shoppingListProductIds = new ArrayList<>();
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
            if(shoppingListItem.getProductId() != null) {
                shoppingListProductIds.add(Integer.parseInt(shoppingListItem.getProductId()));
            }
        }
        chipMissing.setText(
                activity.getString(R.string.msg_missing_products, missingShoppingListItems.size())
        );
        chipUndone.setText(
                activity.getString(R.string.msg_undone_items, undoneShoppingListItems.size())
        );

        if(!isDataStored) {
            // sync modified data and store new data
            new StoreOfflineData(
                    activity,
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

            for(Product product : products) {
                if(!shoppingListProductIds.contains(product.getId())) continue;
                for(ShoppingListItem shoppingListItem : shoppingListItemsSelected) {
                    if(shoppingListItem.getProductId() == null) continue;
                    if(String.valueOf(product.getId()).equals(shoppingListItem.getProductId())) {
                        shoppingListItem.setProduct(product);
                    }
                }
            }

            ShoppingList shoppingList = getShoppingList(selectedShoppingListId);
            Spanned notes = shoppingList != null && shoppingList.getNotes() != null
                    ? (Spanned) TextUtil
                      .trimCharSequence(Html.fromHtml(shoppingList.getNotes().trim()))
                    : null;
            if(shoppingList != null && notes != null && !notes.toString().trim().isEmpty()) {
                binding.linearShoppingListBottomNotes.animate().alpha(0).withEndAction(() -> {
                    binding.textShoppingListBottomNotes.setText(notes);
                    binding.linearShoppingListBottomNotes.setVisibility(View.VISIBLE);
                    binding.linearShoppingListBottomNotes.animate()
                            .alpha(1)
                            .setDuration(150)
                            .start();
                }).setDuration(150).start();
            } else {
                binding.linearShoppingListBottomNotes.animate().alpha(0).withEndAction(
                        () -> binding.linearShoppingListBottomNotes.setVisibility(View.GONE)
                ).setDuration(150).start();
                binding.textShoppingListBottomNotes.setText(null);
            }
            binding.swipeShoppingList.setRefreshing(false);
            filterItems(itemsToDisplay);
        }
    }

    private void onDownloadError(VolleyError error) {
        request.cancelAll(TAG);
        binding.swipeShoppingList.setRefreshing(false);
        if(!showOffline) {
            showOffline = true;
            updateUI();
        }
        new LoadOfflineData(activity, this).execute();
    }

    private static class LoadOfflineData extends AsyncTask<Void, Void, String> {
        private WeakReference<Activity> weakActivity;
        private AsyncResponse response;
        private ArrayList<ShoppingListItem> shoppingListItems;
        private ArrayList<ShoppingList> shoppingLists;
        private ArrayList<ProductGroup> productGroups;
        private ArrayList<QuantityUnit> quantityUnits;

        public LoadOfflineData(Activity activity, AsyncResponse response) {
            weakActivity = new WeakReference<>(activity);
            this.response = response;
        }

        @Override
        protected String doInBackground(Void... voids) {
            MainActivity activity = (MainActivity) weakActivity.get();
            AppDatabase database = activity.getDatabase();
            shoppingListItems = new ArrayList<>(database.shoppingListItemDao().getAll());
            shoppingLists = new ArrayList<>(database.shoppingListDao().getAll());
            productGroups = new ArrayList<>(database.productGroupDao().getAll());
            quantityUnits = new ArrayList<>(database.quantityUnitDao().getAll());
            return null;
        }

        @Override
        protected void onPostExecute(String arg) {
            response.prepareOfflineData(
                    shoppingListItems,
                    shoppingLists,
                    productGroups,
                    quantityUnits
            );
        }
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
                String name = shoppingListItem.getProduct().getName();
                String description = shoppingListItem.getProduct().getDescription();
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
        ArrayList<ProductGroup> neededProductGroups = new ArrayList<>();
        boolean containsUngroupedItems = false;
        for(ShoppingListItem shoppingListItem : displayedItems) {
            Product product = shoppingListItem.getProduct();
            String groupId = null;
            if(product != null) {
                groupId = shoppingListItem.getProduct().getProductGroupId();
            }
            if(groupId != null && !groupId.isEmpty()) {
                for(ProductGroup productGroup : productGroups) {
                    if(productGroup.getId() == Integer.parseInt(groupId)
                            && !neededProductGroups.contains(productGroup)
                    ) {
                        neededProductGroups.add(productGroup);
                        break;
                    }
                }
            } else if(!containsUngroupedItems) {
                containsUngroupedItems = true;
            }
        }
        SortUtil.sortProductGroupsByName(neededProductGroups, true);
        if(containsUngroupedItems) {
            neededProductGroups.add(new ProductGroup(
                    -1,
                    activity.getString(R.string.title_shopping_list_ungrouped)
            ));
        }
        groupedListItems = new ArrayList<>();
        for(ProductGroup productGroup : neededProductGroups) {
            groupedListItems.add(productGroup);
            ArrayList<ShoppingListItem> itemsOneGroup = new ArrayList<>();
            for(ShoppingListItem shoppingListItem : displayedItems) {
                Product product = shoppingListItem.getProduct();
                String groupId = null;
                if(product != null) {
                    groupId = product.getProductGroupId();
                }
                if(groupId == null || groupId.isEmpty()) groupId = "-1";
                if(groupId.equals(String.valueOf(productGroup.getId()))) {
                    itemsOneGroup.add(shoppingListItem);
                }
            }
            SortUtil.sortShoppingListItemsByName(itemsOneGroup, true);
            groupedListItems.addAll(itemsOneGroup);
        }
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
        binding.textShoppingListTitle.animate().alpha(0).withEndAction(() -> {
            binding.textShoppingListTitle.setText(shoppingList.getName());
            binding.textShoppingListTitle.animate().alpha(1).setDuration(150).start();
        }).setDuration(150).start();
        binding.buttonShoppingListLists.animate().alpha(0).withEndAction(
                () -> binding.buttonShoppingListLists.animate().alpha(1).setDuration(150).start()
        ).setDuration(150).start();
        chipMissing.changeState(false);
        chipUndone.changeState(false);
        itemsToDisplay = Constants.SHOPPING_LIST.FILTER.ALL;
        if(showOffline) {
            new LoadOfflineData(activity, this).execute();
        } else {
            onQueueEmpty();
        }
        setUpBottomMenu(); // to hide delete action if necessary
    }

    public void selectShoppingList(String shoppingListName) {
        startupShoppingListName = shoppingListName;
    }

    private ShoppingList getShoppingList(int shoppingListId) {
        for(ShoppingList shoppingList : shoppingLists) {
            if(shoppingList.getId() == shoppingListId) {
                return shoppingList;
            }
        }
        return null;
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
        request.put(
                grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, shoppingListItem.getId()),
                body,
                response -> updateDoneStatus(shoppingListItem, position),
                error -> {
                    showMessage(activity.getString(R.string.msg_error));
                    if(DEBUG) Log.i(TAG, "toggleDoneStatus: " + error);
                }
        );
    }

    private void updateDoneStatus(ShoppingListItem shoppingListItem, int position) {
        new Thread(() -> activity.getDatabase().shoppingListItemDao().update(shoppingListItem))
                .start();
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
        if(position-1 >= 0
                && groupedListItems.get(position-1).getType()
                == GroupedListItem.TYPE_HEADER
                && groupedListItems.size() > position+1
                && groupedListItems.get(position+1).getType()
                == GroupedListItem.TYPE_HEADER
        ) {
            groupedListItems.remove(position);
            shoppingListItemAdapter.notifyItemRemoved(position);
            groupedListItems.remove(position - 1);
            shoppingListItemAdapter.notifyItemRemoved(position - 1);
        } else if(position-1 >= 0
                && groupedListItems.get(position-1).getType()
                == GroupedListItem.TYPE_HEADER
                && groupedListItems.size() == position+1
        ) {
            groupedListItems.remove(position);
            shoppingListItemAdapter.notifyItemRemoved(position);
            groupedListItems.remove(position - 1);
            shoppingListItemAdapter.notifyItemRemoved(position - 1);
        } else {
            groupedListItems.remove(position);
            shoppingListItemAdapter.notifyItemRemoved(position);
        }
    }


    public void addItem() {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.CREATE);
        bundle.putInt(Constants.ARGUMENT.SHOPPING_LIST_ID, selectedShoppingListId);
        activity.replaceFragment(Constants.UI.SHOPPING_LIST_ITEM_EDIT, bundle, true);
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
        activity.showBottomSheet(new TextEditBottomSheetDialogFragment(), bundle);
    }

    private void changeAppBarTitle() {
        // change app bar title to shopping list name
        ShoppingList shoppingList = getShoppingList(selectedShoppingListId);
        if(shoppingList != null && !binding.textShoppingListTitle.getText().toString().equals(
                shoppingList.getName())
        ) {
            binding.textShoppingListTitle.animate().alpha(0).withEndAction(() -> {
                binding.textShoppingListTitle.setText(shoppingList.getName());
                binding.textShoppingListTitle.animate().alpha(1).setDuration(150).start();
            }).setDuration(150).start();
            binding.buttonShoppingListLists.animate().alpha(0).withEndAction(
                    () -> binding.buttonShoppingListLists.animate()
                            .alpha(1)
                            .setDuration(150)
                            .start()
            ).setDuration(150).start();
        }
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
        ArrayList<Integer> shoppingListIds = new ArrayList<>();
        if(isFeatureEnabled()) {
            for(ShoppingList shoppingList1 : shoppingLists) {
                shoppingListIds.add(shoppingList1.getId());
            }
        } else {
            shoppingListIds.add(1);  // id of first and single shopping list
        }

        if(shoppingListIds.isEmpty()) return;
        for(ShoppingListItem shoppingListItem : shoppingListItems) {
            if(!shoppingListIds.contains(shoppingListItem.getShoppingListId())) {
                request.delete(
                        grocyApi.getObject(
                                GrocyApi.ENTITY.SHOPPING_LIST,
                                shoppingListItem.getId()
                        ),
                        response -> new Thread( // delete items in database too
                                () -> activity.getDatabase()
                                .shoppingListItemDao()
                                .delete(shoppingListItem)
                        ).start(),
                        error -> {}
                );
            }
        }
    }

    private static class StoreOfflineData extends AsyncTask<Void, Void, String> {
        private WeakReference<Activity> weakActivity;
        private AsyncResponse response;
        private boolean syncIfNecessary;
        private ArrayList<ShoppingListItem> shoppingListItems;
        private ArrayList<ShoppingList> shoppingLists;
        private ArrayList<ProductGroup> productGroups;
        private ArrayList<QuantityUnit> quantityUnits;
        private ArrayList<Product> products;
        private ArrayList<Integer> usedProductIds;
        private ArrayList<ShoppingListItem> itemsToSync = new ArrayList<>();

        public StoreOfflineData(
                Activity activity,
                AsyncResponse response,
                boolean syncIfNecessary,
                ArrayList<ShoppingList> shoppingLists,
                ArrayList<ShoppingListItem> shoppingListItems,
                ArrayList<ProductGroup> productGroups,
                ArrayList<QuantityUnit> quantityUnits,
                ArrayList<Product> products,
                ArrayList<Integer> usedProductIds
        ) {
            weakActivity = new WeakReference<>(activity);
            this.response = response;
            this.syncIfNecessary = syncIfNecessary;
            this.shoppingLists = shoppingLists;
            this.shoppingListItems = new ArrayList<>(shoppingListItems);
            this.productGroups = productGroups;
            this.quantityUnits = quantityUnits;
            this.products = products;
            this.usedProductIds = usedProductIds;
        }

        @Override
        protected String doInBackground(Void... voids) {
            MainActivity activity = (MainActivity) weakActivity.get();
            AppDatabase database = activity.getDatabase();

            if(syncIfNecessary) {
                ArrayList<Integer> newItemIds = new ArrayList<>();
                for(ShoppingListItem shoppingListItem : shoppingListItems) {
                    newItemIds.add(shoppingListItem.getId());
                }

                // compare new with old items and add modified to separate list
                ArrayList<ShoppingListItem> oldItems = new ArrayList<>(
                        database.shoppingListItemDao().getAll()
                );
                for(ShoppingListItem oldItem : oldItems) {
                    if(!newItemIds.contains(oldItem.getId())) {
                        // item already deleted on server
                        continue;
                    }
                    ShoppingListItem newItem = null;
                    for(ShoppingListItem newItemTmp : shoppingListItems) {
                        if(newItemTmp.getId() == oldItem.getId()) {
                            newItem = newItemTmp;
                        }
                    }
                    if(newItem != null && oldItem.getDoneSynced() != -1
                            && oldItem.getDone() != oldItem.getDoneSynced()
                            && oldItem.getDone() != newItem.getDone()
                    ) {
                        itemsToSync.add(oldItem);
                    }
                }
                if(!itemsToSync.isEmpty()) return null; // don't overwrite items yet
            }

            // shopping list items
            for(Product product : products) {  // fill with products
                if(!usedProductIds.contains(product.getId())) continue;
                for(ShoppingListItem shoppingListItem : shoppingListItems) {
                    if(shoppingListItem.getProductId() == null) continue;
                    if(String.valueOf(product.getId()).equals(shoppingListItem.getProductId())) {
                        shoppingListItem.setProduct(product);
                    }
                }
            }
            ShoppingListItemDao shoppingListItemDao = database.shoppingListItemDao();
            shoppingListItemDao.deleteAll();
            shoppingListItemDao.insertAll(shoppingListItems);

            // shopping lists
            ShoppingListDao shoppingListDao = database.shoppingListDao();
            shoppingListDao.deleteAll();
            shoppingListDao.insertAll(shoppingLists);

            // product groups
            ProductGroupDao productGroupDao = database.productGroupDao();
            productGroupDao.deleteAll();
            productGroupDao.insertAll(productGroups);

            // quantity units
            QuantityUnitDao quantityUnitDao = database.quantityUnitDao();
            quantityUnitDao.deleteAll();
            quantityUnitDao.insertAll(quantityUnits);

            return null;
        }

        @Override
        protected void onPostExecute(String arg) {
            if(itemsToSync.isEmpty()) {
                response.storedDataSuccessfully(shoppingListItems);
            } else {
                response.syncItems(
                        itemsToSync,
                        shoppingLists,
                        shoppingListItems,
                        productGroups,
                        quantityUnits,
                        products,
                        usedProductIds
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
            ArrayList<Integer> usedProductIds
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
                        for(ShoppingListItem shoppingListItemTmp : shoppingListItems) {
                            if(shoppingListItemTmp.getId() == itemToSync.getId()) {
                                shoppingListItemTmp.setDone(itemToSync.getDone());
                            }
                        }
                    },
                    error -> {
                        request.cancelAll(TAG);
                        showMessage("Failed to sync items"); // TODO
                    },
                    () -> {
                        showMessage("Entries synced successfully");
                        new StoreOfflineData(
                                activity,
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
        showItemBottomSheet(groupedListItems.get(position), position);
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
            appBarBehavior.replaceLayout(R.id.linear_shopping_list_app_bar_search, true);
            binding.editTextShoppingListSearch.setText("");
        }
        binding.textInputShoppingListSearch.requestFocus();
        activity.showKeyboard(binding.editTextShoppingListSearch);

        activity.setUI(Constants.UI.SHOPPING_LIST_SEARCH);
    }

    public void dismissSearch() {
        appBarBehavior.replaceLayout(R.id.linear_shopping_list_app_bar_default, true);
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

interface AsyncResponse {
    void prepareOfflineData(
            ArrayList<ShoppingListItem> shoppingListItems,
            ArrayList<ShoppingList> shoppingLists,
            ArrayList<ProductGroup> productGroups,
            ArrayList<QuantityUnit> quantityUnits
    );

    void syncItems(
            ArrayList<ShoppingListItem> itemsToSync,
            ArrayList<ShoppingList> shoppingLists,
            ArrayList<ShoppingListItem> shoppingListItems,
            ArrayList<ProductGroup> productGroups,
            ArrayList<QuantityUnit> quantityUnits,
            ArrayList<Product> products,
            ArrayList<Integer> usedProductIds
    );

    void storedDataSuccessfully(ArrayList<ShoppingListItem> shoppingListItems);
}