package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.ScanBatchActivity;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.web.RequestQueueSingleton;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class BatchBarcodeBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private final static boolean DEBUG = false;
    private final static String TAG = "BatchBarcodeBottomSheet";

    private ScanBatchActivity activity;

    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private WebRequest request;

    private ArrayList<Product> products = new ArrayList<>();
    //private List<BatchItem> batchItems = new ArrayList<>();
    private ArrayList<String> productNames = new ArrayList<>();

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
                R.layout.fragment_bottomsheet_batch_barcode, container, false
        );

        activity = (ScanBatchActivity) getActivity();
        assert activity != null;

        // set bottom sheet to not cancelable, so buttons have to be pressed
        setCancelable(false);

        // WEB REQUESTS & API

        RequestQueue requestQueue = RequestQueueSingleton
                .getInstance(getContext()).getRequestQueue();
        request = new WebRequest(requestQueue);

        grocyApi = new GrocyApi(activity);

        // VIEWS

        Button batchLink = view.findViewById(R.id.button_batch_link);
        // on click, download products for the following bottom sheet
        batchLink.setOnClickListener(v -> downloadProducts(response -> {
            products = gson.fromJson(response, new TypeToken<List<Product>>(){}.getType());
            productNames = getProductNames();

            Bundle bundle = getArguments();
            // barcode is already in bundle, so give error if getArguments()=0
            if(bundle != null) {
                bundle.putParcelableArrayList(Constants.ARGUMENT.PRODUCTS, products);
                bundle.putStringArrayList(Constants.ARGUMENT.PRODUCT_NAMES, productNames);
                // in the following bottom sheet the user will enter the product
                activity.showBottomSheet(new BatchChooseBottomSheetDialogFragment(), bundle);
                dismiss();
            } else {
                if(DEBUG) Log.i(TAG, "onCreateView: getArguments()=null");
                dismissWithErrorMessage(getString(R.string.msg_error));
            }
        }));

        Button batchCreate = view.findViewById(R.id.button_batch_create);
        // hide button if the bottom sheet was called from purchase batch fragment
        if(getArguments() != null && Objects.equals(
                getArguments().getString(Constants.ARGUMENT.TYPE),
                Constants.ACTION.CONSUME)
        ) {
            batchCreate.setVisibility(View.GONE);
        }
        batchCreate.setOnClickListener(v -> {

        });

        view.findViewById(R.id.button_batch_discard).setOnClickListener(v -> {
            dismiss();
            activity.resume();
        });

        return view;
    }

    private void downloadProducts(OnResponseListener onResponse) {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
                onResponse::onResponse,
                error -> dismissWithErrorMessage(getString(R.string.msg_error))
        );
    }

    private ArrayList<String> getProductNames() {
        ArrayList<String> names = new ArrayList<>();
        if(products != null) {
            for(Product product : products) {
                names.add(product.getName());
            }
        }
        return names;
    }

    public interface OnResponseListener {
        void onResponse(String response);
    }

    private void dismissWithErrorMessage(String msg) {
        activity.showSnackbar(
                Snackbar.make(
                        activity.findViewById(R.id.barcode_scan_batch),
                        msg,
                        Snackbar.LENGTH_SHORT
                )
        );
        dismiss();
        activity.resume();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
