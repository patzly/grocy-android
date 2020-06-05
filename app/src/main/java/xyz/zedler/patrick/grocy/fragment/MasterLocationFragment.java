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

import com.google.android.material.checkbox.MaterialCheckBox;
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
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private WebRequest request;

    private Location editLocation;
    private ArrayList<Location> locations = new ArrayList<>();
    private ArrayList<Product> products = new ArrayList<>();
    private ArrayList<String> locationNames = new ArrayList<>();

    private SwipeRefreshLayout swipeRefreshLayout;
    private TextInputLayout textInputName, textInputDescription;
    private EditText editTextName, editTextDescription;
    private ImageView imageViewName, imageViewDescription;
    private MaterialCheckBox checkBoxIsFreezer;
    private boolean isRefresh = false;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_master_location, container, false);
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

        activity.findViewById(R.id.frame_master_location_cancel).setOnClickListener(
                v -> activity.onBackPressed()
        );

        // swipe refresh
        swipeRefreshLayout = activity.findViewById(R.id.swipe_master_location);
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        // name
        textInputName = activity.findViewById(R.id.text_input_master_location_name);
        imageViewName = activity.findViewById(R.id.image_master_location_name);
        editTextName = textInputName.getEditText();
        assert editTextName != null;
        editTextName.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) IconUtil.start(imageViewName);
        });

        // description
        textInputDescription = activity.findViewById(R.id.text_input_master_location_description);
        imageViewDescription = activity.findViewById(R.id.image_master_location_description);
        editTextDescription = textInputDescription.getEditText();
        assert editTextDescription != null;
        editTextDescription.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) IconUtil.start(imageViewDescription);
        });

        // is freezer
        checkBoxIsFreezer = activity.findViewById(R.id.checkbox_master_location_freezer);
        checkBoxIsFreezer.setOnCheckedChangeListener(
                (buttonView, isChecked) -> IconUtil.start(
                        activity,
                        R.id.image_master_location_freezer
                )
        );
        activity.findViewById(R.id.linear_master_location_freezer).setOnClickListener(v -> {
            IconUtil.start(activity, R.id.image_master_location_freezer);
            checkBoxIsFreezer.setChecked(!checkBoxIsFreezer.isChecked());
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
            activity.showMessage(
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

                    swipeRefreshLayout.setRefreshing(false);

                    updateEditReferences();

                    if(isRefresh && editLocation != null) {
                        fillWithEditReferences();
                    } else {
                        resetAll();
                    }
                },
                error -> {
                    swipeRefreshLayout.setRefreshing(false);
                    activity.showMessage(
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
            editTextName.setText(editLocation.getName());
            // description
            editTextDescription.setText(editLocation.getDescription());
            // is freezer
            checkBoxIsFreezer.setChecked(editLocation.getIsFreezer() == 1);
        }
    }

    private void clearInputFocusAndErrors() {
        activity.hideKeyboard();
        textInputName.clearFocus();
        textInputName.setErrorEnabled(false);
        textInputDescription.clearFocus();
        textInputDescription.setErrorEnabled(false);
    }

    public void saveLocation() {
        if(isFormInvalid()) return;

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", editTextName.getText().toString().trim());
            jsonObject.put("description", editTextDescription.getText().toString().trim());
            jsonObject.put("is_freezer", checkBoxIsFreezer.isChecked());
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

        String name = String.valueOf(editTextName.getText()).trim();
        if(name.isEmpty()) {
            textInputName.setError(activity.getString(R.string.error_empty));
            isInvalid = true;
        } else if(!locationNames.isEmpty() && locationNames.contains(name)) {
            textInputName.setError(activity.getString(R.string.error_duplicate));
            isInvalid = true;
        }

        return isInvalid;
    }

    private void resetAll() {
        if(editLocation != null) return;
        clearInputFocusAndErrors();
        editTextName.setText(null);
        editTextDescription.setText(null);
        checkBoxIsFreezer.setChecked(false);
    }

    public void checkForUsage(Location location) {
        if(!products.isEmpty()) {
            for(Product product : products) {
                if(product.getLocationId() == location.getId()) {
                    activity.showMessage(
                            Snackbar.make(
                                    activity.findViewById(R.id.linear_container_main),
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
