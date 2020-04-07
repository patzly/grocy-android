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
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.task.JsonDownloadTask;
import xyz.zedler.patrick.grocy.view.CustomChip;

public class StockFragment extends Fragment implements StockItemAdapter.StockItemAdapterListener {

    private final static String TAG = "StockFragment";
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;

    private List<StockItem> stockItems = new ArrayList<>();
    private List<StockItem> expiringItems = new ArrayList<>();
    private List<StockItem> expiredItems = new ArrayList<>();
    private List<StockItem> missingItems = new ArrayList<>();
    private List<QuantityUnit> quantityUnits = new ArrayList<>();
    private List<Location> locations = new ArrayList<>();

    private RecyclerView recyclerView;
    private StockItemAdapter stockItemAdapter;
    private LinearLayout linearLayoutChipContainer;
    private CustomChip chipExpiring, chipExpired, chipMissing;
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

        grocyApi = new GrocyApi(activity);

        // VIEWS

        swipeRefreshLayout = activity.findViewById(R.id.swipe_stock);
        linearLayoutChipContainer = activity.findViewById(R.id.linear_stock_overview_chip_container);
        recyclerView = activity.findViewById(R.id.recycler_stock_overview);

        // SWIPE REFRESH

        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        // CHIPS

        chipExpiring = new CustomChip(
                activity,
                R.color.retro_yellow,
                activity.getString(R.string.msg_expiring_products, 0),
                0, 4
        );
        chipExpired = new CustomChip(
                activity,
                R.color.retro_red,
                activity.getString(R.string.msg_expired_products, 0),
                4, 4
        );
        chipMissing = new CustomChip(
                activity,
                R.color.retro_dirt,
                activity.getString(R.string.msg_missing_products, 0),
                4, 0
        );
        linearLayoutChipContainer.addView(chipExpiring);
        linearLayoutChipContainer.addView(chipExpired);
        linearLayoutChipContainer.addView(chipMissing);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        activity,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(new StockPlaceholderAdapter());

        /*new ItemTouchHelper(
                new StockItemTouchHelper(
                        0,
                        ItemTouchHelper.RIGHT,
                        (viewHolder, direction, position) -> {

                        }
                )
        ).attachToRecyclerView(recyclerView);*/

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

        /*activity.updateUI(mode, "PageFragment: onActivityCreated");

        if(!sharedPrefs.getBoolean(PREF_ANIM_UI_UPDATE, false)) {
            sharedPrefs.edit().putBoolean(PREF_ANIM_UI_UPDATE, true).apply();
        }*/
    }

    private void load() {
        if(activity.isOnline()) {
            swipeRefreshLayout.setRefreshing(true);
            runnableQuantityUnits().run();
        } else {
            // TODO
        }
    }

    private void refresh() {
        swipeRefreshLayout.setRefreshing(true);
        if(activity.isOnline()) {
            runnableQuantityUnits().run();
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

    private Runnable runnableQuantityUnits() {
        return () -> {
            new JsonDownloadTask(grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS), json -> {
                Type listType = new TypeToken<List<QuantityUnit>>(){}.getType();
                quantityUnits = gson.fromJson(json, listType);
                runnableLocations().run();
            }, () -> {
                // TODO
            }).execute();
        };
    }

    private Runnable runnableLocations() {
        return () -> {
            new JsonDownloadTask(grocyApi.getObjects(GrocyApi.ENTITY.LOCATIONS), json -> {
                Type listType = new TypeToken<List<Location>>(){}.getType();
                locations = gson.fromJson(json, listType);
                runnableVolatiles().run();
            }, () -> {
                // TODO
            }).execute();
        };
    }

    private Runnable runnableVolatiles() {
        return () -> new JsonDownloadTask(grocyApi.getStockVolatile(), json -> {
            try {
                JSONObject jsonObject = new JSONObject(json);
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
                        listType
                );
            } catch (JSONException e) {
                e.printStackTrace();
            }

            chipExpiring.setText(
                    activity.getString(
                            R.string.msg_expiring_products,
                            expiringItems.size()
                    )
            );
            chipExpired.setText(
                    activity.getString(
                            R.string.msg_expired_products,
                            expiredItems.size()
                    )
            );
            chipMissing.setText(
                    activity.getString(
                            R.string.msg_missing_products,
                            missingItems.size()
                    )
            );

            runnableStockItems().run();
        }, () -> {
            // TODO
        }).execute();
    }

    private Runnable runnableStockItems() {
        return () -> new JsonDownloadTask(grocyApi.getStock(), json -> {
            Type listType = new TypeToken<List<StockItem>>(){}.getType();
            stockItems = gson.fromJson(json, listType);

            for(StockItem stockItem : missingItems) {
                if(stockItem.getIsPartlyInStock() == 0) {
                    /*new JsonDownloadTask(grocyApi.getStock(), json -> {
                        Type listType = new TypeToken<List<StockItem>>(){}.getType();
                        stockItems = gson.fromJson(json, listType);


                    }, () -> {
                        // TODO
                    }).execute();
                    grocyApi.getStockProduct(stockItem.getId())*/
                }
            }

            stockItemAdapter = new StockItemAdapter(activity, stockItems, quantityUnits, this);

            recyclerView.setAdapter(stockItemAdapter);

            swipeRefreshLayout.setRefreshing(false);
        }, () -> {
            // TODO
        }).execute();
    }

    @Override
    public void onItemRowClicked(int position) {
        StockItemBottomSheetDialogFragment bottomSheet = new StockItemBottomSheetDialogFragment();
        bottomSheet.setData(stockItems.get(position), quantityUnits, locations);
        activity.showBottomSheet(bottomSheet);
    }
}
