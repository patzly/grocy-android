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

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.RestartUtil;

public class LogoutBottomSheet extends BaseBottomSheet {

    private final static String TAG = LogoutBottomSheet.class.getSimpleName();

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

        if(getArguments() != null) {
            // bundle was set to new Bundle() to indicate the demo type
            TextView textViewTitle = view.findViewById(R.id.text_logout_title);
            textViewTitle.setText(activity.getString(R.string.title_logout_demo));
            TextView textViewMsg = view.findViewById(R.id.text_logout_msg);
            textViewMsg.setText(activity.getText(R.string.msg_logout_demo));
        }

        view.findViewById(R.id.button_logout_cancel).setOnClickListener(v -> dismiss());

        view.findViewById(R.id.button_logout_logout).setOnClickListener(v -> {
            PreferenceManager.getDefaultSharedPreferences(activity).edit()
                    .remove(Constants.PREF.SERVER_URL)
                    .remove(Constants.PREF.API_KEY)
                    .remove(Constants.PREF.SHOPPING_LIST_LAST_ID)
                    .commit();
            RestartUtil.restartApp(activity);
        });

        return view;
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
