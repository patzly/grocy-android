package xyz.zedler.patrick.grocy.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;
import com.google.android.material.button.MaterialButton;
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
import xyz.zedler.patrick.grocy.ScanInputActivity;
import xyz.zedler.patrick.grocy.adapter.StockItemAdapter;
import xyz.zedler.patrick.grocy.adapter.StockPlaceholderAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.view.FilterChip;
import xyz.zedler.patrick.grocy.view.InputChip;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class StockFragment extends Fragment implements StockItemAdapter.StockItemAdapterListener {

    private final static String TAG = StockFragment.class.getSimpleName();
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private AppBarBehavior appBarBehavior;
    private WebRequest request;
    private StockItemAdapter stockItemAdapter;

    private List<StockItem> stockItems = new ArrayList<>();
    private List<StockItem> expiringItems = new ArrayList<>();
    private List<StockItem> expiredItems = new ArrayList<>();
    private List<MissingItem> missingItems = new ArrayList<>();
    private List<StockItem> missingStockItems;
    private List<StockItem> filteredItems = new ArrayList<>();
    private List<StockItem> displayedItems = new ArrayList<>();
    private List<QuantityUnit> quantityUnits = new ArrayList<>();
    private List<Location> locations = new ArrayList<>();
    private List<ProductGroup> productGroups = new ArrayList<>();

    private String itemsToDisplay = Constants.STOCK.FILTER.ALL;
    private String search = "";
    private int filterLocationId = -1;
    private String filterProductGroupId = "";
    private String sortMode;
    private int daysExpiringSoon;
    private boolean sortAscending;

    private RecyclerView recyclerView;
    private FilterChip chipExpiring, chipExpired, chipMissing;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextInputLayout textInputLayoutSearch;
    private EditText editTextSearch;
    private LinearLayout linearLayoutFilterContainer, linearLayoutError;
    private InputChip inputChipFilterLocation, inputChipFilterProductGroup;
    private NestedScrollView scrollView;
    private MaterialButton buttonRetry;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_stock, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (MainActivity) getActivity();
        assert activity != null;

        // GET PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        daysExpiringSoon = sharedPrefs.getInt(Constants.PREF.STOCK_EXPIRING_SOON_DAYS, 5);
        sortMode = sharedPrefs.getString(Constants.PREF.STOCK_SORT_MODE, Constants.STOCK.SORT.NAME);
        sortAscending = sharedPrefs.getBoolean(Constants.PREF.STOCK_SORT_ASCENDING, true);

        // WEB REQUESTS

        request = new WebRequest(activity.getRequestQueue());
        grocyApi = activity.getGrocy();

        // INITIALIZE VIEWS

        linearLayoutFilterContainer = activity.findViewById(
                R.id.linear_stock_filter_container_bottom
        );
        linearLayoutError = activity.findViewById(R.id.linear_stock_error);
        swipeRefreshLayout = activity.findViewById(R.id.swipe_stock);
        scrollView = activity.findViewById(R.id.scroll_stock);
        buttonRetry = activity.findViewById(R.id.button_stock_error_retry);
        buttonRetry.setOnClickListener(v -> refresh());
        recyclerView = activity.findViewById(R.id.recycler_stock);
        textInputLayoutSearch = activity.findViewById(R.id.text_input_stock_search);
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

        appBarBehavior = new AppBarBehavior(activity, R.id.linear_app_bar_stock_default);

        // SWIPE REFRESH

        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        // CHIPS

        chipExpiring = new FilterChip(
                activity,
                R.color.retro_yellow_light,
                activity.getString(R.string.msg_expiring_products, 0),
                () -> {
                    chipExpired.changeState(false);
                    chipMissing.changeState(false);
                    filterItems(Constants.STOCK.FILTER.VOLATILE.EXPIRING);
                },
                () -> filterItems(Constants.STOCK.FILTER.ALL)
        );
        chipExpired = new FilterChip(
                activity,
                R.color.retro_red_light,
                activity.getString(R.string.msg_expired_products, 0),
                () -> {
                    chipExpiring.changeState(false);
                    chipMissing.changeState(false);
                    filterItems(Constants.STOCK.FILTER.VOLATILE.EXPIRED);
                },
                () -> filterItems(Constants.STOCK.FILTER.ALL)
        );
        chipMissing = new FilterChip(
                activity,
                R.color.retro_blue_light,
                activity.getString(R.string.msg_missing_products, 0),
                () -> {
                    chipExpiring.changeState(false);
                    chipExpired.changeState(false);
                    filterItems(Constants.STOCK.FILTER.VOLATILE.MISSING);
                },
                () -> filterItems(Constants.STOCK.FILTER.ALL)
        );
        LinearLayout chipContainer = activity.findViewById(
                R.id.linear_stock_filter_container_top
        );
        chipContainer.addView(chipExpiring);
        chipContainer.addView(chipExpired);
        chipContainer.addView(chipMissing);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        activity,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(new StockPlaceholderAdapter());

        load();

        // UPDATE UI

        activity.updateUI(Constants.UI.STOCK_DEFAULT, TAG);

        if(!sharedPrefs.getBoolean(Constants.PREF.ANIM_UI_UPDATE, false)) {
            sharedPrefs.edit().putBoolean(Constants.PREF.ANIM_UI_UPDATE, true).apply();
        }
    }

    private void load() {
        if(activity.isOnline()) {
            download();
        } else {
            setError(true, false);
        }
    }

    private void refresh() {
        if(activity.isOnline()) {
            setError(false, true);
            download();
        } else {
            swipeRefreshLayout.setRefreshing(false);
            activity.showSnackbar(
                    Snackbar.make(
                            activity.findViewById(R.id.linear_container_main),
                            "No connection", // TODO: XML string
                            Snackbar.LENGTH_SHORT
                    ).setActionTextColor(
                            ContextCompat.getColor(activity, R.color.secondary)
                    ).setAction(
                            "Retry", // TODO: XML string
                            v1 -> refresh()
                    )
            );
        }
    }

    private void setError(boolean isError, boolean animated) {
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
        downloadQuantityUnits();
        downloadLocations();
        downloadProductGroups();
        missingStockItems = new ArrayList<>();
        downloadStock();
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

    private void downloadLocations() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.LOCATIONS),
                TAG,
                response -> {
                    locations = gson.fromJson(
                            response,
                            new TypeToken<List<Location>>(){}.getType()
                    );
                    if(DEBUG) Log.i(TAG, "downloadLocations: locations = " + locations);
                    activity.setLocationFilters(locations);
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
                    activity.setProductGroupFilters(productGroups);
                },
                this::onDownloadError,
                this::onQueueEmpty
        );
    }

    private void downloadStock() {
        request.get(
                grocyApi.getStock(),
                TAG,
                response -> {
                    stockItems = gson.fromJson(
                            response,
                            new TypeToken<List<StockItem>>(){}.getType()
                    );
                    if(DEBUG) Log.i(TAG, "downloadStock: stockItems = " + stockItems);
                    downloadVolatile();
                    for(StockItem stockItem : stockItems) {
                        double minStockAmount = stockItem.getProduct().getMinStockAmount();
                        if(minStockAmount > 0 && stockItem.getAmount() < minStockAmount) {
                            missingStockItems.add(stockItem);
                        }
                    }
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

                        // Parse first part of volatile array: expiring products
                        expiringItems = gson.fromJson(
                                jsonObject.getJSONArray("expiring_products").toString(),
                                new TypeToken<List<StockItem>>(){}.getType()
                        );
                        if(DEBUG) Log.i(TAG, "downloadVolatile: expiring = " + expiringItems);

                        // Parse second part of volatile array: expired products
                        expiredItems = gson.fromJson(
                                jsonObject.getJSONArray("expired_products").toString(),
                                new TypeToken<List<StockItem>>(){}.getType()
                        );
                        if(DEBUG) Log.i(TAG, "downloadVolatile: expired = " + expiredItems);

                        // Parse third part of volatile array: missing products
                        missingItems = gson.fromJson(
                                jsonObject.getJSONArray("missing_products").toString(),
                                new TypeToken<List<MissingItem>>(){}.getType()
                        );
                        if(DEBUG) Log.i(TAG, "downloadVolatile: missing = " + missingItems);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    chipExpiring.setText(
                            activity.getString(R.string.msg_expiring_products, expiringItems.size())
                    );
                    chipExpired.setText(
                            activity.getString(R.string.msg_expired_products, expiredItems.size())
                    );
                    chipMissing.setText(
                            activity.getString(R.string.msg_missing_products, missingItems.size())
                    );

                    downloadMissingProductDetails();
                },
                this::onDownloadError,
                this::onQueueEmpty
        );
    }

    private void downloadMissingProductDetails() {
        for(MissingItem missingItem : missingItems) {
            // Filter missing item if it is partly in stock or already in stock overview
            if(missingItem.getIsPartlyInStock() == 1) continue;
            boolean isInStock = false;
            for(StockItem stockItem : stockItems) {
                if (stockItem.getProductId() == missingItem.getId()) {
                    isInStock = true;
                    break;
                }
            }
            if(isInStock) continue;

            request.get(
                    grocyApi.getStockProductDetails(missingItem.getId()),
                    TAG,
                    response -> {
                        ProductDetails productDetails = gson.fromJson(
                                response,
                                new TypeToken<ProductDetails>(){}.getType()
                        );
                        if(DEBUG) Log.i(
                                TAG,
                                "downloadMissingProductDetails: "
                                        + "name = " + productDetails.getProduct().getName()
                        );
                        StockItem stockItem = new StockItem(
                                productDetails.getStockAmount(),
                                productDetails.getStockAmountAggregated(),
                                productDetails.getNextBestBeforeDate(),
                                productDetails.getStockAmountOpened(),
                                productDetails.getStockAmountOpenedAggregated(),
                                productDetails.getIsAggregatedAmount(),
                                productDetails.getProduct().getId(),
                                productDetails.getProduct()
                        );
                        stockItems.add(stockItem);
                        missingStockItems.add(stockItem);
                    },
                    this::onDownloadError,
                    this::onQueueEmpty
            );
        }
    }

    private void onQueueEmpty() {
        swipeRefreshLayout.setRefreshing(false);
        filterItems(itemsToDisplay);
    }

    private void onDownloadError(VolleyError error) {
        request.cancelAll(TAG);
        swipeRefreshLayout.setRefreshing(false);
        setError(true, true);
    }

    private void filterItems(String filter) {
        itemsToDisplay = filter.equals("") ? Constants.STOCK.FILTER.ALL : filter;
        if(DEBUG) Log.i(
                TAG, "filterItems: filter = " + filter + ", display = " + itemsToDisplay
        );
        // VOLATILE
        switch (itemsToDisplay) {
            case Constants.STOCK.FILTER.VOLATILE.EXPIRING:
                filteredItems = this.expiringItems;
                break;
            case Constants.STOCK.FILTER.VOLATILE.EXPIRED:
                filteredItems = this.expiredItems;
                break;
            case Constants.STOCK.FILTER.VOLATILE.MISSING:
                filteredItems = this.missingStockItems;
                break;
            default:
                filteredItems = this.stockItems;
                break;
        }
        if(DEBUG) Log.i(TAG, "filterItems: filteredItems = " + filteredItems);
        // LOCATION
        if(filterLocationId != -1) {
            List<StockItem> tempItems = new ArrayList<>();
            for(StockItem stockItem : filteredItems) {
                if(filterLocationId == stockItem.getProduct().getLocationId()) {
                    tempItems.add(stockItem);
                }
            }
            filteredItems = tempItems;
        }
        // PRODUCT GROUP
        if(!filterProductGroupId.equals("")) {
            List<StockItem> tempItems = new ArrayList<>();
            for(StockItem stockItem : filteredItems) {
                if(filterProductGroupId.equals(stockItem.getProduct().getProductGroupId())) {
                    tempItems.add(stockItem);
                }
            }
            filteredItems = tempItems;
        }
        // SEARCH
        if(!search.equals("")) { // active search
            searchItems(search);
        } else {
            if(displayedItems != filteredItems) {
                displayedItems = filteredItems;
                sortItems(sortMode, sortAscending);
            }
        }
    }

    private void searchItems(String search) {
        search = search.toLowerCase();
        if(DEBUG) Log.i(TAG, "searchItems: search = " + search);
        this.search = search;
        if(search.equals("")) {
            filterItems(itemsToDisplay);
        } else { // only if search contains something
            List<StockItem> searchedItems = new ArrayList<>();
            for(StockItem stockItem : filteredItems) {
                String name = stockItem.getProduct().getName();
                String description = stockItem.getProduct().getDescription();
                name = name != null ? name.toLowerCase() : "";
                description = description != null ? description.toLowerCase() : "";
                if(name.contains(search) || description.contains(search)) {
                    searchedItems.add(stockItem);
                }
            }
            if(displayedItems != searchedItems) {
                displayedItems = searchedItems;
                sortItems(sortMode, sortAscending);
            }
        }
    }

    public void filterLocation(Location location) {
        if(filterLocationId != location.getId()) { // only if not already selected
            if(DEBUG) Log.i(TAG, "filterLocation: " + location);
            filterLocationId = location.getId();
            if(inputChipFilterLocation != null) {
                inputChipFilterLocation.change(location.getName());
            } else {
                inputChipFilterLocation = new InputChip(
                        activity,
                        location.getName(),
                        R.drawable.ic_round_place,
                        true,
                        () -> {
                            filterLocationId = -1;
                            inputChipFilterLocation = null;
                            filterItems(itemsToDisplay);
                        });
                linearLayoutFilterContainer.addView(inputChipFilterLocation);
            }
            filterItems(itemsToDisplay);
        } else {
            if(DEBUG) Log.i(TAG, "filterLocation: " + location + " already filtered");
        }
    }

    public void filterProductGroup(ProductGroup productGroup) {
        if(!filterProductGroupId.equals(String.valueOf(productGroup.getId()))) {
            if(DEBUG) Log.i(TAG, "filterProductGroup: " + productGroup);
            filterProductGroupId = String.valueOf(productGroup.getId());
            if(inputChipFilterProductGroup != null) {
                inputChipFilterProductGroup.change(productGroup.getName());
            } else {
                inputChipFilterProductGroup = new InputChip(
                        activity,
                        productGroup.getName(),
                        R.drawable.ic_round_category,
                        true,
                        () -> {
                            filterProductGroupId = "";
                            inputChipFilterProductGroup = null;
                            filterItems(itemsToDisplay);
                        });
                linearLayoutFilterContainer.addView(inputChipFilterProductGroup);
            }
            filterItems(itemsToDisplay);
        } else {
            if(DEBUG) Log.i(TAG, "filterProductGroup: " + productGroup + " already filtered");
        }
    }

    private void sortItems(String sortMode, boolean ascending) {
        if(DEBUG) Log.i(TAG, "sortItems: sort by " + sortMode + ", ascending = " + ascending);
        this.sortMode = sortMode;
        sortAscending = ascending;
        sharedPrefs.edit()
                .putString(Constants.PREF.STOCK_SORT_MODE, sortMode)
                .putBoolean(Constants.PREF.STOCK_SORT_ASCENDING, ascending)
                .apply();
        switch (sortMode) {
            case Constants.STOCK.SORT.NAME:
                SortUtil.sortStockItemsByName(displayedItems, ascending);
                break;
            case Constants.STOCK.SORT.BBD:
                SortUtil.sortStockItemsByBBD(displayedItems, ascending);
                break;
        }
        refreshAdapter(
                new StockItemAdapter(
                        activity,
                        displayedItems,
                        quantityUnits,
                        daysExpiringSoon,
                        sortMode,
                        this
                )
        );
    }

    public void sortItems(String sortMode) {
        sortItems(sortMode, sortAscending);
    }

    public void sortItems(boolean ascending) {
        sortItems(sortMode, ascending);
    }

    private void refreshAdapter(StockItemAdapter adapter) {
        stockItemAdapter = adapter;
        recyclerView.animate().alpha(0).setDuration(150).withEndAction(() -> {
            recyclerView.setAdapter(adapter);
            recyclerView.animate().alpha(1).setDuration(150).start();
        }).start();
    }

    private void loadProductDetailsByBarcode(String barcode) {
        swipeRefreshLayout.setRefreshing(true);
        request.get(
                grocyApi.getStockProductByBarcode(barcode),
                response -> {
                    swipeRefreshLayout.setRefreshing(false);
                    ProductDetails productDetails = gson.fromJson(
                            response,
                            new TypeToken<ProductDetails>(){}.getType()
                    );
                    showProductOverview(productDetails);
                }, error -> {
                    NetworkResponse response = error.networkResponse;
                    if(response != null && response.statusCode == 400) {
                        //autoCompleteTextViewProduct.setText(barcode);
                        activity.showBottomSheet(
                                new ConsumeBarcodeBottomSheetDialogFragment(), null
                        );
                    } else {
                        activity.showSnackbar(
                                Snackbar.make(
                                        activity.findViewById(R.id.linear_container_main),
                                        activity.getString(R.string.msg_error),
                                        Snackbar.LENGTH_SHORT
                                )
                        );
                    }
                    swipeRefreshLayout.setRefreshing(false);
                }
        );
    }

    /**
     * Called from product details BottomSheet when button was pressed
     * @param action Constants.ACTION
     */
    public void performAction(String action, int productId) {
        switch (action) {
            case Constants.ACTION.CONSUME:
                consumeProduct(productId, 1, false);
                break;
            case Constants.ACTION.OPEN:
                openProduct(productId);
                break;
            case Constants.ACTION.CONSUME_ALL:
                StockItem stockItem = getStockItem(productId);
                if(stockItem != null) {
                    consumeProduct(
                            productId,
                            stockItem.getProduct().getEnableTareWeightHandling() == 0
                                    ? stockItem.getAmount()
                                    : stockItem.getProduct().getTareWeight(),
                            false
                    );
                }
                break;
            case Constants.ACTION.CONSUME_SPOILED:
                consumeProduct(productId, 1, true);
                break;
        }
    }

    private void consumeProduct(int productId, double amount, boolean spoiled) {
        int index = getProductPosition(productId);
        JSONObject body = new JSONObject();
        try {
            body.put("amount", amount);
            body.put("transaction_type", "consume");
            body.put("spoiled", spoiled);
        } catch (JSONException e) {
            if(DEBUG) Log.e(TAG, "consumeProduct: " + e);
        }
        Log.i(TAG, "consumeProduct: " + activity);
        request.post(
                grocyApi.consumeProduct(productId),
                body,
                response -> {
                    String transactionId = null;
                    try {
                        transactionId = response.getString("transaction_id");
                    } catch (JSONException e) {
                        if(DEBUG) Log.e(TAG, "consumeProduct: " + e);
                    }
                    for(StockItem stockItem : displayedItems) {
                        if(stockItem.getProduct().getId() == productId) {
                            final double savedAmountForUndo = stockItem.getAmount();
                            if(stockItem.getProduct().getEnableTareWeightHandling() == 0) {
                                stockItem.changeAmount(-amount);
                            } else {
                                stockItem.changeAmount(-savedAmountForUndo);
                            }
                            if(stockItem.getAmount() == 0
                                    && stockItem.getProduct().getMinStockAmount() == 0
                            ) {
                                // only delete item if min stock amount is zero
                                displayedItems.remove(index);
                                stockItemAdapter.notifyItemRemoved(index);
                            } else {
                                stockItemAdapter.notifyItemChanged(index);
                            }
                            if(DEBUG) Log.i(TAG, "consumeProduct: consumed " + amount);
                            QuantityUnit quantityUnit = getQuantityUnit(
                                    stockItem.getProduct().getQuIdStock()
                            );
                            Snackbar snackbar = Snackbar.make(
                                    activity.findViewById(R.id.linear_container_main),
                                    activity.getString(
                                            spoiled
                                                    ? R.string.msg_consumed_spoiled
                                                    : R.string.msg_consumed,
                                            NumUtil.trim(
                                                    stockItem.getProduct()
                                                            .getEnableTareWeightHandling() == 0
                                                            ? amount
                                                            : savedAmountForUndo
                                            ), quantityUnit != null
                                                    ? amount == 1
                                                            ? quantityUnit.getName()
                                                            : quantityUnit.getNamePlural()
                                                    : "",
                                            stockItem.getProduct().getName()
                                    ), Snackbar.LENGTH_LONG
                            );
                            if(transactionId != null) {
                                String transId = transactionId;
                                snackbar.setActionTextColor(
                                        ContextCompat.getColor(activity, R.color.secondary)
                                ).setAction(
                                        activity.getString(R.string.action_undo),
                                        v -> undoConsumeTransaction(
                                                transId,
                                                stockItem,
                                                amount,
                                                savedAmountForUndo,
                                                index
                                        )
                                );
                            }
                            activity.showSnackbar(snackbar);
                            break;
                        }
                    }
                },
                error -> {
                    showErrorMessage(error);
                    if(DEBUG) Log.i(TAG, "consumeProduct: " + error);
                }
        );
    }

    private void undoConsumeTransaction(
            String transactionId,
            StockItem stockItem,
            double amount,
            double savedAmountForUndo,
            int index
    ) {
        request.post(
                grocyApi.undoStockTransaction(transactionId),
                success -> {
                    if(stockItem.getAmount() == 0
                            && stockItem.getProduct()
                            .getMinStockAmount() == 0
                    ) {
                        // only insert if it was removed
                        if(stockItem.getProduct().getEnableTareWeightHandling() == 0) {
                            stockItem.changeAmount(amount);
                        } else {
                            stockItem.changeAmount(savedAmountForUndo);
                        }
                        displayedItems.add(index, stockItem);
                        stockItemAdapter.notifyItemInserted(index);
                    } else {
                        if(stockItem.getProduct().getEnableTareWeightHandling() == 0) {
                            stockItem.changeAmount(amount);
                        } else {
                            stockItem.changeAmount(savedAmountForUndo);
                        }
                        stockItemAdapter.notifyItemChanged(index);
                    }
                    activity.showSnackbar(
                            Snackbar.make(
                                    activity.findViewById(R.id.linear_container_main),
                                    "Undone transaction", // TODO: XML string
                                    Snackbar.LENGTH_SHORT
                            )
                    );
                    if(DEBUG) Log.i(
                            TAG, "consumeProduct: undone"
                    );
                },
                this::showErrorMessage
        );
    }

    private void openProduct(int productId) {
        JSONObject body = new JSONObject();
        try {
            double amount = 1;
            body.put("amount", amount);
        } catch (JSONException e) {
            if(DEBUG) Log.e(TAG, "openProduct: " + e);
        }
        request.post(
                grocyApi.openProduct(productId),
                body,
                response -> {
                    String transactionId = null;
                    try {
                        transactionId = response.getString("transaction_id");
                    } catch (JSONException e) {
                        if(DEBUG) Log.e(TAG, "openProduct: " + e);
                    }
                    for(StockItem stockItem : displayedItems) {
                        if(stockItem.getProduct().getId() == productId) {
                            stockItem.changeAmountOpened(1);
                            stockItemAdapter.notifyItemChanged(getProductPosition(productId));
                            if(DEBUG) Log.i(TAG, "openProduct: opened 1");
                            QuantityUnit quantityUnit = getQuantityUnit(
                                    stockItem.getProduct().getQuIdStock()
                            );
                            Snackbar snackbar = Snackbar.make(
                                    activity.findViewById(R.id.linear_container_main),
                                    activity.getString(
                                            R.string.msg_opened,
                                            NumUtil.trim(1),
                                            quantityUnit != null
                                                    ? quantityUnit.getName()
                                                    : "",
                                            stockItem.getProduct().getName()
                                    ),
                                    Snackbar.LENGTH_LONG
                            );
                            if(transactionId != null) {
                                final String transId = transactionId;
                                snackbar.setActionTextColor(
                                        ContextCompat.getColor(activity, R.color.secondary)
                                ).setAction(
                                        activity.getString(R.string.action_undo),
                                        v -> undoOpenTransaction(transId, stockItem)
                                );
                            }
                            activity.showSnackbar(snackbar);
                            break;
                        }
                    }
                },
                error -> {
                    showErrorMessage(error);
                    if(DEBUG) Log.i(TAG, "openProduct: " + error);
                }
        );
    }

    private void undoOpenTransaction(String transactionId, StockItem stockItem) {
        request.post(
                grocyApi.undoStockTransaction(transactionId),
                success -> {
                    stockItem.changeAmountOpened(-1);
                    stockItemAdapter.notifyItemChanged(
                            getProductPosition(stockItem.getProduct().getId())
                    );
                    activity.showSnackbar(
                            Snackbar.make(
                                    activity.findViewById(R.id.linear_container_main),
                                    "Undone transaction", // TODO: XML string
                                    Snackbar.LENGTH_SHORT
                            )
                    );
                    if(DEBUG) Log.i(TAG, "consumeProduct: undone");
                },
                this::showErrorMessage
        );
    }

    private StockItem getStockItem(int productId) {
        for(StockItem stockItem : displayedItems) {
            if(stockItem.getProduct().getId() == productId) {
                return stockItem;
            }
        } return null;
    }

    /**
     * Returns index in the displayed items.
     * Used for providing a safe and up-to-date value
     * e.g. when the items are filtered/sorted before server responds
     */
    private int getProductPosition(int productId) {
        for(int i = 0; i < displayedItems.size(); i++) {
            if(displayedItems.get(i).getProduct().getId() == productId) {
                return i;
            }
        }
        return 0;
    }

    private QuantityUnit getQuantityUnit(int id) {
        for(QuantityUnit quantityUnit : quantityUnits) {
            if(quantityUnit.getId() == id) {
                return quantityUnit;
            }
        } return null;
    }

    private Location getLocation(int id) {
        for(Location location : locations) {
            if(location.getId() == id) {
                return location;
            }
        } return null;
    }

    private void showErrorMessage(VolleyError error) {
        activity.showSnackbar(
                Snackbar.make(
                        activity.findViewById(R.id.linear_container_main),
                        "An error occurred", // TODO: XML string
                        Snackbar.LENGTH_SHORT
                )
        );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == Constants.REQUEST.SCAN && resultCode == Activity.RESULT_OK) {
            if(data != null) {
                loadProductDetailsByBarcode(data.getStringExtra(Constants.EXTRA.SCAN_RESULT));
            }
        }
    }

    @Override
    public void onItemRowClicked(int position) {
        // STOCK ITEM CLICK
        showProductOverview(displayedItems.get(position));
    }

    private void showProductOverview(StockItem stockItem) {
        if(stockItem != null) {
            QuantityUnit quantityUnit = getQuantityUnit(stockItem.getProduct().getQuIdStock());
            Location location = getLocation(stockItem.getProduct().getLocationId());
            Bundle bundle = new Bundle();
            bundle.putBoolean(Constants.ARGUMENT.SHOW_ACTIONS, true);
            bundle.putParcelable(Constants.ARGUMENT.STOCK_ITEM, stockItem);
            bundle.putParcelable(Constants.ARGUMENT.QUANTITY_UNIT, quantityUnit);
            bundle.putParcelable(Constants.ARGUMENT.LOCATION, location);
            activity.showBottomSheet(new ProductOverviewBottomSheetDialogFragment(), bundle);
        }
    }

    private void showProductOverview(ProductDetails productDetails) {
        if(productDetails != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(Constants.ARGUMENT.PRODUCT_DETAILS, productDetails);
            bundle.putBoolean(Constants.ARGUMENT.SET_UP_WITH_PRODUCT_DETAILS, true);
            bundle.putBoolean(Constants.ARGUMENT.SHOW_ACTIONS, true);
            activity.showBottomSheet(
                    new ProductOverviewBottomSheetDialogFragment(),
                    bundle
            );
        }
    }

    public void openBarcodeScanner() {
        startActivityForResult(
                new Intent(activity, ScanInputActivity.class),
                Constants.REQUEST.SCAN
        );
    }

    public void setUpSearch() {
        if(search.equals("")) { // only if no search is active
            appBarBehavior.replaceLayout(R.id.linear_app_bar_stock_search, true);
            editTextSearch.setText("");
        }
        textInputLayoutSearch.requestFocus();
        activity.showKeyboard(editTextSearch);

        activity.findViewById(R.id.frame_close_stock_search).setOnClickListener(
                v -> dismissSearch()
        );

        activity.updateUI(Constants.UI.STOCK_SEARCH, TAG);
    }

    public void dismissSearch() {
        appBarBehavior.replaceLayout(R.id.linear_app_bar_stock_default, true);
        activity.hideKeyboard();
        search = "";
        filterItems(itemsToDisplay); // TODO: buggy animation

        activity.updateUI(Constants.UI.STOCK_DEFAULT, TAG);
    }
}
