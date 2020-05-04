package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.ConsumeFragment;
import xyz.zedler.patrick.grocy.fragment.PurchaseFragment;
import xyz.zedler.patrick.grocy.util.Constants;

public class InputBarcodeBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private final static boolean DEBUG = false;
    private final static String TAG = "ConsumeBarcodeBottomSheet";

    private MainActivity activity;

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
                R.layout.fragment_bottomsheet_input_barcode, container, false
        );

        activity = (MainActivity) getActivity();
        assert activity != null;

        setCancelable(false);

        Fragment currentFragment = activity.getCurrentFragment();

        TextView textView = view.findViewById(R.id.text_input_barcode_question);
        MaterialButton buttonNew = view.findViewById(R.id.button_input_barcode_new);

        if(currentFragment.getClass() == ConsumeFragment.class) {
            textView.setText("Link barcode to an existing product?");
            buttonNew.setVisibility(View.GONE);
        } else { // PurchaseFragment
            textView.setText("Link barcode to an existing product or create a new product with it?");
            buttonNew.setVisibility(View.VISIBLE);
        }

        view.findViewById(R.id.button_input_barcode_link).setOnClickListener(v -> {
            if(currentFragment.getClass() == ConsumeFragment.class) {
                ((ConsumeFragment) currentFragment).addInputAsBarcode();
            } else if(currentFragment.getClass() == PurchaseFragment.class) {
                ((PurchaseFragment) currentFragment).addInputAsBarcode();
            }
            dismiss();
        });

        view.findViewById(R.id.button_input_barcode_new).setOnClickListener(v -> {
            Fragment current = activity.getCurrentFragment();
            if(current.getClass() == PurchaseFragment.class && getArguments() != null) {
                Bundle bundle = new Bundle();
                bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.CREATE_THEN_PURCHASE);
                bundle.putString(
                        Constants.ARGUMENT.BARCODES,
                        getArguments().getString(Constants.ARGUMENT.BARCODES)
                );
                activity.replaceFragment(Constants.UI.MASTER_PRODUCT_EDIT_SIMPLE, bundle, true);
            }
            dismiss();
        });

        view.findViewById(R.id.button_input_barcode_cancel).setOnClickListener(v -> {
            if(currentFragment.getClass() == ConsumeFragment.class) {
                ((ConsumeFragment) currentFragment).clearAll();
            } else if(currentFragment.getClass() == PurchaseFragment.class) {
                ((PurchaseFragment) currentFragment).clearAll();
            }
            dismiss();
        });

        return view;
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
