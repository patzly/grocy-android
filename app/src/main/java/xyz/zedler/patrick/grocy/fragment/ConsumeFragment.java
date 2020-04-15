package xyz.zedler.patrick.grocy.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.ScanInputActivity;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.util.Constants;
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
    private List<String> productNames = new ArrayList<>();
    private List<String> stockLocationNames = new ArrayList<>();

    private AutoCompleteTextView autoCompleteTextViewProduct, autoCompleteTextViewLocation;
    private TextInputLayout textInputAmount;
    private EditText editTextAmount;

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

        download();

        // INITIALIZE VIEWS

        // product

        TextInputLayout textInputProduct = activity.findViewById(R.id.text_input_consume_product);
        textInputProduct.setEndIconOnClickListener(v -> startActivityForResult(
                new Intent(activity, ScanInputActivity.class),
                Constants.REQUEST.SCAN
        ));
        autoCompleteTextViewProduct = (AutoCompleteTextView) textInputProduct.getEditText();
        assert autoCompleteTextViewProduct != null;
        autoCompleteTextViewProduct.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            //if(hasFocus) startAnimatedIcon(R.id.image_login_logo);
        });
        autoCompleteTextViewProduct.setOnClickListener(v -> loadProductNames());
        autoCompleteTextViewProduct.setOnItemClickListener(
                (parent, view, position, id) -> loadProductDetails(
                        products.get(
                                productNames.indexOf((String) parent.getItemAtPosition(position))
                        ).getId()
        ));

        // amount

        textInputAmount = activity.findViewById(R.id.text_input_consume_amount);
        editTextAmount = textInputAmount.getEditText();

        // location

        TextInputLayout textInputLocation = activity.findViewById(R.id.text_input_consume_location);
        autoCompleteTextViewLocation = (AutoCompleteTextView) textInputProduct.getEditText();
        assert autoCompleteTextViewLocation != null;
        autoCompleteTextViewLocation.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            //if(hasFocus) startAnimatedIcon(R.id.image_login_logo);
        });

        // UPDATE UI

        activity.updateUI(Constants.UI.CONSUME, TAG);
    }

    private void download() {
        loadProductNames();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == Constants.REQUEST.SCAN && resultCode == Activity.RESULT_OK) {
            if(data != null) {
                loadProductDetailsByBarcode(data.getStringExtra(Constants.EXTRA.SCAN_RESULT));
            }
        }
    }

    private void loadProductNames() {
        if(productNames.isEmpty()) {
            request.get(
                    grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
                    response -> {
                        Type listType = new TypeToken<List<Product>>(){}.getType();
                        products = gson.fromJson(response, listType);
                        productNames = getProductNames();
                        adapterProducts = new ArrayAdapter<>(
                                activity, android.R.layout.simple_list_item_1, productNames
                        );
                        autoCompleteTextViewProduct.setAdapter(adapterProducts);
                    }, error -> {}
            );
        }
    }

    private void fillWithProductDetails() {
        autoCompleteTextViewProduct.setText(productDetails.getProduct().getName());
        textInputAmount.setHint(
                activity.getString(
                        R.string.property_amount_in,
                        productDetails.getQuantityUnitStock().getNamePlural()
                )
        );
        editTextAmount.setText("1");
        autoCompleteTextViewLocation.setText(productDetails.getLocation().getName());
        loadStockLocations();
    }

    private void loadProductDetails(int productId) {
        request.get(
                grocyApi.getStockProductDetails(productId),
                response -> {
                    Type listType = new TypeToken<ProductDetails>(){}.getType();
                    productDetails = gson.fromJson(response, listType);
                    fillWithProductDetails();
                }, error -> {}
        );
    }

    private void loadStockLocations() {
        request.get(
                grocyApi.getStockLocationsFromProduct(productDetails.getProduct().getId()),
                response -> {
                    Type listType = new TypeToken<List<StockLocation>>(){}.getType();
                    stockLocations = gson.fromJson(response, listType);
                    stockLocationNames = getStockLocationNames();
                    adapterLocations = new ArrayAdapter<>(
                            activity, android.R.layout.simple_list_item_1, stockLocationNames
                    );
                    autoCompleteTextViewLocation.setAdapter(adapterLocations);
                }, error -> {}
        );
    }

    private void loadProductDetailsByBarcode(String barcode) {
        request.get(
                grocyApi.getStockProductByBarcode(barcode),
                response -> {
                    Type listType = new TypeToken<ProductDetails>(){}.getType();
                    productDetails = gson.fromJson(response, listType);
                    fillWithProductDetails();
                    // TODO: loading animation from swipe refresh
                }, error -> {} // TODO: error handling
        );
    }

    private List<String> getStockLocationNames() {
        List<String> locations = new ArrayList<>();
        if(stockLocations != null) {
            for(StockLocation stockLocation : stockLocations) {
                locations.add(stockLocation.getName());
            }
        }
        return locations;
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
}
