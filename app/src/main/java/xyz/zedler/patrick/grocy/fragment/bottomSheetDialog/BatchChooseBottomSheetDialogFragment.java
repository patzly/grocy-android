package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.ScanBatchActivity;
import xyz.zedler.patrick.grocy.adapter.MatchArrayAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.web.RequestQueueSingleton;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class BatchChooseBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private final static boolean DEBUG = false;
    private final static String TAG = "BatchChooseBottomSheet";

    private GrocyApi grocyApi;
    private WebRequest request;
    private Product selectedProduct;
    private String barcode;

    private ArrayList<Product> products;

    private ScanBatchActivity activity;
    private TextInputLayout textInputProduct;
    private MaterialAutoCompleteTextView autoCompleteTextViewProduct;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.fragment_bottomsheet_batch_choose_product, container, false
        );

        activity = (ScanBatchActivity) getActivity();
        assert activity != null;

        if(getArguments() == null
                || getArguments().getStringArrayList(Constants.ARGUMENT.PRODUCT_NAMES) == null
                || getArguments().getStringArrayList(Constants.ARGUMENT.PRODUCTS) == null
                || getArguments().getString(Constants.ARGUMENT.BARCODE) == null) {
            dismissWithErrorMessage();
            return view;
        }

        setCancelable(false);

        List<String> productNames = getArguments().getStringArrayList(
                Constants.ARGUMENT.PRODUCT_NAMES
        );
        products = getArguments().getParcelableArrayList(Constants.ARGUMENT.PRODUCTS);
        barcode = getArguments().getString(Constants.ARGUMENT.BARCODE);

        // WEB REQUESTS

        RequestQueue requestQueue = RequestQueueSingleton
                .getInstance(getContext()).getRequestQueue();
        request = new WebRequest(requestQueue);

        // API

        grocyApi = new GrocyApi(activity);

        view.findViewById(R.id.button_batch_link).setOnClickListener(v -> {
            if(autoCompleteTextViewProduct.getText().toString().equals(selectedProduct.getName())) {
                textInputProduct.setErrorEnabled(false);
                addProductBarcode(barcode);
            } else {
                textInputProduct.setError("Product is not from list");
            }
        });

        view.findViewById(R.id.button_batch_discard).setOnClickListener(v -> {
            dismiss();
            activity.resume();
        });

        // product

        textInputProduct = view.findViewById(R.id.text_input_batch_choose_product_name);
        autoCompleteTextViewProduct = (MaterialAutoCompleteTextView) textInputProduct.getEditText();
        assert autoCompleteTextViewProduct != null;
        autoCompleteTextViewProduct.setAdapter(new MatchArrayAdapter(activity, productNames));
        autoCompleteTextViewProduct.setOnItemClickListener(
                (parent, v, position, id) -> selectedProduct = getProductFromName(
                        String.valueOf(parent.getItemAtPosition(position))
                )
        );

        // Set Input mode -> keyboard hid autocomplete popup, this solves call solves the issue
        Objects.requireNonNull(Objects.requireNonNull(getDialog()).getWindow())
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void addProductBarcode(String barcode) {
        List<String> barcodes;
        if(selectedProduct.getBarcode() != null && !selectedProduct.getBarcode().equals("")) {
            barcodes = new ArrayList<>(Arrays.asList(
                    selectedProduct.getBarcode().split(",")
            ));
        } else {
            barcodes = new ArrayList<>();
        }

        barcodes.add(barcode);
        JSONObject body = new JSONObject();
        try {
            body.put("barcode", TextUtils.join(",", barcodes));
        } catch (JSONException e) {
            dismissWithErrorMessage();
            if(DEBUG) Log.e(TAG, "editProductBarcodes: " + e);
        }
        request.put(
                grocyApi.getObject(GrocyApi.ENTITY.PRODUCTS, selectedProduct.getId()),
                body,
                response -> {
                    activity.loadProductDetailsByBarcode(barcode);
                    dismiss();
                    activity.resume();
                },
                error -> dismissWithErrorMessage()
        );
    }

    private void dismissWithErrorMessage() {
        activity.showSnackbar(
                Snackbar.make(
                        activity.findViewById(R.id.barcode_scan_batch),
                        activity.getString(R.string.msg_error),
                        Snackbar.LENGTH_SHORT
                )
        );
        dismiss();
        activity.resume();
    }

    private Product getProductFromName(String name) {
        if(name != null) {
            for(Product product : products) {
                if(product.getName().equals(name)) {
                    return product;
                }
            }
        }
        return null;
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
