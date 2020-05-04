package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputLayout;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.ScanBatchActivity;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class InputPriceBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private final static boolean DEBUG = false;
    private final static String TAG = "InputBBDateBottomSheetDialogFragment";

    private ScanBatchActivity activity;

    private ProductDetails productDetails;

    private TextInputLayout textInputPrice;
    private EditText editTextPrice;

    private boolean productDiscarded = false;

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
                R.layout.fragment_bottomsheet_input_price, container, false
        );

        activity = (ScanBatchActivity) getActivity();
        Bundle bundle = getArguments();
        assert activity != null && bundle != null;

        String price = bundle.getString(Constants.ARGUMENT.PRICE);
        String currency = bundle.getString(Constants.ARGUMENT.CURRENCY);
        productDetails = bundle.getParcelable(Constants.ARGUMENT.PRODUCT_DETAILS);

        textInputPrice = view.findViewById(R.id.text_input_price);
        textInputPrice.setHint(getString(R.string.property_price_in, currency));

        editTextPrice = textInputPrice.getEditText();

        view.findViewById(R.id.button_input_price_save).setOnClickListener(
                v -> dismiss()
        );
        view.findViewById(R.id.button_input_price_discard).setOnClickListener(
                v -> {
                    activity.discardCurrentProduct();
                    productDiscarded = true;
                    dismiss();
                }
        );

        setCancelable(false);

        fillForm(price);

        return view;
    }

    private void fillForm(String price) {
        if(price != null) {

            editTextPrice.setText(price);

        } else if(productDetails != null) {

            String lastPrice = productDetails.getLastPrice();
            if(lastPrice != null) {
                lastPrice = NumUtil.formatPrice(lastPrice);
                editTextPrice.setText(lastPrice);
            } else {
                editTextPrice.requestFocus();
                activity.showKeyboard(editTextPrice); // TODO: Does not work
            }
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if(productDiscarded) return;

        String price = editTextPrice.getText().toString();
        price = NumUtil.formatPrice(price);

        activity.setPrice(price);
        activity.askNecessaryDetails();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
