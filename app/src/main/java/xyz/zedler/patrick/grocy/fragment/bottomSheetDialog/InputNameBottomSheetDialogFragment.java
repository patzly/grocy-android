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
import com.google.android.material.snackbar.Snackbar;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.PurchaseFragment;
import xyz.zedler.patrick.grocy.util.Constants;

public class InputNameBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private final static boolean DEBUG = false;
    private final static String TAG = "InputNameBottomSheet";

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
                R.layout.fragment_bottomsheet_input_name, container, false
        );

        activity = (MainActivity) getActivity();
        assert activity != null;

        if(getArguments() == null
                || getArguments().getString(Constants.ARGUMENT.PRODUCT_NAME) == null
        ) {
            dismissWithMessage(activity.getString(R.string.msg_error));
            return view;
        }

        setCancelable(false);

        String productName = getArguments().getString(Constants.ARGUMENT.PRODUCT_NAME);

        TextView textView = view.findViewById(R.id.text_input_name_question);
        textView.setText("Create new product with the name \"" + productName + "\"?");

        view.findViewById(R.id.button_input_name_create).setOnClickListener(v -> {
            Fragment current = activity.getCurrentFragment();
            if(current.getClass() == PurchaseFragment.class) {
                Bundle bundle = new Bundle();
                bundle.putString(Constants.ARGUMENT.ACTION, Constants.ACTION.CREATE_THEN_PURCHASE);
                bundle.putString(Constants.ARGUMENT.PRODUCT_NAME, productName);
                activity.replaceFragment(Constants.UI.MASTER_PRODUCT_EDIT_SIMPLE, bundle, true);
            }
            dismiss();
        });

        view.findViewById(R.id.button_input_name_cancel).setOnClickListener(v -> {
            /*Fragment current = activity.getCurrentFragment();
            if(current.getClass() == PurchaseFragment.class) {
                ((PurchaseFragment) current).clearAll();
            }*/
            dismiss();
        });

        return view;
    }

    private void dismissWithMessage(String msg) {
        Snackbar.make(
                activity.findViewById(R.id.linear_container_main),
                msg,
                Snackbar.LENGTH_SHORT
        ).show();
        dismiss();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
