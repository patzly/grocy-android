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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.grocy.NavigationMainDirections;
import xyz.zedler.patrick.grocy.NavigationMainDirections.ActionGlobalOnboardingFragment;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentAboutBinding;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

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
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    binding.setActivity(activity);
    binding.setFragment(this);

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBarAbout);
    systemBarBehavior.setScroll(binding.scrollAbout, binding.constraint);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbarAbout.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

    setOnClickListeners(
        view,
        R.id.linear_intro,
        R.id.linear_changelog,
        R.id.linear_developers,
        R.id.linear_github,
        R.id.linear_license_conscrypt,
        R.id.linear_license_fuzzywuzzy,
        R.id.linear_license_gson,
        R.id.linear_license_jost,
        R.id.linear_license_material_components,
        R.id.linear_license_material_icons,
        R.id.linear_license_netcipher,
        R.id.linear_license_volley,
        R.id.linear_license_xzing_android
    );

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(false);
    activity.getScrollBehavior().setUpScroll(
        binding.appBarAbout, false, binding.scrollAbout
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(false, R.menu.menu_empty);
  }

  private void setOnClickListeners(View view, @IdRes int... viewIds) {
    for (int viewId : viewIds) {
      view.findViewById(viewId).setOnClickListener(this);
    }
  }

  @Override
  public void onClick(View v) {
    if (clickUtil.isDisabled()) {
      return;
    }

    if (v.getId() == R.id.linear_intro) {
      ViewUtil.startIcon(binding.imageIntro);
      ActionGlobalOnboardingFragment directions
          = NavigationMainDirections.actionGlobalOnboardingFragment();
      directions.setShowAgain(true);
      activity.navUtil.navigateFragment(directions);
    } else if (v.getId() == R.id.linear_changelog) {
      ViewUtil.startIcon(binding.imageChangelog);
      VersionUtil.showChangelogBottomSheet(activity);
    } else if (v.getId() == R.id.linear_developers) {
      ViewUtil.startIcon(binding.imageDevelopers);
      startActivity(new Intent(
          Intent.ACTION_VIEW,
          Uri.parse(getString(R.string.url_developer))
      ));
    } else if (v.getId() == R.id.linear_github) {
      startActivity(new Intent(
          Intent.ACTION_VIEW,
          Uri.parse(getString(R.string.url_github))
      ));
    } else if (v.getId() == R.id.linear_license_conscrypt) {
      ViewUtil.startIcon(binding.imageLicenseConscrypt);
      activity.showTextBottomSheet(
          R.raw.license_apache,
          R.string.license_conscrypt,
          R.string.url_conscrypt
      );
    } else if (v.getId() == R.id.linear_license_fuzzywuzzy) {
      ViewUtil.startIcon(binding.imageLicenseFuzzywuzzy);
      activity.showTextBottomSheet(
          R.raw.license_gpl,
          R.string.license_fuzzywuzzy,
          R.string.url_fuzzywuzzy
      );
    } else if (v.getId() == R.id.linear_license_gson) {
      ViewUtil.startIcon(binding.imageLicenseGson);
      activity.showTextBottomSheet(
          R.raw.license_apache,
          R.string.license_gson,
          R.string.url_gson
      );
    } else if (v.getId() == R.id.linear_license_jost) {
      ViewUtil.startIcon(binding.imageLicenseJost);
      activity.showTextBottomSheet(
          R.raw.license_ofl,
          R.string.license_jost,
          R.string.url_jost
      );
    } else if (v.getId() == R.id.linear_license_material_components) {
      ViewUtil.startIcon(binding.imageLicenseMaterialComponents);
      activity.showTextBottomSheet(
          R.raw.license_apache,
          R.string.license_material_components,
          R.string.url_material_components
      );
    } else if (v.getId() == R.id.linear_license_material_icons) {
      ViewUtil.startIcon(binding.imageLicenseMaterialIcons);
      activity.showTextBottomSheet(
          R.raw.license_apache,
          R.string.license_material_icons,
          R.string.url_material_icons
      );
    } else if (v.getId() == R.id.linear_license_netcipher) {
      ViewUtil.startIcon(binding.imageLicenseNetcipher);
      activity.showTextBottomSheet(
          R.raw.license_apache,
          R.string.license_netcipher,
          R.string.url_netcipher
      );
    } else if (v.getId() == R.id.linear_license_volley) {
      ViewUtil.startIcon(binding.imageLicenseVolley);
      activity.showTextBottomSheet(
          R.raw.license_apache,
          R.string.license_volley,
          R.string.url_volley
      );
    } else if (v.getId() == R.id.linear_license_xzing_android) {
      ViewUtil.startIcon(binding.imageLicenseXzingAndroid);
      activity.showTextBottomSheet(
          R.raw.license_apache,
          R.string.license_xzing_android,
          R.string.url_zxing_android
      );
    }
  }
}
