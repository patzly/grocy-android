package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputLayout;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.SettingsActivity;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class DefaultAmountBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private final static boolean DEBUG = false;
    private final static String TAG = "DefaultAmountBottomSheet";

    private SettingsActivity activity;

    private EditText editTextDefaultAmount;

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
                R.layout.fragment_bottomsheet_default_amount, container, false
        );

        activity = (SettingsActivity) getActivity();
        /*Bundle bundle = getArguments();
        assert activity != null && bundle != null;*/

        TextInputLayout textInputDefaultAmount = view.findViewById(R.id.text_input_default_amount);
        editTextDefaultAmount = textInputDefaultAmount.getEditText();
        assert editTextDefaultAmount != null;
        editTextDefaultAmount.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        save();
                        return true;
                    } return false;
                });

        view.findViewById(R.id.button_default_amount_cancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.button_default_amount_save).setOnClickListener(v -> {
            save();
        });

        editTextDefaultAmount.setText("1");
        editTextDefaultAmount.requestFocus();
        new Handler().postDelayed(
                () -> activity.showKeyboard(editTextDefaultAmount),
                200
        );

        return view;
    }

    private void save() {
        activity.setAmountPurchase(
                NumUtil.stringToDouble(editTextDefaultAmount.getText().toString())
        );
        dismiss();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
