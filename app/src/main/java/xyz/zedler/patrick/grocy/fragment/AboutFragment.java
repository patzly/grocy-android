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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import xyz.zedler.patrick.grocy.R;
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

    if (activity.binding.bottomAppBar.getVisibility() == View.VISIBLE) {
      activity.updateBottomAppBar(
          Constants.FAB.POSITION.GONE,
          R.menu.menu_empty,
          true,
          () -> {
          }
      );
      activity.getScrollBehavior().setUpScroll(binding.scrollAbout);
      activity.getScrollBehavior().setHideOnScroll(true);
      activity.binding.fabMain.hide();
    }
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
      IconUtil.start(activity, R.id.image_intro);
      navigate(R.id.onboardingFragment);
    } else if (v.getId() == R.id.linear_changelog) {
      IconUtil.start(activity, R.id.image_changelog);
      showTextBottomSheet("changelog", R.string.info_changelog, 0);
    } else if (v.getId() == R.id.linear_developers) {
      IconUtil.start(activity, R.id.image_developers);
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
      IconUtil.start(activity, R.id.image_license_conscrypt);
      showTextBottomSheet(
          "apache",
          R.string.license_conscrypt,
          R.string.url_conscrypt
      );
    } else if (v.getId() == R.id.linear_license_fuzzywuzzy) {
      IconUtil.start(activity, R.id.image_license_fuzzywuzzy);
      showTextBottomSheet(
          "gpl",
          R.string.license_fuzzywuzzy,
          R.string.url_fuzzywuzzy
      );
    } else if (v.getId() == R.id.linear_license_gson) {
      IconUtil.start(activity, R.id.image_license_gson);
      showTextBottomSheet(
          "apache",
          R.string.license_gson,
          R.string.url_gson
      );
    } else if (v.getId() == R.id.linear_license_jost) {
      IconUtil.start(activity, R.id.image_license_jost);
      showTextBottomSheet(
          "ofl",
          R.string.license_jost,
          R.string.url_jost
      );
    } else if (v.getId() == R.id.linear_license_material_components) {
      IconUtil.start(activity, R.id.image_license_material_components);
      showTextBottomSheet(
          "apache",
          R.string.license_material_components,
          R.string.url_material_components
      );
    } else if (v.getId() == R.id.linear_license_material_icons) {
      IconUtil.start(activity, R.id.image_license_material_icons);
      showTextBottomSheet(
          "apache",
          R.string.license_material_icons,
          R.string.url_material_icons
      );
    } else if (v.getId() == R.id.linear_license_netcipher) {
      IconUtil.start(activity, R.id.image_license_netcipher);
      showTextBottomSheet(
          "apache",
          R.string.license_netcipher,
          R.string.url_netcipher
      );
    } else if (v.getId() == R.id.linear_license_volley) {
      IconUtil.start(activity, R.id.image_license_volley);
      showTextBottomSheet(
          "apache",
          R.string.license_volley,
          R.string.url_volley
      );
    } else if (v.getId() == R.id.linear_license_xzing_android) {
      IconUtil.start(activity, R.id.image_license_xzing_android);
      showTextBottomSheet(
          "apache",
          R.string.license_xzing_android,
          R.string.url_zxing_android
      );
    }
  }

  private void showTextBottomSheet(String file, @StringRes int title, @StringRes int link) {
    Bundle bundle = new Bundle();
    bundle.putString(Constants.ARGUMENT.TITLE, getString(title));
    bundle.putString(Constants.ARGUMENT.FILE, file);
    if (link != 0) {
      bundle.putString(Constants.ARGUMENT.LINK, getString(link));
    }
    activity.showBottomSheet(new TextBottomSheet(), bundle);
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    return setStatusBarColor(transit, enter, nextAnim, activity, R.color.primary);
  }
}
