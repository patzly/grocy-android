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
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.activity.ScanBatchActivity;
import xyz.zedler.patrick.grocy.adapter.MissingBatchItemAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.FragmentMissingBatchItemsBinding;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.BatchPurchaseEntry;
import xyz.zedler.patrick.grocy.model.CreateProduct;
import xyz.zedler.patrick.grocy.model.MissingBatchItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.util.AnimUtil;
import xyz.zedler.patrick.grocy.util.BitmapUtil;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class MissingBatchItemsFragment extends Fragment
        implements MissingBatchItemAdapter.MissingBatchItemAdapterListener {

    private final static String TAG = Constants.UI.MISSING_BATCH_ITEMS;

    private MainActivity activity;
    private GrocyApi grocyApi;
    private DownloadHelper dlHelper;
    private Gson gson;
    private ClickUtil clickUtil;
    private AnimUtil animUtil;
    private MissingBatchItemAdapter missingBatchItemAdapter;
    private FragmentMissingBatchItemsBinding binding;

    private ArrayList<MissingBatchItem> missingBatchItems;

    private String errorState;
    private boolean debug;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentMissingBatchItemsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(binding != null) {
            binding.recyclerMissingBatchItems.animate().cancel();
            binding.recyclerMissingBatchItems.setAdapter(null);
            binding = null;
        }
        dlHelper.destroy();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if(isHidden()) return;

        activity = (MainActivity) getActivity();
        assert activity != null;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        // UTILS

        clickUtil = new ClickUtil();
        animUtil = new AnimUtil();

        // WEB

        dlHelper = new DownloadHelper(activity, TAG);
        grocyApi = activity.getGrocy();
        gson = new Gson();

        // VIEWS

        binding.frameMissingBatchItemsBack.setOnClickListener(v -> activity.onBackPressed());

        if(getArguments() == null ||
                getArguments().getParcelableArrayList(Constants.ARGUMENT.BATCH_ITEMS) == null
        ) {
            setError(Constants.STATE.ERROR, false);
            missingBatchItems = new ArrayList<>();
        } else {
            missingBatchItems = getArguments().getParcelableArrayList(
                    Constants.ARGUMENT.BATCH_ITEMS
            );
        }

        binding.recyclerMissingBatchItems.setLayoutManager(
                new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        );
        binding.recyclerMissingBatchItems.setItemAnimator(new DefaultItemAnimator());

        // SWIPE REFRESH

        binding.swipeMissingBatchItems.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipeMissingBatchItems.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        binding.swipeMissingBatchItems.setOnRefreshListener(this::refresh);

        if(savedInstanceState != null) restoreSavedInstanceState(savedInstanceState);

        // FILL WITH ITEMS

        missingBatchItemAdapter = new MissingBatchItemAdapter(missingBatchItems, this);
        binding.recyclerMissingBatchItems.setAdapter(missingBatchItemAdapter);

        // UPDATE UI

        activity.updateUI(
                Constants.UI.MISSING_BATCH_ITEMS,
                savedInstanceState == null,
                TAG
        );
        activity.updateFab(
                new BitmapDrawable(
                        getResources(),
                        BitmapUtil.getFromDrawableWithNumber(
                                activity,
                                R.drawable.ic_round_shopping_cart,
                                getReadyPurchaseEntriesSize(),
                                7.3f,
                                -1.5f,
                                8
                        )
                ),
                R.string.action_perform_purchasing_processes,
                Constants.FAB.TAG.PURCHASE,
                savedInstanceState == null,
                () -> {
                    if(activity.getCurrentFragment().getClass()== MissingBatchItemsFragment.class) {
                        ((MissingBatchItemsFragment) activity.getCurrentFragment())
                                .doOnePurchaseRequest();
                    }
                }
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(isHidden()) return;

        outState.putString("errorState", errorState);
    }

    private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
        if(isHidden()) return;

        errorState = savedInstanceState.getString("errorState", Constants.STATE.NONE);
        setError(errorState, false);

        binding.swipeMissingBatchItems.setRefreshing(false);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if(!hidden && getView() != null) onViewCreated(getView(), null);
    }

    public void createdOrEditedProduct(Bundle bundle) {
        if(bundle == null) return;

        String intendedAction = bundle.getString(Constants.ARGUMENT.TYPE);
        if(intendedAction == null) return;

        switch (intendedAction) {
            case Constants.ACTION.CREATE_THEN_PURCHASE_BATCH: {
                CreateProduct oldCreateProduct = bundle.getParcelable(
                        Constants.ARGUMENT.CREATE_PRODUCT_OBJECT
                );
                assert oldCreateProduct != null;
                MissingBatchItem missingBatchItem = getMissingBatchItemFromName(
                        oldCreateProduct.getProductName()
                );
                String productName = bundle.getString(Constants.ARGUMENT.PRODUCT_NAME);
                assert missingBatchItem != null;
                missingBatchItem.setProductName(productName);
                missingBatchItem.setIsOnServer(true);
                missingBatchItem.setProductId(bundle.getInt(Constants.ARGUMENT.PRODUCT_ID));
                missingBatchItemAdapter.notifyItemChanged(
                        missingBatchItems.indexOf(missingBatchItem)
                );
                updateFab();
                break;
            }
            case Constants.ACTION.EDIT_THEN_PURCHASE_BATCH: {
                int productId = bundle.getInt(Constants.ARGUMENT.PRODUCT_ID);
                String productName = bundle.getString(Constants.ARGUMENT.PRODUCT_NAME);
                MissingBatchItem missingBatchItem = getMissingBatchItemFromProductId(productId);
                if (missingBatchItem != null && productName != null) {
                    missingBatchItem.setProductName(productName);
                    int index = getIndexOfMissingBatchItemFromProductId(productId);
                    if (index > -1) missingBatchItemAdapter.notifyItemChanged(index);
                }
                break;
            }
            case Constants.ACTION.DELETE_THEN_PURCHASE_BATCH: {
                int productId = bundle.getInt(Constants.ARGUMENT.PRODUCT_ID);
                MissingBatchItem missingBatchItem = getMissingBatchItemFromProductId(productId);
                if (missingBatchItem != null) {
                    missingBatchItem.setIsOnServer(false);
                    int index = getIndexOfMissingBatchItemFromProductId(productId);
                    if (index > -1) missingBatchItemAdapter.notifyItemChanged(index);
                }
                break;
            }
        }
    }

    private void refresh() {
        binding.swipeMissingBatchItems.setRefreshing(true);
        dlHelper.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
                response -> {
                    ArrayList<Product> products = gson.fromJson(
                            response,
                            new TypeToken<List<Product>>(){}.getType()
                    );
                    if(debug) Log.i(TAG, "refresh: products = " + products);

                    ArrayList<String> missingBatchItemsNames = new ArrayList<>();
                    for(MissingBatchItem missingBatchItem : missingBatchItems) {
                        missingBatchItemsNames.add(missingBatchItem.getProductName());
                    }
                    for(Product product : products) {
                        if(!missingBatchItemsNames.contains(product.getName())) continue;

                        for(MissingBatchItem missingBatchItem: missingBatchItems) {
                            if(missingBatchItem.getProductName().equals(product.getName())) {
                                missingBatchItem.setIsOnServer(true);
                                missingBatchItem.setProductId(product.getId());
                                missingBatchItemAdapter.notifyItemChanged(
                                        missingBatchItems.indexOf(missingBatchItem)
                                );
                            }
                        }
                    }
                    updateFab();
                    binding.swipeMissingBatchItems.setRefreshing(false);
                },
                error -> {
                    binding.swipeMissingBatchItems.setRefreshing(false);
                    showMessage(activity.getString(R.string.error_undefined));
                }
        );
    }

    public int getMissingBatchItemsSize() {
        return missingBatchItems.size();
    }

    private int getReadyPurchaseEntriesSize() {
        int readyPurchaseEntriesSize = 0;
        for(MissingBatchItem missingBatchItem : missingBatchItems) {
            if(missingBatchItem.getIsOnServer()) {
                readyPurchaseEntriesSize += missingBatchItem.getPurchaseEntriesSize();
            }
        }
        return readyPurchaseEntriesSize;
    }

    private MissingBatchItem getMissingBatchItemFromName(String productName) {
        for(MissingBatchItem missingBatchItem : missingBatchItems) {
            if(missingBatchItem.getProductName().equals(productName)) {
                return missingBatchItem;
            }
        }
        return null;
    }

    private MissingBatchItem getMissingBatchItemFromProductId(int productId) {
        for(MissingBatchItem missingBatchItem : missingBatchItems) {
            if(missingBatchItem.getProductId() != null
                    && missingBatchItem.getProductId().equals(String.valueOf(productId))
            ) {
                return missingBatchItem;
            }
        }
        return null;
    }

    private int getIndexOfMissingBatchItemFromProductId(int productId) {
        for(int i = 0; i < missingBatchItems.size(); i++) {
            if(missingBatchItems.get(i).getProductId() != null
                    && missingBatchItems.get(i).getProductId().equals(String.valueOf(productId))
            ) {
                return i;
            }
        }
        return -1;
    }

    private void updateFab() {
        new Handler().postDelayed(() -> activity.setFabIcon(
                new BitmapDrawable(
                        getResources(),
                        BitmapUtil.getFromDrawableWithNumber(
                                activity,
                                R.drawable.ic_round_shopping_cart,
                                getReadyPurchaseEntriesSize(),
                                7.3f,
                                -1.5f,
                                8
                        )
                )
        ), 500);
    }

    private void doOnePurchaseRequest() {
        if(getReadyPurchaseEntriesSize() == 0) {
            showMessage(activity.getString(R.string.msg_no_purchase_transactions));
            return;
        }

        if(missingBatchItems.size() < 1) {
            showMessage(activity.getString(R.string.error_undefined));
            if(debug) Log.e(TAG, "doOnePurchaseRequest: missingBatchItems are empty");
            return;
        }

        int indexNextOnServer = 0;
        while(indexNextOnServer < missingBatchItems.size()-1 && !missingBatchItems.get(indexNextOnServer).getIsOnServer()) {
            indexNextOnServer++;
        }

        MissingBatchItem missingBatchItem = missingBatchItems.get(indexNextOnServer);
        ArrayList<BatchPurchaseEntry> batchPurchaseEntries = missingBatchItem.getPurchaseEntries();

        if(batchPurchaseEntries.size() < 1) {
            showMessage(activity.getString(R.string.error_undefined));
            if(debug) Log.e(TAG, "doOnePurchaseRequest: batchPurchaseEntries are empty");
            return;
        }

        BatchPurchaseEntry batchPurchaseEntry = batchPurchaseEntries.get(0);

        // TODO: There should be an error in the UI
        if(missingBatchItem.getProductId() == null) return;

        final int indexNextOnServerFinal = indexNextOnServer;
        purchaseProduct(
                Integer.parseInt(missingBatchItem.getProductId()),
                batchPurchaseEntry.getLocationId(),
                batchPurchaseEntry.getStoreId(),
                batchPurchaseEntry.getPrice(),
                batchPurchaseEntry.getBestBeforeDate(),
                response -> {
                    if(batchPurchaseEntries.isEmpty()) return;
                    batchPurchaseEntries.remove(0);
                    if(batchPurchaseEntries.isEmpty() && !missingBatchItems.isEmpty()) {
                        missingBatchItems.remove(indexNextOnServerFinal);
                        missingBatchItemAdapter.notifyItemRemoved(indexNextOnServerFinal);
                    }
                    updateFab();
                    if(getReadyPurchaseEntriesSize() == 0) {
                        showMessage(activity.getString(R.string.msg_purchase_transactions_done));
                    } else {
                        doOnePurchaseRequest();
                    }
                },
                error -> showMessage(activity.getString(R.string.error_undefined))
        );
    }

    private void purchaseProduct(
            int productId,
            String locationId,
            String storeId,
            String price,
            String bestBeforeDate,
            OnResponseListener responseListener,
            OnErrorListener errorListener
    ) {
        JSONObject body = new JSONObject();
        try {
            body.put("amount", 1);
            body.put("transaction_type", "purchase");
            if(bestBeforeDate != null && !bestBeforeDate.isEmpty()) {
                body.put("best_before_date", bestBeforeDate);
            }
            if(price != null && !price.isEmpty()) {
                body.put("price", NumUtil.formatPrice(price));
            }
            if(storeId != null && !storeId.isEmpty() && Integer.parseInt(storeId) > -1) {
                body.put("shopping_location_id", Integer.parseInt(storeId));
            }
            if(locationId != null && !locationId.isEmpty()) {
                body.put("location_id", locationId);
            }
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "purchaseProduct: " + e);
        }
        dlHelper.post(
                grocyApi.purchaseProduct(productId),
                body,
                responseListener::onResponse,
                errorListener::onError
        );
    }

    private void setError(String state, boolean animated) {
        errorState = state;

        binding.linearError.buttonErrorRetry.setVisibility(View.GONE);

        View viewIn = binding.linearError.linearError;
        View viewOut = binding.scrollMissingBatchItems;

        switch (state) {
            case Constants.STATE.OFFLINE:
                binding.linearError.imageError.setImageResource(R.drawable.illustration_broccoli);
                binding.linearError.textErrorTitle.setText(R.string.error_offline);
                binding.linearError.textErrorSubtitle.setText(R.string.error_offline_subtitle);
                break;
            case Constants.STATE.ERROR:
                binding.linearError.imageError.setImageResource(R.drawable.illustration_popsicle);
                binding.linearError.textErrorTitle.setText(R.string.error_unknown);
                binding.linearError.textErrorSubtitle.setText(R.string.error_undefined);
                break;
            case Constants.STATE.NONE:
                viewIn = binding.scrollMissingBatchItems;
                viewOut = binding.linearError.linearError;
                break;
        }

        animUtil.replaceViews(viewIn, viewOut, animated);
    }

    private void showMessage(String msg) {
        activity.showMessage(
                Snackbar.make(activity.binding.frameMainContainer, msg, Snackbar.LENGTH_SHORT)
        );
    }

    @Override
    public void onItemRowClicked(int position) {
        if(clickUtil.isDisabled()) return;

        if(position >= missingBatchItems.size()) {
            showMessage(activity.getString(R.string.error_undefined));
            if(debug) Log.e(
                    TAG, "onItemRowClicked: size = "
                            + missingBatchItems.size()
                            + ", position = " + position
            );
            return;
        }

        MissingBatchItem batchItem = missingBatchItems.get(position);

        Bundle bundle = new Bundle();

        if(!batchItem.getIsOnServer()) {
            CreateProduct createProduct = new CreateProduct(
                    batchItem.getProductName(),
                    batchItem.getBarcodes(),
                    batchItem.getDefaultStoreId(),
                    String.valueOf(batchItem.getDefaultBestBeforeDays()),
                    String.valueOf(batchItem.getDefaultLocationId())
            );
            bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.CREATE_THEN_PURCHASE_BATCH);
            bundle.putParcelable(Constants.ARGUMENT.CREATE_PRODUCT_OBJECT, createProduct);
            activity.replaceFragment(Constants.UI.MASTER_PRODUCT_SIMPLE, bundle, true);
        } else {
            dlHelper.get(
                    grocyApi.getStockProductDetails(Integer.parseInt(batchItem.getProductId())),
                    response -> {
                        ProductDetails productDetails = gson.fromJson(
                                response,
                                new TypeToken<ProductDetails>(){}.getType()
                        );
                        bundle.putString(
                                Constants.ARGUMENT.TYPE,
                                Constants.ACTION.EDIT_THEN_PURCHASE_BATCH
                        );
                        bundle.putParcelable(
                                Constants.ARGUMENT.PRODUCT,
                                productDetails.getProduct()
                        );
                        activity.replaceFragment(
                                Constants.UI.MASTER_PRODUCT_SIMPLE,
                                bundle,
                                true
                        );
                    },
                    error -> {
                        showMessage(activity.getString(R.string.error_undefined));
                        if(debug) Log.e(TAG, "onItemRowClicked: " + error);
                    }
            );
        }
    }

    public void setUpBottomMenu() {
        MenuItem menuItemScan;
        menuItemScan = activity.getBottomMenu().findItem(R.id.action_scan);
        menuItemScan.setOnMenuItemClickListener(item -> {
            activity.dismissFragments();
            Intent intent = new Intent(activity, ScanBatchActivity.class);
            intent.putExtra(Constants.ARGUMENT.TYPE, Constants.ACTION.PURCHASE);
            intent.putExtra(Constants.ARGUMENT.BUNDLE, getArguments());
            activity.startActivityForResult(intent, Constants.REQUEST.SCAN_BATCH);
            return true;
        });
    }

    public interface OnResponseListener {
        void onResponse(JSONObject response);
    }

    public interface OnErrorListener {
        void onError(VolleyError error);
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
