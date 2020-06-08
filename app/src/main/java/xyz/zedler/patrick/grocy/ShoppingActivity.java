package xyz.zedler.patrick.grocy;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import xyz.zedler.patrick.grocy.adapter.ShoppingListItemSpecialAdapter;
import xyz.zedler.patrick.grocy.adapter.StockPlaceholderAdapter;
import xyz.zedler.patrick.grocy.animator.ItemAnimator;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.databinding.ActivityShoppingBinding;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.LoadOfflineDataShoppingListHelper;
import xyz.zedler.patrick.grocy.helper.StoreOfflineDataShoppingListHelper;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListBottomNotes;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;

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

public class ShoppingActivity extends AppCompatActivity implements
        ShoppingListItemSpecialAdapter.ShoppingListItemSpecialAdapterListener,
        LoadOfflineDataShoppingListHelper.AsyncResponse,
        StoreOfflineDataShoppingListHelper.AsyncResponse {

    private final static boolean DEBUG = true;
    private final static String TAG = "ShoppingActivity";

    private SharedPreferences sharedPrefs;
    private DownloadHelper downloadHelper;
    private Timer timer;
    private AppDatabase database;

    private ArrayList<ShoppingListItem> shoppingListItems;
    private ArrayList<ShoppingListItem> shoppingListItemsSelected;
    private ArrayList<ShoppingList> shoppingLists;
    private ArrayList<ProductGroup> productGroups;
    private ArrayList<QuantityUnit> quantityUnits;
    private ArrayList<MissingItem> missingItems;
    private ArrayList<Product> products;
    private ArrayList<GroupedListItem> groupedListItems;
    private HashMap<Integer, ShoppingList> shoppingListHashMap;

    private int selectedShoppingListId = 1;
    private boolean showOffline;
    private boolean isDataStored;

    private ActivityShoppingBinding binding;
    private ShoppingListItemSpecialAdapter shoppingListItemAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle startupBundle = getIntent().getBundleExtra(Constants.ARGUMENT.BUNDLE);
        if(startupBundle != null) {
            selectedShoppingListId = startupBundle.getInt(Constants.ARGUMENT.SELECTED_ID);
        }

        // PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // DATABASE

        database = AppDatabase.getAppDatabase(getApplicationContext());

        // WEB

        downloadHelper = new DownloadHelper(
                this,
                TAG,
                this::onDownloadError,
                this::onQueueEmpty
        );

        // INITIALIZE VARIABLES

        shoppingLists = new ArrayList<>();
        shoppingListItems = new ArrayList<>();
        shoppingListItemsSelected = new ArrayList<>();
        missingItems = new ArrayList<>();
        quantityUnits = new ArrayList<>();
        products = new ArrayList<>();
        productGroups = new ArrayList<>();
        groupedListItems = new ArrayList<>();
        shoppingListHashMap = new HashMap<>();

        // VIEWS

        binding = ActivityShoppingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        binding.recycler.setAdapter(new StockPlaceholderAdapter());

        load();
    }

    @Override
    protected void onPause() {
        super.onPause();
        timer.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        timer = new Timer();

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if(!downloadHelper.isQueueEmpty()) return;
                downloadShoppingListItems();
            }
        };
        timer.schedule(timerTask, 10000, 10000);
    }

    private void load() {
        if(isOnline()) {
            downloadFull();
        } else {
            showOffline = true;
            new LoadOfflineDataShoppingListHelper(this, this).execute();
        }
    }

    public void refresh() {
        if(isOnline()) {
            downloadFull();
            // TODO: Reset timer
        } else {
            binding.swipe.setRefreshing(false);
            if(!showOffline) {
                showOffline = true;
                new LoadOfflineDataShoppingListHelper(this, this).execute();
            }
            showMessage(getString(R.string.msg_no_connection));
        }
    }

    private void downloadFull() {
        binding.swipe.setRefreshing(true);
        downloadHelper.downloadQuantityUnits(quantityUnits -> this.quantityUnits = quantityUnits);
        downloadHelper.downloadProducts(products -> this.products = products);
        downloadHelper.downloadProductGroups(productGroups -> this.productGroups = productGroups);
        downloadHelper.downloadShoppingListItems(listItems -> this.shoppingListItems = listItems);
        downloadHelper.downloadShoppingLists(shoppingLists -> {
            this.shoppingLists = shoppingLists;
            changeAppBarTitle();
        });
        downloadHelper.downloadVolatile((expiring, expired, missing) -> missingItems = missing);
    }

    private void downloadShoppingListItems() {
        downloadHelper.downloadShoppingListItems(listItems -> this.shoppingListItems = listItems);
    }

    private void onQueueEmpty() {
        if(showOffline) showOffline = false;

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
            // sync modified data and store new data
            new StoreOfflineDataShoppingListHelper(
                    this,
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

            // set product in shoppingListItem
            HashMap<Integer, Product> productHashMap = new HashMap<>();
            for(Product p : products) productHashMap.put(p.getId(), p);
            for(ShoppingListItem shoppingListItem : shoppingListItemsSelected) {
                if(shoppingListItem.getProductId() == null) continue;
                shoppingListItem.setProduct(
                        productHashMap.get(Integer.parseInt(shoppingListItem.getProductId()))
                );
            }

            binding.swipe.setRefreshing(false);
            groupItems();
        }
    }

    private void onDownloadError(VolleyError error) {
        binding.swipe.setRefreshing(false);
        if(!showOffline) {
            showOffline = true;
        }
        new LoadOfflineDataShoppingListHelper(this, this).execute();
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

        shoppingListItemsSelected = new ArrayList<>();

        for(ShoppingListItem shoppingListItem : shoppingListItems) {
            if(shoppingListItem.getShoppingListId() != selectedShoppingListId) continue;
            if(shoppingListItem.isUndone()) {
                shoppingListItemsSelected.add(shoppingListItem);
            }
        }

        changeAppBarTitle();

        groupItems();
    }

    private void groupItems() {
        ArrayList<ProductGroup> neededProductGroups = new ArrayList<>();
        boolean containsUngroupedItems = false;
        for(ShoppingListItem shoppingListItem : shoppingListItemsSelected) {
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
                    getString(R.string.title_shopping_list_ungrouped)
            ));
        }
        groupedListItems = new ArrayList<>();
        for(ProductGroup productGroup : neededProductGroups) {
            groupedListItems.add(productGroup);
            ArrayList<ShoppingListItem> itemsOneGroup = new ArrayList<>();
            for(ShoppingListItem shoppingListItem : shoppingListItemsSelected) {
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

        // add bottom notes if they are not empty
        ShoppingList shoppingList = getShoppingList(selectedShoppingListId);
        Spanned notes = shoppingList != null && shoppingList.getNotes() != null
                ? (Spanned) TextUtil
                .trimCharSequence(Html.fromHtml(shoppingList.getNotes().trim()))
                : null;
        if(shoppingList != null && notes != null && !notes.toString().trim().isEmpty()) {
            groupedListItems.add(
                    new ProductGroup(-1, getString(R.string.property_notes))
            );
            groupedListItems.add(new ShoppingListBottomNotes(notes));
        }

        refreshAdapter(
                new ShoppingListItemSpecialAdapter(
                        this,
                        groupedListItems,
                        quantityUnits,
                        this
                )
        );
    }

    private void refreshAdapter(ShoppingListItemSpecialAdapter adapter) {
        shoppingListItemAdapter = adapter;
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
            HashMap<Integer, ShoppingListItem> serverItemHashMap
    ) {
        downloadHelper.setOnQueueEmptyListener(() -> {
            showMessage("Successfully synced entries");
            new StoreOfflineDataShoppingListHelper(
                    this,
                    this,
                    false,
                    shoppingLists,
                    shoppingListItems,
                    productGroups,
                    quantityUnits,
                    products,
                    usedProductIds
            ).execute();
            downloadHelper.setOnQueueEmptyListener(this::onQueueEmpty);
        });
        for(ShoppingListItem itemToSync : itemsToSync) {
            JSONObject body = new JSONObject();
            try {
                body.put("done", itemToSync.getDone());
            } catch (JSONException e) {
                Log.e(TAG, "syncItems: " + e);
            }
            downloadHelper.editShoppingListItem(
                    itemToSync.getId(),
                    body,
                    response -> {
                        ShoppingListItem serverItem = serverItemHashMap.get(itemToSync.getId());
                        if(serverItem != null) serverItem.setDone(itemToSync.getDone());
                    },
                    error -> {
                        showMessage("Failed to sync items"); // TODO
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
        toggleDoneStatus(position);
    }

    private void changeAppBarTitle() {
        // change app bar title to shopping list name
        /*ShoppingList shoppingList = getShoppingList(selectedShoppingListId);
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
        }*/
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
                    showMessage("You're now offline");
                    showOffline = true;
                    updateDoneStatus(shoppingListItem, position);
                }
        );
    }

    private void updateDoneStatus(ShoppingListItem shoppingListItem, int position) {
        new Thread(() -> database.shoppingListItemDao().update(shoppingListItem)).start();
        if(shoppingListItem.getDone() == 1) {
            shoppingListItemsSelected.remove(shoppingListItem);
        } else {
            shoppingListItemsSelected = new ArrayList<>();
            for(ShoppingListItem shoppingListItem1 : shoppingListItems) {
                if(shoppingListItem1.getShoppingListId() != selectedShoppingListId) {
                    continue;
                }
                if(shoppingListItem1.getDone() == 0) {
                    shoppingListItemsSelected.add(shoppingListItem1);
                }
            }
        }
        removeItemFromList(position);
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

    private ShoppingList getShoppingList(int shoppingListId) {
        if(shoppingListHashMap.isEmpty()) {
            for(ShoppingList s : shoppingLists) shoppingListHashMap.put(s.getId(), s);
        }
        return shoppingListHashMap.get(shoppingListId);
    }

    public boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE
        );
        assert connectivityManager != null;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    private void showMessage(String msg) {
        Snackbar.make(binding.scroll, msg, Snackbar.LENGTH_SHORT).show();
    }
}