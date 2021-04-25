/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler & Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Activity;
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
import androidx.annotation.StringRes;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.net.URLEncoder;

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

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(
                requireContext()
        );
        boolean expandBottomSheets = sharedPrefs.getBoolean(
                Constants.SETTINGS.BEHAVIOR.EXPAND_BOTTOM_SHEETS,
                Constants.SETTINGS_DEFAULT.BEHAVIOR.EXPAND_BOTTOM_SHEETS
        );

        view.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if(view.getViewTreeObserver().isAlive()) {
                            view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }

                        BottomSheetBehavior<View> behavior = getBehavior();
                        if(behavior == null) return;

                        int orientation = getResources().getConfiguration().orientation;
                        if(orientation == Configuration.ORIENTATION_PORTRAIT
                                && skipCollapsedStateInPortrait
                        ) {
                            behavior.setPeekHeight(getDisplayHeight(requireActivity()));
                            behavior.setSkipCollapsed(true);
                        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                            behavior.setPeekHeight(getDisplayHeight(requireActivity()) / 2);
                        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            behavior.setPeekHeight(
                                    expandBottomSheets
                                            ? getDisplayHeight(requireActivity())
                                            : getDisplayHeight(requireActivity()) / 2
                            );
                        }
                    }
                });
    }

    private static int getDisplayHeight(Activity activity) {
        // important for targeting SDK 11
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
            Insets insets = windowMetrics.getWindowInsets().getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars()
            );
            return (windowMetrics.getBounds().height() - insets.top - insets.bottom) / 2;
        } else {*/
            DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.heightPixels;
        //}
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

    void navigateDeepLink(@NonNull Uri uri) {
        NavOptions.Builder builder = new NavOptions.Builder();
        builder.setEnterAnim(R.anim.slide_in_up)
                .setPopExitAnim(R.anim.slide_out_down)
                .setExitAnim(R.anim.slide_no);
        findNavController().navigate(uri, builder.build());
    }

    void navigateDeepLink(@NonNull String uri) {
        navigateDeepLink(Uri.parse(uri));
    }

    void navigateDeepLink(@NonNull String uri, @NonNull Bundle args) {
        navigateDeepLink(getUriWithArgs(uri, args));
    }

    Uri getUriWithArgs(@NonNull String uri, @NonNull Bundle argsBundle) {
        String[] parts = uri.split("\\?");
        if(parts.length == 1) return Uri.parse(uri);
        String linkPart = parts[0];
        String argsPart = parts[parts.length-1];
        String[] pairs = argsPart.split("&");
        String finalDeepLink = linkPart + "?";
        for(int i=0; i<=pairs.length-1; i++) {
            String pair = pairs[i];
            String key = pair.split("=")[0];
            Object valueBundle = argsBundle.get(key);
            if(valueBundle == null) continue;
            try {
                finalDeepLink += key + "=" + URLEncoder.encode(valueBundle.toString(), "UTF-8");
            } catch (Throwable ignore) {}
            if(i != pairs.length-1) finalDeepLink += "&";
        }
        return Uri.parse(finalDeepLink);
    }

    Uri getUriWithArgs(@StringRes int uri, @NonNull Bundle args) {
        return getUriWithArgs(getString(uri), args);
    }
}
