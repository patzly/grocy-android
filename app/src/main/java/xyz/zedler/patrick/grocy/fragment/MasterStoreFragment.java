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

import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterStoreBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class MasterStoreFragment extends Fragment {

    private final static String TAG = Constants.UI.MASTER_STORE;
    private final static boolean DEBUG = false;

    private MainActivity activity;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private WebRequest request;
    private FragmentMasterStoreBinding binding;

    private ArrayList<Store> stores;
    private ArrayList<Product> products;
    private ArrayList<String> storeNames;
    private Store editStore;

    private boolean isRefresh;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentMasterStoreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (MainActivity) getActivity();
        assert activity != null;

        // WEB REQUESTS

        request = new WebRequest(activity.getRequestQueue());
        grocyApi = activity.getGrocy();

        // INITIALIZE VARIABLES

        stores = new ArrayList<>();
        products = new ArrayList<>();
        storeNames = new ArrayList<>();

        editStore = null;
        isRefresh = false;

        // INITIALIZE VIEWS

        binding.frameMasterStoreCancel.setOnClickListener(v -> activity.onBackPressed());

        // swipe refresh
        binding.swipeMasterStore.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipeMasterStore.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        binding.swipeMasterStore.setOnRefreshListener(this::refresh);

        // name
        binding.editTextMasterStoreName.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) IconUtil.start(binding.imageMasterStoreName);
        });

        // description
        binding.editTextMasterStoreDescription.setOnFocusChangeListener(
                (View v, boolean hasFocus) -> {
                    if(hasFocus) IconUtil.start(binding.imageMasterStoreDescription);
                });

        // BUNDLE WHEN EDIT

        Bundle bundle = getArguments();
        if(bundle != null) {
            editStore = bundle.getParcelable(Constants.ARGUMENT.STORE);
            // FILL
            if(editStore != null) {
                fillWithEditReferences();
            } else {
                resetAll();
            }
        } else {
            resetAll();
        }

        // START

        if(savedInstanceState == null) {
            load();
        } else {
            restoreSavedInstanceState(savedInstanceState);
        }

        // UPDATE UI

        activity.updateUI(
                Constants.UI.MASTER_STORE,
                savedInstanceState == null,
                TAG
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(!isHidden()) {
            outState.putParcelableArrayList("stores", stores);
            outState.putParcelableArrayList("products", products);
            outState.putStringArrayList("storeNames", storeNames);

            outState.putParcelable("editStore", editStore);
        }
        super.onSaveInstanceState(outState);
    }

    private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
        if(isHidden()) return;

        stores = savedInstanceState.getParcelableArrayList("stores");
        products = savedInstanceState.getParcelableArrayList("products");
        storeNames = savedInstanceState.getStringArrayList("storeNames");

        editStore = savedInstanceState.getParcelable("editStore");

        isRefresh = false;
        binding.swipeMasterStore.setRefreshing(false);

        updateEditReferences();

        if(isRefresh && editStore != null) {
            fillWithEditReferences();
        } else {
            resetAll();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if(!hidden) onActivityCreated(null);
    }

    private void load() {
        if(activity.isOnline()) {
            download();
        }
    }

    private void refresh() {
        // for only fill with up-to-date data on refresh,
        // not on startup as the bundle should contain everything needed
        isRefresh = true;
        if(activity.isOnline()) {
            download();
        } else {
            binding.swipeMasterStore.setRefreshing(false);
            activity.showMessage(
                    Snackbar.make(
                            activity.findViewById(R.id.frame_main_container),
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

    private void download() {
        binding.swipeMasterStore.setRefreshing(true);
        downloadStores();
        downloadProducts();
    }

    private void downloadStores() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.STORES),
                response -> {
                    stores = gson.fromJson(
                            response,
                            new TypeToken<List<Store>>(){}.getType()
                    );
                    SortUtil.sortStoresByName(stores, true);
                    storeNames = getStoreNames();

                    binding.swipeMasterStore.setRefreshing(false);

                    updateEditReferences();

                    if(isRefresh && editStore != null) {
                        fillWithEditReferences();
                    } else {
                        resetAll();
                    }
                },
                error -> {
                    binding.swipeMasterStore.setRefreshing(false);
                    activity.showMessage(
                            Snackbar.make(
                                    activity.findViewById(R.id.frame_main_container),
                                    activity.getString(R.string.msg_error),
                                    Snackbar.LENGTH_SHORT
                            ).setActionTextColor(
                                    ContextCompat.getColor(activity, R.color.secondary)
                            ).setAction(
                                    activity.getString(R.string.action_retry),
                                    v1 -> download()
                            )
                    );
                }
        );
    }

    private void downloadProducts() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
                response -> products = gson.fromJson(
                        response,
                        new TypeToken<List<Product>>(){}.getType()
                ), error -> {}
        );
    }

    private void updateEditReferences() {
        if(editStore != null) {
            Store editStore = getStore(this.editStore.getId());
            if(editStore != null) this.editStore = editStore;
        }
    }

    private ArrayList<String> getStoreNames() {
        ArrayList<String> names = new ArrayList<>();
        if(stores != null) {
            for(Store store : stores) {
                if(editStore != null) {
                    if(store.getId() != editStore.getId()) {
                        names.add(store.getName().trim());
                    }
                } else {
                    names.add(store.getName().trim());
                }
            }
        }
        return names;
    }

    private Store getStore(int storeId) {
        for(Store store : stores) {
            if(store.getId() == storeId) {
                return store;
            }
        } return null;
    }

    private void fillWithEditReferences() {
        clearInputFocusAndErrors();
        if(editStore != null) {
            // name
            binding.editTextMasterStoreName.setText(editStore.getName());
            // description
            binding.editTextMasterStoreDescription.setText(editStore.getDescription());
        }
    }

    private void clearInputFocusAndErrors() {
        activity.hideKeyboard();
        binding.textInputMasterStoreName.clearFocus();
        binding.textInputMasterStoreName.setErrorEnabled(false);
        binding.textInputMasterStoreDescription.clearFocus();
        binding.textInputMasterStoreDescription.setErrorEnabled(false);
    }

    public void saveStore() {
        if(isFormInvalid()) return;

        JSONObject jsonObject = new JSONObject();
        try {
            Editable name = binding.editTextMasterStoreName.getText();
            Editable description = binding.editTextMasterStoreDescription.getText();
            jsonObject.put("name", (name != null ? name : "").toString().trim());
            jsonObject.put(
                    "description", (description != null ? description : "").toString().trim()
            );
        } catch (JSONException e) {
            Log.e(TAG, "saveStore: " + e);
        }
        if(editStore != null) {
            request.put(
                    grocyApi.getObject(GrocyApi.ENTITY.STORES, editStore.getId()),
                    jsonObject,
                    response -> activity.dismissFragment(),
                    error -> {
                        showErrorMessage();
                        Log.e(TAG, "saveStore: " + error);
                    }
            );
        } else {
            request.post(
                    grocyApi.getObjects(GrocyApi.ENTITY.STORES),
                    jsonObject,
                    response -> activity.dismissFragment(),
                    error -> {
                        showErrorMessage();
                        Log.e(TAG, "saveStore: " + error);
                    }
            );
        }
    }

    private boolean isFormInvalid() {
        clearInputFocusAndErrors();
        boolean isInvalid = false;

        String name = String.valueOf(binding.editTextMasterStoreName.getText()).trim();
        if(name.isEmpty()) {
            binding.textInputMasterStoreName.setError(activity.getString(R.string.error_empty));
            isInvalid = true;
        } else if(!storeNames.isEmpty() && storeNames.contains(name)) {
            binding.textInputMasterStoreName.setError(activity.getString(R.string.error_duplicate));
            isInvalid = true;
        }

        return isInvalid;
    }

    private void resetAll() {
        if(editStore != null) return;
        clearInputFocusAndErrors();
        binding.editTextMasterStoreName.setText(null);
        binding.editTextMasterStoreDescription.setText(null);
    }

    public void checkForUsage(Store store) {
        if(!products.isEmpty()) {
            for(Product product : products) {
                if(product.getStoreId() == null) continue;
                if(product.getStoreId().equals(String.valueOf(store.getId()))) {
                    activity.showMessage(
                            Snackbar.make(
                                    activity.findViewById(R.id.frame_main_container),
                                    activity.getString(
                                            R.string.msg_master_delete_usage,
                                            activity.getString(R.string.type_store)
                                    ),
                                    Snackbar.LENGTH_LONG
                            )
                    );
                    return;
                }
            }
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.ARGUMENT.STORE, store);
        bundle.putString(Constants.ARGUMENT.TYPE, Constants.ARGUMENT.STORE);
        activity.showBottomSheet(new MasterDeleteBottomSheetDialogFragment(), bundle);
    }

    public void deleteStore(Store store) {
        request.delete(
                grocyApi.getObject(GrocyApi.ENTITY.STORES, store.getId()),
                response -> activity.dismissFragment(),
                error -> showErrorMessage()
        );
    }

    private void showErrorMessage() {
        activity.showMessage(
                Snackbar.make(
                        activity.findViewById(R.id.frame_main_container),
                        activity.getString(R.string.msg_error),
                        Snackbar.LENGTH_SHORT
                )
        );
    }

    public void setUpBottomMenu() {
        MenuItem delete = activity.getBottomMenu().findItem(R.id.action_delete);
        if(delete != null) {
            delete.setOnMenuItemClickListener(item -> {
                IconUtil.start(item);
                checkForUsage(editStore);
                return true;
            });
            delete.setVisible(editStore != null);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
