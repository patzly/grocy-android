package xyz.zedler.patrick.grocy.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.adapter.StockItemAdapter;
import xyz.zedler.patrick.grocy.adapter.StockPlaceholderAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.view.FilterChip;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class StockFragment extends Fragment implements StockItemAdapter.StockItemAdapterListener {

    private final static String TAG = "StockFragment";
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private AppBarBehavior appBarBehavior;
    private WebRequest request;

    private List<StockItem> stockItems = new ArrayList<>();
    private List<StockItem> expiringItems = new ArrayList<>();
    private List<StockItem> expiredItems = new ArrayList<>();
    private List<MissingItem> missingItems = new ArrayList<>();
    private List<StockItem> filteredItems = new ArrayList<>();
    private List<QuantityUnit> quantityUnits = new ArrayList<>();
    private List<Location> locations = new ArrayList<>();

    private String itemsToDisplay = Constants.STOCK.ALL;

    private RecyclerView recyclerView;
    private StockItemAdapter stockItemAdapter;
    private FilterChip chipExpiring, chipExpired, chipMissing;
    private SwipeRefreshLayout swipeRefreshLayout;

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

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        request = new WebRequest(activity.getRequestQueue());

        grocyApi = new GrocyApi(activity);

        // VIEWS

        swipeRefreshLayout = activity.findViewById(R.id.swipe_stock);
        recyclerView = activity.findViewById(R.id.recycler_stock);

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
                    filterItems(Constants.STOCK.VOLATILE.EXPIRING);
                },
                () -> filterItems(Constants.STOCK.ALL)
        );
        chipExpired = new FilterChip(
                activity,
                R.color.retro_red_light,
                activity.getString(R.string.msg_expired_products, 0),
                () -> {
                    chipExpiring.changeState(false);
                    chipMissing.changeState(false);
                    filterItems(Constants.STOCK.VOLATILE.EXPIRED);
                },
                () -> filterItems(Constants.STOCK.ALL)
        );
        chipMissing = new FilterChip(
                activity,
                R.color.retro_dirt,
                activity.getString(R.string.msg_missing_products, 0),
                () -> {
                    chipExpiring.changeState(false);
                    chipExpired.changeState(false);
                },
                () -> filterItems(Constants.STOCK.ALL)
        );
        LinearLayout chipContainer = activity.findViewById(
                R.id.linear_stock_chip_container
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

        /*frameLayoutBack.setOnClickListener(v -> activity.onBackPressed());

        if (getArguments() != null) {
            mode = getArguments().getString("mode");
            if(mode != null) {
                switch (mode) {
                    case MainActivity.UI_SAVED_DEFAULT:
                        title = getString(R.string.title_saved);
                        break;
                    case MainActivity.UI_CHANNELS_DEFAULT:
                        title = getString(R.string.title_channels);
                        break;
                    case MainActivity.UI_CHANNEL_DEFAULT:
                        title = getArguments().getString("title");
                        break;
                }
            }
            ((TextView) activity.findViewById(R.id.text_app_bar_title_page_default)).setText(title);
        }

        // DEFAULT TOOLBAR
        Menu menuDefault = toolbarDefault.getMenu();
        if(mode.equals(MainActivity.UI_CHANNELS_DEFAULT)) {
            // sort mode not useful for CHANNELS
            menuDefault.findItem(R.id.action_page_default_sort_mode).setVisible(false);
        } else {
            if(sharedPrefs.getBoolean(PREF_DEFAULT_DEFAULT_SORT_MODE, true)) {
                menuDefault.findItem(R.id.action_page_default_sort_mode_descending).setChecked(true);
            } else {
                menuDefault.findItem(R.id.action_page_default_sort_mode_ascending).setChecked(true);
            }
        }
        toolbarDefault.setOnMenuItemClickListener((MenuItem item) -> {
            switch (item.getItemId()) {
                case R.id.action_page_default_select_all:
                    setUpSelection();
                    selectAll();
                    break;
                case R.id.action_page_default_sort_mode_descending:
                    menuDefault.findItem(R.id.action_page_default_sort_mode_descending).setChecked(true);
                    sharedPrefs.edit().putBoolean(PREF_DEFAULT_DEFAULT_SORT_MODE, true).apply();
                    break;
                case R.id.action_page_default_sort_mode_ascending:
                    menuDefault.findItem(R.id.action_page_default_sort_mode_ascending).setChecked(true);
                    sharedPrefs.edit().putBoolean(PREF_DEFAULT_DEFAULT_SORT_MODE, false).apply();
                    break;
            }
            return true;
        });

        // SELECTION TOOLBAR
        ((Toolbar) activity.findViewById(R.id.toolbar_page_selection)).setOnMenuItemClickListener((MenuItem item) -> {
            if(item.getItemId() == R.id.action_page_selection_select_all) {
                activity.startAnimatedIcon(item);
                selectAll();
            }
            return true;
        });

        // SEARCH TOOLBAR
        Menu menuSearch = toolbarSearch.getMenu();
        if(sharedPrefs.getBoolean(PREF_SEARCH_DEFAULT_SORT_MODE, true)) {
            menuSearch.findItem(R.id.action_page_search_sort_mode_descending).setChecked(true);
        } else {
            menuSearch.findItem(R.id.action_page_search_sort_mode_ascending).setChecked(true);
        }
        toolbarSearch.setOnMenuItemClickListener((MenuItem item) -> {
            switch (item.getItemId()) {
                case R.id.action_page_search_select_all:
                    activity.startAnimatedIcon(item);
                    setUpSelection();
                    break;
                case R.id.action_page_search_sort_mode_descending:
                    menuSearch.findItem(R.id.action_page_search_sort_mode_descending).setChecked(true);
                    sharedPrefs.edit().putBoolean(PREF_SEARCH_DEFAULT_SORT_MODE, true).apply();
                    break;
                case R.id.action_page_search_sort_mode_ascending:
                    menuSearch.findItem(R.id.action_page_search_sort_mode_ascending).setChecked(true);
                    sharedPrefs.edit().putBoolean(PREF_SEARCH_DEFAULT_SORT_MODE, false).apply();
                    break;
            }
            return true;
        });*/




        //pageItemTextAdapter.notifyDataSetChanged();

        // UPDATE UI

        activity.updateUI(Constants.UI.STOCK_DEFAULT, TAG);

        /*activity.updateUI(mode, "PageFragment: onActivityCreated");

        if(!sharedPrefs.getBoolean(PREF_ANIM_UI_UPDATE, false)) {
            sharedPrefs.edit().putBoolean(PREF_ANIM_UI_UPDATE, true).apply();
        }*/
    }

    private void load() {
        if(activity.isOnline()) {
            swipeRefreshLayout.setRefreshing(true);
            downloadQuantityUnits();
        } else {
            // TODO
        }
    }

    private void refresh() {
        swipeRefreshLayout.setRefreshing(true);
        if(activity.isOnline()) {
            downloadQuantityUnits();
        } else {
            swipeRefreshLayout.setRefreshing(false);
            activity.showSnackbar(
                    Snackbar.make(
                            activity.findViewById(R.id.linear_container_main),
                            "No connection",
                            Snackbar.LENGTH_SHORT
                    ).setActionTextColor(
                            ContextCompat.getColor(activity, R.color.secondary)
                    ).setAction(
                            "Retry",
                            v1 -> refresh()
                    )
            );
        }
    }

    private void downloadQuantityUnits() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS),
                response -> {
                    Type listType = new TypeToken<List<QuantityUnit>>(){}.getType();
                    quantityUnits = gson.fromJson(response, listType);
                    downloadLocations();
                },
                msg -> { }
        );
    }

    private void downloadLocations() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.LOCATIONS),
                response -> {
                    Type listType = new TypeToken<List<Location>>(){}.getType();
                    locations = gson.fromJson(response, listType);
                    downloadStock();
                },
                msg -> { }
        );
    }

    private void downloadStock() {
        request.get(
                grocyApi.getStock(),
                response -> {
                    Type listType = new TypeToken<List<StockItem>>(){}.getType();
                    stockItems = gson.fromJson(response, listType);
                    downloadVolatile();
                },
                msg -> { }
        );
    }

    private void downloadVolatile() {
        request.get(
                grocyApi.getStockVolatile(),
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        Type listType = new TypeToken<List<StockItem>>(){}.getType();
                        expiringItems = gson.fromJson(
                                jsonObject.getJSONArray("expiring_products").toString(),
                                listType
                        );
                        expiredItems = gson.fromJson(
                                jsonObject.getJSONArray("expired_products").toString(),
                                listType
                        );
                        missingItems = gson.fromJson(
                                jsonObject.getJSONArray("missing_products").toString(),
                                new TypeToken<List<MissingItem>>(){}.getType()
                        );
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
                msg -> { }
        );
    }

    private void downloadMissingProductDetails() {
        List<MissingItem> notPartlyInStock = new ArrayList<>();
        for(MissingItem missingItem : missingItems) {
            // TODO: Check if product is already in stock overview
            if (missingItem.getIsPartlyInStock() == 0) {
                notPartlyInStock.add(missingItem);
            }
        }
        for(int i = 0; i < notPartlyInStock.size(); i++) {
            int finalI = i;
            request.get(
                    grocyApi.getStockProduct(notPartlyInStock.get(i).getId()),
                    resp -> {
                        Type type = new TypeToken<ProductDetails>(){}.getType();
                        ProductDetails productDetails = gson.fromJson(resp, type);
                        stockItems.add(
                                new StockItem(
                                        productDetails.getStockAmount(),
                                        productDetails.getStockAmountAggregated(),
                                        productDetails.getNextBestBeforeDate(),
                                        productDetails.getStockAmountOpened(),
                                        productDetails.getStockAmountOpenedAggregated(),
                                        productDetails.getIsAggregatedAmount(),
                                        productDetails.getProduct().getId(),
                                        productDetails.getProduct()
                                )
                        );
                        if (finalI == notPartlyInStock.size() - 1) {
                            swipeRefreshLayout.setRefreshing(false);
                            filterItems(itemsToDisplay);
                        }
                    },
                    msg -> {}
            );
        }
    }

    private void filterItems(String itemsToDisplay) {
        this.itemsToDisplay = itemsToDisplay;
        switch (itemsToDisplay) {
            case Constants.STOCK.VOLATILE.EXPIRING:
                filteredItems = this.expiringItems;
                break;
            case Constants.STOCK.VOLATILE.EXPIRED:
                filteredItems = this.expiredItems;
                break;
            case Constants.STOCK.VOLATILE.MISSING:
                filteredItems = this.stockItems;
                break;
            default:
                filteredItems = this.stockItems;
                break;
        }
        stockItemAdapter = new StockItemAdapter(
                activity, filteredItems, quantityUnits, this
        );
        recyclerView.animate().alpha(0).setDuration(150).withEndAction(() -> {
            recyclerView.setAdapter(stockItemAdapter);
            recyclerView.animate().alpha(1).setDuration(150).start();
        }).start();
    }

    // STOCK ITEM CLICK
    @Override
    public void onItemRowClicked(int position) {
        StockItemBottomSheetDialogFragment bottomSheet = new StockItemBottomSheetDialogFragment();
        bottomSheet.setData(filteredItems.get(position), quantityUnits, locations);
        activity.showBottomSheet(bottomSheet);
    }

    public void setUpSearch() {
        // SEARCH APP BAR LAYOUT
        appBarBehavior.replaceLayout(R.id.linear_app_bar_stock_search, true);

        TextInputLayout textInputLayoutSearch = activity.findViewById(R.id.text_input_stock_search);
        textInputLayoutSearch.requestFocus();
        activity.showKeyboard(textInputLayoutSearch.getEditText());

        activity.findViewById(R.id.frame_close_stock_search).setOnClickListener(
                v -> activity.onBackPressed()
        );

        // UPDATE UI

        activity.updateUI(Constants.UI.STOCK_SEARCH, TAG);

        /*activity.updateUI(
                mode.equals(MainActivity.UI_SAVED_DEFAULT)
                        ? MainActivity.UI_SAVED_SEARCH
                        : MainActivity.UI_CHANNEL_SEARCH,
                "PageFragment: setUpSearch"
        );*/
    }

    public void dismissSearch() {
        // DEFAULT APP BAR LAYOUT
        appBarBehavior.replaceLayout(R.id.linear_app_bar_stock_default, true);

        activity.hideKeyboard();

        /*frameLayoutBack.setTooltipText(activity.getString(R.string.action_back));
        imageViewBack.setImageResource(R.drawable.ic_round_close_to_arrow_back_anim);
        activity.startAnimatedIcon(imageViewBack);
        activity.hideKeyboard();
        activity.updateUI(mode, "PageFragment: removeSelection");*/

        // UPDATE UI

        activity.updateUI(Constants.UI.STOCK_DEFAULT, TAG);
    }
}
