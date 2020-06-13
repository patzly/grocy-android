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
import xyz.zedler.patrick.grocy.databinding.FragmentMasterLocationBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class MasterLocationFragment extends Fragment {

    private final static String TAG = Constants.UI.MASTER_LOCATION;
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private Gson gson;
    private GrocyApi grocyApi;
    private WebRequest request;
    private FragmentMasterLocationBinding binding;

    private ArrayList<Location> locations;
    private ArrayList<Product> products;
    private ArrayList<String> locationNames;
    private Location editLocation;

    private boolean isRefresh;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentMasterLocationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
        activity = null;
        gson = null;
        grocyApi = null;
        request = null;
        locations = null;
        products = null;
        locationNames = null;
        editLocation = null;

        System.gc();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if(isHidden()) return;

        activity = (MainActivity) getActivity();
        assert activity != null;

        // WEB REQUESTS

        request = new WebRequest(activity.getRequestQueue());
        grocyApi = activity.getGrocy();
        gson = new Gson();

        // INITIALIZE VARIABLES

        locations = new ArrayList<>();
        products = new ArrayList<>();
        locationNames = new ArrayList<>();

        editLocation = null;
        isRefresh = false;

        // INITIALIZE VIEWS

        binding.frameMasterLocationCancel.setOnClickListener(v -> activity.onBackPressed());

        // swipe refresh
        binding.swipeMasterLocation.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipeMasterLocation.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        binding.swipeMasterLocation.setOnRefreshListener(this::refresh);

        // name
        binding.editTextMasterLocationName.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) IconUtil.start(binding.imageMasterLocationName);
        });

        // description
        binding.editTextMasterLocationDescription.setOnFocusChangeListener(
                (View v, boolean hasFocus) -> {
                    if(hasFocus) IconUtil.start(binding.imageMasterLocationDescription);
                });

        // is freezer
        binding.checkboxMasterLocationFreezer.setOnCheckedChangeListener(
                (buttonView, isChecked) -> IconUtil.start(binding.imageMasterLocationFreezer)
        );
        binding.linearMasterLocationFreezer.setOnClickListener(v -> {
            IconUtil.start(binding.imageMasterLocationFreezer);
            binding.checkboxMasterLocationFreezer.setChecked(
                    !binding.checkboxMasterLocationFreezer.isChecked()
            );
        });

        // BUNDLE WHEN EDIT

        Bundle bundle = getArguments();
        if(bundle != null) {
            editLocation = bundle.getParcelable(Constants.ARGUMENT.LOCATION);
            // FILL
            if(editLocation != null) {
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

        activity.updateUI(Constants.UI.MASTER_LOCATION, savedInstanceState == null, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(isHidden()) return;

        outState.putParcelableArrayList("locations", locations);
        outState.putParcelableArrayList("products", products);
        outState.putStringArrayList("locationNames", locationNames);

        outState.putParcelable("editLocation", editLocation);

        outState.putBoolean("isRefresh", isRefresh);
    }

    private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
        if(isHidden()) return;

        locations = savedInstanceState.getParcelableArrayList("locations");
        products = savedInstanceState.getParcelableArrayList("products");
        locationNames = savedInstanceState.getStringArrayList("locationNames");

        editLocation = savedInstanceState.getParcelable("editLocation");

        isRefresh = savedInstanceState.getBoolean("isRefresh");
        binding.swipeMasterLocation.setRefreshing(false);

        updateEditReferences();

        if(isRefresh && editLocation != null) {
            fillWithEditReferences();
        } else {
            resetAll();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if(!hidden) onViewCreated(requireView(), null);
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
            binding.swipeMasterLocation.setRefreshing(false);
            activity.showMessage(
                    Snackbar.make(
                            activity.binding.frameMainContainer,
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
        binding.swipeMasterLocation.setRefreshing(true);
        downloadLocations();
        downloadProducts();
    }

    private void downloadLocations() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.LOCATIONS),
                response -> {
                    locations = gson.fromJson(
                            response,
                            new TypeToken<ArrayList<Location>>(){}.getType()
                    );
                    SortUtil.sortLocationsByName(locations, true);
                    locationNames = getLocationNames();

                    binding.swipeMasterLocation.setRefreshing(false);

                    updateEditReferences();

                    if(isRefresh && editLocation != null) {
                        fillWithEditReferences();
                    } else {
                        resetAll();
                    }
                },
                error -> {
                    binding.swipeMasterLocation.setRefreshing(false);
                    activity.showMessage(
                            Snackbar.make(
                                    activity.binding.frameMainContainer,
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
        if(editLocation != null) {
            Location editLocation = getLocation(this.editLocation.getId());
            if(editLocation != null) this.editLocation = editLocation;
        }
    }

    private ArrayList<String> getLocationNames() {
        ArrayList<String> names = new ArrayList<>();
        if(locations != null) {
            for(Location location : locations) {
                if(editLocation != null) {
                    if(location.getId() != editLocation.getId()) {
                        names.add(location.getName().trim());
                    }
                } else {
                    names.add(location.getName().trim());
                }
            }
        }
        return names;
    }

    private Location getLocation(int locationId) {
        for(Location location : locations) {
            if(location.getId() == locationId) {
                return location;
            }
        } return null;
    }

    private void fillWithEditReferences() {
        clearInputFocusAndErrors();
        if(editLocation != null) {
            // name
            binding.editTextMasterLocationName.setText(editLocation.getName());
            // description
            binding.editTextMasterLocationDescription.setText(editLocation.getDescription());
            // is freezer
            binding.checkboxMasterLocationFreezer.setChecked(editLocation.getIsFreezer() == 1);
        }
    }

    private void clearInputFocusAndErrors() {
        activity.hideKeyboard();
        binding.textInputMasterLocationName.clearFocus();
        binding.textInputMasterLocationName.setErrorEnabled(false);
        binding.textInputMasterLocationDescription.clearFocus();
        binding.textInputMasterLocationDescription.setErrorEnabled(false);
    }

    public void saveLocation() {
        if(isFormInvalid()) return;

        JSONObject jsonObject = new JSONObject();
        try {
            Editable name = binding.editTextMasterLocationName.getText();
            Editable description = binding.editTextMasterLocationDescription.getText();
            jsonObject.put(
                    "name", (name != null ? name : "").toString().trim()
            );
            jsonObject.put(
                    "description", (description != null ? description : "").toString().trim()
            );
            jsonObject.put("is_freezer", binding.checkboxMasterLocationFreezer.isChecked());
        } catch (JSONException e) {
            Log.e(TAG, "saveLocation: " + e);
        }
        if(editLocation != null) {
            request.put(
                    grocyApi.getObject(GrocyApi.ENTITY.LOCATIONS, editLocation.getId()),
                    jsonObject,
                    response -> activity.dismissFragment(),
                    error -> {
                        showErrorMessage();
                        Log.e(TAG, "saveLocation: " + error);
                    }
            );
        } else {
            request.post(
                    grocyApi.getObjects(GrocyApi.ENTITY.LOCATIONS),
                    jsonObject,
                    response -> activity.dismissFragment(),
                    error -> {
                        showErrorMessage();
                        Log.e(TAG, "saveLocation: " + error);
                    }
            );
        }
    }

    private boolean isFormInvalid() {
        clearInputFocusAndErrors();
        boolean isInvalid = false;

        String name = String.valueOf(binding.editTextMasterLocationName.getText()).trim();
        if(name.isEmpty()) {
            binding.textInputMasterLocationName.setError(activity.getString(R.string.error_empty));
            isInvalid = true;
        } else if(!locationNames.isEmpty() && locationNames.contains(name)) {
            binding.textInputMasterLocationName.setError(
                    activity.getString(R.string.error_duplicate)
            );
            isInvalid = true;
        }

        return isInvalid;
    }

    private void resetAll() {
        if(editLocation != null) return;
        clearInputFocusAndErrors();
        binding.editTextMasterLocationName.setText(null);
        binding.editTextMasterLocationDescription.setText(null);
        binding.checkboxMasterLocationFreezer.setChecked(false);
    }

    public void checkForUsage(Location location) {
        if(!products.isEmpty()) {
            for(Product product : products) {
                if(product.getLocationId() == location.getId()) {
                    activity.showMessage(
                            Snackbar.make(
                                    activity.binding.frameMainContainer,
                                    activity.getString(
                                            R.string.msg_master_delete_usage,
                                            activity.getString(R.string.type_location)
                                    ),
                                    Snackbar.LENGTH_LONG
                            )
                    );
                    return;
                }
            }
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.ARGUMENT.LOCATION, location);
        bundle.putString(Constants.ARGUMENT.TYPE, Constants.ARGUMENT.LOCATION);
        activity.showBottomSheet(new MasterDeleteBottomSheetDialogFragment(), bundle);
    }

    public void deleteLocation(Location location) {
        request.delete(
                grocyApi.getObject(GrocyApi.ENTITY.LOCATIONS, location.getId()),
                response -> activity.dismissFragment(),
                error -> showErrorMessage()
        );
    }

    private void showErrorMessage() {
        activity.showMessage(
                Snackbar.make(
                        activity.binding.frameMainContainer,
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
                checkForUsage(editLocation);
                return true;
            });
            delete.setVisible(editLocation != null);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
