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

import android.annotation.SuppressLint;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class MasterStoreFragment extends Fragment {

    private final static String TAG = Constants.UI.MASTER_STORE;
    private final static boolean DEBUG = false;

    private MainActivity activity;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private WebRequest request;

    private Store editStore;
    private ArrayList<Store> stores = new ArrayList<>();
    private ArrayList<Product> products = new ArrayList<>();
    private ArrayList<String> storeNames = new ArrayList<>();

    private SwipeRefreshLayout swipeRefreshLayout;
    private TextInputLayout textInputName, textInputDescription;
    private EditText editTextName, editTextDescription;
    private ImageView imageViewName, imageViewDescription;
    private boolean isRefresh = false;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_master_store, container, false);
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

        activity.findViewById(R.id.frame_master_store_cancel).setOnClickListener(
                v -> activity.onBackPressed()
        );

        // swipe refresh
        swipeRefreshLayout = activity.findViewById(R.id.swipe_master_store);
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        // name
        textInputName = activity.findViewById(R.id.text_input_master_store_name);
        imageViewName = activity.findViewById(R.id.image_master_store_name);
        editTextName = textInputName.getEditText();
        assert editTextName != null;
        editTextName.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) startAnimatedIcon(imageViewName);
        });

        // description
        textInputDescription = activity.findViewById(R.id.text_input_master_store_description);
        imageViewDescription = activity.findViewById(R.id.image_master_store_description);
        editTextDescription = textInputDescription.getEditText();
        assert editTextDescription != null;
        editTextDescription.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) startAnimatedIcon(imageViewDescription);
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

        load();

        // UPDATE UI

        activity.updateUI(toString(), TAG);
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
            swipeRefreshLayout.setRefreshing(false);
            activity.showSnackbar(
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

    private void download() {
        swipeRefreshLayout.setRefreshing(true);
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

                    swipeRefreshLayout.setRefreshing(false);

                    updateEditReferences();

                    if(isRefresh && editStore != null) {
                        fillWithEditReferences();
                    } else {
                        resetAll();
                    }
                },
                error -> {
                    swipeRefreshLayout.setRefreshing(false);
                    activity.showSnackbar(
                            Snackbar.make(
                                    activity.findViewById(R.id.linear_container_main),
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
            editTextName.setText(editStore.getName());
            // description
            editTextDescription.setText(editStore.getDescription());
        }
    }

    private void clearInputFocusAndErrors() {
        activity.hideKeyboard();
        textInputName.clearFocus();
        textInputName.setErrorEnabled(false);
        textInputDescription.clearFocus();
        textInputDescription.setErrorEnabled(false);
    }

    public void saveStore() {
        if(isFormInvalid()) return;

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", editTextName.getText().toString().trim());
            jsonObject.put("description", editTextDescription.getText().toString().trim());
        } catch (JSONException e) {
            if(DEBUG) Log.e(TAG, "saveStore: " + e);;
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

        String name = String.valueOf(editTextName.getText()).trim();
        if(name.equals("")) {
            textInputName.setError(activity.getString(R.string.error_empty));
            isInvalid = true;
        } else if(!storeNames.isEmpty() && storeNames.contains(name)) {
            textInputName.setError(activity.getString(R.string.error_duplicate));
            isInvalid = true;
        }

        return isInvalid;
    }

    private void resetAll() {
        if(editStore != null) return;
        clearInputFocusAndErrors();
        editTextName.setText(null);
        editTextDescription.setText(null);
    }

    public void checkForUsage(Store store) {
        if(!products.isEmpty()) {
            for(Product product : products) {
                if(product.getStoreId() == null) continue;
                if(product.getStoreId().equals(String.valueOf(store.getId()))) {
                    activity.showSnackbar(
                            Snackbar.make(
                                    activity.findViewById(R.id.linear_container_main),
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
        activity.showSnackbar(
                Snackbar.make(
                        activity.findViewById(R.id.linear_container_main),
                        activity.getString(R.string.msg_error),
                        Snackbar.LENGTH_SHORT
                )
        );
    }

    public void setUpBottomMenu() {
        MenuItem delete = activity.getBottomMenu().findItem(R.id.action_delete);
        if(delete != null) {
            delete.setOnMenuItemClickListener(item -> {
                activity.startAnimatedIcon(item);
                checkForUsage(editStore);
                return true;
            });
            delete.setVisible(editStore != null);
        }
    }

    @SuppressLint("LongLogTag")
    private void startAnimatedIcon(View view) {
        try {
            ((Animatable) ((ImageView) view).getDrawable()).start();
        } catch (ClassCastException cla) {
            Log.e(TAG, "startAnimatedIcon(Drawable) requires AVD!");
        }
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
