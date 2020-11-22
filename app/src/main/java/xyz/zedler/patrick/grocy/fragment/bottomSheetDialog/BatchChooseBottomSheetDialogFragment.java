package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

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

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.ScanBatchActivity;
import xyz.zedler.patrick.grocy.adapter.MatchArrayAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.OpenFoodFactsApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.util.Constants;

public class BatchChooseBottomSheetDialogFragment extends CustomBottomSheetDialogFragment {

    private final static String TAG = "BatchChooseBottomSheet";

    private GrocyApi grocyApi;
    private DownloadHelper dlHelper;
    private Product selectedProduct;
    private String barcode, batchType, buttonAction;

    private ArrayList<Product> products;

    private ScanBatchActivity activity;
    private TextInputLayout textInputProduct;
    private MaterialAutoCompleteTextView autoCompleteTextViewProduct;

    private boolean debug;

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

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        if(getArguments() == null
                || getArguments().getString(Constants.ARGUMENT.TYPE) == null
                || getArguments().getString(Constants.ARGUMENT.BARCODE) == null
                || getArguments().getStringArrayList(Constants.ARGUMENT.PRODUCT_NAMES) == null
                || getArguments().getParcelableArrayList(Constants.ARGUMENT.PRODUCTS) == null
        ) {
            dismissWithMessage(activity.getString(R.string.error_undefined));
            return view;
        }

        // set bottom sheet to not cancelable, so buttons have to be pressed
        setCancelable(false);

        List<String> productNames = getArguments().getStringArrayList(
                Constants.ARGUMENT.PRODUCT_NAMES
        );
        products = getArguments().getParcelableArrayList(Constants.ARGUMENT.PRODUCTS);
        barcode = getArguments().getString(Constants.ARGUMENT.BARCODE);
        batchType = getArguments().getString(Constants.ARGUMENT.TYPE);

        // WEB REQUESTS

        dlHelper = new DownloadHelper(activity, TAG);

        // API

        grocyApi = new GrocyApi(activity);

        Button batchButtonLinkCreate = view.findViewById(R.id.button_batch_name_create);
        TextView description = view.findViewById(R.id.text_batch_choose_product);
        if(batchType.equals(Constants.ACTION.PURCHASE)) {
            batchButtonLinkCreate.setText(activity.getString(R.string.action_create));
            buttonAction = Constants.ACTION.CREATE;
            description.setText(activity.getString(R.string.description_batch_choose_create));
        } else {
            batchButtonLinkCreate.setText(activity.getString(R.string.action_link));
            buttonAction = Constants.ACTION.LINK;
            description.setText(activity.getString(R.string.description_batch_choose_link));
        }

        batchButtonLinkCreate.setOnClickListener(v -> {
            String inputText = autoCompleteTextViewProduct.getText().toString().trim();
            if(inputText.isEmpty()) {
                textInputProduct.setError(activity.getString(R.string.error_empty));
            } else if(buttonAction.equals(Constants.ACTION.CREATE)) {
                textInputProduct.setErrorEnabled(false);
                activity.createMissingBatchItemPurchase(inputText, barcode);
                dismiss();
            } else {
                assert productNames != null;
                if(productNames.contains(inputText)) {
                    textInputProduct.setErrorEnabled(false);
                    selectedProduct = getProductFromName(inputText);
                    if(selectedProduct != null) {
                        addProductBarcode(barcode);
                    } else {
                        // name from list is name of batchItem product
                        activity.addBatchItemBarcode(barcode, inputText);
                        dismiss();
                    }
                } else {
                    textInputProduct.setError(activity.getString(R.string.error_invalid_product));
                }
            }
        });

        view.findViewById(R.id.button_batch_name_discard).setOnClickListener(v -> {
            dismiss();
            activity.resumeScan();
        });

        textInputProduct = view.findViewById(R.id.text_input_batch_choose_name);
        autoCompleteTextViewProduct = (MaterialAutoCompleteTextView) textInputProduct.getEditText();
        assert autoCompleteTextViewProduct != null;
        autoCompleteTextViewProduct.setAdapter(new MatchArrayAdapter(activity, productNames));
        autoCompleteTextViewProduct.setOnItemClickListener(
                (parent, v, position, id) -> {
                    textInputProduct.setErrorEnabled(false);
                    selectedProduct = getProductFromName(
                            String.valueOf(parent.getItemAtPosition(position))
                    );
                }
        );
        autoCompleteTextViewProduct.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                assert productNames != null;
                String inputText = autoCompleteTextViewProduct.getText().toString().trim();
                if(productNames.contains(inputText)) {
                    batchButtonLinkCreate.setText(activity.getString(R.string.action_link));
                    buttonAction = Constants.ACTION.LINK;
                } else if(batchType.equals(Constants.ACTION.PURCHASE)) {
                    batchButtonLinkCreate.setText(activity.getString(R.string.action_create));
                    buttonAction = Constants.ACTION.CREATE;
                } else {
                    batchButtonLinkCreate.setText(activity.getString(R.string.action_link));
                    buttonAction = Constants.ACTION.LINK;
                }
            }
        });

        // GET PRODUCT NAME FROM OPEN FOOD FACTS

        if(activity.isOpenFoodFactsEnabled()) {
            dlHelper.get(
                    OpenFoodFactsApi.getProduct(barcode),
                    response -> {
                        String language = Locale.getDefault().getLanguage();
                        String country = Locale.getDefault().getCountry();
                        String both = language + "_" + country;
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            JSONObject product = jsonObject.getJSONObject("product");
                            String name = product.optString("product_name_" + both);
                            if(name.isEmpty()) {
                                name = product.optString("product_name_" + language);
                            }
                            if(name.isEmpty()) {
                                name = product.optString("product_name");
                            }
                            autoCompleteTextViewProduct.setText(name);
                            if(debug) Log.i(TAG, "onCreateView: OpenFoodFacts = " + name);
                        } catch (JSONException e) {
                            if(debug) Log.e(TAG, "onCreateView: " + e);
                        }
                    },
                    error -> {
                        if(debug) Log.e(TAG, "onCreateView: can't get OpenFoodFacts product");
                    },
                    OpenFoodFactsApi.getUserAgent(activity)
            );
        }

        // Set Input mode -> keyboard hid autocomplete popup, this call solves the issue
        Objects.requireNonNull(Objects.requireNonNull(getDialog()).getWindow())
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dlHelper.destroy();
    }

    private void addProductBarcode(String barcode) {
        List<String> barcodes;
        if(selectedProduct.getBarcode() != null && !selectedProduct.getBarcode().isEmpty()) {
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
            dismissWithMessage(activity.getString(R.string.error_undefined));
            if(debug) Log.e(TAG, "editProductBarcodes: " + e);
        }
        dlHelper.put(
                grocyApi.getObject(GrocyApi.ENTITY.PRODUCTS, selectedProduct.getId()),
                body,
                response -> {
                    activity.loadProductDetailsByBarcode(barcode);
                    dismiss();
                },
                error -> dismissWithMessage(activity.getString(R.string.error_undefined))
        );
    }



    private void dismissWithMessage(String msg) {
        Snackbar.make(
                activity.findViewById(R.id.barcode_scan_batch),
                msg,
                Snackbar.LENGTH_SHORT
        ).show();
        dismiss();
        activity.resumeScan();
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
