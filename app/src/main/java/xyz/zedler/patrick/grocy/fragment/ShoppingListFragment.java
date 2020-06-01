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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

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
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.view.ActionButton;
import xyz.zedler.patrick.grocy.view.FilterChip;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class ShoppingListFragment extends Fragment
        implements ShoppingListItemAdapter.ShoppingListItemAdapterListener {

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

    private ArrayList<ShoppingList> shoppingLists = new ArrayList<>();
    private ArrayList<ShoppingListItem> shoppingListItems = new ArrayList<>();
    private ArrayList<ShoppingListItem> shoppingListItemsSelected = new ArrayList<>();
    private ArrayList<MissingItem> missingItems = new ArrayList<>();
    private ArrayList<ShoppingListItem> missingShoppingListItems = new ArrayList<>();
    private ArrayList<ShoppingListItem> undoneShoppingListItems = new ArrayList<>();
    private ArrayList<ShoppingListItem> filteredItems = new ArrayList<>();
    private ArrayList<ShoppingListItem> displayedItems = new ArrayList<>();
    private ArrayList<QuantityUnit> quantityUnits = new ArrayList<>();
    private ArrayList<Product> products = new ArrayList<>();
    private ArrayList<ProductGroup> productGroups = new ArrayList<>();
    private ArrayList<GroupedListItem> groupedListItems = new ArrayList<>();

    private int selectedShoppingListId = 1;
    private String startupShoppingListName;
    private String itemsToDisplay = Constants.SHOPPING_LIST.FILTER.ALL;
    private String search = "";

    private RecyclerView recyclerView;
    private SwipeBehavior swipeBehavior;
    private FilterChip chipUndone, chipMissing;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextInputLayout textInputLayoutSearch;
    private EditText editTextSearch;
    private LinearLayout linearLayoutBottomNotes;
    private NestedScrollView scrollView;
    private TextView textViewBottomNotes, textViewTitle;
    private ActionButton buttonLists;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_shopping_list, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (MainActivity) getActivity();
        assert activity != null;

        // GET PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        // WEB REQUESTS

        request = new WebRequest(activity.getRequestQueue());
        grocyApi = activity.getGrocy();

        // INITIALIZE VIEWS

        activity.findViewById(R.id.frame_shopping_list_back).setOnClickListener(
                v -> activity.onBackPressed()
        );

        // top app bar
        textViewTitle = activity.findViewById(R.id.text_shopping_list_title);
        textViewTitle.setOnClickListener(
                v -> showShoppingListsBottomSheet()
        );
        buttonLists = activity.findViewById(R.id.button_shopping_list_lists);
        buttonLists.setOnClickListener(
                v -> showShoppingListsBottomSheet()
        );

        linearLayoutBottomNotes = activity.findViewById(R.id.linear_shopping_list_bottom_notes);
        activity.findViewById(R.id.linear_shopping_list_bottom_notes_click).setOnClickListener(
                v -> showNotesEditor()
        );
        textViewBottomNotes = activity.findViewById(R.id.text_shopping_list_bottom_notes);
        swipeRefreshLayout = activity.findViewById(R.id.swipe_shopping_list);
        scrollView = activity.findViewById(R.id.scroll_shopping_list);
        // retry button on offline error page
        activity.findViewById(R.id.button_error_retry).setOnClickListener(v -> refresh());
        recyclerView = activity.findViewById(R.id.recycler_shopping_list);

        // search
        activity.findViewById(R.id.frame_shopping_list_search_close).setOnClickListener(
                v -> dismissSearch()
        );
        textInputLayoutSearch = activity.findViewById(R.id.text_input_shopping_list_search);
        editTextSearch = textInputLayoutSearch.getEditText();
        assert editTextSearch != null;
        editTextSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                search = s.toString();
            }
        });
        editTextSearch.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchItems(editTextSearch.getText().toString());
                activity.hideKeyboard();
                return true;
            } return false;
        });

        // APP BAR BEHAVIOR

        appBarBehavior = new AppBarBehavior(activity, R.id.linear_shopping_list_app_bar_default);

        // SWIPE REFRESH

        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

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
        LinearLayout chipContainer = activity.findViewById(
                R.id.linear_shopping_list_filter_container_top
        );
        chipContainer.addView(chipMissing);
        chipContainer.addView(chipUndone);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        activity,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
        recyclerView.setItemAnimator(new ItemAnimator());
        recyclerView.setAdapter(new StockPlaceholderAdapter());

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
                    ) return;

                    underlayButtons.add(new UnderlayButton(
                            R.drawable.ic_round_local_grocery_store,
                            position -> purchaseItem(position)
                    ));
                }
            }
        };
        swipeBehavior.attachToRecyclerView(recyclerView);


        load();

        // UPDATE UI

        activity.updateUI(
                Constants.UI.SHOPPING_LIST_DEFAULT,
                getArguments() == null || getArguments().getBoolean(
                        Constants.ARGUMENT.ANIMATED, true
                ),
                TAG
        );
        if(getArguments() != null) {
            getArguments().putBoolean(Constants.ARGUMENT.ANIMATED, true);
        }
    }

    private void load() {
        if(activity.isOnline()) {
            download();
        } else {
            setError(true, true, false);
        }
    }

    public void refresh() {
        if(activity.isOnline()) {
            setError(false, false, true);
            download();
        } else {
            swipeRefreshLayout.setRefreshing(false);
            activity.showMessage(
                    Snackbar.make(
                            activity.findViewById(R.id.linear_container_main),
                            activity.getString(R.string.msg_no_connection),
                            Snackbar.LENGTH_SHORT
                    ).setActionTextColor(
                            ContextCompat.getColor(activity, R.color.secondary)
                    ).setAction(
                            activity.getString(R.string.action_retry),
                            v1 -> refresh()
                    )
            );
        }
    }

    private void setError(boolean isError, boolean isOffline, boolean animated) {
        LinearLayout linearLayoutError = activity.findViewById(R.id.linear_error);
        ImageView imageViewError = activity.findViewById(R.id.image_error);
        TextView textViewTitle = activity.findViewById(R.id.text_error_title);
        TextView textViewSubtitle = activity.findViewById(R.id.text_error_subtitle);

        if(isError) {
            imageViewError.setImageResource(
                    isOffline
                            ? R.drawable.illustration_broccoli
                            : R.drawable.illustration_popsicle
            );
            textViewTitle.setText(isOffline ? R.string.error_offline : R.string.error_unknown);
            textViewSubtitle.setText(
                    isOffline
                            ? R.string.error_offline_subtitle
                            : R.string.error_unknown_subtitle
            );
        }

        if(animated) {
            View viewOut = isError ? scrollView : linearLayoutError;
            View viewIn = isError ? linearLayoutError : scrollView;
            if(viewOut.getVisibility() == View.VISIBLE && viewIn.getVisibility() == View.GONE) {
                viewOut.animate().alpha(0).setDuration(150).withEndAction(() -> {
                    viewIn.setAlpha(0);
                    viewOut.setVisibility(View.GONE);
                    viewIn.setVisibility(View.VISIBLE);
                    viewIn.animate().alpha(1).setDuration(150).start();
                }).start();
            }
        } else {
            scrollView.setVisibility(isError ? View.GONE : View.VISIBLE);
            linearLayoutError.setVisibility(isError ? View.VISIBLE : View.GONE);
        }
    }

    private void download() {
        swipeRefreshLayout.setRefreshing(true);
        linearLayoutBottomNotes.animate().alpha(0).withEndAction(() ->
                linearLayoutBottomNotes.setVisibility(View.GONE)).setDuration(150).start();
        downloadQuantityUnits();
        downloadProducts();
        downloadProductGroups();
        downloadShoppingList();
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

    private void downloadShoppingList() {
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
                        e.printStackTrace();
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

                    // change app bar title to shopping list name
                    ShoppingList shoppingList = getShoppingList(selectedShoppingListId);
                    if(shoppingList != null && !textViewTitle.getText().toString().equals(
                            shoppingList.getName())
                    ) {
                        textViewTitle.animate().alpha(0).withEndAction(() -> {
                            textViewTitle.setText(shoppingList.getName());
                            textViewTitle.animate().alpha(1).setDuration(150).start();
                        }).setDuration(150).start();
                        buttonLists.animate().alpha(0).withEndAction(
                                () -> buttonLists.animate().alpha(1).setDuration(150).start()
                        ).setDuration(150).start();
                    }
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
        ArrayList<String> missingProductIds = new ArrayList<>();
        for(MissingItem missingItem : missingItems) {
            missingProductIds.add(String.valueOf(missingItem.getId()));
        }
        missingShoppingListItems = new ArrayList<>();
        undoneShoppingListItems = new ArrayList<>();
        shoppingListItemsSelected = new ArrayList<>();
        ArrayList<String> shoppingListProductIds = new ArrayList<>();
        for(ShoppingListItem shoppingListItem : shoppingListItems) {
            if(shoppingListItem.getShoppingListId() != selectedShoppingListId) continue;
            shoppingListItemsSelected.add(shoppingListItem);
            if(missingProductIds.contains(shoppingListItem.getProductId())) {
                shoppingListItem.setIsMissing(true);
                missingShoppingListItems.add(shoppingListItem);
            }
            if(shoppingListItem.isUndone()) {
                undoneShoppingListItems.add(shoppingListItem);
            }
            shoppingListProductIds.add(shoppingListItem.getProductId());
        }
        chipMissing.setText(
                activity.getString(R.string.msg_missing_products, missingShoppingListItems.size())
        );
        chipUndone.setText(
                activity.getString(R.string.msg_undone_items, undoneShoppingListItems.size())
        );
        for(Product product : products) {
            if(shoppingListProductIds.contains(String.valueOf(product.getId()))) {
                for(ShoppingListItem shoppingListItem : shoppingListItemsSelected) {
                    if(shoppingListItem.getProductId() == null) continue;
                    if(String.valueOf(product.getId()).equals(shoppingListItem.getProductId())) {
                        shoppingListItem.setProduct(product);
                    }
                }
            }
        }
        ShoppingList shoppingList = getShoppingList(selectedShoppingListId);
        Spanned notes = shoppingList != null && shoppingList.getNotes() != null
                ? (Spanned) TextUtil.trimCharSequence(Html.fromHtml(shoppingList.getNotes().trim()))
                : null;
        if(shoppingList != null && notes != null && !notes.toString().trim().isEmpty()) {
            linearLayoutBottomNotes.animate().alpha(0).withEndAction(() -> {
                textViewBottomNotes.setText(notes);
                linearLayoutBottomNotes.setVisibility(View.VISIBLE);
                linearLayoutBottomNotes.animate().alpha(1).setDuration(150).start();
            }).setDuration(150).start();
        } else {
            linearLayoutBottomNotes.animate().alpha(0).withEndAction(() ->
                    linearLayoutBottomNotes.setVisibility(View.GONE)).setDuration(150).start();
            textViewBottomNotes.setText(null);
        }
        swipeRefreshLayout.setRefreshing(false);
        filterItems(itemsToDisplay);
    }

    private void onDownloadError(VolleyError error) {
        request.cancelAll(TAG);
        swipeRefreshLayout.setRefreshing(false);
        setError(true, false, true);
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
        } else {
            if(displayedItems != filteredItems) {
                displayedItems = filteredItems;
                groupItems();
            }
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
        recyclerView.animate().alpha(0).setDuration(150).withEndAction(() -> {
            recyclerView.setAdapter(adapter);
            recyclerView.animate().alpha(1).setDuration(150).start();
        }).start();
    }

    private void showShoppingListsBottomSheet() {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(Constants.ARGUMENT.SHOPPING_LISTS, shoppingLists);
        bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedShoppingListId);
        activity.showBottomSheet(new ShoppingListsBottomSheetDialogFragment(), bundle);
    }

    public void selectShoppingList(int shoppingListId) {
        if(shoppingListId == selectedShoppingListId) return;
        ShoppingList shoppingList = getShoppingList(shoppingListId);
        if(shoppingList == null) return;
        selectedShoppingListId = shoppingListId;
        textViewTitle.animate().alpha(0).withEndAction(() -> {
            textViewTitle.setText(shoppingList.getName());
            textViewTitle.animate().alpha(1).setDuration(150).start();
        }).setDuration(150).start();
        buttonLists.animate().alpha(0).withEndAction(() ->
                buttonLists.animate().alpha(1).setDuration(150).start()).setDuration(150).start();
        chipMissing.changeState(false);
        chipUndone.changeState(false);
        itemsToDisplay = Constants.SHOPPING_LIST.FILTER.ALL;
        onQueueEmpty();
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
        JSONObject body = new JSONObject();
        try {
            body.put("done", shoppingListItem.isUndone());
        } catch (JSONException e) {
            if(DEBUG) Log.e(TAG, "toggleDoneStatus: " + e);
        }
        request.put(
                grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, shoppingListItem.getId()),
                body,
                response -> {
                    shoppingListItem.setDone(shoppingListItem.isUndone());
                    if(!shoppingListItem.isUndone()) {
                        undoneShoppingListItems.remove(shoppingListItem);
                    } else {
                        undoneShoppingListItems = new ArrayList<>();
                        for(ShoppingListItem shoppingListItem1 : shoppingListItems) {
                            if(shoppingListItem1.getShoppingListId() != selectedShoppingListId) {
                                continue;
                            }
                            if(shoppingListItem1.isUndone()) {
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
                },
                error -> {
                    showMessage(activity.getString(R.string.msg_error));
                    if(DEBUG) Log.i(TAG, "toggleDoneStatus: " + error);
                }
        );
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
            if(DEBUG) Log.e(TAG, "saveNotes: " + e);
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
                        if(DEBUG) Log.e(TAG, "setUpBottomMenu: add missing: " + e);
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

                            // Tidy up lost shopping list items, which have deleted shopping lists
                            // as an id – else they will never show up on any shopping list
                            ArrayList<Integer> shoppingListIds = new ArrayList<>();
                            for(ShoppingList shoppingList1 : shoppingLists) {
                                shoppingListIds.add(shoppingList1.getId());
                            }
                            if(shoppingListIds.isEmpty()) return;
                            for(ShoppingListItem shoppingListItem : shoppingListItems) {
                                if(!shoppingListIds.contains(shoppingListItem.getId())) {
                                    request.delete(
                                            grocyApi.getObject(
                                                    GrocyApi.ENTITY.SHOPPING_LIST,
                                                    shoppingListItem.getId()
                                            ),
                                            response1 -> {},
                                            error -> {}
                                    );
                                }
                            }
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
            if(DEBUG) Log.e(TAG, "clearShoppingList: " + e);
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
            if(DEBUG) Log.e(TAG, "deleteShoppingList: delete list: " + e);
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

    @Override
    public void onItemRowClicked(int position) {
        if(clickUtil.isDisabled()) return;
        swipeBehavior.recoverLatestSwipedItem();
        showItemBottomSheet(groupedListItems.get(position), position);
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
                        shoppingListItem.getProduct().getQuIdStock()
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
            editTextSearch.setText("");
        }
        textInputLayoutSearch.requestFocus();
        activity.showKeyboard(editTextSearch);

        activity.updateUI(Constants.UI.SHOPPING_LIST_SEARCH, TAG);
    }

    public void dismissSearch() {
        appBarBehavior.replaceLayout(R.id.linear_shopping_list_app_bar_default, true);
        activity.hideKeyboard();
        search = "";
        filterItems(itemsToDisplay);

        activity.updateUI(Constants.UI.SHOPPING_LIST_DEFAULT, TAG);
    }

    private void showMessage(String msg) {
        activity.showMessage(
                Snackbar.make(
                        activity.findViewById(R.id.linear_container_main),
                        msg,
                        Snackbar.LENGTH_SHORT
                )
        );
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
