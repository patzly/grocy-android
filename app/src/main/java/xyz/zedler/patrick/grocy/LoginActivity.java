package xyz.zedler.patrick.grocy;

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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.webkit.URLUtil;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.web.RequestQueueSingleton;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class LoginActivity extends AppCompatActivity {

    final static String TAG = "LoginActivity";
    private final static boolean DEBUG = false;

    private SharedPreferences sharedPrefs;
    private WebRequest request;

    private TextInputLayout textInputLayoutKey;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_login);

        // WEB REQUESTS

        RequestQueue requestQueue = RequestQueueSingleton
                .getInstance(getApplicationContext())
                .getRequestQueue();
        request = new WebRequest(requestQueue);

        // INITIALIZE VIEWS

        TextInputLayout textInputLayoutServer = findViewById(R.id.text_input_login_server);
        EditText editTextServer = textInputLayoutServer.getEditText();
        assert editTextServer != null;

        textInputLayoutKey = findViewById(R.id.text_input_login_key);
        EditText editTextKey = textInputLayoutKey.getEditText();
        assert editTextKey != null;

        findViewById(R.id.button_login_key).setOnClickListener(v -> {
            if(editTextServer.getText().toString().equals("")) {
                textInputLayoutServer.setError(getString(R.string.error_empty));
            } else if(!URLUtil.isValidUrl(editTextServer.getText().toString())) {
                textInputLayoutServer.setError(getString(R.string.error_invalid_url));
            } else {
                textInputLayoutServer.setErrorEnabled(false);

                Intent browserManageKeys = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.parse(editTextServer.getText().toString() + "/manageapikeys");
                browserManageKeys.setData(uri);
                startActivity(browserManageKeys);
            }
        });

        findViewById(R.id.button_login_login).setOnClickListener(v -> {
            // remove old errors
            textInputLayoutServer.setErrorEnabled(false);
            textInputLayoutKey.setErrorEnabled(false);

            String server = editTextServer.getText()
                    .toString()
                    .replaceAll("/+$", "");
            String key = editTextKey.getText().toString().trim();
            if(server.equals("")) {
                textInputLayoutServer.setError(getString(R.string.error_empty));
            } else if(!Patterns.WEB_URL.matcher(server).matches()) {
                textInputLayoutServer.setError(getString(R.string.error_invalid_url));
            } else {
                requestLogin(server, key);
            }
        });

        findViewById(R.id.button_login_demo).setOnClickListener(v -> {
            sharedPrefs.edit()
                    .putString(Constants.PREF.SERVER_URL, getString(R.string.url_grocy_demo))
                    .putString(Constants.PREF.API_KEY, "")
                    .apply();
            setResult(Activity.RESULT_OK);
            finish();
        });

        if(getIntent().getBooleanExtra(Constants.EXTRA.AFTER_FEATURES_ACTIVITY, false)) {
            showMessage(getString(R.string.msg_features));
        }
    }

    private void requestLogin(String server, String key) {
        request.get(
                server + "/api/system/info?GROCY-API-KEY=" + key,
                response -> {
                    if(DEBUG) Log.i(TAG, "requestLogin: successfully logged in");
                    sharedPrefs.edit()
                            .putString(Constants.PREF.SERVER_URL, server)
                            .putString(Constants.PREF.API_KEY, key)
                            .apply();
                    setResult(Activity.RESULT_OK);
                    finish();
                },
                error -> {
                    if(error instanceof AuthFailureError) {
                        textInputLayoutKey.setError(getString(R.string.error_api_not_working));
                    } else {
                        Log.e(TAG, "requestLogin: VolleyError: " + error);
                        showMessage(getString(R.string.msg_error));
                    }
                }
        );
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private void showMessage(String text) {
        Snackbar.make(
                findViewById(R.id.linear_login_container),
                text,
                Snackbar.LENGTH_SHORT
        ).show();
    }
}
