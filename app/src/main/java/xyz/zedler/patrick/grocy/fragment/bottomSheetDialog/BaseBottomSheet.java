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

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.Constants;


/**
 * This is an extended BottomSheetDialogFragment class. The one overridden method fixes the
 * weird behavior of bottom sheets in landscape mode. All bottom sheets in this app should use this
 * extended class to apply the fix.
 */
public class BaseBottomSheet extends BottomSheetDialogFragment {
    private boolean skipCollapsedStateInPortrait = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean expandBottomSheets = sharedPrefs.getBoolean(
                Constants.SETTINGS.BEHAVIOR.EXPAND_BOTTOM_SHEETS,
                Constants.SETTINGS_DEFAULT.BEHAVIOR.EXPAND_BOTTOM_SHEETS
        );

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(view.getViewTreeObserver().isAlive()) {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }

                BottomSheetBehavior<View> behavior = getBehavior();
                if(behavior == null) return;

                int orientation = getResources().getConfiguration().orientation;
                if(orientation == Configuration.ORIENTATION_PORTRAIT && skipCollapsedStateInPortrait) {
                    DisplayMetrics metrics = new DisplayMetrics();
                    requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    behavior.setPeekHeight(metrics.heightPixels);
                    behavior.setSkipCollapsed(true);
                } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    DisplayMetrics metrics = new DisplayMetrics();
                    requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    behavior.setPeekHeight(
                            expandBottomSheets ? metrics.heightPixels : metrics.heightPixels / 2
                    );
                }
            }
        });
    }

    @Nullable
    BottomSheetBehavior<View> getBehavior() {
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if(dialog == null) return null;
        View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if(sheet == null) return null;
        return BottomSheetBehavior.from(sheet);
    }

    void setSkipCollapsedInPortrait() {
        skipCollapsedStateInPortrait = true;
    }

    @NonNull
    NavController findNavController() {
        return NavHostFragment.findNavController(this);
    }

    void navigate(NavDirections directions) {
        findNavController().navigate(directions);
    }

    void navigate(NavDirections directions, @NonNull Navigator.Extras navigatorExtras) {
        findNavController().navigate(directions, navigatorExtras);
    }

    void navigate(NavDirections directions, @NonNull NavOptions navOptions) {
        findNavController().navigate(directions, navOptions);
    }

    void navigate(@IdRes int destination) {
        navigate(destination, (Bundle) null);
    }

    void navigate(@IdRes int destination, Bundle arguments) {
        NavOptions.Builder builder = new NavOptions.Builder();
        builder.setEnterAnim(R.anim.slide_in_up)
                .setPopExitAnim(R.anim.slide_out_down)
                .setExitAnim(R.anim.slide_no);
        findNavController().navigate(destination, arguments, builder.build());
    }

    void navigate(@IdRes int destination, @NonNull NavOptions navOptions) {
        findNavController().navigate(destination, null, navOptions);
    }

    void navigateDeepLink(@NonNull String uri) {
        NavOptions.Builder builder = new NavOptions.Builder();
        builder.setEnterAnim(R.anim.slide_in_up)
                .setPopExitAnim(R.anim.slide_out_down)
                .setExitAnim(R.anim.slide_no);
        findNavController().navigate(Uri.parse(uri), builder.build());
    }
}
