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
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.ScanBatchActivity;
import xyz.zedler.patrick.grocy.adapter.MissingBatchItemAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.model.BatchPurchaseEntry;
import xyz.zedler.patrick.grocy.model.CreateProduct;
import xyz.zedler.patrick.grocy.model.MissingBatchItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.util.BitmapUtil;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class MissingBatchItemsFragment extends Fragment implements MissingBatchItemAdapter.MissingBatchItemAdapterListener {

    private final static String TAG = Constants.UI.MISSING_BATCH_ITEMS;
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private GrocyApi grocyApi;
    private WebRequest request;
    private Gson gson = new Gson();
    private MissingBatchItemAdapter missingBatchItemAdapter;
    private ClickUtil clickUtil = new ClickUtil();

    private ArrayList<MissingBatchItem> missingBatchItems;

    private SwipeRefreshLayout swipeRefreshLayout;
    private NestedScrollView scrollView;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_missing_batch_items, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (MainActivity) getActivity();
        assert activity != null;

        // WEB REQUESTS

        request = new WebRequest(activity.getRequestQueue());
        grocyApi = activity.getGrocy();

        // INITIALIZE VIEWS

        activity.findViewById(R.id.frame_missing_batch_items_back).setOnClickListener(
                v -> activity.onBackPressed()
        );
        swipeRefreshLayout = activity.findViewById(R.id.swipe_missing_batch_items);
        scrollView = activity.findViewById(R.id.scroll_missing_batch_items);
        RecyclerView recyclerView = activity.findViewById(R.id.recycler_missing_batch_items);

        if(getArguments() == null ||
                getArguments().getParcelableArrayList(Constants.ARGUMENT.BATCH_ITEMS) == null
        ) {
            setError(true, false, false);
            activity.findViewById(R.id.button_error_retry).setVisibility(View.GONE);
        } else {
            missingBatchItems = getArguments().getParcelableArrayList(
                    Constants.ARGUMENT.BATCH_ITEMS
            );
        }

        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        activity,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        missingBatchItemAdapter = new MissingBatchItemAdapter(missingBatchItems, this);
        recyclerView.setAdapter(missingBatchItemAdapter);

        // SWIPE REFRESH

        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        // UPDATE UI

        activity.updateUI(Constants.UI.MISSING_BATCH_ITEMS, TAG);
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
                true,
                () -> {
                    if(activity.getCurrentFragment().getClass()== MissingBatchItemsFragment.class) {
                        ((MissingBatchItemsFragment) activity.getCurrentFragment())
                                .doOnePurchaseRequest();
                    }
                }
        );
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
        swipeRefreshLayout.setRefreshing(true);
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
                response -> {
                    ArrayList<Product> products = gson.fromJson(
                            response,
                            new TypeToken<List<Product>>(){}.getType()
                    );
                    if(DEBUG) Log.i(TAG, "refresh: products = " + products);

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
                    swipeRefreshLayout.setRefreshing(false);
                },
                error -> {
                    swipeRefreshLayout.setRefreshing(false);
                    showMessage(activity.getString(R.string.msg_error));
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
        activity.setFabIcon(
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
        );
    }

    private void doOnePurchaseRequest() {
        if(getReadyPurchaseEntriesSize() == 0) {
            showMessage(activity.getString(R.string.msg_no_purchase_transactions));
            return;
        }

        MissingBatchItem missingBatchItem = missingBatchItems.get(0);
        ArrayList<BatchPurchaseEntry> batchPurchaseEntries = missingBatchItem.getPurchaseEntries();
        BatchPurchaseEntry batchPurchaseEntry = batchPurchaseEntries.get(0);

        purchaseProduct(
                Integer.parseInt(missingBatchItem.getProductId()),
                batchPurchaseEntry.getLocationId(),
                batchPurchaseEntry.getStoreId(),
                batchPurchaseEntry.getPrice(),
                batchPurchaseEntry.getBestBeforeDate(),
                response -> {
                    batchPurchaseEntries.remove(0);
                    if(batchPurchaseEntries.isEmpty()) {
                        missingBatchItems.remove(0);
                        missingBatchItemAdapter.notifyItemRemoved(0);
                    }
                    updateFab();
                    if(getReadyPurchaseEntriesSize() == 0) {
                        showMessage(
                                activity.getString(R.string.msg_purchase_transactions_done)
                        );
                    } else {
                        doOnePurchaseRequest();
                    }
                },
                error -> showMessage(activity.getString(R.string.msg_error))
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
            if(DEBUG) Log.e(TAG, "purchaseProduct: " + e);
        }
        request.post(
                grocyApi.purchaseProduct(productId),
                body,
                responseListener::onResponse,
                errorListener::onError
        );
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

    private void showMessage(String msg) {
        activity.showMessage(
                Snackbar.make(
                        activity.findViewById(R.id.linear_container_main),
                        msg,
                        Snackbar.LENGTH_SHORT
                )
        );
    }

    @Override
    public void onItemRowClicked(int position) {
        if(clickUtil.isDisabled()) return;

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
            request.get(
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
                    error -> showMessage(activity.getString(R.string.msg_error))
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
