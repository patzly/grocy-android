package xyz.zedler.patrick.grocy.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IdRes;
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

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.ScanInputActivity;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.ProductEntries;
import xyz.zedler.patrick.grocy.model.ProductEntry;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.model.StockLocations;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.view.ActionButton;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class ConsumeFragment extends Fragment {

    private final static String TAG = StockFragment.class.getSimpleName();
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private WebRequest request;
    private ArrayAdapter<String> adapterProducts, adapterLocations;
    private ProductDetails productDetails;

    private List<Product> products = new ArrayList<>();
    private List<StockLocation> stockLocations = new ArrayList<>();
    private List<ProductEntry> productEntries = new ArrayList<>();
    private List<String> productNames = new ArrayList<>();

    private ActionButton buttonProductDetails;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AutoCompleteTextView autoCompleteTextViewProduct;
    private TextInputLayout textInputProduct, textInputAmount;
    private EditText editTextAmount;
    private TextView textViewLocation, textViewSpecific;
    private MaterialCheckBox checkBoxSpoiled;
    private int selectedLocationId, selectedProductEntryId;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_consume, container, false);
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

        activity.findViewById(R.id.frame_back_consume).setOnClickListener(v -> {
            activity.onBackPressed();
        });

        buttonProductDetails = activity.findViewById(R.id.button_consume_product_details);
        buttonProductDetails.setState(false);
        buttonProductDetails.setOnClickListener(v -> {
            buttonProductDetails.startIconAnimation();
            if(productDetails != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(Constants.ARGUMENT.PRODUCT_DETAILS, productDetails);
                bundle.putBoolean(Constants.ARGUMENT.SET_UP_WITH_PRODUCT_DETAILS, true);
                activity.showBottomSheet(new StockItemDetailsBottomSheetDialogFragment(), bundle);
            }
        });

        // swipe refresh

        swipeRefreshLayout = activity.findViewById(R.id.swipe_consume);
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        // product

        textInputProduct = activity.findViewById(R.id.text_input_consume_product);
        textInputProduct.setErrorIconDrawable(null);
        textInputProduct.setEndIconOnClickListener(v -> startActivityForResult(
                new Intent(activity, ScanInputActivity.class),
                Constants.REQUEST.SCAN
        ));
        autoCompleteTextViewProduct = (AutoCompleteTextView) textInputProduct.getEditText();
        assert autoCompleteTextViewProduct != null;
        autoCompleteTextViewProduct.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) startAnimatedIcon(R.id.image_consume_product);
        });
        autoCompleteTextViewProduct.setOnItemClickListener((parent, view, position, id) -> {
            loadProductDetails(
                    products.get(
                            productNames.indexOf((String) parent.getItemAtPosition(position))
                    ).getId()
            );
        });
        autoCompleteTextViewProduct.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        editTextAmount.requestFocus();
                        return true;
                    } return false;
        });

        // amount

        textInputAmount = activity.findViewById(R.id.text_input_consume_amount);
        editTextAmount = textInputAmount.getEditText();
        assert editTextAmount != null;
        editTextAmount.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) {
                startAnimatedIcon(R.id.image_consume_amount);
                // editTextAmount.selectAll();
            }
        });
        editTextAmount.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                clearInputFocus();
                return true;
            } return false;
        });

        activity.findViewById(R.id.button_consume_amount_more).setOnClickListener(v -> {
            startAnimatedIcon(R.id.image_consume_amount);
            if(editTextAmount.getText().toString().equals("")) {
                editTextAmount.setText(String.valueOf(1));
            } else {
                String amountNew = NumUtil.trim(
                        Double.parseDouble(editTextAmount.getText().toString()) + 1
                );
                editTextAmount.setText(amountNew);
            }
        });

        activity.findViewById(R.id.button_consume_amount_less).setOnClickListener(v -> {
            if(!editTextAmount.getText().toString().equals("")) {
                startAnimatedIcon(R.id.image_consume_amount);
                double amount = Double.parseDouble(editTextAmount.getText().toString()) - 1;
                if(amount > 0) {
                    editTextAmount.setText(NumUtil.trim(amount));
                } else {
                    editTextAmount.setText(null);
                }
            }
        });

        // location

        activity.findViewById(R.id.linear_consume_location).setOnClickListener(v -> {
            startAnimatedIcon(R.id.image_consume_location);
            if(productDetails != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(
                        Constants.ARGUMENT.STOCK_LOCATIONS,
                        new StockLocations(stockLocations)
                );
                bundle.putParcelable(Constants.ARGUMENT.PRODUCT_DETAILS, productDetails);
                bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedLocationId);
                activity.showBottomSheet(new StockLocationsBottomSheetDialogFragment(), bundle);
            } else {
                // no product selected
                textInputProduct.setError(activity.getString(R.string.error_select_product));
            }
        });
        textViewLocation = activity.findViewById(R.id.text_consume_location);

        // specific

        activity.findViewById(R.id.linear_consume_specific).setOnClickListener(v -> {
            startAnimatedIcon(R.id.image_consume_specific);
            if(productDetails != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(
                        Constants.ARGUMENT.PRODUCT_ENTRIES,
                        new ProductEntries(productEntries)
                );
                bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedProductEntryId);
                activity.showBottomSheet(new ProductEntriesBottomSheetDialogFragment(), bundle);
            } else {
                // no product selected
                textInputProduct.setError(activity.getString(R.string.error_select_product));
            }
        });
        textViewSpecific = activity.findViewById(R.id.text_consume_specific);

        // spoiled

        checkBoxSpoiled = activity.findViewById(R.id.checkbox_consume_spoiled);
        checkBoxSpoiled.setOnCheckedChangeListener(
                (buttonView, isChecked) -> startAnimatedIcon(R.id.image_consume_spoiled)
        );
        activity.findViewById(R.id.linear_consume_spoiled).setOnClickListener(v -> {
            startAnimatedIcon(R.id.image_consume_spoiled);
            checkBoxSpoiled.setChecked(!checkBoxSpoiled.isChecked());
        });

        // START

        load();

        // UPDATE UI

        activity.updateUI(Constants.UI.CONSUME, TAG);
    }

    private void load() {
        if(activity.isOnline()) {
            download();
        }
    }

    private void refresh() {
        if(activity.isOnline()) {
            download();
        } else {
            swipeRefreshLayout.setRefreshing(false);
            activity.showSnackbar(
                    Snackbar.make(
                            activity.findViewById(R.id.linear_container_main),
                            "No connection",
                            Snackbar.LENGTH_SHORT
                    ).setActionTextColor(
                            ContextCompat.getColor(activity, R.color.secondary)
                    ).setAction(
                            "Retry",
                            v1 -> refresh()
                    )
            );
        }

        if(productDetails != null) {
            fillWithProductDetails();
        } else {
            // clear all fields
            textInputProduct.setErrorEnabled(false);
            autoCompleteTextViewProduct.setText(null);
            textInputAmount.setErrorEnabled(false);
            editTextAmount.setText(null);
            textViewLocation.setText(activity.getString(R.string.subtitle_none));
            textViewSpecific.setText(activity.getString(R.string.subtitle_none));
            if(checkBoxSpoiled.isChecked()) checkBoxSpoiled.setChecked(false);
            clearInputFocus();
        }
    }

    private void download() {
        swipeRefreshLayout.setRefreshing(true);
        downloadProductNames();
    }

    private void downloadProductNames() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
                response -> {
                    products = gson.fromJson(
                            response,
                            new TypeToken<List<Product>>(){}.getType()
                    );
                    productNames = getProductNames();
                    adapterProducts = new ArrayAdapter<>(
                            activity, android.R.layout.simple_list_item_1, productNames
                    );
                    autoCompleteTextViewProduct.setAdapter(adapterProducts);
                    // download finished
                    swipeRefreshLayout.setRefreshing(false);
                }, error -> {
                    swipeRefreshLayout.setRefreshing(false);
                    activity.showSnackbar(
                            Snackbar.make(
                                    activity.findViewById(R.id.linear_container_main),
                                    "An error occurred",
                                    Snackbar.LENGTH_SHORT
                            ).setActionTextColor(
                                    ContextCompat.getColor(activity, R.color.secondary)
                            ).setAction(
                                    "Retry",
                                    v1 -> download()
                            )
                    );
                }
        );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == Constants.REQUEST.SCAN && resultCode == Activity.RESULT_OK) {
            if(data != null) {
                loadProductDetailsByBarcode(data.getStringExtra(Constants.EXTRA.SCAN_RESULT));
            }
        }
    }

    private void fillWithProductDetails() {
        clearInputFocus();
        // PRODUCT
        autoCompleteTextViewProduct.setText(productDetails.getProduct().getName());
        textInputProduct.setErrorEnabled(false);
        // AMOUNT
        textInputAmount.setHint(
                activity.getString(
                        R.string.property_amount_in,
                        productDetails.getQuantityUnitStock().getNamePlural()
                )
        );
        editTextAmount.setText("1");
        // LOCATION
        selectDefaultLocation();
        // load other locations for bottomSheet and then for displaying the selected
        loadStockLocations();
        // SPECIFIC
        textViewSpecific.setText(activity.getString(R.string.subtitle_none));
        // SPOILED
        checkBoxSpoiled.setChecked(false);
        // DETAILS
        buttonProductDetails.refreshState(true);
    }

    private void clearInputFocus() {
        activity.hideKeyboard();
        textInputProduct.clearFocus();
        textInputAmount.clearFocus();
    }

    private void loadProductDetails(int productId) {
        request.get(
                grocyApi.getStockProductDetails(productId),
                response -> {
                    productDetails = gson.fromJson(
                            response,
                            new TypeToken<ProductDetails>(){}.getType()
                    );
                    fillWithProductDetails();
                }, error -> {}
        );
    }

    private void loadStockLocations() {
        request.get(
                grocyApi.getStockLocationsFromProduct(productDetails.getProduct().getId()),
                response -> {
                    stockLocations = gson.fromJson(
                            response,
                            new TypeToken<List<StockLocation>>(){}.getType()
                    );
                    SortUtil.sortStockLocationItemsByName(stockLocations);
                }, error -> {}
        );
    }

    private void loadProductDetailsByBarcode(String barcode) {
        swipeRefreshLayout.setRefreshing(true);
        request.get(
                grocyApi.getStockProductByBarcode(barcode),
                response -> {
                    swipeRefreshLayout.setRefreshing(false);
                    productDetails = gson.fromJson(
                            response,
                            new TypeToken<ProductDetails>(){}.getType()
                    );
                    fillWithProductDetails();
                }, error -> {
                    swipeRefreshLayout.setRefreshing(false);
                    activity.showSnackbar(
                            Snackbar.make(
                                    activity.findViewById(R.id.linear_container_main),
                                    "An error occurred",
                                    Snackbar.LENGTH_SHORT
                            )
                    );
                }
        );
    }

    private List<String> getProductNames() {
        List<String> names = new ArrayList<>();
        if(products != null) {
            for(Product product : products) {
                names.add(product.getName());
            }
        }
        return names;
    }

    private void selectDefaultLocation() {
        if(productDetails != null) {
            selectedLocationId = productDetails.getLocation().getId();
            textViewLocation.setText(
                    activity.getString(
                            R.string.subtitle_selection_default,
                            productDetails.getLocation().getName()
                    )
            );
        }
    }

    public void selectLocation(int selectedId) {
        this.selectedLocationId = selectedId;
        String location = null;
        if(stockLocations.isEmpty()) {
            location = productDetails.getLocation().getName();
        } else {
            StockLocation stockLocation = getStockLocation(selectedId);
            if(stockLocation != null) {
                location = stockLocation.getLocationName();
            }
        }

        if(productDetails.getLocation().getId() == selectedId && location != null) {
            // selected is the default location
            location = activity.getString(R.string.subtitle_selection_default, location);
        }

        textViewLocation.setText(location);
    }

    public void selectProductEntry(int selectedId) {
        this.selectedProductEntryId = selectedId;
        /*String location = null;
        if(stockLocations.isEmpty()) {
            location = productDetails.getLocation().getName();
        } else {
            StockLocation stockLocation = getStockLocation(selectedId);
            if(stockLocation != null) {
                location = stockLocation.getLocationName();
            }
        }

        if(productDetails.getLocation().getId() == selectedId && location != null) {
            // selected is the default location
            location = activity.getString(R.string.subtitle_selection_default, location);
        }*/

        //textViewLocation.setText(location);
    }

    private StockLocation getStockLocation(int locationId) {
        for(StockLocation stockLocation : stockLocations) {
            if(stockLocation.getLocationId() == locationId) {
                return stockLocation;
            }
        } return null;
    }

    private void startAnimatedIcon(@IdRes int viewId) {
        try {
            ((Animatable) ((ImageView) activity.findViewById(viewId)).getDrawable()).start();
        } catch (ClassCastException cla) {
            Log.e(TAG, "startAnimatedIcon(Drawable) requires AVD!");
        }
    }
}
