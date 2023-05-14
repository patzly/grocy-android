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
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentLoginApiFormBinding;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.viewmodel.LoginApiFormViewModel;

public class LoginApiFormFragment extends BaseFragment {

  private FragmentLoginApiFormBinding binding;
  private MainActivity activity;
  private LoginApiFormViewModel viewModel;
  private LoginApiFormFragmentArgs args;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentLoginApiFormBinding.inflate(inflater, container, false);
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
    args = LoginApiFormFragmentArgs.fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new LoginApiFormViewModel
        .LoginApiFormViewModelFactory(activity.getApplication(), args)
    ).get(LoginApiFormViewModel.class);
    binding.setFragment(this);
    binding.setClickUtil(new ClickUtil());
    binding.setViewModel(viewModel);
    binding.setFormData(viewModel.getFormData());
    binding.setLifecycleOwner(getViewLifecycleOwner());
    binding.setActivity(activity);

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());
    binding.toolbar.setOnMenuItemClickListener(item -> {
      int id = item.getItemId();
      if (id == R.id.action_help) {
        activity.showHelpBottomSheet();
      } else if (id == R.id.action_feedback) {
        activity.showFeedbackBottomSheet();
      } else if (id == R.id.action_website) {
        openGrocyWebsite();
      } else if (id == R.id.action_settings) {
        activity.navUtil.navigateDeepLink(R.string.deep_link_settingsFragment);
      } else if (id == R.id.action_about) {
        activity.navUtil.navigateDeepLink(R.string.deep_link_aboutFragment);
      }
      return true;
    });

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(false);
    activity.getScrollBehavior().setProvideTopScroll(false);
    activity.getScrollBehavior().setCanBottomAppBarBeVisible(false);
    activity.getScrollBehavior().setBottomBarVisibility(false, true, false);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scroll, false
    );

    new Handler().postDelayed(() -> {
      if (!viewModel.isAutoProceedDoneWasDone() && args.getServerUrl() != null
          && args.getGrocyApiKey() != null && args.getGrocyIngressProxyId() != null
          && args.getHomeAssistantToken() != null && viewModel.getFormData().isFormValid()) {
        viewModel.setAutoProceedDoneWasDone(true);
        proceedWithLogin();
      }
    }, 100);
  }

  public void proceedWithLogin() {
    if (!viewModel.getFormData().isFormValid()) {
      binding.serverUrl.clearFocus();
      binding.token.clearFocus();
      binding.ingressId.clearFocus();
      binding.apiKey.clearFocus();
      activity.hideKeyboard();
      return;
    }
    String ingressProxyId = viewModel.getFormData().getIngressProxyIdTrimmed();
    String hassServerUrl = viewModel.getFormData().getServerUrlTrimmed();
    String grocyServerUrl = viewModel.getFormData().getServerUrlTrimmed();
    if (ingressProxyId != null) {
      grocyServerUrl += "/api/hassio_ingress/" + ingressProxyId;
    }
    activity.navUtil.navigateFragment(
        LoginApiFormFragmentDirections.actionLoginApiFormFragmentToLoginRequestFragment(
            grocyServerUrl,
            viewModel.getFormData().getApiKeyTrimmed()
        ).setHomeAssistantServerUrl(
            ingressProxyId != null ? hassServerUrl : null
        ).setHomeAssistantToken(
            ingressProxyId != null
                ? viewModel.getFormData().getLongLivedAccessTokenTrimmed()
                : null
        )
    );
  }

  public void openHomeAssistantProfileWebsite() {
    if (!viewModel.getFormData().isServerUrlValid()) {
      return;
    }
    Intent browserManageKeys = new Intent(Intent.ACTION_VIEW);
    String url = viewModel.getFormData().getServerUrlTrimmed();
    url += "/profile";
    Uri uri = Uri.parse(url);
    browserManageKeys.setData(uri);
    startActivity(browserManageKeys);
  }

  public void openApiKeysWebsite() {
    if (!viewModel.getFormData().isServerUrlValid()) {
      return;
    }
    Intent browserManageKeys = new Intent(Intent.ACTION_VIEW);
    String url = viewModel.getFormData().getServerUrlTrimmed();
    if (!viewModel.getFormData().getUsingGrocyHassAddOn()) {
      url += "/manageapikeys";
    }
    Uri uri = Uri.parse(url);
    browserManageKeys.setData(uri);
    startActivity(browserManageKeys);
  }

  public void openNabuCasaWebsite() {
    Uri uri = Uri.parse("https://www.nabucasa.com/");
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(uri);
    startActivity(intent);
  }

  public void openGrocyWebsite() {
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_grocy))));
  }
}
