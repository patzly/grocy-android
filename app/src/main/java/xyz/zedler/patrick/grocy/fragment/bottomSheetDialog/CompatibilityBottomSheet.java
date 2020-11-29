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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.util.Constants;

public class CompatibilityBottomSheet extends CustomBottomSheet {

    private final static String TAG = CompatibilityBottomSheet.class.getSimpleName();

    private MainActivity activity;

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
                R.layout.fragment_bottomsheet_compatibility, container, false
        );

        activity = (MainActivity) getActivity();
        assert activity != null;

        if(getArguments() == null) {
            dismiss();
            return view;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);

        ArrayList<String> supportedVersions = getArguments().getStringArrayList(
                Constants.ARGUMENT.SUPPORTED_VERSIONS
        );
        assert supportedVersions != null;
        String currentVersion = getArguments().getString(Constants.ARGUMENT.VERSION);

        TextView textViewMsg = view.findViewById(R.id.text_compatibility_msg);
        textViewMsg.setText(activity.getString(
                R.string.msg_compatibility,
                currentVersion,
                supportedVersions.get(0),
                supportedVersions.get(supportedVersions.size()-1)
        ));

        view.findViewById(R.id.button_compatibility_cancel).setOnClickListener(v -> {
            dismiss();
            activity.getCurrentFragment().enableLoginButtons();
        });

        view.findViewById(R.id.button_compatibility_ignore).setOnClickListener(v -> {
            prefs.edit().putString(Constants.PREF.VERSION_COMPATIBILITY_IGNORED, currentVersion)
                    .apply();

            if(activity instanceof LoginActivity) {
                String server = getArguments().getString(Constants.ARGUMENT.SERVER);
                String key = getArguments().getString(Constants.ARGUMENT.KEY);
                boolean isDemo = getArguments().getBoolean(Constants.ARGUMENT.DEMO_CHOSEN);
                activity.getCurrentFragment().requestLogin(server, key, false, isDemo);
                activity.getCurrentFragment().enableLoginButtons();
            }
            dismiss();
        });

        setCancelable(false);

        return view;
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
