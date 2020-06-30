package xyz.zedler.patrick.grocy;

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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.VolleyError;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import xyz.zedler.patrick.grocy.adapter.ShoppingItemAdapter;
import xyz.zedler.patrick.grocy.adapter.ShoppingPlaceholderAdapter;
import xyz.zedler.patrick.grocy.animator.ItemAnimator;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.databinding.ActivityShoppingBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.LoadOfflineDataShoppingListHelper;
import xyz.zedler.patrick.grocy.helper.ShoppingListHelper;
import xyz.zedler.patrick.grocy.helper.StoreOfflineDataShoppingListHelper;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NetUtil;

public class ShoppingActivity extends AppCompatActivity implements
        ShoppingItemAdapter.ShoppingListItemSpecialAdapterListener,
        LoadOfflineDataShoppingListHelper.AsyncResponse,
        StoreOfflineDataShoppingListHelper.AsyncResponse {

    private final static String TAG = ShoppingActivity.class.getSimpleName();

    private SharedPreferences sharedPrefs;
    private DownloadHelper dlHelper;
    private Timer timer;
    private AppDatabase database;
    private NetUtil netUtil;

    private ArrayList<ShoppingListItem> shoppingListItems;
    private ArrayList<ShoppingListItem> shoppingListItemsSelected;
    private ArrayList<ShoppingList> shoppingLists;
    private ArrayList<ProductGroup> productGroups;
    private ArrayList<QuantityUnit> quantityUnits;
    private ArrayList<Product> products;
    private ArrayList<GroupedListItem> groupedListItems;
    private ArrayList<Integer> missingProductIds;
    private HashMap<Integer, ShoppingList> shoppingListHashMap;
    private HashMap<Integer, Product> productHashMap;

    private int selectedShoppingListId;
    private boolean showOffline;
    private boolean productUpdateDone;
    private boolean isDataStored;
    private boolean debug;
    private Date lastSynced;
    private TimerTask timerTask;

    private ActivityShoppingBinding binding;
    private ShoppingItemAdapter shoppingItemAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        // UTILS

        netUtil = new NetUtil(this);

        // DATABASE

        database = AppDatabase.getAppDatabase(getApplicationContext());

        // WEB

        dlHelper = new DownloadHelper(this, TAG);

        // INITIALIZE VARIABLES

        shoppingLists = new ArrayList<>();
        shoppingListItems = new ArrayList<>();
        shoppingListItemsSelected = new ArrayList<>();
        quantityUnits = new ArrayList<>();
        products = new ArrayList<>();
        productGroups = new ArrayList<>();
        groupedListItems = new ArrayList<>();
        missingProductIds = new ArrayList<>();
        shoppingListHashMap = new HashMap<>();
        productHashMap = new HashMap<>();

        int lastId = sharedPrefs.getInt(Constants.PREF.SHOPPING_LIST_LAST_ID, 1);
        if(lastId != 1 && !isFeatureMultipleListsEnabled()) {
            sharedPrefs.edit().putInt(Constants.PREF.SHOPPING_LIST_LAST_ID, 1).apply();
            lastId = 1;
        }
        selectedShoppingListId = lastId;

        // VIEWS

        binding = ActivityShoppingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.frameShoppingClose.setOnClickListener(v -> onBackPressed());
        binding.textTitle.setOnClickListener(v -> showShoppingListsBottomSheet());
        binding.buttonLists.setOnClickListener(v -> showShoppingListsBottomSheet());

        binding.swipe.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(this, R.color.surface)
        );
        binding.swipe.setColorSchemeColors(
                ContextCompat.getColor(this, R.color.secondary)
        );
        binding.swipe.setOnRefreshListener(this::refresh);

        binding.recycler.setLayoutManager(
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
        binding.recycler.setItemAnimator(new ItemAnimator());
        binding.recycler.setAdapter(new ShoppingPlaceholderAdapter());

        // UI

        getWindow().setStatusBarColor(
                ContextCompat.getColor(
                        this,
                        Build.VERSION.SDK_INT <= Build.VERSION_CODES.M
                                ? R.color.status_bar_lollipop
                                : R.color.primary
                )
        );
        getWindow().setNavigationBarColor(
                ContextCompat.getColor(this, R.color.background_dark)
        );

        load();
    }

    @Override
    protected void onStart() {
        super.onStart();
        keepScreenOn(
                sharedPrefs.getBoolean(Constants.PREF.KEEP_SHOPPING_SCREEN_ON, false)
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        keepScreenOn(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(timer != null) timer.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int seconds = sharedPrefs.getInt(
                Constants.PREF.SHOPPING_MODE_UPDATE_INTERVAL,
                10
        );
        if(seconds == 0) return;
        timer = new Timer();
        initTimerTask();
        timer.schedule(timerTask, 1000, seconds*1000);
    }

    private void load() {
        if(netUtil.isOnline()) {
            downloadFull();
        } else {
            loadOfflineData();
        }
    }

    public void refresh() {
        if(netUtil.isOnline()) {
            downloadFull();
            timer.cancel();
            int seconds = sharedPrefs.getInt(
                    Constants.PREF.SHOPPING_MODE_UPDATE_INTERVAL,
                    10
            );
            if(seconds == 0) return;
            initTimerTask();
            timer = new Timer();
            timer.schedule(timerTask, seconds*1000, seconds*1000);
        } else {
            binding.swipe.setRefreshing(false);
            loadOfflineData();
            showMessage(getString(R.string.msg_no_connection));
        }
    }

    private void downloadFull() {
        binding.swipe.setRefreshing(true);
        DownloadHelper.Queue queue = dlHelper.newQueue(
                () -> onQueueEmpty(false),
                this::onDownloadError
        );
        queue.append(
                dlHelper.getShoppingLists(listItems -> this.shoppingLists = listItems),
                dlHelper.getShoppingListItems(listItems -> this.shoppingListItems = listItems),
                dlHelper.getProductGroups(listItems -> this.productGroups = listItems),
                dlHelper.getQuantityUnits(listItems -> this.quantityUnits = listItems),
                dlHelper.getProducts(listItems -> this.products = listItems)
        );
        queue.start();
        missingProductIds.clear();
        productUpdateDone = true;
    }

    private void downloadOnlyShoppingListItems() {
        dlHelper.getShoppingListItems(listItems -> {
            this.shoppingListItems = listItems;
            onQueueEmpty(true);
        }, this::onDownloadError).perform();
        productUpdateDone = false;
    }

    private void downloadOnlyProducts() {
        dlHelper.getProducts(products -> {
            this.products = products;
            productHashMap.clear();
            for(Product p : products) productHashMap.put(p.getId(), p);
            if(this.missingProductIds.isEmpty()) return;
            ArrayList<Integer> missingProductIds = new ArrayList<>();
            for(int productId : this.missingProductIds) {
                if(productHashMap.get(productId) == null) missingProductIds.add(productId);
            }
            this.missingProductIds = missingProductIds;
            onQueueEmpty(true);
        }).perform();
    }

    private void onQueueEmpty(boolean onlyDeltaUpdate) {
        if(showOffline) {
            showOffline = false;
            appBarOfflineInfo(false);
        }

        shoppingListItemsSelected = new ArrayList<>();
        ArrayList<Integer> allUsedProductIds = new ArrayList<>();  // for database preparing
        for(ShoppingListItem shoppingListItem : shoppingListItems) {
            if(shoppingListItem.getProductId() != null) {
                allUsedProductIds.add(Integer.parseInt(shoppingListItem.getProductId()));
            }
            if(shoppingListItem.getShoppingListId() != selectedShoppingListId) continue;
            if(shoppingListItem.isUndone()) {
                shoppingListItemsSelected.add(shoppingListItem);
            }
        }

        if(!isDataStored) {
            // update action bar
            if(!onlyDeltaUpdate) {
                changeAppBarTitle();
                if(shoppingLists.size() == 1) binding.buttonLists.setVisibility(View.GONE);
            }

            lastSynced = Calendar.getInstance().getTime();
            // sync modified data and store new data
            new StoreOfflineDataShoppingListHelper(
                    AppDatabase.getAppDatabase(getApplicationContext()),
                    this,
                    true,
                    shoppingLists,
                    shoppingListItems,
                    productGroups,
                    quantityUnits,
                    products,
                    allUsedProductIds,
                    onlyDeltaUpdate
            ).execute();
        } else {
            isDataStored = false;

            // set product in shoppingListItem
            boolean missingProductIdsChanged = false;
            for(ShoppingListItem shoppingListItem : shoppingListItemsSelected) {
                String productIdStr = shoppingListItem.getProductId();
                if(productIdStr == null || productIdStr.isEmpty()) continue;
                int productId = Integer.parseInt(productIdStr);
                Product product = productHashMap.get(productId);
                if(product == null && !missingProductIds.contains(productId)) {
                    missingProductIds.add(productId);
                    missingProductIdsChanged = true;
                }
                shoppingListItem.setProduct(product);
            }
            if(missingProductIdsChanged && !productUpdateDone) { // entries with new products were created
                downloadOnlyProducts(); // to display them properly, they have to be downloaded
                return;
            }

            binding.swipe.setRefreshing(false);
            groupItems(onlyDeltaUpdate);
        }
    }

    private void onDownloadError(VolleyError ignored) {
        binding.swipe.setRefreshing(false);
        loadOfflineData();
    }

    private void loadOfflineData() {
        if(!showOffline) {
            showOffline = true;
            appBarOfflineInfo(true);
            if(debug) Log.i(TAG, "loadOfflineData: you are now offline");
            new LoadOfflineDataShoppingListHelper(
                    AppDatabase.getAppDatabase(getApplicationContext()),
                    this
            ).execute();
        }
        lastSynced = null;
    }

    @Override
    public void prepareOfflineData(
            ArrayList<ShoppingListItem> shoppingListItems,
            ArrayList<ShoppingList> shoppingLists,
            ArrayList<ProductGroup> productGroups,
            ArrayList<QuantityUnit> quantityUnits
    ) {
        this.shoppingListItems = shoppingListItems;
        this.shoppingLists = shoppingLists;
        this.productGroups = productGroups;
        this.quantityUnits = quantityUnits;

        shoppingListItemsSelected = new ArrayList<>();

        for(ShoppingListItem shoppingListItem : shoppingListItems) {
            if(shoppingListItem.getShoppingListId() != selectedShoppingListId) continue;
            if(shoppingListItem.isUndone()) {
                shoppingListItemsSelected.add(shoppingListItem);
            }
        }

        changeAppBarTitle();
        if(shoppingLists.size() == 1) binding.buttonLists.setVisibility(View.GONE);

        groupItems(true);
    }

    private void groupItems(boolean onlyDeltaUpdate) {
        groupedListItems = ShoppingListHelper.groupItems(
                this,
                shoppingListItemsSelected,
                productGroups,
                shoppingLists,
                selectedShoppingListId,
                true
        );
        if(onlyDeltaUpdate && shoppingItemAdapter != null) {
            shoppingItemAdapter.updateList(groupedListItems);
            return;
        }
        ShoppingItemAdapter adapter = new ShoppingItemAdapter(
                this,
                groupedListItems,
                quantityUnits,
                this
        );
        shoppingItemAdapter = adapter;
        binding.recycler.animate().alpha(0).setDuration(150).withEndAction(() -> {
            binding.recycler.setAdapter(adapter);
            binding.recycler.animate().alpha(1).setDuration(150).start();
        }).start();
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
                            AppDatabase.getAppDatabase(getApplicationContext()),
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
    }

    @Override
    public void storedDataSuccessfully(
            ArrayList<ShoppingListItem> shoppingListItems,
            boolean onlyDeltaUpdateAdapter
    ) {
        isDataStored = true;
        this.shoppingListItems = shoppingListItems;
        onQueueEmpty(onlyDeltaUpdateAdapter);
    }

    @Override
    public void onItemRowClicked(int position) {
        toggleDoneStatus(position);
    }

    public void toggleDoneStatus(int position) {
        ShoppingListItem shoppingListItem = (ShoppingListItem) groupedListItems.get(position);
        toggleDoneStatus(shoppingListItem);
    }

    public void toggleDoneStatus(@NonNull ShoppingListItem shoppingListItem) {
        if(shoppingListItem.getDoneSynced() == -1) {
            shoppingListItem.setDoneSynced(shoppingListItem.getDone());
        }

        shoppingListItem.setDone(shoppingListItem.getDone() == 0 ? 1 : 0);  // toggle state

        if(showOffline) {
            updateDoneStatus(shoppingListItem, true);
            return;
        }

        dlHelper.getTimeDbChanged(date -> {
            boolean syncNeeded = this.lastSynced == null || this.lastSynced.before(date);
            JSONObject body = new JSONObject();
            try {
                body.put("done", shoppingListItem.getDone());
            } catch (JSONException e) {
                if(debug) Log.e(TAG, "toggleDoneStatus: " + e);
            }
            dlHelper.editShoppingListItem(
                    shoppingListItem.getId(),
                    body,
                    response -> {
                        if(syncNeeded) {
                            updateDoneStatus(shoppingListItem, false);
                            downloadOnlyShoppingListItems();
                        } else {
                            updateDoneStatus(shoppingListItem, true);
                            dlHelper.getTimeDbChanged(
                                    date1 -> lastSynced = date1,
                                    () -> lastSynced = Calendar.getInstance().getTime()
                            );
                            lastSynced = Calendar.getInstance().getTime();
                        }
                    },
                    error -> {
                        updateDoneStatus(shoppingListItem, false);
                        loadOfflineData();
                    }
            ).perform();
        }, () -> {
            updateDoneStatus(shoppingListItem, false);
            loadOfflineData();
        });
    }

    private void updateDoneStatus(ShoppingListItem shoppingListItem, boolean updateList) {
        new Thread(() -> database.shoppingListItemDao().update(shoppingListItem)).start();
        if(updateList) {
            if(shoppingListItem.getDone() == 1) {
                shoppingListItemsSelected.remove(shoppingListItem);
            } else {
                shoppingListItemsSelected.add(shoppingListItem);
            }
            groupItems(true);
        }
        int msg;
        if(shoppingListItem.getDone() == 1) {
            msg = R.string.msg_item_marked_as_done;
        } else {
            msg = R.string.msg_item_marked_as_undone;
        }
        Snackbar snackbar = Snackbar.make(binding.recycler, msg, Snackbar.LENGTH_LONG);
        snackbar.setAction(
                R.string.action_undo,
                v -> toggleDoneStatus(shoppingListItem)
        );
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.secondary));
        snackbar.show();
    }

    private void initTimerTask() {
        if(timerTask != null) timerTask.cancel();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                dlHelper.getTimeDbChanged(
                        date -> {
                            if(!netUtil.isOnline()) {
                                loadOfflineData();
                            } else if(lastSynced == null || lastSynced.before(date)) {
                                downloadOnlyShoppingListItems();
                            } else {
                                if(debug) Log.i(TAG, "run: skip sync of list items");
                            }
                        },
                        () -> downloadOnlyShoppingListItems()
                );
            }
        };
    }

    private void showShoppingListsBottomSheet() {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(Constants.ARGUMENT.SHOPPING_LISTS, shoppingLists);
        bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedShoppingListId);
        bundle.putBoolean(Constants.ARGUMENT.SHOW_OFFLINE, true);
        showBottomSheet(new ShoppingListsBottomSheetDialogFragment(), bundle);
    }

    public void selectShoppingList(int shoppingListId) {
        if(shoppingListId == selectedShoppingListId) return;
        ShoppingList shoppingList = getShoppingList(shoppingListId);
        if(shoppingList == null) return;
        selectedShoppingListId = shoppingListId;
        sharedPrefs.edit().putInt(Constants.PREF.SHOPPING_LIST_LAST_ID, shoppingListId).apply();
        changeAppBarTitle(shoppingList);
        if(showOffline) {
            new LoadOfflineDataShoppingListHelper(
                    AppDatabase.getAppDatabase(getApplicationContext()),
                    this
            ).execute();
        } else {
            onQueueEmpty(false);
        }
    }

    private void changeAppBarTitle(ShoppingList shoppingList) {
        ShoppingListHelper.changeAppBarTitle(binding.textTitle, binding.buttonLists, shoppingList);
    }

    private void changeAppBarTitle() {
        ShoppingList shoppingList = getShoppingList(selectedShoppingListId);
        changeAppBarTitle(shoppingList);
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
        // get actionbar height
        int actionBarHeight = 0;
        TypedValue typedValue = new TypedValue();
        if(getTheme().resolveAttribute(R.attr.actionBarSize, typedValue, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(
                    typedValue.data,
                    getResources().getDisplayMetrics()
            );
        }
        if(visible) actionBarHeight += binding.linearOfflineError.getHeight();
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams)
                binding.linearScroll.getLayoutParams();
        layoutParams.setMargins(0, actionBarHeight, 0, 0);
        binding.linearScroll.setLayoutParams(layoutParams);
    }

    private boolean isFeatureMultipleListsEnabled() {
        return sharedPrefs.getBoolean(Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS, true);
    }

    private ShoppingList getShoppingList(int shoppingListId) {
        if(shoppingListHashMap.isEmpty()) {
            for(ShoppingList s : shoppingLists) shoppingListHashMap.put(s.getId(), s);
        }
        return shoppingListHashMap.get(shoppingListId);
    }

    public void showBottomSheet(@NonNull BottomSheetDialogFragment bottomSheet, Bundle bundle) {
        String tag = bottomSheet.toString();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment == null || !fragment.isVisible()) {
            if(bundle != null) bottomSheet.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().add(bottomSheet, tag).commit();
            if(debug) Log.i(TAG, "showBottomSheet: " + tag);
        } else if(debug) Log.e(TAG, "showBottomSheet: sheet already visible");
    }

    private void showMessage(String msg) {
        Snackbar.make(binding.recycler, msg, Snackbar.LENGTH_SHORT).show();
    }

    private void keepScreenOn(boolean keepOn) {
        if(keepOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}