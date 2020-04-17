package xyz.zedler.patrick.grocy.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;

public class ConsumeBarcodeBottomSheetDialogFragment extends BottomSheetDialogFragment {

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
                R.layout.fragment_bottomsheet_consume_barcode, container, false
        );

        activity = (MainActivity) getActivity();
        assert activity != null;

        setCancelable(false);

        view.findViewById(R.id.button_consume_barcode_add).setOnClickListener(v -> {
            Fragment current = activity.getCurrentFragment();
            if(current.getClass() == ConsumeFragment.class) {
                ((ConsumeFragment) current).addInputAsBarcode();
            }
            dismiss();
        });

        view.findViewById(R.id.button_consume_barcode_cancel).setOnClickListener(v -> {
            Fragment current = activity.getCurrentFragment();
            if(current.getClass() == ConsumeFragment.class) {
                ((ConsumeFragment) current).clearAll();
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
