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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentLoginApiQrCodeBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.FeedbackBottomSheet;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner.BarcodeListener;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScannerBundle;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.NetUtil;

public class LoginApiQrCodeFragment extends BaseFragment implements BarcodeListener {

  private static final int SCAN_GROCY_KEY = 0;
  private static final int SCAN_HASS_TOKEN = 1;

  private FragmentLoginApiQrCodeBinding binding;
  private MainActivity activity;
  private LoginApiQrCodeFragmentArgs args;
  private EmbeddedFragmentScanner embeddedFragmentScanner;
  private int pageStatus = SCAN_GROCY_KEY;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentLoginApiQrCodeBinding.inflate(inflater, container, false);
    embeddedFragmentScanner = new EmbeddedFragmentScannerBundle(
        this,
        binding.containerScanner,
        this,
        true,
        false
    );
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
    args = LoginApiQrCodeFragmentArgs.fromBundle(requireArguments());
    pageStatus = args.getGrocyApiKey() == null ? SCAN_GROCY_KEY : SCAN_HASS_TOKEN;

    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setClickUtil(new ClickUtil());
    embeddedFragmentScanner.setScannerVisibilityLive(new MutableLiveData<>(true));

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setScroll(binding.scroll, binding.linearContainerScroll);
    systemBarBehavior.setUp();

    binding.toolbar.setNavigationOnClickListener(v -> activity.navigateUp());
    binding.toolbar.setOnMenuItemClickListener(item -> {
      int id = item.getItemId();
      if (id == R.id.action_help) {
        openHelpWebsite();
      } else if (id == R.id.action_feedback) {
        showFeedbackBottomSheet();
      } else if (id == R.id.action_website) {
        openGrocyWebsite();
      } else if (id == R.id.action_settings) {
        activity.navigateDeepLink(R.string.deep_link_settingsFragment);
      } else if (id == R.id.action_about) {
        activity.navigateDeepLink(R.string.deep_link_aboutFragment);
      }
      return true;
    });

    activity.getScrollBehavior().setProvideTopScroll(false);
    activity.getScrollBehavior().setCanBottomAppBarBeVisible(false);
    activity.getScrollBehavior().setBottomBarVisibility(false, true, false);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scroll, false
    );
  }

  @Override
  public void onResume() {
    super.onResume();
    embeddedFragmentScanner.onResume();
  }

  @Override
  public void onPause() {
    embeddedFragmentScanner.onPause();
    super.onPause();
  }

  @Override
  public void onDestroy() {
    if (embeddedFragmentScanner != null) embeddedFragmentScanner.onDestroy();
    super.onDestroy();
  }

  @Override
  public void onBarcodeRecognized(String rawValue) {
    if(pageStatus == SCAN_GROCY_KEY) {
      String[] resultSplit = rawValue.split("\\|");
      if (resultSplit.length != 2) {
        activity.showSnackbar(R.string.error_api_qr_code);
        embeddedFragmentScanner.startScannerIfVisible();
        return;
      }
      String apiURL = resultSplit[0];
      String serverURL = apiURL.replaceAll("/api$", "");
      String ingressProxyId = null;
      String serverURLHomeAssistant = null;
      if (serverURL.contains("/api/hassio_ingress/")) {
        String[] serverURLAndIngressProxyId = serverURL.split("/api/hassio_ingress/");
        serverURLHomeAssistant = serverURLAndIngressProxyId[0];
        if (serverURLHomeAssistant != null && serverURLHomeAssistant.isEmpty()) {
          serverURLHomeAssistant = null;
        }
        ingressProxyId = serverURLAndIngressProxyId[1];
      }
      String apiKey = resultSplit[1];

      if (ingressProxyId == null) {
        activity.navigateFragment(LoginApiQrCodeFragmentDirections
            .actionLoginApiQrCodeFragmentToLoginRequestFragment(serverURL, apiKey));
      } else { // grocy home assistant add-on used
        activity.navigateFragment(LoginApiQrCodeFragmentDirections
            .actionLoginApiQrCodeFragmentSelf()
            .setServerURL(serverURLHomeAssistant)
            .setGrocyIngressProxyId(ingressProxyId)
            .setGrocyApiKey(apiKey));
      }
    } else if (pageStatus == SCAN_HASS_TOKEN) {
      String[] resultSplit = rawValue.split("\\.");
      if (resultSplit.length != 3) {
        activity.showSnackbar(R.string.error_token_qr_code);
        embeddedFragmentScanner.startScannerIfVisible();
        return;
      }
      activity.navigateFragment(LoginApiQrCodeFragmentDirections
          .actionLoginApiQrCodeFragmentToLoginApiFormFragment()
          .setServerUrl(args.getServerURL())
          .setGrocyIngressProxyId(args.getGrocyIngressProxyId())
          .setGrocyApiKey(args.getGrocyApiKey())
          .setHomeAssistantToken(rawValue));
    }
  }

  public void toggleTorch() {
    embeddedFragmentScanner.toggleTorch();
  }

  public void enterDataManually() {
    activity.navigateFragment(
        LoginApiQrCodeFragmentDirections.actionLoginApiQrCodeFragmentToLoginApiFormFragment()
    );
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

  public boolean isPageForGrocyKey() {
    return pageStatus == SCAN_GROCY_KEY;
  }
}
