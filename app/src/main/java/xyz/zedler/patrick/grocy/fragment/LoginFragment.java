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

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;
import androidx.navigation.NavOptions;
import androidx.preference.PreferenceManager;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.FragmentLoginPage0Binding;
import xyz.zedler.patrick.grocy.databinding.FragmentLoginPage1Binding;
import xyz.zedler.patrick.grocy.databinding.FragmentLoginPage2Binding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.CompatibilityBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.FeedbackBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MessageBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.scan.ScanInputCaptureManager;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.ConfigUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NetUtil;
import xyz.zedler.patrick.grocy.view.ActionButton;
import xyz.zedler.patrick.grocy.web.RequestQueueSingleton;

public class LoginFragment extends BaseFragment implements ScanInputCaptureManager.BarcodeListener{

    public final static int PAGE_DEMO_OR_OWN = 0;
    public final static int PAGE_QR_CODE_SCAN = 1;
    public final static int PAGE_SERVER_FORM = 2;

    final static String TAG = LoginFragment.class.getSimpleName();

    private FragmentLoginPage0Binding bindingPage0;
    private FragmentLoginPage1Binding bindingPage1;
    private FragmentLoginPage2Binding bindingPage2;
    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private SharedPreferences credentials;
    private DownloadHelper dlHelper;
    private ScanInputCaptureManager capture;
    private final ClickUtil clickUtil = new ClickUtil();
    private boolean debug;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        if(getPageType() == PAGE_DEMO_OR_OWN) {
            bindingPage0 = FragmentLoginPage0Binding.inflate(inflater, container, false);
            return bindingPage0.getRoot();
        } else if(getPageType() == PAGE_QR_CODE_SCAN) {
            bindingPage1 = FragmentLoginPage1Binding.inflate(inflater, container, false);
            return bindingPage1.getRoot();
        } else {
            bindingPage2 = FragmentLoginPage2Binding.inflate(inflater, container, false);
            return bindingPage2.getRoot();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dlHelper.destroy();
        bindingPage0 = null;
        bindingPage1 = null;
        bindingPage2 = null;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) requireActivity();

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);
        credentials = activity.getSharedPreferences(
                Constants.PREF.CREDENTIALS,
                Context.MODE_PRIVATE
        );

        // WEB REQUESTS

        dlHelper = new DownloadHelper(activity, TAG);

        if(getPageType() == PAGE_DEMO_OR_OWN) {
            bindingPage0.demoInstance.setOnClickListener(v -> {
                bindingPage0.ownInstance.setEnabled(false);
                bindingPage0.demoInstance.setEnabled(false);
                requestLogin(getString(R.string.url_grocy_demo), "", true, true);
            });
            bindingPage0.ownInstance.setOnClickListener(v -> {
                bindingPage0.ownInstance.setEnabled(false);
                bindingPage0.demoInstance.setEnabled(false);
                navigate(LoginFragmentDirections.actionLoginFragmentSelf().setPage(PAGE_QR_CODE_SCAN));
            });
            setupBottomButtons();
            return;
        } else if(getPageType() == PAGE_QR_CODE_SCAN) {
            bindingPage1.enterManually.setOnClickListener(v -> {
                bindingPage1.enterManually.setEnabled(false);
                navigate(LoginFragmentDirections.actionLoginFragmentSelf().setPage(PAGE_SERVER_FORM));
            });

            DecoratedBarcodeView barcodeScannerView = bindingPage1.barcodeScanInput;
            barcodeScannerView.setTorchOff();
            CameraSettings cameraSettings = new CameraSettings();
            cameraSettings.setRequestedCameraId(
                    sharedPrefs.getBoolean(Constants.PREF.USE_FRONT_CAM, false) ? 1 : 0
            );
            barcodeScannerView.getBarcodeView().setCameraSettings(cameraSettings);

            capture = new ScanInputCaptureManager(activity, barcodeScannerView, this);
            capture.decode();
            return;
        }

        // PAGE TYPE is PAGE_SERVER_FORM

        if(credentials.getString(Constants.PREF.SERVER_URL, null) != null) {
            bindingPage2.editTextLoginServer.setText(
                    credentials.getString(Constants.PREF.SERVER_URL, null)
            );
        }
        bindingPage2.editTextLoginKey.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                if(bindingPage2.textInputLoginKey.isErrorEnabled()) {
                    bindingPage2.textInputLoginKey.setErrorEnabled(false);
                }
            }
        });

        if(credentials.getString(Constants.PREF.API_KEY, null) != null) {
            bindingPage2.editTextLoginKey.setText(
                    credentials.getString(Constants.PREF.API_KEY, null)
            );
        }
        bindingPage2.editTextLoginServer.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                if(s != null) updatePrefixRadioButtons(s.toString());
                if(bindingPage2.textInputLoginServer.isErrorEnabled()) {
                    bindingPage2.textInputLoginServer.setErrorEnabled(false);
                }
            }
        });
        if(bindingPage2.editTextLoginServer.getText() != null) {
            updatePrefixRadioButtons(bindingPage2.editTextLoginServer.getText().toString());
        }

        bindingPage2.https.setOnClickListener(v -> {
            if(bindingPage2.editTextLoginServer.getText() == null) {
                bindingPage2.editTextLoginServer.setText("https://");
                return;
            }
            String input = bindingPage2.editTextLoginServer.getText().toString();
            if(!input.contains("https://") && !input.contains("http://")) {
                bindingPage2.editTextLoginServer.setText("https://" + input);
            } else if(input.contains("http://")) {
                bindingPage2.editTextLoginServer.setText(input.replace("http://", "https://"));
            }
        });
        bindingPage2.http.setOnClickListener(v -> {
            if(bindingPage2.editTextLoginServer.getText() == null) {
                bindingPage2.editTextLoginServer.setText("http://");
                return;
            }
            String input = bindingPage2.editTextLoginServer.getText().toString();
            if(!input.contains("https://") && !input.contains("http://")) {
                bindingPage2.editTextLoginServer.setText("http://" + input);
            } else if(input.contains("https://")) {
                bindingPage2.editTextLoginServer.setText(input.replace("https://", "http://"));
            }
        });

        bindingPage2.buttonLoginKey.setOnClickListener(v -> {
            if(clickUtil.isDisabled()) return;
            if(getServer().isEmpty()) {
                bindingPage2.textInputLoginServer.setError(getString(R.string.error_empty));
            } else if(!URLUtil.isValidUrl(getServer())) {
                bindingPage2.textInputLoginServer.setError(getString(R.string.error_invalid_url));
            } else {
                bindingPage2.textInputLoginServer.setErrorEnabled(false);

                Intent browserManageKeys = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.parse(getServer() + "/manageapikeys");
                browserManageKeys.setData(uri);
                startActivity(browserManageKeys);
            }
        });

        bindingPage2.buttonLoginLogin.setOnClickListener(v -> {
            if(clickUtil.isDisabled()) return;

            // remove old errors
            bindingPage2.textInputLoginServer.setErrorEnabled(false);
            bindingPage2.textInputLoginKey.setErrorEnabled(false);

            String server = getServer();
            if(server.isEmpty()) {
                bindingPage2.textInputLoginServer.setError(getString(R.string.error_empty));
            } else if(!URLUtil.isNetworkUrl(server)) {
                bindingPage2.textInputLoginServer.setError(getString(R.string.error_invalid_url));
            } else {
                requestLogin(server, getKey(), true, false);
            }
        });

        setupBottomButtons();
    }

    private void updatePrefixRadioButtons(String input) {
        bindingPage2.https.setChecked(input.contains("https://"));
        bindingPage2.http.setChecked(input.contains("http://"));
    }

    private void setupBottomButtons() {
        if(getPageType() != 2 && getPageType() != 0) return;
        ActionButton help = getPageType() == 2 ? bindingPage2.buttonLoginHelp
                : bindingPage0.buttonLoginHelp;
        ActionButton feedback = getPageType() == 2 ? bindingPage2.buttonLoginFeedback
                : bindingPage0.buttonLoginFeedback;
        ActionButton about = getPageType() == 2 ? bindingPage2.buttonLoginAbout
                : bindingPage0.buttonLoginAbout;
        ActionButton website = getPageType() == 2 ? bindingPage2.buttonLoginWebsite
                : bindingPage0.buttonLoginWebsite;
        ActionButton settings = getPageType() == 2 ? bindingPage2.buttonLoginSettings
                : bindingPage0.buttonLoginSettings;
        View container = getPageType() == 2 ? bindingPage2.coordinatorContainer
                : bindingPage0.coordinateContainer;

        help.setTooltipText(getString(R.string.title_help));
        help.setOnClickListener(v -> {
            if(clickUtil.isDisabled()) return;
            help.startIconAnimation();
            new Handler().postDelayed(() -> {
                boolean success = NetUtil.openURL(requireContext(), Constants.URL.HELP);
                if(!success) {
                    Snackbar.make(
                            container,
                            R.string.error_no_browser,
                            Snackbar.LENGTH_LONG
                    ).show();
                }
            }, 300);
        });
        feedback.setTooltipText(getString(R.string.title_feedback));
        feedback.setOnClickListener(v -> {
            if(clickUtil.isDisabled()) return;
            feedback.startIconAnimation();
            activity.showBottomSheet(new FeedbackBottomSheet(), null);
        });
        about.setTooltipText(getString(R.string.title_about_this_app));
        about.setOnClickListener(v -> {
            if(clickUtil.isDisabled()) return;
            about.startIconAnimation();
            navigateDeepLink(R.string.deep_link_aboutFragment);
        });
        website.setTooltipText(getString(R.string.info_website));
        website.setOnClickListener(v -> {
            if(clickUtil.isDisabled()) return;
            website.startIconAnimation();
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_grocy))));
        });
        settings.setTooltipText(getString(R.string.title_settings));
        settings.setOnClickListener(v -> {
            if(clickUtil.isDisabled()) return;
            settings.startIconAnimation();
            navigateDeepLink(R.string.deep_link_settingsFragment);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getPageType() == PAGE_QR_CODE_SCAN) {
            capture.onResume();
        }
        activity.getWindow().setStatusBarColor(ResourcesCompat.getColor(
                getResources(),
                R.color.background,
                null
        ));
    }

    @Override
    public void onPause() {
        super.onPause();
        if(getPageType() == PAGE_QR_CODE_SCAN) {
            capture.onPause();
        }
    }

    @Override
    public void requestLogin(String server, String key, boolean checkVersion, boolean isDemo) {
        if(server.contains(".onion") && !OrbotHelper.get(requireContext()).init()) {
            showMessage(R.string.error_orbot_not_installed);
            OrbotHelper.get(requireContext()).installOrbot(requireActivity());
            return;
        }
        RequestQueueSingleton.getInstance(requireContext()).newRequestQueue(server);
        dlHelper.reloadRequestQueue(requireActivity());

        if(getPageType() == 2) {
            bindingPage2.buttonLoginLogin.setEnabled(false);
        }
        dlHelper.get(
                server + "/api/system/info?GROCY-API-KEY=" + key,
                response -> {
                    if(debug) Log.i(TAG, "requestLogin: " + response);
                    if(!response.contains("grocy_version")) {
                        if(getPageType() == 2) {
                            bindingPage2.textInputLoginServer.setError(
                                getString(R.string.error_not_grocy_instance)
                            );
                            enableLoginButtons();
                        } else {
                            showMessage(R.string.error_not_grocy_instance);
                        }
                        openHomeAssistantHelp(server);
                        return;
                    }
                    try {
                        String grocyVersion = new JSONObject(response)
                                .getJSONObject("grocy_version")
                                .getString("Version");
                        ArrayList<String> supportedVersions = new ArrayList<>(
                                Arrays.asList(
                                        getResources().getStringArray(
                                                R.array.compatible_grocy_versions
                                        )
                                )
                        );
                        if(checkVersion && !supportedVersions.contains(grocyVersion)) {
                            sharedPrefs.edit().remove(Constants.PREF.VERSION_COMPATIBILITY_IGNORED)
                                    .apply();
                            Bundle bundle = new Bundle();
                            bundle.putString(Constants.ARGUMENT.SERVER, server);
                            bundle.putString(Constants.ARGUMENT.KEY, key);
                            bundle.putString(Constants.ARGUMENT.VERSION, grocyVersion);
                            bundle.putBoolean(Constants.ARGUMENT.DEMO_CHOSEN, isDemo);
                            bundle.putStringArrayList(
                                    Constants.ARGUMENT.SUPPORTED_VERSIONS,
                                    supportedVersions
                            );
                            activity.showBottomSheet(
                                    new CompatibilityBottomSheet(),
                                    bundle
                            );
                            return;
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "requestLogin: " + e);
                    }

                    if(debug) Log.i(TAG, "requestLogin: successfully logged in");
                    sharedPrefs.edit()
                            .putString(Constants.PREF.SERVER_URL, server)
                            .putString(Constants.PREF.API_KEY, key)
                            .apply();
                    if(!isDemo) {
                        credentials.edit()
                                .putString(Constants.PREF.SERVER_URL, server)
                                .putString(Constants.PREF.API_KEY, key)
                                .apply();
                    }
                    loadInfoAndFinish();
                },
                error -> {
                    Log.e(TAG, "requestLogin: VolleyError: " + error);
                    if(error instanceof AuthFailureError) {
                        if(getPageType() == 2) {
                            bindingPage2.textInputLoginKey.setError(
                                    getString(R.string.error_api_not_working)
                            );
                        } else {
                            showMessage(R.string.error_api_not_working);
                        }
                        openHomeAssistantHelp(server);
                    } else if(error instanceof NoConnectionError) {
                        if(error.toString().contains("SSLHandshakeException")) {
                            Bundle bundle = new Bundle();
                            bundle.putString(
                                    Constants.ARGUMENT.TITLE,
                                    getString(R.string.error_handshake)
                            );
                            bundle.putString(
                                    Constants.ARGUMENT.TEXT,
                                    getString(R.string.error_handshake_description, server)
                            );
                            activity.showBottomSheet(new MessageBottomSheet(), bundle);
                        } else if(error.toString().contains("Invalid host")) {
                            if(getPageType() == 2) {
                                bindingPage2.textInputLoginServer.setError(
                                        getString(R.string.error_invalid_url)
                                );
                            } else {
                                showMessage(R.string.error_invalid_url);
                            }
                        } else {
                            showMessage(getString(R.string.error_failed_to_connect_to, server));
                        }
                    } else if(error instanceof ServerError && error.networkResponse != null) {
                        int code = error.networkResponse.statusCode;
                        if (code == 404) {
                            if(getPageType() == 2) {
                                bindingPage2.textInputLoginServer.setError(
                                        getString(R.string.error_not_grocy_instance)
                                );
                            } else {
                                showMessage(R.string.error_not_grocy_instance);
                            }
                        } else {
                            showMessage(getString(R.string.error_unexpected_response_code, code));
                        }
                    } else if(error instanceof ServerError) {
                        showMessage(getString(R.string.error_unexpected_response));
                    } else if(error instanceof TimeoutError) {
                        showMessage(getString(R.string.error_timeout));
                    } else {
                        showMessage(getString(R.string.error_undefined) + ": " + error);
                    }
                    enableLoginButtons();
                    if(getPageType() != PAGE_SERVER_FORM) {
                        navigate(LoginFragmentDirections.actionLoginFragmentSelf()
                                .setPage(PAGE_SERVER_FORM));
                    }
                }
        );
    }

    @Override
    public boolean onBackPressed() {
        if(getPageType() == PAGE_SERVER_FORM || getPageType() == PAGE_QR_CODE_SCAN) {
            activity.navigateUp();
            return true;
        }
        activity.finish();
        return true;
    }

    private void loadInfoAndFinish() {
        ConfigUtil.loadInfo(
                new DownloadHelper(requireActivity(), TAG),
                new GrocyApi(requireContext()),
                sharedPrefs,
                this::navigateToStartDestination,
                error -> navigateToStartDestination()
        );
    }

    private void navigateToStartDestination() {
        activity.updateStartDestination();
        NavOptions.Builder builder = new NavOptions.Builder();
        builder.setEnterAnim(R.anim.slide_from_end);
        builder.setExitAnim(R.anim.slide_to_start);
        builder.setPopEnterAnim(R.anim.slide_from_start);
        builder.setPopExitAnim(R.anim.slide_to_end);
        builder.setPopUpTo(R.id.navigation_main, true);
        navigate(findNavController().getGraph().getStartDestination(), builder.build());
    }

    private void openHomeAssistantHelp(String server) {
        if(server.endsWith("_grocy")
                || server.contains("hassio_ingress")
                || server.contains("hassio/ingress")
        ) {
            // maybe a grocy instance on Hass.io - url doesn't work like this
            boolean success = NetUtil.openURL(
                    requireContext(),
                    Constants.URL.FAQ + "#user-content-faq4"
            );
            if(!success) {
                Snackbar.make(
                        bindingPage2.coordinatorContainer,
                        R.string.error_no_browser,
                        Snackbar.LENGTH_LONG
                ).show();
            }
        }
    }

    @Override
    public void enableLoginButtons() {
        if(getPageType() != 2) return;
        bindingPage2.buttonLoginLogin.setEnabled(true);
    }

    @Override
    public void onBarcodeResult(BarcodeResult result) {
        if(result.getText().isEmpty()) {
            resumeScanningAndDecoding();
            return;
        }
        String[] resultSplit = result.getText().split("\\|");
        if(resultSplit.length != 2) {
            showMessage(R.string.error_api_qr_code);
            resumeScanningAndDecoding();
            return;
        }
        String apiURL = resultSplit[0];
        String serverURL = apiURL.replace("/api", "");
        String apiKey = resultSplit[1];
        requestLogin(serverURL, apiKey, true, false);
    }

    private void resumeScanningAndDecoding() {
        capture.onResume();
        new Handler().postDelayed(() -> {
            if(getPageType() == PAGE_QR_CODE_SCAN && bindingPage1 == null) return;
            capture.decode();
        }, 1000);
    }

    private String getServer() {
        Editable server = bindingPage2.editTextLoginServer.getText();
        if(server == null) return "";
        return server.toString().replaceAll("/+$", "").trim();
    }

    private String getKey() {
        Editable key = bindingPage2.editTextLoginKey.getText();
        if(key == null) return "";
        return key.toString().trim();
    }

    private void showMessage(String text) {
        if(getPageType() == 0) {
            Snackbar.make(bindingPage0.coordinateContainer, text, Snackbar.LENGTH_LONG).show();
        } else if(getPageType() == 1) {
            Snackbar.make(bindingPage1.relativeContainer, text, Snackbar.LENGTH_LONG).show();
        } else if(getPageType() == 2) {
            Snackbar.make(bindingPage2.coordinatorContainer, text, Snackbar.LENGTH_LONG).show();
        }
    }

    private void showMessage(@StringRes int text) {
        showMessage(getString(text));
    }

    private int getPageType() {
        return LoginFragmentArgs.fromBundle(requireArguments()).getPage();
    }
}
