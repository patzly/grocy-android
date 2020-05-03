package xyz.zedler.patrick.grocy.fragment;

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
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class MasterQuantityUnitFragment extends Fragment {

    private final static String TAG = Constants.UI.MASTER_QUANTITY_UNIT_EDIT;
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private WebRequest request;

    private QuantityUnit editQuantityUnit;
    private ArrayList<QuantityUnit> quantityUnits = new ArrayList<>();
    private ArrayList<Product> products = new ArrayList<>();
    private ArrayList<String> quantityUnitNames = new ArrayList<>();

    private SwipeRefreshLayout swipeRefreshLayout;
    private TextInputLayout textInputName, textInputNamePlural, textInputDescription;
    private EditText editTextName, editTextNamePlural, editTextDescription;
    private ImageView imageViewName, imageViewNamePlural, imageViewDescription;
    private boolean isRefresh = false;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_master_quantity_unit, container, false);
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

        activity.findViewById(R.id.frame_master_quantity_unit_cancel).setOnClickListener(
                v -> activity.onBackPressed()
        );

        // swipe refresh
        swipeRefreshLayout = activity.findViewById(R.id.swipe_master_quantity_unit);
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        // name
        textInputName = activity.findViewById(R.id.text_input_master_quantity_unit_name);
        imageViewName = activity.findViewById(R.id.image_master_quantity_unit_name);
        editTextName = textInputName.getEditText();
        assert editTextName != null;
        editTextName.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) startAnimatedIcon(imageViewName);
        });

        // name plural
        textInputNamePlural = activity.findViewById(
                R.id.text_input_master_quantity_unit_name_plural
        );
        imageViewNamePlural = activity.findViewById(R.id.image_master_quantity_unit_name_plural);
        editTextNamePlural = textInputNamePlural.getEditText();
        assert editTextNamePlural != null;
        editTextNamePlural.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) startAnimatedIcon(imageViewNamePlural);
        });

        // description
        textInputDescription = activity.findViewById(R.id.text_input_master_quantity_unit_description);
        imageViewDescription = activity.findViewById(R.id.image_master_quantity_unit_description);
        editTextDescription = textInputDescription.getEditText();
        assert editTextDescription != null;
        editTextDescription.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) startAnimatedIcon(imageViewDescription);
        });

        // BUNDLE WHEN EDIT

        Bundle bundle = getArguments();
        if(bundle != null) {
            editQuantityUnit = bundle.getParcelable(Constants.ARGUMENT.QUANTITY_UNIT);
            // FILL
            if(editQuantityUnit != null) {
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
        downloadQuantityUnits();
        downloadProducts();
    }

    private void downloadQuantityUnits() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS),
                response -> {
                    quantityUnits = gson.fromJson(
                            response,
                            new TypeToken<ArrayList<QuantityUnit>>(){}.getType()
                    );
                    SortUtil.sortQuantityUnitsByName(quantityUnits, true);
                    quantityUnitNames = getQuantityUnitNames();

                    swipeRefreshLayout.setRefreshing(false);

                    updateEditReferences();

                    if(isRefresh && editQuantityUnit != null) {
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
        if(editQuantityUnit != null) {
            QuantityUnit editQuantityUnit = getQuantityUnit(this.editQuantityUnit.getId());
            if(editQuantityUnit != null) this.editQuantityUnit = editQuantityUnit;
        }
    }

    private ArrayList<String> getQuantityUnitNames() {
        ArrayList<String> names = new ArrayList<>();
        if(quantityUnits != null) {
            for(QuantityUnit quantityUnit : quantityUnits) {
                if(editQuantityUnit != null) {
                    if(quantityUnit.getId() != editQuantityUnit.getId()) {
                        names.add(quantityUnit.getName().trim());
                        names.add(quantityUnit.getNamePlural().trim());
                    }
                } else {
                    names.add(quantityUnit.getName().trim());
                    names.add(quantityUnit.getNamePlural().trim());
                }
            }
        }
        return names;
    }

    private QuantityUnit getQuantityUnit(int quantityUnitId) {
        for(QuantityUnit quantityUnit : quantityUnits) {
            if(quantityUnit.getId() == quantityUnitId) {
                return quantityUnit;
            }
        } return null;
    }

    private void fillWithEditReferences() {
        clearInputFocusAndErrors();
        if(editQuantityUnit != null) {
            // name
            editTextName.setText(editQuantityUnit.getName());
            // name (plural form)
            editTextNamePlural.setText(editQuantityUnit.getNamePluralCanNull());
            // description
            editTextDescription.setText(editQuantityUnit.getDescription());
        }
    }

    private void clearInputFocusAndErrors() {
        activity.hideKeyboard();
        textInputName.clearFocus();
        textInputName.setErrorEnabled(false);
        textInputNamePlural.clearFocus();
        textInputNamePlural.setErrorEnabled(false);
        textInputDescription.clearFocus();
        textInputDescription.setErrorEnabled(false);
    }

    public void saveQuantityUnit() {
        if(isFormInvalid()) return;

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", editTextName.getText().toString().trim());
            jsonObject.put("name_plural", editTextNamePlural.getText().toString().trim());
            jsonObject.put("description", editTextDescription.getText().toString().trim());
        } catch (JSONException e) {
            if(DEBUG) Log.e(TAG, "saveQuantityUnit: " + e);
        }
        if(editQuantityUnit != null) {
            request.put(
                    grocyApi.getObject(GrocyApi.ENTITY.QUANTITY_UNITS, editQuantityUnit.getId()),
                    jsonObject,
                    response -> activity.dismissFragment(),
                    error -> {
                        showErrorMessage();
                        Log.e(TAG, "saveQuantityUnit: " + error);
                    }
            );
        } else {
            request.post(
                    grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS),
                    jsonObject,
                    response -> activity.dismissFragment(),
                    error -> {
                        showErrorMessage();
                        Log.e(TAG, "saveQuantityUnit: " + error);
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
        } else if(!quantityUnitNames.isEmpty() && quantityUnitNames.contains(name)) {
            textInputName.setError(activity.getString(R.string.error_duplicate));
            isInvalid = true;
        }

        String namePlural = String.valueOf(editTextNamePlural.getText()).trim();
        if(!quantityUnitNames.isEmpty() && quantityUnitNames.contains(namePlural)) {
            textInputNamePlural.setError(activity.getString(R.string.error_duplicate));
            isInvalid = true;
        }

        return isInvalid;
    }

    private void resetAll() {
        if(editQuantityUnit != null) return;
        clearInputFocusAndErrors();
        editTextName.setText(null);
        editTextNamePlural.setText(null);
        editTextDescription.setText(null);
    }

    private void checkForUsage(QuantityUnit quantityUnit) {
        if(!products.isEmpty()) {
            for(Product product : products) {
                if(product.getQuIdStock() != quantityUnit.getId()
                        && product.getQuIdPurchase() != quantityUnit.getId()) continue;
                activity.showSnackbar(
                        Snackbar.make(
                                activity.findViewById(R.id.linear_container_main),
                                activity.getString(
                                        R.string.msg_master_delete_usage,
                                        activity.getString(R.string.type_quantity_unit)
                                ),
                                Snackbar.LENGTH_LONG
                        )
                );
                return;
            }
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.ARGUMENT.QUANTITY_UNIT, quantityUnit);
        bundle.putString(Constants.ARGUMENT.TYPE, Constants.ARGUMENT.QUANTITY_UNIT);
        activity.showBottomSheet(new MasterDeleteBottomSheetDialogFragment(), bundle);
    }

    public void deleteQuantityUnit(QuantityUnit quantityUnit) {
        request.delete(
                grocyApi.getObject(GrocyApi.ENTITY.QUANTITY_UNITS, quantityUnit.getId()),
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
                checkForUsage(editQuantityUnit);
                return true;
            });
            delete.setVisible(editQuantityUnit != null);
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
