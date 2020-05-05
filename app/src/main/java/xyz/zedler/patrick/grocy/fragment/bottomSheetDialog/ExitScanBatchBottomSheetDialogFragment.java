package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.ScanBatchActivity;
import xyz.zedler.patrick.grocy.util.Constants;

public class ExitScanBatchBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private final static boolean DEBUG = false;
    private final static String TAG = "ExitScanBatchBottomSheet";

    private ScanBatchActivity activity;

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
                R.layout.fragment_bottomsheet_exit_scan_batch, container, false
        );

        activity = (ScanBatchActivity) getActivity();
        assert activity != null;

        Bundle bundle = getArguments();
        if(bundle == null) {
            dismiss();
            Snackbar.make(
                    view,
                    activity.getString(R.string.msg_error),
                    Snackbar.LENGTH_SHORT
            ).show();
        }

        view.findViewById(R.id.button_exit_scan_batch_open).setOnClickListener(v -> {
            dismiss();
            activity.setResult(
                    Activity.RESULT_OK,
                    new Intent().putExtra(Constants.ARGUMENT.BUNDLE, bundle)
            );
            activity.finish();
        });

        view.findViewById(R.id.button_exit_scan_batch_discard).setOnClickListener(v -> {
            dismiss();
            activity.setResult(Activity.RESULT_CANCELED);
            activity.finish();
        });

        return view;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        activity.resumeScan();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
