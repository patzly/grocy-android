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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentLoginApiQrCodeBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.FeedbackBottomSheet;
import xyz.zedler.patrick.grocy.scan.ScanInputCaptureManager;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NetUtil;

public class LoginApiQrCodeFragment extends BaseFragment implements ScanInputCaptureManager.BarcodeListener {

    private FragmentLoginApiQrCodeBinding binding;
    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private ScanInputCaptureManager capture;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentLoginApiQrCodeBinding.inflate(inflater, container, false);
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
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        DecoratedBarcodeView barcodeScannerView = binding.barcodeScanInput;
        barcodeScannerView.setTorchOff();
        CameraSettings cameraSettings = new CameraSettings();
        cameraSettings.setRequestedCameraId(
                sharedPrefs.getBoolean(
                        Constants.SETTINGS.SCANNER.FRONT_CAM,
                        Constants.SETTINGS_DEFAULT.SCANNER.FRONT_CAM
                ) ? 1 : 0
        );
        barcodeScannerView.getBarcodeView().setCameraSettings(cameraSettings);

        capture = new ScanInputCaptureManager(activity, barcodeScannerView, this);
        capture.decode();
    }

    @Override
    public void onResume() {
        super.onResume();
        capture.onResume();
    }

    @Override
    public void onPause() {
        capture.onPause();
        super.onPause();
    }

    @Override
    public void onBarcodeResult(BarcodeResult result) {
        if(result.getText().isEmpty()) {
            resumeScanningAndDecoding();
            return;
        }
        String[] resultSplit = result.getText().split("\\|");
        if(resultSplit.length != 2) {
            activity.showMessage(R.string.error_api_qr_code);
            resumeScanningAndDecoding();
            return;
        }
        String apiURL = resultSplit[0];
        String serverURL = apiURL.replaceAll("/api$", "");
        String ingressProxyId = null;
        if(serverURL.startsWith("/api/hassio_ingress/")) {
            ingressProxyId = serverURL.replace("/api/hassio_ingress/", "");
        }
        String apiKey = resultSplit[1];
        if(ingressProxyId == null) {
            navigate(LoginApiQrCodeFragmentDirections
                    .actionLoginApiQrCodeFragmentToLoginRequestFragment(serverURL, apiKey));
        } else { // grocy home assistant add-on used
            navigate(LoginApiQrCodeFragmentDirections
                    .actionLoginApiQrCodeFragmentToLoginApiFormFragment()
                    .setGrocyIngressProxyId(ingressProxyId)
                    .setGrocyApiKey(apiKey));
        }
    }

    private void resumeScanningAndDecoding() {
        capture.onResume();
        new Handler().postDelayed(() -> capture.decode(), 1000);
    }

    public void enterDataManually() {
        navigate(LoginApiQrCodeFragmentDirections
                .actionLoginApiQrCodeFragmentToLoginApiFormFragment());
    }

    public void openHelpWebsite() {
        boolean success = NetUtil.openURL(requireContext(), Constants.URL.HELP);
        if(!success) activity.showMessage(R.string.error_no_browser);
    }

    public void openGrocyWebsite() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_grocy))));
    }

    public void showFeedbackBottomSheet() {
        activity.showBottomSheet(new FeedbackBottomSheet());
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        return setStatusBarColor(transit, enter, nextAnim, activity, R.color.background);
    }
}
