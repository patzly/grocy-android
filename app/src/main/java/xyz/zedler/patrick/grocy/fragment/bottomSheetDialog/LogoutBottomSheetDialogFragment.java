package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.RestartActivity;
import xyz.zedler.patrick.grocy.util.Constants;

public class LogoutBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private final static String TAG = "LogoutBottomSheet";

    private Activity activity;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.fragment_bottomsheet_logout, container, false
        );

        activity = getActivity();
        assert activity != null;

        view.findViewById(R.id.button_logout_cancel).setOnClickListener(v -> dismiss());

        view.findViewById(R.id.button_logout_logout).setOnClickListener(v -> {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
            sharedPrefs.edit().clear().commit();
            sharedPrefs.edit().putBoolean(Constants.PREF.INTRO_SHOWN, true).commit();
            RestartActivity.restartApp(activity);
        });

        return view;
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
