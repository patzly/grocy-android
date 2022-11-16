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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentLoginIntroBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.FeedbackBottomSheet;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.NetUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class LoginIntroFragment extends BaseFragment {

  private FragmentLoginIntroBinding binding;
  private MainActivity activity;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentLoginIntroBinding.inflate(inflater, container, false);
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
    binding.setFragment(this);
    binding.setClickUtil(new ClickUtil());

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setContainer(binding.linearContainerScroll);
    systemBarBehavior.setUp();

    binding.imageLogo.setOnClickListener(v -> ViewUtil.startIcon(binding.imageLogo));

    activity.getScrollBehavior().setProvideTopScroll(false);
    activity.getScrollBehavior().setCanBottomAppBarBeVisible(false);
    activity.getScrollBehavior().setBottomBarVisibility(false, true, false);
  }

  public void loginDemoInstance() {
    activity.navigateFragment(
        LoginIntroFragmentDirections.actionLoginIntroFragmentToLoginRequestFragment(
            getString(R.string.url_grocy_demo),
            ""
        )
    );
  }

  public void loginOwnInstance() {
    PackageManager pm = activity.getPackageManager();
    if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
      activity.navigateFragment(
          LoginIntroFragmentDirections.actionLoginIntroFragmentToLoginApiQrCodeFragment()
      );
    } else {
      activity.navigateFragment(
          LoginIntroFragmentDirections.actionLoginIntroFragmentToLoginApiFormFragment()
      );
    }
  }

  public void openHelpWebsite() {
    boolean success = NetUtil.openURL(activity, Constants.URL.HELP);
    if (!success) {
      activity.showSnackbar(R.string.error_no_browser);
    }
  }

  public void openGrocyWebsite() {
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_grocy))));
  }

  public void showFeedbackBottomSheet() {
    activity.showBottomSheet(new FeedbackBottomSheet());
  }

  @Override
  public boolean onBackPressed() {
    activity.finish();
    return true;
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    return setStatusBarColor(transit, enter, nextAnim, activity, R.color.background);
  }
}
