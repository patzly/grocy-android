package xyz.zedler.patrick.grocy.fragment;

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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.FeaturesActivity;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentAboutBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.TextBottomSheet;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;

public class AboutFragment extends BaseFragment implements View.OnClickListener {

    private final static String TAG = AboutFragment.class.getSimpleName();

    private FragmentAboutBinding binding;
    private MainActivity activity;
    private final ClickUtil clickUtil = new ClickUtil();

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);

        activity = (MainActivity) requireActivity();

        // Add bottom margin if bottomAppBar is visible
        TypedValue tv = new TypedValue();
        if(activity.binding.bottomAppBar.getVisibility() == View.VISIBLE && activity.getTheme()
                .resolveAttribute(android.R.attr.actionBarSize, tv, true)
        ) {
            int actionBarHeight = TypedValue
                    .complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            CoordinatorLayout.LayoutParams layoutParams =
                    (CoordinatorLayout.LayoutParams) binding.scrollAbout.getLayoutParams();
            layoutParams.setMargins(0, 0, 0, actionBarHeight);
            binding.scrollAbout.setLayoutParams(layoutParams);
        }

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        binding.frameAboutBack.setOnClickListener(v -> activity.onBackPressed());

        setOnClickListeners(
                R.id.linear_intro,
                R.id.linear_changelog,
                R.id.linear_developer,
                R.id.linear_github,
                R.id.linear_license_material_components,
                R.id.linear_license_material_icons,
                R.id.linear_license_roboto,
                R.id.linear_license_volley,
                R.id.linear_license_gson,
                R.id.linear_license_xzing_android
        );
    }

    private void setOnClickListeners(@IdRes int... viewIds) {
        for (int viewId : viewIds) {
            activity.findViewById(viewId).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        if(clickUtil.isDisabled()) return;

        switch(v.getId()) {
            case R.id.linear_intro:
                IconUtil.start(activity, R.id.image_intro);
                new Handler().postDelayed(
                        () -> startActivity(new Intent(activity, FeaturesActivity.class)),
                        150
                );
                break;
            case R.id.linear_changelog:
                IconUtil.start(activity, R.id.image_changelog);
                showTextBottomSheet("CHANGELOG", R.string.info_changelog, 0);
                break;
            case R.id.linear_developer:
                IconUtil.start(activity, R.id.image_developer);
                new Handler().postDelayed(
                        () -> startActivity(
                                new Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(getString(R.string.url_developer))
                                )
                        ), 300
                );
                break;
            case R.id.linear_github:
                startActivity(
                        new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.url_github))
                        )
                );
                break;
            case R.id.linear_license_material_components:
                IconUtil.start(activity, R.id.image_license_material_components);
                showTextBottomSheet(
                        "APACHE",
                        R.string.license_material_components,
                        R.string.url_material_components
                );
                break;
            case R.id.linear_license_material_icons:
                IconUtil.start(activity, R.id.image_license_material_icons);
                showTextBottomSheet(
                        "APACHE",
                        R.string.license_material_icons,
                        R.string.url_material_icons
                );
                break;
            case R.id.linear_license_roboto:
                IconUtil.start(activity, R.id.image_license_roboto);
                showTextBottomSheet(
                        "APACHE",
                        R.string.license_roboto,
                        R.string.url_roboto
                );
                break;
            case R.id.linear_license_volley:
                IconUtil.start(activity, R.id.image_license_volley);
                showTextBottomSheet(
                        "APACHE",
                        R.string.license_volley,
                        R.string.url_volley
                );
                break;
            case R.id.linear_license_gson:
                IconUtil.start(activity, R.id.image_license_gson);
                showTextBottomSheet(
                        "APACHE",
                        R.string.license_gson,
                        R.string.url_gson
                );
                break;
            case R.id.linear_license_xzing_android:
                IconUtil.start(activity, R.id.image_license_xzing_android);
                showTextBottomSheet(
                        "APACHE",
                        R.string.license_xzing_android,
                        R.string.url_zxing_android
                );
                break;
        }
    }

    private void showTextBottomSheet(String file, @StringRes int title, @StringRes int link) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.TITLE, getString(title));
        bundle.putString(Constants.ARGUMENT.FILE, file);
        if(link != 0) bundle.putString(Constants.ARGUMENT.LINK, getString(link));
        activity.showBottomSheet(new TextBottomSheet(), bundle);
    }
}
