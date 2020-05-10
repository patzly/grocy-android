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
