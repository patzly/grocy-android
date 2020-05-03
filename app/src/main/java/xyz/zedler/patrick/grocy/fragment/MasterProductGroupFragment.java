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
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class MasterProductGroupFragment extends Fragment {

    private final static String TAG = Constants.UI.MASTER_PRODUCT_GROUP_EDIT;
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private WebRequest request;

    private ProductGroup editProductGroup;
    private ArrayList<ProductGroup> productGroups = new ArrayList<>();
    private ArrayList<Product> products = new ArrayList<>();
    private ArrayList<String> productGroupNames = new ArrayList<>();

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
        return inflater.inflate(
                R.layout.fragment_master_product_group,
                container,
                false
        );
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

        activity.findViewById(R.id.frame_master_product_group_cancel).setOnClickListener(
                v -> activity.onBackPressed()
        );

        // swipe refresh
        swipeRefreshLayout = activity.findViewById(R.id.swipe_master_product_group);
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        // name
        textInputName = activity.findViewById(R.id.text_input_master_product_group_name);
        imageViewName = activity.findViewById(R.id.image_master_product_group_name);
        editTextName = textInputName.getEditText();
        assert editTextName != null;
        editTextName.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) startAnimatedIcon(imageViewName);
        });

        // description
        textInputDescription = activity.findViewById(R.id.text_input_master_product_group_description);
        imageViewDescription = activity.findViewById(R.id.image_master_product_group_description);
        editTextDescription = textInputDescription.getEditText();
        assert editTextDescription != null;
        editTextDescription.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) startAnimatedIcon(imageViewDescription);
        });

        // BUNDLE WHEN EDIT

        Bundle bundle = getArguments();
        if(bundle != null) {
            editProductGroup = bundle.getParcelable(Constants.ARGUMENT.PRODUCT_GROUP);
            // FILL
            if(editProductGroup != null) {
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
        downloadProductGroups();
        downloadProducts();
    }

    private void downloadProductGroups() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_GROUPS),
                response -> {
                    productGroups = gson.fromJson(
                            response,
                            new TypeToken<ArrayList<ProductGroup>>(){}.getType()
                    );
                    SortUtil.sortProductGroupsByName(productGroups, true);
                    productGroupNames = getProductGroupNames();

                    swipeRefreshLayout.setRefreshing(false);

                    updateEditReferences();

                    if(isRefresh && editProductGroup != null) {
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
        if(editProductGroup != null) {
            ProductGroup editProductGroup = getProductGroup(this.editProductGroup.getId());
            if(editProductGroup != null) this.editProductGroup = editProductGroup;
        }
    }

    private ArrayList<String> getProductGroupNames() {
        ArrayList<String> names = new ArrayList<>();
        if(productGroups != null) {
            for(ProductGroup productGroup : productGroups) {
                if(editProductGroup != null) {
                    if(!productGroup.getId().equals(editProductGroup.getId())) {
                        names.add(productGroup.getName().trim());
                    }
                } else {
                    names.add(productGroup.getName().trim());
                }
            }
        }
        return names;
    }

    private ProductGroup getProductGroup(String productGroupId) {
        for(ProductGroup productGroup : productGroups) {
            if(productGroup.getId().equals(productGroupId)) {
                return productGroup;
            }
        } return null;
    }

    private void fillWithEditReferences() {
        clearInputFocusAndErrors();
        if(editProductGroup != null) {
            // name
            editTextName.setText(editProductGroup.getName());
            // description
            editTextDescription.setText(editProductGroup.getDescription());
        }
    }

    private void clearInputFocusAndErrors() {
        activity.hideKeyboard();
        textInputName.clearFocus();
        textInputName.setErrorEnabled(false);
        textInputDescription.clearFocus();
        textInputDescription.setErrorEnabled(false);
    }

    public void saveProductGroup() {
        if(isFormInvalid()) return;

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", editTextName.getText().toString().trim());
            jsonObject.put("description", editTextDescription.getText().toString().trim());
        } catch (JSONException e) {
            if(DEBUG) Log.e(TAG, "saveProductGroup: " + e);
        }
        if(editProductGroup != null) {
            request.put(
                    grocyApi.getObject(
                            GrocyApi.ENTITY.PRODUCT_GROUPS,
                            Integer.parseInt(editProductGroup.getId())
                    ),
                    jsonObject,
                    response -> activity.dismissFragment(),
                    error -> {
                        showErrorMessage();
                        Log.e(TAG, "saveProductGroup: " + error);
                    }
            );
        } else {
            request.post(
                    grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_GROUPS),
                    jsonObject,
                    response -> activity.dismissFragment(),
                    error -> {
                        showErrorMessage();
                        Log.e(TAG, "saveProductGroup: " + error);
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
        } else if(!productGroupNames.isEmpty() && productGroupNames.contains(name)) {
            textInputName.setError(activity.getString(R.string.error_duplicate));
            isInvalid = true;
        }

        return isInvalid;
    }

    private void resetAll() {
        if(editProductGroup != null) return;
        clearInputFocusAndErrors();
        editTextName.setText(null);
        editTextDescription.setText(null);
    }

    public void checkForUsage(ProductGroup productGroup) {
        if(!products.isEmpty()) {
            for(Product product : products) {
                if(product.getProductGroupId() == null) continue;
                if(product.getProductGroupId().equals(String.valueOf(productGroup.getId()))) {
                    activity.showSnackbar(
                            Snackbar.make(
                                    activity.findViewById(R.id.linear_container_main),
                                    activity.getString(
                                            R.string.msg_master_delete_usage,
                                            activity.getString(R.string.type_product_group)
                                    ),
                                    Snackbar.LENGTH_LONG
                            )
                    );
                    return;
                }
            }
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.ARGUMENT.PRODUCT_GROUP, productGroup);
        bundle.putString(Constants.ARGUMENT.TYPE, Constants.ARGUMENT.PRODUCT_GROUP);
        activity.showBottomSheet(new MasterDeleteBottomSheetDialogFragment(), bundle);
    }

    public void deleteProductGroup(ProductGroup productGroup) {
        request.delete(
                grocyApi.getObject(GrocyApi.ENTITY.PRODUCT_GROUPS, Integer.parseInt(productGroup.getId())),
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
                checkForUsage(editProductGroup);
                return true;
            });
            delete.setVisible(editProductGroup != null);
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
